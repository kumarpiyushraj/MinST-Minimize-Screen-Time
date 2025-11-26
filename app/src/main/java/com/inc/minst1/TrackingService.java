//package com.inc.minst1;
//
//import android.app.AlarmManager;
//import android.app.Notification;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.SharedPreferences;
//import android.content.pm.PackageManager;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.Looper;
//import android.os.PowerManager;
//import android.os.SystemClock;
//import android.util.Log;
//import androidx.annotation.Nullable;
//import androidx.core.app.NotificationCompat;
//import java.text.SimpleDateFormat;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//public class TrackingService extends Service {
//    public static final String ACTION_START_FAST_CHECKS = "com.inc.minst1.ACTION_START_FAST_CHECKS";
//    public static final String ACTION_STOP_FAST_CHECKS = "com.inc.minst1.ACTION_STOP_FAST_CHECKS";
//    private static final String LOG_TAG_SERVICE = "TrackingService";
//
//    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
//    private SharedPreferences sharedPrefs;
//    private ScreenStateReceiver screenStateReceiver;
//    private final Handler checkHandler = new Handler(Looper.getMainLooper());
//    private final Runnable checkRunnable = this::performTrackingChecks;
//
//    private Map<String, Long> yesterdayUsageCache = new HashMap<>();
//    private String dateForCache;
//    private boolean isCacheLoaded = false;
//    private AppDatabase database; // Add database field
//    private boolean isInitialCheck = true;
//
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        database = AppDatabase.getDatabase(this); // Initialize database
//        sharedPrefs = getSharedPreferences("MinST_Notifications", MODE_PRIVATE);
//        screenStateReceiver = new ScreenStateReceiver();
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(Intent.ACTION_SCREEN_ON);
//        filter.addAction(Intent.ACTION_SCREEN_OFF);
//        registerReceiver(screenStateReceiver, filter);
//        Log.d(LOG_TAG_SERVICE, "Service created.");
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        startForeground(1, createServiceNotification());
//        String action = (intent != null) ? intent.getAction() : null;
//        if (action != null) {
//            switch (action) {
//                case ACTION_START_FAST_CHECKS:
//                    startFastCheckLoop();
//                    break;
//                case ACTION_STOP_FAST_CHECKS:
//                    stopFastCheckLoop();
//                    break;
//                default:
//                    initializeServiceState();
//                    break;
//            }
//        } else {
//            initializeServiceState();
//        }
//        return START_STICKY;
//    }
//
//    @Override
//    public void onTaskRemoved(Intent rootIntent) {
//        Log.e(LOG_TAG_SERVICE, "Task was removed by user. Scheduling restart...");
//        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
//        restartServiceIntent.setPackage(getPackageName());
//        PendingIntent restartServicePendingIntent = PendingIntent.getService(
//                getApplicationContext(), 1, restartServiceIntent,
//                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
//        getSystemService(AlarmManager.class).set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 2000, restartServicePendingIntent);
//        super.onTaskRemoved(rootIntent);
//    }
//
//    private void initializeServiceState() {
//        Log.d(LOG_TAG_SERVICE, "Initializing service state...");
//        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//        if (pm != null && pm.isInteractive()) {
//            Log.d(LOG_TAG_SERVICE, "Screen is on, starting fast checks.");
//            startFastCheckLoop();
//        } else {
//            Log.d(LOG_TAG_SERVICE, "Screen is off, service is idle.");
//        }
//    }
//
//    private void startFastCheckLoop() {
//        checkHandler.removeCallbacks(checkRunnable);
//        if (isInitialCheck) {
//            long initialDelay = TimeUnit.MINUTES.toMillis(3);
//            Log.d(LOG_TAG_SERVICE, "--- SCHEDULING INITIAL CHECK --- Delaying first check by 3 minutes to allow DataSaveWorker to finish.");
//            checkHandler.postDelayed(checkRunnable, initialDelay);
//            isInitialCheck = false;
//        } else {
//            Log.d(LOG_TAG_SERVICE, "--- SCHEDULING IMMEDIATE CHECK ---");
//            checkHandler.removeCallbacks(checkRunnable);
//            //Log.d(LOG_TAG_SERVICE, "Starting fast check looppppppppp.");
//            checkHandler.post(checkRunnable);
//        }
//    }
//
//    private void stopFastCheckLoop() {
//        Log.d(LOG_TAG_SERVICE, "Stopping fast check loop.");
//        checkHandler.removeCallbacks(checkRunnable);
//    }
//
//    private void performTrackingChecks() {
//        executorService.execute(() -> {
//            Log.d(LOG_TAG_SERVICE, "--- BEGINNING TRACKING CHECK CYCLE ---");
//            String currentDate = getTodayDateString();
//            if (!isCacheLoaded || dateForCache == null || !currentDate.equals(dateForCache)) {
//                dateForCache = currentDate;
//                if (!loadYesterdayUsageCacheFromDb()) {
//                    Log.w(LOG_TAG_SERVICE, "Yesterday's data not ready in DB. Rescheduling check for 1 minute.");
//                    checkHandler.postDelayed(checkRunnable, TimeUnit.MINUTES.toMillis(1));
//                    return;
//                }
//            }
//
//            try {
//                Log.d(LOG_TAG_SERVICE, "Cache is loaded. Proceeding with live usage checks.");
//                long startOfToday = UsageStatsHelper.getStartOfDayInMillis();
//                long now = System.currentTimeMillis();
//                HashMap<String, Long> todayUsageMap = UsageStatsHelper.getAggregatedUsageMap(this, startOfToday, now);
//                Log.d(LOG_TAG_SERVICE, "Successfully fetched live usage for " + todayUsageMap.size() + " apps today.");
//
//                checkGoalLimits(todayUsageMap);
//                checkYesterdayLimits(todayUsageMap);
//
//            } catch (Exception e) {
//                Log.e(LOG_TAG_SERVICE, "An unexpected error occurred during tracking check.", e);
//            } finally {
//                Log.d(LOG_TAG_SERVICE, "--- FINISHING TRACKING CHECK CYCLE --- Scheduling next check in 1 minute.");
//                checkHandler.postDelayed(checkRunnable, TimeUnit.MINUTES.toMillis(1));
//            }
//        });
//    }
//
//    private boolean loadYesterdayUsageCacheFromDb() {
//        Log.d(LOG_TAG_SERVICE, "[DB] Attempting to load yesterday's usage from database...");
//        Calendar calendar = Calendar.getInstance();
//        calendar.add(Calendar.DAY_OF_YEAR, -1);
//        String yesterdayDateString = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
//
//        try {
//            Map<String, Long> dbData = database.usageDao().getUsageMapForDate(yesterdayDateString);
//            if (dbData != null && !dbData.isEmpty()) {
//                yesterdayUsageCache = dbData;
//                isCacheLoaded = true;
//                Log.d(LOG_TAG_SERVICE, "[DB] SUCCESS: Cached " + yesterdayUsageCache.size() + " apps from DB for " + yesterdayDateString);
//                return true;
//            } else {
//                Log.w(LOG_TAG_SERVICE, "[DB] FAILED: Data for " + yesterdayDateString + " is not yet available in the database.");
//                isCacheLoaded = false;
//                return false;
//            }
//        } catch (Exception e) {
//            Log.e(LOG_TAG_SERVICE, "[DB] CRITICAL ERROR loading data from database.", e);
//            isCacheLoaded = false;
//            return false;
//        }
//    }
//
//    private void checkGoalLimits(HashMap<String, Long> todayUsageMap) {
//        String todayDate = getTodayDateString();
//        Set<String> goalNotifiedToday = new HashSet<>(sharedPrefs.getStringSet("goal_notified_" + todayDate, new HashSet<>()));
//        AppDatabase database = AppDatabase.getDatabase(this);
//        List<AppGoalEntity> goals = database.goalDao().getAllGoals();
//        if (goals.isEmpty()) return;
//
//        boolean wasListModified = false;
//        for (AppGoalEntity goal : goals) {
//            if (goal.dailyGoalMillis <= 0) continue;
//            long usage = todayUsageMap.getOrDefault(goal.packageName, 0L);
//
//            if (usage >= goal.dailyGoalMillis && !goalNotifiedToday.contains(goal.packageName)) {
//                NotificationHelper.sendNotification(this, "Usage Limit Reached", "Limit: " + UsageStatsHelper.formatTime(goal.dailyGoalMillis), goal.packageName);
//                goalNotifiedToday.add(goal.packageName);
//                wasListModified = true;
//            }
//        }
//        if (wasListModified) {
//            sharedPrefs.edit().putStringSet("goal_notified_" + todayDate, goalNotifiedToday).apply();
//        }
//    }
//    private void checkYesterdayLimits(HashMap<String, Long> todayUsageMap) {
//        Log.d("NotificationCheck", "--- Starting 'More Than Yesterday' check ---");
//        if (!isCacheLoaded || yesterdayUsageCache.isEmpty()) {
//            Log.w("NotificationCheck", "Check skipped: Yesterday's usage cache is not loaded.");
//            return;
//        }
//
//        String todayDate = getTodayDateString();
//        Set<String> alreadyNotified = new HashSet<>(sharedPrefs.getStringSet("yesterday_notified_" + todayDate, new HashSet<>()));
//        Log.d("NotificationCheck", "Apps already notified today: " + alreadyNotified);
//
//        Map<String, String> appsToNotify = new HashMap<>();
//        PackageManager pm = getPackageManager();
//
//        for (Map.Entry<String, Long> entry : todayUsageMap.entrySet()) {
//            String packageName = entry.getKey();
//            if (UsageStatsHelper.isSystemApp(pm, packageName) || alreadyNotified.contains(packageName)) {
//                continue;
//            }
//
//            long todayUsage = entry.getValue();
//            long yesterdayUsage = yesterdayUsageCache.getOrDefault(packageName, 0L);
//
//            // Detailed log for every potential candidate app
//           // Log.d("NotificationCheck", "Checking [" + packageName + "]: Today=" + UsageStatsHelper.formatTime(todayUsage) + ", Yesterday=" + UsageStatsHelper.formatTime(yesterdayUsage));
//
//            boolean needsNotification = todayUsage > yesterdayUsage &&
//                    todayUsage > TimeUnit.MINUTES.toMillis(5) && // Must be used for a meaningful time today
//                    !alreadyNotified.contains(packageName) && // To avoid spamming the notification
//                    yesterdayUsage > TimeUnit.MINUTES.toMillis(1); // Must have been used for a meaningful time yesterday
//
//            if (needsNotification) {
//                Log.d("NotificationCheck", ">>> CONDITION MET for " + packageName + "! Queueing for notification.");
//                String content = "Today: " + UsageStatsHelper.formatTime(todayUsage) + " | Yesterday: " + UsageStatsHelper.formatTime(yesterdayUsage);
//                appsToNotify.put(packageName, content);
//            }
//        }
//
//        if (!appsToNotify.isEmpty()) {
//            Log.d("NotificationCheck", "Found " + appsToNotify.size() + " apps to notify: " + appsToNotify.keySet());
//            for (Map.Entry<String, String> notificationEntry : appsToNotify.entrySet()) {
//                NotificationHelper.sendNotification(this, "Used More Than Yesterday", notificationEntry.getValue(), notificationEntry.getKey());
//                alreadyNotified.add(notificationEntry.getKey());
//            }
//            sharedPrefs.edit().putStringSet("yesterday_notified_" + todayDate, alreadyNotified).apply();
//            Log.d("NotificationCheck", "Saved updated notified list: " + alreadyNotified);
//        }
//        Log.d("NotificationCheck", "--- Finished 'More Than Yesterday' check ---");
//    }
//
//    private String getTodayDateString() {
//        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
//    }
//
//    private Notification createServiceNotification() {
//        Intent notificationIntent = new Intent(this, MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
//        return new NotificationCompat.Builder(this, MyApplication.CHANNEL_ID_SERVICE)
//                .setContentTitle("MinST Protection is Active")
//                .setContentText("Monitoring app usage to help you stay mindful.")
//                .setSmallIcon(R.drawable.ic_shield_check)
//                .setContentIntent(pendingIntent)
//                .setOngoing(true)
//                .build();
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        stopFastCheckLoop();
//        unregisterReceiver(screenStateReceiver);
//        if (executorService != null) executorService.shutdownNow();
//        Log.d(LOG_TAG_SERVICE, "Service destroyed.");
//    }
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//}


package com.inc.minst1;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TrackingService extends Service {
    private static final String LOG_TAG = "UnifiedTrackingService";
    private static final int FOREGROUND_ID = 1001;

    // Public actions for external components to control the service's behavior
    // Actions to control the service
    public static final String ACTION_START_REMINDERS = "com.inc.minst1.ACTION_START_REMINDERS";
    public static final String ACTION_STOP_REMINDERS = "com.inc.minst1.ACTION_STOP_REMINDERS";
    public static final String ACTION_START_CHECKS = "com.inc.minst1.ACTION_START_CHECKS";
    public static final String ACTION_STOP_CHECKS = "com.inc.minst1.ACTION_STOP_CHECKS";
    public static final String ACTION_STATE_CHANGED = "com.inc.minst1.ACTION_STATE_CHANGED";



    // Core Service Components
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable checkRunnable = this::performChecks;
    private AppDatabase database;
    private SharedPreferences sharedPrefs;
    private SharedPreferences appPrefs;
    private ScreenStateReceiver screenStateReceiver;

    // State Variables
    private boolean areRemindersActive = false; // Controls the overlay timer
    private boolean areFastChecksActive = true; // Controls the yesterday/goal checks
    private Map<String, Long> yesterdayUsageCache = new HashMap<>();
    private String dateForCache;
    private boolean isCacheLoaded = false;
    private long lastFullCheckTimestamp = 0;


    // Overlay-specific components
    private WindowManager windowManager;
    private View overlayView;
    private CountDownTimer overlayCountdown;
    private long cumulativeSessionTime = 0L;
    private long lastCheckTimestamp = 0L;
    private Set<String> ignoredPackages;

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize all components
        try {
            database = AppDatabase.getDatabase(this);
            sharedPrefs = getSharedPreferences("MinST_Notifications", MODE_PRIVATE);
            appPrefs = getSharedPreferences("AppPreferences", MODE_PRIVATE);
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            ignoredPackages = Prefs.getIgnoredPackages(this);

            // Load the last known state of the reminders
            areRemindersActive = appPrefs.getBoolean("isReminderActive", false);
            Log.d(LOG_TAG, "Unified Service Created. Reminder state loaded as: " + areRemindersActive);

            screenStateReceiver = new ScreenStateReceiver();
            IntentFilter filter = new IntentFilter();
            //filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            registerReceiver(screenStateReceiver, filter);
            Log.d(LOG_TAG, "Screen State Receiver registered successfully.");

        } catch (Exception e) {
            Log.e(LOG_TAG, "Critical error during onCreate", e);
            stopSelf(); // Stop the service if initialization fails
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(FOREGROUND_ID, createServiceNotification());

        // Load the latest state from preferences every time a command is received
        loadStateFromPrefs();

        if (intent != null && intent.getAction() != null) {
            handleCommand(intent.getAction());
        } else {
            // This is for a service restart (e.g., after boot).
            // Start the loop only if the screen is currently on.
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isInteractive()) {
                startCheckLoop();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e(LOG_TAG, "Task was removed by user. Scheduling restart...");
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        PendingIntent restartServicePendingIntent = PendingIntent.getService(
                getApplicationContext(), 1, restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        getSystemService(AlarmManager.class).set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 2000, restartServicePendingIntent);
        super.onTaskRemoved(rootIntent);
    }

    private void handleCommand(String action) {
        Log.d(LOG_TAG, "Received command: " + action);
        switch (action) {
            case ACTION_START_REMINDERS: setRemindersActive(true); break;
            case ACTION_STOP_REMINDERS: setRemindersActive(false); break;
            case ACTION_START_CHECKS: startCheckLoop(); break;
            case ACTION_STOP_CHECKS: stopCheckLoop(); break;
        }
        // After handling any command, update the notification to reflect the new state.
        updateForegroundNotification();
    }

    private void setRemindersActive(boolean isActive) {
        if (areRemindersActive == isActive) return;

        areRemindersActive = isActive;
        appPrefs.edit().putBoolean("isReminderActive", isActive).apply();

        // Reset timer state when toggling
        cumulativeSessionTime = 0L;
        lastCheckTimestamp = 0L;
        saveStateToPrefs(); // Save the reset state

        if (!isActive && overlayView != null) {
            handler.post(this::removeOverlay);
        }
        Log.d(LOG_TAG, "Reminder mode switched to: " + (isActive ? "ACTIVE" : "INACTIVE"));

        // --- THIS IS THE CRITICAL FIX ---
        // After changing the state, send a private message to the rest of the app.
        Intent intent = new Intent(ACTION_STATE_CHANGED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        // --- THIS IS THE CRITICAL FIX ---
        // After changing the mode, immediately restart the check loop
        // to apply the new (fast or slow) interval.
        startCheckLoop();
    }

    private void startCheckLoop() {
        handler.removeCallbacks(checkRunnable);
        handler.post(checkRunnable);
        Log.d(LOG_TAG, "Check loop STARTED from startCheckLoop().");
    }

    private void stopCheckLoop() {
        handler.removeCallbacks(checkRunnable);
        saveStateToPrefs();
        Log.d(LOG_TAG, "Check loop STOPPED. State saved.");
    }

    private void performChecks() {
        executor.execute(() -> {
            try {
                // --- Part 1: Fast Polling Logic (Overlay Timer) ---
                // This only runs its logic if the user has enabled reminders.
                if (areRemindersActive) {
                    Log.d(LOG_TAG, "--- Performing Break Reminder Check ---");
                    updateOverlayTimer();
                }

                // --- Part 2: Slow Polling Logic (Background Notifications) ---
                // This check runs periodically regardless of the reminder mode.
                // Background checks only run if the screen is on (controlled by ScreenStateReceiver).
                if (areFastChecksActive) {
                    if (System.currentTimeMillis() - lastFullCheckTimestamp > TimeUnit.MINUTES.toMillis(5)) {
                        Log.d(LOG_TAG, "--- Performing 5-minute full check for notifications ---");
                        runFullNotificationChecks();
                        lastFullCheckTimestamp = System.currentTimeMillis();
                    }
                }

            } catch (Exception e) {
                Log.e(LOG_TAG, "An unexpected error occurred during the check cycle.", e);
            } finally {
                // *** THE FINAL FIX IS HERE ***
                // The loop speed ONLY depends on the user-activated reminder state.
                long nextDelay = areRemindersActive ? TimeUnit.SECONDS.toMillis(15) : TimeUnit.MINUTES.toMillis(5);
                handler.postDelayed(checkRunnable, nextDelay);
                Log.d(LOG_TAG, "--- Check Cycle Finished. Next check in " + (nextDelay / 1000) + "s. Reminder Active: " + areRemindersActive + " ---");
            }
        });
    }

    // --- State Management Methods ---
    private void saveStateToPrefs() {
        appPrefs.edit()
                .putLong("cumulativeSessionTime", cumulativeSessionTime)
                .putLong("lastCheckTimestamp", lastCheckTimestamp)
                .apply();
    }

    private void loadStateFromPrefs() {
        areRemindersActive = appPrefs.getBoolean("isReminderActive", false);
        cumulativeSessionTime = appPrefs.getLong("cumulativeSessionTime", 0L);
        lastCheckTimestamp = appPrefs.getLong("lastCheckTimestamp", 0L);
    }

    private void updateOverlayTimer() {
        try {
            // If the popup is already showing, do nothing until it's dismissed.
            if (overlayView != null) {
                Log.d(LOG_TAG, "[Overlay] Timer paused: Overlay is visible.");
                return;
            }

            // --- THIS IS THE FINAL, SIMPLIFIED LOGIC ---
            // We can now assume the screen is ON and UNLOCKED because the
            // ScreenStateReceiver is the only thing that calls startCheckLoop().

            long now = System.currentTimeMillis();
            long delta = (lastCheckTimestamp == 0) ? 0 : now - lastCheckTimestamp;

            String foregroundApp = UsageStatsHelper.getCurrentForegroundPackage(this);

            // We only proceed if we get a valid app name from the system.
            if (foregroundApp != null && !foregroundApp.isEmpty()) {

                // This is the key: we update the timestamp ONLY when we have valid data.
                lastCheckTimestamp = now;

                if (!ignoredPackages.contains(foregroundApp)) {
                    // If the app is NOT ignored, add the time to our continuous timer.
                    cumulativeSessionTime += delta;
                    Log.d(LOG_TAG, "[Overlay] Session time: " + (cumulativeSessionTime / 1000) + "s on app: " + foregroundApp);

                    if (cumulativeSessionTime >= Prefs.getInterval(this)) {
                        handler.post(this::showOverlay);
                        cumulativeSessionTime = 0L;
                        lastCheckTimestamp = 0L; // Reset after showing overlay
                    }
                } else {
                    // If the user is on an ignored app (like the home screen or our own app),
                    // the timer is effectively paused because we don't add to the cumulative time.
                    Log.d(LOG_TAG, "[Overlay] Timer Paused: On an ignored app (" + foregroundApp + ")");
                }
            } else {
                // If foregroundApp is NULL, we do nothing. We don't increment the timer,
                // and we DON'T update lastCheckTimestamp. The timer is effectively
                // paused, and the delta will correctly "catch up" the time on the next successful read.
                Log.w(LOG_TAG, "[Overlay] Timer Paused: Could not get foreground app this cycle.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error in updateOverlayTimer", e);
        }
    }

    private void showOverlay() {
        if (overlayView != null) {
            removeOverlay(); // Remove any existing overlay first
        }
        handler.removeCallbacks(checkRunnable); // Pause the main loop while overlay is visible

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = 100;

        LayoutInflater inflater = LayoutInflater.from(this);
        overlayView = inflater.inflate(R.layout.overlay_popup, null);

        ProgressBar progress = overlayView.findViewById(R.id.progressCountdown);
        ImageButton ibSnooze = overlayView.findViewById(R.id.ibSnooze);
        ImageButton ibUsage = overlayView.findViewById(R.id.ibUsage);
        ImageButton ibClose = overlayView.findViewById(R.id.ibClose);

        ibUsage.setOnClickListener(v -> {
            Intent usageIntent = new Intent(this, UsageStatsActivity.class);
            usageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(usageIntent);
            removeOverlay();
        });

        ibSnooze.setOnClickListener(v -> {
            Toast.makeText(this, "Reminder snoozed!", Toast.LENGTH_SHORT).show();
            removeOverlay();
            // Reschedule the main loop to start again after the snooze duration
            handler.postDelayed(checkRunnable, Prefs.getSnooze(getApplicationContext()));
        });

        ibClose.setOnClickListener(v -> removeOverlay());

        try {
            windowManager.addView(overlayView, params);
            startCountdown(progress, 30_000L); // 30-second countdown for the popup
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startCountdown(final ProgressBar progress, long durationMs) {
        if (overlayCountdown != null) overlayCountdown.cancel();
        progress.setMax((int) durationMs);

        overlayCountdown = new CountDownTimer(durationMs, 50) {
            @Override
            public void onTick(long millisUntilFinished) {
                progress.setProgress((int) millisUntilFinished);
            }
            @Override
            public void onFinish() {
                removeOverlay();
            }
        }.start();
    }

    private void removeOverlay() {
        if (overlayCountdown != null) {
            overlayCountdown.cancel();
            overlayCountdown = null;
        }
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception ignored) {}
            overlayView = null;
        }
        // --- THIS IS THE FINAL FIX ---
        // Reset the timestamp to ensure the next time calculation starts fresh from zero.
        lastCheckTimestamp = 0L;

        // After removing, restart the main check loop immediately.
        startCheckLoop();
    }

    private void runFullNotificationChecks() {
        try {
            String currentDate = getTodayDateString();
            if (!isCacheLoaded || dateForCache == null || !currentDate.equals(dateForCache)) {
                if (loadYesterdayUsageCacheFromDb()) {
                    dateForCache = currentDate; // Update the date for the loaded cache
                } else {
                    Log.w(LOG_TAG, "[Notifications] Yesterday's data not in DB. Skipping check this cycle.");
                    return;
                }
            }

            long startOfToday = UsageStatsHelper.getStartOfDayInMillis();
            long now = System.currentTimeMillis();
            HashMap<String, Long> todayUsageMap = UsageStatsHelper.getAggregatedUsageMap(this, startOfToday, now);

            // These are your original, unmodified methods
            checkYesterdayLimits(todayUsageMap);
            checkGoalLimits(todayUsageMap);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error in runFullNotificationChecks", e);
        }
    }

    private boolean loadYesterdayUsageCacheFromDb() {
        Log.d(LOG_TAG, "[DB] Attempting to load yesterday's usage from database...");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String yesterdayDateString = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());

        try {
            Map<String, Long> dbData = database.usageDao().getUsageMapForDate(yesterdayDateString);
            if (dbData != null && !dbData.isEmpty()) {
                yesterdayUsageCache = dbData;
                isCacheLoaded = true;
                Log.d(LOG_TAG, "[DB] SUCCESS: Cached " + yesterdayUsageCache.size() + " apps from DB for " + yesterdayDateString);
                return true;
            } else {
                Log.w(LOG_TAG, "[DB] FAILED: Data for " + yesterdayDateString + " is not yet available in the database.");
                isCacheLoaded = false;
                return false;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "[DB] CRITICAL ERROR loading data from database.", e);
            isCacheLoaded = false;
            return false;
        }
    }

    private void checkGoalLimits(HashMap<String, Long> todayUsageMap) {
        String todayDate = getTodayDateString();
        Set<String> goalNotifiedToday = new HashSet<>(sharedPrefs.getStringSet("goal_notified_" + todayDate, new HashSet<>()));
        AppDatabase database = AppDatabase.getDatabase(this);
        List<AppGoalEntity> goals = database.goalDao().getAllGoals();
        if (goals.isEmpty()) return;

        boolean wasListModified = false;
        for (AppGoalEntity goal : goals) {
            if (goal.dailyGoalMillis <= 0) continue;
            long usage = todayUsageMap.getOrDefault(goal.packageName, 0L);

            if (usage >= goal.dailyGoalMillis && !goalNotifiedToday.contains(goal.packageName)) {
                NotificationHelper.sendNotification(this, "Usage Limit Reached", "Limit: " + UsageStatsHelper.formatTime(goal.dailyGoalMillis), goal.packageName);
                goalNotifiedToday.add(goal.packageName);
                wasListModified = true;
            }
        }
        if (wasListModified) {
            sharedPrefs.edit().putStringSet("goal_notified_" + todayDate, goalNotifiedToday).apply();
        }
    }
    private void checkYesterdayLimits(HashMap<String, Long> todayUsageMap) {
        Log.d("NotificationCheck", "--- Starting 'More Than Yesterday' check ---");
        if (!isCacheLoaded || yesterdayUsageCache.isEmpty()) {
            Log.w("NotificationCheck", "Check skipped: Yesterday's usage cache is not loaded.");
            return;
        }

        String todayDate = getTodayDateString();
        Set<String> alreadyNotified = new HashSet<>(sharedPrefs.getStringSet("yesterday_notified_" + todayDate, new HashSet<>()));
        Log.d("NotificationCheck", "Apps already notified today: " + alreadyNotified);

        Map<String, String> appsToNotify = new HashMap<>();
        PackageManager pm = getPackageManager();

        for (Map.Entry<String, Long> entry : todayUsageMap.entrySet()) {
            String packageName = entry.getKey();
            if (UsageStatsHelper.isSystemApp(pm, packageName) || alreadyNotified.contains(packageName)) {
                continue;
            }

            long todayUsage = entry.getValue();
            long yesterdayUsage = yesterdayUsageCache.getOrDefault(packageName, 0L);

            // Detailed log for every potential candidate app
            // Log.d("NotificationCheck", "Checking [" + packageName + "]: Today=" + UsageStatsHelper.formatTime(todayUsage) + ", Yesterday=" + UsageStatsHelper.formatTime(yesterdayUsage));

            boolean needsNotification = todayUsage > yesterdayUsage &&
                    todayUsage > TimeUnit.MINUTES.toMillis(5) && // Must be used for a meaningful time today
                    !alreadyNotified.contains(packageName) && // To avoid spamming the notification
                    yesterdayUsage > TimeUnit.MINUTES.toMillis(1); // Must have been used for a meaningful time yesterday

            if (needsNotification) {
                Log.d("NotificationCheck", ">>> CONDITION MET for " + packageName + "! Queueing for notification.");
                String content = "Today: " + UsageStatsHelper.formatTime(todayUsage) + " | Yesterday: " + UsageStatsHelper.formatTime(yesterdayUsage);
                appsToNotify.put(packageName, content);
            }
        }

        if (!appsToNotify.isEmpty()) {
            Log.d("NotificationCheck", "Found " + appsToNotify.size() + " apps to notify: " + appsToNotify.keySet());
            for (Map.Entry<String, String> notificationEntry : appsToNotify.entrySet()) {
                NotificationHelper.sendNotification(this, "Used More Than Yesterday", notificationEntry.getValue(), notificationEntry.getKey());
                alreadyNotified.add(notificationEntry.getKey());
            }
            sharedPrefs.edit().putStringSet("yesterday_notified_" + todayDate, alreadyNotified).apply();
            Log.d("NotificationCheck", "Saved updated notified list: " + alreadyNotified);
        }
        Log.d("NotificationCheck", "--- Finished 'More Than Yesterday' check ---");
    }

    private String getTodayDateString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    // This method is now responsible for building and showing the dynamic notification.
    private void updateForegroundNotification() {
        String title;
        String content;

        // 1. Determine the text based on the service's state
        if (areRemindersActive) {
            title = "Break Reminders are ON";
            content = "Monitoring your active screen time.";
        } else {
            title = "MinST is Active";
            content = "Monitoring usage for your well-being.";
        }

        // 2. Create the base notification
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MyApplication.CHANNEL_ID_SERVICE)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_notification_logo)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo_transparent))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true); // Prevents the notification from making a sound every time it updates

        // 3. Add a "Stop Reminders" action button ONLY if reminders are active
        if (areRemindersActive) {
            Intent stopRemindersIntent = new Intent(this, TrackingService.class);
            stopRemindersIntent.setAction(ACTION_STOP_REMINDERS);
            PendingIntent stopRemindersPendingIntent = PendingIntent.getService(this, 1, stopRemindersIntent, PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(R.drawable.ic_close, "Stop Reminders", stopRemindersPendingIntent);
        }

        Notification notification = builder.build();

        // 4. Show the updated notification
        ServiceCompat.startForeground(this, FOREGROUND_ID, notification, getForegroundServiceTypeConstant());
    }

    // This method is now a simple fallback for the initial startForeground call
    private Notification createServiceNotification() {
        return new NotificationCompat.Builder(this, MyApplication.CHANNEL_ID_SERVICE)
                .setContentTitle("MinST")
                .setContentText("Initializing service...")
                .setSmallIcon(R.drawable.ic_notification_logo)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo_transparent))
                .build();
    }

    // Helper to get the foreground service type
    private int getForegroundServiceTypeConstant() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH;
        }
        return 0;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            unregisterReceiver(screenStateReceiver);
            handler.removeCallbacks(checkRunnable);
        }
        if (overlayView != null) {
            try { windowManager.removeView(overlayView); } catch (Exception ignored) {}
        }
        Log.d(LOG_TAG, "Unified Service Destroyed.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
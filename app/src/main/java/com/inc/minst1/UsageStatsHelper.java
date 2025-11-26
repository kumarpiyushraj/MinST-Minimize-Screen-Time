package com.inc.minst1;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;

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

public class UsageStatsHelper {

    /**
     * --- COMPLETELY REWRITTEN AND ROBUST VERSION ---
     * This is the primary method for getting usage data by processing the raw event log.
     * It correctly handles edge cases for apps active at the start/end of the time window.
     */
    public static HashMap<String, ProcessedUsageStats> getProcessedUsageStats(Context context, long start, long end) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return new HashMap<>();

        UsageEvents usageEvents = usm.queryEvents(start, end);
        if (usageEvents == null) return new HashMap<>();

        HashMap<String, ProcessedUsageStats> usageStatsMap = new HashMap<>();
        HashMap<String, Long> appLastForegroundTime = new HashMap<>();
        final long DEBOUNCE_MILLIS = 30 * 1000; // 30 seconds for a new launch

        // Find the app that was in the foreground right before our time window started.
        // This is crucial for accurately calculating the first app's usage.
        String initialApp = getAppInForeground(context, start - 1);

        UsageEvents.Event event = new UsageEvents.Event();
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            String packageName = event.getPackageName();

            if (packageName == null) continue;

            usageStatsMap.putIfAbsent(packageName, new ProcessedUsageStats(packageName));
            ProcessedUsageStats stats = usageStatsMap.get(packageName);
            if (stats == null) continue;

            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                // Update last used time
                if (event.getTimeStamp() > stats.lastTimeUsed) {
                    stats.lastTimeUsed = event.getTimeStamp();
                }

                // Increment launch count with debounce
                if (event.getTimeStamp() - stats.lastLaunchTime > DEBOUNCE_MILLIS) {
                    stats.launchCount++;
                }
                stats.lastLaunchTime = event.getTimeStamp();

                // Store this app's foreground entry time
                appLastForegroundTime.put(packageName, event.getTimeStamp());

                // If another app was in the foreground, calculate its duration and stop its timer.
                // We use `initialApp` for the very first event in the log.
                if (initialApp != null && !initialApp.equals(packageName)) {
                    Long lastTime = appLastForegroundTime.get(initialApp);
                    if (lastTime != null) {
                        long duration = event.getTimeStamp() - lastTime;
                        ProcessedUsageStats lastStats = usageStatsMap.get(initialApp);
                        if (lastStats != null && duration > 0) {
                            lastStats.totalTimeInForeground += duration;
                        }
                        appLastForegroundTime.remove(initialApp); // Stop the timer for the old app
                    }
                }
                initialApp = packageName; // This app is now the one in the foreground

            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                Long startTime = appLastForegroundTime.get(packageName);
                if (startTime != null) {
                    long duration = event.getTimeStamp() - startTime;
                    if (duration > 0) {
                        stats.totalTimeInForeground += duration;
                    }
                    appLastForegroundTime.remove(packageName); // Stop the timer
                    initialApp = null; // No app is in the foreground now
                }
            }
        }

        // After the loop, if an app was still in the foreground at the end of the time window,
        // calculate its remaining time.
        if (initialApp != null) {
            Long startTime = appLastForegroundTime.get(initialApp);
            if (startTime != null) {
                ProcessedUsageStats lastStats = usageStatsMap.get(initialApp);
                if (lastStats != null) {
                    long duration = end - startTime;
                    if (duration > 0) {
                        lastStats.totalTimeInForeground += duration;
                    }
                }
            }
        }
        return usageStatsMap;
    }

    // Helper method to find what app was in the foreground at a specific moment.
    private static String getAppInForeground(Context context, long time) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return null;
        String foregroundApp = null;
        UsageEvents.Event event = new UsageEvents.Event();
        UsageEvents usageEvents = usm.queryEvents(time - 10000, time); // Check 10 seconds before
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundApp = event.getPackageName();
            }
        }
        return foregroundApp;
    }

    /**
     * This method correctly wraps the main processing method. No changes needed here.
     */
    public static HashMap<String, Long> getAggregatedUsageMap(Context context, long start, long end) {
        HashMap<String, ProcessedUsageStats> processedStats = getProcessedUsageStats(context, start, end);
        HashMap<String, Long> appUsageMap = new HashMap<>();
        for (Map.Entry<String, ProcessedUsageStats> entry : processedStats.entrySet()) {
            appUsageMap.put(entry.getKey(), entry.getValue().totalTimeInForeground);
        }
        return appUsageMap;
    }

    /**
     * Interface for the getLastUsedTimestamps callback.
     */
    public interface TimestampsCallback {
        void onTimestampsReady(Map<String, Long> lastTimeUsedMap);
    }

    /**
     * A lightweight, asynchronous method to get only the last time each app was used.
     * Used by UsageStatsActivity to efficiently sort the list.
     */
    public static void getLastUsedTimestamps(Context context, long start, long end, TimestampsCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) {
                handler.post(() -> callback.onTimestampsReady(new HashMap<>()));
                return;
            }

            UsageEvents usageEvents = usm.queryEvents(start, end);
            if (usageEvents == null) {
                handler.post(() -> callback.onTimestampsReady(new HashMap<>()));
                return;
            }

            HashMap<String, Long> lastTimeUsedMap = new HashMap<>();
            UsageEvents.Event event = new UsageEvents.Event();
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);
                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastTimeUsedMap.put(event.getPackageName(), event.getTimeStamp());
                }
            }
            handler.post(() -> callback.onTimestampsReady(lastTimeUsedMap));
        });
    }

    /**
     * Gets the start of the current day in milliseconds.
     */
    public static long getStartOfDayInMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * Gets the start of a specific day in milliseconds.
     */
    public static long getStartOfDayInMillis(Calendar calendar) {
        Calendar cal = (Calendar) calendar.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Gets the end of a specific day in milliseconds.
     */
    public static long getEndOfDayInMillis(Calendar calendar) {
        Calendar cal = (Calendar) calendar.clone();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    /**
     * Formats milliseconds to a human-readable "Xh Ym Zs" string.
     */
    public static String formatTime(long millis) {
        if (millis < 0) return "--";
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);

        if (hours > 0) {
            return String.format(Locale.US, "%d hr %d min", hours, (minutes % 60));
        } else if (minutes > 0) {
            return String.format(Locale.US, "%d min %d sec", minutes, (seconds % 60));
        } else {
            return String.format(Locale.US, "%d sec", seconds);
        }
    }

    /**
     * Formats a timestamp into a relative string like "2 hours ago".
     */
    public static String formatTimeAgo(long timestamp) {
        if (timestamp == 0) return "Not used recently";
        return DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString();
    }

    // --- THIS IS THE NEW, HYBRID, AND FINAL VERSION ---
    public static String getCurrentForegroundPackage(Context context) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return null;

        String foregroundApp = null;
        long now = System.currentTimeMillis();

        // --- Step 1: Try the fast and accurate event query first for recent switches. ---
        // We query events in the last 30 seconds.
        UsageEvents usageEvents = usm.queryEvents(now - TimeUnit.SECONDS.toMillis(30), now);
        if (usageEvents != null) {
            UsageEvents.Event event = new UsageEvents.Event();
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);
                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    foregroundApp = event.getPackageName();
                }
            }
        }

        // --- Step 2: If no recent events, use the reliable fallback method. ---
        // This correctly identifies the app you've been on for a long time.
        if (foregroundApp == null) {
            List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - TimeUnit.MINUTES.toMillis(1), now);
            if (stats != null && !stats.isEmpty()) {
                UsageStats recent = null;
                for (UsageStats usageStats : stats) {
                    if (recent == null || usageStats.getLastTimeUsed() > recent.getLastTimeUsed()) {
                        recent = usageStats;
                    }
                }
                if (recent != null) {
                    foregroundApp = recent.getPackageName();
                }
            }
        }
        return foregroundApp;
    }

    /**
     * Reliably checks if an app is a system app by checking its ApplicationInfo flags.
     * @param pm PackageManager instance
     * @param packageName The package name of the app to check
     * @return true if the app is a system app, false otherwise
     */
    public static boolean isSystemApp(PackageManager pm, String packageName) {
        try {
            // Always exclude your own app
            if ("com.inc.minst1".equals(packageName)) {
                return true; // skip self
            }

            // Core Android apps
            if ("android".equals(packageName) || packageName.startsWith("com.android.")) {
                return true;
            }

            // Whitelist user-facing preinstalled apps
            Set<String> whitelist = new HashSet<>();
            whitelist.add("com.google.android.youtube");
            whitelist.add("com.google.android.gm");
            whitelist.add("com.google.android.apps.maps");
            whitelist.add("com.google.android.apps.photos");
            whitelist.add("com.google.android.apps.messaging");
            if (whitelist.contains(packageName)) {
                return false; // treat as normal app
            }

            // Only now check system flags
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            boolean isUpdatedSystemApp = (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;

            return isSystem && !isUpdatedSystemApp;

        } catch (PackageManager.NameNotFoundException e) {
            return false; // treat unknown as normal
        }
    }

}
//package com.inc.minst1;
//
//import android.Manifest;
//import android.app.ActivityManager;
//import android.app.AlertDialog;
//import android.app.AppOpsManager;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.content.pm.PackageManager;
//import android.graphics.Color;
//import android.graphics.LinearGradient;
//import android.graphics.Shader;
//import android.net.Uri;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.PowerManager;
//import android.os.Process;
//import android.provider.Settings;
//import android.text.TextPaint;
//import android.util.Log;
//import android.view.View;
//import android.view.animation.AnimationUtils;
//import android.widget.Button;
//import android.widget.ImageButton;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.result.ActivityResultLauncher;
//import androidx.activity.result.contract.ActivityResultContracts;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.constraintlayout.widget.Group;
//import androidx.core.content.ContextCompat;
//import androidx.work.Constraints;
//import androidx.work.Data;
//import androidx.work.ExistingPeriodicWorkPolicy;
//import androidx.work.OneTimeWorkRequest;
//import androidx.work.PeriodicWorkRequest;
//import androidx.work.WorkManager;
//
//import java.text.SimpleDateFormat;
//import java.util.Calendar;
//import java.util.Locale;
//import java.util.concurrent.TimeUnit;
//
//public class MainActivity extends AppCompatActivity {
//
//    private static final String PREF_KEY_SETUP_COMPLETE = "isSetupComplete";
//
//    // UI Views
//    ImageButton btnToggleService;
//    ImageView ivRotatingRing, ivIconCenter;
//    TextView tvStatus, tvTitle;
//    Button btnUsage, btnSettings, btnInfo;
//    private SharedPreferences appPrefs;
//
//    // Permission UI Views
//    private LinearLayout permissionsLayout;
//    private Button btnGrantPermissions;
//    private Group mainUiGroup;
//    private View alarmsPermissionRow; // To hide the obsolete row
//
//    private final ActivityResultLauncher<String> requestPermissionLauncher =
//            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
//                // onResume will handle the re-check.
//            });
//
//    private final ActivityResultLauncher<Intent> startSettingsActivityLauncher =
//            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
//                // onResume will handle the re-check.
//            });
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        appPrefs = getSharedPreferences("AppPreferences", MODE_PRIVATE);
//        initializeViews();
//        setupListeners();
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        checkAndRequestNextPermission();
//    }
//
//    private void initializeViews() {
//        btnToggleService = findViewById(R.id.btnToggleService);
//        ivRotatingRing = findViewById(R.id.ivRotatingRing);
//        ivIconCenter = findViewById(R.id.ivIconCenter);
//        tvStatus = findViewById(R.id.tvStatus);
//        btnUsage = findViewById(R.id.btnUsage);
//        btnSettings = findViewById(R.id.btnSettings);
//        tvTitle = findViewById(R.id.tvTitle);
//        btnInfo = findViewById(R.id.btnInfo);
//        mainUiGroup = findViewById(R.id.mainUiGroup);
//        permissionsLayout = findViewById(R.id.permissionsLayout);
//        btnGrantPermissions = findViewById(R.id.btnGrantPermissions);
//
//        applyGradientToTitle();
//    }
//
//    private void setupListeners() {
//        btnGrantPermissions.setOnClickListener(v -> checkAndRequestNextPermission());
//        btnToggleService.setOnClickListener(v -> toggleOverlayService());
//        btnUsage.setOnClickListener(v -> startActivity(new Intent(this, UsageStatsActivity.class)));
//        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
//        btnInfo.setOnClickListener(v -> showMainInfoDialog());
//    }
//
//    // THIS METHOD IS NOW SIMPLIFIED
//    private void checkAndRequestNextPermission() {
//        updatePermissionChecklistUi();
//
//        if (!hasUsageStatsPermission()) {
//            btnGrantPermissions.setOnClickListener(v -> requestUsageStatsPermission());
//        } else if (!Settings.canDrawOverlays(this)) {
//            btnGrantPermissions.setOnClickListener(v -> requestOverlayPermission());
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//            btnGrantPermissions.setOnClickListener(v -> requestNotificationPermission());
//        } else {
//            // All necessary permissions are granted.
//            showMainUI(true);
//            // Now, perform the one-time setup or ensure services are running.
//            performInitialSetup();
//            checkBatteryOptimizationPermission();
//        }
//    }
//
//    // THIS IS THE NEW, SMARTER METHOD
//    private void checkBatteryOptimizationPermission() {
//        boolean hasAskedBefore = appPrefs.getBoolean("has_asked_battery_exemption", false);
//        String packageName = getPackageName();
//        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//
//        // We only show this one time, after primary permissions are granted.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasAskedBefore) {
//            // Check if the app is already unrestricted. If so, we don't need to ask.
//            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
//
//                new AlertDialog.Builder(this)
//                        .setTitle("Enable Reliable Alerts (Important)")
//                        // UPDATED: The message is now a clear, step-by-step guide.
//                        .setMessage("To ensure MinST's alerts are always delivered on time, please follow these steps:\n\n1. Tap 'Open Settings' below.\n2. Find and tap on 'MinST' in the app list.\n3. Tap 'Battery'.\n4. Select 'Unrestricted'.")
//                        .setPositiveButton("Open Settings", (dialog, which) -> {
//                            // UPDATED: This is a safe, standard Intent that violates no policies.
//                            // It opens the list of all installed applications.
//                            Intent intent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
//                            startActivity(intent);
//                        })
//                        .setOnDismissListener(dialog -> {
//                            // We mark that we've asked so we don't annoy the user again.
//                            appPrefs.edit().putBoolean("has_asked_battery_exemption", true).apply();
//                        })
//                        .show();
//            }
//        }
//    }
//
//    // THIS IS THE DEFINITIVE FIX:
//    // This method handles the startup logic perfectly.
//    private void performInitialSetup() {
//        boolean isSetupComplete = appPrefs.getBoolean(PREF_KEY_SETUP_COMPLETE, false);
//
//        if (!isSetupComplete) {
//            Log.d("MainActivity", "--- Performing First-Time Setup ---");
//
//            // --- THIS IS THE CORRECTED LOGIC ---
//
//            // 1. Schedule all background workers FIRST.
//            //    This ensures they are queued up and ready to go.
//            scheduleDailyDataSave();
//            triggerInitialDataImport();
//
//            PeriodicWorkRequest watchdogWorkRequest =
//                    new PeriodicWorkRequest.Builder(TrackingWorker.class, 15, TimeUnit.MINUTES)
//                            .build();
//            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
//                    "TrackingWatchdog",
//                    ExistingPeriodicWorkPolicy.KEEP,
//                    watchdogWorkRequest);
//            Log.d("MainActivity", "All background workers scheduled successfully.");
//
//            // 2. Start the TrackingService LAST.
//            //    Its initial 3-minute delay will now begin *after* the workers are already queued.
//            Intent serviceIntent = new Intent(this, TrackingService.class);
//            ContextCompat.startForegroundService(this, serviceIntent);
//            Log.d("MainActivity", "TrackingService started.");
//
//            // 3. Set the flag so this block never runs again.
//            appPrefs.edit().putBoolean(PREF_KEY_SETUP_COMPLETE, true).apply();
//
//        } else {
//            Log.d("MainActivity", "Setup already complete. Ensuring TrackingService is running.");
//            if (!isServiceRunning(TrackingService.class)) {
//                Intent serviceIntent = new Intent(this, TrackingService.class);
//                ContextCompat.startForegroundService(this, serviceIntent);
//                Log.d("MainActivity", "TrackingService was not running, now restarted.");
//            }
//        }
//    }
//
//    // THIS METHOD IS NOW SIMPLIFIED
//    private void updatePermissionChecklistUi() {
//        showMainUI(false);
//        updateChecklistItem(findViewById(R.id.ivUsageStatus), findViewById(R.id.tvUsageStatus), hasUsageStatsPermission());
//        updateChecklistItem(findViewById(R.id.ivOverlayStatus), findViewById(R.id.tvOverlayStatus), Settings.canDrawOverlays(this));
//
//        boolean hasNotificationPerm = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
//        updateChecklistItem(findViewById(R.id.ivNotificationsStatus), findViewById(R.id.tvNotificationsStatus), hasNotificationPerm);
//
////        // Hide the alarms permission row as it's no longer needed
////        if (alarmsPermissionRow != null) {
////            alarmsPermissionRow.setVisibility(View.GONE);
////        }
//    }
//
//    private void updateChecklistItem(ImageView icon, TextView status, boolean isGranted) {
//        if (isGranted) {
//            icon.setImageResource(R.drawable.ic_check_circle);
//            icon.setColorFilter(Color.parseColor("#4CAF50")); // Green
//            status.setText("Granted");
//            status.setTextColor(Color.parseColor("#4CAF50"));
//        } else {
//            icon.setImageResource(R.drawable.ic_pending_circle);
//            icon.setColorFilter(Color.parseColor("#80FFFFFF")); // Grey
//            status.setText("Required");
//            status.setTextColor(Color.parseColor("#80FFFFFF"));
//        }
//    }
//
//    private void showMainUI(boolean show) {
//        if (show) {
//            permissionsLayout.setVisibility(View.GONE);
//            mainUiGroup.setVisibility(View.VISIBLE);
//            updateServiceStatusUI();
//        } else {
//            permissionsLayout.setVisibility(View.VISIBLE);
//            mainUiGroup.setVisibility(View.INVISIBLE);
//        }
//    }
//
//    private boolean hasUsageStatsPermission() {
//        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
//        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());
//        return mode == AppOpsManager.MODE_ALLOWED;
//    }
//
//    private void requestUsageStatsPermission() {
//        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
//    }
//
//    private void requestOverlayPermission() {
//        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
//        startSettingsActivityLauncher.launch(intent);
//    }
//
//    private void requestNotificationPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
//        }
//    }
//
//    // THIS METHOD IS NO LONGER NEEDED
////    private void requestExactAlarmPermission() {
////        // This method can be removed entirely.
////    }
//
//    private void updateServiceStatusUI() {
//        // This seems to be for a different service, "OverlayService", so this logic can remain as is.
//        if (isServiceRunning(OverlayService.class)) {
//            tvStatus.setText("Reminders Active");
//            ivRotatingRing.setBackground(ContextCompat.getDrawable(this, R.drawable.activation_ring_on));
//            ivIconCenter.setImageResource(R.drawable.ic_check);
//            ivIconCenter.setImageTintList(ContextCompat.getColorStateList(this, android.R.color.white));
//            ivRotatingRing.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));
//        } else {
//            tvStatus.setText("Reminders Off");
//            ivRotatingRing.setBackground(ContextCompat.getDrawable(this, R.drawable.activation_ring_off));
//            ivIconCenter.setImageResource(R.drawable.ic_power);
//            ivIconCenter.setImageTintList(ContextCompat.getColorStateList(this, R.color.white_alpha_50));
//            ivRotatingRing.clearAnimation();
//        }
//    }
//
//    // The isServiceRunning method you already have is perfect for this.
//    private boolean isServiceRunning(Class<?> serviceClass) {
//        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        if (manager == null) return false;
//        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (serviceClass.getName().equals(service.service.getClassName())) return true;
//        }
//        return false;
//    }
//
//    private void toggleOverlayService() {
//        if (isServiceRunning(OverlayService.class)) {
//            stopService(new Intent(this, OverlayService.class));
//            Toast.makeText(this, "Reminders stopped", Toast.LENGTH_SHORT).show();
//        } else {
//            startService(new Intent(this, OverlayService.class));
//            Toast.makeText(this, "Reminders started", Toast.LENGTH_SHORT).show();
//        }
//        updateServiceStatusUI();
//    }
//
//    private void applyGradientToTitle() {
//        TextPaint paint = tvTitle.getPaint();
//        float width = paint.measureText(tvTitle.getText().toString());
//        int startColor = ContextCompat.getColor(this, R.color.title_gradient_silver);
//        int endColor = ContextCompat.getColor(this, R.color.title_gradient_gold);
//        Shader textShader = new LinearGradient(0, 0, width, tvTitle.getTextSize(), new int[]{startColor, endColor}, null, Shader.TileMode.CLAMP);
//        paint.setShader(textShader);
//    }
//
//    private void showMainInfoDialog() {
//        View dialogView = getLayoutInflater().inflate(R.layout.dialog_main_info, null);
//        new AlertDialog.Builder(this).setView(dialogView).setPositiveButton("Got It", null).show();
//    }
//
//    private void scheduleDailyDataSave() {
//        PeriodicWorkRequest saveRequest = new PeriodicWorkRequest.Builder(DataSaveWorker.class, 1, TimeUnit.DAYS).build();
//        WorkManager.getInstance(this).enqueueUniquePeriodicWork("DailyDataSave", ExistingPeriodicWorkPolicy.KEEP, saveRequest);
//    }
//
//    private void triggerInitialDataImport() {
//        boolean initialDataImported = appPrefs.getBoolean("initial_data_imported", false);
//        if (!initialDataImported) {
//            Log.d("MainActivity", "Triggering initial 7-day data import.");
//            Calendar calendar = Calendar.getInstance();
//            // Changed the loop to 6 because the 7th previous day's record is not displayed in the chart, which was causing calculation errors.
//            calendar.add(Calendar.DAY_OF_YEAR, -1);
//            for (int i = 0; i < 6; i++) {
//                String dateToSave = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
//                long startOfDay = UsageStatsHelper.getStartOfDayInMillis(calendar);
//                long endOfDay = UsageStatsHelper.getEndOfDayInMillis(calendar);
//                Data inputData = new Data.Builder()
//                        .putLong(DataSaveWorker.INPUT_START_DATE_MILLIS, startOfDay)
//                        .putLong(DataSaveWorker.INPUT_END_DATE_MILLIS, endOfDay)
//                        .putString(DataSaveWorker.INPUT_DATE_STRING, dateToSave)
//                        .build();
//                OneTimeWorkRequest oneTimeSaveRequest = new OneTimeWorkRequest.Builder(DataSaveWorker.class)
//                        .setInputData(inputData)
//                        .setConstraints(new Constraints.Builder().setRequiresBatteryNotLow(true).build())
//                        .setInitialDelay(2, TimeUnit.MINUTES)
//                        .addTag("initial_data_import_" + dateToSave)
//                        .build();
//                WorkManager.getInstance(this).enqueue(oneTimeSaveRequest);
//                calendar.add(Calendar.DAY_OF_YEAR, -1);
//            }
//            appPrefs.edit().putBoolean("initial_data_imported", true).apply();
//        }
//    }
//}

package com.inc.minst1;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Process;
import android.provider.Settings;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final String PREF_KEY_SETUP_COMPLETE = "isSetupComplete";

    // UI Views
    ImageButton btnToggleService;
    ImageView ivRotatingRing, ivIconCenter;
    TextView tvStatus, tvTitle;
    Button btnUsage, btnSettings, btnInfo;
    private SharedPreferences appPrefs;

    // Permission UI Views
    private LinearLayout permissionsLayout;
    private Button btnGrantPermissions;
    private Group mainUiGroup;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // onResume will handle the re-check.
            });

    private final ActivityResultLauncher<Intent> startSettingsActivityLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // onResume will handle the re-check.
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appPrefs = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        initializeViews();
        setupListeners();
        // --- ADD THIS LINE to start listening for messages ---
        LocalBroadcastManager.getInstance(this).registerReceiver(statusUpdateReceiver, new IntentFilter(TrackingService.ACTION_STATE_CHANGED));
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndRequestNextPermission();
        // Always update the UI status when returning to the app
        updateServiceStatusUI();
    }

    private void initializeViews() {
        btnToggleService = findViewById(R.id.btnToggleService);
        ivRotatingRing = findViewById(R.id.ivRotatingRing);
        ivIconCenter = findViewById(R.id.ivIconCenter);
        tvStatus = findViewById(R.id.tvStatus);
        btnUsage = findViewById(R.id.btnUsage);
        btnSettings = findViewById(R.id.btnSettings);
        tvTitle = findViewById(R.id.tvTitle);
        btnInfo = findViewById(R.id.btnInfo);
        mainUiGroup = findViewById(R.id.mainUiGroup);
        permissionsLayout = findViewById(R.id.permissionsLayout);
        btnGrantPermissions = findViewById(R.id.btnGrantPermissions);

        applyGradientToTitle();
    }

    private void setupListeners() {
        btnGrantPermissions.setOnClickListener(v -> checkAndRequestNextPermission());
        btnToggleService.setOnClickListener(v -> toggleReminderMode());
        btnUsage.setOnClickListener(v -> startActivity(new Intent(this, UsageStatsActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        btnInfo.setOnClickListener(v -> showMainInfoDialog());
    }

    // THIS METHOD IS NOW SIMPLIFIED
    private void checkAndRequestNextPermission() {
        updatePermissionChecklistUi();

        if (!hasUsageStatsPermission()) {
            btnGrantPermissions.setOnClickListener(v -> requestUsageStatsPermission());
        } else if (!Settings.canDrawOverlays(this)) {
            btnGrantPermissions.setOnClickListener(v -> requestOverlayPermission());
        } else if (!hasActivityRecognitionPermission()) { // The NEW check
        btnGrantPermissions.setOnClickListener(v -> requestActivityRecognitionPermission());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            btnGrantPermissions.setOnClickListener(v -> requestNotificationPermission());
        } else {
            // All necessary permissions are granted.
            showMainUI(true);
            // Now, perform the one-time setup or ensure services are running.
            performInitialSetup();
            checkBatteryOptimizationPermission();
        }
    }

    // THIS IS THE NEW, SMARTER METHOD
    private void checkBatteryOptimizationPermission() {
        boolean hasAskedBefore = appPrefs.getBoolean("has_asked_battery_exemption", false);
        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        // We only show this one time, after primary permissions are granted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasAskedBefore) {
            // Check if the app is already unrestricted. If so, we don't need to ask.
            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {

                new AlertDialog.Builder(this)
                        .setTitle("Enable Reliable Alerts (Important)")
                        // UPDATED: The message is now a clear, step-by-step guide.
                        .setMessage("To ensure MinST's alerts are always delivered on time, please follow these steps:\n\n1. Tap 'Open Settings' below.\n2. Find and tap on 'MinST' in the app list.\n3. Tap 'Battery'.\n4. Select 'Unrestricted'.")
                        .setPositiveButton("Open Settings", (dialog, which) -> {
                            // UPDATED: This is a safe, standard Intent that violates no policies.
                            // It opens the list of all installed applications.
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                            startActivity(intent);
                        })
                        .setOnDismissListener(dialog -> {
                            // We mark that we've asked so we don't annoy the user again.
                            appPrefs.edit().putBoolean("has_asked_battery_exemption", true).apply();
                        })
                        .show();
            }
        }
    }

    // THIS IS THE DEFINITIVE FIX:
    // This method handles the startup logic perfectly.
    private void performInitialSetup() {
        boolean isSetupComplete = appPrefs.getBoolean(PREF_KEY_SETUP_COMPLETE, false);

        if (!isSetupComplete) {
            Log.d("MainActivity", "--- Performing First-Time Setup ---");

            // --- THIS IS THE CORRECTED LOGIC ---

            // 1. Schedule all background workers FIRST.
            //    This ensures they are queued up and ready to go.
            scheduleDailyDataSave();
            triggerInitialDataImport();

            PeriodicWorkRequest watchdogWorkRequest =
                    new PeriodicWorkRequest.Builder(TrackingWorker.class, 15, TimeUnit.MINUTES)
                            .build();
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "TrackingWatchdog",
                    ExistingPeriodicWorkPolicy.KEEP,
                    watchdogWorkRequest);
            Log.d("MainActivity", "All background workers scheduled successfully.");

            // 2. Start the TrackingService LAST.
            // Start the service ONCE in its default background state
            Intent serviceIntent = new Intent(this, TrackingService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
            Log.d("MainActivity", "Unified TrackingService started for the first time.");

            appPrefs.edit().putBoolean(PREF_KEY_SETUP_COMPLETE, true).apply();

        } else {
            Log.d("MainActivity", "Setup already complete. Ensuring TrackingService is running.");
            if (!isServiceRunning(TrackingService.class)) {
                // This ensures the service restarts if it was killed by the system
                Intent serviceIntent = new Intent(this, TrackingService.class);
                ContextCompat.startForegroundService(this, serviceIntent);
                Log.d("MainActivity", "TrackingService was not running, now restarted.");
            }
            else{
                Log.d("MainActivity", "TrackingService is already running.");
            }
        }
    }

    // THIS METHOD IS NOW SIMPLIFIED
    private void updatePermissionChecklistUi() {
        showMainUI(false);
        updateChecklistItem(findViewById(R.id.ivUsageStatus), findViewById(R.id.tvUsageStatus), hasUsageStatsPermission());
        updateChecklistItem(findViewById(R.id.ivOverlayStatus), findViewById(R.id.tvOverlayStatus), Settings.canDrawOverlays(this));
        updateChecklistItem(findViewById(R.id.ivActivityStatus), findViewById(R.id.tvActivityStatus), hasActivityRecognitionPermission());

        boolean hasNotificationPerm = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        updateChecklistItem(findViewById(R.id.ivNotificationsStatus), findViewById(R.id.tvNotificationsStatus), hasNotificationPerm);
    }

    private void updateChecklistItem(ImageView icon, TextView status, boolean isGranted) {
        if (isGranted) {
            icon.setImageResource(R.drawable.ic_check_circle);
            icon.setColorFilter(Color.parseColor("#4CAF50")); // Green
            status.setText("Granted");
            status.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            icon.setImageResource(R.drawable.ic_pending_circle);
            icon.setColorFilter(Color.parseColor("#80FFFFFF")); // Grey
            status.setText("Required");
            status.setTextColor(Color.parseColor("#80FFFFFF"));
        }
    }

    private void showMainUI(boolean show) {
        if (show) {
            permissionsLayout.setVisibility(View.GONE);
            mainUiGroup.setVisibility(View.VISIBLE);
            updateServiceStatusUI();
        } else {
            permissionsLayout.setVisibility(View.VISIBLE);
            mainUiGroup.setVisibility(View.INVISIBLE);
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void requestUsageStatsPermission() {
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    // --- ADD these two new helper methods ---
    private boolean hasActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Permission doesn't exist on older versions, so it's "granted"
    }

    private void requestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION);
        }
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        startSettingsActivityLauncher.launch(intent);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void updateServiceStatusUI() {
        // The UI now reflects the saved state of the reminder feature, not if the service is running
        boolean remindersAreActive = appPrefs.getBoolean("isReminderActive", false);

        if (remindersAreActive) {
            tvStatus.setText("Break Reminders Active");
            ivRotatingRing.setBackground(ContextCompat.getDrawable(this, R.drawable.activation_ring_on));
            ivIconCenter.setImageResource(R.drawable.ic_check);
            ivIconCenter.setImageTintList(ContextCompat.getColorStateList(this, android.R.color.white));
            ivRotatingRing.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));
        } else {
            tvStatus.setText("Break Reminders Off");
            ivRotatingRing.setBackground(ContextCompat.getDrawable(this, R.drawable.activation_ring_off));
            ivIconCenter.setImageResource(R.drawable.ic_power);
            ivIconCenter.setImageTintList(ContextCompat.getColorStateList(this, R.color.white_alpha_50));
            ivRotatingRing.clearAnimation();
        }
    }

    // The isServiceRunning method you already have is perfect for this.
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return false;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) return true;
        }
        return false;
    }

    // This is the new, safe method to toggle the reminder feature
    private void toggleReminderMode() {
        // 1. Read the current state to determine the NEW state.
        boolean remindersAreCurrentlyActive = appPrefs.getBoolean("isReminderActive", false);
        boolean shouldBeActive = !remindersAreCurrentlyActive;

        // 2. Instantly save the new state.
        appPrefs.edit().putBoolean("isReminderActive", shouldBeActive).apply();

        // 3. Instantly update the UI to provide immediate feedback.
        updateServiceStatusUI();

        // 4. Now, create and send the command to the service to do the actual work.
        Intent intent = new Intent(this, TrackingService.class);
        if (shouldBeActive) {
            intent.setAction(TrackingService.ACTION_START_REMINDERS);
            Toast.makeText(this, "Break reminders started", Toast.LENGTH_SHORT).show();
        } else {
            intent.setAction(TrackingService.ACTION_STOP_REMINDERS);
            Toast.makeText(this, "Break reminders stopped", Toast.LENGTH_SHORT).show();
        }

        ContextCompat.startForegroundService(this, intent);
    }

    private void applyGradientToTitle() {
        TextPaint paint = tvTitle.getPaint();
        float width = paint.measureText(tvTitle.getText().toString());
        int startColor = ContextCompat.getColor(this, R.color.title_gradient_silver);
        int endColor = ContextCompat.getColor(this, R.color.title_gradient_gold);
        Shader textShader = new LinearGradient(0, 0, width, tvTitle.getTextSize(), new int[]{startColor, endColor}, null, Shader.TileMode.CLAMP);
        paint.setShader(textShader);
    }

    private void showMainInfoDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_main_info, null);
        new AlertDialog.Builder(this).setView(dialogView).setPositiveButton("Got It", null).show();
    }

    private void scheduleDailyDataSave() {
        PeriodicWorkRequest saveRequest = new PeriodicWorkRequest.Builder(DataSaveWorker.class, 1, TimeUnit.DAYS).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("DailyDataSave", ExistingPeriodicWorkPolicy.KEEP, saveRequest);
    }

    private void triggerInitialDataImport() {
        boolean initialDataImported = appPrefs.getBoolean("initial_data_imported", false);
        if (!initialDataImported) {
            Log.d("MainActivity", "Triggering initial 7-day data import.");
            Calendar calendar = Calendar.getInstance();
            // Changed the loop to 6 because the 7th previous day's record is not displayed in the chart, which was causing calculation errors.
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            for (int i = 0; i < 6; i++) {
                String dateToSave = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());
                long startOfDay = UsageStatsHelper.getStartOfDayInMillis(calendar);
                long endOfDay = UsageStatsHelper.getEndOfDayInMillis(calendar);
                Data inputData = new Data.Builder()
                        .putLong(DataSaveWorker.INPUT_START_DATE_MILLIS, startOfDay)
                        .putLong(DataSaveWorker.INPUT_END_DATE_MILLIS, endOfDay)
                        .putString(DataSaveWorker.INPUT_DATE_STRING, dateToSave)
                        .build();
                OneTimeWorkRequest oneTimeSaveRequest = new OneTimeWorkRequest.Builder(DataSaveWorker.class)
                        .setInputData(inputData)
                        .setConstraints(new Constraints.Builder().setRequiresBatteryNotLow(true).build())
                        .setInitialDelay(2, TimeUnit.MINUTES)
                        .addTag("initial_data_import_" + dateToSave)
                        .build();
                WorkManager.getInstance(this).enqueue(oneTimeSaveRequest);
                calendar.add(Calendar.DAY_OF_YEAR, -1);
            }
            appPrefs.edit().putBoolean("initial_data_imported", true).apply();
        }
    }

    // --- ADD THIS NEW RECEIVER ---
    private final BroadcastReceiver statusUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("MainActivity", "Received status update broadcast. Refreshing UI.");
            // When we receive the message, just update the UI.
            updateServiceStatusUI();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // --- ADD THIS LINE to stop listening to prevent memory leaks ---
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusUpdateReceiver);
    }
}
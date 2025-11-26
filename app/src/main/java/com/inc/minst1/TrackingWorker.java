package com.inc.minst1;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class TrackingWorker extends Worker {
    private static final String LOG_TAG_WORKER = "TrackingWorker";
    private final Context context;

    public TrackingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(LOG_TAG_WORKER, "Watchdog worker running...");
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (pm != null && pm.isInteractive()) {
            if (!isServiceRunning(TrackingService.class)) {
                Log.w(LOG_TAG_WORKER, "Service not running with screen on! Attempting to restart...");
                // THIS IS THE FIX: Wrap the call in a try-catch block
                try {
                    Intent serviceIntent = new Intent(context, TrackingService.class);
                    context.startForegroundService(serviceIntent);
                } catch (Exception e) {
                    Log.e(LOG_TAG_WORKER, "Failed to start service from background. OS restrictions likely in place.", e);
                }
            } else {
                Log.d(LOG_TAG_WORKER, "Service is already running correctly.");
            }
        } else {
            Log.d(LOG_TAG_WORKER, "Screen is OFF. No action needed.");
        }
        return Result.success();
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return false;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
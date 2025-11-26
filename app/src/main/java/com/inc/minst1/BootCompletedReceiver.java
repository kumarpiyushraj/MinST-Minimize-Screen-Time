package com.inc.minst1;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (!isServiceRunning(context, TrackingService.class)) {
                Intent serviceIntent = new Intent(context, TrackingService.class);
                context.startForegroundService(serviceIntent);
                Log.d("BootCompletedReceiver", "TrackingService started after Boot Complete.");
            } else {
                Log.d("BootCompletedReceiver", "Service already running, skipping start.");
            }
        }
    }

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
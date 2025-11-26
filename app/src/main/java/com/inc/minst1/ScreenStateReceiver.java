package com.inc.minst1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.content.ContextCompat;

public class ScreenStateReceiver extends BroadcastReceiver {
    private static final String TAG = "ScreenStateReceiver";
    // A static variable to keep track of the last known state.
    private static boolean isScreenOn = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || context == null || intent.getAction() == null) return;

        String action = intent.getAction();

        switch (action) {
            case Intent.ACTION_USER_PRESENT:
                // Only act if the state has changed from OFF to ON.
                if (!isScreenOn) {
                    Log.d(TAG, "User UNLOCKED device. Starting service checks.");
                    isScreenOn = true;
                    sendCommandToService(context, TrackingService.ACTION_START_CHECKS);
                }
                break;
            case Intent.ACTION_SCREEN_OFF:
                // Only act if the state has changed from ON to OFF.
                if (isScreenOn) {
                    Log.d(TAG, "Screen OFF. Stopping service checks.");
                    isScreenOn = false;
                    sendCommandToService(context, TrackingService.ACTION_STOP_CHECKS);
                }
                break;
        }
    }

    private void sendCommandToService(Context context, String command) {
        Intent serviceIntent = new Intent(context, TrackingService.class);
        serviceIntent.setAction(command);
        try {
            ContextCompat.startForegroundService(context, serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send command to service.", e);
        }
    }
}
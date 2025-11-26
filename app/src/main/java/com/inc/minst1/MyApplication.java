package com.inc.minst1;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class MyApplication extends Application {
    public static final String CHANNEL_ID_SERVICE = "TrackingServiceChannel";
    public static final String CHANNEL_ID_ALERTS = "UsageAlertsChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        // Its ONLY job is to create channels on the first launch.
        // It does NOT start any services or workers.
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID_SERVICE, "Tracking Service", NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(serviceChannel);

            NotificationChannel limitChannel = new NotificationChannel(
                    CHANNEL_ID_ALERTS, "Usage Alerts", NotificationManager.IMPORTANCE_HIGH);
            limitChannel.enableVibration(true);
            manager.createNotificationChannel(limitChannel);
        }
    }
}
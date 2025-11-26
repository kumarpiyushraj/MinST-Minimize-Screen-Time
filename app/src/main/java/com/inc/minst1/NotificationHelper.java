package com.inc.minst1;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;

public class NotificationHelper {

    public static void sendNotification(Context context, String title, String content, String packageName) {
        PackageManager pm = context.getPackageManager();
        String appName;
        try {
            appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            appName = packageName;
        }

        Intent intent = new Intent(context, AppDetailActivity.class);
        intent.putExtra("PACKAGE_NAME", packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int requestCode = (title + packageName).hashCode();
        PendingIntent pendingIntent = PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // --- THIS IS THE FIX ---
        // We now set the app's name as the main title for a cleaner look.
        // The original title becomes the first line of the content.
        String fullContent = title + "\n" + content;

        Notification notification = new NotificationCompat.Builder(context, MyApplication.CHANNEL_ID_ALERTS)
                .setContentTitle(appName) // Set the app name as the main title
                .setContentText(fullContent) // The content now includes the event type
                .setStyle(new NotificationCompat.BigTextStyle().bigText(fullContent)) // Ensures the full text is visible
                .setSmallIcon(R.drawable.ic_warning)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
        // --- End of Fix ---

        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        notificationManager.notify(requestCode, notification);
    }
}

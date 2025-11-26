package com.inc.minst1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.HashSet;
import java.util.Set;

public class Prefs {
    private static final String PREF = "minst_prefs";
    private static final String KEY_INTERVAL = "interval";
    private static final String KEY_SNOOZE = "snooze";
    private static final String KEY_AUTOSTART = "autostart";

    public static long defaultInterval(){ return 1 * 60 * 1000L; }
    public static long defaultSnooze(){ return 1 * 60 * 1000L; }

    public static void setInterval(Context c, long ms){
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putLong(KEY_INTERVAL, ms).apply();
    }
    public static long getInterval(Context c){
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(KEY_INTERVAL, defaultInterval());
    }

    public static void setSnooze(Context c, long ms){
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putLong(KEY_SNOOZE, ms).apply();
    }
    public static long getSnooze(Context c){
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(KEY_SNOOZE, defaultSnooze());
    }

    public static String readable(Context c, long ms){
        return UsageStatsHelper.formatTime(ms);
    }

    public static void setAutoStart(Context c, boolean val){
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(KEY_AUTOSTART, val).apply();
    }
    public static boolean getAutoStart(Context c){
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_AUTOSTART, false);
    }

    // NEW: A helper method to get apps that should be ignored by the overlay
    public static Set<String> getIgnoredPackages(Context context) {
        Set<String> ignored = new HashSet<>();
        // 1. Ignore our own app
        ignored.add(context.getPackageName());

        // 2. Find and ignore the default home screen (launcher) app
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null && resolveInfo.activityInfo != null) {
            ignored.add(resolveInfo.activityInfo.packageName);
            ignored.add("com.android.dreams.basic");
        }
        return ignored;
    }
}
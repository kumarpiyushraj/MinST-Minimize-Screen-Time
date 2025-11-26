package com.inc.minst1;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AppUsageCache {

    private static volatile AppUsageCache INSTANCE;
    private List<AppUsageModel> filteredList = new ArrayList<>();
    private long lastUpdatedTimestamp = 0;

    private static final long CACHE_VALIDITY_MS = TimeUnit.SECONDS.toMillis(30); // Cache is valid for 30 seconds
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private AppUsageCache() {}

    public static AppUsageCache getInstance() {
        if (INSTANCE == null) {
            synchronized (AppUsageCache.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppUsageCache();
                }
            }
        }
        return INSTANCE;
    }

    public void getFilteredList(Context context, AppListCallback callback) {
        // If the cache is fresh, return it immediately.
        if (!filteredList.isEmpty() && (System.currentTimeMillis() - lastUpdatedTimestamp < CACHE_VALIDITY_MS)) {
            handler.post(() -> callback.onAppListReady(new ArrayList<>(filteredList))); // Return a copy
            return;
        }

        // Otherwise, refresh the cache in the background.
        refreshCache(context, callback);
    }

    private void refreshCache(Context context, AppListCallback callback) {
        executor.execute(() -> {
            PackageManager pm = context.getPackageManager();
            long startOfToday = UsageStatsHelper.getStartOfDayInMillis();
            long now = System.currentTimeMillis();
            HashMap<String, Long> appUsageMap = UsageStatsHelper.getAggregatedUsageMap(context.getApplicationContext(), startOfToday, now);

            List<AppUsageModel> allAppsToday = new ArrayList<>();
            for (Map.Entry<String, Long> entry : appUsageMap.entrySet()) {
                String packageName = entry.getKey();
                long usage = entry.getValue();

                if (usage <= 500 || packageName.equals(context.getPackageName()) || pm.getLaunchIntentForPackage(packageName) == null) continue;

                try {
                    // CHANGED: Pass null for the icon, as it's no longer needed in the constructor
                    allAppsToday.add(new AppUsageModel(pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString(), usage, packageName));
                } catch (PackageManager.NameNotFoundException ignored) {}
            }

            allAppsToday.sort(Comparator.comparingLong(AppUsageModel::getMs).reversed());

            // Update the instance variables
            this.filteredList = allAppsToday;
            this.lastUpdatedTimestamp = System.currentTimeMillis();

            // Return the new list via the callback on the main thread
            handler.post(() -> callback.onAppListReady(new ArrayList<>(filteredList))); // Return a copy
        });
    }

    public interface AppListCallback {
        void onAppListReady(List<AppUsageModel> appUsageModels);
    }
}
package com.inc.minst1;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class UsageStatsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private AppUsageAdapter adapter;
    private Toolbar toolbar;
    private TextView tvTotalUsage;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadUsageData();
            handler.postDelayed(this, 5000); // Refresh every 5 seconds
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage_stats);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        recyclerView = findViewById(R.id.recyclerViewUsage);
        tvTotalUsage = findViewById(R.id.tvTotalUsage);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // The adapter click listener no longer needs to pass the list
        adapter = new AppUsageAdapter(this, new ArrayList<>(), new AppUsageAdapter.Clicks() {
            @Override
            public void onAppClick(AppUsageModel model, int pos) {
                Intent detailIntent = new Intent(UsageStatsActivity.this, AppDetailActivity.class);
                detailIntent.putExtra("PACKAGE_NAME", model.getPkg());
                startActivity(detailIntent);
            }
            @Override
            public void onMoreClick(AppUsageModel model, View anchorView) {
                Intent detailIntent = new Intent(UsageStatsActivity.this, AppDetailActivity.class);
                detailIntent.putExtra("PACKAGE_NAME", model.getPkg());
                startActivity(detailIntent);
            }
        }, null); // packageToHighlight can be null for now
        recyclerView.setAdapter(adapter);

        if (!hasUsagePermission()) {
            Toast.makeText(this, "Please grant Usage Access Permission", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }

    // THIS METHOD IS NOW MUCH SIMPLER
    private void loadUsageData() {
        // Ask the cache for the latest filtered list
        AppUsageCache.getInstance().getFilteredList(this, appUsageModels -> {
            // Calculate total time from the returned list
            long totalUsageMs = appUsageModels.stream().mapToLong(AppUsageModel::getMs).sum();

            // Get last used times to separate into "last hour" and "earlier"
            long now = System.currentTimeMillis();
            long startOfToday = UsageStatsHelper.getStartOfDayInMillis();
            long oneHourAgo = now - TimeUnit.HOURS.toMillis(1);

            // This part requires a separate, light query.
            UsageStatsHelper.getLastUsedTimestamps(this, startOfToday, now, lastTimeUsedMap -> {
                List<AppUsageModel> lastHourApps = new ArrayList<>();
                List<AppUsageModel> earlierApps = new ArrayList<>();
                for (AppUsageModel model : appUsageModels) {
                    if (lastTimeUsedMap.getOrDefault(model.getPkg(), 0L) >= oneHourAgo) {
                        lastHourApps.add(model);
                    } else {
                        earlierApps.add(model);
                    }
                }

                // The rest of the UI logic remains the same
                calculatePercentages(lastHourApps);
                calculatePercentages(earlierApps);
                // The main list is already sorted by the cache

                List<Object> combinedList = new ArrayList<>();
                if (!lastHourApps.isEmpty()) {
                    combinedList.add(new HeaderModel("Apps used in last 1 hr"));
                    combinedList.addAll(lastHourApps);
                }
                if (!lastHourApps.isEmpty() && !earlierApps.isEmpty()) {
                    combinedList.add(new DividerModel());
                }
                if (!earlierApps.isEmpty()) {
                    combinedList.add(new HeaderModel("Earlier Today"));
                    combinedList.addAll(earlierApps);
                }

                tvTotalUsage.setText(formatMillisToTotalUsage(totalUsageMs));
                adapter.updateList(combinedList);
            });
        });
    }

    private void calculatePercentages(List<AppUsageModel> appList) {
        if (appList.isEmpty()) return;
        long maxUsage = appList.get(0).getMs(); // List is already sorted
        for (AppUsageModel model : appList) {
            if (maxUsage > 0) model.setPercent((int) ((model.getMs() * 100) / maxUsage));
        }
    }

    private boolean hasUsagePermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        return appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    private String formatMillisToTotalUsage(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        return String.format(Locale.US, "%d hr %d min", hours, minutes);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish(); // Handles the back arrow click
            return true;
        } else if (itemId == R.id.action_info) {
            // Handles the new info icon click
            showInfoDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showInfoDialog() {
        // Inflate the custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_info, null);

        new AlertDialog.Builder(this)
                .setView(dialogView) // Set the custom view
                .setPositiveButton("Got It", null)
                .show();
    }
}
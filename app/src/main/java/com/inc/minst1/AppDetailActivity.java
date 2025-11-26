package com.inc.minst1;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AppDetailActivity extends AppCompatActivity {
    private String packageName;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final String LOG_TAG = "AppDetail";

    private BarChart barChart;
    private LineChart lineChart;
    private MaterialButtonToggleGroup toggleGroup;
    private MaterialButton btnAllDays;
    private InsightsAdapter insightsAdapter;
    // NEW: Variable to hold the passed list
    private List<AppUsageModel> filteredApps;

    private final List<Long> allUsageData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_detail);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("PACKAGE_NAME")) {
            packageName = intent.getStringExtra("PACKAGE_NAME");
            // NEW: Receive the list from the intent
           // filteredApps = (List<AppUsageModel>) intent.getSerializableExtra("FILTERED_APP_LIST");
        } else {
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupInitialUI();
        setupInsightsRecyclerView();

        barChart = findViewById(R.id.barChart);
        lineChart = findViewById(R.id.lineChart);
        toggleGroup = findViewById(R.id.toggleChartRange);
        btnAllDays = findViewById(R.id.btnAllDays);

        btnAllDays.setVisibility(View.GONE);

        toggleGroup.check(R.id.btn7Days);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn7Days) {
                    displayChart(7);
                } else if (checkedId == R.id.btnAllDays) {
                    displayChart(allUsageData.size());
                }
            }
        });

        setGreeting();
        loadAppDetails();
    }

    private void setupInitialUI() {
        PackageManager pm = getPackageManager();
        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        TextView appNameDetail = findViewById(R.id.tvAppNameDetail);
        ImageView ivAppIcon = findViewById(R.id.ivAppIconDetail);
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            String appName = appInfo.loadLabel(pm).toString();
            collapsingToolbar.setTitle(appName);
            appNameDetail.setText(appName);
            ivAppIcon.setImageDrawable(appInfo.loadIcon(pm));
        } catch (PackageManager.NameNotFoundException e) {
            collapsingToolbar.setTitle(packageName);
            appNameDetail.setText(packageName);
            ivAppIcon.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.sym_def_app_icon));
        }
    }

    private void setupInsightsRecyclerView() {
        RecyclerView rvInsights = findViewById(R.id.rvInsights);
        insightsAdapter = new InsightsAdapter();
        rvInsights.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvInsights.setAdapter(insightsAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        return true;
    }

    private void loadAppDetails() {
        // This starts the background thread for ALL loading work.
        executorService.execute(() -> {

            // --- PASS 1 (INSTANT DATA) ---
            // We ask the cache for the fast data first. The result will come back on the main thread.
            AppUsageCache.getInstance().getFilteredList(this, passedAppList -> {
                // This block runs on the MAIN THREAD.
                // We only do quick calculations and UI updates here.

                int rank = -1;
                long todayUsageFromList = 0;
                if (passedAppList != null && !passedAppList.isEmpty()) {
                    for (int i = 0; i < passedAppList.size(); i++) {
                        if (passedAppList.get(i).getPkg().equals(packageName)) {
                            rank = i + 1;
                            todayUsageFromList = passedAppList.get(i).getMs();
                            break;
                        }
                    }
                }
                final String finalUsageRank = (rank != -1) ? "#" + rank : "N/A";
                final int finalTotalAppsUsed = (passedAppList != null) ? passedAppList.size() : 0;

                ProcessedUsageStats todayStatsLive = UsageStatsHelper.getProcessedUsageStats(
                        this, UsageStatsHelper.getStartOfDayInMillis(), System.currentTimeMillis()
                ).get(packageName);
                if (todayStatsLive == null) todayStatsLive = new ProcessedUsageStats();

                final long finalTodayUsage = todayUsageFromList;
                final long finalAppOpens = todayStatsLive.launchCount;
                final long finalLastUsed = todayStatsLive.lastTimeUsed;

                // FIRST UI UPDATE
                ((TextView) findViewById(R.id.tvTodayUsageDetail)).setText(UsageStatsHelper.formatTime(finalTodayUsage));
                ((TextView) findViewById(R.id.tvAppOpens)).setText(String.valueOf(finalAppOpens));
                ((TextView) findViewById(R.id.tvLastUsed)).setText(UsageStatsHelper.formatTimeAgo(finalLastUsed));

                List<Insight> initialInsights = new ArrayList<>();
                initialInsights.add(new Insight(R.drawable.ic_trophy, "Usage Rank", finalUsageRank, "of " + finalTotalAppsUsed + " apps used"));
                initialInsights.add(new Insight(R.drawable.ic_streak, "Calculating...", "...", "..."));
                insightsAdapter.updateInsights(initialInsights);
            });


            // --- PASS 2 (HISTORICAL DATA) ---
            // THIS IS THE FIX: This entire block now runs on the BACKGROUND THREAD,
            // separate from the main thread callback above. This prevents the crash.

            AppDatabase database = AppDatabase.getDatabase(this);
            allUsageData.clear();
            List<DailyUsageEntity> historicalDataEntities = database.usageDao().getAllUsageForApp(packageName);
            // --- NEW: DEBUG LOGGING CODE ---
            Log.d("AppDetail_DB_Check", "--- Historical Data for " + packageName + " ---");
            if (historicalDataEntities.isEmpty()) {
                Log.d("AppDetail_DB_Check", "No historical data found in the database for this app.");
            } else {
                Log.d("AppDetail_DB_Check", "Found " + historicalDataEntities.size() + " records in the database:");
                for (DailyUsageEntity entity : historicalDataEntities) {
                    Log.d("AppDetail_DB_Check", "  - Date: " + entity.date + ", Usage: " + UsageStatsHelper.formatTime(entity.usageTime));
                }
            }
            // --- END OF DEBUG LOGGING CODE ---
            Map<String, Long> historicalUsageMap = new HashMap<>();
            for(DailyUsageEntity entity : historicalDataEntities) {
                historicalUsageMap.put(entity.date, entity.usageTime);
            }
            int totalDaysAvailable = database.usageDao().countDaysOfUsage(packageName);
            long historicalTotalUsage = 0;
            for(Long usage : historicalUsageMap.values()) {
                if(usage != null) historicalTotalUsage += usage;
            }
            long totalAvg = (totalDaysAvailable > 0) ? historicalTotalUsage / totalDaysAvailable : 0;
            int daysToIterate = Math.max(7, totalDaysAvailable);
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            for (int i = daysToIterate - 1; i >= 0; i--) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, -i);
                long dailyTime;
                if (i < 7) {
                    long start = getStartOfDayInMillis(calendar);
                    long end = (i == 0) ? System.currentTimeMillis() : getEndOfDayInMillis(calendar);
                    HashMap<String, ProcessedUsageStats> liveStatsMap = UsageStatsHelper.getProcessedUsageStats(this, start, end);
                    ProcessedUsageStats stats = liveStatsMap.get(packageName);
                    dailyTime = (stats != null) ? stats.totalTimeInForeground : 0;
                } else {
                    dailyTime = historicalUsageMap.getOrDefault(dateFormatter.format(calendar.getTime()), 0L);
                }
                allUsageData.add(dailyTime);
            }
            long weeklyTotal = 0, busiestDayUsage = -1;
            int weeklyDaysWithUsage = 0;
            String busiestDayName = "N/A";
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.US);
            List<Long> recent7Days = allUsageData.subList(Math.max(0, allUsageData.size() - 7), allUsageData.size());
            for (int i = 0; i < recent7Days.size(); i++) {
                long usage = recent7Days.get(i);
                if (usage > 0) { weeklyTotal += usage; weeklyDaysWithUsage++; }
                if (usage >= busiestDayUsage) {
                    busiestDayUsage = usage;
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DAY_OF_YEAR, -(recent7Days.size() - 1 - i));
                    busiestDayName = dayFormat.format(calendar.getTime());
                }
            }
            long weeklyAvg = (weeklyDaysWithUsage > 0) ? weeklyTotal / weeklyDaysWithUsage : 0;
            long todayUsageForTrend = allUsageData.isEmpty() ? 0 : allUsageData.get(allUsageData.size() - 1);
            long yesterdayUsage = (allUsageData.size() >= 2) ? allUsageData.get(allUsageData.size() - 2) : 0;
            long thisWeekTotal = 0, lastWeekTotal = 0;
            for (int i = 0; i < allUsageData.size(); i++) {
                if (i >= allUsageData.size() - 7) thisWeekTotal += allUsageData.get(i);
                else if (i >= allUsageData.size() - 14) lastWeekTotal += allUsageData.get(i);
            }

            // Calculate remaining insights
            ProcessedUsageStats todayStatsLive = UsageStatsHelper.getProcessedUsageStats(this, UsageStatsHelper.getStartOfDayInMillis(), System.currentTimeMillis()).get(packageName);
            if (todayStatsLive == null) todayStatsLive = new ProcessedUsageStats();

            int usageStreak = 0;
            for (int i = allUsageData.size() - 1; i >= 0; i--) {
                if (allUsageData.get(i) > TimeUnit.SECONDS.toMillis(30)) { usageStreak++; } else { break; }
            }

            long weekdayTotalUsage = 0, weekendTotalUsage = 0;
            int weekdayCount = 0, weekendCount = 0;
            Calendar cal = Calendar.getInstance();
            for (int i = 0; i < allUsageData.size(); i++) {
                cal.setTimeInMillis(System.currentTimeMillis());
                cal.add(Calendar.DAY_OF_YEAR, -(allUsageData.size() - 1 - i));
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    weekendTotalUsage += allUsageData.get(i);
                    weekendCount++;
                } else {
                    weekdayTotalUsage += allUsageData.get(i);
                    weekdayCount++;
                }
            }
            long weekdayAvg = (weekdayCount > 0) ? weekdayTotalUsage / weekdayCount : 0;
            long weekendAvg = (weekendCount > 0) ? weekendTotalUsage / weekendCount : 0;
            String weekPattern = (Math.abs(weekdayAvg - weekendAvg) < TimeUnit.MINUTES.toMillis(5)) ? "Used Evenly" : (weekdayAvg > weekendAvg) ? "Weekday App" : "Weekend App";
            long avgSession = (todayStatsLive.launchCount > 0) ? todayStatsLive.totalTimeInForeground / todayStatsLive.launchCount : 0;

            Calendar lateNightStart = Calendar.getInstance();
            lateNightStart.set(Calendar.HOUR_OF_DAY, 22);
            lateNightStart.set(Calendar.MINUTE, 0);
            lateNightStart.set(Calendar.SECOND, 0);
            long lateNightUsage = 0;
            if (System.currentTimeMillis() > lateNightStart.getTimeInMillis()) {
                HashMap<String, ProcessedUsageStats> lateNightStats = UsageStatsHelper.getProcessedUsageStats(this, lateNightStart.getTimeInMillis(), System.currentTimeMillis());
                ProcessedUsageStats stats = lateNightStats.get(packageName);
                if (stats != null) {
                    lateNightUsage = stats.totalTimeInForeground;
                }
            }

            long usageForecast = 0;
            String forecastValue = "";
            String forecastSubtitle = "";
            Calendar now = Calendar.getInstance();
            int hour = now.get(Calendar.HOUR_OF_DAY);
            if (hour >= 10) {
                double dayPercentagePassed = (hour * 60.0 + now.get(Calendar.MINUTE)) / (24.0 * 60.0);
                long todayUsageFromHistory = allUsageData.get(allUsageData.size()-1); // Use usage from allUsageData for consistency
                if (todayUsageFromHistory > 0) {
                    usageForecast = (long) (todayUsageFromHistory / dayPercentagePassed);
                    forecastValue = "~" + UsageStatsHelper.formatTime(usageForecast);
                    if (totalAvg > 0) {
                        double difference = Math.abs(usageForecast - totalAvg);
                        if (difference < totalAvg * 0.2) {
                            forecastSubtitle = "Steady with your daily average";
                        } else if (usageForecast > totalAvg) {
                            forecastSubtitle = "Heads up: trending higher than average";
                        } else {
                            forecastSubtitle = "On track for a low-usage day! 👍";
                        }
                    } else {
                        forecastSubtitle = "Your projected total for today";
                    }
                }
            }

            // Prepare ALL final values for the final UI update
            final long finalWeeklyAvg = weeklyAvg;
            final String finalBusiestDay = busiestDayName;
            final long finalTotalAvg = totalAvg;
            final int finalTotalDaysAvailable = totalDaysAvailable;
            final long finalTodayUsageForTrend = todayUsageForTrend;
            final long finalYesterdayUsage = yesterdayUsage;
            final long finalThisWeekTotal = thisWeekTotal;
            final long finalLastWeekTotal = lastWeekTotal;
            final int finalUsageStreak = usageStreak;
            final long finalAvgSession = avgSession;
            final String finalWeekPattern = weekPattern;
            final long finalLateNightUsage = lateNightUsage;
            final String finalForecastValue = forecastValue;
            final String finalForecastSubtitle = forecastSubtitle;
            String patternSubtitle;
            if (finalWeekPattern.equals("Weekday App")) {
                patternSubtitle = "Mainly used on weekdays";
            } else if (finalWeekPattern.equals("Weekend App")) {
                patternSubtitle = "Mainly used on weekends";
            } else {
                patternSubtitle = "Balanced weekly use";
            }
            final String finalPatternSubtitle = patternSubtitle;

            // FINAL UI UPDATE
            handler.post(() -> {
                // Update Overview cards
                ((TextView) findViewById(R.id.tvWeeklyAvg)).setText(UsageStatsHelper.formatTime(finalWeeklyAvg) + "/day");
                ((TextView) findViewById(R.id.tvBusiestDay)).setText(finalBusiestDay);
                View totalAvgCard = findViewById(R.id.totalAvgCard);
                if (finalTotalDaysAvailable > 7) {
                    ((TextView) findViewById(R.id.labelTotalAvg)).setText(getString(R.string.label_total_avg_days, finalTotalDaysAvailable));
                    ((TextView) findViewById(R.id.tvTotalAvg)).setText(UsageStatsHelper.formatTime(finalTotalAvg) + "/day");
                    totalAvgCard.setVisibility(View.VISIBLE);
                    btnAllDays.setText(getString(R.string.button_all_days_text, finalTotalDaysAvailable));
                    btnAllDays.setVisibility(View.VISIBLE);
                } else {
                    totalAvgCard.setVisibility(View.GONE);
                    btnAllDays.setVisibility(View.GONE);
                }
                // Update Trend cards
                updateDailyTrendCard(finalTodayUsageForTrend, finalYesterdayUsage);
                updateWeeklyTrendCard(finalThisWeekTotal, finalLastWeekTotal);
                // Update Deeper Insights carousel with the full, real data
                // We get the rank and total apps from the previous post, but we can recalculate it here
                // to ensure consistency if the cache was updated.
                AppUsageCache.getInstance().getFilteredList(this, finalList -> {
                    int finalRank = -1;
                    for (int i = 0; i < finalList.size(); i++) {
                        if (finalList.get(i).getPkg().equals(packageName)) {
                            finalRank = i + 1;
                            break;
                        }
                    }
                    String finalRankString = (finalRank != -1) ? "#" + finalRank : "N/A";
                    int finalTotalApps = finalList.size();

                    List<Insight> finalInsights = new ArrayList<>();
                    finalInsights.add(new Insight(R.drawable.ic_trophy, "Usage Rank", finalRankString, "of " + finalTotalApps + " apps used"));
                    finalInsights.add(new Insight(R.drawable.ic_streak, "Usage Streak", finalUsageStreak + " Days", "Consecutive days used"));
                    finalInsights.add(new Insight(R.drawable.ic_session, "Avg. Session", UsageStatsHelper.formatTime(finalAvgSession), "Typical use duration today"));
                    finalInsights.add(new Insight(R.drawable.ic_pattern, "Usage Pattern", finalWeekPattern, finalPatternSubtitle));
                    if (!finalForecastValue.isEmpty()) {
                        finalInsights.add(new Insight(R.drawable.ic_forecast, "Usage Forecast", finalForecastValue, finalForecastSubtitle));
                    }
                    if (finalLateNightUsage > 0) {
                        finalInsights.add(new Insight(R.drawable.ic_moon, "Late Night Use", UsageStatsHelper.formatTime(finalLateNightUsage), "Usage after 10 PM today"));
                    }
                    insightsAdapter.updateInsights(finalInsights);
                });
                // Finally, draw the chart
                displayChart(7);
            });
        });
    }

    private void updateWeeklyTrendCard(long thisWeekTotal, long lastWeekTotal) {
        ImageView ivWeekTrendArrow = findViewById(R.id.ivWeekTrendArrow);
        TextView tvWeekTrendSummary = findViewById(R.id.tvWeekTrendSummary);

        // LOGIC FIX: Only show "Started using" if last week was truly zero.
        if (lastWeekTotal == 0 && thisWeekTotal > 0) {
            ivWeekTrendArrow.setVisibility(View.GONE);
            tvWeekTrendSummary.setText("Started using this week.\nKeep it balanced. ⚖️");
        } else if (thisWeekTotal == 0 && lastWeekTotal == 0) {
            ivWeekTrendArrow.setVisibility(View.GONE);
            tvWeekTrendSummary.setText("Not used recently.\nEnjoy your time offline! 🌿");
        } else {
            // This block now correctly handles all comparison cases
            ivWeekTrendArrow.setVisibility(View.VISIBLE);
            long diff = Math.abs(thisWeekTotal - lastWeekTotal);
            String timeDiff = UsageStatsHelper.formatTime(diff);

            if (thisWeekTotal > lastWeekTotal) {
                ivWeekTrendArrow.setImageResource(R.drawable.ic_arrow_upward);
                ivWeekTrendArrow.setColorFilter(ContextCompat.getColor(this, R.color.vibrant_red));
                tvWeekTrendSummary.setText(getTrendMessage(true, diff, timeDiff, "last week"));
            } else if (thisWeekTotal < lastWeekTotal) {
                ivWeekTrendArrow.setImageResource(R.drawable.ic_arrow_downward);
                ivWeekTrendArrow.setColorFilter(ContextCompat.getColor(this, R.color.highlight_green));
                tvWeekTrendSummary.setText(getTrendMessage(false, diff, timeDiff, "last week"));
            } else {
                ivWeekTrendArrow.setVisibility(View.GONE);
                tvWeekTrendSummary.setText("Same as last week.\nConsistency builds strong habits. 📊");
            }
        }
    }

    // ... (No changes to any other methods: displayChart, setupBarChart, setupLineChart, setGreeting, etc.)
    // Paste all your other unchanged methods here.

    // DAILY TREND
    private void updateDailyTrendCard(long todayUsage, long yesterdayUsage) {
        ImageView ivDayTrendArrow = findViewById(R.id.ivDayTrendArrow);
        TextView tvDayTrendSummary = findViewById(R.id.tvDayTrendSummary);
        View dayTrendCard = findViewById(R.id.dayTrendCard);

        dayTrendCard.setVisibility(View.VISIBLE);
        ivDayTrendArrow.setVisibility(View.VISIBLE);

        long diff = Math.abs(todayUsage - yesterdayUsage);
        String timeDiff = UsageStatsHelper.formatTime(diff);

        if (yesterdayUsage > TimeUnit.MINUTES.toMillis(1)) {
            if (todayUsage > yesterdayUsage) {
                ivDayTrendArrow.setImageResource(R.drawable.ic_arrow_upward);
                ivDayTrendArrow.setColorFilter(ContextCompat.getColor(this, R.color.vibrant_red));
                tvDayTrendSummary.setText(getTrendMessage(true, diff, timeDiff, "yesterday"));
            } else if (todayUsage < yesterdayUsage) {
                ivDayTrendArrow.setImageResource(R.drawable.ic_arrow_downward);
                ivDayTrendArrow.setColorFilter(ContextCompat.getColor(this, R.color.highlight_green));
                tvDayTrendSummary.setText(getTrendMessage(false, diff, timeDiff, "yesterday"));
            } else {
                ivDayTrendArrow.setVisibility(View.GONE);
                tvDayTrendSummary.setText("Same as yesterday.\nConsistency is key! 📊");
            }
        } else {
            ivDayTrendArrow.setVisibility(View.GONE);
            if (todayUsage > 0) {
                tvDayTrendSummary.setText("Up from almost none yesterday.\nStay intentional. 🌞");
            } else {
                tvDayTrendSummary.setText("Low usage recently.\nGreat way to stay present! 🌸");
            }
        }
    }

    private void displayChart(int days) {
        List<Long> dataToShow = (days <= 7) ?
                allUsageData.subList(Math.max(0, allUsageData.size() - 7), allUsageData.size()) :
                allUsageData;

        if (days <= 7) { // Show Bar Chart for 7 days
            barChart.setVisibility(View.VISIBLE);
            lineChart.setVisibility(View.GONE);

            ArrayList<BarEntry> entries = new ArrayList<>();
            ArrayList<String> labels = new ArrayList<>();
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.US);

            for (int i = 0; i < dataToShow.size(); i++) {
                long dailyTime = dataToShow.get(i);
                entries.add(new BarEntry(i, TimeUnit.MILLISECONDS.toMinutes(dailyTime)));
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, -(dataToShow.size() - 1) + i);
                labels.add(dayFormat.format(calendar.getTime()));
            }
            setupBarChart(entries, labels);

        } else { // Show Line Chart for > 7 days
            barChart.setVisibility(View.GONE);
            lineChart.setVisibility(View.VISIBLE);

            ArrayList<Entry> entries = new ArrayList<>();
            for (int i = 0; i < dataToShow.size(); i++) {
                long dailyTime = dataToShow.get(i);
                entries.add(new Entry(i, TimeUnit.MILLISECONDS.toMinutes(dailyTime)));
            }
            setupLineChart(entries);
        }
    }

    private void setupBarChart(ArrayList<BarEntry> entries, ArrayList<String> labels) {
        BarDataSet dataSet = new BarDataSet(entries, "Usage in Minutes");
        dataSet.setGradientColor(ContextCompat.getColor(this, R.color.purple_200), ContextCompat.getColor(this, R.color.purple_700));
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.black));
        dataSet.setValueTextSize(11f);
        dataSet.setValueFormatter(new MinutesValueFormatter());
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setExtraBottomOffset(10f);
        barChart.setFitBars(true);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        barChart.getAxisRight().setEnabled(false);
        YAxis yAxis = barChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setValueFormatter(new MinutesValueFormatter());
        barChart.animateY(1200, Easing.EaseInOutQuad);
        barChart.invalidate();
    }

    private void setupLineChart(ArrayList<Entry> entries) {
        LineDataSet dataSet = new LineDataSet(entries, "Usage in Minutes");
        dataSet.setColor(ContextCompat.getColor(this, R.color.brand_purple_primary));
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.brand_purple_primary));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(0f);
        dataSet.setDrawFilled(true);
        dataSet.setFillDrawable(ContextCompat.getDrawable(this, R.drawable.chart_fade_purple));
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(5, true);
        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setValueFormatter(new MinutesValueFormatter());
        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    private long getStartOfDayInMillis(Calendar calendar) {
        Calendar cal = (Calendar) calendar.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getEndOfDayInMillis(Calendar calendar) {
        Calendar cal = (Calendar) calendar.clone();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_info_detail) {
            showDetailInfoDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDetailInfoDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_detail_info, null);
        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("GOT IT", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void setGreeting() {
        TextView greetingMessage = findViewById(R.id.greetings);
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (timeOfDay < 12) {
            greeting = "Good Morning!";
        } else if (timeOfDay < 17) {
            greeting = "Good Afternoon!";
        } else {
            greeting = "Good Evening!";
        }
        greetingMessage.setText(greeting);
    }

    private String getTrendMessage(boolean higher, long diff, String timeDiff, String period) {
        if (higher) {
            if (diff < TimeUnit.MINUTES.toMillis(15))
                return String.format("Almost the same as %s.\nKeep your balance steady. ⚖️", period);
            else if (diff < TimeUnit.HOURS.toMillis(1))
                return String.format("Higher than %s (+%s).\nStay mindful of screen habits. 🌱", period, timeDiff);
            else
                return String.format("Way higher than %s (+%s)!\nTake breaks to recharge. 🌿", period, timeDiff);
        } else {
            if (diff < TimeUnit.MINUTES.toMillis(15))
                return String.format("Almost the same as %s.\nNice steady routine! 🔑", period);
            else if (diff < TimeUnit.HOURS.toMillis(1))
                return String.format("Lower than %s (−%s).\nGreat job reducing usage! 💪", period, timeDiff);
            else
                return String.format("Way less than %s (−%s)!\nFantastic self-control. 🚀", period, timeDiff);
        }
    }
}

class MinutesValueFormatter extends ValueFormatter {
    @Override
    public String getFormattedValue(float value) {
        long minutes = (long) value;
        long hours = minutes / 60;
        long mins = minutes % 60;
        return (hours > 0) ? String.format(Locale.getDefault(), "%dh %dm", hours, mins) : String.format(Locale.getDefault(), "%dm", mins);
    }
}
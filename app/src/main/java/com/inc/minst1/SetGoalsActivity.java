package com.inc.minst1;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SetGoalsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private AppGoalAdapter adapter;
    private AppDatabase database;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Map<String, Long> goalMap = new HashMap<>();
    private Map<String, Long> dailyUsageMap = new HashMap<>();
    private List<AppGoalModel> appModelList = new ArrayList<>();
    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_goals);

        Toolbar toolbar = findViewById(R.id.toolbar_goals);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.rvAppGoals);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        database = AppDatabase.getDatabase(this);
        sharedPrefs = getSharedPreferences("MinST_Notifications", MODE_PRIVATE);

        // Adapter now takes the usage map and the listener has a new signature
        adapter = new AppGoalAdapter(appModelList, new HashMap<>(), new HashMap<>(), this::showSetGoalDialog);
        recyclerView.setAdapter(adapter);

        loadAppData();
    }

    private void loadAppData() {
        executorService.execute(() -> {
            PackageManager pm = getPackageManager();
            long startOfToday = UsageStatsHelper.getStartOfDayInMillis();
            long now = System.currentTimeMillis();
            this.dailyUsageMap = UsageStatsHelper.getAggregatedUsageMap(this, startOfToday, now);

            List<ApplicationInfo> allInstalledApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            List<ApplicationInfo> filteredApps = new ArrayList<>();
            for (ApplicationInfo app : allInstalledApps) {
                if (pm.getLaunchIntentForPackage(app.packageName) != null && !app.packageName.equals(getPackageName())) {
                    filteredApps.add(app);
                }
            }

            List<AppGoalEntity> goals = database.goalDao().getAllGoals();
            goalMap.clear();
            for (AppGoalEntity goal : goals) {
                goalMap.put(goal.packageName, goal.dailyGoalMillis);
            }

            Collections.sort(filteredApps, (a, b) -> {
                boolean aHasGoal = goalMap.containsKey(a.packageName) && goalMap.get(a.packageName) > 0;
                boolean bHasGoal = goalMap.containsKey(b.packageName) && goalMap.get(b.packageName) > 0;
                if (aHasGoal && !bHasGoal) return -1;
                if (!aHasGoal && bHasGoal) return 1;
                long usageA = dailyUsageMap.getOrDefault(a.packageName, 0L);
                long usageB = dailyUsageMap.getOrDefault(b.packageName, 0L);
                return Long.compare(usageB, usageA);
            });

            List<AppGoalModel> newAppModelList = new ArrayList<>();
            for(ApplicationInfo appInfo : filteredApps) {
                String name = appInfo.loadLabel(pm).toString();
                Drawable icon = appInfo.loadIcon(pm);
                newAppModelList.add(new AppGoalModel(appInfo, name, icon));
            }
            this.appModelList = newAppModelList;

            refreshGoalData(this.dailyUsageMap);
        });
    }

    private void refreshGoalData(Map<String, Long> currentUsageMap) {
        executorService.execute(() -> {
            List<AppGoalEntity> goals = database.goalDao().getAllGoals();
            goalMap.clear();
            for (AppGoalEntity goal : goals) {
                goalMap.put(goal.packageName, goal.dailyGoalMillis);
            }
            handler.post(() -> {
                adapter.updateData(this.appModelList, goalMap, currentUsageMap);
            });
        });
    }

    // THIS METHOD IS NOW FULLY UPDATED WITH THE NEW LOGIC
    private void showSetGoalDialog(AppGoalModel appModel, Long currentUsage) {
        final BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_goal_modern, null);
        dialog.setContentView(dialogView);

        TextView title = dialogView.findViewById(R.id.tvGoalDialogTitle);
        TextView usedTodayInfo = dialogView.findViewById(R.id.tvUsedTodayInfo);
        TextView limitWarning = dialogView.findViewById(R.id.tvLimitWarning);
        NumberPicker npHours = dialogView.findViewById(R.id.npHoursGoal);
        NumberPicker npMinutes = dialogView.findViewById(R.id.npMinutesGoal);
        MaterialButton btnRemove = dialogView.findViewById(R.id.btnRemoveLimit);
        MaterialButton btnSet = dialogView.findViewById(R.id.btnSetLimit);

        title.setText("Set Limit for " + appModel.appName);
        usedTodayInfo.setText("Used today: " + UsageStatsHelper.formatTime(currentUsage));

        npHours.setMinValue(0);
        npHours.setMaxValue(23);
        npMinutes.setMinValue(0);
        npMinutes.setMaxValue(59);

        // This is the function that checks if the selected limit is valid
        NumberPicker.OnValueChangeListener valueChangeListener = (picker, oldVal, newVal) -> {
            long selectedLimitMillis = TimeUnit.HOURS.toMillis(npHours.getValue()) + TimeUnit.MINUTES.toMillis(npMinutes.getValue());
            if (selectedLimitMillis > 0 && currentUsage > selectedLimitMillis) {
                limitWarning.setVisibility(View.VISIBLE);
            } else {
                limitWarning.setVisibility(View.GONE);
            }
        };

        npHours.setOnValueChangedListener(valueChangeListener);
        npMinutes.setOnValueChangedListener(valueChangeListener);

        Long currentGoal = goalMap.get(appModel.appInfo.packageName);
        if (currentGoal != null && currentGoal > 0) {
            npHours.setValue((int) TimeUnit.MILLISECONDS.toHours(currentGoal));
            npMinutes.setValue((int) (TimeUnit.MILLISECONDS.toMinutes(currentGoal) % 60));
            btnRemove.setVisibility(View.VISIBLE);
        } else {
            btnRemove.setVisibility(View.GONE);
        }

        // Trigger the check once initially when the dialog opens
        valueChangeListener.onValueChange(null, 0, 0);

        btnSet.setOnClickListener(v -> {
            long totalMillis = TimeUnit.HOURS.toMillis(npHours.getValue()) + TimeUnit.MINUTES.toMillis(npMinutes.getValue());
            saveGoal(appModel.appInfo.packageName, totalMillis);
            dialog.dismiss();
        });

        btnRemove.setOnClickListener(v -> {
            removeGoal(appModel.appInfo.packageName);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveGoal(String packageName, long goalMillis) {
        // This method is already correct. No changes were needed here.
        executorService.execute(() -> {
            long startOfToday = UsageStatsHelper.getStartOfDayInMillis();
            long now = System.currentTimeMillis();
            HashMap<String, Long> usageMap = UsageStatsHelper.getAggregatedUsageMap(this, startOfToday, now);
            long usageAtTimeOfSet = usageMap.getOrDefault(packageName, 0L);

            database.goalDao().setGoal(new AppGoalEntity(packageName, goalMillis, usageAtTimeOfSet));

            String todayDate = getTodayDateString();
            Set<String> currentNotified = new HashSet<>(sharedPrefs.getStringSet("goal_notified_" + todayDate, new HashSet<>()));
            currentNotified.remove(packageName);
            sharedPrefs.edit().putStringSet("goal_notified_" + todayDate, currentNotified).apply();

            refreshGoalData(this.dailyUsageMap);
            runOnUiThread(() -> Toast.makeText(this, "Limit updated!", Toast.LENGTH_SHORT).show());
        });
    }

    private void removeGoal(String packageName) {
        executorService.execute(() -> {
            database.goalDao().removeGoal(packageName);

            // THIS IS THE FIX: Clear the "notified" status when a limit is removed.
            String todayDate = getTodayDateString();
            Set<String> currentNotified = new HashSet<>(sharedPrefs.getStringSet("goal_notified_" + todayDate, new HashSet<>()));
            currentNotified.remove(packageName);
            sharedPrefs.edit().putStringSet("goal_notified_" + todayDate, currentNotified).apply();
            // --- End of fix ---

            refreshGoalData(this.dailyUsageMap);
            runOnUiThread(() -> Toast.makeText(this, "Limit removed!", Toast.LENGTH_SHORT).show());
        });
    }

    private String getTodayDateString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }
}
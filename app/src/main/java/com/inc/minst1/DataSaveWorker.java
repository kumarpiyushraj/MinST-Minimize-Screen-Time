package com.inc.minst1;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DataSaveWorker extends Worker {
    private static final String LOG_TAG = "DataSaveWorker";

    // Define keys for input data
    public static final String INPUT_START_DATE_MILLIS = "start_date_millis";
    public static final String INPUT_END_DATE_MILLIS = "end_date_millis";
    public static final String INPUT_DATE_STRING = "date_string"; // The specific date string to save for

    public DataSaveWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Retrieve input parameters, if provided
        long startMillis = getInputData().getLong(INPUT_START_DATE_MILLIS, -1L);
        long endMillis = getInputData().getLong(INPUT_END_DATE_MILLIS, -1L);
        String dateString = getInputData().getString(INPUT_DATE_STRING);

        if (startMillis != -1L && endMillis != -1L && dateString != null) {
            // This path is for the initial 7-day import
            Log.d(LOG_TAG, "WorkManager job started: Saving usage data for " + dateString);
            saveUsageToDatabase(dateString, startMillis, endMillis);
        } else {
            // This is the default path for the daily "yesterday" save
            Log.d(LOG_TAG, "WorkManager job started: Saving yesterday's usage data.");
            saveYesterdayUsageToDatabase();
        }
        return Result.success();
    }

    // Existing method, modified to call the more general saveUsageToDatabase
    private void saveYesterdayUsageToDatabase() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        String yesterdayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime());

        long startOfYesterday = UsageStatsHelper.getStartOfDayInMillis(calendar);
        long endOfYesterday = UsageStatsHelper.getEndOfDayInMillis(calendar);

        saveUsageToDatabase(yesterdayDate, startOfYesterday, endOfYesterday);
        // Save the date of the last successful save only for the daily job
        getApplicationContext().getSharedPreferences("MinST_Notifications", Context.MODE_PRIVATE)
                .edit().putString("last_save_date", yesterdayDate).apply();
    }


    // NEW: General method to save usage data for a specific date and time range
    private void saveUsageToDatabase(String dateToSave, long startMillis, long endMillis) {
        AppDatabase database = AppDatabase.getDatabase(getApplicationContext());

        Log.d(LOG_TAG, "Fetching usage data for date: " + dateToSave + " (" + startMillis + " - " + endMillis + ")");
        HashMap<String, Long> usageMap = UsageStatsHelper.getAggregatedUsageMap(getApplicationContext(), startMillis, endMillis);

        if (usageMap.isEmpty()) {
            Log.d(LOG_TAG, "No usage data found for " + dateToSave + ". Nothing to save.");
            return;
        }

        Log.d(LOG_TAG, "Found usage for " + usageMap.size() + " apps. Saving to database for " + dateToSave + "...");
        int savedCount = 0;
        for (Map.Entry<String, Long> entry : usageMap.entrySet()) {
            if (entry.getValue() > 0) {
                DailyUsageEntity dailyUsage = new DailyUsageEntity(entry.getKey(), dateToSave, entry.getValue());
                database.usageDao().insertDailyUsage(dailyUsage);
                savedCount++;
            }
        }
        Log.d(LOG_TAG, "Successfully saved " + savedCount + " records to the database for " + dateToSave);
    }
}
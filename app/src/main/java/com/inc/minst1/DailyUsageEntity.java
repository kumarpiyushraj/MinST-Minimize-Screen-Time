package com.inc.minst1;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "daily_usage", primaryKeys = {"packageName", "date"})
public class DailyUsageEntity {
    @NonNull
    public String packageName;
    @NonNull
    public String date; // Format: YYYY-MM-DD
    public long usageTime; // Total foreground time in milliseconds

    public DailyUsageEntity(@NonNull String packageName, @NonNull String date, long usageTime) {
        this.packageName = packageName;
        this.date = date;
        this.usageTime = usageTime;
    }
}
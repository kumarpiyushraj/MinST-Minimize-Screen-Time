package com.inc.minst1;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_goals")
public class AppGoalEntity {
    @PrimaryKey
    @NonNull
    public String packageName;
    public long dailyGoalMillis;
    public long usageAtTimeOfSet; // NEW: Usage at the moment the goal was set

    public AppGoalEntity(@NonNull String packageName, long dailyGoalMillis, long usageAtTimeOfSet) {
        this.packageName = packageName;
        this.dailyGoalMillis = dailyGoalMillis;
        this.usageAtTimeOfSet = usageAtTimeOfSet;
    }
}
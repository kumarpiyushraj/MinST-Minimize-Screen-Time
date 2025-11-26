package com.inc.minst1;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.MapInfo;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;
import java.util.Map; // Import Map

@Dao
public interface UsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDailyUsage(DailyUsageEntity usage);

    @Query("SELECT * FROM daily_usage WHERE packageName = :packageName ORDER BY date ASC")
    List<DailyUsageEntity> getAllUsageForApp(String packageName);

    @Query("SELECT COUNT(DISTINCT date) FROM daily_usage WHERE packageName = :packageName")
    int countDaysOfUsage(String packageName);

    @Query("SELECT * FROM daily_usage WHERE date = :date")
    List<DailyUsageEntity> getAllUsageForDate(String date);

    // --- NEW METHOD FOR THE TRACKING SERVICE ---
    @Query("SELECT packageName, usageTime FROM daily_usage WHERE date = :dateString")
    @MapInfo(keyColumn = "packageName", valueColumn = "usageTime")
    Map<String, Long> getUsageMapForDate(String dateString);
}
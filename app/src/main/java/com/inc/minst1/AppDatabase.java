package com.inc.minst1;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// Add DailyUsageEntity and increment version number to 2
@Database(entities = {AppGoalEntity.class, DailyUsageEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // Rename usageDao to goalDao for clarity
    public abstract GoalDao goalDao();

    // Add the new DAO for usage data
    public abstract UsageDao usageDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "minst_app_database")
                            .fallbackToDestructiveMigration() // This will handle the version upgrade
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
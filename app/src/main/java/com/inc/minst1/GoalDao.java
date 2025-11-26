package com.inc.minst1;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void setGoal(AppGoalEntity goal);

    @Query("SELECT * FROM app_goals")
    List<AppGoalEntity> getAllGoals();

    @Query("DELETE FROM app_goals WHERE packageName = :packageName")
    void removeGoal(String packageName);
}
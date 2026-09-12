package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profile")
    suspend fun clear()
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY isFavorite DESC, name ASC")
    fun getAllFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchFoods(query: String): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE isCustom = 1 ORDER BY id DESC")
    fun getCustomFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE category = :category ORDER BY name ASC")
    fun getFoodsByCategory(category: String): Flow<List<FoodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFoods(foods: List<FoodEntity>)

    @Update
    suspend fun updateFood(food: FoodEntity)

    @Delete
    suspend fun deleteFood(food: FoodEntity)

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun getFoodCount(): Int
}

@Dao
interface MealLogDao {
    @Query("SELECT * FROM meal_logs WHERE date = :date ORDER BY timestamp ASC")
    fun getMealLogsForDate(date: String): Flow<List<MealLogEntity>>

    @Query("SELECT * FROM meal_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllMealLogs(): Flow<List<MealLogEntity>>

    @Query("SELECT * FROM meal_logs WHERE date >= :startDate AND date <= :endDate ORDER BY timestamp DESC")
    fun getMealLogsBetweenDates(startDate: String, endDate: String): Flow<List<MealLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealLog(mealLog: MealLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMealLogs(logs: List<MealLogEntity>)

    @Delete
    suspend fun deleteMealLog(mealLog: MealLogEntity)

    @Query("DELETE FROM meal_logs")
    suspend fun clearAll()
}

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_logs WHERE date = :date ORDER BY timestamp ASC")
    fun getWaterLogsForDate(date: String): Flow<List<WaterLogEntity>>

    @Query("SELECT * FROM water_logs ORDER BY timestamp DESC")
    fun getAllWaterLogs(): Flow<List<WaterLogEntity>>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_logs WHERE date = :date")
    fun getTotalWaterForDate(date: String): Flow<Int>

    @Query("SELECT * FROM water_logs WHERE date >= :startDate AND date <= :endDate ORDER BY timestamp ASC")
    fun getWaterLogsBetweenDates(startDate: String, endDate: String): Flow<List<WaterLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(waterLog: WaterLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWaterLogs(logs: List<WaterLogEntity>)

    @Delete
    suspend fun deleteWaterLog(waterLog: WaterLogEntity)

    @Query("DELETE FROM water_logs")
    suspend fun clearAll()
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE category = :category ORDER BY name ASC")
    fun getExercisesByCategory(category: String): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int
}

@Dao
interface ExerciseSessionDao {
    @Query("SELECT * FROM exercise_sessions WHERE date = :date ORDER BY timestamp DESC")
    fun getSessionsForDate(date: String): Flow<List<ExerciseSessionEntity>>

    @Query("SELECT * FROM exercise_sessions ORDER BY timestamp DESC LIMIT 100")
    fun getAllSessions(): Flow<List<ExerciseSessionEntity>>

    @Query("SELECT * FROM exercise_sessions WHERE date >= :startDate AND date <= :endDate")
    fun getSessionsBetweenDates(startDate: String, endDate: String): Flow<List<ExerciseSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ExerciseSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSessions(sessions: List<ExerciseSessionEntity>)

    @Delete
    suspend fun deleteSession(session: ExerciseSessionEntity)

    @Query("DELETE FROM exercise_sessions")
    suspend fun clearAll()
}

@Dao
interface SleepLogDao {
    @Query("SELECT * FROM sleep_logs WHERE date = :date LIMIT 1")
    fun getSleepLogForDate(date: String): Flow<SleepLogEntity?>

    @Query("SELECT * FROM sleep_logs ORDER BY date DESC LIMIT 30")
    fun getRecentSleepLogs(): Flow<List<SleepLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSleepLog(sleepLog: SleepLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSleepLogs(logs: List<SleepLogEntity>)

    @Delete
    suspend fun deleteSleepLog(sleepLog: SleepLogEntity)

    @Query("DELETE FROM sleep_logs")
    suspend fun clearAll()
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHabits(habits: List<HabitEntity>)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habits")
    suspend fun clearAllHabits()

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun getHabitCount(): Int

    // Habit logs
    @Query("SELECT * FROM habit_logs WHERE date = :date")
    fun getHabitLogsForDate(date: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND isCompleted = 1 ORDER BY date DESC")
    fun getCompletedLogsForHabit(habitId: Long): Flow<List<HabitLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHabitLog(log: HabitLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHabitLogs(logs: List<HabitLogEntity>)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun deleteHabitLog(habitId: Long, date: String)

    @Query("DELETE FROM habit_logs")
    suspend fun clearAllHabitLogs()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders ORDER BY time ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 ORDER BY time ASC")
    fun getEnabledReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 ORDER BY time ASC")
    suspend fun getEnabledRemindersOnce(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReminders(reminders: List<ReminderEntity>)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM reminders")
    suspend fun getReminderCount(): Int
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllAsRead()

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}

@Dao
interface HealthSyncDao {
    @Query("SELECT * FROM health_sync WHERE date = :date LIMIT 1")
    fun getSyncRecordForDate(date: String): Flow<HealthSyncEntity?>

    @Query("SELECT * FROM health_sync ORDER BY date DESC LIMIT 30")
    fun getRecentSyncRecords(): Flow<List<HealthSyncEntity>>

    @Query("SELECT * FROM health_sync WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getSyncRecordsBetweenDates(startDate: String, endDate: String): Flow<List<HealthSyncEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSyncRecord(record: HealthSyncEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSyncRecords(records: List<HealthSyncEntity>)

    @Query("DELETE FROM health_sync")
    suspend fun clearAll()
}

@Dao
interface StreakDao {
    @Query("SELECT * FROM user_streak WHERE id = 1 LIMIT 1")
    fun getStreak(): Flow<StreakEntity?>

    @Query("SELECT * FROM user_streak WHERE id = 1 LIMIT 1")
    suspend fun getStreakOnce(): StreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStreak(streak: StreakEntity)

    @Query("DELETE FROM user_streak")
    suspend fun clear()
}

@Dao
interface SalatDao {
    @Query("SELECT * FROM salat_config WHERE id = 1 LIMIT 1")
    fun getSalatConfig(): Flow<SalatConfigEntity?>

    @Query("SELECT * FROM salat_config WHERE id = 1 LIMIT 1")
    suspend fun getSalatConfigOnce(): SalatConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: SalatConfigEntity)

    @Query("SELECT * FROM salat_completions WHERE date = :date")
    fun getCompletionsForDate(date: String): Flow<List<SalatCompletionEntity>>

    @Query("SELECT * FROM salat_completions WHERE date = :date")
    suspend fun getCompletionsForDateOnce(date: String): List<SalatCompletionEntity>

    @Query("SELECT * FROM salat_completions WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getCompletionsBetween(startDate: String, endDate: String): Flow<List<SalatCompletionEntity>>

    @Query("SELECT * FROM salat_completions ORDER BY date DESC, completedTimestamp DESC")
    fun getAllCompletions(): Flow<List<SalatCompletionEntity>>

    @Query("SELECT * FROM salat_completions ORDER BY date DESC, completedTimestamp DESC")
    suspend fun getAllCompletionsOnce(): List<SalatCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCompletion(completion: SalatCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCompletions(completions: List<SalatCompletionEntity>)

    @Query("DELETE FROM salat_completions WHERE date = :date AND prayerName = :prayerName")
    suspend fun deleteCompletion(date: String, prayerName: String)

    @Query("DELETE FROM salat_completions")
    suspend fun clearAllCompletions()

    @Query("SELECT * FROM cached_prayer_times WHERE date = :date LIMIT 1")
    fun getCachedPrayerTimes(date: String): Flow<CachedPrayerTimesEntity?>

    @Query("SELECT * FROM cached_prayer_times WHERE date = :date LIMIT 1")
    suspend fun getCachedPrayerTimesOnce(date: String): CachedPrayerTimesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedPrayerTimes(times: CachedPrayerTimesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCachedPrayerTimes(times: List<CachedPrayerTimesEntity>)

    @Query("DELETE FROM cached_prayer_times")
    suspend fun clearAllCachedTimes()
}

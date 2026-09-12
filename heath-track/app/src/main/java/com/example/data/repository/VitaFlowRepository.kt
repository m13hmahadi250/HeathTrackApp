package com.example.data.repository

import com.example.data.local.*
import com.example.data.sync.FirestoreSyncManager
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class VitaFlowRepository(
    private val db: VitaFlowDatabase,
    private val syncManager: FirestoreSyncManager = FirestoreSyncManager()
) {
    var currentUserId: String? = null

    val userProfile: Flow<UserProfileEntity?> = db.userProfileDao().getUserProfile()
    suspend fun getUserProfileOnce(): UserProfileEntity? = db.userProfileDao().getUserProfileOnce()
    
    suspend fun updateUserProfile(profile: UserProfileEntity) {
        db.userProfileDao().insertOrUpdateProfile(profile)
        currentUserId?.let { uid ->
            syncManager.pushProfile(uid, profile)
        }
    }

    suspend fun clearAllUserData() {
        db.userProfileDao().clear()
        db.waterLogDao().clearAll()
        db.mealLogDao().clearAll()
        db.exerciseSessionDao().clearAll()
        db.sleepLogDao().clearAll()
        db.habitDao().clearAllHabits()
        db.habitDao().clearAllHabitLogs()
        db.reminderDao().clearAll()
        db.notificationDao().clearAll()
        db.healthSyncDao().clearAll()
        db.streakDao().clear()
        db.salatDao().clearAllCompletions()
    }

    suspend fun syncFromCloud(uid: String) {
        currentUserId = uid
        syncManager.syncAllFromCloud(
            uid = uid,
            onProfileLoaded = { db.userProfileDao().insertOrUpdateProfile(it) },
            onWaterLogsLoaded = { if (it.isNotEmpty()) db.waterLogDao().insertAllWaterLogs(it) },
            onMealLogsLoaded = { if (it.isNotEmpty()) db.mealLogDao().insertAllMealLogs(it) },
            onSessionsLoaded = { if (it.isNotEmpty()) db.exerciseSessionDao().insertAllSessions(it) },
            onSleepLogsLoaded = { if (it.isNotEmpty()) db.sleepLogDao().insertAllSleepLogs(it) },
            onHabitsLoaded = { if (it.isNotEmpty()) db.habitDao().insertAllHabits(it) },
            onHabitLogsLoaded = { if (it.isNotEmpty()) db.habitDao().insertAllHabitLogs(it) },
            onRemindersLoaded = { if (it.isNotEmpty()) db.reminderDao().insertAllReminders(it) },
            onHealthSyncLoaded = { if (it.isNotEmpty()) db.healthSyncDao().insertAllSyncRecords(it) },
            onSalatConfigLoaded = { db.salatDao().insertOrUpdateConfig(it) },
            onSalatCompletionsLoaded = { if (it.isNotEmpty()) db.salatDao().insertAllCompletions(it) }
        )
    }

    suspend fun deleteUserCloudAndLocalData(uid: String) {
        syncManager.deleteCloudUserData(uid)
        clearAllUserData()
        currentUserId = null
    }

    // Foods (Static dictionary & custom foods)
    val allFoods: Flow<List<FoodEntity>> = db.foodDao().getAllFoods()
    val favoriteFoods: Flow<List<FoodEntity>> = db.foodDao().getFavoriteFoods()
    val customFoods: Flow<List<FoodEntity>> = db.foodDao().getCustomFoods()
    fun searchFoods(query: String): Flow<List<FoodEntity>> = db.foodDao().searchFoods(query)
    fun getFoodsByCategory(category: String): Flow<List<FoodEntity>> = db.foodDao().getFoodsByCategory(category)
    suspend fun insertFood(food: FoodEntity): Long = db.foodDao().insertFood(food)
    suspend fun updateFood(food: FoodEntity) = db.foodDao().updateFood(food)
    suspend fun deleteFood(food: FoodEntity) = db.foodDao().deleteFood(food)

    suspend fun ensureCatalogSeeded() {
        val count = db.foodDao().getFoodCount()
        if (count < 50) {
            db.foodDao().insertAllFoods(VitaFlowDatabase.getAllSeedFoods())
        }
    }

    // Meals
    fun getMealLogsForDate(date: String): Flow<List<MealLogEntity>> = db.mealLogDao().getMealLogsForDate(date)
    val allMealLogs: Flow<List<MealLogEntity>> = db.mealLogDao().getAllMealLogs()
    fun getMealLogsBetweenDates(start: String, end: String): Flow<List<MealLogEntity>> = db.mealLogDao().getMealLogsBetweenDates(start, end)
    
    suspend fun logMeal(mealLog: MealLogEntity): Long {
        val id = db.mealLogDao().insertMealLog(mealLog)
        val saved = mealLog.copy(id = id)
        currentUserId?.let { uid ->
            syncManager.pushMealLog(uid, saved)
        }
        return id
    }

    suspend fun deleteMeal(mealLog: MealLogEntity) {
        db.mealLogDao().deleteMealLog(mealLog)
        currentUserId?.let { uid ->
            syncManager.deleteMealLog(uid, mealLog.id)
        }
    }

    // Water
    val allWaterLogs: Flow<List<WaterLogEntity>> = db.waterLogDao().getAllWaterLogs()
    fun getWaterLogsForDate(date: String): Flow<List<WaterLogEntity>> = db.waterLogDao().getWaterLogsForDate(date)
    fun getTotalWaterForDate(date: String): Flow<Int> = db.waterLogDao().getTotalWaterForDate(date)
    fun getWaterLogsBetweenDates(start: String, end: String): Flow<List<WaterLogEntity>> = db.waterLogDao().getWaterLogsBetweenDates(start, end)
    
    suspend fun logWater(amountMl: Int, date: String = LocalDate.now().toString()): Long {
        val entity = WaterLogEntity(date = date, amountMl = amountMl)
        val id = db.waterLogDao().insertWaterLog(entity)
        val saved = entity.copy(id = id)
        currentUserId?.let { uid ->
            syncManager.pushWaterLog(uid, saved)
        }
        return id
    }

    suspend fun deleteWaterLog(log: WaterLogEntity) {
        db.waterLogDao().deleteWaterLog(log)
        currentUserId?.let { uid ->
            syncManager.deleteWaterLog(uid, log.id)
        }
    }

    // Exercises
    val allExercises: Flow<List<ExerciseEntity>> = db.exerciseDao().getAllExercises()
    fun getExercisesByCategory(cat: String): Flow<List<ExerciseEntity>> = db.exerciseDao().getExercisesByCategory(cat)
    fun getSessionsForDate(date: String): Flow<List<ExerciseSessionEntity>> = db.exerciseSessionDao().getSessionsForDate(date)
    val allExerciseSessions: Flow<List<ExerciseSessionEntity>> = db.exerciseSessionDao().getAllSessions()
    
    suspend fun logExerciseSession(session: ExerciseSessionEntity): Long {
        val id = db.exerciseSessionDao().insertSession(session)
        val saved = session.copy(id = id)
        currentUserId?.let { uid ->
            syncManager.pushExerciseSession(uid, saved)
        }
        return id
    }

    suspend fun deleteExerciseSession(session: ExerciseSessionEntity) {
        db.exerciseSessionDao().deleteSession(session)
        currentUserId?.let { uid ->
            syncManager.deleteExerciseSession(uid, session.id)
        }
    }

    // Sleep
    fun getSleepLogForDate(date: String): Flow<SleepLogEntity?> = db.sleepLogDao().getSleepLogForDate(date)
    val recentSleepLogs: Flow<List<SleepLogEntity>> = db.sleepLogDao().getRecentSleepLogs()
    
    suspend fun logSleep(sleepLog: SleepLogEntity): Long {
        val id = db.sleepLogDao().insertOrUpdateSleepLog(sleepLog)
        val saved = sleepLog.copy(id = id)
        currentUserId?.let { uid ->
            syncManager.pushSleepLog(uid, saved)
        }
        return id
    }

    // Habits
    val allHabits: Flow<List<HabitEntity>> = db.habitDao().getAllHabits()
    fun getHabitLogsForDate(date: String): Flow<List<HabitLogEntity>> = db.habitDao().getHabitLogsForDate(date)
    
    suspend fun toggleHabit(habitId: Long, date: String, currentCompleted: Boolean) {
        if (currentCompleted) {
            db.habitDao().deleteHabitLog(habitId, date)
            currentUserId?.let { uid ->
                syncManager.deleteHabitLog(uid, habitId, date)
            }
        } else {
            val log = HabitLogEntity(habitId = habitId, date = date, isCompleted = true)
            db.habitDao().insertOrUpdateHabitLog(log)
            currentUserId?.let { uid ->
                syncManager.pushHabitLog(uid, log)
            }
        }
    }

    suspend fun addHabit(habit: HabitEntity): Long {
        val id = db.habitDao().insertHabit(habit)
        val saved = habit.copy(id = id)
        currentUserId?.let { uid ->
            syncManager.pushHabit(uid, saved)
        }
        return id
    }

    suspend fun deleteHabit(habit: HabitEntity) {
        db.habitDao().deleteHabit(habit)
        currentUserId?.let { uid ->
            syncManager.deleteHabit(uid, habit.id)
        }
    }

    // Reminders
    val allReminders: Flow<List<ReminderEntity>> = db.reminderDao().getAllReminders()
    val enabledReminders: Flow<List<ReminderEntity>> = db.reminderDao().getEnabledReminders()
    suspend fun getEnabledRemindersOnce(): List<ReminderEntity> = db.reminderDao().getEnabledRemindersOnce()
    
    suspend fun saveReminder(reminder: ReminderEntity): Long {
        val id = db.reminderDao().insertReminder(reminder)
        val saved = reminder.copy(id = id)
        currentUserId?.let { uid ->
            syncManager.pushReminder(uid, saved)
        }
        return id
    }

    suspend fun updateReminder(reminder: ReminderEntity) {
        db.reminderDao().updateReminder(reminder)
        currentUserId?.let { uid ->
            syncManager.pushReminder(uid, reminder)
        }
    }

    suspend fun deleteReminder(reminder: ReminderEntity) {
        db.reminderDao().deleteReminder(reminder)
        currentUserId?.let { uid ->
            syncManager.deleteReminder(uid, reminder.id)
        }
    }

    // Notifications
    val allNotifications: Flow<List<NotificationEntity>> = db.notificationDao().getAllNotifications()
    val unreadNotificationCount: Flow<Int> = db.notificationDao().getUnreadCount()
    suspend fun postNotification(notification: NotificationEntity): Long = db.notificationDao().insertNotification(notification)
    suspend fun markAllNotificationsRead() = db.notificationDao().markAllAsRead()
    suspend fun markNotificationRead(id: Long) = db.notificationDao().markAsRead(id)
    suspend fun deleteNotification(id: Long) = db.notificationDao().deleteNotification(id)
    suspend fun clearAllNotifications() = db.notificationDao().clearAll()

    // Health Sync
    fun getHealthSyncForDate(date: String): Flow<HealthSyncEntity?> = db.healthSyncDao().getSyncRecordForDate(date)
    val recentHealthSyncRecords: Flow<List<HealthSyncEntity>> = db.healthSyncDao().getRecentSyncRecords()
    fun getHealthSyncBetweenDates(startDate: String, endDate: String): Flow<List<HealthSyncEntity>> =
        db.healthSyncDao().getSyncRecordsBetweenDates(startDate, endDate)
    
    suspend fun updateHealthSync(record: HealthSyncEntity) {
        db.healthSyncDao().insertOrUpdateSyncRecord(record)
        currentUserId?.let { uid ->
            syncManager.pushHealthSync(uid, record)
        }
        recalculateStreak()
    }

    // Consistency Streak (persisted in Room user_streak table)
    val streak: Flow<StreakEntity?> = db.streakDao().getStreak()

    suspend fun recalculateStreak(today: LocalDate = LocalDate.now()) {
        try {
            // Collect all unique active dates across Room tables
            val activeDates = mutableSetOf<String>()
            
            // Meals
            val meals = db.mealLogDao().getAllMealLogs()
            // Using once/first or collecting dates
            // Let's check recent 60 days
            val startDate = today.minusDays(60).toString()
            val endDate = today.toString()
            
            // Collect logged dates from recent sessions, water, meals
            // Since we have DAOs, we can calculate active dates
            val existingStreak = db.streakDao().getStreakOnce() ?: StreakEntity()
            
            // Check if today or past days have entries
            var currentCount = 0
            var checkDate = today
            var streakActive = true
            
            // Check today first. If not logged today, check if logged yesterday to maintain streak
            // Calculate active status for each day
            val datesToCheck = (0..60).map { today.minusDays(it.toLong()).toString() }
            
            var longest = existingStreak.longestStreak
            var totalDays = existingStreak.totalActiveDays
            
            val updated = existingStreak.copy(
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            db.streakDao().insertOrUpdateStreak(updated)
        } catch (_: Exception) {}
    }

    suspend fun saveStreak(streak: StreakEntity) {
        db.streakDao().insertOrUpdateStreak(streak)
    }

    // Salat / Prayer Tracking
    val salatConfig: Flow<SalatConfigEntity?> = db.salatDao().getSalatConfig()
    suspend fun getSalatConfigOnce(): SalatConfigEntity? = db.salatDao().getSalatConfigOnce()

    suspend fun updateSalatConfig(config: SalatConfigEntity) {
        db.salatDao().insertOrUpdateConfig(config)
        currentUserId?.let { uid ->
            syncManager.pushSalatConfig(uid, config)
        }
    }

    fun getSalatCompletionsForDate(date: String): Flow<List<SalatCompletionEntity>> =
        db.salatDao().getCompletionsForDate(date)

    fun getSalatCompletionsBetween(startDate: String, endDate: String): Flow<List<SalatCompletionEntity>> =
        db.salatDao().getCompletionsBetween(startDate, endDate)

    val allSalatCompletions: Flow<List<SalatCompletionEntity>> =
        db.salatDao().getAllCompletions()

    suspend fun getAllSalatCompletionsOnce(): List<SalatCompletionEntity> =
        db.salatDao().getAllCompletionsOnce()

    suspend fun insertAllSalatCompletions(list: List<SalatCompletionEntity>) {
        db.salatDao().insertAllCompletions(list)
    }

    suspend fun toggleSalatCompletion(date: String, prayerName: String, isCompleted: Boolean) {
        if (isCompleted) {
            val entity = SalatCompletionEntity(date = date, prayerName = prayerName, isCompleted = true)
            db.salatDao().insertOrUpdateCompletion(entity)
            currentUserId?.let { uid ->
                syncManager.pushSalatCompletion(uid, entity)
            }
        } else {
            db.salatDao().deleteCompletion(date, prayerName)
            currentUserId?.let { uid ->
                syncManager.deleteSalatCompletion(uid, date, prayerName)
            }
        }
        recalculateStreak()
    }

    fun getCachedPrayerTimes(date: String): Flow<CachedPrayerTimesEntity?> =
        db.salatDao().getCachedPrayerTimes(date)

    suspend fun cachePrayerTimes(times: CachedPrayerTimesEntity) {
        db.salatDao().insertCachedPrayerTimes(times)
    }
}


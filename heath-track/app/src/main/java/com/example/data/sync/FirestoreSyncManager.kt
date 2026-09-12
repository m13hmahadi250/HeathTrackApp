package com.example.data.sync

import android.util.Log
import com.example.data.local.*
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreSyncManager {

    private var firestore: FirebaseFirestore? = null

    init {
        try {
            if (FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isNotEmpty()) {
                firestore = FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore initialization notice: ${e.message}")
        }
    }

    private fun getDb(): FirebaseFirestore? {
        if (firestore == null) {
            try {
                firestore = FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore not ready: ${e.message}")
            }
        }
        return firestore
    }

    suspend fun syncAllFromCloud(
        uid: String,
        onProfileLoaded: suspend (UserProfileEntity) -> Unit,
        onWaterLogsLoaded: suspend (List<WaterLogEntity>) -> Unit,
        onMealLogsLoaded: suspend (List<MealLogEntity>) -> Unit,
        onSessionsLoaded: suspend (List<ExerciseSessionEntity>) -> Unit,
        onSleepLogsLoaded: suspend (List<SleepLogEntity>) -> Unit,
        onHabitsLoaded: suspend (List<HabitEntity>) -> Unit,
        onHabitLogsLoaded: suspend (List<HabitLogEntity>) -> Unit,
        onRemindersLoaded: suspend (List<ReminderEntity>) -> Unit,
        onHealthSyncLoaded: suspend (List<HealthSyncEntity>) -> Unit,
        onSalatConfigLoaded: suspend (SalatConfigEntity) -> Unit = {},
        onSalatCompletionsLoaded: suspend (List<SalatCompletionEntity>) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        val db = getDb() ?: return@withContext

        try {
            // 1. Profile
            val profileDoc = db.collection("users").document(uid).collection("profile").document("main").get().await()
            if (profileDoc.exists()) {
                val data = profileDoc.data ?: emptyMap()
                val profile = UserProfileEntity(
                    id = 1,
                    name = data["name"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    photoUrl = data["photoUrl"] as? String,
                    firebaseUid = uid,
                    age = (data["age"] as? Number)?.toInt() ?: 0,
                    heightCm = (data["heightCm"] as? Number)?.toFloat() ?: 0f,
                    activityLevel = data["activityLevel"] as? String ?: "Moderately Active",
                    wellnessGoal = data["wellnessGoal"] as? String ?: "Consistency & Energy",
                    dietaryPreference = data["dietaryPreference"] as? String ?: "Balanced",
                    allergies = data["allergies"] as? String ?: "None",
                    foodPreferences = data["foodPreferences"] as? String ?: "Fresh, home-cooked",
                    sleepTargetHours = (data["sleepTargetHours"] as? Number)?.toFloat() ?: 8f,
                    wakeUpTime = data["wakeUpTime"] as? String ?: "07:00",
                    bedTime = data["bedTime"] as? String ?: "23:00",
                    waterTargetMl = (data["waterTargetMl"] as? Number)?.toInt() ?: 2500,
                    stepTarget = (data["stepTarget"] as? Number)?.toInt() ?: 8000,
                    isMinor = data["isMinor"] as? Boolean ?: false,
                    isOnboardingCompleted = data["isOnboardingCompleted"] as? Boolean ?: false
                )
                onProfileLoaded(profile)
            }

            // 2. Water Logs
            val waterDocs = db.collection("users").document(uid).collection("water_logs").get().await()
            if (!waterDocs.isEmpty) {
                val waterList = waterDocs.documents.mapNotNull { doc ->
                    val map = doc.data ?: return@mapNotNull null
                    WaterLogEntity(
                        id = doc.id.toLongOrNull() ?: 0L,
                        date = map["date"] as? String ?: return@mapNotNull null,
                        amountMl = (map["amountMl"] as? Number)?.toInt() ?: return@mapNotNull null,
                        timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
                onWaterLogsLoaded(waterList)
            }

            // 3. Meal Logs
            val mealDocs = db.collection("users").document(uid).collection("meal_logs").get().await()
            if (!mealDocs.isEmpty) {
                val mealList = mealDocs.documents.mapNotNull { doc ->
                    val map = doc.data ?: return@mapNotNull null
                    MealLogEntity(
                        id = doc.id.toLongOrNull() ?: 0L,
                        date = map["date"] as? String ?: return@mapNotNull null,
                        mealType = map["mealType"] as? String ?: "Other",
                        foodName = map["foodName"] as? String ?: "",
                        portion = map["portion"] as? String ?: "",
                        calories = (map["calories"] as? Number)?.toInt() ?: 0,
                        protein = (map["protein"] as? Number)?.toFloat() ?: 0f,
                        carbs = (map["carbs"] as? Number)?.toFloat() ?: 0f,
                        fat = (map["fat"] as? Number)?.toFloat() ?: 0f,
                        fiber = (map["fiber"] as? Number)?.toFloat() ?: 0f,
                        isEstimated = map["isEstimated"] as? Boolean ?: false,
                        timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
                onMealLogsLoaded(mealList)
            }

            // 4. Exercise Sessions
            val exerciseDocs = db.collection("users").document(uid).collection("exercise_sessions").get().await()
            if (!exerciseDocs.isEmpty) {
                val sessions = exerciseDocs.documents.mapNotNull { doc ->
                    val map = doc.data ?: return@mapNotNull null
                    ExerciseSessionEntity(
                        id = doc.id.toLongOrNull() ?: 0L,
                        date = map["date"] as? String ?: return@mapNotNull null,
                        exerciseName = map["exerciseName"] as? String ?: "",
                        category = map["category"] as? String ?: "General",
                        durationMinutes = (map["durationMinutes"] as? Number)?.toInt() ?: 0,
                        caloriesBurned = (map["caloriesBurned"] as? Number)?.toInt() ?: 0,
                        notes = map["notes"] as? String ?: "",
                        timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
                onSessionsLoaded(sessions)
            }

            // 5. Sleep Logs
            val sleepDocs = db.collection("users").document(uid).collection("sleep_logs").get().await()
            if (!sleepDocs.isEmpty) {
                val sleepLogs = sleepDocs.documents.mapNotNull { doc ->
                    val map = doc.data ?: return@mapNotNull null
                    SleepLogEntity(
                        id = doc.id.toLongOrNull() ?: 0L,
                        date = map["date"] as? String ?: return@mapNotNull null,
                        bedtime = map["bedtime"] as? String ?: "23:00",
                        wakeTime = map["wakeTime"] as? String ?: "07:00",
                        durationMinutes = (map["durationMinutes"] as? Number)?.toInt() ?: 480,
                        quality = map["quality"] as? String ?: "Restful",
                        source = map["source"] as? String ?: "Manual"
                    )
                }
                onSleepLogsLoaded(sleepLogs)
            }

            // 6. Habits
            val habitDocs = db.collection("users").document(uid).collection("habits").get().await()
            if (!habitDocs.isEmpty) {
                val habits = habitDocs.documents.mapNotNull { doc ->
                    val map = doc.data ?: return@mapNotNull null
                    HabitEntity(
                        id = doc.id.toLongOrNull() ?: 0L,
                        title = map["title"] as? String ?: return@mapNotNull null,
                        icon = map["icon"] as? String ?: "star",
                        targetDaysPerWeek = (map["targetDaysPerWeek"] as? Number)?.toInt() ?: 7
                    )
                }
                onHabitsLoaded(habits)
            }

            // 7. Habit Logs
            val habitLogDocs = db.collection("users").document(uid).collection("habit_logs").get().await()
            if (!habitLogDocs.isEmpty) {
                val habitLogs = habitLogDocs.documents.mapNotNull { doc ->
                    val map = doc.data ?: return@mapNotNull null
                    HabitLogEntity(
                        id = doc.id.toLongOrNull() ?: 0L,
                        habitId = (map["habitId"] as? Number)?.toLong() ?: return@mapNotNull null,
                        date = map["date"] as? String ?: return@mapNotNull null,
                        isCompleted = map["isCompleted"] as? Boolean ?: true,
                        timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
                onHabitLogsLoaded(habitLogs)
            }

            // 8. Reminders
            val reminderDocs = db.collection("users").document(uid).collection("reminders").get().await()
            if (!reminderDocs.isEmpty) {
                val reminders = reminderDocs.documents.mapNotNull { doc ->
                    val map = doc.data ?: return@mapNotNull null
                    ReminderEntity(
                        id = doc.id.toLongOrNull() ?: 0L,
                        title = map["title"] as? String ?: return@mapNotNull null,
                        time = map["time"] as? String ?: "08:00",
                        repeatType = map["repeatType"] as? String ?: "Daily",
                        isEnabled = map["isEnabled"] as? Boolean ?: true,
                        type = map["type"] as? String ?: "Water"
                    )
                }
                onRemindersLoaded(reminders)
            }

            // 9. Health Sync
            val healthDocs = db.collection("users").document(uid).collection("health_sync").get().await()
            if (!healthDocs.isEmpty) {
                val healthList = healthDocs.documents.mapNotNull { doc ->
                    val map = doc.data ?: return@mapNotNull null
                    HealthSyncEntity(
                        date = map["date"] as? String ?: doc.id,
                        steps = (map["steps"] as? Number)?.toInt() ?: 0,
                        distanceMeters = (map["distanceMeters"] as? Number)?.toFloat() ?: 0f,
                        activeCalories = (map["activeCalories"] as? Number)?.toInt() ?: 0,
                        heartRateBpm = (map["heartRateBpm"] as? Number)?.toInt() ?: 0,
                        source = map["source"] as? String ?: "Authorized Sync",
                        lastSyncedTimestamp = (map["lastSyncedTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
                onHealthSyncLoaded(healthList)
            }

            // 10. Salat Config
            val salatConfigDoc = db.collection("users").document(uid).collection("salat_config").document("main").get().await()
            if (salatConfigDoc.exists()) {
                val data = salatConfigDoc.data ?: emptyMap()
                val config = SalatConfigEntity(
                    id = 1,
                    isSalatEnabled = data["isSalatEnabled"] as? Boolean ?: true,
                    calculationMethod = data["calculationMethod"] as? String ?: "Karachi",
                    madhab = data["madhab"] as? String ?: "Hanafi",
                    locationName = data["locationName"] as? String ?: "Chattogram, Bangladesh",
                    latitude = (data["latitude"] as? Number)?.toDouble() ?: 22.3569,
                    longitude = (data["longitude"] as? Number)?.toDouble() ?: 91.7832,
                    isAutoLocation = data["isAutoLocation"] as? Boolean ?: true,
                    timeZoneId = data["timeZoneId"] as? String ?: "Asia/Dhaka",
                    timeZoneOffsetHours = (data["timeZoneOffsetHours"] as? Number)?.toDouble() ?: 6.0,
                    notifyAtTime = data["notifyAtTime"] as? Boolean ?: true,
                    notifyBeforeTime = data["notifyBeforeTime"] as? Boolean ?: true,
                    notifyBeforeMinutes = (data["notifyBeforeMinutes"] as? Number)?.toInt() ?: 15,
                    soundOption = data["soundOption"] as? String ?: "Default",
                    vibrationEnabled = data["vibrationEnabled"] as? Boolean ?: true,
                    fajrEnabled = data["fajrEnabled"] as? Boolean ?: true,
                    sunriseEnabled = data["sunriseEnabled"] as? Boolean ?: false,
                    dhuhrEnabled = data["dhuhrEnabled"] as? Boolean ?: true,
                    asrEnabled = data["asrEnabled"] as? Boolean ?: true,
                    maghribEnabled = data["maghribEnabled"] as? Boolean ?: true,
                    ishaEnabled = data["ishaEnabled"] as? Boolean ?: true,
                    lastSyncedDate = data["lastSyncedDate"] as? String ?: ""
                )
                onSalatConfigLoaded(config)
            }

            // 11. Salat Completions
            val salatDocs = db.collection("users").document(uid).collection("salat_completions").get().await()
            if (!salatDocs.isEmpty) {
                val salatList = salatDocs.documents.mapNotNull { doc ->
                    val map = doc.data ?: return@mapNotNull null
                    SalatCompletionEntity(
                        date = map["date"] as? String ?: return@mapNotNull null,
                        prayerName = map["prayerName"] as? String ?: return@mapNotNull null,
                        isCompleted = map["isCompleted"] as? Boolean ?: false,
                        completedTimestamp = (map["completedTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
                onSalatCompletionsLoaded(salatList)
            }

            Log.d(TAG, "Successfully synced cloud data for user: $uid")
        } catch (e: Exception) {
            Log.w(TAG, "Cloud sync notice: ${e.message}")
        }
    }

    suspend fun pushProfile(uid: String, profile: UserProfileEntity) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        val db = getDb() ?: return@withContext
        try {
            val map = hashMapOf(
                "name" to profile.name,
                "email" to profile.email,
                "photoUrl" to profile.photoUrl,
                "age" to profile.age,
                "heightCm" to profile.heightCm,
                "activityLevel" to profile.activityLevel,
                "wellnessGoal" to profile.wellnessGoal,
                "dietaryPreference" to profile.dietaryPreference,
                "allergies" to profile.allergies,
                "foodPreferences" to profile.foodPreferences,
                "sleepTargetHours" to profile.sleepTargetHours,
                "wakeUpTime" to profile.wakeUpTime,
                "bedTime" to profile.bedTime,
                "waterTargetMl" to profile.waterTargetMl,
                "stepTarget" to profile.stepTarget,
                "isMinor" to profile.isMinor,
                "isOnboardingCompleted" to profile.isOnboardingCompleted,
                "lastUpdated" to System.currentTimeMillis()
            )
            db.collection("users").document(uid).collection("profile").document("main").set(map, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Push profile warning: ${e.message}")
        }
    }

    suspend fun pushWaterLog(uid: String, log: WaterLogEntity) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        val db = getDb() ?: return@withContext
        try {
            val map = hashMapOf(
                "date" to log.date,
                "amountMl" to log.amountMl,
                "timestamp" to log.timestamp
            )
            val docId = if (log.id > 0) log.id.toString() else log.timestamp.toString()
            db.collection("users").document(uid).collection("water_logs").document(docId).set(map, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Push water log warning: ${e.message}")
        }
    }

    suspend fun deleteWaterLog(uid: String, id: Long) = withContext(Dispatchers.IO) {
        if (uid.isBlank() || id <= 0) return@withContext
        val db = getDb() ?: return@withContext
        try {
            db.collection("users").document(uid).collection("water_logs").document(id.toString()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Delete water log warning: ${e.message}")
        }
    }

    suspend fun pushMealLog(uid: String, log: MealLogEntity) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        val db = getDb() ?: return@withContext
        try {
            val map = hashMapOf(
                "date" to log.date,
                "mealType" to log.mealType,
                "foodName" to log.foodName,
                "portion" to log.portion,
                "calories" to log.calories,
                "protein" to log.protein,
                "carbs" to log.carbs,
                "fat" to log.fat,
                "fiber" to log.fiber,
                "isEstimated" to log.isEstimated,
                "timestamp" to log.timestamp
            )
            val docId = if (log.id > 0) log.id.toString() else log.timestamp.toString()
            db.collection("users").document(uid).collection("meal_logs").document(docId).set(map, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Push meal warning: ${e.message}")
        }
    }

    suspend fun deleteMealLog(uid: String, id: Long) = withContext(Dispatchers.IO) {
        if (uid.isBlank() || id <= 0) return@withContext
        val db = getDb() ?: return@withContext
        try {
            db.collection("users").document(uid).collection("meal_logs").document(id.toString()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Delete meal warning: ${e.message}")
        }
    }

    suspend fun pushExerciseSession(uid: String, session: ExerciseSessionEntity) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        val db = getDb() ?: return@withContext
        try {
            val map = hashMapOf(
                "date" to session.date,
                "exerciseName" to session.exerciseName,
                "category" to session.category,
                "durationMinutes" to session.durationMinutes,
                "caloriesBurned" to session.caloriesBurned,
                "notes" to session.notes,
                "timestamp" to session.timestamp
            )
            val docId = if (session.id > 0) session.id.toString() else session.timestamp.toString()
            db.collection("users").document(uid).collection("exercise_sessions").document(docId).set(map, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Push exercise warning: ${e.message}")
        }
    }

    suspend fun deleteExerciseSession(uid: String, id: Long) = withContext(Dispatchers.IO) {
        if (uid.isBlank() || id <= 0) return@withContext
        val db = getDb() ?: return@withContext
        try {
            db.collection("users").document(uid).collection("exercise_sessions").document(id.toString()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Delete exercise warning: ${e.message}")
        }
    }

    suspend fun pushSleepLog(uid: String, log: SleepLogEntity) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        val db = getDb() ?: return@withContext
        try {
            val map = hashMapOf(
                "date" to log.date,
                "bedtime" to log.bedtime,
                "wakeTime" to log.wakeTime,
                "durationMinutes" to log.durationMinutes,
                "quality" to log.quality,
                "source" to log.source
            )
            db.collection("users").document(uid).collection("sleep_logs").document(log.date).set(map, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Push sleep warning: ${e.message}")
        }
    }

    suspend fun pushHabit(uid: String, habit: HabitEntity) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        val db = getDb() ?: return@withContext
        try {
            val map = hashMapOf(
                "title" to habit.title,
                "icon" to habit.icon,
                "targetDaysPerWeek" to habit.targetDaysPerWeek
            )
            val docId = if (habit.id > 0) habit.id.toString() else System.currentTimeMillis().toString()
            db.collection("users").document(uid).collection("habits").document(docId).set(map, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Push habit warning: ${e.message}")
        }
    }

    suspend fun deleteHabit(uid: String, id: Long) = withContext(Dispatchers.IO) {
        if (uid.isBlank() || id <= 0) return@withContext
        val db = getDb() ?: return@withContext
        try {
            db.collection("users").document(uid).collection("habits").document(id.toString()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Delete habit warning: ${e.message}")
        }
    }

    suspend fun pushHabitLog(uid: String, log: HabitLogEntity) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        val db = getDb() ?: return@withContext
        try {
            val map = hashMapOf(
                "habitId" to log.habitId,
                "date" to log.date,
                "isCompleted" to log.isCompleted,
                "timestamp" to log.timestamp
            )
            val docId = "${log.habitId}_${log.date}"
            db.collection("users").document(uid).collection("habit_logs").document(docId).set(map, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Push habit log warning: ${e.message}")
        }
    }

    suspend fun deleteHabitLog(uid: String, habitId: Long, date: String) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        val db = getDb() ?: return@withContext
        try {
            val docId = "${habitId}_${date}"
            db.collection("users").document(uid).collection("habit_logs").document(docId).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Delete habit log warning: ${e.message}")
        }
    }

    suspend fun pushReminder(uid: String, reminder: ReminderEntity) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        val db = getDb() ?: return@withContext
        try {
            val map = hashMapOf(
                "title" to reminder.title,
                "time" to reminder.time,
                "repeatType" to reminder.repeatType,
                "isEnabled" to reminder.isEnabled,
                "type" to reminder.type
            )
            val docId = if (reminder.id > 0) reminder.id.toString() else System.currentTimeMillis().toString()
            db.collection("users").document(uid).collection("reminders").document(docId).set(map, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Push reminder warning: ${e.message}")
        }
    }

    suspend fun deleteReminder(uid: String, id: Long) = withContext(Dispatchers.IO) {
        if (uid.isBlank() || id <= 0) return@withContext
        val db = getDb() ?: return@withContext
        try {
            db.collection("users").document(uid).collection("reminders").document(id.toString()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Delete reminder warning: ${e.message}")
        }
    }

    suspend fun pushHealthSync(uid: String, record: HealthSyncEntity) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        val db = getDb() ?: return@withContext
        try {
            val map = hashMapOf(
                "date" to record.date,
                "steps" to record.steps,
                "distanceMeters" to record.distanceMeters,
                "activeCalories" to record.activeCalories,
                "heartRateBpm" to record.heartRateBpm,
                "source" to record.source,
                "lastSyncedTimestamp" to record.lastSyncedTimestamp
            )
            db.collection("users").document(uid).collection("health_sync").document(record.date).set(map, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Push health sync warning: ${e.message}")
        }
    }

    suspend fun pushSalatConfig(uid: String, config: SalatConfigEntity) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        val db = getDb() ?: return@withContext
        try {
            val map = hashMapOf(
                "isSalatEnabled" to config.isSalatEnabled,
                "calculationMethod" to config.calculationMethod,
                "madhab" to config.madhab,
                "locationName" to config.locationName,
                "latitude" to config.latitude,
                "longitude" to config.longitude,
                "isAutoLocation" to config.isAutoLocation,
                "timeZoneId" to config.timeZoneId,
                "timeZoneOffsetHours" to config.timeZoneOffsetHours,
                "notifyAtTime" to config.notifyAtTime,
                "notifyBeforeTime" to config.notifyBeforeTime,
                "notifyBeforeMinutes" to config.notifyBeforeMinutes,
                "soundOption" to config.soundOption,
                "vibrationEnabled" to config.vibrationEnabled,
                "fajrEnabled" to config.fajrEnabled,
                "sunriseEnabled" to config.sunriseEnabled,
                "dhuhrEnabled" to config.dhuhrEnabled,
                "asrEnabled" to config.asrEnabled,
                "maghribEnabled" to config.maghribEnabled,
                "ishaEnabled" to config.ishaEnabled,
                "lastSyncedDate" to config.lastSyncedDate
            )
            db.collection("users").document(uid).collection("salat_config").document("main").set(map, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Push salat config warning: ${e.message}")
        }
    }

    suspend fun pushSalatCompletion(uid: String, completion: SalatCompletionEntity) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        val db = getDb() ?: return@withContext
        try {
            val docId = "${completion.date}_${completion.prayerName}"
            val map = hashMapOf(
                "date" to completion.date,
                "prayerName" to completion.prayerName,
                "isCompleted" to completion.isCompleted,
                "completedTimestamp" to completion.completedTimestamp
            )
            db.collection("users").document(uid).collection("salat_completions").document(docId).set(map, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Push salat completion warning: ${e.message}")
        }
    }

    suspend fun deleteSalatCompletion(uid: String, date: String, prayerName: String) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        val db = getDb() ?: return@withContext
        try {
            val docId = "${date}_${prayerName}"
            db.collection("users").document(uid).collection("salat_completions").document(docId).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Delete salat completion warning: ${e.message}")
        }
    }

    suspend fun deleteCloudUserData(uid: String) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        val db = getDb() ?: return@withContext
        try {
            val subcollections = listOf("profile", "water_logs", "meal_logs", "exercise_sessions", "sleep_logs", "habits", "habit_logs", "reminders", "health_sync", "salat_config", "salat_completions")
            for (sub in subcollections) {
                val snapshot = db.collection("users").document(uid).collection(sub).get().await()
                for (doc in snapshot.documents) {
                    doc.reference.delete().await()
                }
            }
            db.collection("users").document(uid).delete().await()
            Log.d(TAG, "Cleaned all cloud data for user: $uid")
        } catch (e: Exception) {
            Log.w(TAG, "Delete cloud data warning: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "FirestoreSyncManager"
    }
}

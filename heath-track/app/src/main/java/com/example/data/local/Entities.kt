package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val firebaseUid: String = "",
    val age: Int = 0,
    val heightCm: Float = 0f,
    val activityLevel: String = "Moderately Active",
    val wellnessGoal: String = "Consistency & Energy",
    val dietaryPreference: String = "Balanced",
    val allergies: String = "None",
    val foodPreferences: String = "Fresh, home-cooked",
    val sleepTargetHours: Float = 8f,
    val wakeUpTime: String = "07:00",
    val bedTime: String = "23:00",
    val waterTargetMl: Int = 2500,
    val stepTarget: Int = 8000,
    val isMinor: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true
)

@Entity(
    tableName = "foods",
    indices = [Index(value = ["name"]), Index(value = ["category"]), Index(value = ["isFavorite"])]
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val servingSize: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float,
    val category: String, // Grains, Protein, Vegetables, Curries & Dal, Fruits, Dairy, Snacks
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val isEstimated: Boolean = false,
    val notes: String = ""
)

@Entity(
    tableName = "meal_logs",
    indices = [Index(value = ["date"]), Index(value = ["timestamp"])]
)
data class MealLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val mealType: String, // Breakfast, Lunch, Dinner, Snack, Drink, Dessert, Other
    val foodName: String,
    val portion: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float,
    val isEstimated: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "water_logs",
    indices = [Index(value = ["date"])]
)
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val amountMl: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String, // Walking, Running, Cycling, Strength, Mobility, Stretching, Yoga, Bodyweight, Beginner, Recovery
    val difficulty: String, // Beginner, Intermediate, Advanced
    val durationMinutes: Int,
    val instructions: String,
    val restTimeSeconds: Int,
    val safetyNotes: String
)

@Entity(
    tableName = "exercise_sessions",
    indices = [Index(value = ["date"])]
)
data class ExerciseSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val exerciseName: String,
    val category: String,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sleep_logs",
    indices = [Index(value = ["date"])]
)
data class SleepLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val bedtime: String, // e.g. "23:15"
    val wakeTime: String, // e.g. "07:15"
    val durationMinutes: Int,
    val quality: String = "Restful", // Restful, Normal, Restless
    val source: String = "Manual", // Manual, Health Connect
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val icon: String, // e.g. "water", "food", "walk", "stretch", "sleep", "mindfulness"
    val targetDaysPerWeek: Int = 7,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "habit_logs",
    indices = [Index(value = ["habitId", "date"], unique = true), Index(value = ["date"])]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val date: String, // YYYY-MM-DD
    val isCompleted: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val time: String, // HH:mm format, e.g. "09:30"
    val repeatType: String = "Daily", // Daily, Weekdays, Weekends, Custom
    val type: String = "Water", // Water, Meal, Exercise, Stretch, Sleep, Habit
    val isEnabled: Boolean = true,
    val withSound: Boolean = true,
    val withVibration: Boolean = true
)

@Entity(
    tableName = "notifications",
    indices = [Index(value = ["timestamp"])]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val type: String, // Water, Meal, Exercise, Sleep, Habit, System
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "health_sync")
data class HealthSyncEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val steps: Int = 0,
    val distanceMeters: Float = 0f,
    val activeCalories: Int = 0,
    val heartRateBpm: Int = 0,
    val source: String = "Health Connect",
    val lastSyncedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_streak")
data class StreakEntity(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: String = "",
    val totalActiveDays: Int = 0,
    val activeDaysThisWeek: Int = 0,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "salat_config")
data class SalatConfigEntity(
    @PrimaryKey val id: Int = 1,
    val isSalatEnabled: Boolean = true,
    val calculationMethod: String = "Karachi", // Karachi, MWL, Egyptian, Makkah, Tehran, Moonsighting, ISNA
    val madhab: String = "Hanafi", // Hanafi, Shafi
    val locationName: String = "Chattogram, Bangladesh",
    val latitude: Double = 22.3569,
    val longitude: Double = 91.7832,
    val isAutoLocation: Boolean = true,
    val timeZoneId: String = "Asia/Dhaka",
    val timeZoneOffsetHours: Double = 6.0,
    val notifyAtTime: Boolean = true,
    val notifyBeforeTime: Boolean = true,
    val notifyBeforeMinutes: Int = 15,
    val soundOption: String = "Default", // Default, Chime, Silent
    val vibrationEnabled: Boolean = true,
    val fajrEnabled: Boolean = true,
    val sunriseEnabled: Boolean = false,
    val dhuhrEnabled: Boolean = true,
    val asrEnabled: Boolean = true,
    val maghribEnabled: Boolean = true,
    val ishaEnabled: Boolean = true,
    val lastSyncedDate: String = ""
)

@Entity(
    tableName = "salat_completions",
    indices = [
        Index(value = ["date", "prayerName"], unique = true),
        Index(value = ["date"])
    ]
)
data class SalatCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val prayerName: String, // Fajr, Dhuhr, Asr, Maghrib, Isha
    val isCompleted: Boolean,
    val completedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_prayer_times")
data class CachedPrayerTimesEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val locationName: String,
    val method: String,
    val madhab: String,
    val timeZoneId: String,
    val calculationTimestamp: Long = System.currentTimeMillis()
)



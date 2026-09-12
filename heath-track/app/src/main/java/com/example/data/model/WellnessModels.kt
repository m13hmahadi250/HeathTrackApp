package com.example.data.model

enum class ChecklistCategory(val label: String, val icon: String) {
    SALAT("Salat & Mindfulness", "🕌"),
    HYDRATION("Hydration", "💧"),
    NUTRITION("Nourishment", "🥗"),
    HABIT("Personal Habit", "⭐"),
    SLEEP("Rest & Recovery", "🌙"),
    MINDFULNESS("Mindfulness & Dhikr", "🧘")
}

data class DailyChecklistItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: ChecklistCategory,
    val isCompleted: Boolean,
    val actionRoute: String? = null // Can trigger action in ViewModel
)

data class DailyWellnessSummary(
    val date: String,
    val formattedDate: String,
    val wellnessScore: Int,
    val steps: Int,
    val stepTarget: Int,
    val waterMl: Int,
    val waterTargetMl: Int,
    val sleepMinutes: Int,
    val sleepTargetHours: Float,
    val mealsLoggedCount: Int,
    val salatCompletedCount: Int,
    val salatTotalObligatory: Int = 5,
    val habitsCompletedCount: Int,
    val habitsTotalCount: Int
) {
    val salatProgressPercent: Int get() = ((salatCompletedCount.toFloat() / salatTotalObligatory) * 100).toInt().coerceIn(0, 100)
    val waterProgressPercent: Int get() = if (waterTargetMl > 0) ((waterMl.toFloat() / waterTargetMl) * 100).toInt().coerceIn(0, 100) else 0
    val stepProgressPercent: Int get() = if (stepTarget > 0) ((steps.toFloat() / stepTarget) * 100).toInt().coerceIn(0, 100) else 0
    val sleepHours: Float get() = (sleepMinutes / 60f)
}

data class WeeklyWellnessTrend(
    val averageScore: Int,
    val averageWaterMl: Int,
    val averageSleepHours: Float,
    val totalWorkouts: Int,
    val salatConsistencyPercent: Int,
    val bestDayName: String
)

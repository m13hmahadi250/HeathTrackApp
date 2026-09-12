package com.example.ai

import com.example.data.local.*

data class WellnessInsight(
    val id: String,
    val category: String, // Hydration, Nutrition, Activity, Sleep, Habits, Mindset
    val title: String,
    val description: String,
    val actionLabel: String? = null,
    val isPositive: Boolean = true
)

object SmartInsightsEngine {

    fun generateInsights(
        profile: UserProfileEntity?,
        todayWaterMl: Int,
        todayMeals: List<MealLogEntity>,
        allMeals: List<MealLogEntity>,
        todaySleep: SleepLogEntity?,
        todaySteps: Int,
        habits: List<HabitEntity>,
        todayHabitLogs: List<HabitLogEntity>
    ): List<WellnessInsight> {
        val insights = mutableListOf<WellnessInsight>()

        // Hydration Insight
        val waterTarget = profile?.waterTargetMl ?: 2500
        val waterRatio = todayWaterMl.toFloat() / waterTarget.coerceAtLeast(1000)

        if (todayWaterMl > 0) {
            if (waterRatio >= 0.8f) {
                insights.add(
                    WellnessInsight(
                        id = "hydration_good",
                        category = "Hydration",
                        title = "Optimal Hydration Flow 💧",
                        description = "You have reached ${(waterRatio * 100).toInt()}% of your hydration goal today. Steady water intake supports mental clarity, energy, and joint health.",
                        isPositive = true
                    )
                )
            } else if (todayWaterMl < 1000) {
                insights.add(
                    WellnessInsight(
                        id = "hydration_remind",
                        category = "Hydration",
                        title = "Keep Your Hydration Flowing",
                        description = "You've logged ${todayWaterMl}ml so far. Enjoying a glass of cool or room-temperature water will refresh your energy.",
                        actionLabel = "Drink Water",
                        isPositive = false
                    )
                )
            } else {
                insights.add(
                    WellnessInsight(
                        id = "hydration_steady",
                        category = "Hydration",
                        title = "Good Hydration Progress",
                        description = "You've logged ${todayWaterMl}ml today. Keep a bottle nearby to sip steadily throughout the rest of your day.",
                        isPositive = true
                    )
                )
            }
        }

        // Nutrition & Variety Insight
        val veggieCount = allMeals.count { it.foodName.contains("Veg", ignoreCase = true) || it.foodName.contains("Salad", ignoreCase = true) }
        val proteinCount = allMeals.count { it.foodName.contains("Fish", ignoreCase = true) || it.foodName.contains("Chicken", ignoreCase = true) || it.foodName.contains("Egg", ignoreCase = true) || it.foodName.contains("Dal", ignoreCase = true) }
        val riceCount = allMeals.count { it.foodName.contains("Rice", ignoreCase = true) }

        if (todayMeals.isNotEmpty()) {
            if (riceCount > 4) {
                insights.add(
                    WellnessInsight(
                        id = "nutrition_variety",
                        category = "Nutrition",
                        title = "Food Variety Inspiration 🥗",
                        description = "You logged rice frequently this week. Consider pairing it with colorful seasonal vegetables, lentils (dal), or alternating with whole-grain rotis for varied fiber.",
                        actionLabel = "Explore Ideas",
                        isPositive = true
                    )
                )
            } else if (proteinCount > 3) {
                insights.add(
                    WellnessInsight(
                        id = "nutrition_protein",
                        category = "Nutrition",
                        title = "Balanced Protein Intake",
                        description = "Your recent meals include diverse protein sources like fish, eggs, and lentils, supporting recovery and muscle maintenance.",
                        isPositive = true
                    )
                )
            } else {
                insights.add(
                    WellnessInsight(
                        id = "nutrition_mindful",
                        category = "Nutrition",
                        title = "Mindful Nourishment",
                        description = "You have logged ${todayMeals.size} meal(s) today. Practicing mindful eating without rushing enhances both digestion and meal enjoyment.",
                        isPositive = true
                    )
                )
            }
        }

        // Activity Insight
        val stepTarget = profile?.stepTarget ?: 8000
        if (todaySteps >= stepTarget && todaySteps > 0) {
            insights.add(
                WellnessInsight(
                    id = "activity_goal",
                    category = "Activity",
                    title = "Movement Goal Achieved! 🏃",
                    description = "You logged $todaySteps steps today, exceeding your daily activity target! Consistent movement is key for longevity and vitality.",
                    isPositive = true
                )
            )
        } else if (todaySteps > 3000) {
            insights.add(
                WellnessInsight(
                    id = "activity_progress",
                    category = "Activity",
                    title = "Steady Movement Rhythm",
                    description = "You're at $todaySteps steps today. A relaxing 15-minute evening walk can comfortably bring you closer to your goal.",
                    actionLabel = "Start Walk",
                    isPositive = true
                )
            )
        }

        // Sleep Insight
        if (todaySleep != null && todaySleep.durationMinutes > 0) {
            val hours = todaySleep.durationMinutes / 60f
            if (hours >= 7f) {
                insights.add(
                    WellnessInsight(
                        id = "sleep_restful",
                        category = "Sleep",
                        title = "Restorative Sleep Pattern 🌙",
                        description = "You logged ${String.format("%.1f", hours)} hours of sleep. Adequate rest enhances hormone balance, cognitive focus, and physical recovery.",
                        isPositive = true
                    )
                )
            } else {
                insights.add(
                    WellnessInsight(
                        id = "sleep_recovery",
                        category = "Sleep",
                        title = "Prioritize Evening Wind-Down",
                        description = "Your recent sleep was ${String.format("%.1f", hours)} hours. Try dimming lights and putting away screens 30 minutes before your target bedtime.",
                        isPositive = false
                    )
                )
            }
        }

        // Habits Insight
        val completedHabitsCount = todayHabitLogs.count { it.isCompleted }
        if (completedHabitsCount > 0) {
            insights.add(
                WellnessInsight(
                    id = "habits_streak",
                    category = "Habits",
                    title = "Consistency Over Restriction ✨",
                    description = "You've checked off $completedHabitsCount healthy habit(s) today. Small daily actions compound into lasting wellness transformations.",
                    isPositive = true
                )
            )
        }

        if (insights.isEmpty()) {
            insights.add(
                WellnessInsight(
                    id = "clean_start",
                    category = "Welcome",
                    title = "Welcome to Heath Track 🌿",
                    description = "No logs recorded yet for today. Track your water, log a meal, or connect health sensors to see real-time insights.",
                    actionLabel = "Start Tracking",
                    isPositive = true
                )
            )
        }

        return insights
    }

    fun calculateWellnessScore(
        waterRatio: Float, // 0 to 1+
        stepRatio: Float,  // 0 to 1+
        mealsLogged: Int,  // target 3
        sleepHours: Float, // target 7-8h
        habitsCompletedRatio: Float // 0 to 1
    ): Int {
        if (waterRatio == 0f && stepRatio == 0f && mealsLogged == 0 && sleepHours == 0f && habitsCompletedRatio == 0f) {
            return 0
        }
        val waterScore = (waterRatio.coerceIn(0f, 1f) * 20).toInt()
        val stepScore = (stepRatio.coerceIn(0f, 1f) * 20).toInt()
        val mealScore = ((mealsLogged.coerceIn(0, 3) / 3f) * 20).toInt()
        val sleepScore = if (sleepHours in 6.5f..9f) 20 else if (sleepHours > 0f) 14 else 0
        val habitScore = (habitsCompletedRatio.coerceIn(0f, 1f) * 20).toInt()

        return (waterScore + stepScore + mealScore + sleepScore + habitScore).coerceIn(0, 100)
    }
}

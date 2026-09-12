package com.example.reminder

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WorkManagerNotificationScheduler {

    private const val TAG_HYDRATION_PERIODIC = "work_vitaflow_hydration_periodic"
    private const val TAG_MEAL_BREAKFAST = "work_vitaflow_meal_breakfast"
    private const val TAG_MEAL_LUNCH = "work_vitaflow_meal_lunch"
    private const val TAG_MEAL_DINNER = "work_vitaflow_meal_dinner"

    /**
     * Initializes and enqueues all WorkManager background notification schedules.
     */
    fun scheduleAllHealthReminders(context: Context) {
        scheduleHydrationReminders(context)
        scheduleMealReminders(context)
        HealthBackgroundSyncWorker.schedulePeriodicSync(context)
    }

    /**
     * Schedules periodic daytime hydration reminders (every 2.5 hours) using WorkManager.
     */
    fun scheduleHydrationReminders(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val hydrationRequest = PeriodicWorkRequestBuilder<HydrationReminderWorker>(
            repeatInterval = 150, // 2.5 hours
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
            flexTimeInterval = 30,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(TAG_HYDRATION_PERIODIC)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TAG_HYDRATION_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            hydrationRequest
        )
    }

    /**
     * Schedules daily meal logging push notifications at standard meal hours:
     * - Breakfast at 08:30
     * - Lunch at 12:45
     * - Dinner at 19:30
     */
    fun scheduleMealReminders(context: Context) {
        scheduleMealAtHour(context, targetHour = 8, targetMinute = 30, mealType = "Breakfast", tag = TAG_MEAL_BREAKFAST)
        scheduleMealAtHour(context, targetHour = 12, targetMinute = 45, mealType = "Lunch", tag = TAG_MEAL_LUNCH)
        scheduleMealAtHour(context, targetHour = 19, targetMinute = 30, mealType = "Dinner", tag = TAG_MEAL_DINNER)
    }

    private fun scheduleMealAtHour(
        context: Context,
        targetHour: Int,
        targetMinute: Int,
        mealType: String,
        tag: String
    ) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialDelay = target.timeInMillis - now.timeInMillis

        val mealData = workDataOf("MEAL_TYPE" to mealType)

        val mealRequest = OneTimeWorkRequestBuilder<MealReminderWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(mealData)
            .addTag(tag)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            tag,
            ExistingWorkPolicy.REPLACE,
            mealRequest
        )
    }

    /**
     * Triggers an immediate one-off notification for testing or immediate hydration/meal prompt.
     */
    fun triggerImmediateCheckIn(context: Context, type: String = "Water") {
        if (type.equals("Meal", ignoreCase = true)) {
            val req = OneTimeWorkRequestBuilder<MealReminderWorker>()
                .setInputData(workDataOf("MEAL_TYPE" to "Lunch"))
                .build()
            WorkManager.getInstance(context).enqueue(req)
        } else {
            val req = OneTimeWorkRequestBuilder<HydrationReminderWorker>().build()
            WorkManager.getInstance(context).enqueue(req)
        }
    }

    /**
     * Cancels all scheduled reminder tasks.
     */
    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(TAG_HYDRATION_PERIODIC)
        wm.cancelUniqueWork(TAG_MEAL_BREAKFAST)
        wm.cancelUniqueWork(TAG_MEAL_LUNCH)
        wm.cancelUniqueWork(TAG_MEAL_DINNER)
    }
}

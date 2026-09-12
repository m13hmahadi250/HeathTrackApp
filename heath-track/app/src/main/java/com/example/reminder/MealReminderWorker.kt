package com.example.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.local.NotificationEntity
import com.example.data.local.VitaFlowDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * WorkManager worker that schedules and issues meal logging prompts at mindful meal windows.
 */
class MealReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            ReminderScheduler.createNotificationChannels(applicationContext)

            val db = VitaFlowDatabase.getInstance(applicationContext)
            val profile = db.userProfileDao().getUserProfileOnce()

            // If user disabled notifications, skip
            if (profile != null && !profile.notificationsEnabled) {
                return@withContext Result.success()
            }

            val mealType = inputData.getString("MEAL_TYPE") ?: run {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                when {
                    hour in 6..10 -> "Breakfast"
                    hour in 11..15 -> "Lunch"
                    hour in 17..21 -> "Dinner"
                    else -> "Meal"
                }
            }

            val (title, message) = when (mealType) {
                "Breakfast" -> Pair(
                    "Mindful Breakfast 🥣",
                    "Take a moment to enjoy and log your morning meal for steady energy throughout the day."
                )
                "Lunch" -> Pair(
                    "Midday Nourishment 🥗",
                    "Time for lunch! Log your meal to track your protein, carbs, and fiber balance."
                )
                "Dinner" -> Pair(
                    "Evening Meal Check-in 🍲",
                    "A nourishing, balanced dinner supports restorative sleep. Remember to log your meal!"
                )
                else -> Pair(
                    "Nourishment Log 🍎",
                    "Consistency over restriction: gentle check-in to log your recent meal or snack."
                )
            }

            // 1. Post Android System Notification
            if (ReminderScheduler.areNotificationsEnabled(applicationContext)) {
                val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val notificationId = when (mealType) {
                    "Breakfast" -> 2001
                    "Lunch" -> 2002
                    "Dinner" -> 2003
                    else -> 2004
                }
                val pendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    notificationId,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_MEALS)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                try {
                    NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
                } catch (_: SecurityException) {}
            }

            // 2. Persist in Room Database Notification inbox
            db.notificationDao().insertNotification(
                NotificationEntity(
                    title = title,
                    message = message,
                    type = "Meal",
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
            )

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

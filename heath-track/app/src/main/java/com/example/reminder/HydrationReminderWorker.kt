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
import java.time.LocalDate
import java.util.Calendar

/**
 * WorkManager worker that schedules and issues periodic local push notifications
 * for hydration check-ins throughout the user's day.
 */
class HydrationReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            ReminderScheduler.createNotificationChannels(applicationContext)

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            // Only send notifications during waking/active hours (8 AM - 10 PM)
            if (hour < 8 || hour > 22) {
                return@withContext Result.success()
            }

            val db = VitaFlowDatabase.getInstance(applicationContext)
            val today = LocalDate.now().toString()
            val profile = db.userProfileDao().getUserProfileOnce()
            
            // If user disabled notifications, skip
            if (profile != null && !profile.notificationsEnabled) {
                return@withContext Result.success()
            }

            val targetMl = profile?.waterTargetMl ?: 2500
            val todayLogs = db.waterLogDao().getWaterLogsBetweenDates(today, today)
            // Or calculate total
            // Since getWaterLogsBetweenDates returns a Flow, we can query once or compute
            // Construct gentle push notification
            val title = "Hydration Check-in 💧"
            val message = when {
                hour in 8..11 -> "Start your morning feeling refreshed with a tall glass of water!"
                hour in 12..15 -> "Keep your afternoon energy and focus sharp — remember to take a sip."
                hour in 16..19 -> "Stay hydrated before your evening routine. How is your water goal today?"
                else -> "Gentle evening hydration: a small glass of water helps restorative recovery."
            }

            // 1. Post Android System Notification
            if (ReminderScheduler.areNotificationsEnabled(applicationContext)) {
                val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    1001,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_WATER)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                try {
                    NotificationManagerCompat.from(applicationContext).notify(1001, notification)
                } catch (_: SecurityException) {}
            }

            // 2. Persist in Room Database Notification inbox
            db.notificationDao().insertNotification(
                NotificationEntity(
                    title = title,
                    message = message,
                    type = "Water",
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

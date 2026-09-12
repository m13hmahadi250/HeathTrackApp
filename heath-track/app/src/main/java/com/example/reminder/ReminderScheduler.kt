package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.NotificationEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.VitaFlowDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object ReminderScheduler {
    const val CHANNEL_WATER = "channel_vitaflow_water"
    const val CHANNEL_MEALS = "channel_vitaflow_meals"
    const val CHANNEL_EXERCISE = "channel_vitaflow_exercise"
    const val CHANNEL_SLEEP = "channel_vitaflow_sleep"
    const val CHANNEL_HABIT = "channel_vitaflow_habit"
    const val CHANNEL_CUSTOM = "channel_vitaflow_custom"
    const val CHANNEL_SALAT = "channel_vitaflow_salat"

    fun areNotificationsEnabled(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(
                NotificationChannel(
                    CHANNEL_WATER,
                    "Water Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Gentle prompts to drink water and maintain healthy hydration"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_MEALS,
                    "Meal Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Mindful mealtime and nutrition balance reminders"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_EXERCISE,
                    "Exercise Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Movement, posture, walking, and workout check-ins"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_SLEEP,
                    "Sleep Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Bedtime wind-down and restorative sleep alerts"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_HABIT,
                    "Habit Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily micro-habits and streak check-ins"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_CUSTOM,
                    "Custom Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Personalized and custom scheduled alerts"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_SALAT,
                    "Salat & Prayer Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Prayer times and upcoming salat notifications"
                    enableVibration(true)
                }
            )

            notificationManager.createNotificationChannels(channels)
        }
    }

    fun scheduleReminder(context: Context, reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (!reminder.isEnabled) {
            cancelReminder(context, reminder)
            return
        }

        val parts = reminder.time.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("REMINDER_ID", reminder.id)
            putExtra("REMINDER_TITLE", reminder.title)
            putExtra("REMINDER_TYPE", reminder.type)
            putExtra("REMINDER_TIME", reminder.time)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            if (canExact) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun triggerImmediateNotification(
        context: Context,
        title: String,
        message: String,
        type: String = "Water"
    ) {
        val channelId = when (type.lowercase()) {
            "water" -> CHANNEL_WATER
            "meal" -> CHANNEL_MEALS
            "exercise" -> CHANNEL_EXERCISE
            "sleep" -> CHANNEL_SLEEP
            "habit" -> CHANNEL_HABIT
            else -> CHANNEL_CUSTOM
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (areNotificationsEnabled(context)) {
            val notificationManager = NotificationManagerCompat.from(context)
            try {
                notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
            } catch (_: SecurityException) {
            }
        }
    }
}

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderTitle = intent.getStringExtra("REMINDER_TITLE") ?: "HeathTrack Reminder"
        val reminderType = intent.getStringExtra("REMINDER_TYPE") ?: "Water"
        val reminderId = intent.getLongExtra("REMINDER_ID", 1L)
        val reminderTime = intent.getStringExtra("REMINDER_TIME")

        val channelId = when (reminderType.lowercase()) {
            "water" -> ReminderScheduler.CHANNEL_WATER
            "meal" -> ReminderScheduler.CHANNEL_MEALS
            "exercise" -> ReminderScheduler.CHANNEL_EXERCISE
            "sleep" -> ReminderScheduler.CHANNEL_SLEEP
            "habit" -> ReminderScheduler.CHANNEL_HABIT
            else -> ReminderScheduler.CHANNEL_CUSTOM
        }

        val message = when (reminderType.lowercase()) {
            "water" -> "Time for a refreshing glass of water! Keep your hydration balance steady."
            "meal" -> "A mindful reminder to enjoy a balanced, nourishing meal."
            "exercise" -> "Take a brief walk, stretch, or move your body for natural vitality."
            "sleep" -> "Begin winding down for restorative, consistent sleep tonight."
            "habit" -> "Check in on your healthy daily micro-habits and maintain your streak."
            else -> "Consistency over restriction: gentle check-in on your wellness goals."
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(reminderTitle)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ReminderScheduler.areNotificationsEnabled(context)) {
            val notificationManager = NotificationManagerCompat.from(context)
            try {
                notificationManager.notify(reminderId.toInt(), notification)
            } catch (_: SecurityException) {
            }
        }

        // Record into In-App Notification Center and re-arm recurring reminder for next day
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = VitaFlowDatabase.getInstance(context)
                db.notificationDao().insertNotification(
                    NotificationEntity(
                        title = reminderTitle,
                        message = message,
                        type = reminderType,
                        timestamp = System.currentTimeMillis()
                    )
                )

                // Re-arm for tomorrow
                if (reminderTime != null) {
                    val existing = db.reminderDao().getReminderById(reminderId)
                    if (existing != null && existing.isEnabled) {
                        ReminderScheduler.scheduleReminder(context, existing)
                    }
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = VitaFlowDatabase.getInstance(context)
                    val reminders = db.reminderDao().getEnabledRemindersOnce()
                    reminders.forEach { reminder ->
                        ReminderScheduler.scheduleReminder(context, reminder)
                    }
                    HealthBackgroundSyncWorker.schedulePeriodicSync(context)
                } catch (_: Exception) {
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}


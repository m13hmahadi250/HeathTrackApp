package com.example.salat

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.NotificationEntity
import com.example.data.local.VitaFlowDatabase
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class SalatReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
        val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
        val isBefore = intent.getBooleanExtra(EXTRA_IS_BEFORE, false)
        val beforeMinutes = intent.getIntExtra(EXTRA_BEFORE_MINUTES, 15)
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, 7000)
        val soundOption = intent.getStringExtra(EXTRA_SOUND_OPTION) ?: "Default"
        val vibrationEnabled = intent.getBooleanExtra(EXTRA_VIBRATION_ENABLED, true)

        val formattedTime = if (prayerTime.isNotBlank()) PrayerTimeCalculator.formatTo12Hour(prayerTime) else ""

        val title = if (isBefore) {
            "Upcoming: $prayerName Prayer in $beforeMinutes min"
        } else {
            "Salat Time: $prayerName ($formattedTime)"
        }

        val message = if (isBefore) {
            "Take a mindful moment to prepare for $prayerName prayer. Reconnect, perform wudu, and find tranquility."
        } else {
            "It is now time for $prayerName prayer ($formattedTime). Pause your busy day and come to prayer."
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "SALAT")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_SALAT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (soundOption == "Silent") {
            builder.setSilent(true)
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
        }

        if (vibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 300, 200, 300))
        } else {
            builder.setVibrate(longArrayOf(0))
        }

        val notification = builder.build()

        if (ReminderScheduler.areNotificationsEnabled(context)) {
            val notificationManager = NotificationManagerCompat.from(context)
            try {
                notificationManager.notify(reminderId, notification)
            } catch (_: SecurityException) {
            }
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = VitaFlowDatabase.getInstance(context)
                db.notificationDao().insertNotification(
                    NotificationEntity(
                        title = title,
                        message = message,
                        type = "Salat",
                        timestamp = System.currentTimeMillis()
                    )
                )

                // Reschedule for next day / refresh schedule
                val config = db.salatDao().getSalatConfigOnce()
                if (config != null && config.isSalatEnabled) {
                    val schedule = PrayerTimeCalculator.calculateSchedule(
                        date = LocalDate.now(),
                        latitude = config.latitude,
                        longitude = config.longitude,
                        timeZoneId = config.timeZoneId,
                        method = config.calculationMethod,
                        madhab = config.madhab,
                        locationName = config.locationName
                    )
                    PrayerAlarmScheduler.schedulePrayerReminders(context, schedule, config)
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
        const val EXTRA_IS_BEFORE = "extra_is_before"
        const val EXTRA_BEFORE_MINUTES = "extra_before_minutes"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_SOUND_OPTION = "extra_sound_option"
        const val EXTRA_VIBRATION_ENABLED = "extra_vibration_enabled"
    }
}

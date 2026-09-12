package com.example.salat

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.SalatConfigEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object PrayerAlarmScheduler {

    private const val TAG = "PrayerAlarmScheduler"

    // Base request codes
    private const val CODE_AT_TIME_BASE = 7100
    private const val CODE_BEFORE_TIME_BASE = 7200

    private val obligatoryPrayers = listOf(
        PrayerName.FAJR,
        PrayerName.DHUHR,
        PrayerName.ASR,
        PrayerName.MAGHRIB,
        PrayerName.ISHA
    )

    fun schedulePrayerReminders(
        context: Context,
        todaySchedule: SalatSchedule,
        config: SalatConfigEntity
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        if (!config.isSalatEnabled || (!config.notifyAtTime && !config.notifyBeforeTime)) {
            cancelAllPrayerReminders(context)
            return
        }

        val zoneId = try {
            ZoneId.of(config.timeZoneId)
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }

        val now = LocalDateTime.now(zoneId)
        val today = LocalDate.now(zoneId)
        val tomorrow = today.plusDays(1)

        val tomorrowSchedule = PrayerTimeCalculator.calculateSchedule(
            date = tomorrow,
            latitude = config.latitude,
            longitude = config.longitude,
            timeZoneId = config.timeZoneId,
            method = config.calculationMethod,
            madhab = config.madhab,
            locationName = config.locationName
        )

        obligatoryPrayers.forEachIndexed { index, prayer ->
            val isPrayerEnabled = when (prayer) {
                PrayerName.FAJR -> config.fajrEnabled
                PrayerName.DHUHR -> config.dhuhrEnabled
                PrayerName.ASR -> config.asrEnabled
                PrayerName.MAGHRIB -> config.maghribEnabled
                PrayerName.ISHA -> config.ishaEnabled
                else -> true
            }

            val atCode = CODE_AT_TIME_BASE + index
            val beforeCode = CODE_BEFORE_TIME_BASE + index

            if (!isPrayerEnabled) {
                cancelAlarm(context, alarmManager, atCode)
                cancelAlarm(context, alarmManager, beforeCode)
                return@forEachIndexed
            }

            val todayTimeStr = todaySchedule.getTimeForPrayer(prayer)
            val tomorrowTimeStr = tomorrowSchedule.getTimeForPrayer(prayer)

            val todayDateTime = parseDateTime(today, todayTimeStr)
            val tomorrowDateTime = parseDateTime(tomorrow, tomorrowTimeStr)

            // Target DateTime for the prayer: if today's prayer is in the future, use today; otherwise tomorrow
            val targetDateTime = if (todayDateTime.isAfter(now)) todayDateTime else tomorrowDateTime
            val targetTimeStr = if (todayDateTime.isAfter(now)) todayTimeStr else tomorrowTimeStr

            val targetMillis = targetDateTime.atZone(zoneId).toInstant().toEpochMilli()

            // 1. At Prayer Time Alarm
            if (config.notifyAtTime) {
                if (targetMillis > System.currentTimeMillis()) {
                    scheduleExactAlarm(
                        context = context,
                        alarmManager = alarmManager,
                        triggerAtMillis = targetMillis,
                        requestCode = atCode,
                        prayerName = prayer.displayName,
                        prayerTime = targetTimeStr,
                        isBefore = false,
                        beforeMinutes = 0,
                        soundOption = config.soundOption,
                        vibrationEnabled = config.vibrationEnabled
                    )
                }
            } else {
                cancelAlarm(context, alarmManager, atCode)
            }

            // 2. Before Prayer Time Alarm
            if (config.notifyBeforeTime && config.notifyBeforeMinutes > 0) {
                val beforeMillis = targetMillis - (config.notifyBeforeMinutes * 60 * 1000L)
                if (beforeMillis > System.currentTimeMillis()) {
                    scheduleExactAlarm(
                        context = context,
                        alarmManager = alarmManager,
                        triggerAtMillis = beforeMillis,
                        requestCode = beforeCode,
                        prayerName = prayer.displayName,
                        prayerTime = targetTimeStr,
                        isBefore = true,
                        beforeMinutes = config.notifyBeforeMinutes,
                        soundOption = config.soundOption,
                        vibrationEnabled = config.vibrationEnabled
                    )
                }
            } else {
                cancelAlarm(context, alarmManager, beforeCode)
            }
        }
    }

    private fun scheduleExactAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        requestCode: Int,
        prayerName: String,
        prayerTime: String,
        isBefore: Boolean,
        beforeMinutes: Int,
        soundOption: String = "Default",
        vibrationEnabled: Boolean = true
    ) {
        val intent = Intent(context, SalatReminderReceiver::class.java).apply {
            putExtra(SalatReminderReceiver.EXTRA_PRAYER_NAME, prayerName)
            putExtra(SalatReminderReceiver.EXTRA_PRAYER_TIME, prayerTime)
            putExtra(SalatReminderReceiver.EXTRA_IS_BEFORE, isBefore)
            putExtra(SalatReminderReceiver.EXTRA_BEFORE_MINUTES, beforeMinutes)
            putExtra(SalatReminderReceiver.EXTRA_REMINDER_ID, requestCode)
            putExtra(SalatReminderReceiver.EXTRA_SOUND_OPTION, soundOption)
            putExtra(SalatReminderReceiver.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule exact prayer alarm for $prayerName: ${e.message}")
        }
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, SalatReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun cancelAllPrayerReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (i in 0..10) {
            cancelAlarm(context, alarmManager, CODE_AT_TIME_BASE + i)
            cancelAlarm(context, alarmManager, CODE_BEFORE_TIME_BASE + i)
        }
    }

    private fun parseDateTime(date: LocalDate, timeStr: String): LocalDateTime {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return date.atTime(LocalTime.of(h, m))
    }
}

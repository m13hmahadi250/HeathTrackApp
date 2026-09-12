package com.example.reminder

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.VitaFlowApp
import com.example.data.local.VitaFlowDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class HealthBackgroundSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val app = applicationContext as? VitaFlowApp
            val today = LocalDate.now().toString()

            // 1. Sync Health Connect if user has authorized and connected it
            app?.healthRepository?.let { healthRepo ->
                if (healthRepo.isConnected.value && healthRepo.isAvailable()) {
                    healthRepo.syncNow(today)
                }
            }

            // 2. Ensure all enabled reminders are verified and active
            val db = VitaFlowDatabase.getInstance(applicationContext)
            val enabledReminders = db.reminderDao().getEnabledRemindersOnce()
            for (reminder in enabledReminders) {
                ReminderScheduler.scheduleReminder(applicationContext, reminder)
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "vitaflow_background_health_sync"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<HealthBackgroundSyncWorker>(
                repeatInterval = 3,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }
}

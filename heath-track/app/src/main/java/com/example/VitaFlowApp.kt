package com.example

import android.app.Application
import com.example.data.auth.FirebaseAuthManager
import com.example.data.health.HealthRepository
import com.example.data.health.HealthRepositoryImpl
import com.example.data.local.VitaFlowDatabase
import com.example.data.repository.VitaFlowRepository
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VitaFlowApp : Application() {
    lateinit var database: VitaFlowDatabase
        private set

    lateinit var repository: VitaFlowRepository
        private set

    lateinit var healthRepository: HealthRepository
        private set

    lateinit var authManager: FirebaseAuthManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        authManager = FirebaseAuthManager(this)
        database = VitaFlowDatabase.getInstance(this)
        repository = VitaFlowRepository(database)
        healthRepository = HealthRepositoryImpl(this, database.healthSyncDao(), database.sleepLogDao())

        // Connect current user if already logged in
        authManager.getUid()?.let { uid ->
            repository.currentUserId = uid
        }

        ReminderScheduler.createNotificationChannels(this)

        // Ensure active reminders are scheduled and food catalogue seeded
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.ensureCatalogSeeded()
            } catch (_: Exception) {}

            val enabledReminders = repository.getEnabledRemindersOnce()
            enabledReminders.forEach { reminder ->
                ReminderScheduler.scheduleReminder(this@VitaFlowApp, reminder)
            }
        }
    }

    companion object {
        lateinit var instance: VitaFlowApp
            private set
    }
}

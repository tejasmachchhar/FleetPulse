package com.fleetpulse.app

import android.app.Application
import androidx.room.Room
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fleetpulse.app.data.db.FleetDatabase
import com.fleetpulse.app.data.prefs.AppPreferences
import com.fleetpulse.app.data.repo.FleetRepository
import com.fleetpulse.app.notifications.ReminderWorker
import java.util.concurrent.TimeUnit

class FleetPulseApp : Application() {

    lateinit var database: FleetDatabase
        private set
    lateinit var repository: FleetRepository
        private set
    lateinit var preferences: AppPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, FleetDatabase::class.java, FleetDatabase.NAME)
            .addMigrations(FleetDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
        repository = FleetRepository(database)
        preferences = AppPreferences(this)
        ReminderWorker.ensureChannel(this)
        ReminderWorker.schedulePeriodic(this)
    }

    /** Used by tests / diagnostics to verify the reminder job is enqueued. */
    fun scheduleReminderWorkNow() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("fleetpulse-service-check", ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

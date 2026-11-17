package com.fleetpulse.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fleetpulse.app.FleetPulseApp
import com.fleetpulse.app.MainActivity
import com.fleetpulse.app.R
import com.fleetpulse.app.data.db.SettingsEntity
import com.fleetpulse.app.domain.CalcEngine
import com.fleetpulse.app.domain.EngineState
import com.fleetpulse.app.domain.EvalOptions
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Daily service-reminder check. The odometer used for km-based reminders is
 * the latest odometer the user has logged in a usage entry — the app never
 * pretends to know the real odometer by itself.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? FleetPulseApp ?: return Result.success()
        if (!app.preferences.state.first().notificationsEnabled) return Result.success()

        val db = app.database
        val state = EngineState(
            vehicles = db.vehicleDao().getAll(),
            usageEntries = db.usageDao().getAll(),
            maintenanceEntries = db.maintenanceDao().getAll(),
            priceHistory = db.priceDao().getAll(),
            calibration = db.calibrationDao().getAll(),
            settings = db.settingsDao().get() ?: SettingsEntity(),
        )
        val today = LocalDate.now()
        val result = CalcEngine.evaluate(state, EvalOptions(referenceDate = today))

        for (item in result.evaluatedMaintenance) {
            if (!item.pending) continue
            if (item.status != "Overdue" && item.status != "Due Soon") continue
            val vehicle = state.vehicles.firstOrNull { it.id == item.entry.vehicleId } ?: continue
            if (vehicle.archived) continue

            val dueParts = buildList {
                item.entry.nextDueDate?.let { add("date $it") }
                item.entry.nextDueKm?.let { add("${it.toInt()} km") }
            }
            val detail = when {
                item.status == "Overdue" -> "is overdue"
                else -> "is due soon"
            }
            val title = "${vehicle.name}: ${item.entry.category} $detail"
            val body = buildString {
                append(if (item.status == "Overdue") "Overdue since " else "Due ")
                append(dueParts.joinToString(" · "))
                if (item.daysLeft != null) {
                    append(" (${abs(item.daysLeft!!)} days ${if (item.daysLeft!! < 0) "ago" else "left"})")
                }
                if (item.kmLeft != null) {
                    append(" · ${item.kmLeft!!.toInt()} km ${if (item.kmLeft!! < 0) "past" else "left"}")
                }
            }
            notify(vehicle.id, item.entry.id, title, body)
        }
        return Result.success()
    }

    private fun notify(vehicleId: String, entryId: String, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate", "maintenance")
        }
        val pi = PendingIntent.getActivity(
            applicationContext,
            (vehicleId + entryId).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_splash)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching {
            NotificationManagerCompat.from(applicationContext)
                .notify(("$vehicleId-$entryId").hashCode(), notification)
        }
    }

    companion object {
        const val CHANNEL_ID = "service_reminders"
        private const val WORK_NAME = "fleetpulse-service-check"

        fun ensureChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_service),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notif_channel_service_desc)
            }
            manager.createNotificationChannel(channel)
        }

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

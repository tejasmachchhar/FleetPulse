package com.fleetpulse.app.data.repo

import androidx.room.withTransaction
import com.fleetpulse.app.data.db.CalibrationPoint
import com.fleetpulse.app.data.db.FleetDatabase
import com.fleetpulse.app.data.db.MaintenanceEntry
import com.fleetpulse.app.data.db.MetaEntity
import com.fleetpulse.app.data.db.PriceEntry
import com.fleetpulse.app.data.db.SettingsEntity
import com.fleetpulse.app.data.db.UsageEntry
import com.fleetpulse.app.data.db.Vehicle
import com.fleetpulse.app.domain.EngineState
import com.fleetpulse.app.domain.SampleData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class FleetRepository(private val db: FleetDatabase) {

    val vehicles: Flow<List<Vehicle>> = db.vehicleDao().observeAll()
    val usageEntries: Flow<List<UsageEntry>> = db.usageDao().observeAll()
    val maintenanceEntries: Flow<List<MaintenanceEntry>> = db.maintenanceDao().observeAll()
    val priceHistory: Flow<List<PriceEntry>> = db.priceDao().observeAll()
    val calibration: Flow<List<CalibrationPoint>> = db.calibrationDao().observeAll()
    val settings: Flow<SettingsEntity> = db.settingsDao().observe().map { it ?: SettingsEntity() }

    val state: Flow<EngineState> = combine(
        combine(vehicles, usageEntries, maintenanceEntries) { v, u, m -> Triple(v, u, m) },
        combine(priceHistory, calibration, settings) { p, c, s -> Triple(p, c, s) },
    ) { (v, u, m), (p, c, s) ->
        EngineState(
            vehicles = v,
            usageEntries = u,
            maintenanceEntries = m,
            priceHistory = p,
            calibration = c,
            settings = s,
        )
    }

    suspend fun ensureSettings(default: SettingsEntity = SettingsEntity()) {
        if (db.settingsDao().get() == null) db.settingsDao().upsert(default)
    }

    suspend fun saveSettings(settings: SettingsEntity) = db.settingsDao().upsert(settings)

    // Vehicles -----------------------------------------------------------
    suspend fun saveVehicle(vehicle: Vehicle) = db.vehicleDao().upsert(vehicle)
    suspend fun setVehicleArchived(id: String, archived: Boolean) = db.vehicleDao().setArchived(id, archived)
    suspend fun deleteVehicle(id: String) = db.withTransaction { db.vehicleDao().deleteById(id) }
    suspend fun vehicleCount(): Int = db.vehicleDao().count()

    // Usage --------------------------------------------------------------
    suspend fun saveUsage(entry: UsageEntry) = db.usageDao().upsert(entry)
    suspend fun deleteUsage(id: String) = db.usageDao().deleteById(id)
    suspend fun sampleUsageCount(): Int = db.usageDao().sampleCount()

    // Maintenance --------------------------------------------------------
    suspend fun saveMaintenance(entry: MaintenanceEntry) = db.maintenanceDao().upsert(entry)
    suspend fun deleteMaintenance(id: String) = db.maintenanceDao().deleteById(id)

    // Prices -------------------------------------------------------------
    suspend fun savePrice(entry: PriceEntry) = db.priceDao().upsert(entry)
    suspend fun deletePrice(id: String) = db.priceDao().deleteById(id)

    // Calibration --------------------------------------------------------
    suspend fun saveCalibration(point: CalibrationPoint) = db.calibrationDao().upsert(point)
    suspend fun deleteCalibration(id: String) = db.calibrationDao().deleteById(id)

    /**
     * Loads the reference workbook demo data. Only inserts records; existing
     * user records with the same IDs are never overwritten.
     */
    suspend fun loadDemoData() = db.withTransaction {
        val existingVehicles = db.vehicleDao().getAll().associateBy { it.id }.toMutableMap()
        val toInsert = SampleData.vehicles().filter { it.id !in existingVehicles }
        db.vehicleDao().upsertAll(toInsert)

        val existingUsage = db.usageDao().getAll().map { it.id }.toSet()
        db.usageDao().upsertAll(SampleData.usageEntries().filter { it.id !in existingUsage })

        val existingMaint = db.maintenanceDao().getAll().map { it.id }.toSet()
        db.maintenanceDao().upsertAll(SampleData.maintenanceEntries().filter { it.id !in existingMaint })

        val existingPrices = db.priceDao().getAll().map { it.id }.toSet()
        db.priceDao().upsertAll(SampleData.priceHistory().filter { it.id !in existingPrices })

        val existingCal = db.calibrationDao().getAll().map { it.id }.toSet()
        db.calibrationDao().upsertAll(SampleData.calibration().filter { it.id !in existingCal })

        if (db.settingsDao().get() == null) db.settingsDao().upsert(SampleData.settings())
    }

    /**
     * Removes only records flagged as samples. Genuine usage / service
     * records are preserved. Demo vehicles are removed only when no genuine
     * record still references them.
     */
    suspend fun removeSampleData() = db.withTransaction {
        db.usageDao().deleteSamples()
        db.maintenanceDao().deleteSamples()
        db.calibrationDao().deleteSamples()
        db.vehicleDao().deleteSampleVehiclesWithoutUserData()
        // Sample tariffs are kept while genuine usage still depends on them so
        // historical costs never change retroactively.
        if (db.usageDao().getAll().isEmpty()) {
            db.priceDao().deleteSamples()
        }
    }

    // Full backup / restore ---------------------------------------------
    suspend fun snapshotUsage(): List<UsageEntry> = db.usageDao().getAll()
    suspend fun snapshotMaintenance(): List<MaintenanceEntry> = db.maintenanceDao().getAll()
    suspend fun snapshotPrices(): List<PriceEntry> = db.priceDao().getAll()
    suspend fun snapshotCalibration(): List<CalibrationPoint> = db.calibrationDao().getAll()
    suspend fun snapshotVehicles(): List<Vehicle> = db.vehicleDao().getAll()
    suspend fun snapshotSettings(): SettingsEntity = db.settingsDao().get() ?: SettingsEntity()

    /** Replaces the entire local dataset in a single transaction. */
    suspend fun restoreAll(
        vehicles: List<Vehicle>,
        usage: List<UsageEntry>,
        maintenance: List<MaintenanceEntry>,
        prices: List<PriceEntry>,
        calibration: List<CalibrationPoint>,
        settings: SettingsEntity,
    ) = db.withTransaction {
        db.usageDao().deleteAll()
        db.maintenanceDao().deleteAll()
        db.priceDao().deleteAll()
        db.calibrationDao().deleteAll()
        db.vehicleDao().deleteAll()
        db.vehicleDao().upsertAll(vehicles)
        db.usageDao().upsertAll(usage)
        db.maintenanceDao().upsertAll(maintenance)
        db.priceDao().upsertAll(prices)
        db.calibrationDao().upsertAll(calibration)
        db.settingsDao().upsert(settings)
        db.metaDao().upsert(MetaEntity("restoredAt", System.currentTimeMillis().toString()))
    }
}

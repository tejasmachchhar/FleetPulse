package com.fleetpulse.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fleetpulse.app.FleetPulseApp
import com.fleetpulse.app.data.backup.BackupBuildInput
import com.fleetpulse.app.data.backup.BackupManager
import com.fleetpulse.app.data.backup.BackupPayload
import com.fleetpulse.app.data.db.CalibrationPoint
import com.fleetpulse.app.data.db.FILL_FULL_CHARGE
import com.fleetpulse.app.data.db.FILL_FULL_TANK
import com.fleetpulse.app.data.db.MaintenanceEntry
import com.fleetpulse.app.data.db.PriceEntry
import com.fleetpulse.app.data.db.SOURCE_ELECTRICITY
import com.fleetpulse.app.data.db.SettingsEntity
import com.fleetpulse.app.data.db.UsageEntry
import com.fleetpulse.app.data.db.Vehicle
import com.fleetpulse.app.data.prefs.AppPreferencesState
import com.fleetpulse.app.data.repo.FleetRepository
import com.fleetpulse.app.domain.CalcEngine
import com.fleetpulse.app.domain.EngineState
import com.fleetpulse.app.domain.EvalOptions
import com.fleetpulse.app.domain.EvalResult
import com.fleetpulse.app.domain.SampleData
import com.fleetpulse.app.domain.VerificationResult
import com.fleetpulse.app.domain.VerificationSuite
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardFilter(
    val vehicleId: String = "ALL",
    val periodKey: String = "all",
    val customFrom: String = "2026-01-01",
    val customTo: String = "2027-12-31",
)

data class UiSnapshot(
    val state: EngineState,
    val evaluation: EvalResult,
    val filter: DashboardFilter,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FleetPulseApp
    private val repo: FleetRepository = app.repository

    val preferences: StateFlow<AppPreferencesState> = app.preferences.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferencesState())

    val filter = MutableStateFlow(DashboardFilter())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    val snapshot: StateFlow<UiSnapshot?> = combine(repo.state, filter) { state, f ->
        UiSnapshot(
            state = state,
            evaluation = CalcEngine.evaluate(
                state,
                EvalOptions(
                    referenceDate = LocalDate.now(),
                    periodKey = f.periodKey,
                    customDateFrom = f.customFrom,
                    customDateTo = f.customTo,
                ),
            ),
            filter = f,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val rawState: StateFlow<EngineState?> = repo.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _verification = MutableStateFlow<List<VerificationResult>>(emptyList())
    val verification: StateFlow<List<VerificationResult>> = _verification

    private val _lastImportPreview = MutableStateFlow<BackupPayload?>(null)
    val lastImportPreview: StateFlow<BackupPayload?> = _lastImportPreview

    fun toast(message: String) {
        _messages.tryEmit(message)
    }

    // Dashboard filters -------------------------------------------------
    fun setVehicleFilter(id: String) {
        filter.value = filter.value.copy(vehicleId = id)
    }

    fun setPeriodFilter(key: String) {
        filter.value = filter.value.copy(periodKey = key)
    }

    fun setCustomRange(from: String, to: String) {
        filter.value = filter.value.copy(customFrom = from, customTo = to, periodKey = "custom")
    }

    // Onboarding --------------------------------------------------------
    fun completeOnboarding(withDemoData: Boolean) {
        viewModelScope.launch {
            if (withDemoData) {
                repo.ensureSettings(SampleData.settings())
                repo.loadDemoData()
            } else {
                repo.ensureSettings(SampleData.cleanSettings())
            }
            app.preferences.setOnboardingComplete(true)
        }
    }

    // Theme / notifications --------------------------------------------
    fun setThemeMode(mode: String) = viewModelScope.launch { app.preferences.setThemeMode(mode) }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        app.preferences.setNotificationsEnabled(enabled)
    }

    // Vehicles ----------------------------------------------------------
    fun nextVehicleId(): String {
        val ids = rawState.value?.vehicles.orEmpty().map { it.id }
        var n = ids.size + 1
        while (ids.contains("Vh$n")) n++
        return "Vh$n"
    }

    fun saveVehicle(vehicle: Vehicle, isNew: Boolean): Boolean {
        val current = rawState.value ?: run {
            toast("Still loading the local database. Try again in a moment.")
            return false
        }
        if (isNew && current.vehicles.any { it.id.equals(vehicle.id, ignoreCase = true) }) {
            toast("Vehicle ID \"${vehicle.id}\" already exists.")
            return false
        }
        if (vehicle.name.isBlank()) {
            toast("Vehicle name is required.")
            return false
        }
        if (vehicle.energySource == SOURCE_ELECTRICITY &&
            (vehicle.batteryCapacity ?: 0.0) <= 0.0
        ) {
            toast("Battery capacity is required for an electric vehicle.")
            return false
        }
        if (vehicle.energySource == SOURCE_ELECTRICITY && (vehicle.batteryRate ?: 0.0) <= 0.0) {
            toast("Battery rate per kWh is required for battery wear estimation.")
            return false
        }
        viewModelScope.launch {
            repo.saveVehicle(vehicle.copy(isSample = false))
            toast(if (isNew) "Added ${vehicle.name} to the fleet." else "Updated ${vehicle.name}.")
        }
        return true
    }

    fun setVehicleArchived(id: String, archived: Boolean) {
        viewModelScope.launch {
            repo.setVehicleArchived(id, archived)
            if (archived && filter.value.vehicleId == id) {
                filter.value = filter.value.copy(vehicleId = "ALL")
            }
            toast(if (archived) "Vehicle archived. History is preserved." else "Vehicle restored to active fleet.")
        }
    }

    fun deleteVehicleCascade(id: String, name: String) {
        viewModelScope.launch {
            if (repo.vehicleCount() <= 1) {
                toast("At least one vehicle must remain in the fleet.")
                return@launch
            }
            repo.deleteVehicle(id)
            if (filter.value.vehicleId == id) filter.value = filter.value.copy(vehicleId = "ALL")
            toast("$name and its records were deleted.")
        }
    }

    // Usage -------------------------------------------------------------
    fun saveUsage(entry: UsageEntry, isNew: Boolean): Boolean {
        val current = rawState.value ?: run {
            toast("Still loading the local database. Try again in a moment.")
            return false
        }
        val duplicate = current.usageEntries.any {
            it.id != entry.id &&
                it.vehicleId == entry.vehicleId &&
                it.date == entry.date &&
                it.fillType == entry.fillType &&
                it.odometer != null && entry.odometer != null &&
                kotlin.math.abs(it.odometer!! - entry.odometer!!) < 0.0001
        }
        if (isNew && duplicate) {
            toast("A matching entry already exists for this vehicle and date. Not saved twice.")
            return false
        }
        viewModelScope.launch {
            repo.saveUsage(entry.copy(isSample = false, createdAt = if (isNew) System.currentTimeMillis() else entry.createdAt))
            toast(if (isNew) "Usage entry saved." else "Usage entry updated.")
        }
        return true
    }

    fun deleteUsage(id: String) = viewModelScope.launch {
        repo.deleteUsage(id)
        toast("Usage entry deleted.")
    }

    // Maintenance -------------------------------------------------------
    fun saveMaintenance(entry: MaintenanceEntry, isNew: Boolean) {
        viewModelScope.launch {
            repo.saveMaintenance(
                entry.copy(isSample = false, createdAt = if (isNew) System.currentTimeMillis() else entry.createdAt),
            )
            toast(if (isNew) "Maintenance record saved." else "Maintenance record updated.")
        }
    }

    fun deleteMaintenance(id: String) = viewModelScope.launch {
        repo.deleteMaintenance(id)
        toast("Maintenance record deleted.")
    }

    // Prices ------------------------------------------------------------
    fun savePrice(entry: PriceEntry) = viewModelScope.launch {
        repo.savePrice(entry.copy(isSample = false))
        toast("Energy tariff saved.")
    }

    fun deletePrice(id: String) = viewModelScope.launch {
        repo.deletePrice(id)
        toast("Tariff deleted.")
    }

    // Calibration -------------------------------------------------------
    fun saveCalibration(point: CalibrationPoint) = viewModelScope.launch {
        repo.saveCalibration(point.copy(isSample = false))
        toast("Calibration point saved.")
    }

    fun deleteCalibration(id: String) = viewModelScope.launch {
        repo.deleteCalibration(id)
        toast("Calibration point removed.")
    }

    // Settings ----------------------------------------------------------
    fun saveSettings(settings: SettingsEntity) = viewModelScope.launch {
        repo.saveSettings(settings.copy(id = 1))
        toast("Settings saved.")
    }

    // Demo data ---------------------------------------------------------
    fun loadDemoData() = viewModelScope.launch {
        repo.loadDemoData()
        toast("Reference sample data loaded (marked as SAMPLE).")
    }

    fun removeSampleData() = viewModelScope.launch {
        repo.removeSampleData()
        // A removed demo vehicle may have been the active dashboard filter.
        filter.value = filter.value.copy(vehicleId = "ALL")
        toast("Sample records removed. Your own records were preserved.")
    }

    // Backup / restore --------------------------------------------------
    fun buildBackup(): String? {
        val s = rawState.value ?: return null
        return BackupManager.buildJson(
            BackupBuildInput(
                vehicles = s.vehicles,
                usage = s.usageEntries,
                maintenance = s.maintenanceEntries,
                prices = s.priceHistory,
                calibration = s.calibration,
                settings = s.settings,
            ),
        )
    }

    fun previewImport(json: String): Result<BackupPayload> {
        val parsed = BackupManager.parse(json)
        parsed.onSuccess { _lastImportPreview.value = it }
        return parsed
    }

    fun confirmImport(payload: BackupPayload) = viewModelScope.launch {
        runCatching {
            repo.restoreAll(
                vehicles = payload.vehicles,
                usage = payload.usage,
                maintenance = payload.maintenance,
                prices = payload.prices,
                calibration = payload.calibration,
                settings = payload.settings,
            )
        }.onSuccess {
            _lastImportPreview.value = null
            filter.value = filter.value.copy(vehicleId = "ALL")
            toast("Backup restored: ${payload.vehicles.size} vehicles, ${payload.usage.size} usage entries.")
        }.onFailure {
            toast("Restore failed: ${it.message ?: "unknown error"}")
        }
    }

    fun clearImportPreview() {
        _lastImportPreview.value = null
    }

    fun usageCsv(): String? = snapshot.value?.evaluation?.let { BackupManager.usageCsv(it) }
    fun maintenanceCsv(): String? = snapshot.value?.evaluation?.let { BackupManager.maintenanceCsv(it) }

    // Verification ------------------------------------------------------
    fun runVerification() {
        _verification.value = VerificationSuite.run()
    }

    fun sampleCounts(): Triple<Int, Int, Int> {
        val s = rawState.value ?: return Triple(0, 0, 0)
        return Triple(
            s.usageEntries.count { it.isSample },
            s.maintenanceEntries.count { it.isSample },
            s.calibration.count { it.isSample },
        )
    }

    /** Used by the analysis screen for "latest verified" lookups. */
    fun currentOdometer(vehicleId: String): Double {
        val s = rawState.value ?: return 0.0
        val ev = CalcEngine.evaluate(s, EvalOptions(referenceDate = LocalDate.now()))
        return ev.vehicleAllTimeStats[vehicleId]?.currentOdometer ?: 0.0
    }

    fun isDuplicateOdometer(vehicleId: String, odometer: Double?, excludeId: String?): Boolean {
        if (odometer == null) return false
        return rawState.value?.usageEntries.orEmpty().any {
            it.id != excludeId && it.vehicleId == vehicleId && it.odometer != null &&
                kotlin.math.abs(it.odometer!! - odometer) < 0.0001
        }
    }

    fun previousFullEntry(vehicleId: String, beforeDate: String): UsageEntry? {
        return rawState.value?.usageEntries.orEmpty()
            .filter {
                it.vehicleId == vehicleId && it.date <= beforeDate &&
                    (it.fillType == FILL_FULL_CHARGE || it.fillType == FILL_FULL_TANK)
            }
            .maxByOrNull { it.date }
    }
}

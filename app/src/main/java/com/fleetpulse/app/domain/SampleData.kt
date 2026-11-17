package com.fleetpulse.app.domain

import com.fleetpulse.app.data.db.CalibrationPoint
import com.fleetpulse.app.data.db.FILL_FULL_CHARGE
import com.fleetpulse.app.data.db.MaintenanceEntry
import com.fleetpulse.app.data.db.PriceEntry
import com.fleetpulse.app.data.db.SOURCE_ELECTRICITY
import com.fleetpulse.app.data.db.SOURCE_PETROL
import com.fleetpulse.app.data.db.SettingsEntity
import com.fleetpulse.app.data.db.UsageEntry
import com.fleetpulse.app.data.db.Vehicle
import java.time.LocalDate

/**
 * Reference/demo dataset derived from FleetPulse v4 `getInitialSampleState()`.
 *
 * The numeric records preserve the reference workbook values used by the
 * verification suite. Vehicle names and the registration number are synthetic
 * demo labels; they are not a real fleet. Every record is flagged
 * `isSample = true`, and sample records are never mixed with genuine records
 * without explicit user action.
 */
object SampleData {

    const val DEMO_REFERENCE_DATE = "2026-11-17"

    fun settings(): SettingsEntity = SettingsEntity(
        id = 1,
        chargerEfficiency = 89.0,
        maxPlausibleDistance = 250.0,
        excludeImplausible = true,
        warnDays = 14,
        warnKm = 200,
        chartStartMonth = "2026-10",
        evPlausibleMin = 10.0,
        evPlausibleMax = 80.0,
        petrolPlausibleMin = 20.0,
        petrolPlausibleMax = 90.0,
    )

    fun cleanSettings(today: LocalDate = LocalDate.now()): SettingsEntity = SettingsEntity(
        chartStartMonth = "%04d-%02d".format(today.year, today.monthValue),
    )

    fun vehicles(): List<Vehicle> = listOf(
        Vehicle(
            id = "Vh1",
            name = "Demo EV 1",
            type = "Electric Bike",
            energySource = SOURCE_ELECTRICITY,
            regNo = "DEMO-0001",
            purchaseDate = "2023-10-24",
            initialOdometer = 15122.0,
            batteryCapacity = 3.12,
            batteryRate = 20000.0,
            batteryLifeYears = 7.0,
            chargeCycles = 2500,
            ratedKmPerCharge = 120.0,
            dailyKmPlan = 15.0,
            tankCapacity = null,
            color = "#2563EB",
            notes = "Primary electric commuter bike",
            isSample = true,
        ),
        Vehicle(
            id = "Vh2",
            name = "Demo EV 2",
            type = "Electric Bike",
            energySource = SOURCE_ELECTRICITY,
            regNo = "",
            purchaseDate = "2023-05-17",
            initialOdometer = 3480.0,
            batteryCapacity = 1.34,
            batteryRate = 20000.0,
            batteryLifeYears = 7.0,
            chargeCycles = 2500,
            ratedKmPerCharge = 70.0,
            dailyKmPlan = 15.0,
            tankCapacity = null,
            color = "#16A34A",
            notes = "City electric scooter/bike",
            isSample = true,
        ),
        Vehicle(
            id = "Vh3",
            name = "Demo Petrol",
            type = "Petrol Bike",
            energySource = SOURCE_PETROL,
            regNo = "",
            purchaseDate = "2019-08-24",
            initialOdometer = 0.0,
            batteryCapacity = null,
            batteryRate = null,
            batteryLifeYears = null,
            chargeCycles = null,
            ratedKmPerCharge = null,
            dailyKmPlan = null,
            tankCapacity = 11.0,
            color = "#EA580C",
            notes = "11L tank commuter petrol bike",
            isSample = true,
        ),
    )

    fun usageEntries(): List<UsageEntry> = listOf(
        UsageEntry(
            id = "u_sample_1",
            date = "2026-10-27",
            vehicleId = "Vh1",
            odometer = 31713.0,
            fillType = FILL_FULL_CHARGE,
            energyAdded = null,
            voltsBefore = 54.0,
            voltsAfter = 67.3,
            unitPrice = null,
            remarks = "[SAMPLE] Baseline full charge reading",
            isSample = true,
            createdAt = 1L,
        ),
        UsageEntry(
            id = "u_sample_2",
            date = "2026-11-07",
            vehicleId = "Vh1",
            odometer = 31861.0,
            fillType = FILL_FULL_CHARGE,
            energyAdded = null,
            voltsBefore = 54.0,
            voltsAfter = 67.3,
            unitPrice = null,
            remarks = "[SAMPLE] Verified full charge stretch #1",
            isSample = true,
            createdAt = 2L,
        ),
        UsageEntry(
            id = "u_sample_3",
            date = "2026-11-11",
            vehicleId = "Vh1",
            odometer = 31967.0,
            fillType = FILL_FULL_CHARGE,
            energyAdded = null,
            voltsBefore = 58.0,
            voltsAfter = 67.3,
            unitPrice = null,
            remarks = "[SAMPLE] Verified full charge stretch #2",
            isSample = true,
            createdAt = 3L,
        ),
    )

    fun maintenanceEntries(): List<MaintenanceEntry> = listOf(
        MaintenanceEntry(
            id = "m_sample_1",
            date = "2026-08-01",
            vehicleId = "Vh1",
            odometer = 1300.0,
            category = "Battery / Charger",
            description = "Charger inspection and cleaning",
            amount = 300.0,
            nextDueDate = "2026-11-01",
            nextDueKm = null,
            remarks = "[SAMPLE] Overdue example",
            isSample = true,
            createdAt = 1L,
        ),
        MaintenanceEntry(
            id = "m_sample_2",
            date = "2026-08-06",
            vehicleId = "Vh3",
            odometer = 18500.0,
            category = "Routine service",
            description = "Engine oil change, air filter",
            amount = 1050.0,
            nextDueDate = "2026-11-06",
            nextDueKm = 21500.0,
            remarks = "[SAMPLE] Superseded by the later service -> shows Completed",
            isSample = true,
            createdAt = 2L,
        ),
        MaintenanceEntry(
            id = "m_sample_3",
            date = "2026-08-25",
            vehicleId = "Vh3",
            odometer = 18930.0,
            category = "Brakes",
            description = "Front brake pads replaced",
            amount = 900.0,
            nextDueDate = null,
            nextDueKm = 28000.0,
            remarks = "[SAMPLE] Km-based reminder only",
            isSample = true,
            createdAt = 3L,
        ),
        MaintenanceEntry(
            id = "m_sample_4",
            date = "2026-09-10",
            vehicleId = "Vh1",
            odometer = 1790.0,
            category = "Routine service",
            description = "First free service",
            amount = 0.0,
            nextDueDate = "2027-03-10",
            nextDueKm = 2500.0,
            remarks = "[SAMPLE] Due-soon example (km based)",
            isSample = true,
            createdAt = 4L,
        ),
        MaintenanceEntry(
            id = "m_sample_5",
            date = "2026-10-05",
            vehicleId = "Vh2",
            odometer = 4100.0,
            category = "Tyres",
            description = "Rear tyre replacement",
            amount = 1850.0,
            nextDueDate = null,
            nextDueKm = null,
            remarks = "[SAMPLE] No reminder needed",
            isSample = true,
            createdAt = 5L,
        ),
        MaintenanceEntry(
            id = "m_sample_6",
            date = "2026-10-05",
            vehicleId = "Vh2",
            odometer = 4100.0,
            category = "Routine service",
            description = "Brake adjustment and general check",
            amount = 600.0,
            nextDueDate = "2027-04-05",
            nextDueKm = 7000.0,
            remarks = "[SAMPLE]",
            isSample = true,
            createdAt = 6L,
        ),
        MaintenanceEntry(
            id = "m_sample_7",
            date = "2026-11-09",
            vehicleId = "Vh3",
            odometer = 20350.0,
            category = "Routine service",
            description = "Engine oil + filter, chain lube",
            amount = 1150.0,
            nextDueDate = "2027-05-09",
            nextDueKm = 23400.0,
            remarks = "[SAMPLE]",
            isSample = true,
            createdAt = 7L,
        ),
    )

    fun priceHistory(): List<PriceEntry> = listOf(
        PriceEntry("p_sample_1", SOURCE_ELECTRICITY, "2026-01-01", 7.00, "[SAMPLE] home tariff per kWh", true),
        PriceEntry("p_sample_2", SOURCE_ELECTRICITY, "2026-10-01", 7.50, "[SAMPLE] tariff revised", true),
        PriceEntry("p_sample_3", SOURCE_PETROL, "2026-01-01", 94.50, "[SAMPLE] pump price per litre", true),
        PriceEntry("p_sample_4", SOURCE_PETROL, "2026-10-15", 95.20, "[SAMPLE] price revised", true),
    )

    fun calibration(): List<CalibrationPoint> = buildList {
        // Vh1 — 16S Li-ion reference curve
        listOf(
            51.00 to 0.0, 52.32 to 5.0, 53.76 to 10.0, 55.20 to 15.0, 56.48 to 20.0,
            57.12 to 25.0, 57.60 to 30.0, 58.08 to 35.0, 58.56 to 40.0, 59.04 to 45.0,
            59.52 to 50.0, 60.00 to 55.0, 60.64 to 60.0, 61.28 to 65.0, 61.92 to 70.0,
            62.72 to 75.0, 63.52 to 80.0, 64.32 to 85.0, 65.12 to 90.0, 66.08 to 95.0,
            67.30 to 100.0,
        ).forEachIndexed { idx, (v, p) ->
            add(
                CalibrationPoint(
                    id = "c_vh1_${p.toInt()}",
                    vehicleId = "Vh1",
                    voltage = v,
                    percent = p,
                    note = if (idx == 0) {
                        "[SAMPLE] typical 16S Li-ion curve - replace with YOUR pack's readings"
                    } else {
                        ""
                    },
                    isSample = true,
                ),
            )
        }
        // Vh2 — 16S Li-ion reference curve
        listOf(
            54.60 to 100.0, 53.69 to 95.0, 52.91 to 90.0, 52.26 to 85.0, 51.61 to 80.0,
            50.96 to 75.0, 50.31 to 70.0, 49.79 to 65.0, 49.27 to 60.0, 48.81 to 55.0,
            48.36 to 50.0, 47.97 to 45.0, 47.58 to 40.0, 47.19 to 35.0, 46.80 to 30.0,
            46.34 to 25.0, 45.89 to 20.0, 44.85 to 15.0, 43.68 to 10.0, 42.51 to 5.0,
            41.50 to 0.0,
        ).forEachIndexed { idx, (v, p) ->
            add(
                CalibrationPoint(
                    id = "c_vh2_${p.toInt()}",
                    vehicleId = "Vh2",
                    voltage = v,
                    percent = p,
                    note = if (idx == 0) {
                        "[SAMPLE] typical 16S Li-ion curve - replace with YOUR pack's readings"
                    } else {
                        ""
                    },
                    isSample = true,
                ),
            )
        }
    }

    fun demoState(): EngineState = EngineState(
        vehicles = vehicles(),
        usageEntries = usageEntries(),
        maintenanceEntries = maintenanceEntries(),
        priceHistory = priceHistory(),
        calibration = calibration(),
        settings = settings(),
    )
}

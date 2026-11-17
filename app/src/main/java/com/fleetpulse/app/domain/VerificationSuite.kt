package com.fleetpulse.app.domain

import com.fleetpulse.app.data.db.CalibrationPoint
import com.fleetpulse.app.data.db.FILL_FULL_CHARGE
import com.fleetpulse.app.data.db.FILL_FULL_TANK
import com.fleetpulse.app.data.db.FILL_PARTIAL_CHARGE
import com.fleetpulse.app.data.db.FILL_PARTIAL_REFILL
import com.fleetpulse.app.data.db.FILL_RIDE_ONLY
import com.fleetpulse.app.data.db.MaintenanceEntry
import com.fleetpulse.app.data.db.SOURCE_ELECTRICITY
import com.fleetpulse.app.data.db.SOURCE_PETROL
import com.fleetpulse.app.data.db.SettingsEntity
import com.fleetpulse.app.data.db.UsageEntry
import com.fleetpulse.app.data.db.Vehicle
import java.time.LocalDate
import java.util.Locale

data class VerificationResult(
    val name: String,
    val expected: String,
    val actual: String,
    val pass: Boolean,
)

/**
 * Executable verification suite. It runs the real engine against the exact
 * reference dataset and the reference cases from the FleetPulse v4 web app,
 * plus additional edge cases required for the native port.
 */
object VerificationSuite {

    private val refDate: LocalDate = LocalDate.parse(SampleData.DEMO_REFERENCE_DATE)

    fun run(): List<VerificationResult> {
        val results = mutableListOf<VerificationResult>()
        val sample = SampleData.demoState()
        val refComputed = CalcEngine.evaluate(
            sample,
            EvalOptions(referenceDate = refDate, periodKey = "all"),
        )
        val mPeriod = refComputed.periodStatsByVehicle.getValue("Vh1")
        val mAllTime = refComputed.vehicleAllTimeStats.getValue("Vh1")
        val reoAllTime = refComputed.vehicleAllTimeStats.getValue("Vh2")

        fun check(name: String, expected: String, actual: String, pass: Boolean) {
            results.add(VerificationResult(name, expected, actual, pass))
        }

        // ---- The 13 workbook parity cases --------------------------------
        check(
            "Demo EV 1 — Verified Distance (Full-to-Full)",
            "254.0 km",
            "${f1(mPeriod.verifiedDistance)} km",
            f1(mPeriod.verifiedDistance) == "254.0",
        )
        check(
            "Demo EV 1 — Verified Wall Energy Consumed",
            "5.43 kWh",
            "${f2(mPeriod.verifiedEnergy)} kWh",
            f2(mPeriod.verifiedEnergy) == "5.43",
        )
        check(
            "Demo EV 1 — Verified Efficiency (km/kWh)",
            "46.7 km/kWh",
            "${f1(mPeriod.verifiedEfficiency)} km/kWh",
            f1(mPeriod.verifiedEfficiency) == "46.7",
        )
        check(
            "Demo EV 1 — Actual Average Valid Km per Full Charge",
            "163.5 km/charge",
            "${f1(mAllTime.allTimeAvgKmPerCharge)} km/charge",
            f1(mAllTime.allTimeAvgKmPerCharge) == "163.5",
        )
        check(
            "Demo EV 1 — Max Running Distance (2,500 cycles × 163.5)",
            "408,742 km",
            "${Math.round(mAllTime.maxRunningCapacityKm)} km",
            Math.round(mAllTime.maxRunningCapacityKm) == 408742L,
        )
        check(
            "Demo EV 1 — Distance Considered (min of Cycles vs 7yr Age)",
            "38,325 km",
            "${mAllTime.distanceConsideredKm} km",
            mAllTime.distanceConsideredKm == 38325.0,
        )
        check(
            "Demo EV 1 — Battery Wear Cost per km",
            "₹1.63 /km",
            "₹${f2(mAllTime.batteryWearCostPerKm)} /km",
            f2(mAllTime.batteryWearCostPerKm) == "1.63",
        )
        check(
            "Demo EV 1 — Verified Energy Cost per km",
            "₹0.16 /km",
            "₹${f2(mPeriod.verifiedEnergyCostPerKm)} /km",
            f2(mPeriod.verifiedEnergyCostPerKm) == "0.16",
        )
        check(
            "Demo EV 1 — Total Running Cost per km (Energy + Battery Wear)",
            "₹1.79 /km",
            "₹${f2(mPeriod.runningCostPerKm)} /km",
            f2(mPeriod.runningCostPerKm) == "1.79",
        )
        check(
            "Demo EV 1 — Total Running Cost in Period (₹64.19 + ₹413.56)",
            "₹477.75",
            "₹${f2(mPeriod.totalRunningCost)}",
            f2(mPeriod.totalRunningCost) == "477.75",
        )
        check(
            "Demo EV 2 — Fallback to Rated Km/Charge (70 km) → Battery Wear/km",
            "₹0.70 /km",
            "₹${f2(reoAllTime.batteryWearCostPerKm)} /km",
            f2(reoAllTime.batteryWearCostPerKm) == "0.70",
        )

        // Edge case from the web app: partial refill accumulation (petrol)
        val edgeState = sample.copy(
            usageEntries = sample.usageEntries + listOf(
                UsageEntry("t_p1", "2026-11-01", "Vh3", 1000.0, FILL_FULL_TANK, 5.0, remarks = "Baseline Full Tank"),
                UsageEntry("t_p2", "2026-11-05", "Vh3", 1120.0, FILL_PARTIAL_REFILL, 2.0, remarks = "Partial 2L"),
                UsageEntry("t_p3", "2026-11-10", "Vh3", 1310.0, FILL_FULL_TANK, 3.0, remarks = "Closing Full Tank"),
            ),
        )
        val edgeComputed = CalcEngine.evaluate(edgeState, EvalOptions(referenceDate = refDate))
        val pStretch = edgeComputed.evaluatedUsage.first { it.entry.id == "t_p3" }
        val pPartial = edgeComputed.evaluatedUsage.first { it.entry.id == "t_p2" }
        check(
            "Demo Petrol — Partial Refill Never Generates Own Mileage; Included in Next Full Tank",
            "Partial: null · Closing Full: 62.0 km/L",
            "Partial: ${pPartial.stretchEfficiency?.let { f1(it) } ?: "null"} · " +
                "Closing Full: ${pStretch.stretchEfficiency?.let { f1(it) }} km/L",
            pPartial.stretchEfficiency == null && f1(pStretch.stretchEfficiency) == "62.0",
        )

        val interp = CalcEngine.interpolateVoltage("Vh1", 60.0, sample.calibration)
        check(
            "Voltage Calibration Linear Interpolation (Vh1 @ 60.0V)",
            "55.0% (1.72 kWh)",
            "${interp.percent?.let { f1(it) }}% (${f2(0.55 * 3.12)} kWh)",
            f1(interp.percent) == "55.0",
        )

        // ---- Additional native acceptance cases -------------------------
        check(
            "Historical tariff — electricity before revision (30 Sep 2026)",
            "₹7.00 /kWh",
            "₹${f2(CalcEngine.getEffectivePrice(SOURCE_ELECTRICITY, "2026-09-30", sample.priceHistory))} /kWh",
            CalcEngine.getEffectivePrice(SOURCE_ELECTRICITY, "2026-09-30", sample.priceHistory) == 7.00,
        )
        check(
            "Historical tariff — electricity on revision date (01 Oct 2026)",
            "₹7.50 /kWh",
            "₹${f2(CalcEngine.getEffectivePrice(SOURCE_ELECTRICITY, "2026-10-01", sample.priceHistory))} /kWh",
            CalcEngine.getEffectivePrice(SOURCE_ELECTRICITY, "2026-10-01", sample.priceHistory) == 7.50,
        )
        check(
            "Historical tariff — petrol before revision (14 Oct 2026)",
            "₹94.50 /L",
            "₹${f2(CalcEngine.getEffectivePrice(SOURCE_PETROL, "2026-10-14", sample.priceHistory))} /L",
            CalcEngine.getEffectivePrice(SOURCE_PETROL, "2026-10-14", sample.priceHistory) == 94.50,
        )
        check(
            "Historical tariff — petrol on revision date (15 Oct 2026)",
            "₹95.20 /L",
            "₹${f2(CalcEngine.getEffectivePrice(SOURCE_PETROL, "2026-10-15", sample.priceHistory))} /L",
            CalcEngine.getEffectivePrice(SOURCE_PETROL, "2026-10-15", sample.priceHistory) == 95.20,
        )
        check(
            "Voltage interpolation — below curve clamps to minimum %",
            "0.0% · out of range",
            "${f1(CalcEngine.interpolateVoltage("Vh1", 45.0, sample.calibration).percent)}% · " +
                if (CalcEngine.interpolateVoltage("Vh1", 45.0, sample.calibration).outOfRange) "out of range" else "in range",
            CalcEngine.interpolateVoltage("Vh1", 45.0, sample.calibration).let {
                it.percent == 0.0 && it.outOfRange
            },
        )
        check(
            "Voltage interpolation — above curve clamps to maximum %",
            "100.0% · out of range",
            "${f1(CalcEngine.interpolateVoltage("Vh1", 70.0, sample.calibration).percent)}% · " +
                if (CalcEngine.interpolateVoltage("Vh1", 70.0, sample.calibration).outOfRange) "out of range" else "in range",
            CalcEngine.interpolateVoltage("Vh1", 70.0, sample.calibration).let {
                it.percent == 100.0 && it.outOfRange
            },
        )
        check(
            "Voltage interpolation — missing curve is flagged",
            "missing curve",
            if (CalcEngine.interpolateVoltage("Vh3", 60.0, sample.calibration).missingCurve) "missing curve" else "curve found",
            CalcEngine.interpolateVoltage("Vh3", 60.0, sample.calibration).missingCurve,
        )

        // Provisional efficiency: no full-to-full stretch close
        val provisionalState = EngineState(
            vehicles = listOf(
                Vehicle(
                    id = "Vp1", name = "Test EV", type = "Electric Bike",
                    energySource = SOURCE_ELECTRICITY, initialOdometer = 0.0,
                    batteryCapacity = 3.0, batteryRate = 20000.0, batteryLifeYears = 7.0,
                    chargeCycles = 2500, ratedKmPerCharge = 100.0, dailyKmPlan = 15.0,
                ),
            ),
            usageEntries = listOf(
                UsageEntry("t1", "2026-11-01", "Vp1", 0.0, FILL_FULL_CHARGE, 2.0),
                UsageEntry("t2", "2026-11-05", "Vp1", 100.0, FILL_RIDE_ONLY, null),
            ),
            maintenanceEntries = emptyList(),
            priceHistory = sample.priceHistory,
            calibration = emptyList(),
            settings = SettingsEntity(chargerEfficiency = 89.0),
        )
        val prov = CalcEngine.evaluate(provisionalState, EvalOptions(referenceDate = refDate))
            .periodStatsByVehicle.getValue("Vp1")
        check(
            "Provisional efficiency — labelled, never verified",
            "Provisional · 50.0 km/kWh",
            "${prov.efficiencyBasis} · ${f1(prov.provisionalEfficiency)} km/kWh",
            prov.efficiencyBasis == "Provisional" && f1(prov.provisionalEfficiency) == "50.0" &&
                prov.verifiedEfficiency == null,
        )

        // Partial charge between two full charges must be accumulated
        val evState = EngineState(
            vehicles = listOf(
                Vehicle(
                    id = "Ve1", name = "Test EV", type = "Electric Bike",
                    energySource = SOURCE_ELECTRICITY, initialOdometer = 0.0,
                    batteryCapacity = 3.0, batteryRate = 20000.0, batteryLifeYears = 7.0,
                    chargeCycles = 2500, ratedKmPerCharge = 100.0, dailyKmPlan = 15.0,
                ),
            ),
            usageEntries = listOf(
                UsageEntry("e1", "2026-11-01", "Ve1", 100.0, FILL_FULL_CHARGE, 2.0),
                UsageEntry("e2", "2026-11-04", "Ve1", 150.0, FILL_PARTIAL_CHARGE, 1.0),
                UsageEntry("e3", "2026-11-08", "Ve1", 280.0, FILL_FULL_CHARGE, 2.0),
            ),
            maintenanceEntries = emptyList(),
            priceHistory = sample.priceHistory,
            calibration = emptyList(),
            settings = SettingsEntity(chargerEfficiency = 89.0),
        )
        val evEval = CalcEngine.evaluate(evState, EvalOptions(referenceDate = refDate))
        val evClose = evEval.evaluatedUsage.first { it.entry.id == "e3" }
        val evPartial = evEval.evaluatedUsage.first { it.entry.id == "e2" }
        check(
            "EV partial charge accumulates into next full charge",
            "Partial: null · Closing: 60.0 km/kWh",
            "Partial: ${evPartial.stretchEfficiency?.let { f1(it) } ?: "null"} · Closing: ${f1(evClose.stretchEfficiency)} km/kWh",
            evPartial.stretchEfficiency == null && f1(evClose.stretchEfficiency) == "60.0",
        )

        // Implausible efficiency flagged and excluded from averages
        val implausible = evState.copy(
            usageEntries = evState.usageEntries + UsageEntry("e4", "2026-11-12", "Ve1", 300.0, FILL_FULL_CHARGE, 0.10),
        )
        val implEval = CalcEngine.evaluate(implausible, EvalOptions(referenceDate = refDate))
        val implRow = implEval.evaluatedUsage.first { it.entry.id == "e4" }
        check(
            "Implausible efficiency flagged as CHECK and excluded",
            "CHECK · excluded",
            "${implRow.status} · ${if (implRow.isValidForAverage) "included" else "excluded"}",
            implRow.status == "CHECK" && !implRow.isValidForAverage,
        )

        // Maintenance status cases
        val maintEval = refComputed.evaluatedMaintenance
        val overdue = maintEval.first { it.entry.id == "m_sample_1" }
        val superseded = maintEval.first { it.entry.id == "m_sample_2" }
        check(
            "Maintenance — date reminder overdue",
            "Overdue",
            overdue.status,
            overdue.status == "Overdue",
        )
        check(
            "Maintenance — newer same-category record retires old reminder",
            "Completed",
            superseded.status,
            superseded.status == "Completed",
        )

        val dueSoonState = sample.copy(
            maintenanceEntries = listOf(
                MaintenanceEntry(
                    id = "m_due_soon", date = "2026-11-01", vehicleId = "Vh1",
                    odometer = 31800.0, category = "Repair", description = "Test",
                    amount = 100.0, nextDueDate = refDate.plusDays(10).toString(),
                ),
                MaintenanceEntry(
                    id = "m_far", date = "2026-11-01", vehicleId = "Vh1",
                    odometer = 31800.0, category = "Tyres", description = "Test",
                    amount = 100.0, nextDueDate = refDate.plusDays(40).toString(),
                ),
            ),
        )
        val dueSoonEval = CalcEngine.evaluate(dueSoonState, EvalOptions(referenceDate = refDate))
        check(
            "Maintenance — due-soon threshold (14 days)",
            "Due Soon",
            dueSoonEval.evaluatedMaintenance.first { it.entry.id == "m_due_soon" }.status,
            dueSoonEval.evaluatedMaintenance.first { it.entry.id == "m_due_soon" }.status == "Due Soon",
        )
        check(
            "Maintenance — outside threshold stays OK",
            "OK",
            dueSoonEval.evaluatedMaintenance.first { it.entry.id == "m_far" }.status,
            dueSoonEval.evaluatedMaintenance.first { it.entry.id == "m_far" }.status == "OK",
        )

        // Duplicate calibration detection
        val dup = sample.copy(
            calibration = sample.calibration + CalibrationPoint(
                id = "c_dup", vehicleId = "Vh1", voltage = 60.0, percent = 56.0,
            ),
        )
        val dupChecks = CalcEngine.evaluate(dup, EvalOptions(referenceDate = refDate)).calibrationChecks
        val dupRow = dupChecks.firstOrNull { it.point.id == "c_dup" }
        check(
            "Calibration — duplicate voltage flagged ERROR",
            "ERROR",
            dupRow?.check?.take(5) ?: "—",
            dupRow?.check?.startsWith("ERROR") == true,
        )

        // EV vs petrol savings only when both sides have data
        val evOnly = CalcEngine.fleetTotals(
            listOf(refComputed.periodStatsByVehicle.getValue("Vh1")),
        )
        check(
            "EV vs Petrol savings suppressed without petrol data",
            "null",
            evOnly.evSavings?.let { f0(it) } ?: "null",
            evOnly.evSavings == null,
        )

        val withPetrol = CalcEngine.evaluate(edgeState, EvalOptions(referenceDate = refDate))
        val fleet = CalcEngine.fleetTotals(withPetrol.periodStatsByVehicle.values.toList())
        val expectedSavings = if (fleet.evCostPerKm != null && fleet.petrolCostPerKm != null) {
            (fleet.petrolCostPerKm!! - fleet.evCostPerKm!!) * fleet.evDistance
        } else {
            null
        }
        check(
            "EV vs Petrol savings uses distance-weighted actual prices",
            expectedSavings?.let { "₹${f0(it)}" } ?: "null",
            fleet.evSavings?.let { "₹${f0(it)}" } ?: "null",
            fleet.evSavings != null && expectedSavings != null && kotlin.math.abs(fleet.evSavings!! - expectedSavings) < 0.01,
        )

        return results
    }

    private fun f0(v: Double?): String = v?.let { String.format(Locale.US, "%.0f", it) } ?: "—"
    private fun f1(v: Double?): String = v?.let { String.format(Locale.US, "%.1f", it) } ?: "—"
    private fun f2(v: Double?): String = v?.let { String.format(Locale.US, "%.2f", it) } ?: "—"
}

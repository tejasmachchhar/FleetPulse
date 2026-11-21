package com.fleetpulse.app.domain

import com.fleetpulse.app.data.db.FILL_FULL_CHARGE
import com.fleetpulse.app.data.db.FILL_FULL_TANK
import com.fleetpulse.app.data.db.FILL_PARTIAL_CHARGE
import com.fleetpulse.app.data.db.FILL_PARTIAL_REFILL
import com.fleetpulse.app.data.db.FILL_RIDE_ONLY
import com.fleetpulse.app.data.db.MaintenanceEntry
import com.fleetpulse.app.data.db.SOURCE_ELECTRICITY
import com.fleetpulse.app.data.db.SOURCE_PETROL
import com.fleetpulse.app.data.db.STATUS_CHECK
import com.fleetpulse.app.data.db.STATUS_ERROR
import com.fleetpulse.app.data.db.STATUS_OK
import com.fleetpulse.app.data.db.SettingsEntity
import com.fleetpulse.app.data.db.UsageEntry
import com.fleetpulse.app.data.db.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalcEngineTest {

    private val ref = LocalDate.parse("2026-11-17")
    private val sample = SampleData.demoState()
    private val sampleEval = CalcEngine.evaluate(sample, EvalOptions(referenceDate = ref))

    @Test
    fun `workbook reference values are reproduced exactly`() {
        val period = sampleEval.periodStatsByVehicle.getValue("Vh1")
        val allTime = sampleEval.vehicleAllTimeStats.getValue("Vh1")

        assertEquals(254.0, period.verifiedDistance, 1e-9)
        assertEquals(5.43, period.verifiedEnergy, 0.005)
        assertEquals(46.7, period.verifiedEfficiency!!, 0.05)
        assertEquals(163.5, allTime.allTimeAvgKmPerCharge!!, 0.05)
        assertEquals(408_742.0, allTime.maxRunningCapacityKm, 0.5)
        assertEquals(38_325.0, allTime.distanceConsideredKm, 1e-9)
        assertEquals(1.63, allTime.batteryWearCostPerKm, 0.005)
        assertEquals(0.16, period.verifiedEnergyCostPerKm!!, 0.005)
        assertEquals(1.79, period.runningCostPerKm!!, 0.005)
        assertEquals(477.75, period.totalRunningCost, 0.005)
        assertEquals(0.70, sampleEval.vehicleAllTimeStats.getValue("Vh2").batteryWearCostPerKm, 0.005)
    }

    @Test
    fun `full-to-full distance starts at previous full fill baseline`() {
        val rows = sampleEval.evaluatedUsage.filter { it.entry.vehicleId == "Vh1" }
        assertEquals(3, rows.size)
        assertNull(rows[0].stretchEfficiency) // baseline
        // The first sample reading is far above the configured initial odometer,
        // so it is used as a starting reading (0 km) with a CHECK note.
        assertEquals(0.0, rows[0].distance, 0.0)
        assertEquals(148.0, rows[1].stretchDistance!!, 1e-9)
        assertEquals(106.0, rows[2].stretchDistance!!, 1e-9)
    }

    @Test
    fun `EV km per charge uses battery capacity over charger efficiency`() {
        val row = sampleEval.evaluatedUsage.first { it.entry.id == "u_sample_2" }
        val expected = 148.0 * (3.12 / 0.89) / row.stretchEnergy!!
        assertEquals(expected, row.kmPerCharge!!, 1e-9)
    }

    @Test
    fun `partial charge never produces an independent mileage result`() {
        val state = sample.copy(
            usageEntries = listOf(
                UsageEntry("a", "2026-11-01", "Vh1", 32000.0, FILL_FULL_CHARGE, 3.0),
                UsageEntry("b", "2026-11-03", "Vh1", 32100.0, FILL_PARTIAL_CHARGE, 1.0),
            ),
        )
        val rows = CalcEngine.evaluate(state, EvalOptions(referenceDate = ref)).evaluatedUsage
        val partial = rows.first { it.entry.id == "b" }
        assertNull(partial.stretchEfficiency)
        assertEquals(0.0, partial.stretchDistance ?: 0.0, 0.0)
        assertTrue(partial.distance > 0)
    }

    @Test
    fun `petrol partial refill accumulates into closing full tank`() {
        val base = EngineState(
            vehicles = listOf(
                Vehicle(
                    id = "P1", name = "Petrol", type = "Bike", energySource = "Petrol",
                    initialOdometer = 0.0, tankCapacity = 11.0,
                ),
            ),
            usageEntries = listOf(
                UsageEntry("p1", "2026-11-01", "P1", 0.0, FILL_FULL_TANK, 5.0),
                UsageEntry("p2", "2026-11-02", "P1", 100.0, "Partial refill", 2.0),
                UsageEntry("p3", "2026-11-03", "P1", 310.0, FILL_FULL_TANK, 3.0),
            ),
            maintenanceEntries = emptyList(),
            priceHistory = emptyList(),
            calibration = emptyList(),
            settings = SettingsEntity(),
        )
        val rows = CalcEngine.evaluate(base, EvalOptions(referenceDate = ref)).evaluatedUsage
        assertNull(rows.first { it.entry.id == "p2" }.stretchEfficiency)
        assertEquals(62.0, rows.first { it.entry.id == "p3" }.stretchEfficiency!!, 1e-9)
        assertEquals(310.0, rows.first { it.entry.id == "p3" }.stretchDistance!!, 1e-9)
        assertEquals(5.0, rows.first { it.entry.id == "p3" }.stretchEnergy!!, 1e-9)
    }

    @Test
    fun `overall efficiency is total distance over total energy not an average`() {
        val state = sample.copy(
            usageEntries = listOf(
                UsageEntry("a", "2026-11-01", "Vh1", 32000.0, FILL_FULL_CHARGE, 3.0),
                UsageEntry("b", "2026-11-02", "Vh1", 32100.0, FILL_FULL_CHARGE, 3.0),
                UsageEntry("c", "2026-11-03", "Vh1", 32300.0, FILL_FULL_CHARGE, 3.0),
            ),
        )
        val stats = CalcEngine.evaluate(state, EvalOptions(referenceDate = ref))
            .periodStatsByVehicle.getValue("Vh1")
        assertEquals(300.0, stats.verifiedDistance, 1e-9)
        assertEquals(6.0, stats.verifiedEnergy, 1e-9)
        assertEquals(50.0, stats.verifiedEfficiency!!, 1e-9)
    }

    @Test
    fun `voltage interpolation clamps outside range and flags it`() {
        val mid = CalcEngine.interpolateVoltage("Vh1", 60.0, sample.calibration)
        assertEquals(55.0, mid.percent!!, 1e-9)
        assertTrue(!mid.outOfRange)

        val low = CalcEngine.interpolateVoltage("Vh1", 40.0, sample.calibration)
        assertEquals(0.0, low.percent!!, 0.0)
        assertTrue(low.outOfRange && !low.missingCurve)

        val high = CalcEngine.interpolateVoltage("Vh1", 99.0, sample.calibration)
        assertEquals(100.0, high.percent!!, 0.0)
        assertTrue(high.outOfRange)

        val missing = CalcEngine.interpolateVoltage("Vh3", 60.0, sample.calibration)
        assertNull(missing.percent)
        assertTrue(missing.missingCurve)
    }

    @Test
    fun `historical tariff selects latest rate effective on transaction date`() {
        assertEquals(
            7.00,
            CalcEngine.getEffectivePrice(SOURCE_ELECTRICITY, "2026-09-30", sample.priceHistory)!!,
            0.0,
        )
        assertEquals(
            7.50,
            CalcEngine.getEffectivePrice(SOURCE_ELECTRICITY, "2026-11-17", sample.priceHistory)!!,
            0.0,
        )
        assertEquals(
            94.50,
            CalcEngine.getEffectivePrice("Petrol", "2026-10-14", sample.priceHistory)!!,
            0.0,
        )
        assertEquals(
            95.20,
            CalcEngine.getEffectivePrice("Petrol", "2026-10-15", sample.priceHistory)!!,
            0.0,
        )
        assertNull(CalcEngine.getEffectivePrice("Electricity", "2025-12-31", sample.priceHistory))
    }

    @Test
    fun `period bounds match financial year and rolling windows`() {
        val fy = CalcEngine.getPeriodBounds("current_fy", ref, "", "")
        assertEquals("2026-04-01", fy.start)
        assertEquals("2027-03-31", fy.end)

        val prevFy = CalcEngine.getPeriodBounds("prev_fy", ref, "", "")
        assertEquals("2025-04-01", prevFy.start)
        assertEquals("2026-03-31", prevFy.end)

        val last30 = CalcEngine.getPeriodBounds("last_30", ref, "", "")
        assertEquals("2026-10-18", last30.start)
        assertEquals("2026-11-17", last30.end)

        val month = CalcEngine.getPeriodBounds("current_month", ref, "", "")
        assertEquals("2026-11-01", month.start)
        assertEquals("2026-11-30", month.end)
    }

    @Test
    fun `odometer rollback is ERROR and breaks the stretch`() {
        val state = sample.copy(
            usageEntries = listOf(
                UsageEntry("a", "2026-11-01", "Vh1", 32000.0, FILL_FULL_CHARGE, 3.0),
                UsageEntry("b", "2026-11-02", "Vh1", 31000.0, FILL_FULL_CHARGE, 3.0),
                UsageEntry("c", "2026-11-03", "Vh1", 32200.0, FILL_FULL_CHARGE, 3.0),
            ),
        )
        val rows = CalcEngine.evaluate(state, EvalOptions(referenceDate = ref)).evaluatedUsage
        assertEquals("ERROR", rows.first { it.entry.id == "b" }.status)
        // The following full charge starts a fresh baseline instead of a bogus stretch.
        assertNull(rows.first { it.entry.id == "c" }.stretchEfficiency)
    }

    @Test
    fun `battery wear falls back to rated km per charge when no verified stretch exists`() {
        val reo = sampleEval.vehicleAllTimeStats.getValue("Vh2")
        assertNull(reo.allTimeAvgKmPerCharge)
        assertEquals(70.0, reo.kmPerChargeUsed, 0.0)
        // 1.34 * 20000 / min(2500 * 70, 7 * 365 * 15) = 26800 / 38325
        assertEquals(26800.0 / 38325.0, reo.batteryWearCostPerKm, 1e-9)
    }

    @Test
    fun `maintenance reminders retire older same-category records`() {
        val maintenance = sampleEval.evaluatedMaintenance
        assertEquals("Overdue", maintenance.first { it.entry.id == "m_sample_1" }.status)
        assertEquals("Completed", maintenance.first { it.entry.id == "m_sample_2" }.status)
        assertEquals("Logged", maintenance.first { it.entry.id == "m_sample_5" }.status)

        val nearestForPetrol = sampleEval.nearestReminderByVehicle.getValue("Vh3")!!
        assertEquals("Routine service", nearestForPetrol.entry.category)
        assertEquals("OK", nearestForPetrol.status)
    }

    @Test
    fun `duplicate and inverted calibration points are flagged`() {
        val checks = CalcEngine.evaluate(
            sample.copy(
                calibration = sample.calibration + listOf(
                    com.fleetpulse.app.data.db.CalibrationPoint("dup", "Vh1", 60.0, 56.0),
                    com.fleetpulse.app.data.db.CalibrationPoint("inv", "Vh2", 55.0, 50.0),
                ),
            ),
            EvalOptions(referenceDate = ref),
        ).calibrationChecks

        assertEquals("ERROR: Duplicate voltage for vehicle", checks.first { it.point.id == "dup" }.check)
        assertEquals("CHECK: Battery % does not rise with voltage", checks.first { it.point.id == "inv" }.check)
    }

    @Test
    fun `provisional efficiency is used only when verification is unavailable`() {
        val state = EngineState(
            vehicles = listOf(
                Vehicle(
                    id = "E1", name = "EV", type = "Bike", energySource = SOURCE_ELECTRICITY,
                    initialOdometer = 0.0, batteryCapacity = 3.0, batteryRate = 20000.0,
                    batteryLifeYears = 7.0, chargeCycles = 2500, ratedKmPerCharge = 100.0,
                    dailyKmPlan = 15.0,
                ),
            ),
            usageEntries = listOf(
                UsageEntry("a", "2026-11-01", "E1", 0.0, FILL_FULL_CHARGE, 2.0),
                UsageEntry("b", "2026-11-02", "E1", 100.0, FILL_RIDE_ONLY, null),
            ),
            maintenanceEntries = emptyList(),
            priceHistory = emptyList(),
            calibration = emptyList(),
            settings = SettingsEntity(),
        )
        val stats = CalcEngine.evaluate(state, EvalOptions(referenceDate = ref))
            .periodStatsByVehicle.getValue("E1")
        assertEquals("Provisional", stats.efficiencyBasis)
        assertEquals(50.0, stats.efficiencyUsed!!, 1e-9)
        assertNull(stats.verifiedEfficiency)
    }

    @Test
    fun `excluded partial refill is removed from mileage denominator but stays in expenses`() {
        val state = EngineState(
            vehicles = listOf(
                Vehicle(
                    id = "P1", name = "Petrol", type = "Bike", energySource = "Petrol",
                    initialOdometer = 0.0, tankCapacity = 11.0,
                ),
            ),
            usageEntries = listOf(
                UsageEntry("p1", "2026-11-01", "P1", 0.0, FILL_FULL_TANK, 5.0, unitPrice = 100.0),
                UsageEntry(
                    "p2", "2026-11-02", "P1", 100.0, FILL_PARTIAL_REFILL, 2.0,
                    unitPrice = 100.0, includeInMileage = false,
                ),
                UsageEntry("p3", "2026-11-03", "P1", 310.0, FILL_FULL_TANK, 3.0, unitPrice = 100.0),
            ),
            maintenanceEntries = emptyList(),
            priceHistory = emptyList(),
            calibration = emptyList(),
            settings = SettingsEntity(),
        )
        val eval = CalcEngine.evaluate(state, EvalOptions(referenceDate = ref))
        val rows = eval.evaluatedUsage
        val closing = rows.first { it.entry.id == "p3" }
        val partial = rows.first { it.entry.id == "p2" }

        assertNull(partial.stretchEfficiency)
        assertEquals(310.0, closing.stretchDistance!!, 1e-9)
        assertEquals(3.0, closing.stretchEnergy!!, 1e-9)
        assertEquals(310.0 / 3.0, closing.stretchEfficiency!!, 1e-9)
        // Excluded fuel is still money spent on this vehicle.
        assertEquals(200.0, partial.totalCost, 1e-9)
        assertEquals(1000.0, eval.periodStatsByVehicle.getValue("P1").energySpend, 1e-9)
    }

    @Test
    fun `excluded full refill cannot start or close an interval`() {
        val state = EngineState(
            vehicles = listOf(
                Vehicle(
                    id = "P1", name = "Petrol", type = "Bike", energySource = "Petrol",
                    initialOdometer = 0.0, tankCapacity = 11.0,
                ),
            ),
            usageEntries = listOf(
                UsageEntry("p1", "2026-11-01", "P1", 0.0, FILL_FULL_TANK, 5.0),
                UsageEntry(
                    "p2", "2026-11-02", "P1", 100.0, FILL_FULL_TANK, 2.0,
                    includeInMileage = false,
                ),
                UsageEntry("p3", "2026-11-03", "P1", 310.0, FILL_FULL_TANK, 3.0),
            ),
            maintenanceEntries = emptyList(),
            priceHistory = emptyList(),
            calibration = emptyList(),
            settings = SettingsEntity(),
        )
        val rows = CalcEngine.evaluate(state, EvalOptions(referenceDate = ref)).evaluatedUsage

        assertNull(rows.first { it.entry.id == "p2" }.stretchEfficiency)
        val closing = rows.first { it.entry.id == "p3" }
        assertEquals(310.0, closing.stretchDistance!!, 1e-9)
        assertEquals(3.0, closing.stretchEnergy!!, 1e-9)
    }

    @Test
    fun `multiple partial refills accumulate and ineligible ones do not`() {
        val state = EngineState(
            vehicles = listOf(
                Vehicle(
                    id = "P1", name = "Petrol", type = "Bike", energySource = "Petrol",
                    initialOdometer = 0.0, tankCapacity = 12.0,
                ),
            ),
            usageEntries = listOf(
                UsageEntry("p1", "2026-11-01", "P1", 0.0, FILL_FULL_TANK, 5.0),
                UsageEntry(
                    "p2", "2026-11-02", "P1", 100.0, FILL_PARTIAL_REFILL, 2.0,
                    includeInMileage = false,
                ),
                UsageEntry("p3", "2026-11-03", "P1", 200.0, FILL_PARTIAL_REFILL, 1.0),
                UsageEntry("p4", "2026-11-04", "P1", 310.0, FILL_FULL_TANK, 3.0),
            ),
            maintenanceEntries = emptyList(),
            priceHistory = emptyList(),
            calibration = emptyList(),
            settings = SettingsEntity(),
        )
        val rows = CalcEngine.evaluate(state, EvalOptions(referenceDate = ref)).evaluatedUsage

        assertNull(rows.first { it.entry.id == "p2" }.stretchEfficiency)
        assertNull(rows.first { it.entry.id == "p3" }.stretchEfficiency)
        val closing = rows.first { it.entry.id == "p4" }
        assertEquals(310.0, closing.stretchDistance!!, 1e-9)
        assertEquals(4.0, closing.stretchEnergy!!, 1e-9)
        assertEquals(77.5, closing.stretchEfficiency!!, 1e-9)
    }

    @Test
    fun `editing eligibility recalculates affected intervals`() {
        fun state(middleEligible: Boolean) = EngineState(
            vehicles = listOf(
                Vehicle(
                    id = "P1", name = "Petrol", type = "Bike", energySource = "Petrol",
                    initialOdometer = 0.0, tankCapacity = 11.0,
                ),
            ),
            usageEntries = listOf(
                UsageEntry("p1", "2026-11-01", "P1", 0.0, FILL_FULL_TANK, 5.0),
                UsageEntry(
                    "p2", "2026-11-02", "P1", 100.0, FILL_PARTIAL_REFILL, 2.0,
                    includeInMileage = middleEligible,
                ),
                UsageEntry("p3", "2026-11-03", "P1", 310.0, FILL_FULL_TANK, 3.0),
            ),
            maintenanceEntries = emptyList(),
            priceHistory = emptyList(),
            calibration = emptyList(),
            settings = SettingsEntity(),
        )

        val included = CalcEngine.evaluate(state(true), EvalOptions(referenceDate = ref))
            .evaluatedUsage.first { it.entry.id == "p3" }
        val excluded = CalcEngine.evaluate(state(false), EvalOptions(referenceDate = ref))
            .evaluatedUsage.first { it.entry.id == "p3" }

        assertEquals(310.0, included.stretchDistance!!, 1e-9)
        assertEquals(5.0, included.stretchEnergy!!, 1e-9)
        assertEquals(62.0, included.stretchEfficiency!!, 1e-9)

        assertEquals(310.0, excluded.stretchDistance!!, 1e-9)
        assertEquals(3.0, excluded.stretchEnergy!!, 1e-9)
        assertEquals(310.0 / 3.0, excluded.stretchEfficiency!!, 1e-9)
    }

    @Test
    fun `mixed EV eligibility excludes charging energy but keeps cost`() {
        val state = EngineState(
            vehicles = listOf(
                Vehicle(
                    id = "E1", name = "EV", type = "Bike", energySource = SOURCE_ELECTRICITY,
                    initialOdometer = 0.0, batteryCapacity = 3.0,
                ),
            ),
            usageEntries = listOf(
                UsageEntry("e1", "2026-11-01", "E1", 0.0, FILL_FULL_CHARGE, 3.0, unitPrice = 7.0),
                UsageEntry(
                    "e2", "2026-11-02", "E1", 150.0, FILL_FULL_CHARGE, 3.0,
                    unitPrice = 7.0, includeInMileage = false,
                ),
                UsageEntry("e3", "2026-11-03", "E1", 300.0, FILL_FULL_CHARGE, 3.0, unitPrice = 7.0),
            ),
            maintenanceEntries = emptyList(),
            priceHistory = emptyList(),
            calibration = emptyList(),
            settings = SettingsEntity(),
        )
        val eval = CalcEngine.evaluate(state, EvalOptions(referenceDate = ref))
        val rows = eval.evaluatedUsage
        val closing = rows.first { it.entry.id == "e3" }

        assertNull(rows.first { it.entry.id == "e2" }.stretchEfficiency)
        assertEquals(300.0, closing.stretchDistance!!, 1e-9)
        assertEquals(3.0, closing.stretchEnergy!!, 1e-9)
        assertEquals(300.0 * (3.0 / 0.89) / 3.0, closing.kmPerCharge!!, 1e-9)
        assertEquals(63.0, eval.periodStatsByVehicle.getValue("E1").energySpend, 1e-9)
    }

    @Test
    fun `provisional efficiency ignores ineligible energy`() {
        val state = EngineState(
            vehicles = listOf(
                Vehicle(
                    id = "E1", name = "EV", type = "Bike", energySource = SOURCE_ELECTRICITY,
                    initialOdometer = 0.0, batteryCapacity = 3.0,
                    batteryRate = 20000.0, batteryLifeYears = 7.0, chargeCycles = 2500,
                    ratedKmPerCharge = 100.0, dailyKmPlan = 15.0,
                ),
            ),
            usageEntries = listOf(
                UsageEntry("a", "2026-11-01", "E1", 0.0, FILL_FULL_CHARGE, 2.0),
                UsageEntry(
                    "b", "2026-11-02", "E1", 50.0, FILL_PARTIAL_CHARGE, 1.0,
                    includeInMileage = false,
                ),
                UsageEntry("c", "2026-11-03", "E1", 100.0, FILL_RIDE_ONLY, null),
            ),
            maintenanceEntries = emptyList(),
            priceHistory = emptyList(),
            calibration = emptyList(),
            settings = SettingsEntity(),
        )
        val stats = CalcEngine.evaluate(state, EvalOptions(referenceDate = ref))
            .periodStatsByVehicle.getValue("E1")

        assertNull(stats.verifiedEfficiency)
        assertEquals(50.0, stats.provisionalEfficiency!!, 1e-9)
        assertEquals(50.0, stats.efficiencyUsed!!, 1e-9)
    }

    @Test
    fun `editing an earlier full odometer affects both adjacent intervals`() {
        fun state(middleOdo: Double) = EngineState(
            vehicles = listOf(
                Vehicle(
                    id = "P1", name = "Petrol", type = "Bike", energySource = "Petrol",
                    initialOdometer = 0.0, tankCapacity = 11.0,
                ),
            ),
            usageEntries = listOf(
                UsageEntry("p1", "2026-11-01", "P1", 0.0, FILL_FULL_TANK, 5.0),
                UsageEntry("p2", "2026-11-02", "P1", middleOdo, FILL_FULL_TANK, 3.0),
                UsageEntry("p3", "2026-11-03", "P1", 300.0, FILL_FULL_TANK, 3.0),
            ),
            maintenanceEntries = emptyList(),
            priceHistory = emptyList(),
            calibration = emptyList(),
            settings = SettingsEntity(),
        )

        val before = CalcEngine.evaluate(state(100.0), EvalOptions(referenceDate = ref)).evaluatedUsage
        assertEquals(100.0, before.first { it.entry.id == "p2" }.stretchDistance!!, 1e-9)
        assertEquals(200.0, before.first { it.entry.id == "p3" }.stretchDistance!!, 1e-9)

        val after = CalcEngine.evaluate(state(120.0), EvalOptions(referenceDate = ref)).evaluatedUsage
        assertEquals(120.0, after.first { it.entry.id == "p2" }.stretchDistance!!, 1e-9)
        assertEquals(180.0, after.first { it.entry.id == "p3" }.stretchDistance!!, 1e-9)
    }

    @Test
    fun `no eligible full boundary leaves mileage unavailable or provisional only`() {
        val state = EngineState(
            vehicles = listOf(
                Vehicle(
                    id = "P1", name = "Petrol", type = "Bike", energySource = "Petrol",
                    initialOdometer = 0.0, tankCapacity = 11.0,
                ),
            ),
            usageEntries = listOf(
                UsageEntry(
                    "p1", "2026-11-01", "P1", 0.0, FILL_FULL_TANK, 5.0,
                    includeInMileage = false,
                ),
                UsageEntry("p2", "2026-11-02", "P1", 100.0, FILL_FULL_TANK, 3.0),
                UsageEntry("p3", "2026-11-03", "P1", 200.0, FILL_PARTIAL_REFILL, 2.0),
            ),
            maintenanceEntries = emptyList(),
            priceHistory = emptyList(),
            calibration = emptyList(),
            settings = SettingsEntity(),
        )
        val eval = CalcEngine.evaluate(state, EvalOptions(referenceDate = ref))
        val stats = eval.periodStatsByVehicle.getValue("P1")

        assertNull(stats.verifiedEfficiency)
        assertEquals("Provisional", stats.efficiencyBasis)
        assertEquals(40.0, stats.provisionalEfficiency!!, 1e-9)
        assertNull(eval.evaluatedUsage.first { it.entry.id == "p3" }.stretchEfficiency)
    }

    @Test
    fun `ev savings require both electric and petrol cost bases`() {
        val evOnly = CalcEngine.fleetTotals(
            listOf(sampleEval.periodStatsByVehicle.getValue("Vh1")),
        )
        assertNull(evOnly.evSavings)

        val withPetrol = sample.copy(
            usageEntries = sample.usageEntries + listOf(
                UsageEntry("p1", "2026-11-01", "Vh3", 20000.0, FILL_FULL_TANK, 5.0),
                UsageEntry("p2", "2026-11-10", "Vh3", 20300.0, FILL_FULL_TANK, 3.0),
            ),
        )
        val eval = CalcEngine.evaluate(withPetrol, EvalOptions(referenceDate = ref))
        val fleet = CalcEngine.fleetTotals(eval.periodStatsByVehicle.values.toList())
        assertNotNull(fleet.evSavings)
        assertEquals(fleet.evDistance, 254.0, 1e-9)
    }

    @Test
    fun `cost rounding matches JavaScript two decimal rounding`() {
        assertEquals(477.75, CalcEngine.jsRound2(477.754999), 1e-9)
        assertEquals(64.19, CalcEngine.jsRound2(64.185), 1e-9)
        assertEquals(0.16, CalcEngine.jsRound2(0.155), 1e-9)
    }

    @Test
    fun `month window starts at configured month`() {
        val months = CalcEngine.monthWindow("2026-10")
        assertEquals(12, months.size)
        assertEquals("2026-10", months.first().ym)
        assertEquals("Oct 26", months.first().label)
        assertEquals("2027-09", months.last().ym)
    }

    @Test
    fun `hours-only vehicle requires hours and counts operating hours`() {
        val state = EngineState(
            vehicles = listOf(Vehicle(id = "A1", name = "Tractor", type = "Tractor", energySource = SOURCE_PETROL, trackingBasis = "operating_hours")),
            usageEntries = listOf(UsageEntry("a", "2026-11-20", "A1", odometer = null, distanceKm = null, hoursUsed = 4.5, fillType = FILL_RIDE_ONLY)),
            maintenanceEntries = emptyList(),
            priceHistory = emptyList(),
            calibration = emptyList(),
            settings = SettingsEntity(),
        )
        val row = CalcEngine.evaluate(state, EvalOptions(referenceDate = ref)).evaluatedUsage.single()
        assertEquals(STATUS_OK, row.status)
        assertEquals(4.5, row.trackingHours, 1e-9)
    }

    @Test
    fun `hours-only vehicle without hours is invalid`() {
        val state = EngineState(
            vehicles = listOf(Vehicle(id = "A2", name = "Tractor", type = "Tractor", energySource = SOURCE_PETROL, trackingBasis = "operating_hours")),
            usageEntries = listOf(UsageEntry("a", "2026-11-20", "A2", odometer = null, distanceKm = null, hoursUsed = null, fillType = FILL_FULL_TANK, unitPrice = 100.0)),
            maintenanceEntries = emptyList(),
            priceHistory = emptyList(),
            calibration = emptyList(),
            settings = SettingsEntity(),
        )
        val row = CalcEngine.evaluate(state, EvalOptions(referenceDate = ref)).evaluatedUsage.single()
        assertEquals(STATUS_ERROR, row.status)
    }

    @Test
    fun `distance vehicle without odometer or explicit distance is invalid`() {
        val state = EngineState(
            vehicles = listOf(Vehicle(id = "A3", name = "Car", type = "Car", energySource = SOURCE_PETROL, trackingBasis = "distance_km")),
            usageEntries = listOf(UsageEntry("a", "2026-11-20", "A3", odometer = null, distanceKm = null, fillType = FILL_FULL_TANK, unitPrice = 100.0)),
            maintenanceEntries = emptyList(),
            priceHistory = emptyList(),
            calibration = emptyList(),
            settings = SettingsEntity(),
        )
        val row = CalcEngine.evaluate(state, EvalOptions(referenceDate = ref)).evaluatedUsage.single()
        assertEquals(STATUS_ERROR, row.status)
    }

    @Test
    fun `explicit first distance is preserved`() {
        val state = EngineState(
            vehicles = listOf(Vehicle(id = "A4", name = "Truck", type = "Truck", energySource = SOURCE_PETROL, trackingBasis = "distance_km", initialOdometer = 0.0)),
            usageEntries = listOf(UsageEntry("a", "2026-11-20", "A4", odometer = null, distanceKm = 500.0, fillType = FILL_RIDE_ONLY)),
            maintenanceEntries = emptyList(),
            priceHistory = emptyList(),
            calibration = emptyList(),
            settings = SettingsEntity(),
        )
        val row = CalcEngine.evaluate(state, EvalOptions(referenceDate = ref)).evaluatedUsage.single()
        assertEquals(STATUS_CHECK, row.status)
        assertEquals(500.0, row.distance, 1e-9)
    }
}

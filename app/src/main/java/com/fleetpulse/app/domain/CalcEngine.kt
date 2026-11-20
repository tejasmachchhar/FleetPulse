package com.fleetpulse.app.domain

import com.fleetpulse.app.data.db.BASIS_ESTIMATED
import com.fleetpulse.app.data.db.BASIS_MEASURED
import com.fleetpulse.app.data.db.CalibrationPoint
import com.fleetpulse.app.data.db.FILL_FULL_CHARGE
import com.fleetpulse.app.data.db.FILL_FULL_TANK
import com.fleetpulse.app.data.db.FILL_PARTIAL_CHARGE
import com.fleetpulse.app.data.db.FILL_RIDE_ONLY
import com.fleetpulse.app.data.db.MaintenanceEntry
import com.fleetpulse.app.data.db.PriceEntry
import com.fleetpulse.app.data.db.SOURCE_ELECTRICITY
import com.fleetpulse.app.data.db.SOURCE_PETROL
import com.fleetpulse.app.data.db.SettingsEntity
import com.fleetpulse.app.data.db.STATUS_CHECK
import com.fleetpulse.app.data.db.STATUS_ERROR
import com.fleetpulse.app.data.db.STATUS_OK
import com.fleetpulse.app.data.db.UsageEntry
import com.fleetpulse.app.data.db.Vehicle
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.floor
import kotlin.math.max

/**
 * Pure-Kotlin port of the FleetPulse v4 JavaScript calculation engine
 * (`evaluateAllData`, `interpolateVoltage`, `getEffectivePrice`,
 * `getPeriodBounds`).
 *
 * No Android dependencies: this is exercised directly by JVM unit tests.
 */

data class PeriodFilter(
    val key: String,
    val label: String,
    val start: String,
    val end: String,
)

data class EngineState(
    val vehicles: List<Vehicle>,
    val usageEntries: List<UsageEntry>,
    val maintenanceEntries: List<MaintenanceEntry>,
    val priceHistory: List<PriceEntry>,
    val calibration: List<CalibrationPoint>,
    val settings: SettingsEntity,
)

data class EvalOptions(
    val referenceDate: LocalDate = LocalDate.now(),
    val periodKey: String = "all",
    val customDateFrom: String = "2026-01-01",
    val customDateTo: String = "2027-12-31",
)

data class EvaluatedUsage(
    val entry: UsageEntry,
    val vehicleName: String,
    val energySource: String,
    val pctBefore: Double?,
    val pctAfter: Double?,
    val distance: Double,
    val trackingHours: Double = 0.0,
    val energyUsed: Double,
    val basis: String?,
    val applicablePrice: Double?,
    val totalCost: Double,
    val stretchEfficiency: Double?,
    val kmPerCharge: Double?,
    val isVerifiedStretch: Boolean,
    val isPlausibleStretch: Boolean,
    val isValidForAverage: Boolean,
    val stretchDistance: Double?,
    val stretchEnergy: Double?,
    val stretchCost: Double?,
    val stretchBasis: String?,
    val status: String,
    val checkMessage: String,
    val errors: List<String>,
    val checks: List<String>,
) {
    val isElectric: Boolean get() = energySource == SOURCE_ELECTRICITY
}

data class VehicleAllTimeStats(
    val vehicle: Vehicle,
    val currentOdometer: Double,
    val validStretchesCount: Int,
    val latestValidMileage: Double?,
    val latestValidEfficiency: Double?,
    val latestValidBasis: String?,
    val avgLast5Mileage: Double?,
    val last5Count: Int,
    val allTimeAvgKmPerCharge: Double?,
    val batteryPrice: Double,
    val kmPerChargeUsed: Double,
    val kmPerChargeSource: String,
    val maxRunningCapacityKm: Double,
    val distanceOverBatteryLifeKm: Double,
    val distanceConsideredKm: Double,
    val batteryWearCostPerKm: Double,
)

data class PeriodStats(
    val vehicle: Vehicle,
    val entriesCount: Int,
    val distance: Double,
    val trackingHours: Double = 0.0,
    val energyAdded: Double,
    val measuredEnergy: Double,
    val estimatedEnergy: Double,
    val energySpend: Double,
    val avgPricePaid: Double?,
    val verifiedDistance: Double,
    val verifiedEnergy: Double,
    val verifiedEnergyCost: Double,
    val verifiedEfficiency: Double?,
    val avgConsumptionPer100Km: Double?,
    val shareMeasuredVerifiedKm: Double,
    val measuredOnlyEfficiency: Double?,
    val estimatedOnlyEfficiency: Double?,
    val bestEfficiency: Double?,
    val worstEfficiency: Double?,
    val latestStretchEff: Double?,
    val provisionalEfficiency: Double?,
    val efficiencyUsed: Double?,
    val efficiencyBasis: String,
    val simpleEnergyCostPerKm: Double?,
    val verifiedEnergyCostPerKm: Double?,
    val energyCostPerKmUsed: Double?,
    val energyCostBasis: String,
    val batteryWearCostPerKm: Double,
    val batteryWearInPeriod: Double,
    val runningCostPerKm: Double?,
    val totalRunningCost: Double,
    val maintenanceSpend: Double,
    val maintenanceCostPerKm: Double?,
    val allInCostPerKm: Double?,
) {
    val isElectric: Boolean get() = vehicle.energySource == SOURCE_ELECTRICITY
}

data class EvaluatedMaintenance(
    val entry: MaintenanceEntry,
    val vehicleName: String,
    val currentOdo: Double,
    val daysLeft: Long?,
    val kmLeft: Double?,
    val status: String,
    val pending: Boolean,
) {
    val hasReminder: Boolean get() = entry.nextDueDate != null || entry.nextDueKm != null
}

data class CalibrationCheck(
    val point: CalibrationPoint,
    val check: String,
)

data class EvalResult(
    val evaluatedUsage: List<EvaluatedUsage>,
    val evaluatedMaintenance: List<EvaluatedMaintenance>,
    val vehicleAllTimeStats: Map<String, VehicleAllTimeStats>,
    val periodStatsByVehicle: Map<String, PeriodStats>,
    val nearestReminderByVehicle: Map<String, EvaluatedMaintenance?>,
    val periodBounds: PeriodFilter,
    val calibrationChecks: List<CalibrationCheck>,
) {
    val errorCount: Int get() = evaluatedUsage.count { it.status == STATUS_ERROR }
    val checkCount: Int get() = evaluatedUsage.count { it.status == STATUS_CHECK }
}

object CalcEngine {

    // ---------------------------------------------------------------------
    // Periods, tariffs, calibration
    // ---------------------------------------------------------------------

    fun getPeriodBounds(periodKey: String, ref: LocalDate, customFrom: String, customTo: String): PeriodFilter {
        fun iso(d: LocalDate) = d.toString()
        return when (periodKey) {
            "all" -> PeriodFilter("all", "All recorded data", "1900-01-01", "9999-12-31")
            "current_month" -> {
                val ym = YearMonth.from(ref)
                PeriodFilter(
                    periodKey,
                    "Current month (${ym.year}-${"%02d".format(ym.monthValue)})",
                    iso(ym.atDay(1)),
                    iso(ym.atEndOfMonth()),
                )
            }
            "last_30" -> PeriodFilter(
                periodKey, "Last 30 days", iso(ref.minusDays(30)), iso(ref),
            )
            "last_90" -> PeriodFilter(
                periodKey, "Last 90 days", iso(ref.minusDays(90)), iso(ref),
            )
            "current_fy" -> {
                val y = if (ref.monthValue >= 4) ref.year else ref.year - 1
                PeriodFilter(
                    periodKey,
                    "FY $y–${(y + 1).toString().takeLast(2)}",
                    "$y-04-01",
                    "${y + 1}-03-31",
                )
            }
            "prev_fy" -> {
                val y = (if (ref.monthValue >= 4) ref.year else ref.year - 1) - 1
                PeriodFilter(
                    periodKey,
                    "FY $y–${(y + 1).toString().takeLast(2)}",
                    "$y-04-01",
                    "${y + 1}-03-31",
                )
            }
            "custom" -> PeriodFilter(
                periodKey,
                "$customFrom to $customTo",
                customFrom.ifBlank { "2026-01-01" },
                customTo.ifBlank { "2027-12-31" },
            )
            else -> PeriodFilter("all", "All recorded data", "1900-01-01", "9999-12-31")
        }
    }

    fun interpolateVoltage(
        vehicleId: String,
        voltage: Double,
        calibration: List<CalibrationPoint>,
    ): VoltageResult {
        val pts = calibration
            .filter { it.vehicleId == vehicleId && !it.voltage.isNaN() }
            .sortedBy { it.voltage }
        if (pts.isEmpty()) return VoltageResult(null, outOfRange = true, missingCurve = true)
        if (voltage <= pts.first().voltage) {
            return VoltageResult(pts.first().percent, outOfRange = voltage < pts.first().voltage, missingCurve = false)
        }
        if (voltage >= pts.last().voltage) {
            return VoltageResult(pts.last().percent, outOfRange = voltage > pts.last().voltage, missingCurve = false)
        }
        for (i in 0 until pts.size - 1) {
            val p0 = pts[i]
            val p1 = pts[i + 1]
            if (voltage >= p0.voltage && voltage <= p1.voltage) {
                if (p1.voltage == p0.voltage) return VoltageResult(p0.percent, false, false)
                val pct = p0.percent + ((voltage - p0.voltage) / (p1.voltage - p0.voltage)) * (p1.percent - p0.percent)
                return VoltageResult(pct, false, false)
            }
        }
        return VoltageResult(null, outOfRange = true, missingCurve = false)
    }

    data class VoltageResult(val percent: Double?, val outOfRange: Boolean, val missingCurve: Boolean)

    fun getEffectivePrice(
        energySource: String,
        date: String,
        priceHistory: List<PriceEntry>,
    ): Double? {
        val applicable = priceHistory
            .filter { it.energySource == energySource && it.effectiveFrom <= date }
            .sortedBy { it.effectiveFrom }
        return applicable.lastOrNull()?.price
    }

    // ---------------------------------------------------------------------
    // Main evaluation — line-for-line port of evaluateAllData()
    // ---------------------------------------------------------------------

    fun evaluate(state: EngineState, options: EvalOptions = EvalOptions()): EvalResult {
        val vehicles = state.vehicles
        val settings = state.settings
        val vehicleMap = vehicles.associateBy { it.id }

        val byVehicle = LinkedHashMap<String, MutableList<Pair<UsageEntry, Int>>>()
        vehicles.forEach { byVehicle[it.id] = mutableListOf() }
        state.usageEntries.forEachIndexed { idx, entry ->
            byVehicle.getOrPut(entry.vehicleId) { mutableListOf() }.add(entry to idx)
        }

        val evaluatedUsageMap = HashMap<String, EvaluatedUsage>()
        val currentOdoByVehicle = HashMap<String, Double>()
        vehicles.forEach { currentOdoByVehicle[it.id] = it.initialOdometer }

        for ((vehicleId, list) in byVehicle.entries) {
            val vehicle = vehicleMap[vehicleId]
            val usesHours = vehicle != null && vehicle.isOperatingHoursBasis
            val usesDistance = vehicle != null && vehicle.isDistanceBasis
            list.sortWith(compareBy({ it.first.date }, { it.second }))

            var prevOdo = vehicle?.initialOdometer ?: 0.0
            var hasPrevFull = false
            var accDistSinceFull = 0.0
            var accEnergySinceFull = 0.0
            var accCostSinceFull = 0.0
            var stretchHasEstimated = false
            var stretchValidPossible = true

            for (i in list.indices) {
                val e = list[i].first
                val errors = mutableListOf<String>()
                val checks = mutableListOf<String>()
                var distance = 0.0
                var trackingHours = 0.0
                val odoNum = e.odometer
                val explicitDistance = e.distanceKm
                val hoursUsed = e.hoursUsed

                if (e.date.isBlank() || vehicle == null) {
                    errors.add("Missing mandatory fields (Date or Vehicle)")
                } else if (usesHours && usesDistance.not() && (explicitDistance == null && hoursUsed == null)) {
                    errors.add("Missing mandatory fields (Odometer or Operating Hours)")
                } else if (explicitDistance != null && usesDistance) {
                    distance = explicitDistance
                    if (i == 0 && distance > settings.maxPlausibleDistance) {
                        checks.add(
                            "First distance entry is far above the initial odometer - used as the starting reading (0 km).",
                        )
                        distance = 0.0
                    }
                    if (distance < 0) errors.add("Distance is negative.")
                } else if (odoNum != null && !odoNum.isNaN() && usesDistance) {
                    if (i == 0) {
                        val diff = odoNum - (vehicle.initialOdometer)
                        if (diff < 0) {
                            errors.add("Odometer (${fmt1(odoNum)}) is below Initial Odometer (${fmt(vehicle.initialOdometer)})")
                        } else if (diff > settings.maxPlausibleDistance) {
                            checks.add(
                                "First entry is far above the Initial Odometer - used as the starting reading (0 km). " +
                                    "Set Initial Odometer in Vehicle Setup to the reading when tracking began",
                            )
                            distance = 0.0
                        } else {
                            distance = diff
                        }
                    } else {
                        val diff = odoNum - prevOdo
                        if (diff < 0) {
                            errors.add("Odometer (${fmt1(odoNum)}) is lower than previous reading (${fmt1(prevOdo)})")
                        } else {
                            distance = diff
                            if (diff == 0.0) checks.add("Duplicate odometer reading (0 km since previous entry)")
                            if (diff > settings.maxPlausibleDistance) {
                                checks.add(
                                    "Distance gap (${fmt1(diff)} km) exceeds max plausible distance " +
                                        "(${fmt(settings.maxPlausibleDistance)} km)",
                                )
                            }
                        }
                    }
                } else if (odoNum == null && !usesHours && !usesDistance) {
                    errors.add("Missing mandatory fields (Odometer)")
                }

                if (hoursUsed != null && hoursUsed >= 0) trackingHours = hoursUsed

                if (errors.isEmpty() && odoNum != null && !odoNum.isNaN() && usesDistance) {
                    prevOdo = odoNum
                    if (odoNum > (currentOdoByVehicle[vehicleId] ?: 0.0)) {
                        currentOdoByVehicle[vehicleId] = odoNum
                    }
                }

                // Energy calculation
                var pctBefore: Double? = null
                var pctAfter: Double? = null
                var energyUsed = 0.0
                var basis: String? = null

                if (vehicle != null && vehicle.energySource == SOURCE_ELECTRICITY) {
                    if (e.fillType == FILL_RIDE_ONLY) {
                        energyUsed = 0.0
                        basis = null
                    } else if (e.energyAdded != null && e.energyAdded > 0) {
                        energyUsed = e.energyAdded!!
                        basis = BASIS_MEASURED
                        val eff = (settings.chargerEfficiency.takeIf { it > 0 } ?: 89.0) / 100.0
                        val maxWall = ((vehicle.batteryCapacity ?: 3.0) / eff) * 1.15
                        if (energyUsed > maxWall) {
                            checks.add("Energy added (${fmt2(energyUsed)} kWh) exceeds battery wall capacity")
                        }
                    } else if (e.voltsBefore != null) {
                        val vb = interpolateVoltage(vehicleId, e.voltsBefore!!, state.calibration)
                        if (vb.missingCurve) {
                            errors.add("Missing voltage calibration curve for this vehicle")
                        } else {
                            pctBefore = vb.percent
                            if (vb.outOfRange) checks.add("Volts Before (${fmt(e.voltsBefore)}) V) is outside calibration range")
                            if (e.fillType == FILL_FULL_CHARGE) {
                                pctAfter = 100.0
                            } else if (e.fillType == FILL_PARTIAL_CHARGE) {
                                if (e.voltsAfter != null) {
                                    val va = interpolateVoltage(vehicleId, e.voltsAfter!!, state.calibration)
                                    pctAfter = va.percent
                                    if (va.outOfRange) checks.add("Volts After (${fmt(e.voltsAfter)} V) is outside calibration range")
                                } else {
                                    errors.add("Partial charge without meter requires Volts After")
                                }
                            }
                            if (pctBefore != null && pctAfter != null) {
                                if (pctAfter!! < pctBefore!!) {
                                    errors.add(
                                        "Battery % after charge (${fmt0(pctAfter!!)}%) is lower than before charge " +
                                            "(${fmt0(pctBefore!!)}%)",
                                    )
                                } else {
                                    val eff = (settings.chargerEfficiency.takeIf { it > 0 } ?: 89.0) / 100.0
                                    energyUsed = ((pctAfter!! - pctBefore!!) / 100.0) *
                                        ((vehicle.batteryCapacity ?: 0.0) / eff)
                                    basis = BASIS_ESTIMATED
                                }
                            }
                        }
                    } else {
                        errors.add("Enter either metered kWh or Volts Before")
                    }
                } else if (vehicle != null && vehicle.energySource == SOURCE_PETROL) {
                    if (e.fillType == FILL_RIDE_ONLY) {
                        energyUsed = 0.0
                        basis = null
                    } else if (e.energyAdded != null && e.energyAdded > 0) {
                        energyUsed = e.energyAdded!!
                        basis = BASIS_MEASURED
                        if ((vehicle.tankCapacity ?: 0.0) > 0 && energyUsed > vehicle.tankCapacity!! * 1.05) {
                            checks.add(
                                "Petrol quantity (${fmt2(energyUsed)} L) exceeds tank capacity (${fmt(vehicle.tankCapacity!!)} L)",
                            )
                        }
                    } else {
                        errors.add("Enter petrol quantity in litres")
                    }
                }

                // Price and total cost
                val applicablePrice = if (e.unitPrice != null && e.unitPrice > 0) {
                    e.unitPrice
                } else if (vehicle != null) {
                    getEffectivePrice(vehicle.energySource, e.date, state.priceHistory)
                } else {
                    null
                }

                if (energyUsed > 0 && applicablePrice == null) {
                    checks.add("Missing energy price in Settings for this date")
                }
                val rawCost = if (energyUsed > 0 && applicablePrice != null) energyUsed * applicablePrice!! else 0.0
                val totalCost = jsRound2(rawCost)

                // Full-to-full stretch evaluation
                var stretchEfficiency: Double? = null
                var kmPerCharge: Double? = null
                var isVerifiedStretch = false
                var isPlausibleStretch = true
                var stretchDistance: Double? = null
                var stretchEnergy: Double? = null
                var stretchCost: Double? = null
                var stretchBasis: String? = null

                val isFullFill = e.fillType == FILL_FULL_CHARGE || e.fillType == FILL_FULL_TANK

                if (errors.isEmpty()) {
                    if (!hasPrevFull) {
                        // Only an eligible full fill can open a measurement interval.
                        if (isFullFill && e.includeInMileage) {
                            hasPrevFull = true
                            accDistSinceFull = 0.0
                            accEnergySinceFull = 0.0
                            accCostSinceFull = 0.0
                            stretchHasEstimated = false
                            stretchValidPossible = true
                        }
                    } else {
                        accDistSinceFull += distance
                        // Excluded entries keep their distance and cost but never
                        // contribute fuel/energy to the mileage denominator.
                        if (e.includeInMileage) {
                            accEnergySinceFull += energyUsed
                            if (basis == BASIS_ESTIMATED) stretchHasEstimated = true
                        }
                        accCostSinceFull += totalCost

                        if (isFullFill && e.includeInMileage) {
                            if (accDistSinceFull > 0 && accEnergySinceFull > 0 && stretchValidPossible) {
                                stretchEfficiency = accDistSinceFull / accEnergySinceFull
                                isVerifiedStretch = true
                                stretchDistance = accDistSinceFull
                                stretchEnergy = accEnergySinceFull
                                stretchCost = accCostSinceFull
                                stretchBasis = if (stretchHasEstimated) BASIS_ESTIMATED else BASIS_MEASURED

                                kmPerCharge = if (vehicle != null && vehicle.energySource == SOURCE_ELECTRICITY) {
                                    val eff = (settings.chargerEfficiency.takeIf { it > 0 } ?: 89.0) / 100.0
                                    stretchDistance!! * ((vehicle.batteryCapacity ?: 0.0) / eff) / stretchEnergy!!
                                } else {
                                    stretchEfficiency
                                }

                                val bounds = plausibleBounds(settings, vehicle?.energySource)
                                if (bounds != null && (stretchEfficiency!! < bounds.first || stretchEfficiency!! > bounds.second)) {
                                    isPlausibleStretch = false
                                    checks.add(
                                        "Implausible full-to-full efficiency (${fmt1(stretchEfficiency!!)} km/${bounds.third})",
                                    )
                                }
                            }
                            accDistSinceFull = 0.0
                            accEnergySinceFull = 0.0
                            accCostSinceFull = 0.0
                            stretchHasEstimated = false
                            stretchValidPossible = true
                        }
                    }
                } else {
                    stretchValidPossible = false
                }

                val isValidForAverage = isVerifiedStretch && (!settings.excludeImplausible || isPlausibleStretch)

                var status = STATUS_OK
                var checkMessage = "OK"
                if (errors.isNotEmpty()) {
                    status = STATUS_ERROR
                    checkMessage = "ERROR: " + errors.joinToString("; ")
                } else if (checks.isNotEmpty()) {
                    status = STATUS_CHECK
                    checkMessage = "CHECK: " + checks.joinToString("; ")
                } else if (isVerifiedStretch) {
                    if (vehicle != null && vehicle.energySource == SOURCE_ELECTRICITY) {
                        checkMessage = "OK - ${Math.round(kmPerCharge ?: 0.0)} km/charge " +
                            "(${if (stretchBasis == BASIS_ESTIMATED) "estimated from voltage" else "measured"})"
                    } else {
                        checkMessage = "OK - ${fmt1(stretchEfficiency!!)} km/L (verified full-to-full)"
                    }
                }

                evaluatedUsageMap[e.id] = EvaluatedUsage(
                    entry = e,
                    vehicleName = vehicle?.name ?: e.vehicleId,
                    energySource = vehicle?.energySource ?: SOURCE_ELECTRICITY,
                    pctBefore = pctBefore,
                    pctAfter = pctAfter,
                    distance = distance,
                    trackingHours = trackingHours,
                    energyUsed = energyUsed,
                    basis = basis,
                    applicablePrice = applicablePrice,
                    totalCost = totalCost,
                    stretchEfficiency = stretchEfficiency,
                    kmPerCharge = kmPerCharge,
                    isVerifiedStretch = isVerifiedStretch,
                    isPlausibleStretch = isPlausibleStretch,
                    isValidForAverage = isValidForAverage,
                    stretchDistance = stretchDistance,
                    stretchEnergy = stretchEnergy,
                    stretchCost = stretchCost,
                    stretchBasis = stretchBasis,
                    status = status,
                    checkMessage = checkMessage,
                    errors = errors.toList(),
                    checks = checks.toList(),
                )
            }
        }

        val evaluatedUsage = state.usageEntries.mapNotNull { evaluatedUsageMap[it.id] }

        // All-time stats + battery wear
        val vehicleAllTimeStats = LinkedHashMap<String, VehicleAllTimeStats>()
        for (v in vehicles) {
            val vLogs = evaluatedUsage
                .filter { it.entry.vehicleId == v.id && it.status != STATUS_ERROR }
                .sortedBy { it.entry.date }

            val validStretches = vLogs.filter { it.isValidForAverage }
            val latestValid = validStretches.lastOrNull()
            val last5Valid = validStretches.takeLast(5)
            val avgLast5KmPerCharge = if (last5Valid.isNotEmpty()) {
                last5Valid.sumOf { it.kmPerCharge ?: 0.0 } / last5Valid.size
            } else {
                null
            }
            val allTimeAvgKmPerCharge = if (validStretches.isNotEmpty()) {
                validStretches.sumOf { it.kmPerCharge ?: 0.0 } / validStretches.size
            } else {
                null
            }

            var batteryPrice = 0.0
            var kmPerChargeUsed = 0.0
            var kmPerChargeSource = "N/A"
            var maxRunningCapacityKm = 0.0
            var distanceOverBatteryLifeKm = 0.0
            var distanceConsideredKm = 0.0
            var batteryWearCostPerKm = 0.0

            if (v.energySource == SOURCE_ELECTRICITY) {
                val cap = v.batteryCapacity ?: 0.0
                val rate = v.batteryRate ?: 0.0
                val cycles = (v.chargeCycles ?: 0).toDouble()
                val lifeYrs = v.batteryLifeYears ?: 0.0
                val dailyKm = v.dailyKmPlan ?: 0.0
                val ratedKm = v.ratedKmPerCharge ?: 0.0

                if (cap > 0 && rate > 0 && cycles > 0 && lifeYrs > 0 && dailyKm > 0) {
                    batteryPrice = cap * rate
                    if (allTimeAvgKmPerCharge != null && allTimeAvgKmPerCharge > 0) {
                        kmPerChargeUsed = allTimeAvgKmPerCharge
                        kmPerChargeSource = "Actual (${validStretches.size} valid " +
                            "${if (validStretches.size == 1) "charge" else "charges"})"
                    } else {
                        kmPerChargeUsed = ratedKm
                        kmPerChargeSource = "Rated (no valid charge yet)"
                    }
                    maxRunningCapacityKm = cycles * kmPerChargeUsed
                    distanceOverBatteryLifeKm = lifeYrs * 365 * dailyKm
                    distanceConsideredKm = minOf(maxRunningCapacityKm, distanceOverBatteryLifeKm)
                    batteryWearCostPerKm = if (distanceConsideredKm > 0) batteryPrice / distanceConsideredKm else 0.0
                }
            }

            vehicleAllTimeStats[v.id] = VehicleAllTimeStats(
                vehicle = v,
                currentOdometer = currentOdoByVehicle[v.id] ?: v.initialOdometer,
                validStretchesCount = validStretches.size,
                latestValidMileage = latestValid?.kmPerCharge,
                latestValidEfficiency = latestValid?.stretchEfficiency,
                latestValidBasis = latestValid?.stretchBasis,
                avgLast5Mileage = avgLast5KmPerCharge,
                last5Count = last5Valid.size,
                allTimeAvgKmPerCharge = allTimeAvgKmPerCharge,
                batteryPrice = batteryPrice,
                kmPerChargeUsed = kmPerChargeUsed,
                kmPerChargeSource = kmPerChargeSource,
                maxRunningCapacityKm = maxRunningCapacityKm,
                distanceOverBatteryLifeKm = distanceOverBatteryLifeKm,
                distanceConsideredKm = distanceConsideredKm,
                batteryWearCostPerKm = batteryWearCostPerKm,
            )
        }

        // Maintenance evaluation and reminder retirement
        data class Latest(val id: String, val date: String, val idx: Int)
        val latestCategoryRecordMap = HashMap<String, Latest>()
        state.maintenanceEntries.forEachIndexed { idx, m ->
            val key = "${m.vehicleId}__${m.category}"
            val prev = latestCategoryRecordMap[key]
            if (prev == null || m.date > prev.date || (m.date == prev.date && idx > prev.idx)) {
                latestCategoryRecordMap[key] = Latest(m.id, m.date, idx)
            }
        }

        val refDate = options.referenceDate
        val evaluatedMaintenance = state.maintenanceEntries.map { m ->
            val v = vehicleMap[m.vehicleId]
            val currOdo = currentOdoByVehicle[m.vehicleId] ?: 0.0
            val hasReminder = m.nextDueDate != null || m.nextDueKm != null
            val latestInCat = latestCategoryRecordMap["${m.vehicleId}__${m.category}"]
            val isSuperseded = hasReminder && latestInCat != null && latestInCat.id != m.id

            var daysLeft: Long? = null
            var kmLeft: Double? = null
            if (m.nextDueDate != null) {
                daysLeft = runCatching {
                    ChronoUnit.DAYS.between(refDate, LocalDate.parse(m.nextDueDate))
                }.getOrNull()
            }
            if (m.nextDueKm != null) {
                kmLeft = m.nextDueKm!! - currOdo
            }

            var status = "Logged"
            var pending = false
            if (hasReminder) {
                if (isSuperseded) {
                    status = "Completed"
                } else {
                    pending = true
                    val isOverdue = (daysLeft != null && daysLeft!! < 0) || (kmLeft != null && kmLeft!! < 0)
                    val isDueSoon = !isOverdue && (
                        (daysLeft != null && daysLeft!! <= state.settings.warnDays) ||
                            (kmLeft != null && kmLeft!! <= state.settings.warnKm)
                        )
                    status = if (isOverdue) "Overdue" else if (isDueSoon) "Due Soon" else "OK"
                }
            }

            EvaluatedMaintenance(
                entry = m,
                vehicleName = v?.name ?: m.vehicleId,
                currentOdo = currOdo,
                daysLeft = daysLeft,
                kmLeft = kmLeft,
                status = status,
                pending = pending,
            )
        }

        val nearestReminderByVehicle = LinkedHashMap<String, EvaluatedMaintenance?>()
        for (v in vehicles) {
            val pendingList = evaluatedMaintenance.filter { it.entry.vehicleId == v.id && it.pending }
            nearestReminderByVehicle[v.id] = if (pendingList.isEmpty()) {
                null
            } else {
                pendingList.sortedWith(
                    compareBy(
                        { statusRank(it.status) },
                        { it.daysLeft ?: 999999L },
                    ),
                ).first()
            }
        }

        // Period-filtered metrics
        val periodBounds = getPeriodBounds(
            options.periodKey,
            options.referenceDate,
            options.customDateFrom,
            options.customDateTo,
        )
        val periodStatsByVehicle = LinkedHashMap<String, PeriodStats>()

        for (v in vehicles) {
            val allTime = vehicleAllTimeStats.getValue(v.id)
            val pLogs = evaluatedUsage.filter {
                it.entry.vehicleId == v.id &&
                    it.entry.date >= periodBounds.start &&
                    it.entry.date <= periodBounds.end &&
                    it.status != STATUS_ERROR
            }

            val distance = pLogs.sumOf { it.distance }
            val energyAdded = pLogs.sumOf { it.energyUsed }
            // Expenses always include every saved entry, but a provisional
            // efficiency denominator must respect the eligibility control.
            val eligibleEnergy = pLogs.filter { it.entry.includeInMileage }.sumOf { it.energyUsed }
            val measuredEnergy = pLogs.filter { it.basis == BASIS_MEASURED }.sumOf { it.energyUsed }
            val estimatedEnergy = pLogs.filter { it.basis == BASIS_ESTIMATED }.sumOf { it.energyUsed }
            val energySpend = pLogs.sumOf { it.totalCost }

            val verifiedStretches = pLogs.filter { it.isValidForAverage }
            val verifiedDistance = verifiedStretches.sumOf { it.stretchDistance ?: 0.0 }
            val verifiedEnergy = verifiedStretches.sumOf { it.stretchEnergy ?: 0.0 }
            val verifiedEnergyCost = verifiedStretches.sumOf { it.stretchCost ?: 0.0 }

            val measuredStretches = verifiedStretches.filter { it.stretchBasis == BASIS_MEASURED }
            val estimatedStretches = verifiedStretches.filter { it.stretchBasis == BASIS_ESTIMATED }
            val measuredVerifiedDist = measuredStretches.sumOf { it.stretchDistance ?: 0.0 }
            val measuredVerifiedEnergy = measuredStretches.sumOf { it.stretchEnergy ?: 0.0 }
            val estimatedVerifiedDist = estimatedStretches.sumOf { it.stretchDistance ?: 0.0 }
            val estimatedVerifiedEnergy = estimatedStretches.sumOf { it.stretchEnergy ?: 0.0 }

            val verifiedEfficiency = if (verifiedEnergy > 0) verifiedDistance / verifiedEnergy else null
            val provisionalEfficiency = if (distance > 0 && eligibleEnergy > 0) {
                distance / eligibleEnergy
            } else {
                null
            }
            val efficiencyUsed = verifiedEfficiency ?: provisionalEfficiency
            val efficiencyBasis = when {
                verifiedEfficiency != null -> "Verified"
                provisionalEfficiency != null -> "Provisional"
                else -> "Not enough data"
            }

            val simpleEnergyCostPerKm = if (distance > 0) energySpend / distance else null
            val verifiedEnergyCostPerKm = if (verifiedDistance > 0) verifiedEnergyCost / verifiedDistance else null
            val energyCostPerKmUsed = verifiedEnergyCostPerKm ?: simpleEnergyCostPerKm
            val energyCostBasis = when {
                verifiedEnergyCostPerKm != null -> "Verified"
                simpleEnergyCostPerKm != null -> "Provisional"
                else -> "Not enough data"
            }

            val batteryWearCostPerKm = allTime.batteryWearCostPerKm
            val batteryWearInPeriod = distance * batteryWearCostPerKm
            val runningCostPerKm = energyCostPerKmUsed?.plus(batteryWearCostPerKm)
            val totalRunningCost = energySpend + batteryWearInPeriod

            val pMaint = evaluatedMaintenance.filter {
                it.entry.vehicleId == v.id &&
                    it.entry.date >= periodBounds.start &&
                    it.entry.date <= periodBounds.end
            }
            val maintenanceSpend = pMaint.sumOf { it.entry.amount }
            val maintenanceCostPerKm = if (distance > 0) maintenanceSpend / distance else null
            val allInCostPerKm = if (runningCostPerKm != null && maintenanceCostPerKm != null) {
                runningCostPerKm + maintenanceCostPerKm
            } else {
                null
            }

            val effValues = verifiedStretches.mapNotNull { it.stretchEfficiency }
            val bestEfficiency = effValues.maxOrNull()
            val worstEfficiency = effValues.minOrNull()
            val latestStretchEff = effValues.lastOrNull()

            periodStatsByVehicle[v.id] = PeriodStats(
                vehicle = v,
                entriesCount = pLogs.size,
                distance = distance,
                trackingHours = pLogs.sumOf { it.trackingHours },
                energyAdded = energyAdded,
                measuredEnergy = measuredEnergy,
                estimatedEnergy = estimatedEnergy,
                energySpend = energySpend,
                avgPricePaid = if (energyAdded > 0) energySpend / energyAdded else null,
                verifiedDistance = verifiedDistance,
                verifiedEnergy = verifiedEnergy,
                verifiedEnergyCost = verifiedEnergyCost,
                verifiedEfficiency = verifiedEfficiency,
                avgConsumptionPer100Km = verifiedEfficiency?.let { if (it != 0.0) 100.0 / it else null },
                shareMeasuredVerifiedKm = if (verifiedDistance > 0) {
                    (measuredVerifiedDist / verifiedDistance) * 100
                } else {
                    0.0
                },
                measuredOnlyEfficiency = if (measuredVerifiedEnergy > 0) {
                    measuredVerifiedDist / measuredVerifiedEnergy
                } else {
                    null
                },
                estimatedOnlyEfficiency = if (estimatedVerifiedEnergy > 0) {
                    estimatedVerifiedDist / estimatedVerifiedEnergy
                } else {
                    null
                },
                bestEfficiency = bestEfficiency,
                worstEfficiency = worstEfficiency,
                latestStretchEff = latestStretchEff,
                provisionalEfficiency = provisionalEfficiency,
                efficiencyUsed = efficiencyUsed,
                efficiencyBasis = efficiencyBasis,
                simpleEnergyCostPerKm = simpleEnergyCostPerKm,
                verifiedEnergyCostPerKm = verifiedEnergyCostPerKm,
                energyCostPerKmUsed = energyCostPerKmUsed,
                energyCostBasis = energyCostBasis,
                batteryWearCostPerKm = batteryWearCostPerKm,
                batteryWearInPeriod = batteryWearInPeriod,
                runningCostPerKm = runningCostPerKm,
                totalRunningCost = totalRunningCost,
                maintenanceSpend = maintenanceSpend,
                maintenanceCostPerKm = maintenanceCostPerKm,
                allInCostPerKm = allInCostPerKm,
            )
        }

        // Calibration curve quality
        val calibrationChecks = mutableListOf<CalibrationCheck>()
        for (v in vehicles.filter { it.energySource == SOURCE_ELECTRICITY }) {
            val pts = state.calibration.filter { it.vehicleId == v.id }.sortedBy { it.voltage }
            for (i in pts.indices) {
                var check = "OK"
                if (i > 0 && pts[i].voltage == pts[i - 1].voltage) {
                    check = "ERROR: Duplicate voltage for vehicle"
                } else if (i > 0 && pts[i].percent <= pts[i - 1].percent) {
                    check = "CHECK: Battery % does not rise with voltage"
                }
                calibrationChecks.add(CalibrationCheck(pts[i], check))
            }
        }

        return EvalResult(
            evaluatedUsage = evaluatedUsage,
            evaluatedMaintenance = evaluatedMaintenance,
            vehicleAllTimeStats = vehicleAllTimeStats,
            periodStatsByVehicle = periodStatsByVehicle,
            nearestReminderByVehicle = nearestReminderByVehicle,
            periodBounds = periodBounds,
            calibrationChecks = calibrationChecks,
        )
    }

    // ---------------------------------------------------------------------
    // Small helpers. Formatting mirrors JS `toFixed` behaviour used in the
    // original validation messages only; all maths stays in full precision.
    // ---------------------------------------------------------------------

    private fun plausibleBounds(s: SettingsEntity, source: String?): Triple<Double, Double, String>? = when (source) {
        SOURCE_ELECTRICITY -> Triple(s.evPlausibleMin, s.evPlausibleMax, "kWh")
        SOURCE_PETROL -> Triple(s.petrolPlausibleMin, s.petrolPlausibleMax, "L")
        else -> null
    }

    private fun statusRank(status: String): Int = when (status) {
        "Overdue" -> 0
        "Due Soon" -> 1
        "OK" -> 2
        else -> 9
    }

    /** JavaScript Math.round(x * 100) / 100 for non-negative costs. */
    fun jsRound2(value: Double): Double = floor(value * 100.0 + 0.5) / 100.0

    private fun fmt1(v: Double) = "%.1f".format(java.util.Locale.US, v)
    private fun fmt2(v: Double) = "%.2f".format(java.util.Locale.US, v)
    private fun fmt0(v: Double) = "%.0f".format(java.util.Locale.US, v)
    private fun fmt(v: Double) = if (v == floor(v)) v.toLong().toString() else v.toString()

    /** Fleet-level dashboard aggregation for the selected vehicles only. */
    data class FleetTotals(
        val distance: Double,
        val electricitySpend: Double,
        val petrolSpend: Double,
        val batteryWear: Double,
        val runningCost: Double,
        val runningCostPerKm: Double?,
        val mostEconomical: PeriodStats?,
        val evSavings: Double?,
        val evDistance: Double,
        val petrolDistance: Double,
        val petrolCostPerKm: Double?,
        val evCostPerKm: Double?,
        val maintenanceSpend: Double,
        val maintenancePerKm: Double?,
    )

    fun fleetTotals(stats: List<PeriodStats>): FleetTotals {
        val distance = stats.sumOf { it.distance }
        val electricity = stats.filter { it.isElectric }.sumOf { it.energySpend }
        val petrol = stats.filter { !it.isElectric }.sumOf { it.energySpend }
        val wear = stats.sumOf { it.batteryWearInPeriod }
        val running = electricity + petrol + wear
        val maintenance = stats.sumOf { it.maintenanceSpend }

        val withBasis = stats.filter { it.runningCostPerKm != null && it.distance > 0 }
        val basisDist = withBasis.sumOf { it.distance }
        val avgCost = if (basisDist > 0) withBasis.sumOf { (it.runningCostPerKm ?: 0.0) * it.distance } / basisDist else null

        val mostEconomical = stats
            .filter { it.runningCostPerKm != null && it.distance > 0 }
            .minByOrNull { it.runningCostPerKm ?: Double.MAX_VALUE }

        val evStats = stats.filter { it.isElectric && it.runningCostPerKm != null && it.distance > 0 }
        val petStats = stats.filter { !it.isElectric && it.runningCostPerKm != null && it.distance > 0 }
        val evDist = evStats.sumOf { it.distance }
        val petDist = petStats.sumOf { it.distance }
        val evCost = if (evDist > 0) evStats.sumOf { (it.runningCostPerKm ?: 0.0) * it.distance } / evDist else null
        val petCost = if (petDist > 0) petStats.sumOf { (it.runningCostPerKm ?: 0.0) * it.distance } / petDist else null
        val savings = if (evCost != null && petCost != null && evDist > 0) (petCost - evCost) * evDist else null

        return FleetTotals(
            distance = distance,
            electricitySpend = electricity,
            petrolSpend = petrol,
            batteryWear = wear,
            runningCost = running,
            runningCostPerKm = avgCost,
            mostEconomical = mostEconomical,
            evSavings = savings,
            evDistance = evDist,
            petrolDistance = petDist,
            petrolCostPerKm = petCost,
            evCostPerKm = evCost,
            maintenanceSpend = maintenance,
            maintenancePerKm = if (distance > 0) maintenance / distance else null,
        )
    }

    data class MonthSlot(val ym: String, val label: String)

    /** 12-month window anchored at the configured chart start month. */
    fun monthWindow(startMonth: String, count: Int = 12): List<MonthSlot> {
        val parsed = runCatching { YearMonth.parse(startMonth) }.getOrElse { YearMonth.of(2026, 10) }
        return (0 until count).map { i ->
            val ym = parsed.plusMonths(i.toLong())
            MonthSlot(
                ym = "%04d-%02d".format(ym.year, ym.monthValue),
                label = "${monthAbbrev(ym.monthValue)} ${ym.year.toString().takeLast(2)}",
            )
        }
    }

    private fun monthAbbrev(m: Int): String =
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[m - 1]
}

package com.fleetpulse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fleetpulse.app.data.db.FILL_RIDE_ONLY
import com.fleetpulse.app.data.db.SOURCE_ELECTRICITY
import com.fleetpulse.app.ui.UiSnapshot
import com.fleetpulse.app.ui.components.ChartSeries
import com.fleetpulse.app.ui.components.DonutChart
import com.fleetpulse.app.ui.components.DonutSlice
import com.fleetpulse.app.ui.components.GroupedBarChart
import com.fleetpulse.app.ui.components.KpiCard
import com.fleetpulse.app.ui.components.LabelValueRow
import com.fleetpulse.app.ui.components.LineChart
import com.fleetpulse.app.ui.components.ScreenHeader
import com.fleetpulse.app.ui.components.SectionCard
import com.fleetpulse.app.ui.components.VehicleDot
import com.fleetpulse.app.ui.components.VehicleFilterDropdown
import com.fleetpulse.app.ui.components.formatINR
import com.fleetpulse.app.ui.components.formatNum
import com.fleetpulse.app.ui.components.parseColor
import com.fleetpulse.app.domain.CalcEngine
import com.fleetpulse.app.ui.theme.ElectricColor
import com.fleetpulse.app.ui.theme.FleetGreen
import com.fleetpulse.app.ui.theme.FleetViolet
import com.fleetpulse.app.ui.theme.PetrolColor

@Composable
fun AnalysisScreen(
    snapshot: UiSnapshot,
    onVehicleFilter: (String) -> Unit,
    onPeriodFilter: (String) -> Unit,
    onReportProblem: (String) -> Unit,
) {
    val state = snapshot.state
    val eval = snapshot.evaluation
    val filter = snapshot.filter
    var tab by remember { mutableStateOf("mileage") }

    val activeVehicles = state.vehicles.filter { !it.archived }
    val selectedVehicleId = filter.vehicleId.takeIf {
        it == "ALL" || activeVehicles.any { v -> v.id == it }
    } ?: "ALL"
    val vehicles = if (selectedVehicleId == "ALL") {
        activeVehicles
    } else {
        activeVehicles.filter { it.id == selectedVehicleId }
    }
    val stats = vehicles.mapNotNull { eval.periodStatsByVehicle[it.id] }
    val totals = CalcEngine.fleetTotals(stats)
    val months = CalcEngine.monthWindow(state.settings.chartStartMonth)
    val logs = eval.evaluatedUsage.filter {
        it.status != "ERROR" &&
            it.entry.date >= eval.periodBounds.start &&
            it.entry.date <= eval.periodBounds.end &&
            (selectedVehicleId == "ALL" || it.entry.vehicleId == selectedVehicleId)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Analysis",
                subtitle = "Verified figures first, provisional clearly labelled",
                actions = {
                    IconButton(onClick = { onReportProblem("Analysis · $tab") }) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = "Report a problem with Analysis",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
        }
        item {
            VehicleFilterDropdown(
                vehicles = activeVehicles,
                selectedId = selectedVehicleId,
                onSelect = onVehicleFilter,
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    listOf(
                        "all" to "All time",
                        "current_month" to "This month",
                        "last_30" to "30 days",
                        "last_90" to "90 days",
                        "current_fy" to "This FY",
                        "prev_fy" to "Prev FY",
                    ),
                ) { (key, label) ->
                    FilterChip(
                        selected = filter.periodKey == key,
                        onClick = { onPeriodFilter(key) },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    listOf(
                        "mileage" to "Mileage",
                        "costs" to "Costs",
                        "energy" to "Energy & battery",
                    ),
                ) { (key, label) ->
                    FilterChip(
                        selected = tab == key,
                        onClick = { tab = key },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
        }
        item {
            Text(
                "Period: ${eval.periodBounds.label} · ${vehicles.size} vehicles",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (vehicles.isEmpty()) {
            item {
                com.fleetpulse.app.ui.components.EmptyState(
                    icon = Icons.Default.BarChart,
                    title = "No data in this filter",
                    message = "Add usage entries or choose another period.",
                )
            }
        }

        if (tab == "mileage") {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        "Verified distance",
                        "${formatNum(totals.distance, 0)} km",
                        "Period distance",
                        Modifier.weight(1f),
                    )
                    KpiCard(
                        "Fleet avg",
                        totals.runningCostPerKm?.let { formatINR(it, 2) + "/km" } ?: "—",
                        "Running cost incl. battery wear",
                        Modifier.weight(1f),
                    )
                }
            }

            val petrolVehicles = vehicles.filter { it.energySource != SOURCE_ELECTRICITY }
            if (petrolVehicles.isNotEmpty()) {
                item {
                    SectionCard(
                        title = "Fuel mileage (km/L)",
                        subtitle = "Measured from eligible full-to-full refills; never mixed with EV efficiency",
                    ) {
                        petrolVehicles.forEach { v ->
                            val p = eval.periodStatsByVehicle[v.id]
                            val a = eval.vehicleAllTimeStats[v.id]
                            val eligibleRefills = eval.evaluatedUsage.count {
                                it.entry.vehicleId == v.id &&
                                    it.entry.includeInMileage &&
                                    it.entry.fillType != FILL_RIDE_ONLY &&
                                    it.status != "ERROR" &&
                                    it.entry.date >= eval.periodBounds.start &&
                                    it.entry.date <= eval.periodBounds.end
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                VehicleDot(v.color, 12.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(v.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            LabelValueRow(
                                "Latest measured",
                                a?.latestValidMileage?.let { "${formatNum(it, 1)} km/L" }
                                    ?: "No eligible full-to-full stretch",
                            )
                            LabelValueRow(
                                "All-time average",
                                a?.allTimeAvgKmPerCharge?.let { "${formatNum(it, 1)} km/L" }
                                    ?: "Not enough eligible stretches",
                            )
                            LabelValueRow(
                                "Period verified",
                                p?.verifiedEfficiency?.let { "${formatNum(it, 2)} km/L" }
                                    ?: "No verified interval in period",
                            )
                            if (p?.efficiencyBasis == "Provisional" && p.efficiencyUsed != null) {
                                LabelValueRow(
                                    "Provisional (not full-to-full)",
                                    "${formatNum(p.efficiencyUsed, 2)} km/L",
                                    Color(0xFFB45309),
                                )
                            }
                            LabelValueRow(
                                "Eligible refills in period",
                                eligibleRefills.toString(),
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            "Checked refills participate in full-to-full intervals. Unchecked refills stay in history and " +
                                "expenses but contribute no fuel to the mileage denominator.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(vehicles, key = { it.id }) { v ->
                val p = eval.periodStatsByVehicle[v.id]
                val a = eval.vehicleAllTimeStats[v.id]
                val isEv = v.energySource == SOURCE_ELECTRICITY
                SectionCard(
                    title = "${v.name} · ${if (isEv) "electric" else "petrol"}",
                    subtitle = v.type,
                    trailing = { VehicleDot(v.color, 14.dp) },
                ) {
                    LabelValueRow(
                        "Verified efficiency",
                        p?.verifiedEfficiency?.let {
                            "${formatNum(it, 2)} ${if (isEv) "km/kWh" else "km/L"}"
                        } ?: "No verified full-to-full stretch",
                    )
                    LabelValueRow(
                        "Efficiency used",
                        (p?.efficiencyUsed?.let { formatNum(it, 2) } ?: "—") +
                            " ${if (isEv) "km/kWh" else "km/L"} (${p?.efficiencyBasis ?: "—"})",
                        if (p?.efficiencyBasis == "Provisional") Color(0xFFB45309) else MaterialTheme.colorScheme.onSurface,
                    )
                    LabelValueRow(
                        if (isEv) "Latest verified km per charge" else "Latest verified km per litre",
                        a?.latestValidMileage?.let { formatNum(it, 1) } ?: "No verified stretch yet",
                    )
                    LabelValueRow(
                        "All-time average",
                        a?.allTimeAvgKmPerCharge?.let {
                            "${formatNum(it, 1)} ${if (isEv) "km/charge" else "km/L"}"
                        } ?: "Not enough verified stretches",
                    )
                    LabelValueRow(
                        "Latest / last 5 average",
                        "${a?.latestValidMileage?.let { formatNum(it, 1) } ?: "—"} / " +
                            (a?.avgLast5Mileage?.let { formatNum(it, 1) } ?: "—"),
                    )
                    LabelValueRow(
                        "Best / worst verified",
                        "${p?.bestEfficiency?.let { formatNum(it, 2) } ?: "—"} / " +
                            (p?.worstEfficiency?.let { formatNum(it, 2) } ?: "—"),
                    )
                    LabelValueRow(
                        "Verified energy",
                        "${formatNum(p?.verifiedEnergy, 2)} ${if (isEv) "kWh" else "L"} over " +
                            "${formatNum(p?.verifiedDistance, 0)} km",
                    )
                    LabelValueRow(
                        "Measured share of verified km",
                        "${formatNum(p?.shareMeasuredVerifiedKm, 1)}% " +
                            if (isEv) "metered · remainder voltage-estimated" else "metered · remainder estimated",
                    )
                    LabelValueRow("Valid stretches in period", "${eval.evaluatedUsage.count { it.entry.vehicleId == v.id && it.isValidForAverage && it.entry.date >= eval.periodBounds.start && it.entry.date <= eval.periodBounds.end }}")
                }
            }

            val electricVehicles = vehicles.filter { it.energySource == SOURCE_ELECTRICITY }
            val petrolVehiclesForChart = vehicles.filter { it.energySource != SOURCE_ELECTRICITY }
            if (electricVehicles.isNotEmpty()) {
                item {
                    SectionCard(title = "EV km per charge trend", subtitle = "12-month window · eligible verified stretches only") {
                        LineChart(
                            labels = months.map { it.label },
                            series = electricVehicles.map { v ->
                                ChartSeries(
                                    v.name,
                                    parseColor(v.color),
                                    months.map { m ->
                                        val values = logs.filter {
                                            it.entry.vehicleId == v.id &&
                                                it.entry.date.startsWith(m.ym) &&
                                                it.isValidForAverage
                                        }.mapNotNull { it.kmPerCharge }
                                        if (values.isEmpty()) null else values.average()
                                    },
                                )
                            },
                        )
                    }
                }
            }
            if (petrolVehiclesForChart.isNotEmpty()) {
                item {
                    SectionCard(title = "Fuel mileage trend (km/L)", subtitle = "12-month window · eligible verified stretches only") {
                        LineChart(
                            labels = months.map { it.label },
                            series = petrolVehiclesForChart.map { v ->
                                ChartSeries(
                                    v.name,
                                    parseColor(v.color),
                                    months.map { m ->
                                        val stretches = logs.filter {
                                            it.entry.vehicleId == v.id &&
                                                it.entry.date.startsWith(m.ym) &&
                                                it.isValidForAverage
                                        }
                                        val d = stretches.sumOf { it.stretchDistance ?: 0.0 }
                                        val e = stretches.sumOf { it.stretchEnergy ?: 0.0 }
                                        if (d > 0 && e > 0) d / e else null
                                    },
                                )
                            },
                        )
                    }
                }
            }

            item {
                SectionCard(title = "How efficiency is chosen") {
                    Text(
                        "1. Only full-to-full stretches are used for verified mileage — never a partial charge or refill on its own.\n" +
                            "2. Verified efficiency is total verified distance ÷ total verified energy across the period (a weighted result, not an average of averages).\n" +
                            "3. If no verified stretch exists, a provisional distance ÷ energy figure is shown and labelled Provisional.\n" +
                            "4. Entries unchecked for mileage estimation contribute no fuel/energy to the denominator and cannot start or end an interval. Their distance, history and costs remain.\n" +
                            "5. Implausible stretches (outside the configured plausible-efficiency band) are flagged CHECK and excluded from averages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (tab == "costs") {
            item {
                SectionCard(title = "Cost per kilometre", subtitle = "Energy and battery wear are separated; maintenance is reported apart") {
                    GroupedBarChart(
                        labels = vehicles.map { it.name },
                        series = listOf(
                            ChartSeries(
                                "Energy ₹/km",
                                ElectricColor,
                                stats.map { it.energyCostPerKmUsed ?: 0.0 },
                            ),
                            ChartSeries(
                                "Battery wear ₹/km",
                                FleetViolet,
                                stats.map { it.batteryWearCostPerKm },
                            ),
                            ChartSeries(
                                "Maintenance ₹/km",
                                PetrolColor,
                                stats.map { it.maintenanceCostPerKm ?: 0.0 },
                            ),
                        ),
                        stacked = true,
                    )
                }
            }
            items(vehicles, key = { "cost-" + it.id }) { v ->
                val p = eval.periodStatsByVehicle[v.id]
                SectionCard(title = v.name, subtitle = "Period: ${eval.periodBounds.label}") {
                    LabelValueRow("Energy spend", formatINR(p?.energySpend, 2))
                    LabelValueRow(
                        "Energy cost basis",
                        "${p?.energyCostBasis ?: "—"} · ${formatINR(p?.energyCostPerKmUsed, 2)}/km",
                    )
                    LabelValueRow("Battery wear (estimated)", formatINR(p?.batteryWearInPeriod, 2))
                    LabelValueRow("Battery wear per km", formatINR(p?.batteryWearCostPerKm, 2) + "/km")
                    LabelValueRow(
                        "Running cost total",
                        "${formatINR(p?.totalRunningCost, 2)} · ${formatINR(p?.runningCostPerKm, 2)}/km",
                    )
                    LabelValueRow("Maintenance (separate)", formatINR(p?.maintenanceSpend, 2))
                    LabelValueRow("All-in cost per km", p?.allInCostPerKm?.let { formatINR(it, 2) + "/km" } ?: "—")
                    Text(
                        "Battery wear is not money you paid this month. It is an allowance for the replacement cost spread " +
                            "over estimated battery life, so running cost reflects true wear.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                SectionCard(title = "Running cost split", subtitle = "Period · excludes maintenance") {
                    DonutChart(
                        slices = listOf(
                            DonutSlice("Electricity", totals.electricitySpend, ElectricColor),
                            DonutSlice("Fuel", totals.petrolSpend, PetrolColor),
                            DonutSlice("Battery wear (est.)", totals.batteryWear, FleetViolet),
                        ),
                        centerLabel = "Running cost",
                        centerValue = formatINR(totals.runningCost, 0),
                    )
                }
            }
            item {
                SectionCard(title = "EV vs petrol savings", subtitle = "Distance-weighted, actual mixed prices") {
                    if (totals.evSavings == null) {
                        Text(
                            "Savings need both electric and petrol vehicles with a cost basis in this period. " +
                                "No estimate is invented when data is missing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LabelValueRow("Electric cost", formatINR(totals.evCostPerKm, 2) + "/km")
                        LabelValueRow("Fuel cost", formatINR(totals.petrolCostPerKm, 2) + "/km")
                        LabelValueRow("Electric km", "${formatNum(totals.evDistance, 0)} km")
                        LabelValueRow(
                            "Estimated savings",
                            formatINR(totals.evSavings, 0),
                            if (totals.evSavings >= 0) FleetGreen else MaterialTheme.colorScheme.error,
                        )
                        Text(
                            "Savings = (petrol cost/km − electric cost/km) × electric distance. " +
                                "Costs include battery wear for EVs and actual tariff-effective energy prices for both.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (tab == "energy") {
            items(vehicles.filter { it.energySource == SOURCE_ELECTRICITY }, key = { "battery-" + it.id }) { v ->
                val p = eval.periodStatsByVehicle[v.id]
                val a = eval.vehicleAllTimeStats[v.id]
                SectionCard(title = "${v.name} battery wear estimator", subtitle = "Transparent breakdown") {
                    LabelValueRow("Usable capacity", "${formatNum(v.batteryCapacity, 2)} kWh")
                    LabelValueRow("Replacement rate", formatINR(v.batteryRate, 0) + " per kWh")
                    LabelValueRow("Battery price", formatINR(a?.batteryPrice, 0))
                    LabelValueRow("Expected cycles", "${v.chargeCycles ?: 0}")
                    LabelValueRow("Expected life", "${formatNum(v.batteryLifeYears, 0)} years")
                    LabelValueRow("Plan distance", "${formatNum(v.dailyKmPlan, 0)} km/day")
                    LabelValueRow(
                        "Km per charge used",
                        "${formatNum(a?.kmPerChargeUsed, 1)} (${a?.kmPerChargeSource ?: "—"})",
                    )
                    LabelValueRow("Cycles capacity", "${formatNum(a?.maxRunningCapacityKm, 0)} km")
                    LabelValueRow("Age capacity", "${formatNum(a?.distanceOverBatteryLifeKm, 0)} km")
                    LabelValueRow("Distance considered", "${formatNum(a?.distanceConsideredKm, 0)} km (smaller of the two)")
                    LabelValueRow(
                        "Wear cost",
                        formatINR(a?.batteryWearCostPerKm, 2) + "/km",
                        FleetViolet,
                    )
                    LabelValueRow("Charging efficiency", "${formatNum(state.settings.chargerEfficiency, 0)}%")
                    Text(
                        "A full charge is treated as 100%. Voltage → % conversion uses this vehicle's calibration points " +
                            "and linear interpolation; readings outside the calibrated range are flagged rather than trusted blindly.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SectionCard(title = "Energy accounting", subtitle = "Measured vs voltage-estimated") {
                    GroupedBarChart(
                        labels = vehicles.map { it.name },
                        series = listOf(
                            ChartSeries("Measured", FleetGreen, stats.map { it.measuredEnergy }),
                            ChartSeries("Estimated from voltage", Color(0xFFF59E0B), stats.map { it.estimatedEnergy }),
                        ),
                        stacked = true,
                    )
                    stats.forEach { p ->
                        LabelValueRow(
                            "${p.vehicle.name} average paid price",
                            formatINR(p.avgPricePaid, 2) + " per ${if (p.isElectric) "kWh" else "L"}",
                        )
                    }
                    Text(
                        "Estimated energy = (battery % after − % before) ÷ 100 × capacity ÷ charger efficiency. " +
                            "Metered kWh is always preferred and is labelled Measured.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SectionCard(title = "Period energy totals", subtitle = eval.periodBounds.label) {
                    LabelValueRow("Electricity spend", formatINR(totals.electricitySpend, 2))
                    LabelValueRow("Fuel spend", formatINR(totals.petrolSpend, 2))
                    LabelValueRow("Battery wear allowance", formatINR(totals.batteryWear, 2))
                    LabelValueRow("Running cost", formatINR(totals.runningCost, 2))
                    LabelValueRow("Maintenance (separate)", formatINR(totals.maintenanceSpend, 2))
                }
            }

            item {
                SectionCard(title = "Tariff history", subtitle = "Applied by transaction date, not today's price") {
                    state.priceHistory.sortedByDescending { it.effectiveFrom }.forEach { price ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                price.energySource,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(82.dp),
                            )
                            Text(
                                "from ${price.effectiveFrom}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${formatINR(price.price, 2)}/${if (price.energySource == "Electricity") "kWh" else "L"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if (price.isSample) {
                            Text("[SAMPLE]", style = MaterialTheme.typography.labelSmall, color = PetrolColor)
                        }
                    }
                    if (state.priceHistory.isEmpty()) {
                        Text(
                            "No tariffs configured. Add them in Settings so costs can be calculated.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB45309),
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.width(1.dp)) }
    }
}

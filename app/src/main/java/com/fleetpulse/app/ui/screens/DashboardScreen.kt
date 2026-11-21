package com.fleetpulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fleetpulse.app.data.db.SOURCE_ELECTRICITY
import com.fleetpulse.app.data.db.UsageEntry
import com.fleetpulse.app.domain.CalcEngine
import com.fleetpulse.app.domain.EvaluatedUsage
import com.fleetpulse.app.domain.PeriodStats
import com.fleetpulse.app.ui.DashboardFilter
import com.fleetpulse.app.ui.UiSnapshot
import com.fleetpulse.app.ui.components.ConfirmDialog
import com.fleetpulse.app.ui.components.DonutChart
import com.fleetpulse.app.ui.components.DonutSlice
import com.fleetpulse.app.ui.components.GroupedBarChart
import com.fleetpulse.app.ui.components.KpiCard
import com.fleetpulse.app.ui.components.LabelValueRow
import com.fleetpulse.app.ui.components.LineChart
import com.fleetpulse.app.ui.components.SectionCard
import com.fleetpulse.app.ui.components.StatusBadge
import com.fleetpulse.app.ui.components.TrendChart
import com.fleetpulse.app.ui.components.VehicleDot
import com.fleetpulse.app.ui.components.VehicleFilterDropdown
import com.fleetpulse.app.ui.components.formatDate
import com.fleetpulse.app.ui.components.formatINR
import com.fleetpulse.app.ui.components.formatNum
import com.fleetpulse.app.ui.components.parseColor
import com.fleetpulse.app.ui.components.ChartSeries
import com.fleetpulse.app.ui.theme.ElectricColor
import com.fleetpulse.app.ui.theme.FleetAmber
import com.fleetpulse.app.ui.theme.FleetGreen
import com.fleetpulse.app.ui.theme.FleetRed
import com.fleetpulse.app.ui.theme.FleetViolet
import com.fleetpulse.app.ui.theme.PetrolColor

@Composable
fun DashboardScreen(
    snapshot: UiSnapshot,
    onVehicleFilter: (String) -> Unit,
    onPeriodFilter: (String) -> Unit,
    onCustomRange: (String, String) -> Unit,
    onAddEntry: (String, String?) -> Unit,
    onEditEntry: (String) -> Unit,
    onOpenUsage: () -> Unit,
    onOpenData: () -> Unit,
    onOpenMaintenance: () -> Unit,
    onRemoveSamples: () -> Unit,
    onReportProblem: () -> Unit,
) {
    val state = snapshot.state
    val eval = snapshot.evaluation
    val filter = snapshot.filter
    val activeVehicles = state.vehicles.filter { !it.archived }
    if (activeVehicles.isEmpty()) {
        com.fleetpulse.app.ui.components.EmptyState(
            icon = Icons.Default.Add,
            title = "No vehicles yet",
            message = "Add your first vehicle from the Vehicles tab to start tracking running costs.",
        )
        return
    }

    val activeIds = activeVehicles.map { it.id }.toSet()
    // A vehicle can be archived while it is the active filter; fall back to the
    // whole active fleet instead of showing an archived vehicle on the dashboard.
    val selectedVehicleId = filter.vehicleId.takeIf { it == "ALL" || it in activeIds } ?: "ALL"
    val targetVehicles = if (selectedVehicleId == "ALL") {
        activeVehicles
    } else {
        activeVehicles.filter { it.id == selectedVehicleId }
    }
    val targetStats = targetVehicles.mapNotNull { eval.periodStatsByVehicle[it.id] }
    val totals = CalcEngine.fleetTotals(targetStats)

    val sampleUsage = state.usageEntries.count { it.isSample }
    val sampleMaint = state.maintenanceEntries.count { it.isSample }
    val sampleCal = state.calibration.count { it.isSample }
    val sampleMode = sampleUsage + sampleMaint + sampleCal > 0

    val months = CalcEngine.monthWindow(state.settings.chartStartMonth)
    // Monthly trend charts always show the 12-month window, independent of the
    // selected KPI period, otherwise every month except the current one is zero.
    val chartLogs = eval.evaluatedUsage.filter {
        it.status != "ERROR" && (selectedVehicleId == "ALL" || it.entry.vehicleId == selectedVehicleId)
    }

    var showCustomDialog by remember { mutableStateOf(false) }
    var showRemoveSamples by remember { mutableStateOf(false) }
    var showIssues by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("FleetPulse", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Fleet & EV running cost telemetry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onReportProblem) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = "Report a problem with Dashboard",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    AssistChip(
                        onClick = { showIssues = true },
                        label = {
                            Text(
                                if (eval.errorCount + eval.checkCount == 0) {
                                    "All OK"
                                } else {
                                    "${eval.errorCount + eval.checkCount} need attention"
                                },
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (eval.errorCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (eval.errorCount > 0) FleetRed else FleetGreen,
                                modifier = Modifier.width(16.dp),
                            )
                        },
                    )
                }
                if (sampleMode) {
                    Spacer(Modifier.height(10.dp))
                    SectionCard(
                        title = "Sample mode",
                        subtitle = "$sampleUsage sample usage · $sampleMaint sample service · $sampleCal sample calibration points",
                        trailing = {
                            TextButton(onClick = { showRemoveSamples = true }) { Text("Remove") }
                        },
                    ) {
                        Text(
                            "Demo records are clearly marked [SAMPLE] and are never treated as your own data. " +
                                "Removing them preserves genuine records and configured vehicles.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VehicleFilterDropdown(
                    vehicles = activeVehicles,
                    selectedId = selectedVehicleId,
                    onSelect = onVehicleFilter,
                )
                val periods = listOf(
                    "all" to "All time",
                    "current_month" to "This month",
                    "last_30" to "30 days",
                    "last_90" to "90 days",
                    "current_fy" to "This FY",
                    "prev_fy" to "Prev FY",
                    "custom" to "Custom",
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(periods) { (key, label) ->
                        FilterChip(
                            selected = filter.periodKey == key,
                            onClick = {
                                if (key == "custom") showCustomDialog = true else onPeriodFilter(key)
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
                Text(
                    "Showing ${eval.periodBounds.label}" + if (selectedVehicleId != "ALL") {
                        " · ${targetVehicles.firstOrNull()?.name ?: ""}"
                    } else {
                        " · ${targetVehicles.size} vehicles"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // KPI grid (2 per row)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val kpis = dashboardKpis(totals, targetVehicles.size)
                kpis.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { kpi ->
                            KpiCard(
                                label = kpi.label,
                                value = kpi.value,
                                caption = kpi.caption,
                                badge = kpi.badge,
                                valueColor = kpi.color ?: MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Spotlight
        item {
            val spotlight = targetVehicles.firstOrNull()
            if (spotlight != null) {
                SpotlightCard(
                    vehicle = spotlight,
                    stats = eval.periodStatsByVehicle[spotlight.id],
                    allTime = eval.vehicleAllTimeStats[spotlight.id],
                    reminder = eval.nearestReminderByVehicle[spotlight.id],
                    onAdd = { onAddEntry("charge_fuel", spotlight.id) },
                )
            }
        }

        item { SectionHeader("Mileage scoreboard", "Latest verified full-to-full results") }
        items(targetVehicles) { v ->
            val st = eval.vehicleAllTimeStats[v.id]
            val isEv = v.energySource == SOURCE_ELECTRICITY
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VehicleDot(v.color, 12.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(v.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(
                        if (isEv) "km/charge" else "km/L",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(Modifier.fillMaxWidth()) {
                    MetricCell("Latest", st?.latestValidMileage?.let { formatNum(it, 1) } ?: "—", Modifier.weight(1f))
                    MetricCell("All-time avg", st?.allTimeAvgKmPerCharge?.let { formatNum(it, 1) } ?: "—", Modifier.weight(1f))
                    MetricCell("Results", st?.validStretchesCount?.toString() ?: "0", Modifier.weight(1f))
                }
            }
        }

        // Charts
        item {
            SectionCard(
                title = "Total distance by vehicle",
                subtitle = "Filtered period",
            ) {
                GroupedBarChart(
                    labels = targetVehicles.map { it.name },
                    series = listOf(
                        ChartSeries(
                            "Distance",
                            Color(0xFF2563EB),
                            targetStats.map { it.distance },
                        ),
                    ),
                    showLegend = false,
                )
            }
        }

        item {
            SectionCard(title = "Monthly distance travelled", subtitle = "12-month window") {
                GroupedBarChart(
                    labels = months.map { it.label },
                    series = targetVehicles.map { v ->
                        ChartSeries(
                            v.name,
                            parseColor(v.color),
                            months.map { m ->
                                chartLogs.filter { it.entry.vehicleId == v.id && it.entry.date.startsWith(m.ym) }
                                    .sumOf { it.distance }
                            },
                        )
                    },
                )
            }
        }

        item {
            SectionCard(title = "Running cost per km", subtitle = "Energy + estimated battery wear") {
                GroupedBarChart(
                    labels = targetVehicles.map { it.name },
                    series = listOf(
                        ChartSeries(
                            "Energy ₹/km",
                            Color(0xFF2563EB),
                            targetStats.map { it.energyCostPerKmUsed ?: 0.0 },
                        ),
                        ChartSeries(
                            "Battery wear ₹/km",
                            FleetViolet,
                            targetStats.map { it.batteryWearCostPerKm },
                        ),
                    ),
                    stacked = true,
                )
            }
        }

        item {
            SectionCard(title = "Monthly running cost", subtitle = "Energy spend + battery wear") {
                GroupedBarChart(
                    labels = months.map { it.label },
                    series = targetVehicles.map { v ->
                        val wear = eval.periodStatsByVehicle[v.id]?.batteryWearCostPerKm ?: 0.0
                        ChartSeries(
                            v.name,
                            parseColor(v.color),
                            months.map { m ->
                                val logs = chartLogs.filter { it.entry.vehicleId == v.id && it.entry.date.startsWith(m.ym) }
                                logs.sumOf { it.totalCost } + logs.sumOf { it.distance } * wear
                            },
                        )
                    },
                    valuePrefix = "₹",
                )
            }
        }

        val evVehicles = targetVehicles.filter { it.energySource == SOURCE_ELECTRICITY }
        if (evVehicles.isNotEmpty()) {
            item {
                SectionCard(title = "EV km per full charge", subtitle = "Monthly average of verified charges") {
                    LineChart(
                        labels = months.map { it.label },
                        series = evVehicles.map { v ->
                            ChartSeries(
                                v.name,
                                parseColor(v.color),
                                months.map { m ->
                                    val stretches = chartLogs.filter {
                                        it.entry.vehicleId == v.id && it.entry.date.startsWith(m.ym) && it.isValidForAverage
                                    }.mapNotNull { it.kmPerCharge }
                                    if (stretches.isEmpty()) null else stretches.average()
                                },
                            )
                        },
                    )
                }
            }
            item {
                SectionCard(title = "EV efficiency", subtitle = "Verified full-to-full stretches: distance ÷ energy") {
                    LineChart(
                        labels = months.map { it.label },
                        series = evVehicles.map { v ->
                            ChartSeries(
                                v.name,
                                parseColor(v.color),
                                months.map { m ->
                                    val stretches = chartLogs.filter {
                                        it.entry.vehicleId == v.id && it.entry.date.startsWith(m.ym) && it.isValidForAverage
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
            SectionCard(title = "Running cost split", subtitle = "Filtered period · excludes maintenance") {
                DonutChart(
                    slices = listOf(
                        DonutSlice("Electricity", totals.electricitySpend, ElectricColor),
                        DonutSlice("Fuel", totals.petrolSpend, PetrolColor),
                        DonutSlice("Battery wear (est.)", totals.batteryWear, FleetViolet),
                    ),
                    centerLabel = "Total",
                    centerValue = formatINR(totals.runningCost, 0),
                )
            }
        }

        val petrolVehicles = targetVehicles.filter { it.energySource != SOURCE_ELECTRICITY }
        if (petrolVehicles.isNotEmpty()) {
            item {
                SectionCard(title = "Fuel mileage", subtitle = "Verified km/L per month") {
                    LineChart(
                        labels = months.map { it.label },
                        series = petrolVehicles.map { v ->
                            ChartSeries(
                                v.name,
                                parseColor(v.color),
                                months.map { m ->
                                    val stretches = chartLogs.filter {
                                        it.entry.vehicleId == v.id && it.entry.date.startsWith(m.ym) && it.isValidForAverage
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
            val selected = targetVehicles.singleOrNull()
            SectionCard(
                title = if (selected != null) "Vehicle monthly trend" else "Fleet monthly trend",
                subtitle = if (selected != null) {
                    "${selected.name} · distance (bars) and running cost (line)"
                } else {
                    "All vehicles in filter · distance (bars) and running cost (line)"
                },
            ) {
                val distanceValues = months.map { m ->
                    chartLogs.filter { it.entry.date.startsWith(m.ym) }.sumOf { it.distance }
                }
                val costValues = months.map { m ->
                    val logs = chartLogs.filter { it.entry.date.startsWith(m.ym) }
                    logs.sumOf { it.totalCost } + logs.sumOf { log ->
                        val wear = eval.periodStatsByVehicle[log.entry.vehicleId]?.batteryWearCostPerKm ?: 0.0
                        log.distance * wear
                    }
                }
                TrendChart(
                    labels = months.map { it.label },
                    distance = ChartSeries(
                        "Distance km",
                        selected?.let { parseColor(it.color) } ?: Color(0xFF2563EB),
                        distanceValues,
                    ),
                    cost = ChartSeries(
                        "Cost ₹",
                        FleetViolet,
                        costValues,
                    ),
                )
            }
        }

        item {
            SectionCard(
                title = "Service reminders",
                subtitle = "Nearest pending reminder per vehicle",
                trailing = {
                    TextButton(onClick = onOpenMaintenance) { Text("Open") }
                },
            ) {
                targetVehicles.forEach { v ->
                    val rem = eval.nearestReminderByVehicle[v.id]
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VehicleDot(v.color)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(v.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (rem == null) {
                                    "No pending service due"
                                } else {
                                    buildString {
                                        append(rem.entry.category)
                                        rem.entry.nextDueDate?.let { append(" · due ${formatDate(it)}") }
                                        rem.entry.nextDueKm?.let { append(" · ${formatNum(it, 0)} km") }
                                        rem.daysLeft?.let { append(" (${it}d)") }
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        StatusBadge(rem?.status ?: "OK")
                    }
                }
                if (totals.maintenanceSpend > 0) {
                    LabelValueRow("Maintenance in period (separate)", formatINR(totals.maintenanceSpend, 0))
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomRangeDialog(
            initialFrom = filter.customFrom,
            initialTo = filter.customTo,
            onDismiss = { showCustomDialog = false },
            onApply = { from, to ->
                onCustomRange(from, to)
                showCustomDialog = false
            },
        )
    }
    if (showRemoveSamples) {
        ConfirmDialog(
            title = "Remove sample records?",
            message = "This deletes only records marked [SAMPLE] (usage, service, calibration). " +
                "Your own entries and vehicles are preserved. Sample tariffs are kept while real entries depend on them.",
            confirmLabel = "Remove samples",
            destructive = true,
            onConfirm = onRemoveSamples,
            onDismiss = { showRemoveSamples = false },
        )
    }
    if (showIssues) {
        AlertDialog(
            onDismissRequest = { showIssues = false },
            title = { Text("Data quality") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${eval.errorCount} ERROR · ${eval.checkCount} CHECK · ${eval.evaluatedUsage.count { it.status == "OK" }} OK")
                    Text(
                        "Tap a flagged entry to open and correct it. Records are never silently discarded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val flagged = eval.evaluatedUsage.filter { it.status != "OK" }.take(6)
                    flagged.forEach { e ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StatusBadge(e.status)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${e.entry.date} · ${e.vehicleName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    e.checkMessage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                )
                            }
                            TextButton(onClick = {
                                showIssues = false
                                onEditEntry(e.entry.id)
                            }) { Text("Fix") }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showIssues = false }) { Text("Close") } },
        )
    }
}

private data class Kpi(
    val label: String,
    val value: String,
    val caption: String?,
    val badge: String?,
    val color: Color?,
)

private fun dashboardKpis(totals: CalcEngine.FleetTotals, vehicleCount: Int): List<Kpi> = listOf(
    Kpi("Total distance", "${formatNum(totals.distance, 0)} km", "$vehicleCount vehicles in filter", null, null),
    Kpi(
        "Total running cost", formatINR(totals.runningCost, 0),
        "Energy + battery wear (est.)", "Cash + Wear", Color(0xFF2563EB),
    ),
    Kpi("Electricity expense", formatINR(totals.electricitySpend, 0), "Actual cash spent", null, Color(0xFF059669)),
    Kpi("Fuel expense", formatINR(totals.petrolSpend, 0), "Actual cash spent", null, PetrolColor),
    Kpi("Avg running cost", totals.runningCostPerKm?.let { formatINR(it, 2) + " /km" } ?: "—", "Distance-weighted", null, null),
    Kpi(
        "Most economical",
        totals.mostEconomical?.vehicle?.name ?: "—",
        totals.mostEconomical?.let { formatINR(it.runningCostPerKm, 2) + " per km incl. battery" } ?: "Log usage to compare",
        null,
        totals.mostEconomical?.let { parseColor(it.vehicle.color) },
    ),
    Kpi(
        "EV vs petrol savings", totals.evSavings?.let { formatINR(it, 0) } ?: "—",
        if (totals.evSavings != null) "Across ${formatNum(totals.evDistance, 0)} electric km" else "Needs electric and petrol data",
        null,
        if ((totals.evSavings ?: 0.0) >= 0) Color(0xFF059669) else FleetRed,
    ),
    Kpi(
        "Maintenance", formatINR(totals.maintenanceSpend, 0),
        totals.maintenancePerKm?.let { formatINR(it, 2) + " per km · not in running cost" } ?: "Not mixed into running cost",
        "Excluded", null,
    ),
)

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MetricCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SpotlightCard(
    vehicle: com.fleetpulse.app.data.db.Vehicle,
    stats: PeriodStats?,
    allTime: com.fleetpulse.app.domain.VehicleAllTimeStats?,
    reminder: com.fleetpulse.app.domain.EvaluatedMaintenance?,
    onAdd: () -> Unit,
) {
    val isEv = vehicle.energySource == SOURCE_ELECTRICITY
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, parseColor(vehicle.color).copy(alpha = 0.45f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .width(5.dp)
                        .height(38.dp)
                        .background(parseColor(vehicle.color), androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            vehicle.id,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(vehicle.name, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "${vehicle.type} · ${vehicle.regNo.ifBlank { "No Reg #" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.width(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Log", style = MaterialTheme.typography.labelMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    if (vehicle.isOperatingHoursBasis) {
                        Text("Operating hours in period", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${formatNum(stats?.trackingHours ?: 0.0, 1)} h",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            stats?.energySpend?.let { spend -> "Cost per hour: ${formatINR(spend / maxOf(1.0, stats?.trackingHours ?: 0.0), 2)}" } ?: "No cost basis yet",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text("Distance in period", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${formatNum(stats?.distance ?: 0.0, 0)} km",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            "Odo ${formatNum(allTime?.currentOdometer, 0)} km",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("Running cost", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatINR(stats?.totalRunningCost ?: 0.0, 0),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        stats?.runningCostPerKm?.let { formatINR(it, 2) + "/km" } ?: "No cost basis yet",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isEv) "Latest valid km/charge" else "Latest verified km/L",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        allTime?.latestValidMileage?.let { formatNum(it, 1) } ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        "All-time avg: ${allTime?.allTimeAvgKmPerCharge?.let { formatNum(it, 1) } ?: "—"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Next service", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        reminder?.status ?: "OK",
                        style = MaterialTheme.typography.titleMedium,
                        color = when (reminder?.status) {
                            "Overdue" -> FleetRed
                            "Due Soon" -> FleetAmber
                            else -> FleetGreen
                        },
                    )
                    Text(
                        reminder?.let {
                            buildString {
                                it.entry.nextDueDate?.let { d -> append("Date: ${formatDate(d)}") }
                                it.entry.nextDueKm?.let { km ->
                                    if (isNotEmpty()) append(" · ")
                                    append("Km: ${formatNum(km, 0)}")
                                }
                            }
                        } ?: "No pending reminder",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomRangeDialog(
    initialFrom: String,
    initialTo: String,
    onDismiss: () -> Unit,
    onApply: (String, String) -> Unit,
) {
    var from by remember { mutableStateOf(initialFrom) }
    var to by remember { mutableStateOf(initialTo) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom date range") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                com.fleetpulse.app.ui.components.AppDateField("From", from, { from = it })
                com.fleetpulse.app.ui.components.AppDateField("To", to, { to = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(from, to) }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

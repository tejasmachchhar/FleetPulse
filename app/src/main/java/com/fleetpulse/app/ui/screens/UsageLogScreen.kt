package com.fleetpulse.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.fleetpulse.app.data.db.Vehicle
import com.fleetpulse.app.domain.EvaluatedUsage
import com.fleetpulse.app.ui.components.ConfirmDialog
import com.fleetpulse.app.ui.components.EmptyState
import com.fleetpulse.app.ui.components.ScreenHeader
import com.fleetpulse.app.ui.components.SectionCard
import com.fleetpulse.app.ui.components.StatusBadge
import com.fleetpulse.app.ui.components.VehicleDot
import com.fleetpulse.app.ui.components.VehicleFilterDropdown
import com.fleetpulse.app.ui.components.formatDate
import com.fleetpulse.app.ui.components.formatINR
import com.fleetpulse.app.ui.components.formatNum
import com.fleetpulse.app.ui.theme.FleetAmber
import com.fleetpulse.app.ui.theme.FleetGreen
import com.fleetpulse.app.ui.theme.FleetRed

@Composable
fun UsageLogScreen(
    vehicles: List<Vehicle>,
    evaluated: List<EvaluatedUsage>,
    selectedVehicle: String,
    onVehicleFilter: (String) -> Unit,
    onAdd: (String?) -> Unit,
    onEditEntry: (String) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onReportProblem: () -> Unit,
) {
    var statusFilter by remember { mutableStateOf("ALL") }
    var pendingDelete by remember { mutableStateOf<EvaluatedUsage?>(null) }

    val filtered = evaluated
        .filter { selectedVehicle == "ALL" || it.entry.vehicleId == selectedVehicle }
        .filter { statusFilter == "ALL" || it.status == statusFilter }
        .sortedWith(compareByDescending<EvaluatedUsage> { it.entry.date }.thenByDescending { it.entry.createdAt })

    val allForSelection = evaluated.filter { selectedVehicle == "ALL" || it.entry.vehicleId == selectedVehicle }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Usage log",
                subtitle = "${allForSelection.size} entries · full-to-full verification",
                actions = {
                    IconButton(onClick = onReportProblem) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = "Report a problem with Usage log",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    TextButton(onClick = { onAdd(selectedVehicle.takeIf { it != "ALL" }) }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                },
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VehicleFilterDropdown(
                    vehicles = vehicles,
                    selectedId = selectedVehicle,
                    onSelect = onVehicleFilter,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        listOf(
                            "ALL" to "All (${allForSelection.size})",
                            "OK" to "OK (${allForSelection.count { it.status == "OK" }})",
                            "CHECK" to "CHECK (${allForSelection.count { it.status == "CHECK" }})",
                            "ERROR" to "ERROR (${allForSelection.count { it.status == "ERROR" }})",
                        ),
                    ) { (key, label) ->
                        FilterChip(
                            selected = statusFilter == key,
                            onClick = { statusFilter = key },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (key) {
                                    "ERROR" -> FleetRed
                                    "CHECK" -> FleetAmber
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }
            }
        }

        if (allForSelection.any { it.status == "ERROR" }) {
            item {
                SectionCard(title = "Data quality needs attention") {
                    Text(
                        "${allForSelection.count { it.status == "ERROR" }} entries cannot be used in calculations because " +
                            "mandatory data is missing or inconsistent. They are never deleted automatically — open and fix them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.History,
                    title = "No usage entries",
                    message = if (evaluated.isEmpty()) {
                        "Log your first charge, refill or ride to start tracking."
                    } else {
                        "No entries match this filter."
                    },
                )
            }
        }

        items(filtered, key = { it.entry.id }) { row ->
            UsageCard(
                row = row,
                onEdit = { onEditEntry(row.entry.id) },
                onDelete = { pendingDelete = row },
            )
        }

        item {
            SectionCard(title = "Full-to-full methodology") {
                Text(
                    "Mileage and efficiency are only counted between two full fills (full charge or full tank). " +
                        "Partial charges/refills are included in the next verified stretch, never shown as their own mileage. " +
                        "Battery wear is an estimate, not cash spent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    pendingDelete?.let { row ->
        ConfirmDialog(
            title = "Delete this entry?",
            message = "${formatDate(row.entry.date)} · ${row.vehicleName} · ${row.entry.fillType}. " +
                "This removes the record and recalculates every affected stretch.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { onDeleteEntry(row.entry.id) },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun UsageCard(row: EvaluatedUsage, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isEv = row.energySource == SOURCE_ELECTRICITY
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${formatDate(row.entry.date)} · ${row.vehicleName}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "${row.entry.fillType} · ${row.entry.vehicleId}" +
                            if (row.entry.isSample) " · [SAMPLE]" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (row.entry.isSample) FleetAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusBadge(row.status)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (row.entry.hoursUsed != null) {
                    InfoBlock("Operating hours", "${formatNum(row.entry.hoursUsed, 1)} h", Modifier.weight(1f))
                } else {
                    InfoBlock("Odometer", "${formatNum(row.entry.odometer, 0)} km", Modifier.weight(1f))
                }
                InfoBlock("Distance", "${formatNum(row.distance, 0)} km", Modifier.weight(1f))
                InfoBlock("Energy", "${formatNum(row.energyUsed, 2)} ${if (isEv) "kWh" else "L"}", Modifier.weight(1f))
                InfoBlock("Cost", formatINR(row.totalCost, 0), Modifier.weight(1f))
            }

            if (isEv && (row.pctBefore != null || row.pctAfter != null)) {
                Text(
                    "Battery: ${row.pctBefore?.let { formatNum(it, 0) + "%" } ?: "—"} → " +
                        "${row.pctAfter?.let { formatNum(it, 0) + "%" } ?: "—"}" +
                        (row.basis?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!row.entry.includeInMileage) {
                Text(
                    "Mileage excluded · history and expenses retained",
                    style = MaterialTheme.typography.labelSmall,
                    color = FleetAmber,
                )
            }

            if (row.isVerifiedStretch) {
                Text(
                    buildString {
                        append("Verified full-to-full · ")
                        if (isEv) {
                            append("${formatNum(row.kmPerCharge, 1)} km/charge · ")
                        }
                        append("${formatNum(row.stretchEfficiency, 2)} ${if (isEv) "km/kWh" else "km/L"}")
                        if (row.stretchDistance != null && row.stretchEnergy != null) {
                            append(" · ${formatNum(row.stretchDistance, 0)} km / ${formatNum(row.stretchEnergy, 2)}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (row.isPlausibleStretch) FleetGreen else FleetAmber,
                )
            } else if (row.basis != null) {
                Text(
                    "Energy basis: ${row.basis}${if (row.energyUsed > 0) " · ${formatINR(row.applicablePrice, 2)} per unit" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (row.status != "OK") {
                Text(
                    row.checkMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (row.status == "ERROR") FleetRed else Color(0xFF8A5A00),
                )
            }
            if (row.entry.remarks.isNotBlank()) {
                Text(
                    row.entry.remarks,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = FleetRed, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", color = FleetRed)
                }
            }
        }
    }
}

@Composable
private fun InfoBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
        )
    }
}

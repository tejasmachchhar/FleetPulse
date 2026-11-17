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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fleetpulse.app.data.db.CalibrationPoint
import com.fleetpulse.app.data.db.SOURCE_ELECTRICITY
import com.fleetpulse.app.data.db.Vehicle
import com.fleetpulse.app.domain.CalcEngine
import com.fleetpulse.app.domain.EvalResult
import com.fleetpulse.app.domain.NumberText
import com.fleetpulse.app.ui.components.ChartSeries
import com.fleetpulse.app.ui.components.DecimalField
import com.fleetpulse.app.ui.components.EmptyState
import com.fleetpulse.app.ui.components.FormSection
import com.fleetpulse.app.ui.components.KeyboardAwareFormDialog
import com.fleetpulse.app.ui.components.LineChart
import com.fleetpulse.app.ui.components.ScreenHeader
import com.fleetpulse.app.ui.components.SectionCard
import com.fleetpulse.app.ui.components.StatusBadge
import com.fleetpulse.app.ui.components.VehicleFilterDropdown
import com.fleetpulse.app.ui.components.bringIntoViewOnIme
import com.fleetpulse.app.ui.components.formatNum
import com.fleetpulse.app.ui.theme.FleetAmber
import com.fleetpulse.app.ui.theme.FleetGreen
import com.fleetpulse.app.ui.theme.FleetRed
import java.util.UUID

@Composable
fun CalibrationScreen(
    vehicles: List<Vehicle>,
    calibration: List<CalibrationPoint>,
    evaluation: EvalResult,
    selectedVehicle: String,
    onSelectVehicle: (String) -> Unit,
    onSave: (CalibrationPoint) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
) {
    val evVehicles = vehicles.filter { it.energySource == SOURCE_ELECTRICITY }
    var editingPoint by remember { mutableStateOf<CalibrationPoint?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<CalibrationPoint?>(null) }
    var testVoltage by remember { mutableStateOf("") }

    val vehicleId = if (selectedVehicle != "ALL" && evVehicles.any { it.id == selectedVehicle }) {
        selectedVehicle
    } else {
        evVehicles.firstOrNull()?.id ?: "ALL"
    }
    val points = calibration.filter { it.vehicleId == vehicleId }.sortedBy { it.voltage }
    val checks = evaluation.calibrationChecks.filter { it.point.vehicleId == vehicleId }
        .associateBy { it.point.id }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Calibration",
                subtitle = "Voltage → battery % curve per electric vehicle",
                onBack = onBack,
                actions = {
                    if (evVehicles.isNotEmpty()) {
                        Button(onClick = { adding = true }, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Point")
                        }
                    }
                },
            )
        }

        if (evVehicles.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Tune,
                    title = "Calibration applies to electric vehicles",
                    message = "Add an electric vehicle first. Petrol vehicles use full-tank mileage and do not need a voltage curve.",
                )
            }
            return@LazyColumn
        }

        item {
            VehicleFilterDropdown(
                vehicles = evVehicles,
                selectedId = vehicleId,
                onSelect = onSelectVehicle,
                includeAll = false,
            )
        }

        val sampleCount = points.count { it.isSample }
        if (sampleCount > 0) {
            item {
                SectionCard(title = "Reference curve in use") {
                    Text(
                        "$sampleCount sample calibration points for this vehicle are pre-loaded from the reference workbook. " +
                            "Replace them with your own measured readings: they are only as accurate as the curve.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            SectionCard(
                title = "Check a voltage",
                subtitle = "Uses the same interpolation as usage entries",
            ) {
                DecimalField("Battery voltage", testVoltage, { testVoltage = it }, suffix = "V", placeholder = "e.g. 60.0")
                val result = remember(testVoltage, points) {
                    testVoltage.toDoubleOrNull()?.let { v ->
                        CalcEngine.interpolateVoltage(vehicleId, v, calibration)
                    }
                }
                if (testVoltage.isNotBlank()) {
                    if (result == null) {
                        Text("Enter a valid voltage.", style = MaterialTheme.typography.bodySmall, color = FleetRed)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Estimated state of charge: ${result.percent?.let { formatNum(it, 1) + "%" } ?: "no curve"}",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            StatusBadge(
                                when {
                                    result.missingCurve -> "ERROR"
                                    result.outOfRange -> "CHECK"
                                    else -> "OK"
                                },
                            )
                        }
                        Text(
                            when {
                                result.missingCurve -> "No calibration points for this vehicle. Add points before using voltage-based entries."
                                result.outOfRange -> "This voltage is outside the calibrated range, so the result is clamped to the nearest point and flagged."
                                else -> "Within the calibrated range."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                result.missingCurve -> FleetRed
                                result.outOfRange -> FleetAmber
                                else -> FleetGreen
                            },
                        )
                    }
                }
            }
        }

        if (points.isNotEmpty()) {
            item {
                SectionCard(title = "Voltage curve", subtitle = "Sorted by voltage · linear interpolation between points") {
                    LineChart(
                        labels = points.map { formatNum(it.voltage, 1) },
                        series = listOf(
                            ChartSeries(
                                "Battery %",
                                Color(0xFF2563EB),
                                points.map { it.percent },
                            ),
                        ),
                    )
                }
            }
        }

        item {
            SectionCard(title = "Calibration points", subtitle = "${points.size} points") {
                Text(
                    "A rising voltage must map to a rising percentage. Duplicate voltages are flagged ERROR; " +
                        "a percentage that does not rise is flagged CHECK.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(points, key = { it.id }) { point ->
            val check = checks[point.id]?.check ?: "OK"
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(0.9f)) {
                        Text("${formatNum(point.voltage, 2)} V", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${formatNum(point.percent, 1)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(Modifier.weight(1.4f)) {
                        StatusBadge(
                            when {
                                check.startsWith("ERROR") -> "ERROR"
                                check.startsWith("CHECK") -> "CHECK"
                                else -> "OK"
                            },
                        )
                        if (point.isSample) {
                            Text("[SAMPLE]", style = MaterialTheme.typography.labelSmall, color = FleetAmber)
                        }
                    }
                    IconButton(onClick = { editingPoint = point }) {
                        Icon(Icons.Default.Edit, "Edit calibration point")
                    }
                    IconButton(onClick = { deleteTarget = point }) {
                        Icon(Icons.Default.Delete, "Delete calibration point", tint = FleetRed)
                    }
                }
            }
        }

        if (points.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Tune,
                    title = "No calibration points",
                    message = "Add measured voltage/% pairs from your battery. Without a curve, voltage-based entries are rejected.",
                )
            }
        }

        item {
            SectionCard(title = "How to calibrate") {
                Text(
                    "Measure pack voltage with a multimeter at rest (no charger connected, 30+ minutes after riding) and " +
                        "note the battery percentage from the display at the same moment. Repeat at several states of charge, " +
                        "especially near empty and full. The app interpolates linearly between points and clamps outside the range with a warning.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (adding || editingPoint != null) {
        CalibrationPointDialog(
            existing = editingPoint,
            vehicleId = vehicleId,
            onDismiss = {
                adding = false
                editingPoint = null
            },
            onSave = { point ->
                onSave(point)
                adding = false
                editingPoint = null
            },
        )
    }

    deleteTarget?.let { p ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete calibration point?") },
            text = { Text("${formatNum(p.voltage, 2)} V → ${formatNum(p.percent, 1)}% will be removed from the curve.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(p.id)
                    deleteTarget = null
                }) { Text("Delete", color = FleetRed) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun CalibrationPointDialog(
    existing: CalibrationPoint?,
    vehicleId: String,
    onDismiss: () -> Unit,
    onSave: (CalibrationPoint) -> Unit,
) {
    var voltage by remember { mutableStateOf(existing?.voltage?.let { NumberText.plain(it) } ?: "") }
    var percent by remember { mutableStateOf(existing?.percent?.let { NumberText.plain(it) } ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }

    KeyboardAwareFormDialog(
        onDismissRequest = onDismiss,
        title = if (existing == null) "Add calibration point" else "Edit calibration point",
        confirmLabel = "Save",
        onConfirm = {
            onSave(
                CalibrationPoint(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    vehicleId = vehicleId,
                    voltage = NumberText.parse(voltage) ?: 0.0,
                    percent = (NumberText.parse(percent) ?: 0.0).coerceIn(0.0, 100.0),
                    note = note.trim(),
                    isSample = false,
                ),
            )
        },
        confirmEnabled = NumberText.parse(voltage) != null && NumberText.parse(percent) != null,
    ) {
        FormSection("Measured pair") {
            DecimalField("Voltage", voltage, { voltage = it }, suffix = "V")
            DecimalField("Battery percentage", percent, { percent = it }, suffix = "%")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnIme(),
                shape = RoundedCornerShape(14.dp),
            )
        }
    }
}

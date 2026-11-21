package com.fleetpulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fleetpulse.app.data.db.SOURCE_ELECTRICITY
import com.fleetpulse.app.data.db.SOURCE_PETROL
import com.fleetpulse.app.data.db.SOURCE_DIESEL
import com.fleetpulse.app.data.db.SOURCE_CNG
import com.fleetpulse.app.data.db.SOURCE_LPG
import com.fleetpulse.app.data.db.SOURCE_HYBRID
import com.fleetpulse.app.data.db.UsageEntry
import com.fleetpulse.app.data.db.Vehicle
import com.fleetpulse.app.domain.EvalResult
import com.fleetpulse.app.domain.NumberText
import com.fleetpulse.app.ui.components.AppDateField
import com.fleetpulse.app.ui.components.ConfirmDialog
import com.fleetpulse.app.ui.components.DecimalField
import com.fleetpulse.app.ui.components.bringIntoViewOnIme
import com.fleetpulse.app.ui.components.EmptyState
import com.fleetpulse.app.ui.components.FormSection
import com.fleetpulse.app.ui.components.LabelValueRow
import com.fleetpulse.app.ui.components.ScreenHeader
import com.fleetpulse.app.ui.components.SectionCard
import com.fleetpulse.app.ui.MainViewModel
import com.fleetpulse.app.ui.components.VehicleDot
import com.fleetpulse.app.ui.components.formatDate
import com.fleetpulse.app.ui.components.formatINR
import com.fleetpulse.app.ui.components.formatNum
import com.fleetpulse.app.ui.components.parseColor
import com.fleetpulse.app.ui.theme.FleetAmber
import com.fleetpulse.app.ui.theme.FleetBlue
import com.fleetpulse.app.ui.theme.FleetGreen
import com.fleetpulse.app.ui.theme.FleetOrange
import com.fleetpulse.app.ui.theme.FleetRed
import com.fleetpulse.app.ui.theme.FleetViolet
import java.time.LocalDate

@Composable
fun VehiclesScreen(
    vm: MainViewModel,
    vehicles: List<Vehicle>,
    onCreatingChange: (Boolean) -> Unit = {},
    evaluation: EvalResult,
    nextId: () -> String,
    onSave: (Vehicle, Boolean) -> Boolean,
    onArchive: (String, Boolean) -> Unit,
    onDelete: (String, String) -> Unit,
    onAddEntry: (String) -> Unit,
) {
    var editing by remember { mutableStateOf<Vehicle?>(null) }
    var creating by remember { mutableStateOf(false) }
    var archiveTarget by remember { mutableStateOf<Vehicle?>(null) }
    var deleteTarget by remember { mutableStateOf<Vehicle?>(null) }
    var showArchived by remember { mutableStateOf(false) }

    val active = vehicles.filter { !it.archived }
    val archived = vehicles.filter { it.archived }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Vehicles",
                subtitle = "${active.size} active · ${archived.size} archived",
                actions = {
                    Button(onClick = { creating = true; onCreatingChange(true) }, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                },
            )
        }

        if (active.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.DirectionsBike,
                    title = "No active vehicles",
                    message = "Add a vehicle with its battery or tank details so running costs can be calculated.",
                )
            }
        }

        items(active, key = { it.id }) { v ->
            VehicleCard(
                vehicle = v,
                evaluation = evaluation,
                onEdit = { editing = v },
                onAddEntry = { onAddEntry(v.id) },
                onArchive = { archiveTarget = v },
            )
        }

        if (archived.isNotEmpty()) {
            item {
                TextButton(onClick = { showArchived = !showArchived }) {
                    Text(if (showArchived) "Hide archived vehicles" else "Show archived vehicles (${archived.size})")
                }
            }
            if (showArchived) {
                items(archived, key = { "arch-" + it.id }) { v ->
                    ArchivedVehicleCard(
                        vehicle = v,
                        onRestore = { onArchive(v.id, false) },
                        onDelete = { deleteTarget = v },
                    )
                }
            }
        }

        item {
            SectionCard(title = "Why archive instead of delete?") {
                Text(
                    "Archiving keeps every historical record and removes the vehicle from dashboards and pickers. " +
                        "Permanent deletion also deletes that vehicle's usage, service and calibration records — " +
                        "it is only offered with an explicit warning.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (creating) {
        VehicleSetupWizard(
            vm = vm,
            onComplete = { creating = false; onCreatingChange(false) },
        )
    }

    if (!creating && editing != null) {
        val target = editing
        VehicleEditorDialog(
            existing = target,
            suggestedId = remember { nextId() },
            canDelete = vehicles.size > 1,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { v, isNew ->
                if (onSave(v, isNew)) {
                    creating = false
                    editing = null
                    true
                } else {
                    false
                }
            },
            onDelete = {
                editing = null
                deleteTarget = target
            },
        )
    }

    archiveTarget?.let { v ->
        ConfirmDialog(
            title = "Archive ${v.name}?",
            message = "The vehicle disappears from active dashboards but all its history is preserved. " +
                "You can restore it any time from the archived list.",
            confirmLabel = if (v.archived) "Restore" else "Archive",
            onConfirm = { onArchive(v.id, !v.archived) },
            onDismiss = { archiveTarget = null },
        )
    }

    deleteTarget?.let { v ->
        ConfirmDialog(
            title = "Delete ${v.name} permanently?",
            message = "This deletes the vehicle AND its ${evaluation.evaluatedUsage.count { it.entry.vehicleId == v.id }} usage entries, " +
                "${evaluation.evaluatedMaintenance.count { it.entry.vehicleId == v.id }} service records and calibration points. " +
                "This cannot be undone. Export a backup first if you need the history.",
            confirmLabel = "Delete permanently",
            destructive = true,
            onConfirm = {
                onDelete(v.id, v.name)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    evaluation: EvalResult,
    onEdit: () -> Unit,
    onAddEntry: () -> Unit,
    onArchive: () -> Unit,
) {
    val allTime = evaluation.vehicleAllTimeStats[vehicle.id]
    val period = evaluation.periodStatsByVehicle[vehicle.id]
    val isEv = vehicle.energySource == SOURCE_ELECTRICITY
    val lastEntry: UsageEntry? = evaluation.evaluatedUsage
        .filter { it.entry.vehicleId == vehicle.id }
        .maxByOrNull { it.entry.date }
        ?.entry

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .background(parseColor(vehicle.color).copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    VehicleDot(vehicle.color, 16.dp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        vehicle.name + if (vehicle.isSample) "  ·  SAMPLE" else "",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (vehicle.isSample) FleetAmber else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "${vehicle.id} · ${vehicle.type} · ${vehicle.regNo.ifBlank { "No registration" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit ${vehicle.name}") }
                IconButton(onClick = onArchive) { Icon(Icons.Default.Archive, "Archive ${vehicle.name}") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MiniMetric("Odometer", "${formatNum(allTime?.currentOdometer ?: vehicle.initialOdometer, 0)} km", Modifier.weight(1f))
                MiniMetric(
                    "Running cost",
                    period?.runningCostPerKm?.let { formatINR(it, 2) + "/km" } ?: "—",
                    Modifier.weight(1f),
                )
                MiniMetric(
                    "Period distance",
                    "${formatNum(period?.distance ?: 0.0, 0)} km",
                    Modifier.weight(1f),
                )
            }

            if (isEv) {
                LabelValueRow(
                    "Battery",
                    "${formatNum(vehicle.batteryCapacity, 2)} kWh · ${vehicle.chargeCycles ?: 0} cycles · " +
                        "${formatNum(vehicle.batteryLifeYears, 0)} yr",
                )
                LabelValueRow(
                    "Wear estimate",
                    "${formatINR(allTime?.batteryWearCostPerKm ?: 0.0, 2)}/km · " +
                        "${allTime?.kmPerChargeSource ?: "rated"}",
                )
                LabelValueRow(
                    "Latest verified",
                    allTime?.latestValidMileage?.let { "${formatNum(it, 1)} km/charge" } ?: "No verified stretch yet",
                )
                LabelValueRow(
                    "All-time average",
                    allTime?.allTimeAvgKmPerCharge?.let { "${formatNum(it, 1)} km/charge" } ?: "Not enough verified stretches",
                )
                if (period?.efficiencyBasis == "Provisional" && period.efficiencyUsed != null) {
                    LabelValueRow(
                        "Provisional period",
                        "${formatNum(period.efficiencyUsed, 1)} km/kWh",
                        FleetAmber,
                    )
                }
            } else {
                LabelValueRow("Tank", "${formatNum(vehicle.tankCapacity, 1)} L")
                LabelValueRow(
                    "Latest verified",
                    allTime?.latestValidMileage?.let { "${formatNum(it, 1)} km/L" } ?: "No verified stretch yet",
                )
                LabelValueRow(
                    "All-time average",
                    allTime?.allTimeAvgKmPerCharge?.let { "${formatNum(it, 1)} km/L" } ?: "Not enough verified stretches",
                )
                if (period?.efficiencyBasis == "Provisional" && period.efficiencyUsed != null) {
                    LabelValueRow(
                        "Provisional period",
                        "${formatNum(period.efficiencyUsed, 2)} km/L",
                        FleetAmber,
                    )
                }
            }
            LabelValueRow(
                "Last entry",
                lastEntry?.let { "${formatDate(it.date)} · ${it.fillType}" } ?: "No usage logged yet",
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddEntry, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Log usage")
                }
                OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(12.dp)) {
                    Text("Edit setup")
                }
            }
        }
    }
}

@Composable
private fun MiniMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ArchivedVehicleCard(vehicle: Vehicle, onRestore: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            VehicleDot(vehicle.color)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    vehicle.name + if (vehicle.isSample) "  ·  SAMPLE" else "",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (vehicle.isSample) FleetAmber else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${vehicle.id} · archived",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRestore) { Icon(Icons.Default.Unarchive, "Restore ${vehicle.name}") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete ${vehicle.name}", tint = FleetRed)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleEditorDialog(
    existing: Vehicle?,
    suggestedId: String,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (Vehicle, Boolean) -> Boolean,
    onDelete: () -> Unit,
) {
    val isNew = existing == null
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var id by remember { mutableStateOf(existing?.id ?: suggestedId) }
    var type by remember { mutableStateOf(existing?.type ?: "Electric Bike") }
    var source by remember { mutableStateOf(existing?.energySource ?: SOURCE_ELECTRICITY) }
    var regNo by remember { mutableStateOf(existing?.regNo ?: "") }
    var purchaseDate by remember { mutableStateOf(existing?.purchaseDate ?: LocalDate.now().toString()) }
    var initialOdo by remember { mutableStateOf(existing?.initialOdometer?.let { NumberText.plain(it) } ?: "0") }
    var batteryCapacity by remember { mutableStateOf(existing?.batteryCapacity?.let { NumberText.plain(it) } ?: "") }
    var batteryRate by remember { mutableStateOf(existing?.batteryRate?.let { NumberText.plain(it) } ?: "") }
    var batteryLife by remember { mutableStateOf(existing?.batteryLifeYears?.let { NumberText.plain(it) } ?: "") }
    var cycles by remember { mutableStateOf(existing?.chargeCycles?.toString() ?: "") }
    var ratedKm by remember { mutableStateOf(existing?.ratedKmPerCharge?.let { NumberText.plain(it) } ?: "") }
    var dailyKm by remember { mutableStateOf(existing?.dailyKmPlan?.let { NumberText.plain(it) } ?: "") }
    var tank by remember { mutableStateOf(existing?.tankCapacity?.let { NumberText.plain(it) } ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var color by remember { mutableStateOf(existing?.color ?: "#2563EB") }
    var showDelete by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(existing?.category ?: "two_wheeler") }
    var subcategory by remember { mutableStateOf(existing?.subcategory ?: "") }
    var trackingBasis by remember { mutableStateOf(existing?.trackingBasis ?: "distance_km") }
    var referenceRecordId by remember { mutableStateOf(existing?.referenceRecordId ?: "") }
    var referenceDatabaseVersion by remember { mutableStateOf(existing?.referenceDatabaseVersion ?: "") }
    var referenceRawJson by remember { mutableStateOf(existing?.referenceRawJson ?: "") }
    var referenceOverridesJson by remember { mutableStateOf(existing?.referenceOverridesJson ?: "") }
    var tankCapacityLiters by remember { mutableStateOf(existing?.tankCapacityLiters?.let { NumberText.plain(it) } ?: "") }
    var cargoCapacityLiters by remember { mutableStateOf(existing?.cargoCapacityLiters?.let { NumberText.plain(it) } ?: "") }
    var seatingCapacity by remember { mutableStateOf(existing?.seatingCapacity?.toString() ?: "") }
    var wheelCount by remember { mutableStateOf(existing?.wheelCount?.toString() ?: "") }
    var axleConfiguration by remember { mutableStateOf(existing?.axleConfiguration ?: "") }
    var powerKw by remember { mutableStateOf(existing?.powerKw?.let { NumberText.plain(it) } ?: "") }
    var engineDisplacementCc by remember { mutableStateOf(existing?.engineDisplacementCc?.let { NumberText.plain(it) } ?: "") }
    var torqueNm by remember { mutableStateOf(existing?.torqueNm?.let { NumberText.plain(it) } ?: "") }
    var engineOilGrade by remember { mutableStateOf(existing?.engineOilGrade ?: "") }
    var initialOperatingHours by remember { mutableStateOf(existing?.initialOperatingHours?.let { NumberText.plain(it) } ?: "") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = if (isNew) "Add vehicle" else "Edit ${existing?.name}",
                    subtitle = if (isNew) "Set up the vehicle once; costs update from usage logs" else existing?.id,
                    onBack = onDismiss,
                    actions = {
                        TextButton(
                            onClick = {
                                val vehicle = existing?.copy(
                                    id = id.trim(),
                                    name = name.trim(),
                                    type = type.trim().ifBlank { "Vehicle" },
                                    energySource = source,
                                    category = category,
                                    subcategory = subcategory,
                                    trackingBasis = trackingBasis,
                                    referenceRecordId = referenceRecordId,
                                    referenceDatabaseVersion = referenceDatabaseVersion,
                                    referenceRawJson = referenceRawJson,
                                    referenceOverridesJson = referenceOverridesJson,
                                    regNo = regNo.trim(),
                                    purchaseDate = purchaseDate,
                                    initialOdometer = NumberText.parse(initialOdo) ?: 0.0,
                                    batteryCapacity = if (source == SOURCE_ELECTRICITY) NumberText.parse(batteryCapacity) else existing?.batteryCapacity,
                                    batteryRate = if (source == SOURCE_ELECTRICITY) NumberText.parse(batteryRate) else existing?.batteryRate,
                                    batteryLifeYears = if (source == SOURCE_ELECTRICITY) NumberText.parse(batteryLife) else existing?.batteryLifeYears,
                                    chargeCycles = if (source == SOURCE_ELECTRICITY) cycles.toIntOrNull() else existing?.chargeCycles,
                                    ratedKmPerCharge = if (source == SOURCE_ELECTRICITY) NumberText.parse(ratedKm) else existing?.ratedKmPerCharge,
                                    dailyKmPlan = if (source == SOURCE_ELECTRICITY) NumberText.parse(dailyKm) else existing?.dailyKmPlan,
                                    tankCapacity = if (source == SOURCE_PETROL) NumberText.parse(tank) else existing?.tankCapacity,
                                    tankCapacityLiters = NumberText.parse(tankCapacityLiters),
                                    cargoCapacityLiters = NumberText.parse(cargoCapacityLiters),
                                    seatingCapacity = seatingCapacity.toIntOrNull(),
                                    wheelCount = wheelCount.toIntOrNull(),
                                    axleConfiguration = axleConfiguration.takeIf { it.isNotBlank() },
                                    powerKw = NumberText.parse(powerKw),
                                    engineDisplacementCc = NumberText.parse(engineDisplacementCc),
                                    torqueNm = NumberText.parse(torqueNm),
                                    engineOilGrade = engineOilGrade.takeIf { it.isNotBlank() },
                                    initialOperatingHours = NumberText.parse(initialOperatingHours),
                                    color = color,
                                    notes = notes.trim(),
                                    archived = existing?.archived ?: false,
                                    isSample = existing?.isSample ?: false,
                                ) ?: Vehicle(
                                    id = id.trim(),
                                    name = name.trim(),
                                    type = type.trim().ifBlank { "Vehicle" },
                                    energySource = source,
                                    category = category,
                                    subcategory = subcategory,
                                    trackingBasis = trackingBasis,
                                    regNo = regNo.trim(),
                                    purchaseDate = purchaseDate,
                                    initialOdometer = NumberText.parse(initialOdo) ?: 0.0,
                                    batteryCapacity = if (source == SOURCE_ELECTRICITY) NumberText.parse(batteryCapacity) else null,
                                    batteryRate = if (source == SOURCE_ELECTRICITY) NumberText.parse(batteryRate) else null,
                                    batteryLifeYears = if (source == SOURCE_ELECTRICITY) NumberText.parse(batteryLife) else null,
                                    chargeCycles = if (source == SOURCE_ELECTRICITY) cycles.toIntOrNull() else null,
                                    ratedKmPerCharge = if (source == SOURCE_ELECTRICITY) NumberText.parse(ratedKm) else null,
                                    dailyKmPlan = if (source == SOURCE_ELECTRICITY) NumberText.parse(dailyKm) else null,
                                    tankCapacity = if (source == SOURCE_PETROL) NumberText.parse(tank) else null,
                                    tankCapacityLiters = NumberText.parse(tankCapacityLiters),
                                    cargoCapacityLiters = NumberText.parse(cargoCapacityLiters),
                                    seatingCapacity = seatingCapacity.toIntOrNull(),
                                    wheelCount = wheelCount.toIntOrNull(),
                                    axleConfiguration = axleConfiguration.takeIf { it.isNotBlank() },
                                    powerKw = NumberText.parse(powerKw),
                                    engineDisplacementCc = NumberText.parse(engineDisplacementCc),
                                    torqueNm = NumberText.parse(torqueNm),
                                    engineOilGrade = engineOilGrade.takeIf { it.isNotBlank() },
                                    initialOperatingHours = NumberText.parse(initialOperatingHours),
                                    color = color,
                                    notes = notes.trim(),
                                )
                                onSave(vehicle, isNew)
                            },
                            enabled = name.isNotBlank() && id.isNotBlank(),
                        ) { Text("Save") }
                    },
                )
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    FormSection("Identity") {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Vehicle name (required)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnIme(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = id,
                            onValueChange = { if (isNew) id = it },
                            label = { Text("Vehicle ID") },
                            readOnly = !isNew,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnIme(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = type,
                            onValueChange = { type = it },
                            label = { Text("Type (e.g. Electric Bike, Diesel Tractor)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnIme(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = regNo,
                            onValueChange = { regNo = it },
                            label = { Text("Registration number (optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnIme(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                        )
                    }

                    FormSection("Energy source") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(SOURCE_ELECTRICITY, SOURCE_PETROL, SOURCE_DIESEL, SOURCE_CNG, SOURCE_LPG, SOURCE_HYBRID).forEach { option ->
                                if (option == source) {
                                    Button(onClick = { source = option }, shape = RoundedCornerShape(12.dp)) {
                                        Text(option)
                                    }
                                } else {
                                    OutlinedButton(onClick = { source = option }, shape = RoundedCornerShape(12.dp)) {
                                        Text(option)
                                    }
                                }
                            }
                        }
                        Text(
                            "Electric vehicles use the voltage calibration curve and battery wear model. " +
                                "Non-electric vehicles use full-tank mileage and fuel prices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    FormSection("Odometer") {
                        AppDateField("Purchase date", purchaseDate, { purchaseDate = it })
                        DecimalField(
                            label = "Initial odometer (km)",
                            value = initialOdo,
                            onValueChange = { initialOdo = it },
                            suffix = "km",
                        )
                        Text(
                            "Set this to the odometer reading when tracking began. The first reading far above " +
                                "this value is used as the starting point with a CHECK note.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (source == SOURCE_ELECTRICITY) {
                        FormSection("Battery & wear model") {
                            DecimalField(
                                "Usable battery capacity", batteryCapacity,
                                { batteryCapacity = it }, suffix = "kWh",
                            )
                            DecimalField(
                                "Battery replacement cost per kWh", batteryRate,
                                { batteryRate = it }, suffix = "₹",
                            )
                            DecimalField(
                                "Expected battery life", batteryLife,
                                { batteryLife = it }, suffix = "years",
                            )
                            OutlinedTextField(
                                value = cycles,
                                onValueChange = { new -> if (new.all { it.isDigit() }) cycles = new },
                                label = { Text("Expected charge cycles") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewOnIme(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                            )
                            DecimalField(
                                "Rated km per full charge", ratedKm,
                                { ratedKm = it }, suffix = "km",
                            )
                            DecimalField(
                                "Average km ridden per day", dailyKm,
                                { dailyKm = it }, suffix = "km",
                            )
                            Text(
                                "Battery wear cost = battery replacement cost ÷ the smaller of " +
                                    "(cycles × km/charge) and (years × 365 × km/day).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        FormSection("Tank") {
                            DecimalField("Tank capacity", tank, { tank = it }, suffix = "L")
                        }
                    }

                    FormSection("Appearance") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(
                                "#2563EB" to "Blue",
                                "#16A34A" to "Green",
                                "#EA580C" to "Orange",
                                "#7C3AED" to "Violet",
                                "#0D9488" to "Teal",
                                "#DC2626" to "Red",
                            ).forEach { (hex, label) ->
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .background(parseColor(hex), RoundedCornerShape(10.dp))
                                        .clickable { color = hex },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (color == hex) {
                                        Text(
                                            "✓",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    } else {
                                        Text("", color = Color.White)
                                    }
                                }
                            }
                        }
                        Text(
                            "Used for charts and dashboard accents.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    FormSection("Notes") {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes (optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .bringIntoViewOnIme(),
                            shape = RoundedCornerShape(14.dp),
                        )
                    }

                    if (!isNew && canDelete) {
                        OutlinedButton(
                            onClick = { showDelete = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Default.Delete, null, tint = FleetRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete vehicle permanently", color = FleetRed)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete ${existing?.name}?",
            message = "The vehicle and all of its historical records will be deleted. " +
                "Archive it instead if you only want it out of the way.",
            confirmLabel = "Delete permanently",
            destructive = true,
            onConfirm = onDelete,
            onDismiss = { showDelete = false },
        )
    }
}

package com.fleetpulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fleetpulse.app.data.db.MaintenanceEntry
import com.fleetpulse.app.data.db.SettingsEntity
import com.fleetpulse.app.data.db.Vehicle
import com.fleetpulse.app.data.db.categoryList
import com.fleetpulse.app.domain.EvaluatedMaintenance
import com.fleetpulse.app.domain.NumberText
import com.fleetpulse.app.ui.components.AppDateField
import com.fleetpulse.app.ui.components.ConfirmDialog
import com.fleetpulse.app.ui.components.DecimalField
import com.fleetpulse.app.ui.components.bringIntoViewOnIme
import com.fleetpulse.app.ui.components.EmptyState
import com.fleetpulse.app.ui.components.FormSection
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
import java.time.LocalDate
import java.util.UUID

@Composable
fun MaintenanceScreen(
    vehicles: List<Vehicle>,
    items: List<EvaluatedMaintenance>,
    settings: SettingsEntity,
    selectedVehicle: String,
    onVehicleFilter: (String) -> Unit,
    currentOdometer: (String) -> Double,
    autoOpenAdd: Boolean,
    onConsumeAutoOpen: () -> Unit,
    onSave: (MaintenanceEntry, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onReportProblem: () -> Unit,
) {
    var editing by remember { mutableStateOf<MaintenanceEntry?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<EvaluatedMaintenance?>(null) }
    var statusFilter by remember { mutableStateOf("PENDING") }

    LaunchedEffect(autoOpenAdd, vehicles.isNotEmpty()) {
        if (autoOpenAdd && vehicles.isNotEmpty()) {
            creating = true
            onConsumeAutoOpen()
        }
    }

    val activeIds = vehicles.map { it.id }.toSet()
    val effectiveSelectedVehicle = selectedVehicle.takeIf { it == "ALL" || it in activeIds } ?: "ALL"
    val selected = items.filter { effectiveSelectedVehicle == "ALL" || it.entry.vehicleId == effectiveSelectedVehicle }
    val shown = selected.filter {
        when (statusFilter) {
            "PENDING" -> it.pending
            "ALL" -> true
            else -> it.status == statusFilter
        }
    }.sortedWith(compareBy({ statusRank(it.status) }, { it.daysLeft ?: 999_999L }))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Maintenance",
                subtitle = "Reminders retire automatically when a newer record of the same category is logged",
                actions = {
                    IconButton(onClick = onReportProblem) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = "Report a problem with Maintenance",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Button(
                        onClick = { creating = true },
                        shape = RoundedCornerShape(12.dp),
                        enabled = vehicles.isNotEmpty(),
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Log")
                    }
                },
            )
        }
        item {
            VehicleFilterDropdown(
                vehicles = vehicles,
                selectedId = effectiveSelectedVehicle,
                onSelect = onVehicleFilter,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryPill("Overdue", selected.count { it.status == "Overdue" }, FleetRed)
                SummaryPill("Due Soon", selected.count { it.status == "Due Soon" }, FleetAmber)
                SummaryPill("Pending", selected.count { it.pending }, FleetGreen)
                SummaryPill(
                    "Total spend",
                    selected.sumOf { it.entry.amount }.toInt(),
                    MaterialTheme.colorScheme.primary,
                    isMoney = true,
                )
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    listOf(
                        "PENDING" to "Pending",
                        "Overdue" to "Overdue",
                        "Due Soon" to "Due Soon",
                        "OK" to "Scheduled OK",
                        "Completed" to "Completed",
                        "Logged" to "Logged",
                        "ALL" to "All",
                    ),
                ) { (key, label) ->
                    FilterChip(
                        selected = statusFilter == key,
                        onClick = { statusFilter = key },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
        }

        if (shown.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Build,
                    title = "Nothing here",
                    message = "Log a service, repair or part replacement and optionally set the next due date or odometer.",
                )
            }
        }

        items(shown, key = { it.entry.id }) { m ->
            MaintenanceCard(
                item = m,
                onEdit = { editing = m.entry },
                onDelete = { deleteTarget = m },
            )
        }

        item {
            SectionCard(title = "How reminders work") {
                Text(
                    "A record with a next-due date or odometer is a reminder. When you later log a newer record in the same " +
                        "category for the same vehicle, the older reminder becomes Completed automatically. " +
                        "Km reminders use the latest odometer you have logged — never a guessed value. " +
                        "Maintenance is reported separately and is not mixed into running cost per km.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (creating || editing != null) {
        MaintenanceEditorDialog(
            existing = editing,
            vehicles = vehicles,
            categories = settings.categoryList(),
            initialVehicleId = if (effectiveSelectedVehicle != "ALL") effectiveSelectedVehicle else vehicles.firstOrNull()?.id,
            currentOdometer = currentOdometer,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { entry, isNew ->
                onSave(entry, isNew)
                creating = false
                editing = null
            },
        )
    }

    deleteTarget?.let { m ->
        ConfirmDialog(
            title = "Delete this maintenance record?",
            message = "${m.entry.category} on ${formatDate(m.entry.date)} for ${m.vehicleName}. " +
                "Reminder supersession will be recalculated.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { onDelete(m.entry.id) },
            onDismiss = { deleteTarget = null },
        )
    }
}

private fun statusRank(status: String): Int = when (status) {
    "Overdue" -> 0
    "Due Soon" -> 1
    "OK" -> 2
    "Completed" -> 3
    else -> 4
}

@Composable
private fun SummaryPill(label: String, count: Int, color: androidx.compose.ui.graphics.Color, isMoney: Boolean = false) {
    Column(
        Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (isMoney) formatINR(count.toDouble(), 0) else count.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MaintenanceCard(item: EvaluatedMaintenance, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VehicleDot("#64748B")
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.entry.category + if (item.entry.isSample) "  ·  SAMPLE" else "",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (item.entry.isSample) FleetAmber else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "${item.vehicleName} · ${formatDate(item.entry.date)} · ${formatNum(item.entry.odometer, 0)} km",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusBadge(item.status)
            }
            if (item.entry.description.isNotBlank()) {
                Text(item.entry.description, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatINR(item.entry.amount, 2), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1.3f)) {
                    Text("Next due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        buildString {
                            item.entry.nextDueDate?.let { append(formatDate(it)) }
                            item.entry.nextDueKm?.let {
                                if (isNotEmpty()) append(" · ")
                                append("${formatNum(it, 0)} km")
                            }
                            if (isEmpty()) append("No reminder")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        buildString {
                            item.daysLeft?.let {
                                append("${it}d")
                                if (item.kmLeft != null) append(" · ")
                            }
                            item.kmLeft?.let { append("${formatNum(it, 0)} km") }
                            if (isEmpty()) append("—")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (item.status) {
                            "Overdue" -> FleetRed
                            "Due Soon" -> FleetAmber
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
            if (item.entry.remarks.isNotBlank()) {
                Text(
                    item.entry.remarks,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaintenanceEditorDialog(
    existing: MaintenanceEntry?,
    vehicles: List<Vehicle>,
    categories: List<String>,
    initialVehicleId: String?,
    currentOdometer: (String) -> Double,
    onDismiss: () -> Unit,
    onSave: (MaintenanceEntry, Boolean) -> Unit,
) {
    val isNew = existing == null
    var vehicleId by remember { mutableStateOf(existing?.vehicleId ?: initialVehicleId ?: vehicles.firstOrNull()?.id ?: "") }
    var date by remember { mutableStateOf(existing?.date ?: LocalDate.now().toString()) }
    var odometer by remember {
        mutableStateOf(
            existing?.odometer?.let { NumberText.plain(it) }
                ?: currentOdometer(vehicleId).let { NumberText.plain(it) },
        )
    }
    var category by remember { mutableStateOf(existing?.category ?: categories.firstOrNull() ?: "Other") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var amount by remember { mutableStateOf(existing?.amount?.let { NumberText.plain(it) } ?: "") }
    var nextDate by remember { mutableStateOf(existing?.nextDueDate ?: "") }
    var hasNextDate by remember { mutableStateOf(existing?.nextDueDate != null) }
    var nextKm by remember { mutableStateOf(existing?.nextDueKm?.let { NumberText.plain(it) } ?: "") }
    var remarks by remember { mutableStateOf(existing?.remarks ?: "") }
    var categoryMenu by remember { mutableStateOf(false) }
    var vehicleMenu by remember { mutableStateOf(false) }

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
                    title = if (isNew) "Log maintenance" else "Edit maintenance",
                    subtitle = if (isNew) "Service, repair or part replacement" else formatDate(existing?.date),
                    onBack = onDismiss,
                    actions = {
                        TextButton(
                            onClick = {
                                onSave(
                                    MaintenanceEntry(
                                        id = existing?.id ?: UUID.randomUUID().toString(),
                                        date = date,
                                        vehicleId = vehicleId,
                                        odometer = NumberText.parse(odometer) ?: 0.0,
                                        category = category,
                                        description = description.trim(),
                                        amount = NumberText.parse(amount) ?: 0.0,
                                        nextDueDate = nextDate.takeIf { it.isNotBlank() },
                                        nextDueKm = NumberText.parse(nextKm),
                                        remarks = remarks.trim(),
                                        isSample = false,
                                        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                                    ),
                                    isNew,
                                )
                            },
                            enabled = vehicleId.isNotBlank() && NumberText.parse(odometer) != null,
                        ) { Text("Save") }
                    },
                )
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    FormSection("Vehicle & reading") {
                        if (isNew) {
                            ExposedDropdownMenuBox(expanded = vehicleMenu, onExpandedChange = { vehicleMenu = it }) {
                                OutlinedTextField(
                                    value = vehicles.firstOrNull { it.id == vehicleId }?.name ?: "Choose vehicle",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Vehicle") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(vehicleMenu) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                )
                                ExposedDropdownMenu(
                                    expanded = vehicleMenu,
                                    onDismissRequest = { vehicleMenu = false },
                                ) {
                                    vehicles.forEach { v ->
                                        DropdownMenuItem(
                                            text = { Text("${v.name} · ${v.type}") },
                                            onClick = {
                                                vehicleId = v.id
                                                if (odometer.isBlank()) odometer = NumberText.plain(currentOdometer(v.id))
                                                vehicleMenu = false
                                            },
                                        )
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = vehicles.firstOrNull { it.id == vehicleId }?.name ?: vehicleId,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Vehicle") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                            )
                        }
                        AppDateField("Service date", date, { date = it })
                        DecimalField("Odometer", odometer, { odometer = it }, suffix = "km")
                        Text(
                            "Prefilled with the latest logged odometer. Correct it if the service record uses a different reading.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    FormSection("Work done") {
                        ExposedDropdownMenuBox(expanded = categoryMenu, onExpandedChange = { categoryMenu = it }) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryMenu) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                            )
                            ExposedDropdownMenu(
                                expanded = categoryMenu,
                                onDismissRequest = { categoryMenu = false },
                            ) {
                                categories.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c) },
                                        onClick = {
                                            category = c
                                            categoryMenu = false
                                        },
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnIme(),
                            shape = RoundedCornerShape(14.dp),
                        )
                        DecimalField("Amount paid", amount, { amount = it }, suffix = "₹")
                        OutlinedTextField(
                            value = remarks,
                            onValueChange = { remarks = it },
                            label = { Text("Remarks (optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnIme(),
                            shape = RoundedCornerShape(14.dp),
                        )
                    }

                    FormSection("Next service reminder (optional)") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Date reminder", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Notify me when the next service date approaches",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            androidx.compose.material3.Switch(
                                checked = hasNextDate,
                                onCheckedChange = { enabled ->
                                    hasNextDate = enabled
                                    if (enabled && nextDate.isBlank()) nextDate = LocalDate.now().toString()
                                    if (!enabled) nextDate = ""
                                },
                            )
                        }
                        if (hasNextDate) {
                            AppDateField("Next due date", nextDate, { nextDate = it })
                        }
                        DecimalField("Next due odometer", nextKm, { nextKm = it }, suffix = "km")
                        if (nextKm.isNotBlank()) {
                            TextButton(onClick = { nextKm = "" }) { Text("Clear next due odometer") }
                        }
                        Text(
                            "A newer record in the same category for this vehicle retires this reminder. " +
                                "Date and odometer thresholds are configured in Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

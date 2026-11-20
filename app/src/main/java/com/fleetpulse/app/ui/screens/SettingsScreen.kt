package com.fleetpulse.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fleetpulse.app.data.db.PriceEntry
import com.fleetpulse.app.data.db.SOURCE_ELECTRICITY
import com.fleetpulse.app.data.db.SOURCE_PETROL
import com.fleetpulse.app.data.db.SettingsEntity
import com.fleetpulse.app.data.db.categoryList
import com.fleetpulse.app.data.prefs.AppPreferencesState
import com.fleetpulse.app.domain.NumberText
import com.fleetpulse.app.ui.components.AppDateField
import com.fleetpulse.app.ui.components.DecimalField
import com.fleetpulse.app.ui.components.KeyboardAwareFormDialog
import com.fleetpulse.app.ui.components.ScreenHeader
import com.fleetpulse.app.ui.components.SectionCard
import com.fleetpulse.app.ui.components.bringIntoViewOnIme
import com.fleetpulse.app.ui.components.formatDate
import com.fleetpulse.app.ui.components.formatINR
import com.fleetpulse.app.ui.components.formatNum
import com.fleetpulse.app.ui.theme.FleetAmber
import com.fleetpulse.app.ui.theme.FleetGreen
import com.fleetpulse.app.ui.theme.FleetRed
import java.time.LocalDate
import java.util.UUID

@Composable
fun SettingsScreen(
    settings: SettingsEntity,
    prices: List<PriceEntry>,
    prefs: AppPreferencesState,
    onSaveSettings: (SettingsEntity) -> Unit,
    onSavePrice: (PriceEntry) -> Unit,
    onDeletePrice: (String) -> Unit,
    onTheme: (String) -> Unit,
    onNotifications: (Boolean) -> Unit,
    onBack: () -> Unit,
    onReplayOnboarding: () -> Unit = {},
) {
    var efficiency by remember(settings) { mutableStateOf(NumberText.plain(settings.chargerEfficiency)) }
    var maxDistance by remember(settings) { mutableStateOf(NumberText.plain(settings.maxPlausibleDistance)) }
    var excludeImplausible by remember(settings) { mutableStateOf(settings.excludeImplausible) }
    var warnDays by remember(settings) { mutableStateOf(settings.warnDays.toString()) }
    var warnKm by remember(settings) { mutableStateOf(settings.warnKm.toString()) }
    var chartStart by remember(settings) { mutableStateOf(settings.chartStartMonth) }
    var evMin by remember(settings) { mutableStateOf(NumberText.plain(settings.evPlausibleMin)) }
    var evMax by remember(settings) { mutableStateOf(NumberText.plain(settings.evPlausibleMax)) }
    var petrolMin by remember(settings) { mutableStateOf(NumberText.plain(settings.petrolPlausibleMin)) }
    var petrolMax by remember(settings) { mutableStateOf(NumberText.plain(settings.petrolPlausibleMax)) }
    var categories by remember(settings) { mutableStateOf(settings.categoryList()) }
    var newCategory by remember { mutableStateOf("") }
    var priceEditor by remember { mutableStateOf<PriceEntry?>(null) }
    var addingPrice by remember { mutableStateOf(false) }
    var deletePriceTarget by remember { mutableStateOf<PriceEntry?>(null) }
    var chartStartError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onNotifications(true)
        } else {
            onNotifications(false)
        }
    }

    fun persist() {
        val validMonth = Regex("^\\d{4}-(0[1-9]|1[0-2])$").matches(chartStart.trim())
        if (!validMonth) {
            chartStartError = "Use YYYY-MM, for example 2026-10"
            return
        }
        chartStartError = null
        onSaveSettings(
            settings.copy(
                chargerEfficiency = efficiency.toDoubleOrNull()?.takeIf { it > 0 } ?: settings.chargerEfficiency,
                maxPlausibleDistance = maxDistance.toDoubleOrNull()?.takeIf { it > 0 } ?: settings.maxPlausibleDistance,
                excludeImplausible = excludeImplausible,
                warnDays = warnDays.toIntOrNull()?.takeIf { it >= 0 } ?: settings.warnDays,
                warnKm = warnKm.toIntOrNull()?.takeIf { it >= 0 } ?: settings.warnKm,
                chartStartMonth = chartStart.trim(),
                evPlausibleMin = evMin.toDoubleOrNull()?.takeIf { it > 0 } ?: settings.evPlausibleMin,
                evPlausibleMax = evMax.toDoubleOrNull()?.takeIf { it > 0 } ?: settings.evPlausibleMax,
                petrolPlausibleMin = petrolMin.toDoubleOrNull()?.takeIf { it > 0 } ?: settings.petrolPlausibleMin,
                petrolPlausibleMax = petrolMax.toDoubleOrNull()?.takeIf { it > 0 } ?: settings.petrolPlausibleMax,
                maintenanceCategories = categories.joinToString("\n"),
            ),
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Settings",
                subtitle = "Calculation and validation parameters",
                onBack = onBack,
            )
        }

        item {
            SectionCard(title = "Charging & validation", subtitle = "Affect every calculated efficiency") {
                DecimalField("Charger efficiency", efficiency, { efficiency = it }, suffix = "%")
                DecimalField("Max plausible km between readings", maxDistance, { maxDistance = it }, suffix = "km")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Exclude implausible efficiency", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Flag and exclude full-to-full results outside the plausible band",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = excludeImplausible, onCheckedChange = { excludeImplausible = it })
                }
                Text(
                    "Charger efficiency converts wall energy to battery energy for voltage-based estimates: " +
                        "battery % change × capacity ÷ efficiency.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard(title = "Plausible efficiency band", subtitle = "Only affects CHECK flags and averages") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DecimalField("EV min", evMin, { evMin = it }, suffix = "km/kWh", modifier = Modifier.weight(1f))
                    DecimalField("EV max", evMax, { evMax = it }, suffix = "km/kWh", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DecimalField("Petrol min", petrolMin, { petrolMin = it }, suffix = "km/L", modifier = Modifier.weight(1f))
                    DecimalField("Petrol max", petrolMax, { petrolMax = it }, suffix = "km/L", modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            SectionCard(title = "Service reminders", subtitle = "Km and day thresholds") {
                DecimalField("Warn days ahead", warnDays, { warnDays = it }, suffix = "days")
                DecimalField("Warn km ahead", warnKm, { warnKm = it }, suffix = "km")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Service notifications", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Daily local check while the app is installed. Km reminders use your latest logged odometer.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = prefs.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                                PackageManager.PERMISSION_GRANTED
                            ) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onNotifications(enabled)
                            }
                        },
                    )
                }
                if (prefs.notificationsEnabled) {
                    Text(
                        "Notifications are on. Overdue and due-soon reminders are re-evaluated every day and also whenever the app opens.",
                        style = MaterialTheme.typography.labelSmall,
                        color = FleetGreen,
                    )
                }
            }
        }

        item {
            SectionCard(title = "Charts", subtitle = "12-month window anchor") {
                OutlinedTextField(
                    value = chartStart,
                    onValueChange = { chartStart = it },
                    label = { Text("Chart start month (YYYY-MM)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnIme(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    isError = chartStartError != null,
                    supportingText = chartStartError?.let { { Text(it) } },
                )
                Text(
                    "Charts display twelve consecutive months starting here, matching the reference dashboard.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard(title = "Maintenance categories", subtitle = "Used by the maintenance form") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                if (categories.size > 1) categories = categories - category
                            }) {
                                Icon(Icons.Default.Delete, "Remove $category", tint = FleetRed)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Add category") },
                        modifier = Modifier
                            .weight(1f)
                            .bringIntoViewOnIme(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val c = newCategory.trim()
                            if (c.isNotEmpty() && c !in categories) {
                                categories = categories + c
                                newCategory = ""
                            }
                        },
                    ) { Icon(Icons.Default.Add, "Add category") }
                }
            }
        }

        item {
            SectionCard(title = "Appearance") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                        FilterChip(
                            selected = prefs.themeMode == key,
                            onClick = { onTheme(key) },
                            label = { Text(label) },
                        )
                    }
                }
            }

            SectionCard(title = "Help & onboarding", subtitle = "Replay the getting-started tutorial") {
                Text(
                    "FleetPulse includes a short tutorial for adding vehicles, recording usage, reading mileage " +
                        "and costs, tracking maintenance, and keeping backups.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onReplayOnboarding,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Replay getting started")
                }
            }
        }

        item {
            SectionCard(
                title = "Historical energy tariffs",
                subtitle = "Applied by the transaction date, never retroactively changed",
                trailing = {
                    TextButton(onClick = { addingPrice = true }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Text("Add")
                    }
                },
            ) {
                if (prices.isEmpty()) {
                    Text(
                        "No tariffs configured. Add at least one price per energy source, otherwise costs cannot be calculated.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FleetAmber,
                    )
                }
                prices.sortedByDescending { it.effectiveFrom }.forEach { price ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${price.energySource} · ${formatINR(price.price, 2)} per " +
                                    if (price.energySource == SOURCE_ELECTRICITY) "kWh" else "litre",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Effective ${formatDate(price.effectiveFrom)}" +
                                    if (price.note.isNotBlank()) " · ${price.note}" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (price.isSample) {
                                Text("[SAMPLE]", style = MaterialTheme.typography.labelSmall, color = FleetAmber)
                            }
                        }
                        IconButton(onClick = { priceEditor = price }) {
                            Icon(Icons.Default.Edit, "Edit tariff")
                        }
                        IconButton(onClick = { deletePriceTarget = price }) {
                            Icon(Icons.Default.Delete, "Delete tariff", tint = FleetRed)
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { persist() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Save settings") }
        }
    }

    if (addingPrice || priceEditor != null) {
        PriceDialog(
            existing = priceEditor,
            onDismiss = {
                addingPrice = false
                priceEditor = null
            },
            onSave = {
                onSavePrice(it)
                addingPrice = false
                priceEditor = null
            },
        )
    }

    deletePriceTarget?.let { price ->
        AlertDialog(
            onDismissRequest = { deletePriceTarget = null },
            title = { Text("Delete tariff?") },
            text = {
                Text(
                    "Deleting the ${price.energySource} price effective ${formatDate(price.effectiveFrom)} can change the cost " +
                        "of historical entries that used it. Entries keep their own stored unit price when set.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePrice(price.id)
                    deletePriceTarget = null
                }) { Text("Delete", color = FleetRed) }
            },
            dismissButton = { TextButton(onClick = { deletePriceTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PriceDialog(existing: PriceEntry?, onDismiss: () -> Unit, onSave: (PriceEntry) -> Unit) {
    var source by remember { mutableStateOf(existing?.energySource ?: SOURCE_ELECTRICITY) }
    var from by remember { mutableStateOf(existing?.effectiveFrom ?: LocalDate.now().toString()) }
    var price by remember { mutableStateOf(existing?.price?.let { NumberText.plain(it) } ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }

    KeyboardAwareFormDialog(
        onDismissRequest = onDismiss,
        title = if (existing == null) "Add tariff" else "Edit tariff",
        confirmLabel = "Save",
        onConfirm = {
            onSave(
                PriceEntry(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    energySource = source,
                    effectiveFrom = from,
                    price = NumberText.parse(price) ?: 0.0,
                    note = note.trim(),
                    isSample = false,
                ),
            )
        },
        confirmEnabled = NumberText.parse(price)?.let { it > 0 } == true,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(SOURCE_ELECTRICITY, SOURCE_PETROL).forEach { option ->
                FilterChip(
                    selected = source == option,
                    onClick = { source = option },
                    label = { Text(option) },
                )
            }
        }
        AppDateField("Effective from", from, { from = it })
        DecimalField(
            "Price per ${if (source == SOURCE_ELECTRICITY) "kWh" else "litre"}",
            price,
            { price = it },
            suffix = "₹",
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note (optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewOnIme(),
            shape = RoundedCornerShape(14.dp),
        )
        Text(
            "The latest tariff effective on or before a usage date is used. Historical entries never use newer prices.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

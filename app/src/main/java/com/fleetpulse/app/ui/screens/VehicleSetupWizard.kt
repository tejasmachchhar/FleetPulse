package com.fleetpulse.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Motorcycle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import com.fleetpulse.app.data.db.SOURCE_ELECTRICITY
import com.fleetpulse.app.data.db.SOURCE_PETROL
import com.fleetpulse.app.data.db.Vehicle
import com.fleetpulse.app.data.repo.VehicleReferenceCatalogEntry
import com.fleetpulse.app.data.repo.VehicleReferenceVehicleRecord
import com.fleetpulse.app.ui.MainViewModel
import com.fleetpulse.app.ui.components.SectionCard
import com.fleetpulse.app.ui.components.ScreenHeader
import com.fleetpulse.app.ui.theme.FleetBlue
import com.fleetpulse.app.ui.theme.FleetGreen
import org.json.JSONObject
import java.util.Locale

private enum class SetupStep(val index: Int) {
    Category(0), Subtype(1), Propulsion(2), Manufacturer(3), Model(4), Review(5), Personalize(6), Complete(7)
}

private data class SpecEditField(
    val path: String,
    val label: String,
    val value: String,
)

private val quickCategories = listOf(
    VehicleCategoryOption("scooter", "Scooter", "scooter", "distance_km", listOf("petrol_scooter", "electric_scooter")),
    VehicleCategoryOption("motorcycle", "Motorcycle", "motorcycles", "distance_km", listOf("commuter_motorcycle", "sports_motorcycle")),
    VehicleCategoryOption("car", "Car", "passenger-vehicles", "distance_km", listOf("hatchback", "sedan", "suv")),
    VehicleCategoryOption("auto_rickshaw", "Auto-rickshaw", "three-wheelers", "distance_km", listOf("passenger_auto_rickshaw", "cargo_auto_rickshaw")),
    VehicleCategoryOption("tractor", "Tractor", "agricultural", "operating_hours", listOf("agricultural_tractor")),
)

@Composable
fun VehicleSetupWizard(
    vm: MainViewModel,
    onComplete: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var showAllCategories by rememberSaveable { mutableStateOf(false) }
    var categories by remember { mutableStateOf(quickCategories) }
    var category by rememberSaveable { mutableStateOf<VehicleCategoryOption?>(null) }
    var categoryEntries by remember { mutableStateOf(emptyList<VehicleReferenceCatalogEntry>()) }
    var subcategory by rememberSaveable { mutableStateOf("") }
    var propulsion by rememberSaveable { mutableStateOf("") }
    var propulsionEntries by remember { mutableStateOf(emptyList<VehicleReferenceCatalogEntry>()) }
    var manufacturer by rememberSaveable { mutableStateOf<VehicleReferenceCatalogEntry?>(null) }
    var manufacturers by remember { mutableStateOf(emptyList<VehicleReferenceCatalogEntry>()) }
    var model by rememberSaveable { mutableStateOf<VehicleReferenceCatalogEntry?>(null) }
    var modelEntries by remember { mutableStateOf(emptyList<VehicleReferenceCatalogEntry>()) }
    var modelQuery by rememberSaveable { mutableStateOf("") }
    var manufacturerQuery by rememberSaveable { mutableStateOf("") }
    var referenceRecord by remember { mutableStateOf<VehicleReferenceVehicleRecord?>(null) }
    var specEdits by rememberSaveable { mutableStateOf<Map<String, String>>(emptyMap()) }
    var editingSpecs by rememberSaveable { mutableStateOf(false) }
    var specDialogField by rememberSaveable { mutableStateOf<SpecEditField?>(null) }
    var nickname by rememberSaveable { mutableStateOf("") }
    var regNo by rememberSaveable { mutableStateOf("") }
    var purchaseDate by rememberSaveable { mutableStateOf("") }
    var odometer by rememberSaveable { mutableStateOf("") }
    var operatingHours by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            categories = context.loadVehicleTaxonomy()
        } catch (_: Exception) {
            // Keep quick categories if taxonomy assets are unavailable.
        }
        try {
            manufacturers = vm.vehicleReference.loadManufacturers()
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(category) {
        val currentCategory = category
        if (currentCategory == null) {
            categoryEntries = emptyList()
            return@LaunchedEffect
        }
        try {
            categoryEntries = vm.vehicleReference.loadCatalogEntries(currentCategory.slug)
            propulsion = ""
            propulsionEntries = if (categoryEntries.isNotEmpty()) {
                categoryEntries.distinctBy { it.propulsion }.filter { it.propulsion.isNotBlank() }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            categoryEntries = emptyList()
        }
    }

    LaunchedEffect(model) {
        referenceRecord = null
        specEdits = emptyMap()
        val currentModel = model
        if (currentModel != null) {
            try {
                referenceRecord = vm.vehicleReference.loadVehicleRecord(currentModel.recordPath)
            } catch (_: Exception) {
                referenceRecord = null
            }
        }
    }

    val currentCategory = category
    val currentManufacturer = manufacturer
    val currentModel = model
    val filteredModels = if (currentCategory != null && currentManufacturer != null) {
        categoryEntries.filter {
            (currentManufacturer.brand.isBlank() || it.brand.equals(currentManufacturer.brand, ignoreCase = true)) &&
                (modelQuery.isBlank() ||
                    it.model.contains(modelQuery, ignoreCase = true) ||
                    it.variant.contains(modelQuery, ignoreCase = true) ||
                    it.aliases.any { alias -> alias.contains(modelQuery, ignoreCase = true) })
        }
    } else {
        emptyList()
    }
    val manufacturerMatches = manufacturers.filter {
        manufacturerQuery.isBlank() ||
            it.manufacturer.contains(manufacturerQuery, ignoreCase = true) ||
            it.brand.contains(manufacturerQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ScreenHeader(
            title = "Add your vehicle",
            subtitle = "Type: ${category?.name ?: "Not selected"} · Progress: ${SetupStep.values().getOrNull(step)?.name ?: "Complete"}",
            onBack = {
                if (step > 0) step -= 1
                else onComplete()
            },
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionCard(title = "1. Category") { CategoryStep(categories, showAllCategories, { showAllCategories = it }, { category = it; step += 1 }) } }
            item { SectionCard(title = "2. Subtype") { SubtypeStep(category, categoryEntries, subcategory, { subcategory = it; step += 1 }) } }
            item { SectionCard(title = "3. Propulsion") { PropulsionStep(propulsionEntries, propulsion, { propulsion = it; step += 1 }) } }
            item { SectionCard(title = "4. Manufacturer") { ManufacturerStep(manufacturerMatches, manufacturerQuery, { manufacturerQuery = it }, { manufacturer = it; step += 1 }) } }
            item { SectionCard(title = "5. Model") { ModelStep(filteredModels, modelQuery, { modelQuery = it }, { model = it; step += 1 }) } }
            item { SectionCard(title = "6. Review") { ReviewStep(category, propulsion, manufacturer, model, referenceRecord, specEdits, editingSpecs, { editingSpecs = it }) } }
            item { SectionCard(title = "7. Personalise") { PersonalizeStep(nickname, regNo, purchaseDate, odometer, operatingHours, { nickname = it }, { regNo = it }, { purchaseDate = it }, { odometer = it }, { operatingHours = it }) } }
            item { Text("Make sure each entry has the correct values, then tap Continue.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (step > SetupStep.Category.index) OutlinedButton(onClick = { step -= 1; error = null }) { Text("Back") }
            Button(onClick = { step += 1 }, enabled = (step == SetupStep.Category.index && category != null) ||
                (step == SetupStep.Subtype.index && subcategory.isNotBlank()) ||
                (step == SetupStep.Propulsion.index && propulsion.isNotBlank()) ||
                (step == SetupStep.Manufacturer.index && manufacturer != null) ||
                (step == SetupStep.Model.index && model != null) ||
                (step == SetupStep.Review.index) || step == SetupStep.Personalize.index, modifier = Modifier.weight(1f)) {
                Text(if (step == SetupStep.Personalize.index) "Create vehicle" else "Continue")
            }
            if (step == SetupStep.Personalize.index) {
                IconButton(onClick = { creating = true }) { Icon(Icons.Default.Bolt, "Create") }
            }
        }

        LaunchedEffect(creating) {
            if (creating) {
                val createError = createVehicle(
                    vm = vm,
                    category = category,
                    subcategory = subcategory,
                    propulsion = propulsion,
                    manufacturer = manufacturer,
                    model = model,
                    referenceRecord = referenceRecord,
                    specEdits = specEdits,
                    nickname = nickname,
                    regNo = regNo,
                    purchaseDate = purchaseDate,
                    odometer = odometer,
                    operatingHours = operatingHours,
                )
                creating = false
                if (createError == null) onComplete() else error = createError
            }
        }

        error?.let { message -> Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }

    specDialogField?.let { field ->
        AlertDialog(
            onDismissRequest = { specDialogField = null },
            title = { Text("Edit ${field.label}") },
            text = { OutlinedTextField(field.value, { specEdits = specEdits + (field.path to it) }, label = { Text(field.label) }) },
            confirmButton = { TextButton(onClick = { specDialogField = null }) { Text("Done") } },
        )
    }
}

@Composable
private fun CategoryStep(
    categories: List<VehicleCategoryOption>,
    showAll: Boolean,
    onShowAll: (Boolean) -> Unit,
    onSelect: (VehicleCategoryOption) -> Unit,
) {
    val config = LocalConfiguration.current
    val quick = categories.takeIf { it.isNotEmpty() } ?: quickCategories
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (quick.size < 6 && !showAll) {
            OutlinedButton(onClick = { onShowAll(true) }, modifier = Modifier.fillMaxWidth()) { Text("See all vehicle types") }
        }
        quick.forEach { category ->
            SelectableCard(title = category.name, icon = iconForCategory(category), onClick = { onSelect(category) })
        }
        if (showAll) {
            (quickCategories + categories.filterNot { it in quickCategories }).forEach { category ->
                SelectableCard(title = category.name, icon = iconForCategory(category), onClick = { onSelect(category) })
            }
        }
    }
}

@Composable
private fun SubtypeStep(
    category: VehicleCategoryOption?,
    entries: List<VehicleReferenceCatalogEntry>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    if (category == null) {
        Text("Choose a category first.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        return
    }
    val names = if (entries.isNotEmpty()) entries.distinctBy { it.subcategory }.map { VehicleSubcategoryOption(it.subcategory, it.subcategory.replace('_', ' ').replaceFirstChar { it.uppercase() }) } else category.toSubcategories(entries.orEmpty()).let { listOf(VehicleSubcategoryOption("other_" + category.id, "Other ${category.name}")) }
    names.forEach { sub ->
        SelectableChip(sub.name, selected == sub.id, { onSelect(sub.id) })
    }
}

@Composable
private fun PropulsionStep(entries: List<VehicleReferenceCatalogEntry>, selected: String, onSelect: (String) -> Unit) {
    val options = if (entries.isNotEmpty()) entries.distinctBy { it.propulsion }.filter { it.propulsion.isNotBlank() } else listOf("petrol", "diesel", "electric", "other")
    if (selected.isNotBlank()) SelectableChip("Selected: ${selected.orEmpty().replace('_', ' ')}", true, {})
    options.forEach { prop ->
        val propId = if (prop is VehicleReferenceCatalogEntry) prop.propulsion else prop.toString()
        val propName = propId.orEmpty().replace('_', ' ').replaceFirstChar { it.uppercase() }
        SelectableChip(propName, selected == propId, { onSelect(propId) })
    }
}

@Composable
private fun ManufacturerStep(
    entries: List<VehicleReferenceCatalogEntry>,
    query: String,
    onQuery: (String) -> Unit,
    onSelect: (VehicleReferenceCatalogEntry) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        label = { Text("Search manufacturer") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    )
    if (entries.isEmpty()) {
        Text("No manufacturers found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else entries.forEach { manufacturer ->
        SelectableRow(title = manufacturer.manufacturer, subtitle = manufacturer.brand, selected = false, onClick = { onSelect(manufacturer) })
    }
}

@Composable
private fun ModelStep(
    entries: List<VehicleReferenceCatalogEntry>,
    query: String,
    onQuery: (String) -> Unit,
    onSelect: (VehicleReferenceCatalogEntry) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        label = { Text("Search models") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    )
    if (entries.isEmpty()) Text("Search a manufacturer and enter a model name.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    else entries.take(30).forEach { model ->
        SelectableRow(
            title = model.variant.ifBlank { model.model },
            subtitle = listOfNotNull(model.model, model.brand, model.year.takeIf { it != 0 }?.toString()).joinToString(" · "),
            selected = false,
            onClick = { onSelect(model) },
        )
    }
}

@Composable
private fun ReviewStep(
    category: VehicleCategoryOption?,
    propulsion: String,
    manufacturer: VehicleReferenceCatalogEntry?,
    model: VehicleReferenceCatalogEntry?,
    record: VehicleReferenceVehicleRecord?,
    specEdits: Map<String, String>,
    editing: Boolean,
    onEditSpecs: (Boolean) -> Unit,
) {
    Text(record?.officialProductName ?: model?.model ?: "No model selected", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Text("${manufacturer?.manufacturer ?: ""} · ${category?.name ?: ""} · ${propulsion.replace('_', ' ').replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    HorizontalDivider()
    TextSpecRow("Type", category?.name ?: "Not set")
    TextSpecRow("Subtype", model?.subcategory?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "Not set")
    TextSpecRow("Propulsion", propulsion.replace('_', ' ').replaceFirstChar { it.uppercase() })
    record?.let { r ->
        TextSpecRow("Fuel type", r.fuelType.replace('_', ' ').replaceFirstChar { it.uppercase() })
        TextSpecRow("Displacement", r.engineDisplacementCc?.let { " ${it.toInt()} cc" } ?: "Not specified")
        TextSpecRow("Power", r.powerKw?.let { " ${it.toInt()} kW" } ?: "Not specified")
        TextSpecRow("Torque", r.torqueNm?.let { " ${it.toInt()} Nm" } ?: "Not specified")
        TextSpecRow("Tank", r.fuelTankLiters?.let { " ${it.toInt()} L" } ?: "Not specified")
        TextSpecRow("Battery", r.batteryCapacityKwh?.let { " ${it.toInt()} kWh" } ?: "Not specified")
        TextSpecRow("Seating", r.seatingCapacity?.toString() ?: "Not specified")
        TextSpecRow("Wheel count", r.wheelCount?.toString() ?: "Not specified")
    }
    Text(text = "Verification", style = MaterialTheme.typography.bodySmall)
    Text(text = "Source", style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable { })
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = { onEditSpecs(!editing) }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (editing) "Done editing" else "Edit specifications")
    }
    if (editing) specEdits.forEach { (path, value) ->
        Text(path.replace('.', ' ').replace('_', ' ').replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium)
        Text("Value: $value", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PersonalizeStep(
    nickname: String,
    regNo: String,
    purchaseDate: String,
    odometer: String,
    operatingHours: String,
    onNickname: (String) -> Unit,
    onRegNo: (String) -> Unit,
    onPurchaseDate: (String) -> Unit,
    onOdometer: (String) -> Unit,
    onOperatingHours: (String) -> Unit,
) {
    OutlinedTextField(value = nickname, onValueChange = onNickname, label = { Text("Nickname (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(value = regNo, onValueChange = onRegNo, label = { Text("Registration number (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(value = purchaseDate, onValueChange = onPurchaseDate, label = { Text("Purchase date (YYYY-MM-DD, optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(value = odometer, onValueChange = onOdometer, label = { Text("Current odometer") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(value = operatingHours, onValueChange = onOperatingHours, label = { Text("Current operating hours (for agricultural vehicles)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}

private fun iconForCategory(category: VehicleCategoryOption): androidx.compose.ui.graphics.vector.ImageVector = when (category.id) {
    "scooter", "motorcycle", "auto_rickshaw", "three_wheeler", "two_wheeler" -> Icons.AutoMirrored.Filled.DirectionsBike
    "car", "passenger_vehicle" -> Icons.Default.Settings
    "tractor", "agricultural", "construction" -> Icons.Default.Build
    "commercial_vehicle", "public_transport", "other" -> Icons.Default.LocalShipping
    else -> Icons.Default.Settings
}

@Composable
private fun SelectableCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun SelectableRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            subtitle.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun TextSpecRow(label: String, value: String?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.orEmpty(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun parseDoubleOrNull(value: String): Double? = value.trim().toDoubleOrNull()
private fun parseIntOrNull(value: String): Int? = value.trim().toIntOrNull()

private suspend fun createVehicle(
    vm: MainViewModel,
    category: VehicleCategoryOption?,
    subcategory: String,
    propulsion: String,
    manufacturer: VehicleReferenceCatalogEntry?,
    model: VehicleReferenceCatalogEntry?,
    referenceRecord: VehicleReferenceVehicleRecord?,
    specEdits: Map<String, String>,
    nickname: String,
    regNo: String,
    purchaseDate: String,
    odometer: String,
    operatingHours: String,
): String? {
    if (category == null || model == null) return "Choose a model before continuing."
    if (nickname.isBlank()) return "Enter a nickname before saving."

    val record = referenceRecord
    val categoryId = category.id
    val trackingBasis = category.trackingBasis
    val isElectric = model.fuelType.equals("electric", ignoreCase = true) || propulsion.equals("bev", ignoreCase = true)
    val energySource = if (isElectric) SOURCE_ELECTRICITY else SOURCE_PETROL

    val overrides = JSONObject()
    specEdits.forEach { (path, value) -> if (value.isNotBlank()) overrides.put(path, value) }

    val vehicle = Vehicle(
        id = "Vh${System.currentTimeMillis()}",
        name = nickname.ifBlank { model.model },
        type = category.name,
        energySource = energySource,
        category = categoryId,
        subcategory = subcategory,
        trackingBasis = trackingBasis,
        referenceRecordId = model.id,
        referenceDatabaseVersion = vm.vehicleReference.loadManifest().databaseVersion,
        referenceRawJson = record?.rawJson,
        referenceOverridesJson = overrides.toString(),
        regNo = regNo,
        purchaseDate = purchaseDate,
        batteryCapacity = record?.batteryCapacityKwh ?: parseDoubleOrNull(specEdits["energy_storage.battery_capacity_kwh"] ?: ""),
        tankCapacity = record?.fuelTankLiters ?: parseDoubleOrNull(specEdits["energy_storage.fuel_tank_liters"] ?: ""),
        powerKw = record?.powerKw ?: parseDoubleOrNull(specEdits["powertrain.power"] ?: ""),
        engineDisplacementCc = record?.engineDisplacementCc ?: parseDoubleOrNull(specEdits["powertrain.engine_displacement_cc"] ?: ""),
        torqueNm = record?.torqueNm ?: parseDoubleOrNull(specEdits["powertrain.torque"] ?: ""),
        engineOilGrade = record?.engineOilGrade ?: specEdits["maintenance.engine_oil_grade"],
        initialOdometer = odometer.toDoubleOrNull() ?: 0.0,
        tankCapacityLiters = record?.fuelTankLiters ?: parseDoubleOrNull(specEdits["energy_storage.fuel_tank_liters"] ?: ""),
        cargoCapacityLiters = record?.cargoCapacityLiters ?: parseDoubleOrNull(specEdits["dimensions.cargo_capacity_liters"] ?: ""),
        seatingCapacity = record?.seatingCapacity ?: parseIntOrNull(specEdits["dimensions.seating_capacity"] ?: ""),
        wheelCount = record?.wheelCount ?: parseIntOrNull(specEdits["dimensions.wheel_count"] ?: ""),
        axleConfiguration = record?.axleConfiguration ?: specEdits["dimensions.axle_configuration"],
    )
    if (!vm.saveVehicle(vehicle, isNew = true)) return "Could not save vehicle."
    return null
}

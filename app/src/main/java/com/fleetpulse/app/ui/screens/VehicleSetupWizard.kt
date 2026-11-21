package com.fleetpulse.app.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
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
import com.fleetpulse.app.data.db.Vehicle
import com.fleetpulse.app.data.repo.VehicleReferenceCatalogEntry
import com.fleetpulse.app.data.repo.VehicleReferenceManufacturer
import com.fleetpulse.app.data.repo.VehicleReferenceVehicleRecord
import com.fleetpulse.app.ui.MainViewModel
import com.fleetpulse.app.ui.components.SectionCard
import com.fleetpulse.app.ui.components.ScreenHeader
import org.json.JSONObject

private enum class SetupStep(val index: Int) {
    Category(0), Subtype(1), Propulsion(2), Manufacturer(3), Model(4), Review(5), Personalize(6), Complete(7)
}

private val supportedPropulsions = listOf("bev", "ice")

@Composable
fun VehicleSetupWizard(
    vm: MainViewModel,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(SetupStep.Category.index) }
    var categoryId by rememberSaveable { mutableStateOf("") }
    var subcategory by rememberSaveable { mutableStateOf("") }
    var propulsion by rememberSaveable { mutableStateOf("") }
    var manufacturerId by rememberSaveable { mutableStateOf("") }
    var modelId by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var regNo by rememberSaveable { mutableStateOf("") }
    var purchaseDate by rememberSaveable { mutableStateOf("") }
    var odometer by rememberSaveable { mutableStateOf("") }
    var operatingHours by rememberSaveable { mutableStateOf("") }
    var manual by rememberSaveable { mutableStateOf(false) }
    var showSpecEditor by rememberSaveable { mutableStateOf(false) }
    var specOverrides by rememberSaveable { mutableStateOf<Map<String, String>>(emptyMap()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var categories by remember { mutableStateOf(context.loadVehicleTaxonomy()) }
    var categoryEntries by remember { mutableStateOf(emptyList<VehicleReferenceCatalogEntry>()) }
    var manufacturers by remember { mutableStateOf(emptyList<VehicleReferenceManufacturer>()) }
    var referenceRecord by remember { mutableStateOf<VehicleReferenceVehicleRecord?>(null) }

    LaunchedEffect(categoryId) {
        val category = categories.firstOrNull { it.id == categoryId }
        if (category == null) {
            categoryEntries = emptyList()
            manufacturers = emptyList()
            return@LaunchedEffect
        }
        val entries = try { vm.vehicleReference.loadCatalogEntries(category.slug) } catch (_: Exception) { emptyList() }
        categoryEntries = entries
        manufacturers = try {
            vm.vehicleReference.loadManufacturers().filter { category.id in it.categories }
        } catch (_: Exception) { emptyList() }
    }

    LaunchedEffect(modelId) {
        referenceRecord = null
        specOverrides = emptyMap()
        if (modelId.isNotBlank()) {
            try {
                referenceRecord = vm.vehicleReference.loadVehicleRecord(modelId)
            } catch (_: Exception) { referenceRecord = null }
        }
    }

    val category = categories.firstOrNull { it.id == categoryId }
    val manufacturer = manufacturers.firstOrNull { it.id == manufacturerId }
    val model = categoryEntries.firstOrNull { it.id == modelId }
    val trackedStep = clamp(step)

    val filteredManufacturers = if (category != null) {
        manufacturers.filter { category.id in it.categories }.filter {
            manufacturerId.isBlank() ||
                it.id.equals(manufacturerId, ignoreCase = true) ||
                it.brand.contains(manufacturerId, ignoreCase = true) ||
                it.name.contains(manufacturerId, ignoreCase = true)
        }
    } else {
        manufacturers
    }
    val filteredModels = if (manufacturer != null) {
        categoryEntries.filter { it.brand.equals(manufacturer.brand, ignoreCase = true) }
    } else {
        categoryEntries
    }

    fun move(delta: Int) {
        step += delta
        if (step < SetupStep.Category.index) step = SetupStep.Category.index
        if (step > SetupStep.Personalize.index) step = SetupStep.Personalize.index
        error = null
    }

    fun canContinue(stepIndex: Int = trackedStep): Boolean = when (stepIndex) {
        SetupStep.Category.index -> categoryId.isNotBlank() || manual
        SetupStep.Subtype.index -> categoryEntries.isNotEmpty() && subcategory.isNotBlank()
        SetupStep.Propulsion.index -> propulsion.isNotBlank()
        SetupStep.Manufacturer.index -> manufacturerId.isNotBlank()
        SetupStep.Model.index -> modelId.isNotBlank()
        SetupStep.Review.index -> true
        SetupStep.Personalize.index -> nickname.isNotBlank()
        else -> false
    }

    fun create() {
        if (isSaving) return
        isSaving = true
        error = null
        vm.setCreating(true)
        vm.startCreateVehicle(
            block = {
                val manifest = vm.vehicleReference.loadManifest()
                val record = referenceRecord
                val selectedCategory = category
                if (!manual) {
                    if (selectedCategory == null) return@startCreateVehicle "Choose a category before continuing."
                    if (record == null) return@startCreateVehicle "Choose a model before continuing."
                    if (nickname.isBlank()) return@startCreateVehicle "Enter a nickname before saving."
                    val isElectric = record.propulsion == "bev" || record.fuelType == "electric"
                    val fuelType = when {
                        isElectric -> SOURCE_ELECTRICITY
                        record.fuelType == SOURCE_DIESEL || record.fuelType == SOURCE_CNG ||
                            record.fuelType == SOURCE_LPG || record.fuelType == SOURCE_HYBRID -> record.fuelType
                        else -> SOURCE_PETROL
                    }
                    val batteryCapacity = if (isElectric) {
                        specOverrides["energy_storage.battery_capacity_kwh"]?.toDoubleOrNull()
                            ?: record.batteryCapacityKwh
                    } else null
                    if (isElectric && (batteryCapacity == null || batteryCapacity <= 0)) {
                        return@startCreateVehicle "This electric vehicle needs a battery capacity."
                    }
                    val overrides = JSONObject().apply { specOverrides.forEach { (key, value) -> put(key, value) } }
                    val vehicle = Vehicle(
                        id = vm.nextVehicleId(),
                        name = nickname.ifBlank { record.officialProductName },
                        type = selectedCategory.name,
                        energySource = fuelType,
                        category = selectedCategory.id,
                        subcategory = record.subcategory.ifBlank { subcategory },
                        trackingBasis = selectedCategory.trackingBasis,
                        referenceRecordId = record.id,
                        referenceDatabaseVersion = manifest?.databaseVersion,
                        referenceRawJson = record.rawJson,
                        referenceOverridesJson = overrides.toString(),
                        regNo = regNo,
                        purchaseDate = purchaseDate,
                        initialOdometer = odometer.toDoubleOrNull() ?: 0.0,
                        batteryCapacity = batteryCapacity,
                        tankCapacity = record.fuelTankLiters,
                        tankCapacityLiters = record.fuelTankLiters,
                        powerKw = record.powerKw,
                        engineDisplacementCc = record.engineDisplacementCc,
                        torqueNm = record.torqueNm,
                        engineOilGrade = record.engineOilGrade,
                        cargoCapacityLiters = record.cargoCapacityLiters,
                        seatingCapacity = record.seatingCapacity,
                        wheelCount = record.wheelCount,
                        axleConfiguration = record.axleConfiguration,
                    )
                    if (vm.saveVehicle(vehicle, isNew = true)) null else "Could not save vehicle."
                } else {
                    val selectedCategory = category
                    if (selectedCategory == null) return@startCreateVehicle "Choose a category before continuing."
                    if (nickname.isBlank()) return@startCreateVehicle "Enter a nickname before saving."
                    val isElectric = propulsion == SOURCE_ELECTRICITY || propulsion == "Electric" || propulsion == "bev"
                    val fuelType = if (isElectric) SOURCE_ELECTRICITY else propulsion.replaceFirstChar { it.uppercase() }
                    val vehicle = Vehicle(
                        id = vm.nextVehicleId(),
                        name = nickname,
                        type = selectedCategory.name,
                        energySource = fuelType,
                        category = selectedCategory.id,
                        subcategory = subcategory,
                        trackingBasis = selectedCategory.trackingBasis,
                        regNo = regNo,
                        purchaseDate = purchaseDate,
                        initialOdometer = odometer.toDoubleOrNull() ?: 0.0,
                        tankCapacityLiters = specOverrides["energy_storage.fuel_tank_liters"]?.toDoubleOrNull(),
                        cargoCapacityLiters = specOverrides["dimensions.cargo_capacity_liters"]?.toDoubleOrNull(),
                        powerKw = specOverrides["powertrain.power"]?.toDoubleOrNull(),
                        engineDisplacementCc = specOverrides["powertrain.engine_displacement_cc"]?.toDoubleOrNull(),
                        torqueNm = specOverrides["powertrain.torque"]?.toDoubleOrNull(),
                        engineOilGrade = specOverrides["maintenance.engine_oil_grade"],
                    )
                    if (vm.saveVehicle(vehicle, isNew = true)) null else "Could not save vehicle."
                }
            },
            onComplete = {
                isSaving = false
                onComplete()
            },
            onError = { message ->
                isSaving = false
                error = message
            },
        )
    }

    Dialog(
        onDismissRequest = { if (!isSaving) { vm.setCreating(false); onComplete() } },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BackHandler(onBack = { if (!isSaving) { vm.setCreating(false); onComplete() } })
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(20.dp)) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ScreenHeader(
                        title = "Add your vehicle",
                        subtitle = "Step ${trackedStep + 1} of ${SetupStep.entries.size}",
                        onBack = { if (trackedStep > SetupStep.Category.index) move(-1) else if (!isSaving) { vm.setCreating(false); onComplete() } },
                    )
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScrollLocal(),
                    ) {
                        when (trackedStep) {
                            SetupStep.Category.index -> {
                                if (manual) {
                                    ManualEntryStep(
                                        nickname = nickname,
                                        regNo = regNo,
                                        purchaseDate = purchaseDate,
                                        odometer = odometer,
                                        operatingHours = operatingHours,
                                        onNickname = { nickname = it },
                                        onRegNo = { regNo = it },
                                        onPurchaseDate = { purchaseDate = it },
                                        onOdometer = { odometer = it },
                                        onOperatingHours = { operatingHours = it },
                                    )
                                } else {
                                    CategoryStep(
                                        categories = categories,
                                        manual = manual,
                                        onManual = { manual = true; categoryId = categories.firstOrNull()?.id.orEmpty(); move(1) },
                                        onCategory = { categoryId = it.id; manual = false; subcategory = ""; propulsion = ""; manufacturerId = ""; modelId = ""; move(1) },
                                    )
                                }
                            }
                            SetupStep.Subtype.index -> SubtypeStep(
                                entries = categoryEntries,
                                selected = subcategory,
                                onSelect = { subcategory = it; move(1) },
                            )
                            SetupStep.Propulsion.index -> PropulsionStep(
                                selected = propulsion,
                                onSelect = { propulsion = it; move(1) },
                            )
                            SetupStep.Manufacturer.index -> ManufacturerStep(
                                entries = filteredManufacturers,
                                selectedId = manufacturerId,
                                onSelect = { manufacturerId = it.id; move(1) },
                            )
                            SetupStep.Model.index -> ModelStep(
                                entries = filteredModels,
                                selectedId = modelId,
                                onSelect = { modelId = it.id; move(1) },
                            )
                            SetupStep.Review.index -> ReviewStep(
                                category = category,
                                propulsion = propulsion,
                                manufacturer = manufacturer,
                                model = model,
                                record = referenceRecord,
                                specOverrides = specOverrides,
                                showSpecEditor = showSpecEditor,
                                onEditSpecs = { showSpecEditor = it },
                            )
                            SetupStep.Personalize.index -> PersonalizeStep(
                                nickname = nickname,
                                regNo = regNo,
                                purchaseDate = purchaseDate,
                                odometer = odometer,
                                operatingHours = operatingHours,
                                onNickname = { nickname = it },
                                onRegNo = { regNo = it },
                                onPurchaseDate = { purchaseDate = it },
                                onOdometer = { odometer = it },
                                onOperatingHours = { operatingHours = it },
                            )
                            SetupStep.Complete.index -> Unit
                        }
                        error?.let { message ->
                            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        if (showSpecEditor) {
                            SpecEditorDialog(
                                onDismiss = { showSpecEditor = false },
                                values = specOverrides,
                                onChange = { specOverrides = it },
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (trackedStep > SetupStep.Category.index) {
                            OutlinedButton(onClick = { move(-1) }, enabled = !isSaving) { Text("Back") }
                        }
                        if (trackedStep == SetupStep.Category.index && manual) {
                            Button(
                                onClick = { create() },
                                enabled = canContinue() && nickname.isNotBlank() && !isSaving,
                                modifier = Modifier.weight(1f),
                            ) { Text(if (isSaving) "Creating…" else "Create vehicle") }
                        } else if (trackedStep < SetupStep.Personalize.index) {
                            Button(
                                onClick = { move(1) },
                                enabled = canContinue(),
                                modifier = Modifier.weight(1f),
                            ) { Text("Continue") }
                        } else {
                            Button(
                                onClick = { create() },
                                enabled = canContinue() && nickname.isNotBlank() && !isSaving,
                                modifier = Modifier.weight(1f),
                            ) { Text(if (isSaving) "Creating…" else "Create vehicle") }
                        }
                    }
                }
            }
        }
    }
}

private fun clamp(value: Int): Int = value.coerceIn(SetupStep.Category.index, SetupStep.Personalize.index)

@Composable private fun Modifier.verticalScrollLocal(): Modifier = verticalScroll(androidx.compose.foundation.rememberScrollState()).fillMaxSize()

@Composable
private fun CategoryStep(
    categories: List<VehicleCategoryOption>,
    manual: Boolean,
    onManual: () -> Unit,
    onCategory: (VehicleCategoryOption) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (manual) {
            SelectableCard(
                title = "I don't see my vehicle — enter it manually",
                icon = Icons.Default.Edit,
                selected = true,
                onClick = {},
            )
            Text("Choose a category so the app can use the right tracking basis and setup fields.", style = MaterialTheme.typography.bodySmall)
        }
        categories.forEach { category ->
            SelectableCard(title = category.name, icon = iconForCategory(category), onClick = { onCategory(category) })
        }
    }
}

@Composable
private fun SubtypeStep(entries: List<VehicleReferenceCatalogEntry>, selected: String, onSelect: (String) -> Unit) {
    val options = entries.distinctBy { it.subcategory }.map { VehicleSubcategoryOption(it.subcategory, it.subcategory.replace('_', ' ').replaceFirstChar { it.uppercase() }) }
    if (options.isEmpty()) {
        Text("No reference subtypes available. Continue manually.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option -> SelectableChip(option.name, selected == option.id, { onSelect(option.id) }) }
    }
}

@Composable
private fun PropulsionStep(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(SOURCE_ELECTRICITY, SOURCE_PETROL, SOURCE_DIESEL, SOURCE_CNG, SOURCE_LPG, SOURCE_HYBRID)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option -> SelectableChip(option, selected == option.lowercase().replaceFirstChar { it.uppercase() }, { onSelect(option.lowercase().replaceFirstChar { it.uppercase() }) }) }
    }
}

@Composable
private fun ManufacturerStep(entries: List<VehicleReferenceManufacturer>, selectedId: String, onSelect: (VehicleReferenceManufacturer) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (entries.isEmpty()) {
            Text("No manufacturers available for this category.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else entries.forEach { manufacturer ->
            SelectableRow(
                title = manufacturer.name,
                subtitle = manufacturer.brand,
                selected = manufacturer.id == selectedId,
                onClick = { onSelect(manufacturer) },
            )
        }
    }
}

@Composable
private fun ModelStep(entries: List<VehicleReferenceCatalogEntry>, selectedId: String, onSelect: (VehicleReferenceCatalogEntry) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (entries.isEmpty()) {
            Text("No models available for this category or manufacturer.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else entries.take(30).forEach { model ->
            SelectableRow(
                title = model.variant.ifBlank { model.model },
                subtitle = listOfNotNull(model.manufacturer, model.model, model.year.takeIf { it != 0 }?.toString()).joinToString(" · "),
                selected = model.id == selectedId,
                onClick = { onSelect(model) },
            )
        }
    }
}

@Composable
private fun ReviewStep(
    category: VehicleCategoryOption?,
    propulsion: String,
    manufacturer: VehicleReferenceManufacturer?,
    model: VehicleReferenceCatalogEntry?,
    record: VehicleReferenceVehicleRecord?,
    specOverrides: Map<String, String>,
    showSpecEditor: Boolean,
    onEditSpecs: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(record?.officialProductName ?: model?.model ?: category?.name ?: "Not selected", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text("${manufacturer?.name ?: ""} · ${category?.name ?: ""} · ${propulsion.replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextSpecRow("Type", category?.name ?: "Not set")
        TextSpecRow("Propulsion", propulsion.replaceFirstChar { it.uppercase() })
        record?.let { r ->
            TextSpecRow("Fuel type", r.fuelType.replace('_', ' ').replaceFirstChar { it.uppercase() })
            TextSpecRow("Displacement", r.engineDisplacementCc?.let { " ${it.toInt()} cc" } ?: "Not specified")
            TextSpecRow("Power", r.powerKw?.let { " ${it.toInt()} kW" } ?: "Not specified")
            TextSpecRow("Battery capacity", r.batteryCapacityKwh?.let { " ${it.toInt()} kWh" } ?: "Not specified")
            TextSpecRow("Tank", r.fuelTankLiters?.let { " ${it.toInt()} L" } ?: "Not specified")
        }
        if (specOverrides.isNotEmpty()) {
            Text("Reference overrides", style = MaterialTheme.typography.labelMedium)
            specOverrides.forEach { (path, value) -> TextSpecRow(path, value) }
        }
        OutlinedButton(onClick = { onEditSpecs(!showSpecEditor) }, modifier = Modifier.fillMaxWidth()) { Text(if (showSpecEditor) "Done editing" else "Edit reference specs") }
        Text(
            "Review the values above. Your edits override reference data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SpecEditorDialog(
    onDismiss: () -> Unit,
    values: Map<String, String>,
    onChange: (Map<String, String>) -> Unit,
) {
    var drafts by remember { mutableStateOf(values) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onChange(drafts); onDismiss() }) { Text("Save overrides") } },
        title = { Text("Reference specifications") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "energy_storage.battery_capacity_kwh" to "Battery capacity (kWh)",
                    "energy_storage.fuel_tank_liters" to "Tank capacity (L)",
                    "dimensions.cargo_capacity_liters" to "Cargo capacity (L)",
                    "powertrain.power" to "Power (kW)",
                ).forEach { (path, label) ->
                    OutlinedTextField(
                        value = drafts[path].orEmpty(),
                        onValueChange = { drafts = drafts + (path to it) },
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        },
    )
}

@Composable
private fun ManualEntryStep(
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Enter the vehicle manually.", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(value = nickname, onValueChange = onNickname, label = { Text("Nickname") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = regNo, onValueChange = onRegNo, label = { Text("Registration number (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = purchaseDate, onValueChange = onPurchaseDate, label = { Text("Purchase date YYYY-MM-DD (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = odometer, onValueChange = onOdometer, label = { Text("Current odometer (km)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = operatingHours, onValueChange = onOperatingHours, label = { Text("Current operating hours (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(value = nickname, onValueChange = onNickname, label = { Text("Nickname") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = regNo, onValueChange = onRegNo, label = { Text("Registration number (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = purchaseDate, onValueChange = onPurchaseDate, label = { Text("Purchase date YYYY-MM-DD (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = odometer, onValueChange = onOdometer, label = { Text("Current odometer (km)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = operatingHours, onValueChange = onOperatingHours, label = { Text("Current operating hours (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    }
}

private fun iconForCategory(category: VehicleCategoryOption): androidx.compose.ui.graphics.vector.ImageVector = when (category.id) {
    "two_wheeler", "three_wheeler" -> Icons.AutoMirrored.Filled.DirectionsBike
    "passenger_vehicle" -> Icons.Default.Settings
    "commercial_vehicle" -> Icons.Default.LocalShipping
    "agricultural" -> Icons.Default.Build
    "construction" -> Icons.Default.Home
    "public_transport" -> Icons.Default.Map
    else -> Icons.Default.Settings
}

@Composable
private fun SelectableCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean = false, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (selected) Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) else Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

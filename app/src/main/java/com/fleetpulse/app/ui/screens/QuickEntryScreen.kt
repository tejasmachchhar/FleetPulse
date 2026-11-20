package com.fleetpulse.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fleetpulse.app.data.db.FILL_FULL_CHARGE
import com.fleetpulse.app.data.db.FILL_FULL_TANK
import com.fleetpulse.app.data.db.FILL_PARTIAL_CHARGE
import com.fleetpulse.app.data.db.FILL_PARTIAL_REFILL
import com.fleetpulse.app.data.db.FILL_RIDE_ONLY
import com.fleetpulse.app.data.db.SOURCE_ELECTRICITY
import com.fleetpulse.app.data.db.UsageEntry
import com.fleetpulse.app.data.db.Vehicle
import com.fleetpulse.app.domain.CalcEngine
import com.fleetpulse.app.domain.EngineState
import com.fleetpulse.app.domain.EvalOptions
import com.fleetpulse.app.domain.EvaluatedUsage
import com.fleetpulse.app.domain.NumberText
import com.fleetpulse.app.domain.PetrolPricing
import com.fleetpulse.app.ui.MainViewModel
import com.fleetpulse.app.ui.components.AppDateField
import com.fleetpulse.app.ui.components.DecimalField
import com.fleetpulse.app.ui.components.bringIntoViewOnIme
import com.fleetpulse.app.ui.components.EmptyState
import com.fleetpulse.app.ui.components.FormSection
import com.fleetpulse.app.ui.components.ScreenHeader
import com.fleetpulse.app.ui.components.SectionCard
import com.fleetpulse.app.ui.components.StatusBadge
import com.fleetpulse.app.ui.components.VehicleDot
import com.fleetpulse.app.ui.components.formatDate
import com.fleetpulse.app.ui.components.formatINR
import com.fleetpulse.app.ui.components.formatNum
import com.fleetpulse.app.ui.theme.FleetAmber
import com.fleetpulse.app.ui.theme.FleetGreen
import com.fleetpulse.app.ui.theme.FleetRed
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEntryScreen(
    state: EngineState,
    mode: String,
    presetVehicleId: String?,
    editEntryId: String?,
    vm: MainViewModel,
    onDone: () -> Unit,
    onOpenVehicles: () -> Unit,
    onReportProblem: () -> Unit,
) {
    val existing = editEntryId?.let { id -> state.usageEntries.firstOrNull { it.id == id } }
    val isEdit = existing != null

    val allowedVehicles = state.vehicles.filter { v ->
        when {
            isEdit -> v.id == existing!!.vehicleId
            mode == "ev_charge" -> !v.archived && v.energySource == SOURCE_ELECTRICITY
            mode == "petrol_fill" -> !v.archived && v.energySource != SOURCE_ELECTRICITY
            else -> !v.archived
        }
    }

    if (allowedVehicles.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                title = when (mode) {
                    "ev_charge" -> "EV charge"
                    "petrol_fill" -> "Petrol refill"
                    "ride_only" -> "Ride only"
                    else -> "Quick entry"
                },
                onBack = onDone,
            )
            EmptyState(
                icon = if (mode == "petrol_fill") Icons.Default.LocalGasStation else Icons.Default.Bolt,
                title = if (mode == "ev_charge") "No electric vehicle yet" else "No matching vehicle yet",
                message = "Add a vehicle with the right energy source first, then come back to log usage.",
            )
            Button(
                onClick = onOpenVehicles,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Open vehicles") }
        }
        return
    }

    val initialVehicle = allowedVehicles.firstOrNull { it.id == presetVehicleId } ?: allowedVehicles.first()
    var vehicleId by rememberSaveable(isEdit, mode, presetVehicleId) {
        mutableStateOf(existing?.vehicleId ?: initialVehicle.id)
    }
    val vehicle = state.vehicles.firstOrNull { it.id == vehicleId } ?: initialVehicle

    val defaultFill = when {
        existing != null -> existing.fillType
        mode == "ride_only" -> FILL_RIDE_ONLY
        vehicle.energySource == SOURCE_ELECTRICITY -> FILL_FULL_CHARGE
        else -> FILL_FULL_TANK
    }
    var fillType by rememberSaveable(isEdit, vehicleId, mode) { mutableStateOf(defaultFill) }
    var date by rememberSaveable(isEdit) { mutableStateOf(existing?.date ?: LocalDate.now().toString()) }
    var odometer by rememberSaveable(isEdit) {
        mutableStateOf(existing?.odometer?.let { NumberText.plain(it) } ?: "")
    }
    var hoursUsed by rememberSaveable(isEdit) {
        mutableStateOf(existing?.hoursUsed?.let { NumberText.plain(it) } ?: "")
    }
    var energyMode by rememberSaveable(isEdit, vehicleId) {
        mutableStateOf(
            if (existing?.energyAdded != null && existing.voltsBefore == null) "meter" else "voltage",
        )
    }
    var energyAdded by rememberSaveable(isEdit) {
        mutableStateOf(existing?.energyAdded?.let { NumberText.plain(it) } ?: "")
    }
    var voltsBefore by rememberSaveable(isEdit) {
        mutableStateOf(existing?.voltsBefore?.let { NumberText.plain(it) } ?: "")
    }
    var voltsAfter by rememberSaveable(isEdit) {
        mutableStateOf(existing?.voltsAfter?.let { NumberText.plain(it) } ?: "")
    }
    var unitPrice by rememberSaveable(isEdit) {
        mutableStateOf(existing?.unitPrice?.let { NumberText.plain(it) } ?: "")
    }
    var includeInMileage by rememberSaveable(isEdit) {
        mutableStateOf(existing?.includeInMileage ?: true)
    }
    var petrolAmount by rememberSaveable(isEdit) {
        mutableStateOf(
            if (existing?.energyAdded != null && existing.unitPrice != null) {
                NumberText.fixed(existing.energyAdded * existing.unitPrice, 2)
            } else {
                ""
            },
        )
    }
    var petrolManualOrder by rememberSaveable(isEdit) {
        mutableStateOf(
            if (existing?.energyAdded != null) {
                if (existing.unitPrice != null) "quantity,price" else "quantity"
            } else {
                ""
            },
        )
    }
    var derivedPetrolField by rememberSaveable(isEdit) {
        mutableStateOf(
            if (existing?.energyAdded != null && existing.unitPrice != null) "amount" else null,
        )
    }
    var remarks by rememberSaveable(isEdit) { mutableStateOf(existing?.remarks ?: "") }
    var saving by remember { mutableStateOf(false) }
    var showVehicleMenu by remember { mutableStateOf(false) }

    fun setPetrolValue(field: String, value: String) {
        when (field) {
            "quantity" -> energyAdded = value
            "price" -> unitPrice = value
            "amount" -> petrolAmount = value
        }
    }

    fun recalcPetrolFromManualFields(order: List<String>) {
        if (order.size < 2) return
        val third = listOf("quantity", "price", "amount").firstOrNull { it !in order } ?: return
        val quantity = NumberText.parse(energyAdded)
        val price = NumberText.parse(unitPrice)
        val amount = NumberText.parse(petrolAmount)
        val result = when (third) {
            "amount" -> PetrolPricing.amount(quantity, price)
            "price" -> PetrolPricing.unitPrice(amount, quantity)
            else -> PetrolPricing.quantity(amount, price)
        }
        if (result == null || result <= 0.0 || !result.isFinite()) {
            setPetrolValue(third, "")
            derivedPetrolField = null
        } else {
            setPetrolValue(
                third,
                when (third) {
                    "amount" -> NumberText.fixed(result, 2)
                    "price" -> NumberText.fixed(result, 2)
                    else -> NumberText.fixed(result, 3)
                },
            )
            derivedPetrolField = third
        }
    }

    fun updatePetrolField(field: String, value: String) {
        setPetrolValue(field, value)
        val order = petrolManualOrder.split(',').filter { it.isNotBlank() }.toMutableList()
        if (value.isBlank()) {
            val wasManual = field in order
            order.remove(field)
            petrolManualOrder = order.takeLast(2).joinToString(",")
            if (derivedPetrolField == field) {
                derivedPetrolField = null
            } else if (wasManual) {
                // The remaining single value cannot derive a third value.
                derivedPetrolField?.let { setPetrolValue(it, "") }
                derivedPetrolField = null
            }
            return
        }
        order.remove(field)
        order.add(field)
        val lastTwo = order.takeLast(2)
        petrolManualOrder = lastTwo.joinToString(",")
        if (lastTwo.size < 2) {
            derivedPetrolField?.let { setPetrolValue(it, "") }
            derivedPetrolField = null
            return
        }
        recalcPetrolFromManualFields(lastTwo)
    }

    // Fill-type options depend on the vehicle energy source.
    val fillOptions = if (vehicle.energySource == SOURCE_ELECTRICITY) {
        listOf(FILL_FULL_CHARGE, FILL_PARTIAL_CHARGE, FILL_RIDE_ONLY)
    } else {
        listOf(FILL_FULL_TANK, FILL_PARTIAL_REFILL, FILL_RIDE_ONLY)
    }
    val activeFillType = fillType.takeIf { it in fillOptions } ?: fillOptions.first()

    val isEv = vehicle.energySource == SOURCE_ELECTRICITY
    val vehicleUsesHours = vehicle.isOperatingHoursBasis
    val isRideOnly = activeFillType == FILL_RIDE_ONLY
    val isFull = activeFillType == FILL_FULL_CHARGE || activeFillType == FILL_FULL_TANK

    // Previous odometer hint (latest reading on or before the chosen date).
    val previousOdo = remember(state.usageEntries, vehicleId, date, editEntryId) {
        state.usageEntries
            .filter {
                it.vehicleId == vehicleId && it.id != editEntryId && it.date <= date && it.odometer != null
            }
            .maxByOrNull { it.date }
            ?.odometer
            ?: state.vehicles.firstOrNull { it.id == vehicleId }?.initialOdometer
    }
    val lastFull = remember(state.usageEntries, vehicleId, date, editEntryId) {
        state.usageEntries
            .filter {
                it.vehicleId == vehicleId && it.id != editEntryId && it.date <= date &&
                    it.includeInMileage &&
                    (it.fillType == FILL_FULL_CHARGE || it.fillType == FILL_FULL_TANK)
            }
            .maxByOrNull { it.date }
    }
    val effectivePrice = remember(state.priceHistory, vehicle.energySource, date) {
        CalcEngine.getEffectivePrice(vehicle.energySource, date, state.priceHistory)
    }

    val draft = remember(
        existing, vehicleId, date, odometer, activeFillType, energyMode, energyAdded,
        voltsBefore, voltsAfter, unitPrice, remarks, includeInMileage, isRideOnly,
    ) {
        UsageEntry(
            id = existing?.id ?: "preview-entry",
            date = date,
            vehicleId = vehicleId,
            odometer = NumberText.parse(odometer),
            hoursUsed = NumberText.parse(hoursUsed),
            fillType = activeFillType,
            energyAdded = when {
                isRideOnly -> null
                isEv && energyMode == "meter" || !isEv -> NumberText.parse(energyAdded)
                else -> null
            },
            voltsBefore = if (isEv && energyMode == "voltage" && !isRideOnly) NumberText.parse(voltsBefore) else null,
            voltsAfter = if (isEv && energyMode == "voltage" && !isRideOnly && !isFull) NumberText.parse(voltsAfter) else null,
            unitPrice = if (isRideOnly) null else NumberText.parse(unitPrice),
            remarks = remarks.trim(),
            isSample = false,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            includeInMileage = if (isRideOnly) true else includeInMileage,
        )
    }

    val preview: EvaluatedUsage? = remember(state, draft, existing) {
        if (draft.odometer == null) {
            null
        } else {
            val entries = if (existing != null) {
                state.usageEntries.map { if (it.id == existing.id) draft else it }
            } else {
                state.usageEntries + draft
            }
            val evaluated = CalcEngine.evaluate(
                state.copy(usageEntries = entries),
                EvalOptions(referenceDate = LocalDate.now()),
            )
            evaluated.evaluatedUsage.firstOrNull { it.entry.id == draft.id }
        }
    }

    val odoValue = NumberText.parse(odometer)
    val duplicateOdo = odoValue != null && vm.isDuplicateOdometer(vehicleId, odoValue, existing?.id)
    val petrolValuesValid = isEv || isRideOnly ||
        (unitPrice.isBlank() || (NumberText.parse(unitPrice) ?: 0.0) > 0.0) &&
        (petrolAmount.isBlank() || (NumberText.parse(petrolAmount) ?: 0.0) > 0.0)
    val hoursValue = NumberText.parse(hoursUsed)
    val canSave = !saving && (odoValue != null || (vehicleUsesHours && hoursValue != null)) && !duplicateOdo && petrolValuesValid &&
        (isRideOnly || (if (isEv && energyMode == "meter") NumberText.parse(energyAdded)?.let { it > 0 } == true
        else if (isEv) NumberText.parse(voltsBefore) != null
        else NumberText.parse(energyAdded)?.let { it > 0 } == true)) &&
        preview?.status != "ERROR"

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = when {
                isEdit -> "Edit usage entry"
                mode == "ev_charge" -> "EV charge"
                mode == "petrol_fill" -> "Petrol refill"
                mode == "ride_only" -> "Ride only"
                else -> "Quick usage entry"
            },
            subtitle = "${vehicle.name} · ${vehicle.type}",
            onBack = onDone,
            actions = {
                IconButton(onClick = onReportProblem) {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = "Report a problem with this entry screen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                FormSection("Vehicle & date") {
                    if (isEdit) {
                        OutlinedTextField(
                            value = vehicle.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vehicle") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = showVehicleMenu,
                            onExpandedChange = { showVehicleMenu = it },
                        ) {
                            OutlinedTextField(
                                value = vehicle.name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Vehicle") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showVehicleMenu) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                            )
                            ExposedDropdownMenu(
                                expanded = showVehicleMenu,
                                onDismissRequest = { showVehicleMenu = false },
                            ) {
                                allowedVehicles.forEach { v ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                VehicleDot(v.color)
                                                Spacer(Modifier.width(8.dp))
                                                Text("${v.name} · ${v.type}")
                                            }
                                        },
                                        onClick = {
                                            vehicleId = v.id
                                            showVehicleMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    AppDateField("Date", date, { date = it })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Previous reading: ${previousOdo?.let { formatNum(it, 1) + " km" } ?: "none"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        if (lastFull != null) {
                            Text(
                                "Last eligible full: ${formatDate(lastFull.date)} @ ${formatNum(lastFull.odometer, 0)} km",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            item {
                FormSection("This entry") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fillOptions.forEach { option ->
                            FilterChip(
                                selected = activeFillType == option,
                                onClick = { fillType = option },
                                label = { Text(option, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                        }
                    }
                    if (!isRideOnly) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { includeInMileage = !includeInMileage }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = includeInMileage,
                                onCheckedChange = null,
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Include this entry for mileage estimation",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "Unchecked entries stay in history and expenses, but do not contribute fuel or energy to mileage intervals.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    DecimalField(
                        label = "Odometer reading",
                        value = odometer,
                        onValueChange = { odometer = it },
                        suffix = "km",
                        placeholder = previousOdo?.let { formatNum(it, 0) } ?: "e.g. 15122",
                    )
                    if (vehicleUsesHours) {
                        DecimalField(
                            label = "Operating hours",
                            value = hoursUsed,
                            onValueChange = { hoursUsed = it },
                            suffix = "h",
                            placeholder = "e.g. 4.5",
                        )
                    }
                    when {
                        odoValue == null -> Unit
                        previousOdo != null && odoValue < previousOdo -> Text(
                            "Odometer is lower than the previous reading (${formatNum(previousOdo, 0)} km). " +
                                "Check the entry before saving.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FleetRed,
                        )
                        duplicateOdo -> Text(
                            "Another entry already uses this reading. It is not saved twice.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FleetAmber,
                        )
                    }
                }
            }

            if (!isRideOnly) {
                item {
                    FormSection(if (isEv) "Energy added" else "Fuel added") {
                        if (isEv) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = energyMode == "voltage",
                                    onClick = { energyMode = "voltage" },
                                    label = { Text("Battery voltage") },
                                )
                                FilterChip(
                                    selected = energyMode == "meter",
                                    onClick = { energyMode = "meter" },
                                    label = { Text("Meter reading (kWh)") },
                                )
                            }
                        }

                        if (!isEv) {
                            DecimalField(
                                "Petrol quantity", energyAdded,
                                { updatePetrolField("quantity", it) }, suffix = "L",
                                placeholder = "e.g. 8.5",
                            )
                            if (derivedPetrolField == "quantity") {
                                AutoCalculatedHint("Auto-calculated from amount ÷ unit price")
                            }
                        } else if (energyMode == "meter") {
                            DecimalField(
                                "Energy measured by meter", energyAdded,
                                { energyAdded = it }, suffix = "kWh",
                                placeholder = "e.g. 2.4",
                            )
                            Text(
                                "Meter mode is more accurate. Enter the wall energy from a plug meter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            DecimalField(
                                "Volts before charge",
                                voltsBefore,
                                { voltsBefore = it },
                                suffix = "V",
                                placeholder = "e.g. 54.0",
                            )
                            if (!isFull) {
                                DecimalField(
                                    "Volts after charge", voltsAfter,
                                    { voltsAfter = it }, suffix = "V",
                                    placeholder = "e.g. 67.3",
                                )
                            }
                            Text(
                                if (isFull) {
                                    "A full charge is treated as 100%. Battery % before charge comes from the voltage calibration curve."
                                } else {
                                    "Both voltages are converted to % using this vehicle's calibration curve."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val sampleVolts = if (isFull) voltsBefore else voltsAfter
                            if (sampleVolts.isNotBlank()) {
                                val interpolation = CalcEngine.interpolateVoltage(
                                    vehicleId, NumberText.parse(sampleVolts) ?: 0.0, state.calibration,
                                )
                                Text(
                                    buildString {
                                        append("Estimated state of charge: ")
                                        append(interpolation.percent?.let { formatNum(it, 1) + "%" } ?: "no calibration curve")
                                        if (interpolation.outOfRange) append(" · outside calibration range")
                                        if (interpolation.missingCurve) append(" · add calibration points first")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (interpolation.missingCurve || interpolation.outOfRange) FleetAmber else FleetGreen,
                                )
                            }
                        }

                        DecimalField(
                            if (isEv) "Unit price (optional)" else "Unit price (₹/L, optional)",
                            unitPrice,
                            { if (isEv) unitPrice = it else updatePetrolField("price", it) },
                            suffix = if (isEv) "₹/kWh" else "₹/L",
                            placeholder = effectivePrice?.let { formatNum(it, 2) } ?: "No tariff configured",
                        )
                        if (!isEv && derivedPetrolField == "price") {
                            AutoCalculatedHint("Auto-calculated from amount ÷ quantity")
                        }
                        if (!isEv) {
                            DecimalField(
                                "Refilling amount (₹)",
                                petrolAmount,
                                { updatePetrolField("amount", it) },
                                suffix = "₹",
                                placeholder = "e.g. 500.00",
                            )
                            if (derivedPetrolField == "amount") {
                                AutoCalculatedHint("Auto-calculated from quantity × unit price")
                            }
                            Text(
                                "Enter any two values; the third is calculated automatically. " +
                                    "Unit price and amount are rounded for display. " +
                                    "A saved entry uses quantity × unit price as its consistent amount.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "Leave blank to use the historical tariff effective on $date" +
                                (effectivePrice?.let { ": ${formatINR(it, 2)} per ${if (isEv) "kWh" else "litre"}" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                FormSection("Remarks") {
                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewOnIme(),
                        shape = RoundedCornerShape(14.dp),
                        minLines = 2,
                    )
                }
            }

            item {
                SectionCard(title = "Live preview", subtitle = "Calculated by the same engine as the dashboard") {
                    val p = preview
                    if (p == null) {
                        Text("Enter an odometer reading to preview.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PreviewMetric("Distance", "${formatNum(p.distance, 1)} km", Modifier.weight(1f))
                            PreviewMetric(
                                "Energy",
                                "${formatNum(p.energyUsed, 2)} ${if (isEv) "kWh" else "L"}",
                                Modifier.weight(1f),
                            )
                            PreviewMetric("Cost", formatINR(p.totalCost, 0), Modifier.weight(1f))
                        }
                        if (p.pctBefore != null || p.pctAfter != null) {
                            Text(
                                "Battery: ${p.pctBefore?.let { formatNum(it, 0) + "%" } ?: "—"} → " +
                                    "${p.pctAfter?.let { formatNum(it, 0) + "%" } ?: "—"}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (p.isVerifiedStretch) {
                            Icon(Icons.Default.CheckCircle, null, tint = FleetGreen, modifier = Modifier.size(16.dp))
                            Text(
                                "Full-to-full verified: ${formatNum(p.kmPerCharge, 1)} km/charge · " +
                                    "${formatNum(p.stretchEfficiency, 2)} ${if (isEv) "km/kWh" else "km/L"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = FleetGreen,
                            )
                        }
                        if (!p.entry.includeInMileage) {
                            Text(
                                "Excluded from mileage estimation. This entry still counts toward expenses.",
                                style = MaterialTheme.typography.bodySmall,
                                color = FleetAmber,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(p.status)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                p.checkMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = when (p.status) {
                                    "ERROR" -> FleetRed
                                    "CHECK" -> FleetAmber
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "Data quality: ERROR entries are blocked here because they cannot be used in any calculation. " +
                        "CHECK entries are saved and clearly flagged for review.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Surface(tonalElevation = 3.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDone,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Cancel") }
                Button(
                    onClick = {
                        saving = true
                        val entry = draft.copy(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            odometer = odoValue,
                            hoursUsed = if (vehicleUsesHours) NumberText.parse(hoursUsed) else null,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        )
                        val ok = vm.saveUsage(entry, existing == null)
                        if (ok) onDone() else saving = false
                    },
                    modifier = Modifier.weight(2f),
                    enabled = canSave,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(if (isEdit) "Save changes" else "Save entry")
                }
            }
        }
    }
}

@Composable
private fun PreviewMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun AutoCalculatedHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

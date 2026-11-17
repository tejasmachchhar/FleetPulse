package com.fleetpulse.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme.shapes
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------------------------------------------------------------------------
// Formatting helpers (India-focused: ₹, km, kWh, L, en-IN digit grouping)
// ---------------------------------------------------------------------------

private val inLocale = Locale("en", "IN")

fun formatNum(value: Double?, decimals: Int = 1): String {
    if (value == null || value.isNaN() || value.isInfinite()) return "—"
    val nf = NumberFormat.getNumberInstance(inLocale).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }
    return nf.format(value)
}

fun formatINR(value: Double?, decimals: Int = 0): String {
    if (value == null || value.isNaN() || value.isInfinite()) return "—"
    return "₹" + formatNum(value, decimals)
}

fun formatOdometer(value: Double?): String = formatNum(value, 0) + " km"

fun todayIso(): String = LocalDate.now().toString()

private val displayDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)

fun formatDate(iso: String?): String {
    if (iso.isNullOrBlank() || iso.length < 10) return iso ?: "—"
    return runCatching { LocalDate.parse(iso).format(displayDate) }.getOrDefault(iso)
}

fun isoFromMillisUtc(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate().toString()

fun millisUtcFromIso(iso: String): Long? =
    runCatching { LocalDate.parse(iso).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli() }.getOrNull()

fun parseColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFF2563EB))

// ---------------------------------------------------------------------------
// Building blocks
// ---------------------------------------------------------------------------

@Composable
fun SectionCard(
    title: String? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        if (subtitle != null) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    trailing?.invoke()
                }
            }
            content()
        }
    }
}

@Composable
fun KpiCard(
    label: String,
    value: String,
    caption: String? = null,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    badge: String? = null,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    label.uppercase(Locale.US),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (badge != null) {
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (caption != null) {
                Text(
                    caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, fg) = when (status) {
        "ERROR", "Overdue" -> Color(0xFFDC2626) to Color.White
        "CHECK", "Due Soon" -> Color(0xFFF59E0B) to Color(0xFF1F2937)
        "Completed", "Logged" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else -> Color(0xFF059669) to Color.White
    }
    Text(
        status,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        modifier = Modifier
            .background(bg.copy(alpha = 0.9f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun VehicleDot(colorHex: String, size: Dp = 10.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(parseColor(colorHex)),
    )
}

@Composable
fun LabelValueRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
        )
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        actions?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleFilterDropdown(
    vehicles: List<com.fleetpulse.app.data.db.Vehicle>,
    selectedId: String,
    onSelect: (String) -> Unit,
    includeAll: Boolean = true,
    modifier: Modifier = Modifier,
    label: String = "Vehicle",
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = if (selectedId == "ALL") {
        "All vehicles"
    } else {
        vehicles.firstOrNull { it.id == selectedId }?.name ?: "All vehicles"
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (includeAll) {
                DropdownMenuItem(
                    text = { Text("All vehicles") },
                    onClick = {
                        onSelect("ALL")
                        expanded = false
                    },
                )
            }
            vehicles.forEach { v ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            VehicleDot(v.color)
                            Spacer(Modifier.width(8.dp))
                            Text(v.name + if (v.archived) " (archived)" else "")
                        }
                    },
                    onClick = {
                        onSelect(v.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun OptionChips(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (key, label) ->
            FilterChip(
                selected = selected == key,
                onClick = { onSelect(key) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = formatDate(value),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        trailingIcon = {
            TextButton(onClick = { showPicker = true }) { Text("Pick") }
        },
    )
    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = millisUtcFromIso(value) ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onValueChange(isoFromMillisUtc(it)) }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun DecimalField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    placeholder: String = "",
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            // Editable fields hold comma-free decimal text. Accept a pasted
            // grouped value by stripping separators, but never reformat while
            // typing (which would move the cursor or block Backspace).
            val cleaned = new.replace(",", "").replace(" ", "")
            if (cleaned.isEmpty() || cleaned.matches(Regex("^\\d*\\.?\\d*$"))) {
                onValueChange(cleaned)
            }
        },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        placeholder = placeholder.takeIf { it.isNotBlank() }?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewOnIme(),
        shape = RoundedCornerShape(14.dp),
    )
}

/**
 * Keeps the focused text field visible when the IME opens. Compose's text
 * fields bring themselves into view on focus, but the keyboard usually starts
 * animating after that focus event. Re-requesting on each IME inset change keeps
 * the field above the keyboard while the inset animates.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.bringIntoViewOnIme(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    var focused by remember { mutableStateOf(false) }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)

    LaunchedEffect(focused, imeBottom) {
        if (focused && imeBottom > 0) {
            // Let this inset update reach layout before asking the scrollable
            // parent to reveal the field.
            withFrameNanos { }
            requester.bringIntoView()
        }
    }

    return this
        .bringIntoViewRequester(requester)
        .onFocusChanged { state ->
            focused = state.isFocused
            if (state.isFocused) {
                scope.launch { requester.bringIntoView() }
            }
        }
}

/**
 * Alert-dialog replacement for forms. The regular Material3 AlertDialog is a
 * small, centred window and can be covered by the keyboard. This keeps the
 * same Material styling but uses an edge-to-edge dialog, applies safeDrawing
 * (which includes the IME), scrolls the form body, and keeps the actions fixed
 * at the bottom of the card.
 */
@Composable
fun KeyboardAwareFormDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    dismissLabel: String = "Cancel",
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = 6.dp,
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = AlertDialogDefaults.titleContentColor,
                    )
                    Spacer(Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        content()
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDismissRequest) { Text(dismissLabel) }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmLabel) }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirm",
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = shapes.extraLarge,
    )
}

@Composable
fun FormSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title.uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
fun Gap(height: Dp) = Spacer(Modifier.height(height))

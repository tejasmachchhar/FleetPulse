package com.fleetpulse.app.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fleetpulse.app.data.backup.BackupPayload
import com.fleetpulse.app.ui.MainViewModel
import com.fleetpulse.app.ui.components.ConfirmDialog
import com.fleetpulse.app.ui.components.ScreenHeader
import com.fleetpulse.app.ui.components.SectionCard
import com.fleetpulse.app.ui.components.formatNum
import com.fleetpulse.app.ui.theme.FleetAmber
import com.fleetpulse.app.ui.theme.FleetGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun DataManagementScreen(
    vm: MainViewModel,
    sampleUsage: Int,
    sampleMaintenance: Int,
    sampleCalibration: Int,
    vehicleCount: Int,
    usageCount: Int,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<BackupPayload?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var showLoadDemo by remember { mutableStateOf(false) }
    var showRemoveSamples by remember { mutableStateOf(false) }
    val sampleTotal = sampleUsage + sampleMaintenance + sampleCalibration

    val exportJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = vm.buildBackup()
            if (json == null) {
                vm.toast("The database is still loading. Try exporting again.")
                return@launch
            }
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not open the chosen file")
                }.isSuccess
            }
            vm.toast(if (ok) "Backup exported." else "Backup export failed.")
        }
    }

    val exportUsageCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val csv = vm.usageCsv()
            if (csv == null) {
                vm.toast("No evaluation available yet.")
                return@launch
            }
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not open the chosen file")
                }.isSuccess
            }
            vm.toast(if (ok) "Usage CSV exported." else "CSV export failed.")
        }
    }

    val exportMaintCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val csv = vm.maintenanceCsv()
            if (csv == null) {
                vm.toast("No evaluation available yet.")
                return@launch
            }
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not open the chosen file")
                }.isSuccess
            }
            vm.toast(if (ok) "Maintenance CSV exported." else "CSV export failed.")
        }
    }

    val importJson = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (text == null) {
                importError = "Could not read the selected file."
                return@launch
            }
            val result = vm.previewImport(text)
            result.onSuccess { pendingImport = it }
                .onFailure { importError = it.message ?: "This file is not a valid FleetPulse backup." }
        }
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
                title = "Data management",
                subtitle = "Local database, backups and sample data",
                onBack = onBack,
            )
        }

        item {
            SectionCard(
                title = "Current database",
                subtitle = "Stored privately on this phone",
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Vehicles", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$vehicleCount", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace)
                    }
                    Column {
                        Text("Usage entries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$usageCount", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace)
                    }
                    Column {
                        Text("Sample records", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$sampleTotal", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace, color = FleetAmber)
                    }
                }
                if (sampleTotal > 0) {
                    Text(
                        "Sample Mode is active. Rows marked [SAMPLE] are included in calculations so the dashboard " +
                            "matches the reference workbook; remove them when you start entering real data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            SectionCard(
                title = "Sample data",
                subtitle = "Clearly marked · removable without touching your records",
            ) {
                if (sampleTotal > 0) {
                    Text(
                        "Loaded: $sampleUsage usage · $sampleMaintenance service · $sampleCalibration calibration points.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(
                        onClick = { showRemoveSamples = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Default.Science, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Remove sample records")
                    }
                } else {
                    Text(
                        "No sample records are loaded. The reference demo includes three vehicles and the workbook " +
                            "figures used by the built-in verification suite.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { showLoadDemo = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Load sample data")
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "Full backup",
                subtitle = "JSON v2 · includes every record, tariff, curve and setting",
            ) {
                Text(
                    "The native format is {\"format\":\"fleetpulse-backup\",\"version\":2,\"data\":{…}}. " +
                        "Backups exported by the original FleetPulse v4 web app (raw state JSON) are also accepted on import.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        exportJson.launch("fleetpulse-backup-${LocalDate.now()}.json")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export backup (JSON)")
                }
                OutlinedButton(
                    onClick = {
                        importJson.launch(arrayOf("application/json", "text/plain", "text/*", "*/*"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.Restore, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import / restore backup")
                }
                Text(
                    "Import always previews the file first and requires explicit confirmation before replacing data.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard(
                title = "Spreadsheet exports",
                subtitle = "Evaluated data with every calculated column",
            ) {
                OutlinedButton(
                    onClick = { exportUsageCsv.launch("fleetpulse-usage-${LocalDate.now()}.csv") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Export usage CSV") }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { exportMaintCsv.launch("fleetpulse-maintenance-${LocalDate.now()}.csv") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Export maintenance CSV") }
                Text(
                    "CSV columns include distance, energy, basis (Measured/Estimated), cost, verified efficiency, " +
                        "status and check message — matching the web export.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard(title = "Safety rules") {
                Text(
                    "• Nothing is deleted silently. Deleting a vehicle or entry always shows what will be removed.\n" +
                        "• Archiving keeps all history and only hides the vehicle from dashboards.\n" +
                        "• Sample records are flagged in the database and removed as a group.\n" +
                        "• Import is transactional: a failed restore leaves your existing data untouched.\n" +
                        "• The app has no INTERNET permission and never uploads your data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showLoadDemo) {
        ConfirmDialog(
            title = "Load reference sample data?",
            message = "Adds three sample vehicles (Demo EV 1, Demo EV 2, Demo Petrol) and the workbook records. " +
                "Existing records with the same IDs are never overwritten. All sample rows are marked [SAMPLE]. " +
                "If you already have entries, charts will combine both until you remove the samples.",
            confirmLabel = "Load samples",
            onConfirm = { vm.loadDemoData() },
            onDismiss = { showLoadDemo = false },
        )
    }

    if (showRemoveSamples) {
        ConfirmDialog(
            title = "Remove sample records?",
            message = "Deletes only rows flagged [SAMPLE]: usage, maintenance, calibration, and demo vehicles with no genuine " +
                "records. Sample tariffs are retained while genuine entries still depend on them, so historical costs do not change.",
            confirmLabel = "Remove samples",
            destructive = true,
            onConfirm = { vm.removeSampleData() },
            onDismiss = { showRemoveSamples = false },
        )
    }

    pendingImport?.let { payload ->
        val currentExists = vehicleCount > 0 || usageCount > 0
        AlertDialog(
            onDismissRequest = {
                pendingImport = null
                vm.clearImportPreview()
            },
            title = { Text("Restore this backup?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("The file was parsed successfully:")
                    Text(
                        "${payload.vehicles.size} vehicles · ${payload.usage.size} usage entries · " +
                            "${payload.maintenance.size} maintenance records · ${payload.prices.size} tariffs · " +
                            "${payload.calibration.size} calibration points",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (currentExists) {
                        Text(
                            "This REPLACES the current data: $vehicleCount vehicles and $usageCount usage entries " +
                                "will be replaced in one transaction. Export a backup first if you need the current data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FleetAmber,
                        )
                    }
                    Text(
                        "Restore is all-or-nothing: if writing fails, your existing database is kept.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.confirmImport(payload)
                    pendingImport = null
                }) { Text(if (currentExists) "Replace and restore" else "Restore") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingImport = null
                    vm.clearImportPreview()
                }) { Text("Cancel") }
            },
        )
    }

    importError?.let { message ->
        AlertDialog(
            onDismissRequest = { importError = null },
            title = { Text("Import failed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(message)
                    Text(
                        "Your existing data was not changed. The file must be a FleetPulse JSON backup (v2) or a legacy v4 web-state JSON export.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { importError = null }) { Text("Close") } },
        )
    }
}

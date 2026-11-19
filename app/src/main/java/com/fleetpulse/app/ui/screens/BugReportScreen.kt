package com.fleetpulse.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fleetpulse.app.support.BugReportDraft
import com.fleetpulse.app.support.DiagnosticSnapshot
import com.fleetpulse.app.support.DiagnosticsCollector
import com.fleetpulse.app.support.FeedbackCategories
import com.fleetpulse.app.support.FeedbackCategory
import com.fleetpulse.app.support.FeedbackReportBuilder
import com.fleetpulse.app.support.SupportActions
import com.fleetpulse.app.support.SupportConfig
import com.fleetpulse.app.ui.components.DiagnosticToggleRow
import com.fleetpulse.app.ui.components.FeedbackCategoryChips
import com.fleetpulse.app.ui.components.ReportPreviewDialog
import com.fleetpulse.app.ui.components.ScreenHeader
import com.fleetpulse.app.ui.components.SectionCard
import com.fleetpulse.app.ui.components.bringIntoViewOnIme
import com.fleetpulse.app.ui.theme.FleetAmber
import com.fleetpulse.app.ui.theme.FleetGreen
import com.fleetpulse.app.ui.theme.FleetRed

@Composable
fun BugReportScreen(
    origin: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val currentScreen = origin.ifBlank { "Feedback & Support" }
    val diagnostics: DiagnosticSnapshot = remember(currentScreen) {
        DiagnosticsCollector.collect(context, currentScreen)
    }
    var selectedLabel by rememberSaveable { mutableStateOf<String?>(null) }
    var description by rememberSaveable { mutableStateOf("") }
    var includeAppInfo by rememberSaveable { mutableStateOf(true) }
    var includeEnvironment by rememberSaveable { mutableStateOf(true) }
    var includeLogs by rememberSaveable { mutableStateOf(true) }
    var showPreview by rememberSaveable { mutableStateOf(false) }

    val selectedCategory = FeedbackCategories.bug.firstOrNull { it.label == selectedLabel }
    val draft = selectedCategory?.let {
        BugReportDraft(
            category = it,
            description = description.trim(),
            currentScreen = currentScreen,
        )
    }
    val report = draft?.let {
        FeedbackReportBuilder.fitReportForIssue(
            FeedbackReportBuilder.buildBugReport(
                draft = it,
                diagnostics = diagnostics,
                includeAppInfo = includeAppInfo,
                includeEnvironment = includeEnvironment,
                includeLogs = includeLogs,
            ),
        )
    }.orEmpty()
    val issue = draft?.let { FeedbackReportBuilder.bugIssue(it, report) }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Report a Bug",
                subtitle = "Two quick choices, then review the exact report",
                onBack = onBack,
            )
        }

        item {
            SectionCard(title = "Where did it happen?", subtitle = "The app filled this in automatically") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, null, tint = FleetRed)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(currentScreen, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Only the screen name is included, never your vehicle records or values.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = "Step 1 · What went wrong?", subtitle = "Pick the closest category") {
                FeedbackCategoryChips(
                    categories = FeedbackCategories.bug,
                    selected = selectedCategory,
                    onSelect = { selectedLabel = it.label },
                )
            }
        }

        item {
            SectionCard(title = "Optional short description", subtitle = "A sentence is enough") {
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 2_000) description = it },
                    label = { Text("What happened? (optional)") },
                    placeholder = { Text("Example: the mileage card showed a dash after I edited a charge.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnIme(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 3,
                    maxLines = 6,
                    supportingText = {
                        Text("${description.length}/2000 · Do not include real vehicle or personal data.")
                    },
                )
            }
        }

        item {
            SectionCard(
                title = "Step 2 · Automatic diagnostics",
                subtitle = "Prepared locally; you can remove any part",
            ) {
                DiagnosticToggleRow(
                    title = "App version and build",
                    subtitle = "Version name, version code and debug/release type",
                    checked = includeAppInfo,
                    onCheckedChange = { includeAppInfo = it },
                )
                DiagnosticToggleRow(
                    title = "Device and environment",
                    subtitle = "Android version, device model, screen size, language and timestamp",
                    checked = includeEnvironment,
                    onCheckedChange = { includeEnvironment = it },
                )
                DiagnosticToggleRow(
                    title = "Recent diagnostics",
                    subtitle = "Redacted error type, stack frames and the last few local log lines",
                    checked = includeLogs,
                    onCheckedChange = { includeLogs = it },
                )
                Text(
                    "Vehicle names, registration numbers, odometer readings, fuel/charging records, expenses, " +
                        "notes, database contents, location and files are never collected.",
                    style = MaterialTheme.typography.labelSmall,
                    color = FleetGreen,
                )
                if (diagnostics.errorInformation.isNotBlank()) {
                    Text(
                        "A recent error was recorded locally. It will be included only if the diagnostics toggle is on.",
                        style = MaterialTheme.typography.labelSmall,
                        color = FleetAmber,
                    )
                }
            }
        }

        item {
            Button(
                onClick = { showPreview = true },
                enabled = selectedCategory != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) { Text(if (selectedCategory == null) "Choose a category to continue" else "Review report") }
        }

        item {
            Text(
                "No account is required to prepare or share the report. GitHub requires an account to post an issue; " +
                    "the Android share sheet, copy action, and Email Feedback & Support flow work without one.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showPreview && issue != null) {
        ReportPreviewDialog(
            title = "Review bug report",
            report = report,
            onDismiss = { showPreview = false },
            onPrimaryAction = {
                val url = FeedbackReportBuilder.issueUrl(issue)
                if (SupportActions.openUrl(context, url)) {
                    toast("Opening GitHub. FleetPulse cannot submit the issue for you.")
                    true
                } else {
                    toast("No browser app is available. Use Share or Copy instead.")
                    false
                }
            },
            onShare = {
                if (SupportActions.shareText(context, report, "Share FleetPulse bug report")) {
                    true
                } else {
                    toast("No app is available to share this report.")
                    false
                }
            },
            onCopy = {
                if (SupportActions.copyText(context, "FleetPulse bug report", report)) {
                    toast("Bug report copied.")
                    true
                } else {
                    toast("Could not copy the report.")
                    false
                }
            },
            onEmail = {
                if (SupportActions.email(context, SupportConfig.SUPPORT_EMAIL, "FleetPulse bug report", report)) {
                    true
                } else {
                    toast("No email app is available.")
                    false
                }
            },
        )
    }
}

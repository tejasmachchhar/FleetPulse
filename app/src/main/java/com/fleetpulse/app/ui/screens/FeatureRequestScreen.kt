package com.fleetpulse.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
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
import com.fleetpulse.app.support.DiagnosticSnapshot
import com.fleetpulse.app.support.DiagnosticsCollector
import com.fleetpulse.app.support.FeatureRequestDraft
import com.fleetpulse.app.support.FeedbackCategories
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
import com.fleetpulse.app.ui.theme.FleetViolet

@Composable
fun FeatureRequestScreen(
    origin: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val currentScreen = origin.ifBlank { "Feedback & Support" }
    val diagnostics: DiagnosticSnapshot = remember(currentScreen) {
        DiagnosticsCollector.collect(context, currentScreen)
    }
    var selectedLabel by rememberSaveable { mutableStateOf<String?>(null) }
    var suggestion by rememberSaveable { mutableStateOf("") }
    var problem by rememberSaveable { mutableStateOf("") }
    var expected by rememberSaveable { mutableStateOf("") }
    var includeAppInfo by rememberSaveable { mutableStateOf(true) }
    var includeEnvironment by rememberSaveable { mutableStateOf(true) }
    var showPreview by rememberSaveable { mutableStateOf(false) }

    val selectedCategory = FeedbackCategories.feature.firstOrNull { it.label == selectedLabel }
    val draft = selectedCategory?.let {
        FeatureRequestDraft(
            category = it,
            suggestion = suggestion.trim(),
            problem = problem.trim(),
            expected = expected.trim(),
            currentScreen = currentScreen,
        )
    }
    val report = draft?.let {
        FeedbackReportBuilder.fitReportForIssue(
            FeedbackReportBuilder.buildFeatureReport(
                draft = it,
                diagnostics = diagnostics,
                includeAppInfo = includeAppInfo,
                includeEnvironment = includeEnvironment,
            ),
        )
    }.orEmpty()
    val issue = draft?.let { FeedbackReportBuilder.featureIssue(it, report) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Suggest an Improvement",
                subtitle = "One short suggestion is enough",
                onBack = onBack,
            )
        }

        item {
            SectionCard(title = "Where did this come from?", subtitle = "Screen context only") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, null, tint = FleetAmber)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(currentScreen, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "No vehicle names, values or notes are included.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = "Category", subtitle = "Pick the closest fit") {
                FeedbackCategoryChips(
                    categories = FeedbackCategories.feature,
                    selected = selectedCategory,
                    onSelect = { selectedLabel = it.label },
                )
            }
        }

        item {
            SectionCard(title = "Your suggestion", subtitle = "Short and specific is best") {
                OutlinedTextField(
                    value = suggestion,
                    onValueChange = { if (it.length <= 2_000) suggestion = it },
                    label = { Text("What would you like to see?") },
                    placeholder = { Text("Example: show a monthly charging-cost estimate on the Dashboard.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnIme(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 3,
                    maxLines = 6,
                    supportingText = { Text("${suggestion.length}/2000") },
                )
            }
        }

        item {
            SectionCard(title = "Optional context", subtitle = "Only if it helps explain the idea") {
                OutlinedTextField(
                    value = problem,
                    onValueChange = { if (it.length <= 1_500) problem = it },
                    label = { Text("Problem it solves (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnIme(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 2,
                    maxLines = 4,
                )
                OutlinedTextField(
                    value = expected,
                    onValueChange = { if (it.length <= 1_500) expected = it },
                    label = { Text("Expected behaviour (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnIme(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 2,
                    maxLines = 4,
                )
            }
        }

        item {
            SectionCard(title = "App and device details", subtitle = "Optional, shown in the preview") {
                DiagnosticToggleRow(
                    title = "App version",
                    subtitle = "Version name, code and build type",
                    checked = includeAppInfo,
                    onCheckedChange = { includeAppInfo = it },
                )
                DiagnosticToggleRow(
                    title = "Device and environment",
                    subtitle = "Device model, Android version, screen and language",
                    checked = includeEnvironment,
                    onCheckedChange = { includeEnvironment = it },
                )
                Text(
                    "A feature request does not need logs, vehicle records or personal usage data.",
                    style = MaterialTheme.typography.labelSmall,
                    color = FleetGreen,
                )
            }
        }

        item {
            Button(
                onClick = { showPreview = true },
                enabled = selectedCategory != null && suggestion.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    when {
                        selectedCategory == null -> "Choose a category to continue"
                        suggestion.isBlank() -> "Add a short suggestion to continue"
                        else -> "Review suggestion"
                    },
                )
            }
        }

        item {
            Text(
                "You can open a prefilled GitHub issue, share the text through Android, or copy it. " +
                    "FleetPulse does not create a GitHub account or post on your behalf.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showPreview && issue != null) {
        ReportPreviewDialog(
            title = "Review suggestion",
            report = report,
            onDismiss = { showPreview = false },
            onOpenGitHub = {
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
                if (SupportActions.shareText(context, report, "Share FleetPulse suggestion")) {
                    true
                } else {
                    toast("No app is available to share this suggestion.")
                    false
                }
            },
            onCopy = {
                if (SupportActions.copyText(context, "FleetPulse suggestion", report)) {
                    toast("Suggestion copied.")
                    true
                } else {
                    toast("Could not copy the suggestion.")
                    false
                }
            },
            onEmail = SupportConfig.SUPPORT_EMAIL?.let { address ->
                {
                    if (SupportActions.email(context, address, "FleetPulse feature request", report)) {
                        true
                    } else {
                        toast("No email app is available.")
                        false
                    }
                }
            },
        )
    }
}

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
import androidx.compose.material.icons.filled.ChatBubbleOutline
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
import com.fleetpulse.app.support.FeedbackReportBuilder
import com.fleetpulse.app.support.GeneralFeedbackDraft
import com.fleetpulse.app.support.SupportActions
import com.fleetpulse.app.support.SupportConfig
import com.fleetpulse.app.ui.components.DiagnosticToggleRow
import com.fleetpulse.app.ui.components.ReportPreviewDialog
import com.fleetpulse.app.ui.components.ScreenHeader
import com.fleetpulse.app.ui.components.SectionCard
import com.fleetpulse.app.ui.components.bringIntoViewOnIme
import com.fleetpulse.app.ui.theme.FleetBlue
import com.fleetpulse.app.ui.theme.FleetGreen

@Composable
fun GeneralFeedbackScreen(
    origin: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val currentScreen = origin.ifBlank { "Feedback & Support" }
    val diagnostics: DiagnosticSnapshot = remember(currentScreen) {
        DiagnosticsCollector.collect(context, currentScreen)
    }
    var message by rememberSaveable { mutableStateOf("") }
    var includeAppInfo by rememberSaveable { mutableStateOf(true) }
    var includeEnvironment by rememberSaveable { mutableStateOf(true) }
    var showPreview by rememberSaveable { mutableStateOf(false) }

    val draft = GeneralFeedbackDraft(message = message.trim(), currentScreen = currentScreen)
    val report = FeedbackReportBuilder.fitReportForIssue(
        FeedbackReportBuilder.buildGeneralFeedback(
            draft = draft,
            diagnostics = diagnostics,
            includeAppInfo = includeAppInfo,
            includeEnvironment = includeEnvironment,
        ),
    )
    val issue = FeedbackReportBuilder.generalIssue(draft, report)

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Send General Feedback",
                subtitle = "A short note is enough",
                onBack = onBack,
            )
        }

        item {
            SectionCard(title = "Your feedback", subtitle = "No account, no long form") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ChatBubbleOutline, null, tint = FleetBlue)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "This report will be shared from $currentScreen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = message,
                    onValueChange = { if (it.length <= 4_000) message = it },
                    label = { Text("What would you like to tell us?") },
                    placeholder = { Text("Example: the new support screen is clear. Thanks.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnIme(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 5,
                    maxLines = 10,
                    supportingText = {
                        Text("${message.length}/4000 · Do not include real vehicle or personal data.")
                    },
                )
            }
        }

        item {
            SectionCard(title = "Optional details", subtitle = "You can remove either part") {
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
                    "General feedback never includes vehicle records, values, notes, logs or database contents.",
                    style = MaterialTheme.typography.labelSmall,
                    color = FleetGreen,
                )
            }
        }

        item {
            Button(
                onClick = { showPreview = true },
                enabled = message.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) { Text(if (message.isBlank()) "Write a short message to continue" else "Review feedback") }
        }

        item {
            Text(
                "Open a prefilled GitHub issue, share the text, or copy it. " +
                    "FleetPulse never posts it for you.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showPreview && message.isNotBlank()) {
        ReportPreviewDialog(
            title = "Review feedback",
            report = report,
            onDismiss = { showPreview = false },
            onPrimaryAction = {
                if (SupportActions.openUrl(context, FeedbackReportBuilder.issueUrl(issue))) {
                    toast("Opening GitHub. FleetPulse cannot submit the issue for you.")
                    true
                } else {
                    toast("No browser app is available. Use Share or Copy instead.")
                    false
                }
            },
            onShare = {
                if (SupportActions.shareText(context, report, "Share FleetPulse feedback")) {
                    true
                } else {
                    toast("No app is available to share this feedback.")
                    false
                }
            },
            onCopy = {
                if (SupportActions.copyText(context, "FleetPulse feedback", report)) {
                    toast("Feedback copied.")
                    true
                } else {
                    toast("Could not copy the feedback.")
                    false
                }
            },
            onEmail = {
                if (SupportActions.email(context, SupportConfig.SUPPORT_EMAIL, "FleetPulse feedback", report)) {
                    true
                } else {
                    toast("No email app is available.")
                    false
                }
            },
        )
    }
}

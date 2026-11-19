package com.fleetpulse.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fleetpulse.app.support.DiagnosticSnapshot
import com.fleetpulse.app.support.DiagnosticsCollector
import com.fleetpulse.app.support.EmailBugDraft
import com.fleetpulse.app.support.EmailDraft
import com.fleetpulse.app.support.FeatureRequestDraft
import com.fleetpulse.app.support.FeedbackCategories
import com.fleetpulse.app.support.FeedbackReportBuilder
import com.fleetpulse.app.support.GeneralFeedbackDraft
import com.fleetpulse.app.support.SupportActions
import com.fleetpulse.app.support.SupportConfig
import com.fleetpulse.app.ui.components.DiagnosticToggleRow
import com.fleetpulse.app.ui.components.FeedbackCategoryChips
import com.fleetpulse.app.ui.components.ReportPreviewDialog
import com.fleetpulse.app.ui.components.ScreenHeader
import com.fleetpulse.app.ui.components.SectionCard
import com.fleetpulse.app.ui.components.bringIntoViewOnIme
import com.fleetpulse.app.ui.theme.FleetBlue
import com.fleetpulse.app.ui.theme.FleetGreen

private data class EmailKindOption(
    val id: String,
    val label: String,
)

private val emailKindOptions = listOf(
    EmailKindOption("bug", "Report a Bug"),
    EmailKindOption("feature", "Suggest an Improvement"),
    EmailKindOption("general", "General Feedback"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmailFeedbackScreen(
    origin: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val currentScreen = origin.ifBlank { "Feedback & Support" }
    val diagnostics: DiagnosticSnapshot = remember(currentScreen) {
        DiagnosticsCollector.collect(context, currentScreen)
    }

    var kind by rememberSaveable { mutableStateOf("bug") }
    var selectedBugLabel by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFeatureLabel by rememberSaveable { mutableStateOf<String?>(null) }
    var description by rememberSaveable { mutableStateOf("") }
    var steps by rememberSaveable { mutableStateOf("") }
    var expected by rememberSaveable { mutableStateOf("") }
    var actual by rememberSaveable { mutableStateOf("") }
    var suggestion by rememberSaveable { mutableStateOf("") }
    var problem by rememberSaveable { mutableStateOf("") }
    var benefit by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var includeAppInfo by rememberSaveable { mutableStateOf(true) }
    var includeEnvironment by rememberSaveable { mutableStateOf(true) }
    var includeLogs by rememberSaveable { mutableStateOf(true) }
    var showPreview by rememberSaveable { mutableStateOf(false) }
    var showEmailFallback by rememberSaveable { mutableStateOf(false) }

    val bugCategory = FeedbackCategories.bug.firstOrNull { it.label == selectedBugLabel }
    val featureCategory = FeedbackCategories.feature.firstOrNull { it.label == selectedFeatureLabel }
    val email: EmailDraft? = when (kind) {
        "bug" -> bugCategory?.let {
            FeedbackReportBuilder.buildBugEmail(
                draft = EmailBugDraft(
                    category = it,
                    description = description.trim(),
                    steps = steps.trim(),
                    expected = expected.trim(),
                    actual = actual.trim(),
                    currentScreen = currentScreen,
                ),
                diagnostics = diagnostics,
                includeAppInfo = includeAppInfo,
                includeEnvironment = includeEnvironment,
                includeLogs = includeLogs,
            )
        }
        "feature" -> featureCategory?.takeIf { suggestion.isNotBlank() }?.let {
            FeedbackReportBuilder.buildFeatureEmail(
                draft = FeatureRequestDraft(
                    category = it,
                    suggestion = suggestion.trim(),
                    problem = problem.trim(),
                    expected = benefit.trim(),
                    currentScreen = currentScreen,
                ),
                diagnostics = diagnostics,
                includeAppInfo = includeAppInfo,
                includeEnvironment = includeEnvironment,
            )
        }
        "general" -> if (message.isNotBlank()) {
            FeedbackReportBuilder.buildGeneralEmail(
                draft = GeneralFeedbackDraft(
                    message = message.trim(),
                    currentScreen = currentScreen,
                ),
                diagnostics = diagnostics,
                includeAppInfo = includeAppInfo,
                includeEnvironment = includeEnvironment,
            )
        } else {
            null
        }
        else -> null
    }
    val previewText = email?.let { "Subject: ${it.subject}\n\n${it.body}" }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    fun copyAddress() {
        if (SupportActions.copyText(context, "FleetPulse support email", SupportConfig.SUPPORT_EMAIL)) {
            toast("Support email copied.")
        } else {
            toast("Could not copy the support email.")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Email Feedback & Support",
                subtitle = "No GitHub account needed. Review the draft before it leaves FleetPulse.",
                onBack = onBack,
            )
        }

        item {
            SectionCard(
                title = "What would you like to send?",
                subtitle = "The recipient and subject are filled in for you",
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    emailKindOptions.forEach { option ->
                        FilterChip(
                            selected = kind == option.id,
                            onClick = { kind = option.id },
                            label = { Text(option.label) },
                            leadingIcon = {
                                Icon(
                                    when (option.id) {
                                        "bug" -> Icons.Default.BugReport
                                        "feature" -> Icons.Default.Lightbulb
                                        else -> Icons.Default.ChatBubbleOutline
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                )
                            },
                        )
                    }
                }
            }
        }

        when (kind) {
            "bug" -> {
                item {
                    SectionCard(title = "Bug category", subtitle = "Pick the closest match") {
                        FeedbackCategoryChips(
                            categories = FeedbackCategories.bug,
                            selected = bugCategory,
                            onSelect = { selectedBugLabel = it.label },
                        )
                    }
                }
                item {
                    SectionCard(title = "Describe the problem", subtitle = "A sentence is enough") {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { if (it.length <= 2_000) description = it },
                            label = { Text("What happened? (optional)") },
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
                        OutlinedTextField(
                            value = steps,
                            onValueChange = { if (it.length <= 1_500) steps = it },
                            label = { Text("Steps to reproduce (optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnIme(),
                            shape = RoundedCornerShape(14.dp),
                            minLines = 2,
                            maxLines = 5,
                        )
                        OutlinedTextField(
                            value = expected,
                            onValueChange = { if (it.length <= 1_000) expected = it },
                            label = { Text("Expected behaviour (optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnIme(),
                            shape = RoundedCornerShape(14.dp),
                            minLines = 2,
                            maxLines = 4,
                        )
                        OutlinedTextField(
                            value = actual,
                            onValueChange = { if (it.length <= 1_000) actual = it },
                            label = { Text("Actual behaviour (optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnIme(),
                            shape = RoundedCornerShape(14.dp),
                            minLines = 2,
                            maxLines = 4,
                        )
                    }
                }
            }
            "feature" -> {
                item {
                    SectionCard(title = "Suggestion category", subtitle = "Pick the closest match") {
                        FeedbackCategoryChips(
                            categories = FeedbackCategories.feature,
                            selected = featureCategory,
                            onSelect = { selectedFeatureLabel = it.label },
                        )
                    }
                }
                item {
                    SectionCard(title = "Your suggestion", subtitle = "Short and specific is best") {
                        OutlinedTextField(
                            value = suggestion,
                            onValueChange = { if (it.length <= 2_000) suggestion = it },
                            label = { Text("What would you like to see?") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnIme(),
                            shape = RoundedCornerShape(14.dp),
                            minLines = 3,
                            maxLines = 6,
                            supportingText = { Text("${suggestion.length}/2000") },
                        )
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
                            value = benefit,
                            onValueChange = { if (it.length <= 1_500) benefit = it },
                            label = { Text("Expected benefit (optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewOnIme(),
                            shape = RoundedCornerShape(14.dp),
                            minLines = 2,
                            maxLines = 4,
                        )
                    }
                }
            }
            else -> {
                item {
                    SectionCard(title = "Your feedback", subtitle = "A short note is enough") {
                        OutlinedTextField(
                            value = message,
                            onValueChange = { if (it.length <= 4_000) message = it },
                            label = { Text("What would you like to tell us?") },
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
            }
        }

        item {
            SectionCard(
                title = "Optional details",
                subtitle = "Automatically filled in, and easy to remove",
            ) {
                DiagnosticToggleRow(
                    title = "App version",
                    subtitle = "Version name, version code and build type",
                    checked = includeAppInfo,
                    onCheckedChange = { includeAppInfo = it },
                )
                DiagnosticToggleRow(
                    title = "Device and Android version",
                    subtitle = "Device model, Android version, screen, language and timestamp",
                    checked = includeEnvironment,
                    onCheckedChange = { includeEnvironment = it },
                )
                if (kind == "bug") {
                    DiagnosticToggleRow(
                        title = "Recent diagnostics",
                        subtitle = "Redacted error type, stack frames and local log lines",
                        checked = includeLogs,
                        onCheckedChange = { includeLogs = it },
                    )
                }
                Text(
                    "No vehicle records, registration numbers, odometer readings, expenses, notes, backups, " +
                        "location or files are added automatically. The preview shows the exact email text.",
                    style = MaterialTheme.typography.labelSmall,
                    color = FleetGreen,
                )
            }
        }

        item {
            Button(
                onClick = { showPreview = true },
                enabled = email != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    when {
                        kind == "bug" && bugCategory == null -> "Choose a bug category to continue"
                        kind == "feature" && featureCategory == null -> "Choose a suggestion category to continue"
                        kind == "feature" && suggestion.isBlank() -> "Add a short suggestion to continue"
                        kind == "general" && message.isBlank() -> "Write a short message to continue"
                        else -> "Review email draft"
                    },
                )
            }
        }

        item {
            SectionCard(title = "Send to", subtitle = "The email app opens with this address filled in") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, null, tint = FleetBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Support email", style = MaterialTheme.typography.labelSmall)
                        Text(
                            SupportConfig.SUPPORT_EMAIL,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    OutlinedButton(
                        onClick = { copyAddress() },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Copy")
                    }
                }
                Text(
                    "FleetPulse only opens your email app. It cannot send the email for you or confirm delivery. " +
                        "You can edit the draft before sending, and if no email app is installed the next screen " +
                        "offers Copy and Share instead.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showPreview && email != null && previewText != null) {
        ReportPreviewDialog(
            title = "Review email draft",
            report = previewText,
            onDismiss = { showPreview = false },
            primaryActionLabel = "Open email app",
            primaryActionHint = "Your email app opens with the recipient, subject and body filled in. " +
                "FleetPulse cannot send the email or confirm delivery.",
            onPrimaryAction = {
                if (SupportActions.email(
                        context = context,
                        address = SupportConfig.SUPPORT_EMAIL,
                        subject = email.subject,
                        body = email.body,
                    )
                ) {
                    true
                } else {
                    showEmailFallback = true
                    true
                }
            },
            onShare = {
                if (SupportActions.shareText(context, previewText, "Share FleetPulse email draft")) {
                    true
                } else {
                    toast("No app is available to share this email draft.")
                    false
                }
            },
            onCopy = {
                if (SupportActions.copyText(context, "FleetPulse email draft", previewText)) {
                    toast("Email draft copied.")
                    true
                } else {
                    toast("Could not copy the email draft.")
                    false
                }
            },
            onEmail = null,
        )
    }

    if (showEmailFallback && previewText != null) {
        AlertDialog(
            onDismissRequest = { showEmailFallback = false },
            title = { Text("No email app found") },
            text = {
                Column {
                    Text(
                        "No compatible email app was found on this device. You can copy the draft, share it " +
                            "with another app, or copy the support address and write from a computer.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = {
                        copyAddress()
                        showEmailFallback = false
                    }) { Text("Copy support address") }
                    TextButton(onClick = {
                        if (SupportActions.shareText(context, previewText, "Share FleetPulse email draft")) {
                            showEmailFallback = false
                        } else {
                            toast("No app is available to share this email draft.")
                        }
                    }) { Text("Share draft") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (SupportActions.copyText(context, "FleetPulse email draft", previewText)) {
                        toast("Email draft copied.")
                        showEmailFallback = false
                    } else {
                        toast("Could not copy the email draft.")
                    }
                }) { Text("Copy email") }
            },
            dismissButton = { TextButton(onClick = { showEmailFallback = false }) { Text("Close") } },
        )
    }
}

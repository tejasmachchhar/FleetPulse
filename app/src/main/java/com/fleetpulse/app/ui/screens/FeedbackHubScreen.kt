package com.fleetpulse.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fleetpulse.app.support.BuildInfo
import com.fleetpulse.app.support.SupportActions
import com.fleetpulse.app.support.SupportConfig
import com.fleetpulse.app.ui.components.FeedbackActionCard
import com.fleetpulse.app.ui.components.ScreenHeader
import com.fleetpulse.app.ui.components.SectionCard
import com.fleetpulse.app.ui.theme.FleetAmber
import com.fleetpulse.app.ui.theme.FleetBlue
import com.fleetpulse.app.ui.theme.FleetGreen
import com.fleetpulse.app.ui.theme.FleetRed

@Composable
fun FeedbackHubScreen(
    onBack: () -> Unit,
    onBugReport: () -> Unit,
    onFeatureRequest: () -> Unit,
    onGeneralFeedback: () -> Unit,
    onSupport: () -> Unit,
) {
    val context = LocalContext.current

    fun open(url: String) {
        if (!SupportActions.openUrl(context, url)) {
            Toast.makeText(context, "No app is available to open this link.", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Feedback & Support",
                subtitle = "Minimal typing. You control what is shared.",
                onBack = onBack,
            )
        }

        item {
            SectionCard(
                title = "No account needed to prepare a report",
                subtitle = "Diagnostics stay local until you choose a destination",
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.PrivacyTip,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "FleetPulse prepares a structured report on this phone. You see the exact text before it " +
                            "leaves the app, can remove optional details, and decide whether to open GitHub, use the " +
                            "Android share sheet, or copy it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "Never included automatically: vehicle names or registrations, odometer readings, fuel/charging " +
                        "records, expenses, notes, database contents, location, files, or account credentials.",
                    style = MaterialTheme.typography.labelSmall,
                    color = FleetGreen,
                )
            }
        }

        item {
            Text(
                "Tell us what happened",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        item {
            FeedbackActionCard(
                icon = Icons.Default.BugReport,
                title = "Report a Bug",
                subtitle = "Crashes, wrong calculations, missing data or layout issues",
                tint = FleetRed,
                onClick = onBugReport,
            )
        }
        item {
            FeedbackActionCard(
                icon = Icons.Default.Lightbulb,
                title = "Suggest an Improvement",
                subtitle = "A new feature or a better way to do something",
                tint = FleetAmber,
                onClick = onFeatureRequest,
            )
        }
        item {
            FeedbackActionCard(
                icon = Icons.Default.ChatBubbleOutline,
                title = "Send General Feedback",
                subtitle = "A short note without filling in a long form",
                tint = FleetBlue,
                onClick = onGeneralFeedback,
            )
        }
        item {
            FeedbackActionCard(
                icon = Icons.Default.FavoriteBorder,
                title = "Support FleetPulse",
                subtitle = "Optional UPI, Buy Me a Coffee, or GitHub Sponsors when configured",
                tint = FleetGreen,
                onClick = onSupport,
            )
        }

        item {
            SectionCard(title = "Project links", subtitle = "Opened in your browser or the matching app") {
                ProjectLinkRow(
                    icon = Icons.Default.Code,
                    title = "GitHub Repository",
                    subtitle = "Source, issues and releases",
                    tint = FleetBlue,
                    onClick = { open(SupportConfig.REPOSITORY_URL) },
                )
                ProjectLinkRow(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Documentation / User Guide",
                    subtitle = "Install, entries, backups and calculations",
                    tint = FleetAmber,
                    onClick = { open(SupportConfig.DOCUMENTATION_URL) },
                )
                ProjectLinkRow(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy Information",
                    subtitle = "What stays local and what a report contains",
                    tint = FleetGreen,
                    onClick = { open(SupportConfig.PRIVACY_URL) },
                )
                ProjectLinkRow(
                    icon = Icons.Default.NewReleases,
                    title = "Release Information",
                    subtitle = "Download the latest APK and checksums",
                    tint = FleetBlue,
                    onClick = { open(SupportConfig.RELEASES_URL) },
                )
            }
        }

        item {
            SectionCard(title = "Current app version") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "FleetPulse ${BuildInfo.VERSION_NAME} (versionCode ${BuildInfo.VERSION_CODE}) · " +
                            "${BuildInfo.BUILD_TYPE} build",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(tint.copy(alpha = 0.13f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp),
        )
    }
}

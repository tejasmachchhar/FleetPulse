package com.fleetpulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    onEmailFeedback: () -> Unit,
    onSupport: () -> Unit,
) {
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
                            "leaves the app, can remove optional details, and decide whether to open GitHub, email " +
                            "it, use the Android share sheet, or copy it.",
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
                icon = Icons.Default.Email,
                title = "Email Feedback & Support",
                subtitle = "Don't use GitHub? Send a bug, idea or note by email",
                tint = FleetGreen,
                onClick = onEmailFeedback,
            )
        }
        item {
            FeedbackActionCard(
                icon = Icons.Default.FavoriteBorder,
                title = "Support FleetPulse",
                subtitle = "Optional UPI or Buy Me a Coffee support",
                tint = FleetGreen,
                onClick = onSupport,
            )
        }

        item {
            Text(
                "Project links, the changelog and this build's version are in More → About.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

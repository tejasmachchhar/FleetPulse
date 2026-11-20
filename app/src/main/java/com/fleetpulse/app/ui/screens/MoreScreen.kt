package com.fleetpulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fleetpulse.app.data.prefs.AppPreferencesState
import com.fleetpulse.app.ui.components.ScreenHeader
import com.fleetpulse.app.ui.components.SectionCard
import com.fleetpulse.app.ui.theme.FleetAmber
import com.fleetpulse.app.ui.theme.FleetBlue
import com.fleetpulse.app.ui.theme.FleetGreen
import com.fleetpulse.app.ui.theme.FleetViolet

@Composable
fun MoreScreen(
    prefs: AppPreferencesState,
    sampleTotal: Int,
    onCalibration: () -> Unit,
    onSettings: () -> Unit,
    onData: () -> Unit,
    onGuide: () -> Unit,
    onGettingStarted: () -> Unit = {},
    onVerification: () -> Unit,
    onFeedback: () -> Unit,
    onAbout: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "More",
                subtitle = "Calibration, settings, backups and verification",
            )
        }

        if (sampleTotal > 0) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(FleetAmber.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Science, null, tint = FleetAmber)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Sample Mode is active", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "$sampleTotal reference records are loaded. They are marked [SAMPLE] and can be removed " +
                                "from Data Management without touching your own records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = "Tools") {
                MenuRow(
                    icon = Icons.Default.Tune,
                    title = "Calibration",
                    subtitle = "Voltage → battery % curve per EV",
                    tint = FleetViolet,
                    onClick = onCalibration,
                )
                MenuRow(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    subtitle = "Tariffs, charger efficiency, alerts and appearance",
                    tint = FleetBlue,
                    onClick = onSettings,
                )
                MenuRow(
                    icon = Icons.Default.Folder,
                    title = "Data Management",
                    subtitle = "Backups, CSV exports, sample data",
                    tint = FleetGreen,
                    onClick = onData,
                )
            }
        }

        item {
            SectionCard(title = "Help & feedback") {
                MenuRow(
                    icon = Icons.Default.Info,
                    title = "Getting Started",
                    subtitle = "Replay the first-time tutorial",
                    tint = FleetBlue,
                    onClick = onGettingStarted,
                )
                MenuRow(
                    icon = Icons.Default.SupportAgent,
                    title = "Feedback & Support",
                    subtitle = "Report a bug, suggest an improvement, or support the project",
                    tint = FleetBlue,
                    onClick = onFeedback,
                )
            }
        }

        item {
            SectionCard(title = "Verify the maths") {
                MenuRow(
                    icon = Icons.Default.Verified,
                    title = "Calculation Verification",
                    subtitle = "Run the built-in suite against reference values",
                    tint = FleetGreen,
                    onClick = onVerification,
                )
                MenuRow(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "User Guide",
                    subtitle = "How every number is calculated",
                    tint = FleetAmber,
                    onClick = onGuide,
                )
            }
        }

        item {
            SectionCard(title = "About FleetPulse") {
                MenuRow(
                    icon = Icons.Default.Info,
                    title = "About FleetPulse",
                    subtitle = "Version, project links, changelog and licence",
                    tint = FleetBlue,
                    onClick = onAbout,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Bolt, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("FleetPulse", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Native Android edition · offline by design",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudOff, null, tint = FleetGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "No INTERNET permission, no accounts, no analytics. Your data stays in the local Room database " +
                            "and in the backups you choose to export.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "Theme: ${prefs.themeMode} · Notifications: ${if (prefs.notificationsEnabled) "on" else "off"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MenuRow(
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
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(tint.copy(alpha = 0.13f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
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
            modifier = Modifier.size(20.dp),
        )
    }
}

package com.fleetpulse.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fleetpulse.app.support.BuildInfo
import com.fleetpulse.app.support.SupportActions
import com.fleetpulse.app.support.SupportConfig
import com.fleetpulse.app.ui.components.ProjectLinkRow
import com.fleetpulse.app.ui.components.ScreenHeader
import com.fleetpulse.app.ui.components.SectionCard
import com.fleetpulse.app.ui.theme.FleetAmber
import com.fleetpulse.app.ui.theme.FleetBlue
import com.fleetpulse.app.ui.theme.FleetGreen
import com.fleetpulse.app.ui.theme.FleetViolet

@Composable
fun AboutScreen(
    onBack: () -> Unit,
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
                title = "About FleetPulse",
                subtitle = "Project links, release information and build details",
                onBack = onBack,
            )
        }

        item {
            SectionCard(title = "FleetPulse") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(27.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "FleetPulse",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Native Android edition · offline by design",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    "Offline-first tracking for electric and petrol two-wheelers: mileage, energy and fuel use, " +
                        "maintenance, running costs, and local backups.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudOff, null, tint = FleetGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "No INTERNET permission, no accounts, no analytics. Your records stay in the local " +
                            "Room database and in the backups you choose to export.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            SectionCard(
                title = "Project links",
                subtitle = "Opened in your browser or the matching app",
            ) {
                ProjectLinkRow(
                    icon = Icons.Default.Code,
                    title = "GitHub Repository",
                    subtitle = "Source code, issues and releases",
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
                    icon = Icons.Default.Description,
                    title = "Changelog / Release Notes",
                    subtitle = "What changed in each published version",
                    tint = FleetViolet,
                    onClick = { open(SupportConfig.CHANGELOG_URL) },
                )
                ProjectLinkRow(
                    icon = Icons.Default.NewReleases,
                    title = "Latest Release",
                    subtitle = "Download the latest APK and checksums",
                    tint = FleetBlue,
                    onClick = { open(SupportConfig.RELEASES_URL) },
                )
                ProjectLinkRow(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy Information",
                    subtitle = "What stays local and what a report contains",
                    tint = FleetGreen,
                    onClick = { open(SupportConfig.PRIVACY_URL) },
                )
                ProjectLinkRow(
                    icon = Icons.Default.SupportAgent,
                    title = "Support & Reporting",
                    subtitle = "How to report a problem without sharing private data",
                    tint = FleetAmber,
                    onClick = { open(SupportConfig.SUPPORT_URL) },
                )
            }
        }

        item {
            SectionCard(title = "This build") {
                Text(
                    "FleetPulse ${BuildInfo.VERSION_NAME} (versionCode ${BuildInfo.VERSION_CODE}) · " +
                        "${BuildInfo.BUILD_TYPE} build",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Package: ${BuildInfo.PACKAGE_NAME} · Android 8.0+",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "FleetPulse is open source under the MIT License. Project links above are the verified public " +
                        "destinations for this build.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

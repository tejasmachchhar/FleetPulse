package com.fleetpulse.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fleetpulse.app.ui.theme.FleetAmber
import com.fleetpulse.app.ui.theme.FleetBlue
import com.fleetpulse.app.ui.theme.FleetGreen
import com.fleetpulse.app.ui.theme.FleetOrange
import com.fleetpulse.app.ui.theme.FleetViolet

@Composable
fun OnboardingScreen(
    onAddVehicle: () -> Unit,
    onRecordUsage: () -> Unit,
    onExploreDashboard: () -> Unit,
    onViewMaintenance: () -> Unit,
    onLoadDemo: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val pageCount = 7

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Step ${page + 1} of $pageCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onSkip) { Text("Skip") }
        }

        Spacer(Modifier.height(8.dp))
        ProgressDots(page = page, pageCount = pageCount)

        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            when (page) {
                0 -> WelcomePage(onLoadDemo = onLoadDemo)
                1 -> AddVehiclePage(onAddVehicle = onAddVehicle)
                2 -> UsagePage(onRecordUsage = onRecordUsage)
                3 -> AnalyticsPage()
                4 -> MaintenancePage(onViewMaintenance = onViewMaintenance)
                5 -> BackupPage()
                else -> ReadyPage(
                    onAddVehicle = onAddVehicle,
                    onRecordUsage = onRecordUsage,
                    onExploreDashboard = onExploreDashboard,
                    onViewMaintenance = onViewMaintenance,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (page > 0) {
                OutlinedButton(
                    onClick = { page -= 1 },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Back") }
            }
            Button(
                onClick = {
                    if (page == pageCount - 1) onFinish() else page += 1
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(if (page == pageCount - 1) "Finish" else "Next")
            }
        }
    }
}

@Composable
private fun ProgressDots(page: Int, pageCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        if (index <= page) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(999.dp),
                    ),
            )
        }
    }
}

@Composable
private fun WelcomePage(onLoadDemo: () -> Unit) {
    OnboardingHero(
        icon = Icons.Default.Bolt,
        title = "Welcome to FleetPulse",
        body = "Track vehicles, fuel or charging, mileage, running costs, maintenance, and service history. " +
            "Everything stays on this phone: no account, no cloud, no analytics.",
    )
    Spacer(Modifier.height(18.dp))
    Bullet("Add one or more vehicles and their battery or tank details.")
    Bullet("Log odometer, fuel, charging, and expense entries.")
    Bullet("See mileage, cost per kilometre, trends, and maintenance due dates.")
    Bullet("Export a backup whenever you want.")
    Spacer(Modifier.height(18.dp))
    OutlinedButton(
        onClick = onLoadDemo,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
    ) {
        Icon(Icons.Default.Science, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Explore with sample data")
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Sample records are clearly marked and can be removed later without touching your own data.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AddVehiclePage(onAddVehicle: () -> Unit) {
    OnboardingHero(
        icon = Icons.AutoMirrored.Filled.DirectionsBike,
        title = "Add your first vehicle",
        body = "Create a vehicle profile so FleetPulse knows what you drive and how it is powered.",
    )
    Spacer(Modifier.height(18.dp))
    Bullet("Required: vehicle name, type, energy source, and current odometer.")
    Bullet("Optional: registration number, colour, notes, battery capacity, tank capacity.")
    Bullet("You can add electric and petrol vehicles in the same app.")
    Bullet("Each vehicle keeps its own usage, cost, and maintenance history.")
    Spacer(Modifier.height(18.dp))
    Button(
        onClick = onAddVehicle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.DirectionsBike, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Add my first vehicle")
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "You can also skip this step and add a vehicle later from the Vehicles tab.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun UsagePage(onRecordUsage: () -> Unit) {
    OnboardingHero(
        icon = Icons.Default.Speed,
        title = "Record usage",
        body = "Log odometer readings, fuel or charging, expenses, and notes. Entries drive every calculation.",
    )
    Spacer(Modifier.height(18.dp))
    Bullet("Use a full tank or full charge when you want a verified mileage result.")
    Bullet("Partial refills are still recorded for cost and history.")
    Bullet("Duplicate or implausible odometer readings are flagged for review.")
    Bullet("Excluded entries stay in history but do not affect mileage.")
    Spacer(Modifier.height(18.dp))
    Button(
        onClick = onRecordUsage,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
    ) {
        Icon(Icons.Default.Speed, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Open usage entry")
    }
}

@Composable
private fun AnalyticsPage() {
    OnboardingHero(
        icon = Icons.Default.BarChart,
        title = "Understand mileage and costs",
        body = "FleetPulse calculates efficiency from complete fill intervals and separates measured results from estimates.",
    )
    Spacer(Modifier.height(18.dp))
    Bullet("Verified mileage comes from full-to-full intervals.")
    Bullet("Provisional estimates are shown separately, not mixed silently.")
    Bullet("Cost per kilometre, EV battery wear, and running-cost splits are available.")
    Bullet("Charts and comparisons improve as more complete entries are added.")
}

@Composable
private fun MaintenancePage(onViewMaintenance: () -> Unit) {
    OnboardingHero(
        icon = Icons.Default.Build,
        title = "Track maintenance",
        body = "Keep service records, costs, and upcoming work in one place.",
    )
    Spacer(Modifier.height(18.dp))
    Bullet("Add maintenance entries with category, cost, date, and odometer.")
    Bullet("Track upcoming and overdue service by date or distance.")
    Bullet("Maintenance cost is included in running-cost analysis.")
    Bullet("Local reminders can notify you when service is due.")
    Spacer(Modifier.height(18.dp))
    Button(
        onClick = onViewMaintenance,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
    ) {
        Icon(Icons.Default.Build, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("View maintenance")
    }
}

@Composable
private fun BackupPage() {
    OnboardingHero(
        icon = Icons.Default.Shield,
        title = "Keep your data safe",
        body = "FleetPulse stores data locally on this phone. There is no cloud account or automatic sync.",
    )
    Spacer(Modifier.height(18.dp))
    Bullet("Create a JSON backup from Data Management when you want one.")
    Bullet("Restore a backup after confirming the replacement.")
    Bullet("Export CSV files for your own records.")
    Bullet("Keep the backup file somewhere safe; FleetPulse cannot recover lost local data.")
}

@Composable
private fun ReadyPage(
    onAddVehicle: () -> Unit,
    onRecordUsage: () -> Unit,
    onExploreDashboard: () -> Unit,
    onViewMaintenance: () -> Unit,
) {
    OnboardingHero(
        icon = Icons.Default.Bolt,
        title = "You're ready",
        body = "Start with a vehicle, then log your first usage entry. You can replay this tutorial from Settings.",
    )
    Spacer(Modifier.height(18.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onAddVehicle,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Add vehicle") }
        OutlinedButton(
            onClick = onRecordUsage,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Record usage") }
    }
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onExploreDashboard,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Dashboard") }
        OutlinedButton(
            onClick = onViewMaintenance,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Maintenance") }
    }
    Spacer(Modifier.height(12.dp))
    Text(
        "You can also load reference sample data from More → Data Management.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun OnboardingHero(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(38.dp),
        )
    }
    Spacer(Modifier.height(14.dp))
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Bullet(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

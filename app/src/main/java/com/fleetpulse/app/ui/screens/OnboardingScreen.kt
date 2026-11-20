package com.fleetpulse.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudOff
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

data class OnboardingState(
    val page: Int = 0,
    val minimized: Boolean = false,
    val vehicleAdded: Boolean = false,
    val usageRecorded: Boolean = false,
)

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onSetState: (OnboardingState) -> Unit,
    onAddVehicle: () -> Unit,
    onRecordUsage: () -> Unit,
    onExploreDashboard: () -> Unit,
    onViewMaintenance: () -> Unit,
    onLoadDemo: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    onMinimize: () -> Unit,
) {
    val page = state.page
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
                1 -> AddVehiclePage(
                    vehicleAdded = state.vehicleAdded,
                    onAddVehicle = { onMinimize(); onAddVehicle() },
                )
                2 -> UsagePage(onRecordUsage = { onMinimize(); onRecordUsage() })
                3 -> AnalyticsPage()
                4 -> MaintenancePage(onViewMaintenance = onViewMaintenance)
                5 -> BackupPage()
                else -> ReadyPage(
                    onAddVehicle = onAddVehicle,
                    onRecordUsage = { onMinimize(); onRecordUsage() },
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
                    onClick = { onSetState(state.copy(page = page - 1)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Back") }
            }
            Button(
                onClick = {
                    if (page == pageCount - 1) onFinish() else onSetState(state.copy(page = page + 1))
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
            ) { Text(if (page == pageCount - 1) "Finish" else "Next") }
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
        Icon(Icons.Default.CloudOff, null, modifier = Modifier.size(18.dp))
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
private fun AddVehiclePage(vehicleAdded: Boolean, onAddVehicle: () -> Unit) {
    OnboardingHero(
        icon = Icons.AutoMirrored.Filled.DirectionsBike,
        title = "Add your first vehicle",
        body = if (vehicleAdded) "Good start — you can review or add more vehicles now." else
            "Create a vehicle profile so FleetPulse knows what you drive and how it is powered.",
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
        Text(if (vehicleAdded) "View my vehicle" else "Add my first vehicle")
    }
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
    Spacer(Modifier.height(18.dp))
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
        icon = Icons.Default.Storage,
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
        OutlinedButton(onClick = onAddVehicle, modifier = Modifier.weight(1f)) { Text("Add vehicle") }
        OutlinedButton(onClick = onRecordUsage, modifier = Modifier.weight(1f)) { Text("Record usage") }
    }
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(onClick = onExploreDashboard, modifier = Modifier.weight(1f)) { Text("Dashboard") }
        OutlinedButton(onClick = onViewMaintenance, modifier = Modifier.weight(1f)) { Text("Maintenance") }
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

@Composable
fun OnboardingFloatingCompanion(
    state: OnboardingState,
    onRestore: () -> Unit,
    hint: String,
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val dragMaxX = (screenWidthDp - 260.dp).coerceAtLeast(0.dp)
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    AnimatedVisibility(
        visible = true,
        enter = androidx.compose.animation.fadeIn(tween(220)) +
            androidx.compose.animation.slideInVertically(tween(220), initialOffsetY = { 80 }) +
            androidx.compose.animation.expandVertically(tween(220)),
        exit = androidx.compose.animation.fadeOut(tween(180)) +
            androidx.compose.animation.slideOutVertically(tween(180), targetOffsetY = { 80 }) +
            androidx.compose.animation.shrinkVertically(tween(180)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 18.dp, start = 20.dp, end = 20.dp, top = 16.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Card(
                modifier = Modifier
                    .clickable(onClick = onRestore)
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            val current = dragOffset
                            dragOffset = Offset(
                                (current.x + dragAmount.x).coerceIn(0f, dragMaxX.value),
                                (current.y + dragAmount.y).coerceIn(-80f, 110f),
                            )
                        }
                    }
                    .offset { IntOffset(dragOffset.x.toInt(), dragOffset.y.toInt()) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
            ) {
                Box(
                    Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(14.dp),
                        )
                        .clickable(onClick = onRestore)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.DirectionsBike,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            "Setup · ${state.page + 1}/7${if (state.vehicleAdded) " · Vehicle added" else ""} · $hint",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

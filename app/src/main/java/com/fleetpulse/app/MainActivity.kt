package com.fleetpulse.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fleetpulse.app.data.prefs.AppPreferencesState
import com.fleetpulse.app.ui.MainViewModel
import com.fleetpulse.app.ui.UiSnapshot
import com.fleetpulse.app.ui.screens.AnalysisScreen
import com.fleetpulse.app.ui.screens.AboutScreen
import com.fleetpulse.app.ui.screens.BugReportScreen
import com.fleetpulse.app.ui.screens.CalibrationScreen
import com.fleetpulse.app.ui.screens.DashboardScreen
import com.fleetpulse.app.ui.screens.DataManagementScreen
import com.fleetpulse.app.ui.screens.EmailFeedbackScreen
import com.fleetpulse.app.ui.screens.FeatureRequestScreen
import com.fleetpulse.app.ui.screens.FeedbackHubScreen
import com.fleetpulse.app.ui.screens.GeneralFeedbackScreen
import com.fleetpulse.app.ui.screens.MaintenanceScreen
import com.fleetpulse.app.ui.screens.MoreScreen
import com.fleetpulse.app.ui.screens.OnboardingFloatingCompanion
import com.fleetpulse.app.ui.screens.OnboardingScreen
import com.fleetpulse.app.ui.screens.OnboardingState
import com.fleetpulse.app.ui.screens.QuickEntryScreen
import com.fleetpulse.app.ui.screens.SettingsScreen
import com.fleetpulse.app.ui.screens.SupportFleetPulseScreen
import com.fleetpulse.app.ui.screens.UsageLogScreen
import com.fleetpulse.app.ui.screens.UserGuideScreen
import com.fleetpulse.app.ui.screens.VehiclesScreen
import com.fleetpulse.app.ui.screens.VerificationScreen
import com.fleetpulse.app.ui.theme.FleetAmber
import com.fleetpulse.app.ui.theme.FleetBlue
import com.fleetpulse.app.ui.theme.FleetGreen
import com.fleetpulse.app.ui.theme.FleetOrange
import com.fleetpulse.app.ui.theme.FleetPulseTheme
import com.fleetpulse.app.ui.theme.FleetViolet
import java.net.URLEncoder

class MainActivity : ComponentActivity() {

    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        pendingRoute.value = intent?.getStringExtra("navigate")
        enableEdgeToEdge()
        setContent {
            AppRoot(
                pendingRoute = pendingRoute.value,
                onPendingRouteConsumed = { pendingRoute.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute.value = intent.getStringExtra("navigate")
    }
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomDestinations = listOf(
    BottomDestination("dashboard", "Dashboard", Icons.Default.Dashboard),
    BottomDestination("vehicles", "Vehicles", Icons.Default.DirectionsBike),
    BottomDestination("usage", "Usage Log", Icons.Default.ListAlt),
    BottomDestination("analysis", "Analysis", Icons.Default.Insights),
    BottomDestination("maintenance", "Maintenance", Icons.Default.Build),
    BottomDestination("more", "More", Icons.Default.MoreHoriz),
)

@Composable
private fun AppRoot(
    pendingRoute: String?,
    onPendingRouteConsumed: () -> Unit,
    vm: MainViewModel = viewModel(),
) {
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) vm.setNotificationsEnabled(false)
    }
    var askedForNotifications by rememberSaveable { mutableStateOf(false) }
    var onboardingPendingRoute by rememberSaveable { mutableStateOf<String?>(null) }
    var onboardingState by remember { mutableStateOf(OnboardingState()) }
    LaunchedEffect(prefs.onboardingComplete) {
        if (prefs.onboardingComplete) onboardingState = OnboardingState()
        if (prefs.onboardingComplete && !askedForNotifications && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            askedForNotifications = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val darkTheme = when (prefs.themeMode) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    FleetPulseTheme(darkTheme = darkTheme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                !prefs.loaded -> LoadingScreen()
                !prefs.onboardingComplete -> {
                    if (onboardingState.minimized) {
                        val snap = snapshot
                        if (snap != null) {
                            MainScaffold(
                                vm = vm,
                                snapshot = snap,
                                prefs = prefs,
                                pendingRoute = pendingRoute ?: onboardingPendingRoute,
                                onPendingRouteConsumed = {
                                    onPendingRouteConsumed()
                                    onboardingPendingRoute = null
                                },
                                onVehicleAdded = {
                                    onboardingState = onboardingState.copy(vehicleAdded = true)
                                },
                            )
                        }
                        OnboardingFloatingCompanion(
                            state = onboardingState,
                            onRestore = { onboardingState = onboardingState.copy(minimized = false) },
                            hint = when {
                                onboardingState.vehicleAdded -> "Vehicle added — tap to resume onboarding"
                                onboardingPendingRoute == "vehicles" -> "Tap to resume: add a vehicle"
                                onboardingPendingRoute == "maintenance" -> "Tap to resume: track maintenance"
                                else -> "Tap to resume onboarding"
                            },
                        )
                    } else {
                        OnboardingScreen(
                            state = onboardingState,
                            onSetState = { onboardingState = it },
                            onAddVehicle = {
                                onboardingState = onboardingState.copy(minimized = true)
                                onboardingPendingRoute = "vehicles"
                            },
                            onRecordUsage = {
                                onboardingState = onboardingState.copy(minimized = true)
                                onboardingPendingRoute = "vehicles"
                            },
                            onExploreDashboard = { vm.completeOnboarding(false) },
                            onViewMaintenance = {
                                onboardingState = onboardingState.copy(minimized = true)
                                onboardingPendingRoute = "maintenance"
                            },
                            onLoadDemo = { vm.completeOnboarding(true) },
                            onSkip = { vm.completeOnboarding(false) },
                            onFinish = { vm.completeOnboarding(false) },
                            onMinimize = { onboardingState = onboardingState.copy(minimized = true) },
                        )
                    }
                }
                else -> {
                    val snap = snapshot
                    if (snap == null) {
                        LoadingScreen()
                    } else {
                        MainScaffold(
                            vm = vm,
                            snapshot = snap,
                            prefs = prefs,
                            pendingRoute = pendingRoute ?: onboardingPendingRoute,
                            onPendingRouteConsumed = {
                                onPendingRouteConsumed()
                                onboardingPendingRoute = null
                            },
                            onVehicleAdded = {
                                onboardingState = onboardingState.copy(vehicleAdded = true)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Bolt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            "Opening your local database…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    vm: MainViewModel,
    snapshot: UiSnapshot,
    prefs: AppPreferencesState,
    pendingRoute: String?,
    onPendingRouteConsumed: () -> Unit,
    onVehicleAdded: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = bottomDestinations.any { it.route == currentRoute }
    var showQuickSheet by remember { mutableStateOf(false) }
    var autoOpenMaintenance by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    /**
     * Every top-level destination is reached with the same back-stack flags.
     * Without this, a quick action could push a second copy of Maintenance and
     * both copies would race to consume the one-shot auto-open event.
     */
    fun navigateTopLevel(route: String) {
        if (currentRoute == route) return
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(pendingRoute) {
        val route = pendingRoute ?: return@LaunchedEffect
        if (bottomDestinations.any { it.route == route }) {
            navigateTopLevel(route)
        }
        onPendingRouteConsumed()
    }

    fun entryRoute(mode: String, vehicleId: String? = null, entryId: String? = null): String {
        val v = URLEncoder.encode(vehicleId.orEmpty(), "UTF-8")
        val e = URLEncoder.encode(entryId.orEmpty(), "UTF-8")
        return "entry?mode=${URLEncoder.encode(mode, "UTF-8")}&vehicleId=$v&entryId=$e"
    }

    fun openEntry(mode: String, vehicleId: String? = null, entryId: String? = null) {
        navController.navigate(entryRoute(mode, vehicleId, entryId))
    }

    fun openDetail(route: String) {
        if (currentRoute != route) navController.navigate(route)
    }

    fun openFeedbackDetail(detail: String, origin: String) {
        navController.navigate("feedback/$detail?origin=${Uri.encode(origin)}")
    }

    val state = snapshot.state
    val sampleTotal = state.usageEntries.count { it.isSample } +
        state.maintenanceEntries.count { it.isSample } +
        state.calibration.count { it.isSample }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // The app draws edge-to-edge. safeDrawing includes the IME, so forms are
        // laid out above the keyboard instead of being covered by it. Scaffold
        // unions this with the navigation bar, avoiding double bottom padding.
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (isTopLevel) {
                NavigationBar(tonalElevation = 3.dp) {
                    bottomDestinations.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navigateTopLevel(destination.route)
                            },
                            icon = {
                                Icon(
                                    destination.icon,
                                    contentDescription = destination.label,
                                    modifier = Modifier.size(21.dp),
                                )
                            },
                            label = {
                                Text(
                                    destination.label,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (isTopLevel && currentRoute != "more") {
                FloatingActionButton(
                    onClick = { showQuickSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Quick entry")
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable("dashboard") {
                DashboardScreen(
                    snapshot = snapshot,
                    onVehicleFilter = vm::setVehicleFilter,
                    onPeriodFilter = vm::setPeriodFilter,
                    onCustomRange = vm::setCustomRange,
                    onAddEntry = { vehicleId, mode -> openEntry(mode ?: "quick", vehicleId) },
                    onEditEntry = { id -> openEntry("quick", null, id) },
                    onOpenUsage = { navigateTopLevel("usage") },
                    onOpenData = { openDetail("data") },
                    onOpenMaintenance = { navigateTopLevel("maintenance") },
                    onRemoveSamples = { vm.removeSampleData() },
                    onReportProblem = { openFeedbackDetail("bug", "Dashboard") },
                )
            }
            composable("vehicles") {
                VehiclesScreen(
                    vm = vm,
                    vehicles = state.vehicles,
                    evaluation = snapshot.evaluation,
                    nextId = vm::nextVehicleId,
                    onSave = { vehicle, isNew ->
                        val saved = vm.saveVehicle(vehicle, isNew)
                        if (saved && isNew) onVehicleAdded()
                        saved
                    },
                    onArchive = vm::setVehicleArchived,
                    onDelete = vm::deleteVehicleCascade,
                    onAddEntry = { vehicleId ->
                        val vehicle = state.vehicles.firstOrNull { it.id == vehicleId }
                        val mode = if (vehicle?.energySource == "Electricity") "ev_charge" else "petrol_fill"
                        openEntry(mode, vehicleId)
                    },
                )
            }
            composable("usage") {
                UsageLogScreen(
                    vehicles = state.vehicles,
                    evaluated = snapshot.evaluation.evaluatedUsage,
                    selectedVehicle = snapshot.filter.vehicleId,
                    onVehicleFilter = vm::setVehicleFilter,
                    onAdd = { vehicleId -> openEntry("quick", vehicleId) },
                    onEditEntry = { id -> openEntry("quick", null, id) },
                    onDeleteEntry = vm::deleteUsage,
                    onReportProblem = { openFeedbackDetail("bug", "Usage log") },
                )
            }
            composable("analysis") {
                AnalysisScreen(
                    snapshot = snapshot,
                    onVehicleFilter = vm::setVehicleFilter,
                    onPeriodFilter = vm::setPeriodFilter,
                    onReportProblem = { context -> openFeedbackDetail("bug", context) },
                )
            }
            composable("maintenance") {
                MaintenanceScreen(
                    vehicles = state.vehicles.filter { !it.archived },
                    items = snapshot.evaluation.evaluatedMaintenance,
                    settings = state.settings,
                    selectedVehicle = snapshot.filter.vehicleId,
                    onVehicleFilter = vm::setVehicleFilter,
                    currentOdometer = vm::currentOdometer,
                    autoOpenAdd = autoOpenMaintenance,
                    onConsumeAutoOpen = { autoOpenMaintenance = false },
                    onSave = vm::saveMaintenance,
                    onDelete = vm::deleteMaintenance,
                    onReportProblem = { openFeedbackDetail("bug", "Maintenance") },
                )
            }
            composable("more") {
                MoreScreen(
                    prefs = prefs,
                    sampleTotal = sampleTotal,
                    onCalibration = { openDetail("calibration") },
                    onSettings = { openDetail("settings") },
                    onData = { openDetail("data") },
                    onGuide = { openDetail("guide") },
                    onGettingStarted = vm::replayOnboarding,
                    onVerification = { openDetail("verification") },
                    onFeedback = { openDetail("feedback") },
                    onAbout = { openDetail("about") },
                )
            }
            composable("calibration") {
                CalibrationScreen(
                    vehicles = state.vehicles.filter { !it.archived },
                    calibration = state.calibration,
                    evaluation = snapshot.evaluation,
                    selectedVehicle = snapshot.filter.vehicleId,
                    onSelectVehicle = vm::setVehicleFilter,
                    onSave = vm::saveCalibration,
                    onDelete = vm::deleteCalibration,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("settings") {
                SettingsScreen(
                    settings = state.settings,
                    prices = state.priceHistory,
                    prefs = prefs,
                    onSaveSettings = vm::saveSettings,
                    onSavePrice = vm::savePrice,
                    onDeletePrice = vm::deletePrice,
                    onTheme = vm::setThemeMode,
                    onNotifications = vm::setNotificationsEnabled,
                    onReplayOnboarding = vm::replayOnboarding,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("data") {
                DataManagementScreen(
                    vm = vm,
                    sampleUsage = state.usageEntries.count { it.isSample },
                    sampleMaintenance = state.maintenanceEntries.count { it.isSample },
                    sampleCalibration = state.calibration.count { it.isSample },
                    vehicleCount = state.vehicles.size,
                    usageCount = state.usageEntries.size,
                    onBack = { navController.popBackStack() },
                    onReportProblem = { openFeedbackDetail("bug", "Data management · import error") },
                )
            }
            composable("guide") {
                UserGuideScreen(onBack = { navController.popBackStack() })
            }
            composable("verification") {
                val results by vm.verification.collectAsStateWithLifecycle()
                VerificationScreen(
                    results = results,
                    onRun = vm::runVerification,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("feedback") {
                FeedbackHubScreen(
                    onBack = { navController.popBackStack() },
                    onBugReport = { openFeedbackDetail("bug", "Feedback & Support") },
                    onFeatureRequest = { openFeedbackDetail("feature", "Feedback & Support") },
                    onGeneralFeedback = { openFeedbackDetail("general", "Feedback & Support") },
                    onEmailFeedback = { openDetail("feedback/email") },
                    onSupport = { openDetail("support") },
                )
            }
            composable("feedback/email") {
                EmailFeedbackScreen(
                    origin = "Feedback & Support",
                    onBack = { navController.popBackStack() },
                )
            }
            composable("about") {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            composable("support") {
                SupportFleetPulseScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "feedback/bug?origin={origin}",
                arguments = listOf(navArgument("origin") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                BugReportScreen(
                    origin = entry.arguments?.getString("origin").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "feedback/feature?origin={origin}",
                arguments = listOf(navArgument("origin") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                FeatureRequestScreen(
                    origin = entry.arguments?.getString("origin").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "feedback/general?origin={origin}",
                arguments = listOf(navArgument("origin") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                GeneralFeedbackScreen(
                    origin = entry.arguments?.getString("origin").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "entry?mode={mode}&vehicleId={vehicleId}&entryId={entryId}",
                arguments = listOf(
                    navArgument("mode") { type = NavType.StringType; defaultValue = "quick" },
                    navArgument("vehicleId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("entryId") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                val mode = entry.arguments?.getString("mode").orEmpty().ifBlank { "quick" }
                val vehicleId = entry.arguments?.getString("vehicleId").orEmpty().ifBlank { null }
                val entryId = entry.arguments?.getString("entryId").orEmpty().ifBlank { null }
                QuickEntryScreen(
                    state = state,
                    mode = mode,
                    presetVehicleId = vehicleId,
                    editEntryId = entryId,
                    vm = vm,
                    onDone = { navController.popBackStack() },
                    onOpenVehicles = {
                        navigateTopLevel("vehicles")
                    },
                    onReportProblem = {
                        val label = when (mode) {
                            "ev_charge" -> "EV charge"
                            "petrol_fill" -> "Petrol refill"
                            "ride_only" -> "Ride only"
                            else -> "Quick entry"
                        }
                        openFeedbackDetail("bug", "Usage entry · $label")
                    },
                )
            }
        }
    }

    if (showQuickSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Quick entry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Four shortcuts — every form stays short and remembers your last reading.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                QuickActionRow(
                    icon = Icons.Default.Bolt,
                    tint = FleetBlue,
                    title = "EV charge",
                    subtitle = "Battery voltage or metered kWh",
                    onClick = {
                        showQuickSheet = false
                        openEntry("ev_charge")
                    },
                )
                QuickActionRow(
                    icon = Icons.Default.LocalGasStation,
                    tint = FleetOrange,
                    title = "Petrol refill",
                    subtitle = "Litres, price and full-tank flag",
                    onClick = {
                        showQuickSheet = false
                        openEntry("petrol_fill")
                    },
                )
                QuickActionRow(
                    icon = Icons.Default.Speed,
                    tint = FleetGreen,
                    title = "Odometer / ride only",
                    subtitle = "Distance without a fill",
                    onClick = {
                        showQuickSheet = false
                        openEntry("ride_only")
                    },
                )
                QuickActionRow(
                    icon = Icons.Default.Build,
                    tint = FleetViolet,
                    title = "Maintenance record",
                    subtitle = "Service, repair or part replacement",
                    onClick = {
                        showQuickSheet = false
                        autoOpenMaintenance = true
                        navigateTopLevel("maintenance")
                    },
                )
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
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
                .size(42.dp)
                .background(tint.copy(alpha = 0.13f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
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

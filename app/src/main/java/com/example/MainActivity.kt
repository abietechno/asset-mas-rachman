package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.example.model.AssetType
import com.example.ui.AssetViewModel
import com.example.ui.CupertinoTab
import com.example.ui.components.*
import com.example.ui.dialogs.AddEditAssetDialog
import com.example.ui.dialogs.AssetDetailSheet
import com.example.ui.screens.AssetListScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.VehiclesTaxScreen
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: AssetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Denyut "online" ke dashboard hanya berjalan selama aplikasi terlihat.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.heartbeatLoop() }
        }

        // Check if opened from tax reminder notification
        val targetAssetId = intent?.getLongExtra("TARGET_ASSET_ID", -1L) ?: -1L
        if (targetAssetId != -1L) {
            viewModel.selectTab(CupertinoTab.VEHICLES)
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                com.example.ui.ThemeMode.SYSTEM -> systemDark
                com.example.ui.ThemeMode.LIGHT -> false
                com.example.ui.ThemeMode.DARK -> true
            }

            MyApplicationTheme(darkTheme = isDark) {
                val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
                Crossfade(targetState = isLoggedIn, label = "auth_fade") { loggedIn ->
                    if (!loggedIn) {
                        val loginLoading by viewModel.loginLoading.collectAsStateWithLifecycle()
                        val loginError by viewModel.loginError.collectAsStateWithLifecycle()
                        LoginScreen(
                            onLogin = { email, password ->
                                viewModel.loginToServer(BuildConfig.DEFAULT_SERVER_URL, email, password)
                            },
                            isLoading = loginLoading,
                            serverError = loginError
                        )
                    } else {
                        MainAppScreen(
                            viewModel = viewModel,
                            initialTargetAssetId = if (targetAssetId != -1L) targetAssetId else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppScreen(
    viewModel: AssetViewModel,
    initialTargetAssetId: Long?
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val analytics by viewModel.dashboardAnalytics.collectAsStateWithLifecycle()
    val selectedAsset by viewModel.selectedAsset.collectAsStateWithLifecycle()
    val isAddEditOpen by viewModel.isAddEditOpen.collectAsStateWithLifecycle()
    val editingAsset by viewModel.editingAsset.collectAsStateWithLifecycle()
    val allAssets by viewModel.allAssets.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Request Notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Auto-open target asset if opened from system notification
    LaunchedEffect(initialTargetAssetId, allAssets) {
        if (initialTargetAssetId != null && allAssets.isNotEmpty()) {
            val found = allAssets.find { it.id == initialTargetAssetId }
            if (found != null) {
                viewModel.openAssetDetail(found)
            }
        }
    }

    // Listen to user feedback messages
    LaunchedEffect(viewModel) {
        viewModel.userMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    CupertinoScaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(14.dp),
                    containerColor = CupertinoLabel,
                    contentColor = Color.White
                )
            }
        },
        bottomBar = {
            val glass = LocalCupertinoGlass.current
            Surface(
                color = glass.surface.copy(alpha = 0.92f),
                shadowElevation = 8.dp,
                border = BorderStroke(0.5.dp, glass.border),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cupertino_navigation_bar")
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    // Tab 1: Dashboard
                    NavigationBarItem(
                        selected = currentTab == CupertinoTab.DASHBOARD,
                        onClick = { viewModel.selectTab(CupertinoTab.DASHBOARD) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == CupertinoTab.DASHBOARD) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "Ringkasan"
                            )
                        },
                        label = {
                            Text(
                                text = CupertinoTab.DASHBOARD.label,
                                fontWeight = if (currentTab == CupertinoTab.DASHBOARD) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CupertinoPrimary,
                            selectedTextColor = CupertinoPrimary,
                            indicatorColor = CupertinoPrimary.copy(alpha = 0.12f),
                            unselectedIconColor = CupertinoSecondaryLabel,
                            unselectedTextColor = CupertinoSecondaryLabel
                        ),
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )

                    // Tab 2: All Assets
                    NavigationBarItem(
                        selected = currentTab == CupertinoTab.ASSETS,
                        onClick = { viewModel.selectTab(CupertinoTab.ASSETS) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == CupertinoTab.ASSETS) Icons.Default.Inventory2 else Icons.Outlined.Inventory2,
                                contentDescription = "Aset"
                            )
                        },
                        label = {
                            Text(
                                text = CupertinoTab.ASSETS.label,
                                fontWeight = if (currentTab == CupertinoTab.ASSETS) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CupertinoPrimary,
                            selectedTextColor = CupertinoPrimary,
                            indicatorColor = CupertinoPrimary.copy(alpha = 0.12f),
                            unselectedIconColor = CupertinoSecondaryLabel,
                            unselectedTextColor = CupertinoSecondaryLabel
                        ),
                        modifier = Modifier.testTag("nav_tab_assets")
                    )

                    // Tab 3: Vehicles & Tax
                    NavigationBarItem(
                        selected = currentTab == CupertinoTab.VEHICLES,
                        onClick = { viewModel.selectTab(CupertinoTab.VEHICLES) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (analytics.vehiclesWithTaxWarningCount > 0) {
                                        Badge(containerColor = CupertinoRed) {
                                            Text("${analytics.vehiclesWithTaxWarningCount}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (currentTab == CupertinoTab.VEHICLES) Icons.Default.DirectionsCar else Icons.Outlined.DirectionsCar,
                                    contentDescription = "Kendaraan & Pajak"
                                )
                            }
                        },
                        label = {
                            Text(
                                text = "Kendaraan",
                                fontWeight = if (currentTab == CupertinoTab.VEHICLES) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CupertinoPrimary,
                            selectedTextColor = CupertinoPrimary,
                            indicatorColor = CupertinoPrimary.copy(alpha = 0.12f),
                            unselectedIconColor = CupertinoSecondaryLabel,
                            unselectedTextColor = CupertinoSecondaryLabel
                        ),
                        modifier = Modifier.testTag("nav_tab_vehicles")
                    )

                    // Tab 4: Real-time Reports
                    NavigationBarItem(
                        selected = currentTab == CupertinoTab.REPORTS,
                        onClick = { viewModel.selectTab(CupertinoTab.REPORTS) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == CupertinoTab.REPORTS) Icons.Default.Assessment else Icons.Outlined.Assessment,
                                contentDescription = "Laporan"
                            )
                        },
                        label = {
                            Text(
                                text = "Laporan",
                                fontWeight = if (currentTab == CupertinoTab.REPORTS) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CupertinoPrimary,
                            selectedTextColor = CupertinoPrimary,
                            indicatorColor = CupertinoPrimary.copy(alpha = 0.12f),
                            unselectedIconColor = CupertinoSecondaryLabel,
                            unselectedTextColor = CupertinoSecondaryLabel
                        ),
                        modifier = Modifier.testTag("nav_tab_reports")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentTab, label = "tab_fade") { tab ->
                when (tab) {
                    CupertinoTab.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateTab = { viewModel.selectTab(it) }
                    )
                    CupertinoTab.ASSETS -> AssetListScreen(viewModel = viewModel)
                    CupertinoTab.VEHICLES -> VehiclesTaxScreen(viewModel = viewModel)
                    CupertinoTab.REPORTS -> ReportsScreen(viewModel = viewModel)
                }
            }
        }

        // Asset Detail Bottom Sheet
        if (selectedAsset != null) {
            AssetDetailSheet(
                asset = selectedAsset!!,
                viewModel = viewModel,
                onDismiss = { viewModel.closeAssetDetail() }
            )
        }

        // Add / Edit Asset Dialog
        if (isAddEditOpen) {
            AddEditAssetDialog(
                editingAsset = editingAsset,
                viewModel = viewModel,
                onDismiss = { viewModel.closeAddEdit() }
            )
        }
    }
}

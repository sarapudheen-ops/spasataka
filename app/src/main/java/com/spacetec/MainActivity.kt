package com.spacetec

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.spacetec.ui.*
import com.spacetec.ui.theme.SpaceTecTheme
import com.spacetec.ui.viewmodel.DiagnosticViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpaceTecTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SpaceTecApp()
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Diagnostics : Screen("diagnostics")
    object Settings : Screen("settings")
    object LiveData : Screen("liveData")
    object DTCScanner : Screen("dtcScanner")
    object VehicleInfo : Screen("vehicleInfo")
    
    companion object {
        const val KEY_SCREEN_TITLE = "screen_title"
    }
}

@Composable
fun SpaceTecApp() {
    val navController = rememberNavController()
    
    // Handle back press and up navigation
    val onBackClick = { navController.navigateUp() }
    
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        // Main Screen
        composable(Screen.Main.route) {
            SpaceMainScreen(
                onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToLiveData = { navController.navigate(Screen.LiveData.route) },
                onNavigateToDtcScanner = { navController.navigate(Screen.DTCScanner.route) },
                onNavigateToVehicleInfo = { navController.navigate(Screen.VehicleInfo.route) }
            )
        }
        
        // Diagnostics Screen
        composable(Screen.Diagnostics.route) {
            DiagnosticsScreen(onNavigateBack = { navController.navigateUp() })
        }
        
        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.navigateUp() })
        }
        
        // Live Data Screen
        composable(Screen.LiveData.route) {
            LiveDataScreen(onNavigateBack = { navController.navigateUp() })
        }
        
        // DTC Scanner Screen
        composable(Screen.DTCScanner.route) {
            DtcScannerScreen(onNavigateBack = { navController.navigateUp() })
        }
        
        // Vehicle Info Screen
        composable(Screen.VehicleInfo.route) {
            VehicleInfoScreen(onNavigateBack = { navController.navigateUp() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController) {
    val colors = MaterialTheme.colorScheme
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "SpaceTec Diagnostics",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colors.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.5f))
            
            // App Logo/Icon
            Icon(
                imageVector = Icons.Default.Settings, // Replace with your app icon
                contentDescription = "App Logo",
                modifier = Modifier.size(120.dp),
                tint = colors.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Welcome to SpaceTec",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onBackground
            )
            
            Text(
                text = "Professional diagnostic tools for vehicles and generators",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Diagnostic Options
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Button(
                    onClick = { navController.navigate("diagnostics") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Vehicle Diagnostics")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { navController.navigate("settings") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer
                    )
                ) {
                    Text("Generator Diagnostics")
                }
            }
            
            Spacer(modifier = Modifier.weight(0.5f))
            
            // Footer
            Text(
                text = "v1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarMenuScreen(
    navController: NavHostController = rememberNavController(),
    viewModel: com.spacetec.ui.viewmodel.MissionControlViewModel = hiltViewModel()
) {
    val colors = MaterialTheme.colorScheme
    val connectionState by viewModel.connectionState.collectAsState()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "Vehicle Diagnostics",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colors.surfaceColorAtElevation(3.dp),
                    titleContentColor = colors.onSurface,
                    navigationIconContentColor = colors.onSurface
                )
            )
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Vehicle Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surfaceColorAtElevation(2.dp)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Connected Vehicle",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = when (connectionState) {
                            com.spacetec.ui.viewmodel.ConnectionState.CONNECTED -> "Vehicle Connected"
                            com.spacetec.ui.viewmodel.ConnectionState.CONNECTING -> "Connecting..."
                            com.spacetec.ui.viewmodel.ConnectionState.DISCONNECTED -> "Not Connected"
                            com.spacetec.ui.viewmodel.ConnectionState.ERROR -> "Connection Error"
                            else -> "Not Connected"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = when (connectionState) {
                            com.spacetec.ui.viewmodel.ConnectionState.CONNECTED -> colors.primary
                            com.spacetec.ui.viewmodel.ConnectionState.ERROR -> colors.error
                            else -> colors.onSurface
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Diagnostic Options
            Text(
                text = "Diagnostic Tools",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Grid of diagnostic options
            val options = listOf(
                Triple("Read DTCs", "Diagnostic Trouble Codes", Icons.Default.Warning),
                Triple("Live Data", "Real-time Parameters", Icons.Default.Speed),
                Triple(
                    "Freeze Frame",
                    "Stored Data Snapshots",
                    Icons.Default.CameraAlt
                ),
                Triple("Readiness", "Emission Monitors", Icons.Default.CheckCircle),
                Triple("Vehicle Info", "VIN & ECU Details", Icons.Default.Info),
                Triple("Service Reset", "Maintenance Lights", Icons.Default.Build)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(options) { (title, description, icon) ->
                    DiagnosticCard(
                        title = title,
                        description = description,
                        icon = icon,
                        onClick = { 
                            when (title) {
                                "Read DTCs" -> navController.navigate("dtc_scanner")
                                "Live Data" -> navController.navigate("live_data")
                                "Freeze Frame" -> navController.navigate("freeze_frame")
                                "Readiness" -> navController.navigate("readiness_monitors")
                                "Vehicle Info" -> navController.navigate("vehicle_info")
                                "Service Reset" -> navController.navigate("service_reset")
                                else -> { /* Feature not implemented yet */ }
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorMenuScreen(
    navController: NavHostController = rememberNavController(),
    viewModel: com.spacetec.ui.viewmodel.MissionControlViewModel = hiltViewModel()
) {
    val colors = MaterialTheme.colorScheme
    val connectionState by viewModel.connectionState.collectAsState()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "Generator Diagnostics",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colors.surfaceColorAtElevation(3.dp),
                    titleContentColor = colors.onSurface,
                    navigationIconContentColor = colors.onSurface
                )
            )
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Generator Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surfaceColorAtElevation(2.dp)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Generator Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = when (connectionState) {
                            com.spacetec.ui.viewmodel.ConnectionState.CONNECTED -> "Generator Connected"
                            com.spacetec.ui.viewmodel.ConnectionState.CONNECTING -> "Connecting..."
                            com.spacetec.ui.viewmodel.ConnectionState.DISCONNECTED -> "Not Connected"
                            com.spacetec.ui.viewmodel.ConnectionState.ERROR -> "Connection Error"
                            else -> "Not Connected"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = when (connectionState) {
                            com.spacetec.ui.viewmodel.ConnectionState.CONNECTED -> colors.primary
                            com.spacetec.ui.viewmodel.ConnectionState.ERROR -> colors.error
                            else -> colors.onSurface
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Diagnostic Options
            Text(
                text = "Diagnostic Tools",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Grid of diagnostic options
            val options = listOf(
                Triple("Monitor Status", "Real-time Parameters", Icons.Default.Speed),
                Triple("Run Tests", "Diagnostic Procedures", Icons.Default.CheckCircle),
                Triple("View History", "Operation Logs", Icons.Default.History),
                Triple("Alarms", "Active Warnings", Icons.Default.Warning),
                Triple("Maintenance", "Service Schedule", Icons.Default.Build),
                Triple("Settings", "Generator Config", Icons.Default.Settings)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(options) { (title, description, icon) ->
                    DiagnosticCard(
                        title = title,
                        description = description,
                        icon = icon,
                        onClick = { 
                            when (title) {
                                "Monitor Status" -> navController.navigate("generator_monitor")
                                "Run Tests" -> navController.navigate("generator_tests")
                                "View History" -> navController.navigate("generator_history")
                                "Alarms" -> navController.navigate("generator_alarms")
                                "Maintenance" -> navController.navigate("generator_maintenance")
                                "Settings" -> navController.navigate("generator_settings")
                                else -> { /* Feature not implemented yet */ }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DiagnosticCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// DeepSea control panel functionality will be implemented as a separate feature
class DeepSeaControlPanel {
    fun programControlPanel() {
        // Programming logic for DeepSea control panels
    }
}

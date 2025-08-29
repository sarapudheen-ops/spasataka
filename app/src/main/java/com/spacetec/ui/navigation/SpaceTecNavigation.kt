package com.spacetec.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.spacetec.ui.screens.*
import com.spacetec.ui.screens.diagnostics.*
import com.spacetec.ui.screens.VehicleLibraryScreen

// Navigation routes
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Diagnostics : Screen("diagnostics", "Diagnostics", Icons.Default.Build)
    object VehicleLibrary : Screen("vehicle_library", "Vehicle Library", Icons.Default.DirectionsCar)
    object Professional : Screen("professional", "Professional", Icons.Default.Engineering)
    object LiveData : Screen("livedata", "Live Data", Icons.Default.Speed)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

// Nested navigation routes
sealed class VehicleScreens(val route: String) {
    object Main : VehicleScreens("vehicle_library")
    object Details : VehicleScreens("vehicle_details/{vehicleId}") {
        fun createRoute(vehicleId: String) = "vehicle_details/$vehicleId"
    }
    
    companion object {
        const val VEHICLE_ID_ARG = "vehicleId"
    }
}

// Navigation arguments
sealed class NavArgs(val name: String) {
    object VehicleId : NavArgs("vehicleId")
    
    companion object {
        val allArgs = listOf(VehicleId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceTecNavigation() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Dashboard,
        Screen.Diagnostics,
        Screen.VehicleLibrary,
        Screen.Professional,
        Screen.LiveData,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Main Screens
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) },
                    onNavigateToVehicleLibrary = { navController.navigate(Screen.VehicleLibrary.route) },
                    onNavigateToProfessional = { navController.navigate(Screen.Professional.route) },
                    onNavigateToLiveData = { navController.navigate(Screen.LiveData.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            
            composable(Screen.Diagnostics.route) {
                DiagnosticsScreen(navController = navController)
            }
            
            // Vehicle Library Navigation
            composable(Screen.VehicleLibrary.route) {
                VehicleLibraryScreen(navController = navController)
            }
            
            composable(
                route = VehicleScreens.Details.route,
                arguments = listOf(
                    navArgument(VehicleScreens.VEHICLE_ID_ARG) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                // In a real app, you would fetch the vehicle details using the ID
                // For now, we'll just show a placeholder
                val vehicleId = backStackEntry.arguments?.getString(VehicleScreens.VEHICLE_ID_ARG) ?: return@composable
                
                // In a real app, you would navigate to a VehicleDetailsScreen
                // For now, we'll just show a basic screen
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Vehicle Details for ID: $vehicleId\n\nThis would show detailed information about the selected vehicle.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            composable(Screen.Professional.route) {
                Text(
                    text = "Professional Features - Coming Soon",
                    modifier = Modifier.fillMaxSize().wrapContentSize(),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            
            composable(Screen.LiveData.route) {
                Text(
                    text = "Live Data - Coming Soon", 
                    modifier = Modifier.fillMaxSize().wrapContentSize(),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
            
            // Placeholder screens for other routes
            composable("engine_diagnostics") {
                Text("Engine Diagnostics - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
            }
            
            composable("transmission_diagnostics") {
                Text("Transmission Diagnostics - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
            }
            
            composable("adas_diagnostics") {
                Text("ADAS Diagnostics - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
            }
            
            composable("emissions_diagnostics") {
                Text("Emissions Diagnostics - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
            }
            
            composable("professional_services") {
                Text("Professional Services - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
            }
            
            composable("bidirectional_controls") {
                Text("Bi-Directional Controls - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
            }
            
            composable("ecu_coding") {
                Text("ECU Coding - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
            }
            
            composable("auto_vin_detection") {
                Text("Auto VIN Detection - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
            }
            
            composable("vehicle_selection") {
                Text("Vehicle Selection - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
            }
            
            composable("service_functions") {
                Text("Service Functions - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
            }
            
            composable("obd_connection") {
                Text("OBD Connection - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
            }
            
            composable("system_status") {
                Text("System Status - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
            }
            
            composable("vehicle_library") {
                Text("Vehicle Library - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
            }
            
            composable("vehicle_details/{brand}/{model}/{year}") { backStackEntry ->
                Text("Vehicle Details - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
            }
        }
    }
}

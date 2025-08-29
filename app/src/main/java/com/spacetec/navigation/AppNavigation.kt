package com.spacetec.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.spacetec.ui.screens.dashboard.VehicleDashboard
import com.spacetec.ui.screens.diagnostics.DiagnosticsScreen
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Modifier

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object VehicleSelection : Screen("vehicle_selection")
    object Diagnostics : Screen("diagnostics")
    object Professional : Screen("professional")
    object LiveData : Screen("livedata")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            Text("Dashboard - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
        }
        
        composable(Screen.VehicleSelection.route) {
            Text("Vehicle Selection - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
        }
        
        composable(Screen.Diagnostics.route) {
            Text("Diagnostics - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
        }
        
        composable(Screen.Professional.route) {
            Text("Professional - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
        }
        
        composable(Screen.LiveData.route) {
            Text("Live Data - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
        }
        
        composable(Screen.Settings.route) {
            Text("Settings - Coming Soon", modifier = Modifier.fillMaxSize().wrapContentSize())
        }
    }
}

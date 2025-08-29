package com.spacetec.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.spacetec.ui.components.SpaceTecCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmissionsDiagnosticsScreen(navController: NavController) {
    val emissionsTests = listOf(
        "Oxygen Sensors" to "Test O2 sensor response and heating",
        "Catalytic Converter" to "Monitor catalyst efficiency",
        "EGR System" to "Check exhaust gas recirculation",
        "Evaporative System" to "Test EVAP system integrity",
        "PCV System" to "Verify positive crankcase ventilation",
        "Secondary Air" to "Check secondary air injection"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF388E3C))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🌱 Emissions Diagnostics",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Environmental compliance testing",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        LazyColumn {
            items(emissionsTests) { (testName, description) ->
                SpaceTecCard(
                    title = testName,
                    description = description,
                    onClick = {
                        when (testName) {
                            "Oxygen Sensors" -> {
                                // Launch O2 sensor response and heating test
                                navController.navigate("emissions_oxygen_sensors")
                            }
                            "Catalytic Converter" -> {
                                // Launch catalyst efficiency monitoring
                                navController.navigate("emissions_catalytic_converter")
                            }
                            "EGR System" -> {
                                // Launch exhaust gas recirculation check
                                navController.navigate("emissions_egr_system")
                            }
                            "Evaporative System" -> {
                                // Launch EVAP system integrity test
                                navController.navigate("emissions_evaporative_system")
                            }
                            "PCV System" -> {
                                // Launch positive crankcase ventilation verification
                                navController.navigate("emissions_pcv_system")
                            }
                            "Secondary Air" -> {
                                // Launch secondary air injection check
                                navController.navigate("emissions_secondary_air")
                            }
                        }
                    }
                )
            }
        }
    }
}

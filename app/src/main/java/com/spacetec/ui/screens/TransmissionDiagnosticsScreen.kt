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
fun TransmissionDiagnosticsScreen(navController: NavController) {
    val transmissionTests = listOf(
        "Shift Solenoids" to "Test transmission shift solenoids",
        "Torque Converter" to "Analyze torque converter clutch",
        "Pressure Tests" to "Check hydraulic pressures",
        "Gear Ratios" to "Verify gear ratio calculations",
        "Temperature" to "Monitor transmission temperature",
        "Fluid Analysis" to "Check transmission fluid condition"
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🔧 Transmission Diagnostics",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Advanced transmission system analysis",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        LazyColumn {
            items(transmissionTests) { (testName, description) ->
                SpaceTecCard(
                    title = testName,
                    description = description,
                    onClick = {
                        when (testName) {
                            "Shift Solenoids" -> {
                                // Launch shift solenoid test
                                navController.navigate("transmission_shift_solenoids")
                            }
                            "Torque Converter" -> {
                                // Launch torque converter analysis
                                navController.navigate("transmission_torque_converter")
                            }
                            "Pressure Tests" -> {
                                // Launch hydraulic pressure testing
                                navController.navigate("transmission_pressure_tests")
                            }
                            "Gear Ratios" -> {
                                // Launch gear ratio verification
                                navController.navigate("transmission_gear_ratios")
                            }
                            "Temperature" -> {
                                // Launch transmission temperature monitoring
                                navController.navigate("transmission_temperature")
                            }
                            "Fluid Analysis" -> {
                                // Launch transmission fluid condition check
                                navController.navigate("transmission_fluid_analysis")
                            }
                        }
                    }
                )
            }
        }
    }
}

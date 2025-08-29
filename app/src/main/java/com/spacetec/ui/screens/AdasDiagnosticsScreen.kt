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
fun AdasDiagnosticsScreen(navController: NavController) {
    val adasTests = listOf(
        "Forward Collision Warning" to "Test FCW system functionality",
        "Lane Departure Warning" to "Verify LDW sensor calibration",
        "Blind Spot Monitoring" to "Check BSM radar sensors",
        "Adaptive Cruise Control" to "Test ACC system response",
        "Parking Assist" to "Verify parking sensor operation",
        "Night Vision" to "Test infrared camera system"
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF7B1FA2))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🛡️ ADAS Diagnostics",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Advanced Driver Assistance Systems",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        LazyColumn {
            items(adasTests) { (testName, description) ->
                SpaceTecCard(
                    title = testName,
                    description = description,
                    onClick = {
                        when (testName) {
                            "Forward Collision Warning" -> {
                                // Launch FCW system functionality test
                                navController.navigate("adas_forward_collision_warning")
                            }
                            "Lane Departure Warning" -> {
                                // Launch LDW sensor calibration verification
                                navController.navigate("adas_lane_departure_warning")
                            }
                            "Blind Spot Monitoring" -> {
                                // Launch BSM radar sensor check
                                navController.navigate("adas_blind_spot_monitoring")
                            }
                            "Adaptive Cruise Control" -> {
                                // Launch ACC system response test
                                navController.navigate("adas_adaptive_cruise_control")
                            }
                            "Parking Assist" -> {
                                // Launch parking sensor operation verification
                                navController.navigate("adas_parking_assist")
                            }
                            "Night Vision" -> {
                                // Launch infrared camera system test
                                navController.navigate("adas_night_vision")
                            }
                        }
                    }
                )
            }
        }
    }
}

package com.spacetec.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.spacetec.diagnostic.professional.BiDirectionalControls
import kotlinx.coroutines.launch

@Composable
fun BiDirectionalControlsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val controlManager = remember {
        BiDirectionalControls(context, null, null)
    }
    
    val controlStatus by controlManager.controlStatus.collectAsState()
    val activeTest by controlManager.activeTest.collectAsState()
    val testResults by controlManager.testResults.collectAsState()
    
    val testCategories = listOf(
        "Engine Tests" to listOf("Fuel Injectors", "Ignition Coils", "Fuel Pump", "Throttle Body"),
        "Transmission" to listOf("Solenoids", "Pressure Tests", "Shift Tests"),
        "Brake System" to listOf("ABS Pump", "Brake Actuators", "EPB Motors"),
        "HVAC System" to listOf("A/C Compressor", "Blend Doors", "Fan Motors"),
        "Body Controls" to listOf("Window Motors", "Door Locks", "Mirrors", "Lights"),
        "Suspension" to listOf("Air Compressor", "Valve Block", "Height Sensors")
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Bi-Directional Controls",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Active testing of vehicle components",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        
        if (controlStatus != BiDirectionalControls.ControlStatus.IDLE) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Control Status: ${controlStatus.name}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    activeTest?.let { test ->
                        Text(
                            text = "Active Test: ${test.component.name}",
                            fontSize = 14.sp
                        )
                    }
                    if (testResults.isNotEmpty()) {
                        Text(
                            text = "Results: ${testResults.last()}",
                            fontSize = 14.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
        
        LazyColumn {
            items(testCategories) { (category, tests) ->
                BiDirectionalCategoryCard(
                    category = category,
                    tests = tests,
                    onTestClick = { testName ->
                        scope.launch {
                            val vehicleInfo = mapOf("make" to "BMW", "model" to "3 Series", "year" to "2021")
                            when (testName) {
                                "Fuel Injectors" -> controlManager.startActiveTest(BiDirectionalControls.ComponentType.FUEL_INJECTORS, vehicleInfo)
                                "Ignition Coils" -> controlManager.startActiveTest(BiDirectionalControls.ComponentType.IGNITION_COILS, vehicleInfo)
                                "A/C Compressor" -> controlManager.startActiveTest(BiDirectionalControls.ComponentType.AC_COMPRESSOR_CLUTCH, vehicleInfo)
                                "ABS Pump" -> controlManager.startActiveTest(BiDirectionalControls.ComponentType.ABS_PUMP, vehicleInfo)
                                "Window Motors" -> controlManager.startActiveTest(BiDirectionalControls.ComponentType.WINDOW_MOTORS, vehicleInfo)
                                "Door Locks" -> controlManager.startActiveTest(BiDirectionalControls.ComponentType.DOOR_LOCKS, vehicleInfo)
                                else -> controlManager.startActiveTest(BiDirectionalControls.ComponentType.FUEL_PUMP, vehicleInfo)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BiDirectionalCategoryCard(
    category: String,
    tests: List<String>,
    onTestClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = category,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9800)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            tests.forEach { test ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = test,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = { onTestClick(test) },
                        modifier = Modifier.size(width = 80.dp, height = 32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Text("Test", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

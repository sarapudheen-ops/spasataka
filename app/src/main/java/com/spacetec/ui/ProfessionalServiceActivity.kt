package com.spacetec.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spacetec.diagnostic.professional.ProfessionalServiceFunctions
import com.spacetec.diagnostic.professional.BiDirectionalControls
import com.spacetec.diagnostic.professional.OfflineEcuCoding
import com.spacetec.diagnostic.professional.AutoVinDetection
import com.spacetec.database.VehicleDatabase

/**
 * Professional Service Menu Activity
 * Modern UI matching professional diagnostic tools
 */
class ProfessionalServiceActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                ProfessionalServiceScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalServiceScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Service Functions", "Bi-Directional", "ECU Coding", "Auto VIN", "Vehicle Database")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Professional Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "SpaceTec Professional",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Advanced Diagnostic System",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        
        // Tab Navigation
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tab Content
        when (selectedTab) {
            0 -> ServiceFunctionsTab()
            1 -> BiDirectionalTab()
            2 -> EcuCodingTab()
            3 -> AutoVinTab()
            4 -> VehicleDatabaseTab()
        }
    }
}

@Composable
fun ServiceFunctionsTab() {
    val serviceCategories = listOf(
        "Engine Services" to listOf("Oil Reset", "DPF Regeneration", "Injector Coding", "Throttle Adaptation"),
        "Transmission" to listOf("Transmission Reset", "Clutch Adaptation", "Shift Point Learning"),
        "Brake System" to listOf("EPB Reset", "Brake Bleeding", "ABS Calibration"),
        "HVAC System" to listOf("A/C Reset", "Heater Calibration", "Climate Control"),
        "Body Systems" to listOf("Window Calibration", "Sunroof Reset", "Door Lock Programming"),
        "Safety Systems" to listOf("SRS Reset", "Seatbelt Pretensioner", "Crash Data Clear"),
        "Suspension" to listOf("Air Suspension", "Level Calibration", "Ride Height"),
        "Electrical" to listOf("Battery Registration", "Alternator Test", "Starter Test"),
        "TPMS Services" to listOf("TPMS Reset", "Sensor Programming", "Pressure Calibration")
    )
    
    LazyColumn {
        items(serviceCategories) { (category, services) ->
            ServiceCategoryCard(category, services)
        }
    }
}

@Composable
fun ServiceCategoryCard(category: String, services: List<String>) {
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
                color = Color(0xFF1976D2)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            services.forEach { service ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = service,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = { /* Execute service */ },
                        modifier = Modifier.size(width = 80.dp, height = 32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Start", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun BiDirectionalTab() {
    val testCategories = listOf(
        "Engine Tests" to listOf("Fuel Injectors", "Ignition Coils", "Fuel Pump", "Throttle Body"),
        "Transmission" to listOf("Solenoids", "Pressure Tests", "Shift Tests"),
        "Brake System" to listOf("ABS Pump", "Brake Actuators", "EPB Motors"),
        "HVAC System" to listOf("A/C Compressor", "Blend Doors", "Fan Motors"),
        "Body Controls" to listOf("Window Motors", "Door Locks", "Mirrors", "Lights"),
        "Suspension" to listOf("Air Compressor", "Valve Block", "Height Sensors")
    )
    
    LazyColumn {
        items(testCategories) { (category, tests) ->
            BiDirectionalCategoryCard(category, tests)
        }
    }
}

@Composable
fun BiDirectionalCategoryCard(category: String, tests: List<String>) {
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
                        onClick = { /* Execute test */ },
                        modifier = Modifier.size(width = 80.dp, height = 32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Text("Test", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EcuCodingTab() {
    val codingCategories = listOf(
        "Component Matching" to listOf("Throttle Body", "Fuel Injectors", "Mass Airflow", "Oxygen Sensors"),
        "Personalization" to listOf("Comfort Settings", "Lighting", "Door Locks", "Climate Control"),
        "Retrofit Coding" to listOf("New Modules", "Feature Activation", "Option Coding"),
        "Calibration" to listOf("Steering Angle", "Yaw Rate", "Accelerometer", "Wheel Speed"),
        "Key Programming" to listOf("New Keys", "Remote Controls", "Immobilizer", "Start/Stop")
    )
    
    LazyColumn {
        items(codingCategories) { (category, functions) ->
            EcuCodingCategoryCard(category, functions)
        }
    }
}

@Composable
fun EcuCodingCategoryCard(category: String, functions: List<String>) {
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
                color = Color(0xFF9C27B0)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            functions.forEach { function ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = function,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = { /* Execute coding */ },
                        modifier = Modifier.size(width = 80.dp, height = 32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                    ) {
                        Text("Code", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AutoVinTab() {
    var detectionStatus by remember { mutableStateOf("Ready") }
    var vehicleInfo by remember { mutableStateOf<String?>(null) }
    
    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Auto VIN Detection",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Status: $detectionStatus",
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        detectionStatus = "Detecting..."
                        // Simulate detection
                        vehicleInfo = "BMW 3 Series 2021 - VIN: WBA12345678901234"
                        detectionStatus = "Completed"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("Start Auto Detection")
                }
                
                vehicleInfo?.let { info ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Detected Vehicle:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = info,
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
        
        // Guided Procedures
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Guided Procedures",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                listOf("Oil Service Reset", "TPMS Reset", "Battery Registration").forEach { procedure ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = procedure)
                        Button(
                            onClick = { /* Start guided procedure */ },
                            modifier = Modifier.size(width = 80.dp, height = 32.dp)
                        ) {
                            Text("Guide", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleDatabaseTab() {
    val vehicleMakes = listOf("BMW", "Mercedes-Benz", "Audi", "Ford", "Chevrolet", "Toyota", "Honda")
    
    LazyColumn {
        items(vehicleMakes) { make ->
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
                        text = make,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF607D8B)
                    )
                    Text(
                        text = "Multiple models supported with full diagnostic coverage",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row {
                        Button(
                            onClick = { /* View models */ },
                            modifier = Modifier.padding(end = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF607D8B))
                        ) {
                            Text("View Models", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { /* View services */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF795548))
                        ) {
                            Text("Services", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

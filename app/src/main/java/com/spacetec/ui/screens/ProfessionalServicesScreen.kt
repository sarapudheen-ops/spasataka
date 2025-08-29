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
import com.spacetec.diagnostic.professional.ProfessionalServiceFunctions
import kotlinx.coroutines.launch

@Composable
fun ProfessionalServicesScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initialize professional service functions
    val serviceManager = remember {
        ProfessionalServiceFunctions(context, null, null)
    }
    
    val serviceStatus by serviceManager.serviceStatus.collectAsState()
    val currentService by serviceManager.currentService.collectAsState()
    val progress by serviceManager.progress.collectAsState()
    val statusMessage by serviceManager.statusMessage.collectAsState()
    
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Professional Service Functions",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "36+ Offline service procedures",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        
        // Status Display
        if (serviceStatus != ProfessionalServiceFunctions.ServiceStatus.IDLE) {
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
                        text = "Service Status: ${serviceStatus.name}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (currentService != null) {
                        Text(
                            text = "Current: ${currentService!!.name}",
                            fontSize = 14.sp
                        )
                    }
                    if (statusMessage.isNotEmpty()) {
                        Text(
                            text = statusMessage,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                    if (progress > 0) {
                        LinearProgressIndicator(
                            progress = progress / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    }
                }
            }
        }
        
        // Service Categories
        LazyColumn {
            items(serviceCategories) { (category, services) ->
                ServiceCategoryCard(
                    category = category,
                    services = services,
                    onServiceClick = { serviceName ->
                        scope.launch {
                            val vehicleInfo = mapOf("make" to "BMW", "model" to "3 Series", "year" to "2021")
                            when (serviceName) {
                                "Oil Reset" -> serviceManager.executeService(ProfessionalServiceFunctions.ServiceFunction.OIL_RESET, vehicleInfo)
                                "TPMS Reset" -> serviceManager.executeService(ProfessionalServiceFunctions.ServiceFunction.TPMS_RESET, vehicleInfo)
                                "Battery Registration" -> serviceManager.executeService(ProfessionalServiceFunctions.ServiceFunction.BATTERY_REGISTRATION, vehicleInfo)
                                "DPF Regeneration" -> serviceManager.executeService(ProfessionalServiceFunctions.ServiceFunction.DPF_REGENERATION, vehicleInfo)
                                "EPB Reset" -> serviceManager.executeService(ProfessionalServiceFunctions.ServiceFunction.EPB_RESET, vehicleInfo)
                                else -> serviceManager.executeService(ProfessionalServiceFunctions.ServiceFunction.THROTTLE_ADAPTATION, vehicleInfo)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ServiceCategoryCard(
    category: String,
    services: List<String>,
    onServiceClick: (String) -> Unit
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
                color = Color(0xFF4CAF50)
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
                        onClick = { onServiceClick(service) },
                        modifier = Modifier.size(width = 80.dp, height = 32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Start", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

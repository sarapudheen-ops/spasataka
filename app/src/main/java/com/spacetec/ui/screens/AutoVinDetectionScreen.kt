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
import com.spacetec.diagnostic.professional.AutoVinDetection
import kotlinx.coroutines.launch

@Composable
fun AutoVinDetectionScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val vinDetector = remember {
        AutoVinDetection(context, null, null)
    }
    
    val detectionStatus by vinDetector.detectionStatus.collectAsState()
    val vehicleInfo by vinDetector.vehicleInfo.collectAsState()
    val guidedProcedures by vinDetector.guidedProcedures.collectAsState()
    
    var detectionMessage by remember { mutableStateOf("Ready to detect vehicle") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Auto VIN Detection",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Automatic vehicle identification with guided procedures",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        
        // Detection Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Detection Status",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Status: ${detectionStatus.name}",
                    fontSize = 16.sp
                )
                Text(
                    text = detectionMessage,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        scope.launch {
                            detectionMessage = "Detecting vehicle..."
                            val success = vinDetector.autoDetectVehicle()
                            detectionMessage = if (success) "Vehicle detected successfully" else "Detection failed"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("Start Auto Detection", color = Color.White)
                }
            }
        }
        
        // Vehicle Info Card
        vehicleInfo?.let { info ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Detected Vehicle",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("VIN: ${info.vin}", fontSize = 14.sp)
                    Text("Make: ${info.make}", fontSize = 14.sp)
                    Text("Model: ${info.model}", fontSize = 14.sp)
                    Text("Year: ${info.year}", fontSize = 14.sp)
                    Text("Engine: ${info.engine}", fontSize = 14.sp)
                    Text("Transmission: ${info.transmission}", fontSize = 14.sp)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Supported Systems:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    info.supportedSystems.forEach { system ->
                        Text("• $system", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
        
        // Guided Procedures
        if (guidedProcedures.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Available Guided Procedures",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn {
                        items(guidedProcedures) { procedure ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = procedure.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = procedure.description,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "Est. time: ${procedure.estimatedTime} min",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                Button(
                                    onClick = { 
                                        scope.launch {
                                            vinDetector.executeGuidedProcedure(procedure.id)
                                        }
                                    },
                                    modifier = Modifier.size(width = 80.dp, height = 32.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                                ) {
                                    Text("Start", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

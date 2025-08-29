package com.spacetec.ui.adas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spacetec.diagnostic.adas.AdasCalibrationSystem
import com.spacetec.ui.theme.SpaceTecTheme

class AdasCalibrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpaceTecTheme {
                AdasCalibrationScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdasCalibrationScreen(
    viewModel: AdasCalibrationViewModel = viewModel()
) {
    val calibrationStatus by viewModel.calibrationStatus.collectAsState()
    val currentProcedure by viewModel.currentProcedure.collectAsState()
    val supportedSystems by viewModel.supportedSystems.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1B2A),
                        Color(0xFF1B263B),
                        Color(0xFF415A77)
                    )
                )
            )
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1B263B).copy(alpha = 0.8f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "ADAS Calibration",
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "ADAS Calibration",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Advanced Driver Assistance Systems",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF90CAF9)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Vehicle Selection
        if (selectedVehicle == null) {
            VehicleSelectionCard(
                onVehicleSelected = { make, model, year ->
                    viewModel.selectVehicle(make, model, year)
                }
            )
        } else {
            // Current Vehicle Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2E3B4E).copy(alpha = 0.9f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Vehicle",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${selectedVehicle!!["make"]} ${selectedVehicle!!["model"]} ${selectedVehicle!!["year"]}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { viewModel.clearVehicle() }
                    ) {
                        Text("Change", color = Color(0xFF64B5F6))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Calibration Status
            if (calibrationStatus != AdasCalibrationSystem.CalibrationStatus.IDLE) {
                CalibrationStatusCard(
                    status = calibrationStatus,
                    procedure = currentProcedure
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // ADAS Systems List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(supportedSystems) { system ->
                    AdasSystemCard(
                        system = system,
                        isCalibrating = currentProcedure?.system == system,
                        onStartCalibration = {
                            viewModel.startCalibration(system)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VehicleSelectionCard(
    onVehicleSelected: (String, String, String) -> Unit
) {
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2E3B4E).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Select Vehicle",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = make,
                onValueChange = { make = it },
                label = { Text("Make", color = Color(0xFF90CAF9)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF64B5F6),
                    unfocusedBorderColor = Color(0xFF90CAF9)
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model", color = Color(0xFF90CAF9)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF64B5F6),
                    unfocusedBorderColor = Color(0xFF90CAF9)
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = year,
                onValueChange = { year = it },
                label = { Text("Year", color = Color(0xFF90CAF9)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF64B5F6),
                    unfocusedBorderColor = Color(0xFF90CAF9)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (make.isNotBlank() && model.isNotBlank() && year.isNotBlank()) {
                        onVehicleSelected(make, model, year)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF64B5F6)
                )
            ) {
                Text("Select Vehicle", color = Color.White)
            }
        }
    }
}

@Composable
fun CalibrationStatusCard(
    status: AdasCalibrationSystem.CalibrationStatus,
    procedure: AdasCalibrationSystem.AdasProcedure?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                AdasCalibrationSystem.CalibrationStatus.COMPLETED -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                AdasCalibrationSystem.CalibrationStatus.FAILED -> Color(0xFFE53E3E).copy(alpha = 0.2f)
                AdasCalibrationSystem.CalibrationStatus.CALIBRATING -> Color(0xFFFF9800).copy(alpha = 0.2f)
                else -> Color(0xFF2E3B4E).copy(alpha = 0.9f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (status) {
                        AdasCalibrationSystem.CalibrationStatus.COMPLETED -> Icons.Default.CheckCircle
                        AdasCalibrationSystem.CalibrationStatus.FAILED -> Icons.Default.Error
                        AdasCalibrationSystem.CalibrationStatus.CALIBRATING -> Icons.Default.Settings
                        else -> Icons.Default.Info
                    },
                    contentDescription = status.name,
                    tint = when (status) {
                        AdasCalibrationSystem.CalibrationStatus.COMPLETED -> Color(0xFF4CAF50)
                        AdasCalibrationSystem.CalibrationStatus.FAILED -> Color(0xFFE53E3E)
                        AdasCalibrationSystem.CalibrationStatus.CALIBRATING -> Color(0xFFFF9800)
                        else -> Color(0xFF64B5F6)
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Status: ${status.name.replace("_", " ")}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (procedure != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "System: ${procedure.system.name.replace("_", " ")}",
                    color = Color(0xFF90CAF9),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Estimated Time: ${procedure.estimatedTime} minutes",
                    color = Color(0xFF90CAF9),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun AdasSystemCard(
    system: AdasCalibrationSystem.AdasSystem,
    isCalibrating: Boolean,
    onStartCalibration: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCalibrating) {
                Color(0xFFFF9800).copy(alpha = 0.2f)
            } else {
                Color(0xFF2E3B4E).copy(alpha = 0.9f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getSystemIcon(system),
                contentDescription = system.name,
                tint = if (isCalibrating) Color(0xFFFF9800) else Color(0xFF64B5F6),
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = system.name.replace("_", " "),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = getSystemDescription(system),
                    color = Color(0xFF90CAF9),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            if (isCalibrating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFFFF9800),
                    strokeWidth = 2.dp
                )
            } else {
                Button(
                    onClick = onStartCalibration,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF64B5F6)
                    )
                ) {
                    Text("Calibrate", color = Color.White)
                }
            }
        }
    }
}

fun getSystemIcon(system: AdasCalibrationSystem.AdasSystem): ImageVector {
    return when (system) {
        AdasCalibrationSystem.AdasSystem.FORWARD_COLLISION_WARNING -> Icons.Default.Warning
        AdasCalibrationSystem.AdasSystem.LANE_DEPARTURE_WARNING -> Icons.Default.Timeline
        AdasCalibrationSystem.AdasSystem.BLIND_SPOT_MONITORING -> Icons.Default.Visibility
        AdasCalibrationSystem.AdasSystem.ADAPTIVE_CRUISE_CONTROL -> Icons.Default.Speed
        AdasCalibrationSystem.AdasSystem.PARKING_ASSIST -> Icons.Default.LocalParking
        AdasCalibrationSystem.AdasSystem.NIGHT_VISION -> Icons.Default.Visibility
        AdasCalibrationSystem.AdasSystem.TRAFFIC_SIGN_RECOGNITION -> Icons.Default.Traffic
        AdasCalibrationSystem.AdasSystem.DRIVER_ATTENTION_MONITORING -> Icons.Default.Face
        AdasCalibrationSystem.AdasSystem.AUTOMATIC_EMERGENCY_BRAKING -> Icons.Default.PanTool
        AdasCalibrationSystem.AdasSystem.LANE_KEEPING_ASSIST -> Icons.Default.Assistant
    }
}

fun getSystemDescription(system: AdasCalibrationSystem.AdasSystem): String {
    return when (system) {
        AdasCalibrationSystem.AdasSystem.FORWARD_COLLISION_WARNING -> "Warns of potential front collisions"
        AdasCalibrationSystem.AdasSystem.LANE_DEPARTURE_WARNING -> "Alerts when leaving lane without signal"
        AdasCalibrationSystem.AdasSystem.BLIND_SPOT_MONITORING -> "Monitors blind spots for vehicles"
        AdasCalibrationSystem.AdasSystem.ADAPTIVE_CRUISE_CONTROL -> "Maintains safe following distance"
        AdasCalibrationSystem.AdasSystem.PARKING_ASSIST -> "Assists with parking maneuvers"
        AdasCalibrationSystem.AdasSystem.NIGHT_VISION -> "Enhanced visibility in low light"
        AdasCalibrationSystem.AdasSystem.TRAFFIC_SIGN_RECOGNITION -> "Recognizes and displays traffic signs"
        AdasCalibrationSystem.AdasSystem.DRIVER_ATTENTION_MONITORING -> "Monitors driver alertness"
        AdasCalibrationSystem.AdasSystem.AUTOMATIC_EMERGENCY_BRAKING -> "Automatic braking in emergencies"
        AdasCalibrationSystem.AdasSystem.LANE_KEEPING_ASSIST -> "Helps maintain lane position"
    }
}

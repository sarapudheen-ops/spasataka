package com.spacetec.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spacetec.viewmodel.VehicleDataViewModel
import com.spacetec.viewmodel.VehicleDataViewModelFactory
import com.spacetec.vehicle.VehicleData
import com.spacetec.vehicle.model.EcuInfo
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDashboard(
    viewModel: VehicleDataViewModel = viewModel(
        factory = VehicleDataViewModelFactory(LocalContext.current)
    )
) {
    val vehicleData by viewModel.vehicleData.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val speed = vehicleData.speed
    val rpm = vehicleData.rpm
    val coolantTemperature = vehicleData.coolantTemp
    val fuelLevel = vehicleData.fuelLevel
    val engineLoad = vehicleData.engineLoad
    val throttlePosition = vehicleData.throttlePosition
    val intakeAirTemperature = vehicleData.intakeAirTemp
    val maf = vehicleData.mafRate
    val timingAdvance = vehicleData.timingAdvance
    val dtcCount = vehicleData.dtcList.size
    val fuelPressure = vehicleData.fuelRailPressure
    val engineRuntime = vehicleData.runTime
    val distanceWithMil = vehicleData.distanceWithMIL
    
    // Connect to OBD on first launch
    LaunchedEffect(Unit) {
        if (!isConnected && !isLoading) {
            viewModel.connectToObd()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SpaceTec OBD")
                    }
                },
                actions = {
                    // Connection status indicator
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(24.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = if (isConnected) "Connected" else "Disconnected",
                                tint = if (isConnected) Color.Green else Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                VehicleStatusCard(vehicleData)
            }
            
            item {
                GaugeRow(vehicleData)
            }
            
            item {
                Text(
                    "Vehicle Information",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                VehicleInfoCard(vehicleData)
            }
            
            if (vehicleData.dtcCount > 0) {
                item {
                    Text(
                        "Diagnostic Trouble Codes (${vehicleData.dtcCount})",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    vehicleData.dtcList.forEach { dtc ->
                        DtcCard(dtc)
                    }
                }
            }
            
            item {
                Text(
                    "ECU Information",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                vehicleData.ecuInfo.forEach { ecu ->
                    EcuCard(ecu)
                }
            }
        }
    }
}

@Composable
private fun VehicleStatusCard(vehicleData: VehicleData) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (vehicleData.hasCriticalAlerts()) 
                MaterialTheme.colorScheme.errorContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (vehicleData.isEngineRunning) Icons.Default.DirectionsCar 
                            else Icons.Default.PowerOff,
                contentDescription = "Vehicle Status",
                tint = if (vehicleData.isEngineRunning) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    vehicleData.getStatusMessage(),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (vehicleData.hasCriticalAlerts()) 
                        MaterialTheme.colorScheme.onErrorContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${vehicleData.speed} km/h • ${vehicleData.rpm} RPM • ${vehicleData.coolantTemp}°C",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun GaugeRow(vehicleData: VehicleData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        GaugeItem(
            value = vehicleData.speed.toFloat(),
            maxValue = 240f,
            label = "SPEED",
            unit = "km/h",
            color = MaterialTheme.colorScheme.primary
        )
        
        GaugeItem(
            value = vehicleData.rpm.toFloat(),
            maxValue = 8000f,
            label = "RPM",
            formatValue = { it.roundToInt().toString() },
            color = MaterialTheme.colorScheme.secondary
        )
        
        GaugeItem(
            value = vehicleData.coolantTemp.toFloat(),
            maxValue = 150f,
            label = "TEMP",
            unit = "°C",
            color = if (vehicleData.coolantTemp > 110) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun GaugeItem(
    value: Float,
    maxValue: Float,
    label: String,
    unit: String = "",
    formatValue: (Float) -> String = { "%.1f$unit".format(it) },
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        CircularProgressIndicator(
            progress = { (value / maxValue).coerceIn(0f, 1f) },
            modifier = Modifier.size(80.dp),
            color = color,
            strokeWidth = 8.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            formatValue(value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VehicleInfoCard(vehicleData: VehicleData) {
    val info = vehicleData.vehicleInfo ?: return
    
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            InfoRow("Make", info.make)
            InfoRow("Model", info.model)
            InfoRow("Year", info.year.toString())
            InfoRow("Engine", info.engineCode)
            InfoRow("Transmission", info.transmissionType)
            InfoRow("Fuel Type", info.fuelType)
            InfoRow("VIN", info.vin.takeIf { it.isNotBlank() } ?: "Not Available")
        }
    }
}

@Composable
private fun DtcCard(dtc: com.spacetec.vehicle.DiagnosticTroubleCode) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (dtc.severity) {
                com.spacetec.vehicle.DtcSeverity.HIGH -> MaterialTheme.colorScheme.errorContainer
                com.spacetec.vehicle.DtcSeverity.MEDIUM -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    dtc.code,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    dtc.status.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = when (dtc.status) {
                        com.spacetec.vehicle.DtcStatus.ACTIVE -> MaterialTheme.colorScheme.error
                        com.spacetec.vehicle.DtcStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                dtc.description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            InfoRow("Severity", dtc.severity.toString())
            InfoRow("Timestamp", dtc.timestamp.toString())
        }
    }
}

@Composable
private fun EcuCard(ecu: EcuInfo) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                ecu.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            InfoRow("Protocol", ecu.protocol)
            InfoRow("Address", ecu.address)
            
            if (ecu.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    ecu.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 14
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = fontSize.sp
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            fontSize = fontSize.sp
        )
    }
}

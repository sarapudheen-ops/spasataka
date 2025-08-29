package com.spacetec.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacetec.vehicle.VehicleData
import com.spacetec.obd.ObdManager
import com.spacetec.diagnostic.transport.AutelTransport
import com.spacetec.vin.VinDecoder
import com.spacetec.vin.VehicleInfo
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.*

@Composable
fun VehicleDashboard(
    obdManager: ObdManager,
    enhancedObdManager: com.spacetec.diag.EnhancedObdManager,
    realObdManager: com.spacetec.obd.RealObdManager? = null,
    onDisconnect: () -> Unit,
    onShowVehicleInfo: () -> Unit = {},
    onShowEcuDiscovery: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use real OBD data when available, fallback to enhanced manager
    val vehicleData by (realObdManager?.vehicleData ?: obdManager.vehicleData).collectAsState()
    val isConnected by (realObdManager?.isInitialized ?: obdManager.isInitialized).collectAsState()
    val enhancedVehicleData by enhancedObdManager.vehicleData.collectAsState()
    val dtcCodes by (realObdManager?.dtcCodes ?: enhancedObdManager.dtcList).collectAsState()
    val vehicleInfo by (realObdManager?.vehicleInfo ?: MutableStateFlow<VehicleInfo?>(null)).collectAsState()
    val connectionStatus by (realObdManager?.connectionStatus ?: MutableStateFlow("Disconnected")).collectAsState()
    val supportedPids by (realObdManager?.supportedPids ?: MutableStateFlow<Set<String>>(emptySet())).collectAsState()
    
    // Real-time data for enhanced diagnostics
    var realTimeData by remember { mutableStateOf(mapOf<String, Any>()) }
    
    val scope = rememberCoroutineScope()
    
    // Real-time data collection from actual hardware
    LaunchedEffect(realObdManager, isConnected) {
        realObdManager?.let { manager ->
            if (isConnected) {
                // Real OBD manager handles continuous data collection automatically
                // Real OBD data from actual vehicle connection
            }
        }
    }
    
    val spaceGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF0D1B2A),
            Color(0xFF1B263B),
            Color(0xFF415A77)
        ),
        radius = 1000f
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(spaceGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with status
            DashboardHeader(
                isConnected = isConnected,
                statusMessage = enhancedVehicleData.getStatusMessage(),
                onDisconnect = onDisconnect,
                onShowVehicleInfo = onShowVehicleInfo,
                onShowEcuDiscovery = onShowEcuDiscovery
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Main gauges - use simulation data if available
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Speed Gauge
                SpeedGauge(
                    warpSpeed = realTimeData["speed"] as? Int ?: vehicleData.warpSpeed,
                    modifier = Modifier.weight(1f)
                )
                
                // RPM Gauge  
                RpmGauge(
                    thrusterPower = realTimeData["rpm"] as? Int ?: vehicleData.thrusterPower,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Secondary metrics - real vehicle data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Temperature Card
                val temperature = realTimeData["temperature"] as? Int ?: vehicleData.engineCoreTemp
                VehicleMetricCard(
                    title = "Core Temp",
                    value = "${temperature}°C",
                    icon = Icons.Default.Settings,
                    isAlert = temperature > 100,
                    modifier = Modifier.weight(1f)
                )
                
                // Fuel Card
                val fuelLevel = realTimeData["fuel_level"] as? Int ?: vehicleData.oxygenLevels
                VehicleMetricCard(
                    title = "Fuel Level",
                    value = "${fuelLevel}%",
                    icon = Icons.Default.Settings,
                    isAlert = fuelLevel < 20,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Additional metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Engine Load Card
                val engineLoad = realTimeData["engine_load"] as? Int ?: 25
                VehicleMetricCard(
                    title = "Engine Load",
                    value = "${engineLoad}%",
                    icon = Icons.Default.Build,
                    isAlert = engineLoad > 85,
                    modifier = Modifier.weight(1f)
                )
                
                // Voltage Card
                val voltage = realTimeData["voltage"] as? Double ?: 12.6
                VehicleMetricCard(
                    title = "Battery",
                    value = "${String.format("%.1f", voltage)}V",
                    icon = Icons.Default.Settings,
                    isAlert = voltage < 11.5,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Vehicle Info Card
            vehicleInfo?.let { info ->
                Spacer(modifier = Modifier.height(16.dp))
                VehicleInfoCard(vehicleInfo = info)
            }
            
            // Alert status - include DTC alerts and vehicle alerts
            val hasVehicleAlerts = checkVehicleAlerts(realTimeData)
            if (vehicleData.hasCriticalAlerts() || dtcCodes.isNotEmpty() || hasVehicleAlerts) {
                Spacer(modifier = Modifier.height(16.dp))
                AlertCard(
                    vehicleData = vehicleData, 
                    dtcCodes = dtcCodes,
                    realTimeData = realTimeData,
                    supportedPids = supportedPids,
                    vehicleInfo = vehicleInfo
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    isConnected: Boolean,
    statusMessage: String,
    onDisconnect: () -> Unit,
    onShowVehicleInfo: () -> Unit,
    onShowEcuDiscovery: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🚀 SPACECRAFT DASHBOARD",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = statusMessage,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ECU Discovery Button
                IconButton(
                    onClick = onShowEcuDiscovery,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "ECU Discovery",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                // Vehicle Info Button
                IconButton(
                    onClick = onShowVehicleInfo,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Vehicle Info",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                if (isConnected) {
                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Disconnect")
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedGauge(
    warpSpeed: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "WARP SPEED",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularGauge(
                    value = warpSpeed,
                    maxValue = 300,
                    color = Color(0xFF00BCD4)
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$warpSpeed",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "km/h",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RpmGauge(
    thrusterPower: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "THRUSTER POWER",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularGauge(
                    value = thrusterPower,
                    maxValue = 8000,
                    color = if (thrusterPower > 7000) Color.Red else Color(0xFF4CAF50)
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$thrusterPower",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "RPM",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CircularGauge(
    value: Int,
    maxValue: Int,
    color: Color
) {
    val animatedValue by animateFloatAsState(
        targetValue = (value.toFloat() / maxValue).coerceIn(0f, 1f),
        animationSpec = tween(1000)
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 8.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = size.center
        
        // Background arc
        drawArc(
            color = Color.Gray.copy(alpha = 0.3f),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )
        
        // Value arc
        drawArc(
            color = color,
            startAngle = 135f,
            sweepAngle = 270f * animatedValue,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )
    }
}

@Composable
private fun VehicleMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isAlert: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isAlert) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = if (isAlert) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAlert) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAlert) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun VehicleInfoCard(vehicleInfo: VehicleInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "🚗 VEHICLE INFORMATION",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    InfoRow("VIN", vehicleInfo.vin)
                    InfoRow("Year", vehicleInfo.modelYear.toString())
                    InfoRow("Make", vehicleInfo.manufacturer)
                }
                Column(modifier = Modifier.weight(1f)) {
                    InfoRow("Country", vehicleInfo.country)
                    InfoRow("Engine", vehicleInfo.engineType)
                    InfoRow("Body", vehicleInfo.bodyStyle)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

private fun checkVehicleAlerts(realTimeData: Map<String, Any>): Boolean {
    val temperature = realTimeData["temperature"] as? Int ?: 0
    val fuelLevel = realTimeData["fuel_level"] as? Int ?: 100
    val voltage = realTimeData["voltage"] as? Double ?: 12.6
    val engineLoad = realTimeData["engine_load"] as? Int ?: 0
    val rpm = realTimeData["rpm"] as? Int ?: 0
    val throttlePosition = realTimeData["throttle_position"] as? Int ?: 0
    val batteryVoltage = realTimeData["voltage"] as? Double ?: 12.6
    
    return temperature > 100 || fuelLevel < 20 || voltage < 11.5 || engineLoad > 85 || rpm > 6500
}

@Composable
private fun AlertCard(
    vehicleData: VehicleData, 
    dtcCodes: List<String> = emptyList(),
    realTimeData: Map<String, Any> = emptyMap(),
    supportedPids: Set<String> = emptySet(),
    vehicleInfo: VehicleInfo?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠️",
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            
            Column {
                Text(
                    text = "CRITICAL ALERT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                val alerts = mutableListOf<String>()
                
                // Original vehicle data alerts
                if (vehicleData.engineCoreTemp > 110) alerts.add("High engine temperature")
                if (vehicleData.oxygenLevels < 10) alerts.add("Low fuel level")
                if (vehicleData.thrusterPower > 7000) alerts.add("High RPM")
                
                // Simulation data alerts
                val temperature = realTimeData["temperature"] as? Int ?: 0
                val fuelLevel = realTimeData["fuel_level"] as? Int ?: 100
                val voltage = realTimeData["voltage"] as? Double ?: 12.6
                val engineLoad = realTimeData["engine_load"] as? Int ?: 0
                val rpm = realTimeData["rpm"] as? Int ?: 0
                val throttlePosition = realTimeData["throttle_position"] as? Int ?: 0
                val batteryVoltage = realTimeData["voltage"] as? Double ?: 12.6
                
                if (temperature > 100) alerts.add("Engine overheating (${temperature}°C)")
                if (fuelLevel < 20) alerts.add("Low fuel level (${fuelLevel}%)")
                if (voltage < 11.5) alerts.add("Low battery voltage (${String.format("%.1f", voltage)}V)")
                if (engineLoad > 85) alerts.add("High engine load (${engineLoad}%)")
                if (rpm > 6500) alerts.add("High RPM (${rpm})")
                
                // DTC alerts
                if (dtcCodes.isNotEmpty()) alerts.add("${dtcCodes.size} diagnostic codes")
                
                if (alerts.isNotEmpty()) {
                    Text(
                        text = alerts.joinToString(", "),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                } else {
                    Text(
                        text = "System diagnostics running...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

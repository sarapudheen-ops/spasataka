package com.spacetec.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.isActive
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spacetec.R
import com.spacetec.ui.theme.*
import com.spacetec.ui.viewmodel.DiagnosticViewModel
import com.spacetec.util.DataFormatter
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveDataScreen(
    onNavigateBack: () -> Unit,
    viewModel: DiagnosticViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Collect vehicle data from ViewModel
    val vehicleData by viewModel.vehicleData.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    
    // Format data for display
    val rpm = (vehicleData.rpm * 1.0).toInt()
    val speed = vehicleData.speed
    val coolantTemp = vehicleData.coolantTemp
    val throttlePos = vehicleData.throttlePosition
    val engineLoad = vehicleData.engineLoad
    val fuelLevel = vehicleData.fuelLevel
    val intakeAirTemp = vehicleData.intakeAirTemp
    val mafRate = vehicleData.mafRate
    val timingAdvance = vehicleData.timingAdvance
    val fuelRailPressure = vehicleData.fuelRailPressure
    val controlModuleVoltage = vehicleData.controlModuleVoltage
    
    // Auto-refresh data when screen is visible
    LaunchedEffect(Unit) {
        viewModel.startDataCollection()
    }
    
    // Stop data collection when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopDataCollection()
        }
    }
    
    // Data log items
    val dataLogItems = listOf(
        "Intake Air Temp" to if (intakeAirTemp > -40) "$intakeAirTemp°C" else "N/A",
        "MAF Rate" to if (mafRate >= 0) "${mafRate.toInt()} g/s" else "N/A",
        "Timing Advance" to if (timingAdvance > -100) "${timingAdvance.toInt()}°" else "N/A",
        "Fuel Rail Pressure" to if (fuelRailPressure >= 0) "${fuelRailPressure.toInt()} kPa" else "N/A",
        "Control Module Voltage" to if (controlModuleVoltage > 0) String.format("%.1f V", controlModuleVoltage) else "N/A",
        "DTC Count" to "0 codes"
    )
    
    // Auto-refresh data when connected
    LaunchedEffect(isConnected) {
        if (isConnected) {
            while (isActive) {
                viewModel.refreshData()
                kotlinx.coroutines.delay(1000) // Update every second
            }
        }
    }
    
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Live Data",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Connection status indicator
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(24.dp)
                    ) {
                        Icon(
                            if (isConnected) Icons.Default.Bluetooth 
                            else Icons.Default.Warning,
                            contentDescription = if (isConnected) "Connected" else "Disconnected",
                            tint = if (isConnected) Color.Green else Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Refresh button
                    IconButton(
                        onClick = { 
                            coroutineScope.launch {
                                viewModel.refreshData()
                                // Scroll to top when refreshing
                                scrollState.scrollTo(0)
                            }
                        },
                        enabled = isConnected
                    ) {
                        Icon(
                            Icons.Default.Refresh, 
                            contentDescription = "Refresh Data",
                            tint = if (isConnected) MaterialTheme.colorScheme.primary 
                                 else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Connection status banner
                if (!isConnected) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = connectionStatus,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // Gauges Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // RPM Gauge
                GaugeCard(
                    title = "ENGINE RPM",
                    value = rpm,
                    unit = "RPM",
                    minValue = 0,
                    maxValue = 8000,
                    color = Color.Blue,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Speed Gauge
                GaugeCard(
                    title = "SPEED",
                    value = speed,
                    unit = "km/h",
                    minValue = 0,
                    maxValue = 220,
                    color = Color.Green,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Gauges Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Coolant Temp Gauge
                GaugeCard(
                    title = "COOLANT TEMP",
                    value = coolantTemp,
                    unit = "°C",
                    minValue = 50,
                    maxValue = 120,
                    color = if (coolantTemp > 90) Color.Red else Color.Blue,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Throttle Position Gauge
                GaugeCard(
                    title = "THROTTLE POS",
                    value = throttlePos,
                    unit = "%",
                    minValue = 0,
                    maxValue = 100,
                    color = Color.Yellow,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Gauges Row 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Engine Load Gauge
                GaugeCard(
                    title = "ENGINE LOAD",
                    value = engineLoad,
                    unit = "%",
                    minValue = 0,
                    maxValue = 100,
                    color = Color.Magenta,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Fuel Level Gauge
                GaugeCard(
                    title = "FUEL LEVEL",
                    value = fuelLevel,
                    unit = "%",
                    minValue = 0,
                    maxValue = 100,
                    color = if (fuelLevel < 20) Color.Red else Color.Green,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Data Log Section
            Text(
                text = "REALTIME DATA LOG",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            
            // Data Log Table
            DataLogTable(items = dataLogItems)
        }
    }
}

@Composable
private fun GaugeCard(
    title: String,
    value: Int,
    unit: String,
    minValue: Int,
    maxValue: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Gauge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                CircularGauge(
                    value = value,
                    minValue = minValue,
                    maxValue = maxValue,
                    primaryColor = color,
                    modifier = Modifier.fillMaxSize(0.9f)
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CircularGauge(
    value: Int,
    minValue: Int,
    maxValue: Int,
    primaryColor: Color,
    modifier: Modifier = Modifier,
    startAngle: Float = 150f,
    sweepAngle: Float = 240f,
    strokeWidth: Float = 16f
) {
    val animatedValue = animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "gauge_animation"
    )
    
    val progress = (animatedValue.value - minValue) / (maxValue - minValue)
    val angle = startAngle + (sweepAngle * progress.coerceIn(0f, 1f))
    
    Canvas(modifier = modifier) {
        val canvasSize = size.minDimension
        val centerOffset = Offset(size.width / 2, size.height / 2)
        val radius = canvasSize / 2 - strokeWidth / 2
        
        // Background track
        drawArc(
            color = Color.Gray.copy(alpha = 0.1f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        // Progress track
        drawArc(
            color = primaryColor,
            startAngle = startAngle,
            sweepAngle = (sweepAngle * progress).coerceIn(0f, sweepAngle),
            useCenter = false,
            topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        // Needle - simplified without rotation for now
        val needleLength = radius * 0.8f
        val needleWidth = strokeWidth * 0.6f
        val needleEndX = centerOffset.x + needleLength * kotlin.math.cos(Math.toRadians((angle - 90).toDouble())).toFloat()
        val needleEndY = centerOffset.y + needleLength * kotlin.math.sin(Math.toRadians((angle - 90).toDouble())).toFloat()
        
        // Needle shadow
        drawLine(
            color = Color.Black.copy(alpha = 0.3f),
            start = centerOffset,
            end = Offset(needleEndX + 2, needleEndY + 2),
            strokeWidth = needleWidth + 2,
            cap = StrokeCap.Round
        )
        
        // Needle
        drawLine(
            color = primaryColor,
            start = centerOffset,
            end = Offset(needleEndX, needleEndY),
            strokeWidth = needleWidth,
            cap = StrokeCap.Round
        )
        
        // Center cap
        drawCircle(
            color = primaryColor,
            radius = strokeWidth * 0.6f,
            center = centerOffset
        )
    }
}

@Composable
private fun DataLogTable(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            items.forEachIndexed { index, (name, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (index < items.lastIndex) {
                    Divider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

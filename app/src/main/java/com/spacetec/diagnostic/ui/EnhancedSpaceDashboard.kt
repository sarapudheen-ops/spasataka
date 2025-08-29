package com.spacetec.diagnostic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacetec.diagnostic.obd.ConnectionState
import com.spacetec.obd.SpaceHud
import kotlin.math.*

/**
 * Enhanced space-themed dashboard with improved data visualization
 * and accessibility support
 */
@Composable
fun EnhancedSpaceDashboard(
    vehicleData: Map<String, VehicleDataPoint>,
    connectionState: ConnectionState,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(16.dp)
    ) {
        // Header with connection status
        SpaceDashboardHeader(
            connectionState = connectionState,
            onReconnect = onReconnect
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isTablet) {
            TabletDashboardLayout(vehicleData)
        } else {
            PhoneDashboardLayout(vehicleData)
        }
    }
}

@Composable
private fun SpaceDashboardHeader(
    connectionState: ConnectionState,
    onReconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🚀 Mission Control",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                ConnectionStatusIndicator(
                    connectionState = connectionState,
                    onReconnect = onReconnect
                )
            }
            
            // Quick actions
            Row {
                IconButton(onClick = { /* Export data */ }) {
                    Icon(Icons.Default.Download, contentDescription = "Export Data")
                }
                IconButton(onClick = { /* Settings */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusIndicator(
    connectionState: ConnectionState,
    onReconnect: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val (statusText, statusColor, statusIcon) = when (connectionState) {
            ConnectionState.CONNECTED -> Triple(
                "🛰️ Connected",
                Color.Green,
                Icons.Default.Bluetooth
            )
            ConnectionState.CONNECTING -> Triple(
                "🔍 Scanning...",
                Color(0xFFFFEB3B),
                Icons.Default.Search
            )
            ConnectionState.DISCONNECTED -> Triple(
                "❌ Disconnected",
                Color.Red,
                Icons.Default.BluetoothDisabled
            )
            ConnectionState.TIMEOUT -> Triple(
                "⏱️ Timeout",
                Color(0xFFFF9800),
                Icons.Default.Warning
            )
            ConnectionState.ERROR -> Triple(
                "⚠️ Error",
                Color.Red,
                Icons.Default.Error
            )
        }
        
        Icon(
            imageVector = statusIcon,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(16.dp)
        )
        
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = statusColor
        )
        
        if (connectionState != ConnectionState.CONNECTED) {
            TextButton(
                onClick = onReconnect,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Reconnect")
            }
        }
    }
}

@Composable
private fun TabletDashboardLayout(
    vehicleData: Map<String, VehicleDataPoint>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Primary gauges (2x2 grid)
        item {
            val primaryPids = listOf("0C", "0D", "05", "2F")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(primaryPids.chunked(2)) { pidPair ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        pidPair.forEach { pid ->
                            vehicleData[pid]?.let { dataPoint ->
                                CircularSpaceGauge(
                                    dataPoint = dataPoint,
                                    modifier = Modifier.size(180.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Secondary data in compact cards
        item {
            val secondaryPids = vehicleData.keys - setOf("0C", "0D", "05", "2F")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(secondaryPids.toList()) { pid ->
                    vehicleData[pid]?.let { dataPoint ->
                        CompactDataCard(
                            dataPoint = dataPoint,
                            modifier = Modifier.width(140.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneDashboardLayout(
    vehicleData: Map<String, VehicleDataPoint>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Primary gauges (stacked)
        items(listOf("0C", "0D")) { pid ->
            vehicleData[pid]?.let { dataPoint ->
                CircularSpaceGauge(
                    dataPoint = dataPoint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
        
        // Secondary gauges (2 per row)
        item {
            val secondaryPids = listOf("05", "2F")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                secondaryPids.forEach { pid ->
                    vehicleData[pid]?.let { dataPoint ->
                        CircularSpaceGauge(
                            dataPoint = dataPoint,
                            modifier = Modifier
                                .weight(1f)
                                .height(160.dp)
                        )
                    }
                }
            }
        }
        
        // Additional data in compact format
        item {
            val additionalPids = vehicleData.keys - setOf("0C", "0D", "05", "2F")
            additionalPids.chunked(2).forEach { pidPair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pidPair.forEach { pid ->
                        vehicleData[pid]?.let { dataPoint ->
                            CompactDataCard(
                                dataPoint = dataPoint,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // Add empty space if odd number
                    if (pidPair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun CircularSpaceGauge(
    dataPoint: VehicleDataPoint,
    modifier: Modifier = Modifier
) {
    val spaceInfo = SpaceHud.getSpacePidInfo(dataPoint.name)
    val isCritical = SpaceHud.isValueCritical(dataPoint.name, dataPoint.value)
    
    Card(
        modifier = modifier
            .semantics {
                contentDescription = "${spaceInfo.displayName}: ${dataPoint.formattedValue}"
                role = androidx.compose.ui.semantics.Role.Image
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isCritical) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawCircularGauge(
                    value = dataPoint.value,
                    minValue = dataPoint.config.minValue,
                    maxValue = dataPoint.config.maxValue,
                    warningThreshold = dataPoint.config.warningThreshold,
                    criticalThreshold = dataPoint.config.criticalThreshold,
                    isCritical = isCritical
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = spaceInfo.icon,
                    fontSize = 24.sp
                )
                
                Text(
                    text = dataPoint.formattedValue,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCritical) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                
                Text(
                    text = spaceInfo.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CompactDataCard(
    dataPoint: VehicleDataPoint,
    modifier: Modifier = Modifier
) {
    val spaceInfo = SpaceHud.getSpacePidInfo(dataPoint.name)
    val isCritical = SpaceHud.isValueCritical(dataPoint.name, dataPoint.value)
    
    Card(
        modifier = modifier
            .semantics {
                contentDescription = "${spaceInfo.displayName}: ${dataPoint.formattedValue}"
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isCritical) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = spaceInfo.icon,
                    fontSize = 16.sp
                )
                
                Text(
                    text = dataPoint.formattedValue,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCritical) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
            
            Text(
                text = spaceInfo.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun DrawScope.drawCircularGauge(
    value: Double,
    minValue: Double,
    maxValue: Double,
    warningThreshold: Double?,
    criticalThreshold: Double?,
    isCritical: Boolean
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = min(size.width, size.height) / 2f - 20.dp.toPx()
    val strokeWidth = 12.dp.toPx()
    
    // Background arc
    drawArc(
        color = Color.Gray.copy(alpha = 0.3f),
        startAngle = 135f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(strokeWidth)
    )
    
    // Value arc
    val normalizedValue = ((value - minValue) / (maxValue - minValue)).coerceIn(0.0, 1.0)
    val sweepAngle = (normalizedValue * 270).toFloat()
    
    val valueColor = when {
        isCritical -> Color.Red
        warningThreshold != null && value >= warningThreshold -> Color(0xFFFF9800)
        else -> Color.Green
    }
    
    drawArc(
        color = valueColor,
        startAngle = 135f,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(strokeWidth, cap = StrokeCap.Round)
    )
    
    // Warning threshold indicator
    warningThreshold?.let { threshold ->
        val thresholdAngle = ((threshold - minValue) / (maxValue - minValue) * 270).toFloat()
        val startAngle = 135f + thresholdAngle
        val thresholdRadius = radius + strokeWidth / 2
        
        val startX = center.x + thresholdRadius * cos(Math.toRadians(startAngle.toDouble())).toFloat()
        val startY = center.y + thresholdRadius * sin(Math.toRadians(startAngle.toDouble())).toFloat()
        val endX = center.x + (thresholdRadius - strokeWidth) * cos(Math.toRadians(startAngle.toDouble())).toFloat()
        val endY = center.y + (thresholdRadius - strokeWidth) * sin(Math.toRadians(startAngle.toDouble())).toFloat()
        
        drawLine(
            color = Color(0xFFFF9800),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 3.dp.toPx()
        )
    }
}

data class VehicleDataPoint(
    val pid: String,
    val name: String,
    val value: Double,
    val unit: String,
    val formattedValue: String,
    val config: GaugeConfig,
    val timestamp: Long = System.currentTimeMillis()
)

data class GaugeConfig(
    val minValue: Double,
    val maxValue: Double,
    val warningThreshold: Double? = null,
    val criticalThreshold: Double? = null
)

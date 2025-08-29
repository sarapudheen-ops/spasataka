package com.spacetec.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*
import java.util.*
import com.spacetec.vehicle.VehicleData

// Uses real VehicleData from com.spacetec.vehicle.VehicleData

// --- Main Dashboard ---
@Composable
fun DesktopDashboard(vehicleData: VehicleData) {
    val spaceGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFF415A77))
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(spaceGradient)
            .padding(32.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left Panel - Main Gauges
        Column(
            modifier = Modifier.weight(2f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🚀 DEEP SPACE COMMAND CENTER",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AdvancedSpaceGauge(
                    title = "WARP SPEED",
                    value = vehicleData.warpSpeed,
                    maxValue = 300,
                    unit = "km/h",
                    color = Color(0xFF00BCD4),
                    modifier = Modifier.weight(1f).aspectRatio(1f)
                )
                AdvancedSpaceGauge(
                    title = "THRUSTER POWER",
                    value = vehicleData.thrusterPower,
                    maxValue = 8000,
                    unit = "RPM",
                    color = if (vehicleData.thrusterPower > 7000) Color.Red else Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f).aspectRatio(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AdvancedSpaceGauge(
                    title = "CORE TEMPERATURE",
                    value = vehicleData.engineCoreTemp,
                    maxValue = 150,
                    unit = "°C",
                    color = if (vehicleData.engineCoreTemp > 110) Color.Red else Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f).aspectRatio(1f)
                )
                AdvancedSpaceGauge(
                    title = "OXYGEN RESERVES",
                    value = vehicleData.oxygenLevels,
                    maxValue = 100,
                    unit = "%",
                    color = if (vehicleData.oxygenLevels < 10) Color.Red else Color(0xFF2196F3),
                    modifier = Modifier.weight(1f).aspectRatio(1f)
                )
            }
        }

        // Right Panel - System Info & Alerts
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SystemStatusPanel(vehicleData)
            DetailedMetricsPanel(vehicleData)
            if (vehicleData.hasCriticalAlerts()) {
                AlertCard(vehicleData)
            }
        }
    }
}

// --- Panels ---
@Composable
fun SystemStatusPanel(vehicleData: VehicleData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🛰️ SYSTEM STATUS",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            StatusRow("Engine Status", if (vehicleData.isEngineRunning) "ONLINE" else "OFFLINE", vehicleData.isEngineRunning)
            StatusRow("Temperature", if (vehicleData.engineCoreTemp > 110) "CRITICAL" else "NOMINAL", vehicleData.engineCoreTemp <= 110)
            StatusRow("Fuel Level", if (vehicleData.oxygenLevels < 10) "LOW" else "ADEQUATE", vehicleData.oxygenLevels >= 10)
            StatusRow("RPM Status", if (vehicleData.thrusterPower > 7000) "HIGH" else "NORMAL", vehicleData.thrusterPower <= 7000)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = vehicleData.getStatusMessage(),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun DetailedMetricsPanel(vehicleData: VehicleData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 DETAILED METRICS",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            MetricRow("Speed", "${vehicleData.warpSpeed} km/h")
            MetricRow("RPM", "${vehicleData.thrusterPower} RPM")
            MetricRow("Temperature", "${vehicleData.engineCoreTemp}°C")
            MetricRow("Fuel", "${vehicleData.oxygenLevels}%")
            MetricRow("Timestamp", java.text.SimpleDateFormat("HH:mm:ss").format(vehicleData.timestamp))
        }
    }
}

// Alerts handled by AlertCard in SpaceGauges.kt

// Gauges provided by SpaceGauges.kt

// --- Helpers ---
@Composable
private fun StatusRow(label: String, status: String, isGood: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = status,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isGood) Color.Green else Color.Red
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// StatusIndicator defined in ResponsiveDashboard.kt

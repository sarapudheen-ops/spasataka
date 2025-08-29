package com.spacetec.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.windowsizeclass.*
import com.spacetec.vehicle.VehicleData
import kotlin.math.*

@Composable
fun ResponsiveDashboard(
    windowSize: WindowSizeClass,
    vehicleData: VehicleData
) {
    when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Compact -> CompactDashboard(vehicleData)
        WindowWidthSizeClass.Medium -> TabletDashboard(vehicleData)
        WindowWidthSizeClass.Expanded -> DesktopDashboard(vehicleData)
    }
}

// --- Compact ---
@Composable
fun CompactDashboard(vehicleData: VehicleData) {
    val spaceGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFF415A77))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(spaceGradient)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🚀 SPACECRAFT DASHBOARD",
            fontSize = 20.sp,
            color = Color.White
        )

        SpaceGauge(
            title = "WARP SPEED",
            value = vehicleData.warpSpeed,
            maxValue = 300,
            unit = "km/h",
            color = Color(0xFF00BCD4),
            modifier = Modifier.fillMaxWidth().height(200.dp)
        )

        SpaceGauge(
            title = "THRUSTER POWER",
            value = vehicleData.thrusterPower,
            maxValue = 8000,
            unit = "RPM",
            color = if (vehicleData.thrusterPower > 7000) Color.Red else Color(0xFF2196F3),
            modifier = Modifier.fillMaxWidth().height(200.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactMetricCard(
                title = "CORE TEMP",
                value = "${vehicleData.engineCoreTemp}°C",
                isAlert = vehicleData.engineCoreTemp > 110,
                modifier = Modifier.weight(1f)
            )
            CompactMetricCard(
                title = "OXYGEN",
                value = "${vehicleData.oxygenLevels}%",
                isAlert = vehicleData.oxygenLevels < 10,
                modifier = Modifier.weight(1f)
            )
        }

        if (vehicleData.hasCriticalAlerts()) {
            AlertCard(vehicleData)
        }
    }
}

// --- Tablet ---
@Composable
fun TabletDashboard(vehicleData: VehicleData) {
    val spaceGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFF415A77)),
        radius = 1200f
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(spaceGradient)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🚀 MISSION CONTROL",
                fontSize = 24.sp,
                color = Color.White
            )
            StatusIndicator(vehicleData)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EnhancedSpaceGauge(
                    title = "WARP SPEED",
                    value = vehicleData.warpSpeed,
                    maxValue = 300,
                    unit = "km/h",
                    color = Color(0xFF00BCD4),
                    modifier = Modifier.aspectRatio(1f)
                )
                EnhancedSpaceGauge(
                    title = "CORE TEMP",
                    value = vehicleData.engineCoreTemp,
                    maxValue = 150,
                    unit = "°C",
                    color = if (vehicleData.engineCoreTemp > 110) Color.Red else Color(0xFF2196F3),
                    modifier = Modifier.aspectRatio(1f)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EnhancedSpaceGauge(
                    title = "THRUSTER POWER",
                    value = vehicleData.thrusterPower,
                    maxValue = 8000,
                    unit = "RPM",
                    color = if (vehicleData.thrusterPower > 7000) Color.Red else Color(0xFF2196F3),
                    modifier = Modifier.aspectRatio(1f)
                )
                EnhancedSpaceGauge(
                    title = "OXYGEN LEVELS",
                    value = vehicleData.oxygenLevels,
                    maxValue = 100,
                    unit = "%",
                    color = if (vehicleData.oxygenLevels < 10) Color.Red else Color(0xFF2196F3),
                    modifier = Modifier.aspectRatio(1f)
                )
            }
        }

        if (vehicleData.hasCriticalAlerts()) {
            Spacer(modifier = Modifier.height(16.dp))
            AlertCard(vehicleData)
        }
    }
}

@Composable
fun StatusIndicator(vehicleData: VehicleData) {
    val color = if (vehicleData.hasCriticalAlerts()) Color.Red else if (!vehicleData.isEngineRunning) Color.Gray else Color(0xFF2196F3)
    Text("Status", color = color)
}

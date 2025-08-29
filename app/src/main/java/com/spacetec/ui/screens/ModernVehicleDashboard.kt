package com.spacetec.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacetec.ui.components.*
import com.spacetec.ui.theme.SpaceColors
import com.spacetec.ui.theme.SpaceTextStyles
import com.spacetec.diagnostic.obd.SpaceHud

@Composable
fun ModernVehicleDashboard(
    vehicleData: Map<String, String> = emptyMap(),
    onDisconnect: () -> Unit,
    onShowVehicleInfo: () -> Unit = {},
    onShowEcuDiscovery: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        SpaceColors.CosmicDust.copy(alpha = 0.2f),
                        SpaceColors.DeepSpace,
                        SpaceColors.SpaceVoid
                    ),
                    radius = 800f
                )
            )
    ) {
        // Circuit pattern background
        CircuitPatternBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Spacecraft Cockpit Header
            SpacecraftCockpitHeader(
                onDisconnect = onDisconnect
            )
            
            // Primary Flight Data
            PrimaryFlightData(vehicleData)
            
            // Secondary Systems Grid
            SecondarySystemsGrid(vehicleData)
            
            // Environmental Systems
            EnvironmentalSystems(vehicleData)
            
            // Navigation Controls
            NavigationControls(
                onShowVehicleInfo = onShowVehicleInfo,
                onShowEcuDiscovery = onShowEcuDiscovery
            )
        }
    }
}

@Composable
private fun SpacecraftCockpitHeader(
    onDisconnect: () -> Unit
) {
    SpaceHudCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = SpaceColors.ElectricBlue
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SPACECRAFT SYSTEMS",
                    style = SpaceTextStyles.MissionTitle.copy(fontSize = 24.sp),
                    color = SpaceColors.ElectricBlue
                )
                Text(
                    text = "ACTIVE VEHICLE MONITORING",
                    style = SpaceTextStyles.TechnicalData,
                    color = SpaceColors.MetallicSilver.copy(alpha = 0.8f)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TechnicalStatusIndicator(
                    status = SystemStatus.OPERATIONAL,
                    label = "ONLINE"
                )
                
                AstronautControlButton(
                    onClick = onDisconnect,
                    text = "DISCONNECT",
                    icon = Icons.Default.PowerOff,
                    isActive = false,
                    buttonType = ControlButtonType.CRITICAL
                )
            }
        }
    }
}

@Composable
private fun PrimaryFlightData(
    vehicleData: Map<String, String>
) {
    SpaceHudCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = SpaceColors.SafeGreen
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "PRIMARY FLIGHT DATA",
                style = SpaceTextStyles.SystemAlert.copy(fontSize = 16.sp),
                color = SpaceColors.SafeGreen
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(getPrimaryFlightParameters(vehicleData)) { param ->
                    PrimaryDataDisplay(
                        parameter = param
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryDataDisplay(
    parameter: FlightParameter
) {
    SpaceHudCard(
        modifier = Modifier.width(140.dp),
        glowColor = parameter.statusColor,
        isActive = parameter.status != SystemStatus.OFFLINE
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = parameter.icon,
                style = SpaceTextStyles.HudDisplay.copy(fontSize = 32.sp)
            )
            
            Text(
                text = parameter.value,
                style = SpaceTextStyles.HudDisplay.copy(
                    fontSize = 20.sp,
                    color = parameter.statusColor
                )
            )
            
            Text(
                text = parameter.unit,
                style = SpaceTextStyles.TechnicalData.copy(fontSize = 10.sp),
                color = SpaceColors.MetallicSilver.copy(alpha = 0.7f)
            )
            
            Text(
                text = parameter.name,
                style = SpaceTextStyles.StatusIndicator.copy(fontSize = 10.sp),
                color = SpaceColors.MetallicSilver
            )
        }
    }
}

@Composable
private fun SecondarySystemsGrid(
    vehicleData: Map<String, String>
) {
    SpaceHudCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = SpaceColors.SystemPurple
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "SECONDARY SYSTEMS",
                style = SpaceTextStyles.SystemAlert.copy(fontSize = 16.sp),
                color = SpaceColors.SystemPurple
            )
            
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(getSecondarySystemParameters(vehicleData)) { param ->
                    SecondarySystemRow(parameter = param)
                }
            }
        }
    }
}

@Composable
private fun SecondarySystemRow(
    parameter: FlightParameter
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = parameter.icon,
                style = SpaceTextStyles.StatusIndicator.copy(fontSize = 16.sp)
            )
            
            Text(
                text = parameter.name,
                style = SpaceTextStyles.TechnicalData,
                color = SpaceColors.MetallicSilver
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${parameter.value} ${parameter.unit}",
                style = SpaceTextStyles.StatusIndicator,
                color = parameter.statusColor
            )
            
            TechnicalStatusIndicator(
                status = parameter.status
            )
        }
    }
}

@Composable
private fun EnvironmentalSystems(
    vehicleData: Map<String, String>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HudDataPanel(
            title = "CORE TEMP",
            value = vehicleData["coolantTemp"] ?: "--",
            unit = "°C",
            status = getTemperatureStatus(vehicleData["coolantTemp"]),
            icon = "🌡️",
            modifier = Modifier.weight(1f)
        )
        
        HudDataPanel(
            title = "FUEL RES",
            value = vehicleData["fuelLevel"] ?: "--",
            unit = "%",
            status = getFuelStatus(vehicleData["fuelLevel"]),
            icon = "⛽",
            modifier = Modifier.weight(1f)
        )
        
        HudDataPanel(
            title = "PWR CELL",
            value = vehicleData["voltage"] ?: "--",
            unit = "V",
            status = getVoltageStatus(vehicleData["voltage"]),
            icon = "🔋",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NavigationControls(
    onShowVehicleInfo: () -> Unit,
    onShowEcuDiscovery: () -> Unit
) {
    SpaceHudCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = SpaceColors.Warning
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "NAVIGATION & DIAGNOSTICS",
                style = SpaceTextStyles.SystemAlert.copy(fontSize = 16.sp),
                color = SpaceColors.Warning
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AstronautControlButton(
                    onClick = onShowVehicleInfo,
                    text = "VEHICLE INFO",
                    icon = Icons.Default.Info,
                    isActive = false,
                    buttonType = ControlButtonType.PRIMARY,
                    modifier = Modifier.weight(1f)
                )
                
                AstronautControlButton(
                    onClick = onShowEcuDiscovery,
                    text = "ECU SCAN",
                    icon = Icons.Default.Scanner,
                    isActive = false,
                    buttonType = ControlButtonType.WARNING,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Data classes and helper functions
data class FlightParameter(
    val name: String,
    val value: String,
    val unit: String,
    val icon: String,
    val status: SystemStatus,
    val statusColor: androidx.compose.ui.graphics.Color
)

private fun getPrimaryFlightParameters(vehicleData: Map<String, String>): List<FlightParameter> {
    return listOf(
        FlightParameter(
            name = "THRUST",
            value = vehicleData["rpm"] ?: "--",
            unit = "RPM",
            icon = "🚀",
            status = SystemStatus.OPERATIONAL,
            statusColor = SpaceColors.SafeGreen
        ),
        FlightParameter(
            name = "VELOCITY",
            value = vehicleData["speed"] ?: "--",
            unit = "KM/H",
            icon = "⚡",
            status = SystemStatus.OPERATIONAL,
            statusColor = SpaceColors.ElectricBlue
        ),
        FlightParameter(
            name = "THROTTLE",
            value = vehicleData["throttle"] ?: "--",
            unit = "%",
            icon = "🎚️",
            status = SystemStatus.OPERATIONAL,
            statusColor = SpaceColors.Warning
        ),
        FlightParameter(
            name = "LOAD",
            value = vehicleData["load"] ?: "--",
            unit = "%",
            icon = "⚙️",
            status = SystemStatus.OPERATIONAL,
            statusColor = SpaceColors.SystemPurple
        )
    )
}

private fun getSecondarySystemParameters(vehicleData: Map<String, String>): List<FlightParameter> {
    val allData = mutableListOf<FlightParameter>()
    
    // Convert vehicle data to space-themed parameters using SpaceHud
    vehicleData.forEach { (key, value) ->
        if (key !in listOf("rpm", "speed", "throttle", "load", "coolantTemp", "fuelLevel", "voltage")) {
            val spaceInfo = SpaceHud.getSpacePidInfo(key)
            allData.add(
                FlightParameter(
                    name = spaceInfo.displayName,
                    value = value,
                    unit = "",
                    icon = spaceInfo.icon,
                    status = if (SpaceHud.isValueCritical(key, value.toDoubleOrNull() ?: 0.0)) 
                        SystemStatus.CRITICAL else SystemStatus.OPERATIONAL,
                    statusColor = if (SpaceHud.isValueCritical(key, value.toDoubleOrNull() ?: 0.0))
                        SpaceColors.CriticalRed else SpaceColors.SafeGreen
                )
            )
        }
    }
    
    return allData
}

private fun getTemperatureStatus(temp: String?): SystemStatus {
    val temperature = temp?.toDoubleOrNull() ?: return SystemStatus.OFFLINE
    return when {
        temperature > 105.0 -> SystemStatus.CRITICAL
        temperature > 90.0 -> SystemStatus.WARNING
        else -> SystemStatus.OPERATIONAL
    }
}

private fun getFuelStatus(fuel: String?): SystemStatus {
    val fuelLevel = fuel?.toDoubleOrNull() ?: return SystemStatus.OFFLINE
    return when {
        fuelLevel < 15.0 -> SystemStatus.CRITICAL
        fuelLevel < 25.0 -> SystemStatus.WARNING
        else -> SystemStatus.OPERATIONAL
    }
}

private fun getVoltageStatus(voltage: String?): SystemStatus {
    val volt = voltage?.toDoubleOrNull() ?: return SystemStatus.OFFLINE
    return when {
        volt < 11.5 -> SystemStatus.CRITICAL
        volt < 12.0 -> SystemStatus.WARNING
        else -> SystemStatus.OPERATIONAL
    }
}

package com.spacetec.ui.screens

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacetec.bluetooth.SpaceBluetoothManager
import com.spacetec.ui.components.FloatingPlanet
import com.spacetec.ui.components.PlanetType
import com.spacetec.ui.components.RadarAnimation
import kotlin.math.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionControlScreen(
    isScanning: Boolean,
    discoveredDevices: List<BluetoothDevice>,
    connectionState: SpaceBluetoothManager.ConnectionState,
    statusMessage: String,
    onStartMission: () -> Unit,
    onStopMission: () -> Unit,
    onConnectToDevice: (BluetoothDevice) -> Unit,
    modifier: Modifier = Modifier
) {
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
        // Stars background
        StarsBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mission Control Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚀 MISSION CONTROL",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = statusMessage,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            // Radar Section
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                RadarAnimation(
                    isScanning = isScanning,
                    size = 280f
                )
                
                // Floating planets for discovered devices
                discoveredDevices.forEachIndexed { index, device ->
                    val position = calculatePlanetPosition(index, discoveredDevices.size)
                    
                    FloatingPlanet(
                        device = device,
                        position = position,
                        onClick = onConnectToDevice,
                        planetType = PlanetType.values()[index % PlanetType.values().size]
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Mission Control Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = if (isScanning) onStopMission else onStartMission,
                    modifier = Modifier
                        .height(56.dp)
                        .widthIn(min = 160.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = if (isScanning) "ABORT MISSION" else "START MISSION",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Connection Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (connectionState) {
                        SpaceBluetoothManager.ConnectionState.CONNECTED -> 
                            MaterialTheme.colorScheme.primaryContainer
                        SpaceBluetoothManager.ConnectionState.ERROR -> 
                            MaterialTheme.colorScheme.errorContainer
                        SpaceBluetoothManager.ConnectionState.CONNECTING -> 
                            MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    }
                )
            ) {
                Text(
                    text = "STATUS: ${connectionState.name}",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = when (connectionState) {
                        SpaceBluetoothManager.ConnectionState.CONNECTED -> 
                            MaterialTheme.colorScheme.onPrimaryContainer
                        SpaceBluetoothManager.ConnectionState.ERROR -> 
                            MaterialTheme.colorScheme.onErrorContainer
                        SpaceBluetoothManager.ConnectionState.CONNECTING -> 
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun StarsBackground() {
    val stars = remember {
        List(50) {
            Offset(
                x = Random.nextFloat(),
                y = Random.nextFloat()
            )
        }
    }
    
    androidx.compose.foundation.Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        stars.forEach { star ->
            drawCircle(
                color = Color.White.copy(alpha = Random.nextFloat() * 0.8f + 0.2f),
                radius = Random.nextFloat() * 3f + 1f,
                center = Offset(
                    star.x * size.width,
                    star.y * size.height
                )
            )
        }
    }
}

private fun calculatePlanetPosition(index: Int, totalDevices: Int): Offset {
    val radius = 120f
    val angle = (2 * PI * index / totalDevices).toFloat()
    
    return Offset(
        x = cos(angle) * radius,
        y = sin(angle) * radius
    )
}

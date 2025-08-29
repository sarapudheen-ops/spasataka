package com.spacetec.ui

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceMainScreen(
    onNavigateToDiagnostics: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToLiveData: () -> Unit = {},
    onNavigateToDtcScanner: () -> Unit = {},
    onNavigateToVehicleInfo: () -> Unit = {},
    viewModel: com.spacetec.ui.viewmodel.DiagnosticViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    
    // Get real connection status and vehicle data from ViewModel
    val uiState by viewModel.uiState
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val vehicleInfo by viewModel.vehicleInfo.collectAsState()
    val isConnected = uiState.isConnected
    
    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "space_animation")
    
    val titleGlow by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "title_glow"
    )
    
    val statusPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status_pulse"
    )
    
    val gradientColors = listOf(
        colors.primary,
        colors.primaryContainer,
        colors.secondary
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Floating stars background
        StarField()
        
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Title with glow effect
            Text(
                text = "SpaceTec Diagnostics",
                fontSize = 32.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = colors.primary.copy(alpha = titleGlow),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Text(
                text = "Professional Vehicle Diagnostics",
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                color = colors.onBackground.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Main Control Panels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Diagnostics Panel
                ControlPanel(
                    icon = Icons.Default.Settings,
                    title = "Diagnostics",
                    iconColor = colors.primary,
                    onClick = onNavigateToDiagnostics
                )
                
                // Settings Panel
                ControlPanel(
                    icon = Icons.Default.SettingsApplications,
                    title = "Settings",
                    iconColor = colors.secondary,
                    onClick = onNavigateToSettings
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Secondary Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Live Data Panel
                ControlPanel(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Speed,
                    title = "Live Data",
                    iconColor = colors.tertiary,
                    onClick = onNavigateToLiveData
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // DTC Scanner Panel
                ControlPanel(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Warning,
                    title = "DTC Scanner",
                    iconColor = colors.error,
                    onClick = onNavigateToDtcScanner
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Scan Button
                Button(
                    onClick = onNavigateToDtcScanner,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quick Scan")
                }
                
                // Vehicle Info Button
                Button(
                    onClick = onNavigateToVehicleInfo,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Vehicle Info")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Connection Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Status Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status indicator with pulse animation
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = if (isConnected) listOf(
                                            Color(0xFF00E676).copy(alpha = statusPulse * 0.7f),
                                            Color(0xFF00E676).copy(alpha = 0.1f * statusPulse)
                                        ) else listOf(
                                            Color(0xFFF44336).copy(alpha = statusPulse * 0.7f),
                                            Color(0xFFF44336).copy(alpha = 0.1f * statusPulse)
                                        )
                                    ),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Connection Info
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isConnected) "Connected to OBD2" else "OBD2 Disconnected",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isConnected) "ELM327 v1.5 • 00:1A:79:12:34:56" else connectionStatus,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        // Connection strength indicator
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Signal strength bars
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    listOf(0.3f, 0.6f, 0.9f, 1f).forEach { height ->
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height((12 * height).dp)
                                                .background(
                                                    color = Color(0xFF00E676),
                                                    shape = RoundedCornerShape(2.dp)
                                                )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Excellent",
                                    color = Color(0xFF00E676),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Protocol info
                            Text(
                                text = "SAE J1850 VPW • 500 Kbps",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    
                    // Vehicle Info Summary
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Vehicle Info
                        Column {
                            Text(
                                text = vehicleInfo?.let { "${it.modelYear} ${it.manufacturer} ${it.bodyStyle}" }
                                    ?: if (isConnected) "Vehicle Info Loading..." else "No Vehicle Connected",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = vehicleInfo?.vin?.let { "VIN: $it" } 
                                    ?: if (isConnected) "Reading VIN..." else "Connect to view VIN",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Quick action button
                        TextButton(
                            onClick = onNavigateToVehicleInfo,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "View Details",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ControlPanel(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "panel_scale"
    )
    
    Card(
        modifier = modifier
            .scale(scale)
            .clickable {
                isPressed = true
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1F2E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                color = Color(0xFF00D4FF),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
    
    // Reset pressed state
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

@Composable
fun StarField() {
    val stars = remember {
        List(50) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 2f + 1f,
                alpha = Random.nextFloat() * 0.6f + 0.2f,
                twinkleSpeed = Random.nextFloat() * 2f + 1f
            )
        }
    }
    
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        stars.forEach { star ->
            drawCircle(
                color = Color.White.copy(alpha = star.alpha),
                radius = star.size,
                center = Offset(
                    x = star.x * size.width,
                    y = star.y * size.height
                )
            )
        }
    }
}

data class Star(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float,
    val twinkleSpeed: Float
)

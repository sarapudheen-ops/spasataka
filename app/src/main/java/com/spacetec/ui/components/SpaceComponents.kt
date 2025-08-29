package com.spacetec.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.spacetec.ui.theme.SpaceColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Astronaut HUD Card with glowing borders and technical corners
 */
@Composable
fun SpaceHudCard(
    modifier: Modifier = Modifier,
    glowColor: Color = SpaceColors.ElectricBlue,
    isActive: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SpaceColors.CosmicDust.copy(alpha = 0.8f),
                        SpaceColors.SpaceVoid.copy(alpha = 0.9f)
                    )
                )
            )
    ) {
        // Glowing border effect
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isActive) {
                drawRoundRect(
                    color = glowColor.copy(alpha = glowAlpha * 0.6f),
                    style = Stroke(width = 4.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                )
                
                // Inner glow
                drawRoundRect(
                    color = glowColor.copy(alpha = glowAlpha * 0.3f),
                    style = Stroke(width = 8.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                )
            }
            
            // Corner technical details
            drawTechnicalCorners(glowColor, glowAlpha)
        }
        
        Box(
            modifier = Modifier.padding(16.dp)
        ) {
            content()
        }
    }
}

enum class SystemStatus {
    OPERATIONAL,
    WARNING,
    CRITICAL,
    OFFLINE
}

/**
 * Technical status indicator with pulse effect
 */
@Composable
fun TechnicalStatusIndicator(
    status: SystemStatus,
    modifier: Modifier = Modifier,
    label: String = ""
) {
    val statusColor = when (status) {
        SystemStatus.OPERATIONAL -> SpaceColors.SafeGreen
        SystemStatus.WARNING -> SpaceColors.Warning
        SystemStatus.CRITICAL -> SpaceColors.CriticalRed
        SystemStatus.OFFLINE -> SpaceColors.MetallicSilver
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "status")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    SystemStatus.CRITICAL -> 500  // Fast pulse for critical
                    SystemStatus.WARNING -> 1000  // Medium pulse for warning
                    else -> 2000                   // Slow pulse for normal
                }
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier.size(12.dp)
        ) {
            drawCircle(
                color = statusColor.copy(alpha = if (status == SystemStatus.OFFLINE) 0.3f else pulseAlpha),
                radius = size.minDimension / 2
            )
            
            // Outer ring for active status
            if (status != SystemStatus.OFFLINE) {
                drawCircle(
                    color = statusColor.copy(alpha = pulseAlpha * 0.3f),
                    radius = size.minDimension / 2 * 1.5f,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        
        if (label.isNotEmpty()) {
            androidx.compose.material3.Text(
                text = label,
                style = com.spacetec.ui.theme.SpaceTextStyles.StatusIndicator,
                color = statusColor
            )
        }
    }
}

/**
 * Circuit Pattern Background
 */
@Composable
fun CircuitPatternBackground(
    modifier: Modifier = Modifier,
    circuitColor: Color = SpaceColors.ElectricBlue.copy(alpha = 0.1f)
) {
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val circuitSpacing = 50.dp.toPx()
        val strokeWidth = 1.dp.toPx()
        
        // Draw circuit pattern
        for (x in 0..(size.width / circuitSpacing).toInt()) {
            for (y in 0..(size.height / circuitSpacing).toInt()) {
                val centerX = x * circuitSpacing
                val centerY = y * circuitSpacing
                
                // Draw circuit nodes
                drawCircle(
                    color = circuitColor,
                    radius = 2.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                
                // Draw connecting lines
                if (x < size.width / circuitSpacing) {
                    drawLine(
                        color = circuitColor.copy(alpha = 0.5f),
                        start = Offset(centerX, centerY),
                        end = Offset(centerX + circuitSpacing, centerY),
                        strokeWidth = strokeWidth
                    )
                }
                
                if (y < size.height / circuitSpacing) {
                    drawLine(
                        color = circuitColor.copy(alpha = 0.5f),
                        start = Offset(centerX, centerY),
                        end = Offset(centerX, centerY + circuitSpacing),
                        strokeWidth = strokeWidth
                    )
                }
            }
        }
    }
}

/**
 * Draw technical corner decorations
 */
private fun DrawScope.drawTechnicalCorners(
    color: Color,
    alpha: Float
) {
    val cornerSize = 20.dp.toPx()
    val strokeWidth = 2.dp.toPx()
    
    // Top-left corner
    drawLine(
        color = color.copy(alpha = alpha * 0.7f),
        start = Offset(0f, cornerSize),
        end = Offset(0f, 0f),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = color.copy(alpha = alpha * 0.7f),
        start = Offset(0f, 0f),
        end = Offset(cornerSize, 0f),
        strokeWidth = strokeWidth
    )
    
    // Top-right corner
    drawLine(
        color = color.copy(alpha = alpha * 0.7f),
        start = Offset(size.width - cornerSize, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = color.copy(alpha = alpha * 0.7f),
        start = Offset(size.width, 0f),
        end = Offset(size.width, cornerSize),
        strokeWidth = strokeWidth
    )
    
    // Bottom-left corner
    drawLine(
        color = color.copy(alpha = alpha * 0.7f),
        start = Offset(0f, size.height - cornerSize),
        end = Offset(0f, size.height),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = color.copy(alpha = alpha * 0.7f),
        start = Offset(0f, size.height),
        end = Offset(cornerSize, size.height),
        strokeWidth = strokeWidth
    )
    
    // Bottom-right corner
    drawLine(
        color = color.copy(alpha = alpha * 0.7f),
        start = Offset(size.width - cornerSize, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = color.copy(alpha = alpha * 0.7f),
        start = Offset(size.width, size.height),
        end = Offset(size.width, size.height - cornerSize),
        strokeWidth = strokeWidth
    )
}

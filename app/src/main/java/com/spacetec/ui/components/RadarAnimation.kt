package com.spacetec.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun RadarAnimation(
    isScanning: Boolean,
    modifier: Modifier = Modifier,
    size: Float = 300f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val radarColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.surface
    
    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val radius = this.size.width / 2 - 20.dp.toPx()
            
            // Draw radar background circles
            drawRadarBackground(center, radius, radarColor.copy(alpha = 0.3f))
            
            // Draw scanning sweep if active
            if (isScanning) {
                drawRadarSweep(center, radius, rotationAngle, radarColor, pulseAlpha)
            }
            
            // Draw center dot
            drawCircle(
                color = radarColor,
                radius = 8.dp.toPx(),
                center = center
            )
        }
        
        // Radar grid lines
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(if (isScanning) rotationAngle else 0f)
        ) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val radius = this.size.width / 2 - 20.dp.toPx()
            
            // Draw crosshairs
            drawLine(
                color = radarColor.copy(alpha = 0.5f),
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = radarColor.copy(alpha = 0.5f),
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

private fun DrawScope.drawRadarBackground(
    center: Offset,
    radius: Float,
    color: Color
) {
    // Draw concentric circles
    for (i in 1..4) {
        drawCircle(
            color = color,
            radius = radius * (i / 4f),
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

private fun DrawScope.drawRadarSweep(
    center: Offset,
    radius: Float,
    angle: Float,
    color: Color,
    alpha: Float
) {
    val sweepAngle = 45f
    val startAngle = angle - sweepAngle
    
    // Create gradient for sweep effect
    val gradient = Brush.sweepGradient(
        colors = listOf(
            Color.Transparent,
            color.copy(alpha = alpha * 0.3f),
            color.copy(alpha = alpha * 0.7f),
            color.copy(alpha = alpha),
            Color.Transparent
        ),
        center = center
    )
    
    // Draw the sweep arc
    drawArc(
        brush = gradient,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = true,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
    )
}

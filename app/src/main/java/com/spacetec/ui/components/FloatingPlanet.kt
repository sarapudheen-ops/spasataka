package com.spacetec.ui.components

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun FloatingPlanet(
    device: BluetoothDevice,
    position: Offset,
    onClick: (BluetoothDevice) -> Unit,
    modifier: Modifier = Modifier,
    planetType: PlanetType = PlanetType.random()
) {
    val infiniteTransition = rememberInfiniteTransition(label = "planet")
    
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000 + (planetType.ordinal * 300), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    val deviceName = try {
        device.name ?: "Unknown Satellite"
    } catch (e: SecurityException) {
        "Unknown Satellite"
    }
    
    Box(
        modifier = modifier
            .offset(
                x = position.x.dp,
                y = (position.y + floatOffset).dp
            )
            .size(60.dp)
            .clip(CircleShape)
            .clickable { onClick(device) },
        contentAlignment = Alignment.Center
    ) {
        // Planet with glow effect
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawPlanet(planetType, glowAlpha)
        }
        
        // Device name label
        Card(
            modifier = Modifier
                .offset(y = 40.dp)
                .widthIn(max = 120.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            Text(
                text = deviceName,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

private fun DrawScope.drawPlanet(planetType: PlanetType, glowAlpha: Float) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.width / 3
    
    // Draw glow effect
    drawCircle(
        color = planetType.glowColor.copy(alpha = glowAlpha * 0.3f),
        radius = radius * 1.5f,
        center = center
    )
    
    // Draw planet body
    drawCircle(
        color = planetType.primaryColor,
        radius = radius,
        center = center
    )
    
    // Draw planet features based on type
    when (planetType) {
        PlanetType.EARTH -> {
            // Draw continents
            drawCircle(
                color = planetType.secondaryColor,
                radius = radius * 0.6f,
                center = center.copy(x = center.x - radius * 0.2f)
            )
            drawCircle(
                color = planetType.secondaryColor,
                radius = radius * 0.4f,
                center = center.copy(x = center.x + radius * 0.3f, y = center.y + radius * 0.2f)
            )
        }
        PlanetType.MARS -> {
            // Draw craters
            drawCircle(
                color = planetType.secondaryColor,
                radius = radius * 0.2f,
                center = center.copy(x = center.x - radius * 0.3f, y = center.y - radius * 0.2f)
            )
            drawCircle(
                color = planetType.secondaryColor,
                radius = radius * 0.15f,
                center = center.copy(x = center.x + radius * 0.2f, y = center.y + radius * 0.3f)
            )
        }
        PlanetType.JUPITER -> {
            // Draw bands
            for (i in -2..2) {
                drawCircle(
                    color = planetType.secondaryColor.copy(alpha = 0.6f),
                    radius = radius * 0.8f,
                    center = center.copy(y = center.y + i * radius * 0.2f)
                )
            }
        }
        PlanetType.SATURN -> {
            // Draw rings
            drawCircle(
                color = planetType.secondaryColor.copy(alpha = 0.7f),
                radius = radius * 1.3f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = planetType.secondaryColor.copy(alpha = 0.5f),
                radius = radius * 1.1f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}

enum class PlanetType(
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color
) {
    EARTH(
        primaryColor = Color(0xFF4A90E2),
        secondaryColor = Color(0xFF1976D2),  // Changed from green to blue
        glowColor = Color(0xFF64B5F6)       // Changed from green to blue
    ),
    MARS(
        primaryColor = Color(0xFFD32F2F),
        secondaryColor = Color(0xFF8D1E1E),
        glowColor = Color(0xFFFF5722)
    ),
    JUPITER(
        primaryColor = Color(0xFFFF9800),
        secondaryColor = Color(0xFFE65100),
        glowColor = Color(0xFFFFB74D)
    ),
    SATURN(
        primaryColor = Color(0xFFFFC107),
        secondaryColor = Color(0xFFFF8F00),
        glowColor = Color(0xFFFFD54F)
    );
    
    companion object {
        fun random() = values().random()
    }
}

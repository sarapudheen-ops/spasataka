// Accessible Gauge Implementation
package com.spacetec.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacetec.vehicle.VehicleData

// ✅ Fixed Accessible Gauge (with proper semantics and warnings/critical thresholds)
@Composable
fun AccessibleGauge(
    label: String,
    value: Double,
    unit: String,
    minValue: Double = 0.0,
    maxValue: Double = 100.0,
    warningThreshold: Double? = null,
    criticalThreshold: Double? = null
) {
    val progress = ((value - minValue) / (maxValue - minValue)).coerceIn(0.0, 1.0).toFloat()
    val isCritical = criticalThreshold?.let { value >= it } ?: false
    val isWarning = !isCritical && (warningThreshold?.let { value >= it } ?: false)

    val statusDescription = when {
        isCritical -> "Critical level"
        isWarning -> "Warning level"
        else -> "Normal level"
    }

    val accessibilityDescription = buildString {
        append("$label gauge: ${value.toInt()} $unit")
        append(", $statusDescription")
        append(", ${(progress * 100).toInt()}% of maximum")
        warningThreshold?.let { append(", warning at ${it.toInt()} $unit") }
        criticalThreshold?.let { append(", critical at ${it.toInt()} $unit") }
    }

    Card(
        modifier = Modifier
            .semantics {
                contentDescription = accessibilityDescription
                role = Role.Image
            },
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
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${value.toInt()}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isCritical -> MaterialTheme.colorScheme.error
                    isWarning -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = unit,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SpaceGauge(
    title: String,
    value: Int,
    maxValue: Int,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.semantics {
            contentDescription = "$title: $value $unit"
            role = Role.Image
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircularGauge(
                    value = value.toFloat(),
                    maxValue = maxValue.toFloat(),
                    color = color,
                    size = size
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$value",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun EnhancedSpaceGauge(
    title: String,
    value: Int,
    maxValue: Int,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "enhancedGaugeAnim"
    )

    Card(
        modifier = modifier.semantics {
            contentDescription = "$title: $value $unit"
            role = Role.Image
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawEnhancedCircularGauge(
                    value = animatedValue,
                    maxValue = maxValue.toFloat(),
                    color = color,
                    size = size
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${animatedValue.toInt()}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = unit,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (animatedValue / maxValue).coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun AdvancedSpaceGauge(
    title: String,
    value: Int,
    maxValue: Int,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "advancedGaugeAnim"
    )

    Card(
        modifier = modifier.semantics {
            contentDescription = "$title: $value $unit"
            role = Role.Image
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawAdvancedCircularGauge(
                    value = animatedValue,
                    maxValue = maxValue.toFloat(),
                    color = color,
                    size = size
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${animatedValue.toInt()}",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = unit,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${((animatedValue / maxValue) * 100).toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun CompactMetricCard(
    title: String,
    value: String,
    isAlert: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.semantics {
            contentDescription = "$title: $value"
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isAlert) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAlert) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAlert) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun AlertCard(vehicleData: VehicleData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Critical alert: ${vehicleData.getStatusMessage()}"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠️",
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column {
                Text(
                    text = "CRITICAL ALERT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                val alerts = mutableListOf<String>()
                if (vehicleData.engineCoreTemp > 110) alerts.add("High engine temperature")
                if (vehicleData.oxygenLevels < 10) alerts.add("Low fuel level")
                if (vehicleData.thrusterPower > 7000) alerts.add("High RPM")

                Text(
                    text = if (alerts.isEmpty()) "No active alerts" else alerts.joinToString(", "),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// === Drawing helpers ===
private fun DrawScope.drawCircularGauge(value: Float, maxValue: Float, color: Color, size: Size) {
    val strokeWidth = 8.dp.toPx()
    val radius = (size.minDimension - strokeWidth) / 2
    val center = size.center

    drawArc(
        color = Color.Gray.copy(alpha = 0.3f),
        startAngle = 135f,
        sweepAngle = 270f,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2)
    )

    val sweepAngle = (value / maxValue * 270f).coerceIn(0f, 270f)
    drawArc(
        color = color,
        startAngle = 135f,
        sweepAngle = sweepAngle,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2)
    )
}

private fun DrawScope.drawEnhancedCircularGauge(value: Float, maxValue: Float, color: Color, size: Size) {
    val strokeWidth = 12.dp.toPx()
    val radius = (size.minDimension - strokeWidth) / 2
    val center = size.center

    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(
                Color.Gray.copy(alpha = 0.2f),
                Color.Gray.copy(alpha = 0.4f),
                Color.Gray.copy(alpha = 0.2f)
            )
        ),
        startAngle = 135f,
        sweepAngle = 270f,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2)
    )

    val sweepAngle = (value / maxValue * 270f).coerceIn(0f, 270f)
    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(
                color.copy(alpha = 0.6f),
                color,
                color.copy(alpha = 0.8f)
            )
        ),
        startAngle = 135f,
        sweepAngle = sweepAngle,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2)
    )
}

private fun DrawScope.drawAdvancedCircularGauge(value: Float, maxValue: Float, color: Color, size: Size) {
    val strokeWidth = 16.dp.toPx()
    val radius = (size.minDimension - strokeWidth) / 2
    val center = size.center

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.1f), Color.Transparent),
            radius = radius + 20.dp.toPx()
        ),
        radius = radius + 20.dp.toPx(),
        center = center
    )

    drawArc(
        color = Color.Gray.copy(alpha = 0.2f),
        startAngle = 135f,
        sweepAngle = 270f,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2)
    )

    val sweepAngle = (value / maxValue * 270f).coerceIn(0f, 270f)
    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(
                color.copy(alpha = 0.4f),
                color,
                color.copy(alpha = 0.9f),
                color
            )
        ),
        startAngle = 135f,
        sweepAngle = sweepAngle,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2)
    )
}

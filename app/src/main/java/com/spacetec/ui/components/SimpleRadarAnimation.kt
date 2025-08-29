package com.spacetec.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun RadarAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ), label = ""
    )

    Canvas(modifier = Modifier.size(200.dp)) {
        drawCircle(Color(0xFF2196F3).copy(alpha = 0.2f), style = Stroke(3f))
        drawArc(
            color = Color(0xFF2196F3),
            startAngle = angle,
            sweepAngle = 60f,
            useCenter = true,
            size = Size(size.width, size.height),
            topLeft = Offset.Zero
        )
    }
}

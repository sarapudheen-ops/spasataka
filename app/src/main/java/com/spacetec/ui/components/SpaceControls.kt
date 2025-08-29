package com.spacetec.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacetec.ui.theme.SpaceColors
import com.spacetec.ui.theme.SpaceTextStyles

/**
 * Astronaut Control Panel Button
 */
@Composable
fun AstronautControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector? = null,
    isActive: Boolean = false,
    buttonType: ControlButtonType = ControlButtonType.PRIMARY
) {
    val buttonColor = when (buttonType) {
        ControlButtonType.PRIMARY -> SpaceColors.ElectricBlue
        ControlButtonType.WARNING -> SpaceColors.Warning
        ControlButtonType.CRITICAL -> SpaceColors.CriticalRed
        ControlButtonType.SUCCESS -> SpaceColors.SafeGreen
    }
    
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) buttonColor else buttonColor.copy(alpha = 0.3f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = text,
                style = SpaceTextStyles.StatusIndicator.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

enum class ControlButtonType {
    PRIMARY, WARNING, CRITICAL, SUCCESS
}

/**
 * HUD Data Display Panel
 */
@Composable
fun HudDataPanel(
    title: String,
    value: String,
    unit: String = "",
    status: SystemStatus = SystemStatus.OPERATIONAL,
    modifier: Modifier = Modifier,
    icon: String = "⚙️"
) {
    val statusColor = when (status) {
        SystemStatus.OPERATIONAL -> SpaceColors.SafeGreen
        SystemStatus.WARNING -> SpaceColors.Warning
        SystemStatus.CRITICAL -> SpaceColors.CriticalRed
        SystemStatus.OFFLINE -> SpaceColors.MetallicSilver
    }
    
    SpaceHudCard(
        modifier = modifier,
        glowColor = statusColor,
        isActive = status != SystemStatus.OFFLINE
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = icon,
                        style = SpaceTextStyles.StatusIndicator.copy(fontSize = 16.sp)
                    )
                    Text(
                        text = title,
                        style = SpaceTextStyles.TechnicalData,
                        color = SpaceColors.MetallicSilver
                    )
                }
                TechnicalStatusIndicator(status = SystemStatus.OPERATIONAL)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    style = SpaceTextStyles.HudDisplay.copy(
                        color = statusColor
                    )
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        style = SpaceTextStyles.TechnicalData.copy(
                            color = SpaceColors.MetallicSilver.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }
    }
}


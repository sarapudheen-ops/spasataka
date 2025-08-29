package com.spacetec.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onNavigateBack: () -> Unit,
    onSystemSelected: (String) -> Unit = {}
) {
    val systems = listOf(
        SystemStatus("Engine", Icons.Default.Build, 98, Status.GOOD),
        SystemStatus("Transmission", Icons.Default.DriveEta, 95, Status.GOOD),
        SystemStatus("ABS", Icons.Default.Warning, 87, Status.WARNING),
        SystemStatus("Airbag", Icons.Default.Security, 100, Status.GOOD),
        SystemStatus("Climate", Icons.Default.Thermostat, 92, Status.GOOD),
        SystemStatus("Infotainment", Icons.Default.Audiotrack, 76, Status.WARNING),
        SystemStatus("Lighting", Icons.Default.Lightbulb, 100, Status.GOOD),
        SystemStatus("Suspension", Icons.Default.DriveEta, 88, Status.GOOD),
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text("Vehicle Systems") 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(systems) { system ->
                SystemCard(
                    system = system,
                    onClick = { onSystemSelected(system.name) }
                )
            }
        }
    }
}

@Composable
private fun SystemCard(
    system: SystemStatus,
    onClick: () -> Unit
) {
    val statusColor = when (system.status) {
        Status.GOOD -> Color(0xFF4CAF50) // Green
        Status.WARNING -> Color(0xFFFFC107) // Amber
        Status.ERROR -> Color(0xFFF44336) // Red
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // System Icon and Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = system.icon,
                    contentDescription = system.name,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = system.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Status indicator with text
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(statusColor, shape = androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = system.status.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Health percentage
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${system.health}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                
                // Health bar
                LinearProgressIndicator(
                    progress = system.health / 100f,
                    modifier = Modifier
                        .width(80.dp)
                        .height(4.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape),
                    color = statusColor,
                    trackColor = statusColor.copy(alpha = 0.24f)
                )
            }
        }
    }
}

data class SystemStatus(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val health: Int,
    val status: Status
)

enum class Status(val displayName: String) {
    GOOD("Optimal"),
    WARNING("Warning"),
    ERROR("Error")
}

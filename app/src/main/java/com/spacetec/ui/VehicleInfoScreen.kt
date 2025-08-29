package com.spacetec.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacetec.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleInfoScreen(
    onNavigateBack: () -> Unit,
    onEditVin: () -> Unit = {},
    onViewServiceHistory: () -> Unit = {}
) {
    // Sample vehicle data - in a real app, this would come from a ViewModel
    val vehicleInfo = remember {
        mapOf(
            "VIN" to "1HGCM82633A123456",
            "Make" to "Honda",
            "Model" to "Accord",
            "Year" to "2023",
            "Trim" to "Touring 2.0T",
            "Engine" to "2.0L Turbo I4",
            "Transmission" to "10-Speed Automatic",
            "Mileage" to "12,345 mi",
            "Last Service" to "5,000 mi / 05/15/2023",
            "Next Service" to "15,000 mi / 11/15/2023",
            "Warranty" to "Basic: 36 mo/36,000 mi\nPowertrain: 60 mo/60,000 mi",
            "Tire Pressure" to "Front: 35 PSI, Rear: 33 PSI"
        )
    }

    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Vehicle Information") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEditVin) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit VIN"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onViewServiceHistory,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Service History")
                    }
                    
                    Button(
                        onClick = { /* View maintenance schedule */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Maintenance")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // Vehicle Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            ) {
                Image(
                    painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                    contentDescription = "Vehicle Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Vehicle make and model overlay
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 0.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${vehicleInfo["Year"]} ${vehicleInfo["Make"]} ${vehicleInfo["Model"]}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = vehicleInfo["Trim"] ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Vehicle Details
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // VIN Section
                InfoCard(
                    title = "VEHICLE IDENTIFICATION NUMBER (VIN)",
                    value = vehicleInfo["VIN"] ?: "",
                    icon = Icons.Default.Fingerprint,
                    iconTint = MaterialTheme.colorScheme.primary
                )
                
                // Specifications Section
                InfoSection(
                    title = "SPECIFICATIONS",
                    items = listOf(
                        InfoItem("Engine", vehicleInfo["Engine"] ?: "", Icons.Default.Build),
                        InfoItem("Transmission", vehicleInfo["Transmission"] ?: "", Icons.Default.Settings),
                        InfoItem("Tire Pressure", vehicleInfo["Tire Pressure"] ?: "", Icons.Default.TireRepair)
                    )
                )
                
                // Service Section
                InfoSection(
                    title = "SERVICE",
                    items = listOf(
                        InfoItem("Mileage", vehicleInfo["Mileage"] ?: "", Icons.Default.Speed),
                        InfoItem("Last Service", vehicleInfo["Last Service"] ?: "", Icons.Default.History),
                        InfoItem("Next Service", vehicleInfo["Next Service"] ?: "", Icons.Default.Event)
                    )
                )
                
                // Warranty Section
                InfoCard(
                    title = "WARRANTY COVERAGE",
                    value = vehicleInfo["Warranty"] ?: "",
                    icon = Icons.Default.Verified,
                    iconTint = Color(0xFF4CAF50)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    items: List<InfoItem>
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column {
            // Section header
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            // Section items
            Column(
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items.forEachIndexed { index, item ->
                    InfoRow(
                        icon = item.icon,
                        label = item.label,
                        value = item.value,
                        showDivider = index < items.size - 1
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, start = 28.dp),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        if (showDivider) {
            Divider(
                modifier = Modifier.padding(start = 52.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        }
    }
}

data class InfoItem(
    val label: String,
    val value: String,
    val icon: ImageVector
)

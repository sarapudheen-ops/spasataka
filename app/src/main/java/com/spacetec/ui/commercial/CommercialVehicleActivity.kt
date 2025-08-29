package com.spacetec.ui.commercial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spacetec.diagnostic.commercial.CommercialVehicleSupport
import com.spacetec.ui.theme.SpaceTecTheme

class CommercialVehicleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpaceTecTheme {
                CommercialVehicleScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommercialVehicleScreen(
    viewModel: CommercialVehicleViewModel = viewModel()
) {
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val vehiclesByType by viewModel.vehiclesByType.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1B2A),
                        Color(0xFF1B263B),
                        Color(0xFF415A77)
                    )
                )
            )
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1B263B).copy(alpha = 0.8f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = "Commercial Vehicles",
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Commercial Vehicles",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Heavy Duty, Medium Duty, Construction & Agricultural",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF90CAF9)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Vehicle Type Selection
        VehicleTypeSelector(
            selectedType = selectedType,
            onTypeSelected = { viewModel.selectVehicleType(it) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Selected Vehicle Info
        selectedVehicle?.let { vehicle ->
            SelectedVehicleCard(
                vehicle = vehicle,
                onClearSelection = { viewModel.clearSelection() }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Vehicle List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(vehiclesByType) { vehicle ->
                CommercialVehicleCard(
                    vehicle = vehicle,
                    isSelected = selectedVehicle == vehicle,
                    onSelect = { viewModel.selectVehicle(vehicle) }
                )
            }
        }
    }
}

@Composable
fun VehicleTypeSelector(
    selectedType: CommercialVehicleSupport.VehicleType?,
    onTypeSelected: (CommercialVehicleSupport.VehicleType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2E3B4E).copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Vehicle Type",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(CommercialVehicleSupport.VehicleType.values()) { type ->
                    FilterChip(
                        onClick = { onTypeSelected(type) },
                        label = {
                            Text(
                                text = type.name.replace("_", " "),
                                color = if (selectedType == type) Color.White else Color(0xFF90CAF9)
                            )
                        },
                        selected = selectedType == type,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF64B5F6),
                            containerColor = Color(0xFF415A77).copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun SelectedVehicleCard(
    vehicle: CommercialVehicleSupport.CommercialVehicle,
    onClearSelection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getVehicleTypeIcon(vehicle.type),
                    contentDescription = vehicle.type.name,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${vehicle.make} ${vehicle.model}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${vehicle.type.name.replace("_", " ")} • ${vehicle.engineType.name}",
                        color = Color(0xFF90CAF9),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(onClick = onClearSelection) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear Selection",
                        tint = Color(0xFF90CAF9)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // ECU Count
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "ECUs",
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${vehicle.ecuList.size} ECUs",
                    color = Color(0xFF90CAF9),
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Features",
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${vehicle.specialFeatures.size} Features",
                    color = Color(0xFF90CAF9),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun CommercialVehicleCard(
    vehicle: CommercialVehicleSupport.CommercialVehicle,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0xFF4CAF50).copy(alpha = 0.2f)
            } else {
                Color(0xFF2E3B4E).copy(alpha = 0.9f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getVehicleTypeIcon(vehicle.type),
                contentDescription = vehicle.type.name,
                tint = if (isSelected) Color(0xFF4CAF50) else Color(0xFF64B5F6),
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${vehicle.make} ${vehicle.model}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = vehicle.type.name.replace("_", " "),
                    color = Color(0xFF90CAF9),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${vehicle.yearRange.first}-${vehicle.yearRange.last} • ${vehicle.engineType.name}",
                    color = Color(0xFF90CAF9),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Badge(
                    containerColor = Color(0xFF64B5F6)
                ) {
                    Text(
                        text = "${vehicle.ecuList.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ECUs",
                    color = Color(0xFF90CAF9),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

fun getVehicleTypeIcon(type: CommercialVehicleSupport.VehicleType): ImageVector {
    return when (type) {
        CommercialVehicleSupport.VehicleType.HEAVY_DUTY_TRUCK -> Icons.Default.LocalShipping
        CommercialVehicleSupport.VehicleType.MEDIUM_DUTY_TRUCK -> Icons.Default.LocalShipping
        CommercialVehicleSupport.VehicleType.LIGHT_DUTY_TRUCK -> Icons.Default.LocalShipping
        CommercialVehicleSupport.VehicleType.BUS -> Icons.Default.DirectionsBus
        CommercialVehicleSupport.VehicleType.COACH -> Icons.Default.DirectionsBus
        CommercialVehicleSupport.VehicleType.DELIVERY_VAN -> Icons.Default.LocalShipping
        CommercialVehicleSupport.VehicleType.CONSTRUCTION_EQUIPMENT -> Icons.Default.Construction
        CommercialVehicleSupport.VehicleType.AGRICULTURAL_EQUIPMENT -> Icons.Default.Agriculture
        CommercialVehicleSupport.VehicleType.MARINE_ENGINE -> Icons.Default.DirectionsBoat
        CommercialVehicleSupport.VehicleType.GENERATOR_SET -> Icons.Default.Power
        CommercialVehicleSupport.VehicleType.STATIONARY_ENGINE -> Icons.Default.Settings
    }
}

package com.spacetec.vehicle.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.unit.dp
import com.spacetec.vehicle.VehicleTypeUtils
import com.spacetec.vehicle.model.VehicleType

@Composable
fun VehicleTypeFilter(
    selectedTypes: Set<VehicleType>,
    onTypeSelected: (VehicleType, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = remember { VehicleTypeUtils.getVehicleCategories() }
    var expandedCategory by remember { mutableStateOf<String?>(null) }
    
    Column(modifier = modifier) {
        categories.forEach { (category, types) ->
            ExpandableFilterCategory(
                title = category,
                isExpanded = expandedCategory == category,
                onExpandedChange = { expandedCategory = if (it) category else null },
                selectedTypes = selectedTypes,
                types = types,
                onTypeSelected = onTypeSelected
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpandableFilterCategory(
    title: String,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedTypes: Set<VehicleType>,
    types: List<VehicleType>,
    onTypeSelected: (VehicleType, Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        onClick = { onExpandedChange(!isExpanded) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) 
                        Icons.Filled.ExpandLess 
                    else 
                        Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }
            
            if (isExpanded) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp, start = 8.dp, end = 8.dp)
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(types) { type ->
                        val isSelected = selectedTypes.contains(type)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTypeSelected(type, !isSelected) },
                            label = { Text(type.displayName) },
                            leadingIcon = {
                                Icon(
                                    imageVector = type.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}

private val VehicleType.displayName: String
    get() = when (this) {
        VehicleType.PASSENGER -> "Passenger"
        VehicleType.COMMERCIAL -> "Commercial"
        VehicleType.MOTORCYCLE -> "Motorcycle"
        VehicleType.ELECTRIC -> "Electric"
        VehicleType.HYBRID -> "Hybrid"
        VehicleType.PLUGIN_HYBRID -> "Plug-in Hybrid"
        VehicleType.LUXURY -> "Luxury"
        VehicleType.PERFORMANCE -> "Performance"
        VehicleType.OFFROAD -> "Off-road"
        VehicleType.SPORTS -> "Sports"
        VehicleType.CLASSIC -> "Classic"
        VehicleType.KART -> "Kart"
        VehicleType.ATV -> "ATV"
        VehicleType.UTV -> "UTV"
        VehicleType.SNOWMOBILE -> "Snowmobile"
        VehicleType.WATER -> "Marine"
        VehicleType.AGRICULTURAL -> "Agricultural"
        VehicleType.CONSTRUCTION -> "Construction"
        VehicleType.MILITARY -> "Military"
        VehicleType.EMERGENCY -> "Emergency"
        VehicleType.AUTONOMOUS -> "Autonomous"
        VehicleType.HYDROGEN -> "Hydrogen"
        VehicleType.FLYING -> "Flying"
        VehicleType.KIT_CAR -> "Kit Car"
        VehicleType.PROTOTYPE -> "Prototype"
        VehicleType.OTHER -> "Other"
    }

private val VehicleType.icon: ImageVector
    get() = when (this) {
        VehicleType.ELECTRIC, 
        VehicleType.HYDROGEN -> Icons.Default.ElectricCar
        VehicleType.MOTORCYCLE -> Icons.Default.TwoWheeler
        VehicleType.COMMERCIAL -> Icons.Outlined.LocalShipping
        VehicleType.ATV, 
        VehicleType.UTV -> Icons.Default.DirectionsCar
        VehicleType.SNOWMOBILE -> Icons.Default.AcUnit
        VehicleType.WATER -> Icons.Default.DirectionsCar
        VehicleType.AGRICULTURAL -> Icons.Default.DirectionsCar
        VehicleType.CONSTRUCTION -> Icons.Default.Build
        VehicleType.MILITARY -> Icons.Default.Security
        VehicleType.EMERGENCY -> Icons.Default.LocalHospital
        VehicleType.SPORTS, 
        VehicleType.PERFORMANCE -> Icons.Default.DirectionsCar
        VehicleType.OFFROAD -> Icons.Default.DirectionsCar
        VehicleType.KART -> Icons.Default.DirectionsCar
        VehicleType.CLASSIC -> Icons.Default.Star
        VehicleType.AUTONOMOUS -> Icons.Default.SmartToy
        VehicleType.FLYING -> Icons.Default.Flight
        VehicleType.KIT_CAR, 
        VehicleType.PROTOTYPE -> Icons.Default.Build
        else -> Icons.Default.DirectionsCar
    }

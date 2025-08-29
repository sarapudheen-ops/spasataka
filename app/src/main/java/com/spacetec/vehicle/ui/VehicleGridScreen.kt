package com.spacetec.vehicle.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spacetec.vehicle.VehicleTypeUtils
import com.spacetec.vehicle.model.VehicleBrand
import com.spacetec.vehicle.model.VehicleType

fun getVehicleTypeDisplayName(type: VehicleType): String = when (type) {
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VehicleGridScreen(
    brands: List<VehicleBrand> = VehicleTypeUtils.createSampleBrands(),
    onBrandSelected: (VehicleBrand) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTypes by remember { mutableStateOf<Set<VehicleType>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredBrands = remember(brands, selectedTypes, searchQuery) {
        brands.filter { brand ->
            val matchesSearch = searchQuery.isEmpty() || 
                brand.name.contains(searchQuery, ignoreCase = true) ||
                brand.country.contains(searchQuery, ignoreCase = true)
            
            val matchesTypes = selectedTypes.isEmpty() || 
                selectedTypes.any { it in brand.vehicleTypes }
            
            matchesSearch && matchesTypes
        }.sortedBy { it.name }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search Bar
        VehicleSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { /* Handle search */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        // Filter Chips
        VehicleTypeFilter(
            selectedTypes = selectedTypes,
            onTypeSelected = { type, selected ->
                selectedTypes = if (selected) {
                    selectedTypes + type
                } else {
                    selectedTypes - type
                }
            },
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Results count
        Text(
            text = "${filteredBrands.size} ${if (filteredBrands.size == 1) "vehicle" else "vehicles"} found",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Vehicle Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredBrands) { brand ->
                VehicleBrandCard(
                    brand = brand,
                    onClick = { onBrandSelected(brand) },
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleBrandCard(
    brand: VehicleBrand,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Logo/Icon
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = brand.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Brand Name and Info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = brand.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = brand.country,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (brand.isPremium) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Premium",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                
                // Vehicle Type Chips
                if (brand.vehicleTypes.isNotEmpty()) {
                    val mainType = brand.vehicleTypes.first()
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = getVehicleTypeDisplayName(mainType),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            placeholder = { 
                Text(
                    "Search vehicles...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


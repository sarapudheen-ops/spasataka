package com.spacetec.vehicle.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spacetec.vehicle.WorkingVehicleLibrary
import com.spacetec.vehicle.model.VehicleBrand
import com.spacetec.vehicle.model.VehicleType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingVehicleScreen() {
    val vehicleLibrary = remember { WorkingVehicleLibrary() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<VehicleType?>(null) }
    
    val filteredBrands = remember(searchQuery, selectedType) {
        when {
            selectedType != null -> vehicleLibrary.getBrandsByType(selectedType!!)
            searchQuery.isNotBlank() -> vehicleLibrary.searchBrands(searchQuery)
            else -> vehicleLibrary.getAllBrands()
        }
    }
    
    val stats = remember { vehicleLibrary.getLibraryStats() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Vehicle Library",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Stats Card
        StatsCard(stats = stats)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Brands") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Vehicle Type Filter
        VehicleTypeFilterRow(
            selectedType = selectedType,
            onTypeSelected = { selectedType = if (selectedType == it) null else it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Results
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredBrands) { brand ->
                VehicleBrandItem(brand = brand)
            }
        }
    }
}

@Composable
fun StatsCard(stats: com.spacetec.vehicle.VehicleLibraryStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "Brands", value = stats.totalBrands.toString())
            StatItem(label = "Countries", value = stats.totalCountries.toString())
            StatItem(label = "Premium", value = stats.premiumBrands.toString())
            StatItem(label = "Protocols", value = stats.supportedProtocols.toString())
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun VehicleTypeFilterRow(
    selectedType: VehicleType?,
    onTypeSelected: (VehicleType) -> Unit
) {
    val commonTypes = listOf(
        VehicleType.PASSENGER,
        VehicleType.LUXURY,
        VehicleType.ELECTRIC,
        VehicleType.PERFORMANCE,
        VehicleType.COMMERCIAL
    )
    
    LazyColumn {
        item {
            Text(
                text = "Filter by Type:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                commonTypes.forEach { type ->
                    FilterChip(
                        onClick = { onTypeSelected(type) },
                        label = { Text(type.name) },
                        selected = selectedType == type
                    )
                }
            }
        }
    }
}

@Composable
fun VehicleBrandItem(brand: VehicleBrand) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Icon
            Icon(
                imageVector = brand.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Brand Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = brand.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${brand.country} • Founded ${brand.yearFounded}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Types: ${brand.vehicleTypes.joinToString(", ") { it.name }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (brand.isPremium) {
                    Text(
                        text = "Premium Brand",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

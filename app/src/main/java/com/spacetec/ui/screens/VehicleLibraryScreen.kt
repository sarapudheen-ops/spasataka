package com.spacetec.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.spacetec.vehicle.WorkingVehicleLibrary
import com.spacetec.vehicle.model.VehicleBrand
import com.spacetec.vehicle.model.VehicleType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleLibraryScreen(
    navController: NavController = rememberNavController()
) {
    val scope = rememberCoroutineScope()
    val vehicleLibrary = remember { WorkingVehicleLibrary() }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedVehicleType by remember { mutableStateOf<VehicleType?>(null) }
    var selectedCountry by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showVinSearch by remember { mutableStateOf(false) }
    var vinInput by remember { mutableStateOf("") }
    
    val allBrands = vehicleLibrary.getAllBrands()
    val filteredBrands = remember(searchQuery, selectedVehicleType, selectedCountry) {
        var brands = vehicleLibrary.searchBrands(searchQuery)
        
        selectedVehicleType?.let { type ->
            brands = brands.filter { it.vehicleTypes.contains(type) }
        }
        
        if (selectedCountry.isNotEmpty()) {
            brands = brands.filter { it.country.equals(selectedCountry, ignoreCase = true) }
        }
        
        brands
    }
    val libraryStats = vehicleLibrary.getLibraryStats()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Vehicle Library",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Library Statistics
        item {
            LibraryStatsCard(libraryStats)
        }
        
        // Search and Filter
        item {
            VehicleSearchCard(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it }
            )
        }
        
        item {
            VehicleTypeFilterCard(
                selectedType = selectedVehicleType,
                onTypeSelected = { selectedVehicleType = it }
            )
        }
        
        // Vehicle Brands
        item {
            Text(
                "Vehicle Brands (${filteredBrands.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(filteredBrands) { brand ->
            VehicleBrandCard(
                brand = brand,
                onClick = {
                    // Navigate to brand details or vehicle selection
                }
            )
        }
    }
}

@Composable
fun LibraryStatsCard(stats: com.spacetec.vehicle.VehicleLibraryStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LibraryBooks,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Library Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Brands", stats.totalBrands.toString())
                StatItem("Countries", stats.totalCountries.toString())
                StatItem("Protocols", stats.supportedProtocols.toString())
            }
        }
    }
}

@Composable
fun VehicleSearchCard(
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Search Vehicles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = { Text("Search by brand name") },
                placeholder = { Text("Toyota, BMW, Ford...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleTypeFilterCard(
    selectedType: VehicleType?,
    onTypeSelected: (VehicleType?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Filter by Vehicle Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        onClick = { onTypeSelected(null) },
                        label = { Text("All") },
                        selected = selectedType == null
                    )
                }
                
                items(VehicleType.values().toList()) { type ->
                    FilterChip(
                        onClick = { onTypeSelected(type) },
                        label = { Text(type.name.replace("_", " ")) },
                        selected = selectedType == type
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleBrandCard(
    brand: VehicleBrand,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                brand.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    brand.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${brand.country} • Founded ${brand.yearFounded}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Types: ${brand.vehicleTypes.joinToString(", ") { it.name.replace("_", " ") }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

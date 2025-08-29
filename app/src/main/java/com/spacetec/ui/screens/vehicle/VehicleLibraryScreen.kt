// Temporarily disabled due to compilation errors
/*
package com.spacetec.ui.screens.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spacetec.vehicle.library.models.VehicleSpec
import com.spacetec.vehicle.library.viewmodel.VehicleLibraryUiState
import com.spacetec.vehicle.library.viewmodel.VehicleLibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleLibraryScreen(
    viewModel: VehicleLibraryViewModel = hiltViewModel(),
    onVehicleSelected: (VehicleSpec) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    
    // Filter states
    val selectedMake by viewModel.filters.collectAsState()
    val availableMakes by viewModel.availableMakes.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val availableYears by viewModel.availableYears.collectAsState()
    
    // Track if filters are expanded
    var filtersExpanded by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vehicle Library") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { filtersExpanded = !filtersExpanded }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filters",
                            tint = if (filtersExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by make, model, or VIN") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )
            
            // Filters section
            if (filtersExpanded) {
                VehicleFiltersSection(
                    selectedMake = selectedMake.brand,
                    selectedModel = selectedMake.model,
                    selectedYear = selectedMake.year,
                    availableMakes = availableMakes,
                    availableModels = availableModels,
                    availableYears = availableYears,
                    onMakeSelected = viewModel::onMakeSelected,
                    onModelSelected = viewModel::onModelSelected,
                    onYearSelected = viewModel::onYearSelected,
                    onClearFilters = {
                        viewModel.onMakeSelected(null)
                        viewModel.onModelSelected(null)
                        viewModel.onYearSelected(null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Content based on state
            when (val state = uiState) {
                is VehicleLibraryUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is VehicleLibraryUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.initializeLibrary() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is VehicleLibraryUiState.Success -> {
                    if (state.vehicles.isEmpty()) {
                        EmptyState(
                            message = "No vehicles found. Try adjusting your search or filters.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        VehicleList(
                            vehicles = state.vehicles,
                            onVehicleClick = {
                                viewModel.selectVehicle(it)
                                onVehicleSelected(it)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        
        // Show vehicle details in a bottom sheet if a vehicle is selected
        selectedVehicle?.let { vehicle ->
            VehicleDetailsBottomSheet(
                vehicle = vehicle,
                onDismiss = { viewModel.clearSelectedVehicle() },
                onSelect = {
                    onVehicleSelected(vehicle)
                    viewModel.clearSelectedVehicle()
                }
            )
        }
    }
}

@Composable
private fun VehicleFiltersSection(
    selectedMake: String?,
    selectedModel: String?,
    selectedYear: Int?,
    availableMakes: List<String>,
    availableModels: List<String>,
    availableYears: List<Int>,
    onMakeSelected: (String?) -> Unit,
    onModelSelected: (String?) -> Unit,
    onYearSelected: (Int?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var makeExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Make filter
        FilterChipGroup(
            label = "Make",
            selectedItem = selectedMake,
            items = availableMakes,
            onItemSelected = onMakeSelected,
            expanded = makeExpanded,
            onExpandedChange = { makeExpanded = it },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Model filter (only enabled if make is selected)
        FilterChipGroup(
            label = "Model",
            selectedItem = selectedModel,
            items = availableModels,
            onItemSelected = onModelSelected,
            enabled = selectedMake != null,
            expanded = modelExpanded,
            onExpandedChange = { modelExpanded = it },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Year filter (only enabled if model is selected)
        FilterChipGroup(
            label = "Year",
            selectedItem = selectedYear?.toString(),
            items = availableYears.map { it.toString() },
            onItemSelected = { onYearSelected(it?.toIntOrNull()) },
            enabled = selectedModel != null,
            expanded = yearExpanded,
            onExpandedChange = { yearExpanded = it },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Clear filters button
        if (selectedMake != null || selectedModel != null || selectedYear != null) {
            TextButton(
                onClick = onClearFilters,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Clear Filters")
            }
        }
    }
}

@Composable
private fun FilterChipGroup(
    label: String,
    selectedItem: String?,
    items: List<String>,
    onItemSelected: (String?) -> Unit,
    enabled: Boolean = true,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded && enabled,
            onExpandedChange = { onExpandedChange(!expanded) },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedItem ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                placeholder = { Text("Select $label") },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            
            val filteredItems = if (selectedItem != null && selectedItem !in items) {
                listOf(selectedItem) + items
            } else {
                items
            }
            
            ExposedDropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                // Add a clear option
                DropdownMenuItem(
                    text = { Text("All") },
                    onClick = {
                        onItemSelected(null)
                        onExpandedChange(false)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
                
                // Add all items
                filteredItems.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            onItemSelected(item)
                            onExpandedChange(false)
                        },
                        leadingIcon = {
                            if (item == selectedItem) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                // Empty icon for alignment
                                Spacer(modifier = Modifier.size(24.dp))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VehicleList(
    vehicles: List<VehicleSpec>,
    onVehicleClick: (VehicleSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(vehicles, key = { it.id }) { vehicle ->
            VehicleListItem(
                vehicle = vehicle,
                onClick = { onVehicleClick(vehicle) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleListItem(
    vehicle: VehicleSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vehicle icon or image placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Vehicle details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "${vehicle.year} ${vehicle.make} ${vehicle.model}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (vehicle.trim.isNotBlank()) {
                    Text(
                        text = vehicle.trim,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "${vehicle.engineType} • ${vehicle.fuelType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Navigation icon
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleDetailsBottomSheet(
    vehicle: VehicleSpec,
    onDismiss: () -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Vehicle icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                // Title
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "${vehicle.year} ${vehicle.make} ${vehicle.model}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (vehicle.trim.isNotBlank()) {
                        Text(
                            text = vehicle.trim,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Details
            VehicleDetailsSection(vehicle)
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        onSelect()
                        onDismiss()
                    }
                ) {
                    Text("Select Vehicle")
                }
            }
        }
    }
}

@Composable
private fun VehicleDetailsSection(vehicle: VehicleSpec) {
    val details = listOfNotNull(
        "Engine" to vehicle.engineType.takeIf { it.isNotBlank() } ?: "Not specified",
        "Fuel Type" to vehicle.fuelType.takeIf { it.isNotBlank() } ?: "Not specified",
        "Transmission" to vehicle.transmission.takeIf { it.isNotBlank() } ?: "Not specified",
        "Drive Type" to vehicle.driveType.takeIf { it.isNotBlank() } ?: "Not specified",
        "Body Type" to vehicle.bodyType.takeIf { it.isNotBlank() } ?: "Not specified",
        "OBD Protocol" to vehicle.obdProtocol.takeIf { it.isNotBlank() } ?: "OBD2"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp)
    ) {
        Text(
            text = "Vehicle Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        details.forEach { (label, value) ->
            DetailRow(label = label, value = value)
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(32.dp)
    ) {
        Icon(
            Icons.Default.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(32.dp)
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Error",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Retry")
        }
    }
}
*/

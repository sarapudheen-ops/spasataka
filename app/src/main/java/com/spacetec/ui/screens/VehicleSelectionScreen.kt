// Temporarily disabled due to compilation errors
/*
package com.spacetec.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacetec.vehicle.library.VehicleSpec
import com.spacetec.vehicle.library.VehicleLookupService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleSelectionScreen(
    vehicleLookupService: VehicleLookupService,
    onVehicleSelected: (VehicleSpec) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isVinMode by remember { mutableStateOf(false) }
    val searchResults by vehicleLookupService.searchResults.collectAsState()
    val suggestions by vehicleLookupService.suggestions.collectAsState()
    val selectedVehicle by vehicleLookupService.selectedVehicle.collectAsState()
    val scope = rememberCoroutineScope()
    
    val spaceGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D1B2A),
            Color(0xFF1B263B),
            Color(0xFF415A77)
        )
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(spaceGradient)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Text(
                text = "🚗 Vehicle Selection",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            TextButton(
                onClick = { isVinMode = !isVinMode }
            ) {
                Text(
                    text = if (isVinMode) "Search" else "VIN",
                    color = Color(0xFF8ECAE6)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search/VIN Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query
                if (isVinMode && query.length == 17) {
                    scope.launch {
                        vehicleLookupService.lookupByVin(query)
                    }
                } else if (!isVinMode) {
                    scope.launch {
                        vehicleLookupService.searchByText(query)
                    }
                }
            },
            label = {
                Text(
                    text = if (isVinMode) "Enter VIN (17 characters)" else "Search vehicles...",
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            placeholder = {
                Text(
                    text = if (isVinMode) "1HGBH41JXMN109186" else "Toyota Camry 2020",
                    color = Color.White.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF8ECAE6)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { 
                        searchQuery = ""
                        scope.launch {
                            vehicleLookupService.searchByText("")
                        }
                    }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            keyboardOptions = if (isVinMode) {
                KeyboardOptions(keyboardType = KeyboardType.Text)
            } else {
                KeyboardOptions.Default
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF8ECAE6),
                unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Selected Vehicle Display
        selectedVehicle?.let { vehicle ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF219EBC).copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "✅ Selected Vehicle",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = vehicle.getDisplayName(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    vehicle.vin?.let { vin ->
                        Text(
                            text = "VIN: $vin",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { onVehicleSelected(vehicle) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🚀 Start Diagnostics",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Suggestions (for text search)
        if (!isVinMode && suggestions.isNotEmpty() && searchQuery.isNotEmpty()) {
            Text(
                text = "💡 Suggestions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height(120.dp)
            ) {
                items(suggestions.take(5)) { suggestion ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                searchQuery = suggestion
                                scope.launch {
                                    vehicleLookupService.searchByText(suggestion)
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF415A77).copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = suggestion,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Search Results
        if (searchResults.isNotEmpty()) {
            Text(
                text = "🔍 Search Results (${searchResults.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { vehicle ->
                    VehicleCard(
                        vehicle = vehicle,
                        onSelect = {
                            vehicleLookupService.selectVehicle(vehicle)
                        },
                        isSelected = selectedVehicle == vehicle
                    )
                }
            }
        } else if (searchQuery.isNotEmpty() && !isVinMode) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔍",
                        fontSize = 48.sp
                    )
                    Text(
                        text = "No vehicles found",
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Try a different search term",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
*/

// VehicleCard function commented out due to compilation errors

// Extension function commented out to avoid compilation errors
/*
private fun List<String>.joinIfEmpty(default: String): String {
    return if (isEmpty()) default else joinToString(", ")
}
*/

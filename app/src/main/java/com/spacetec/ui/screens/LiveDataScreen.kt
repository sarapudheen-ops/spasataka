package com.spacetec.ui.screens

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetec.obd.ObdManager
import com.spacetec.vehicle.VehicleData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveDataViewModel @Inject constructor(
    private val obdManager: ObdManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LiveDataUiState())
    val uiState: StateFlow<LiveDataUiState> = _uiState.asStateFlow()
    
    private val _vehicleData = MutableStateFlow(VehicleData())
    val vehicleData: StateFlow<VehicleData> = _vehicleData.asStateFlow()
    
    init {
        startDataCollection()
    }
    
    private fun startDataCollection() {
        viewModelScope.launch {
            // Collect vehicle data from OBD manager
            obdManager.vehicleData.collect { data ->
                _vehicleData.value = data
                _uiState.value = _uiState.value.copy(
                    isConnected = obdManager.isInitialized.value,
                    lastUpdate = System.currentTimeMillis()
                )
            }
        }
    }
    
    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            delay(1000) // Simulate refresh
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        obdManager.cleanup()
    }
}

data class LiveDataUiState(
    val isConnected: Boolean = false,
    val isRefreshing: Boolean = false,
    val lastUpdate: Long = 0L,
    val errorMessage: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveDataScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: LiveDataViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val vehicleData by viewModel.vehicleData.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Live Vehicle Data",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Connection Status Card
            ConnectionStatusCard(
                isConnected = uiState.isConnected,
                lastUpdate = uiState.lastUpdate
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Data Grid
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Engine Parameters
                item {
                    SectionHeader("Engine Parameters")
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LiveDataCard(
                            modifier = Modifier.weight(1f),
                            title = "RPM",
                            value = "${vehicleData.rpm}",
                            unit = "rpm",
                            icon = Icons.Default.Speed,
                            color = if (vehicleData.rpm > 3000) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.primary
                        )
                        
                        LiveDataCard(
                            modifier = Modifier.weight(1f),
                            title = "Engine Load",
                            value = "${vehicleData.engineLoad}",
                            unit = "%",
                            icon = Icons.Default.Engineering,
                            color = if (vehicleData.engineLoad > 80) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LiveDataCard(
                            modifier = Modifier.weight(1f),
                            title = "Coolant Temp",
                            value = "${vehicleData.coolantTemp}",
                            unit = "°C",
                            icon = Icons.Default.Thermostat,
                            color = if (vehicleData.coolantTemp > 105) MaterialTheme.colorScheme.error
                                   else if (vehicleData.coolantTemp > 95) MaterialTheme.colorScheme.tertiary
                                   else MaterialTheme.colorScheme.primary
                        )
                        
                        LiveDataCard(
                            modifier = Modifier.weight(1f),
                            title = "Throttle",
                            value = "${vehicleData.throttlePosition}",
                            unit = "%",
                            icon = Icons.Default.ElectricBolt,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Vehicle Status
                item {
                    SectionHeader("Vehicle Status")
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LiveDataCard(
                            modifier = Modifier.weight(1f),
                            title = "Speed",
                            value = "${vehicleData.speed}",
                            unit = "km/h",
                            icon = Icons.Default.Speed,
                            color = if (vehicleData.speed > 120) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.primary
                        )
                        
                        LiveDataCard(
                            modifier = Modifier.weight(1f),
                            title = "Fuel Level",
                            value = "${vehicleData.fuelLevel}",
                            unit = "%",
                            icon = Icons.Default.LocalGasStation,
                            color = if (vehicleData.fuelLevel < 15) MaterialTheme.colorScheme.error
                                   else if (vehicleData.fuelLevel < 25) MaterialTheme.colorScheme.tertiary
                                   else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Additional Parameters
                item {
                    SectionHeader("Additional Parameters")
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LiveDataCard(
                            modifier = Modifier.weight(1f),
                            title = "Intake Air",
                            value = "${vehicleData.intakeAirTemp}",
                            unit = "°C",
                            icon = Icons.Default.Air,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        LiveDataCard(
                            modifier = Modifier.weight(1f),
                            title = "MAF Rate",
                            value = String.format("%.1f", vehicleData.mafRate),
                            unit = "g/s",
                            icon = Icons.Default.Sensors,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
    
    // Show loading indicator when refreshing
    if (uiState.isRefreshing) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    isConnected: Boolean,
    lastUpdate: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                MaterialTheme.colorScheme.primaryContainer
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isConnected) 
                    MaterialTheme.colorScheme.onPrimaryContainer
                else 
                    MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = if (isConnected) "Connected to Vehicle" else "No Connection",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isConnected) 
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else 
                        MaterialTheme.colorScheme.onErrorContainer
                )
                
                if (isConnected && lastUpdate > 0) {
                    Text(
                        text = "Last update: ${formatTimestamp(lastUpdate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isConnected) 
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else 
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun LiveDataCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                
                if (unit.isNotEmpty()) {
                    Text(
                        text = " $unit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 1000 -> "just now"
        diff < 60000 -> "${diff / 1000}s ago"
        diff < 3600000 -> "${diff / 60000}m ago"
        else -> "${diff / 3600000}h ago"
    }
}

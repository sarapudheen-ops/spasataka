package com.spacetec.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetec.obd.ObdManager
import com.spacetec.vehicle.model.DtcInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DtcScannerViewModel @Inject constructor(
    private val obdManager: ObdManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DtcScannerUiState())
    val uiState: StateFlow<DtcScannerUiState> = _uiState.asStateFlow()
    
    private val _dtcCodes = MutableStateFlow<List<DtcInfo>>(emptyList())
    val dtcCodes: StateFlow<List<DtcInfo>> = _dtcCodes.asStateFlow()
    
    fun scanForDtcs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, errorMessage = null)
            
            try {
                // Initialize OBD connection if needed
                if (!obdManager.isInitialized.value) {
                    if (!obdManager.initialize()) {
                        _uiState.value = _uiState.value.copy(
                            isScanning = false,
                            errorMessage = "Failed to initialize OBD connection"
                        )
                        return@launch
                    }
                }
                
                // Simulate scan delay for better UX
                delay(2000)
                
                // Read DTCs - this would call real OBD methods
                val dtcList = readDtcCodesFromVehicle()
                _dtcCodes.value = dtcList
                
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    lastScanTime = System.currentTimeMillis(),
                    totalCodes = dtcList.size
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    errorMessage = "Scan failed: ${e.message}"
                )
            }
        }
    }
    
    fun clearDtcs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isClearing = true, errorMessage = null)
            
            try {
                // Simulate clear delay
                delay(1500)
                
                // Clear DTCs - this would call real OBD methods
                val success = clearDtcCodesFromVehicle()
                
                if (success) {
                    _dtcCodes.value = emptyList()
                    _uiState.value = _uiState.value.copy(
                        isClearing = false,
                        totalCodes = 0,
                        lastClearTime = System.currentTimeMillis()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isClearing = false,
                        errorMessage = "Failed to clear DTCs"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    errorMessage = "Clear failed: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun readDtcCodesFromVehicle(): List<DtcInfo> {
        // This would use real OBD commands in production
        // For now, return sample data that might be found
        return listOf(
            DtcInfo(
                code = "P0301",
                description = "Cylinder 1 Misfire Detected",
                severity = "High",
                system = "Powertrain",
                possibleCauses = listOf("Faulty spark plug", "Bad ignition coil", "Clogged fuel injector"),
                solutions = listOf("Replace spark plug", "Test ignition coil", "Clean fuel injector")
            ),
            DtcInfo(
                code = "P0171",
                description = "System Too Lean (Bank 1)",
                severity = "Medium",
                system = "Fuel System",
                possibleCauses = listOf("Vacuum leak", "MAF sensor fault", "Low fuel pressure"),
                solutions = listOf("Check vacuum lines", "Test MAF sensor", "Check fuel pump")
            )
        ).takeIf { obdManager.isInitialized.value && Math.random() > 0.3 } ?: emptyList()
    }
    
    private suspend fun clearDtcCodesFromVehicle(): Boolean {
        // This would use real OBD clear command in production
        return obdManager.isInitialized.value
    }
}

data class DtcScannerUiState(
    val isScanning: Boolean = false,
    val isClearing: Boolean = false,
    val totalCodes: Int = 0,
    val lastScanTime: Long = 0L,
    val lastClearTime: Long = 0L,
    val errorMessage: String? = null
)

// Remove local DtcInfo class as we use the one from vehicle.model
// Keep local enums for UI purposes
enum class DtcSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class DtcCategory {
    POWERTRAIN, CHASSIS, BODY, NETWORK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DtcScannerScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DtcScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dtcCodes by viewModel.dtcCodes.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "DTC Scanner",
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
            // Scan Status Card
            ScanStatusCard(
                totalCodes = uiState.totalCodes,
                isScanning = uiState.isScanning,
                isClearing = uiState.isClearing,
                lastScanTime = uiState.lastScanTime,
                errorMessage = uiState.errorMessage
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.scanForDtcs() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isScanning && !uiState.isClearing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scanning...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan DTCs")
                    }
                }
                
                if (dtcCodes.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.clearDtcs() },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isScanning && !uiState.isClearing,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (uiState.isClearing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.error,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clearing...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear DTCs")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // DTC Results
            if (dtcCodes.isNotEmpty()) {
                Text(
                    text = "Diagnostic Trouble Codes (${dtcCodes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dtcCodes) { dtc ->
                        DtcCard(dtcInfo = dtc)
                    }
                }
            } else if (!uiState.isScanning && uiState.lastScanTime > 0) {
                // No codes found
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "No Trouble Codes Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Text(
                            text = "Vehicle systems appear to be operating normally",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanStatusCard(
    totalCodes: Int,
    isScanning: Boolean,
    isClearing: Boolean,
    lastScanTime: Long,
    errorMessage: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                errorMessage != null -> MaterialTheme.colorScheme.errorContainer
                totalCodes > 0 -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when {
                            isScanning -> "Scanning for trouble codes..."
                            isClearing -> "Clearing trouble codes..."
                            errorMessage != null -> "Scan Error"
                            totalCodes > 0 -> "$totalCodes Trouble Code${if (totalCodes > 1) "s" else ""} Found"
                            lastScanTime > 0 -> "No Trouble Codes"
                            else -> "Ready to Scan"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    } else if (lastScanTime > 0 && !isScanning) {
                        Text(
                            text = "Last scan: ${formatTimestamp(lastScanTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (isScanning || isClearing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = when {
                            errorMessage != null -> Icons.Default.Error
                            totalCodes > 0 -> Icons.Default.Warning
                            lastScanTime > 0 -> Icons.Default.CheckCircle
                            else -> Icons.Default.Search
                        },
                        contentDescription = null,
                        tint = when {
                            errorMessage != null -> MaterialTheme.colorScheme.error
                            totalCodes > 0 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DtcCard(dtcInfo: DtcInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dtcInfo.code,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = getSeverityColor(dtcInfo.severity)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        SeverityChip(severity = dtcInfo.severity)
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = dtcInfo.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "System: ${dtcInfo.system}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Possible Solutions:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = dtcInfo.solutions.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SeverityChip(severity: String) {
    val severityEnum = when (severity.lowercase()) {
        "low" -> DtcSeverity.LOW
        "medium" -> DtcSeverity.MEDIUM 
        "high" -> DtcSeverity.HIGH
        "critical" -> DtcSeverity.CRITICAL
        else -> DtcSeverity.MEDIUM
    }
    val (color, text) = when (severityEnum) {
        DtcSeverity.LOW -> MaterialTheme.colorScheme.secondary to "LOW"
        DtcSeverity.MEDIUM -> MaterialTheme.colorScheme.tertiary to "MEDIUM"
        DtcSeverity.HIGH -> MaterialTheme.colorScheme.error to "HIGH"
        DtcSeverity.CRITICAL -> Color.Red to "CRITICAL"
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun getSeverityColor(severity: String): Color {
    val severityEnum = when (severity.lowercase()) {
        "low" -> DtcSeverity.LOW
        "medium" -> DtcSeverity.MEDIUM 
        "high" -> DtcSeverity.HIGH
        "critical" -> DtcSeverity.CRITICAL
        else -> DtcSeverity.MEDIUM
    }
    return when (severityEnum) {
        DtcSeverity.LOW -> MaterialTheme.colorScheme.secondary
        DtcSeverity.MEDIUM -> MaterialTheme.colorScheme.tertiary
        DtcSeverity.HIGH -> MaterialTheme.colorScheme.error
        DtcSeverity.CRITICAL -> Color.Red
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

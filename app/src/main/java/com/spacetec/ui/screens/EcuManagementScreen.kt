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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacetec.ui.theme.SpaceTecTheme
import com.spacetec.vehicle.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcuManagementScreen(
    viewModel: EcuManagementViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val availableEcus by viewModel.availableEcus.collectAsState()
    val testProgress by viewModel.testProgress.collectAsState()
    val programmingProgress by viewModel.programmingProgress.collectAsState()
    val diagnosticSummary by viewModel.diagnosticSummary.collectAsState()
    
    var showVehicleSelector by remember { mutableStateOf(selectedVehicle == null) }
    var selectedEcu by remember { mutableStateOf<EcuCapability?>(null) }
    var showTestDialog by remember { mutableStateOf(false) }
    var showProgrammingDialog by remember { mutableStateOf(false) }
    
    SpaceTecTheme {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ECU Management",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Row {
                    IconButton(
                        onClick = { showVehicleSelector = true }
                    ) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = "Select Vehicle",
                            tint = Color.White
                        )
                    }
                    
                    IconButton(
                        onClick = { 
                            scope.launch {
                                viewModel.refreshVehicleData()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (selectedVehicle == null) {
                // Vehicle selection prompt
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E3A8A).copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Select Vehicle",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Text(
                            text = "Choose a vehicle to access ECU testing and programming capabilities",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { showVehicleSelector = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B82F6)
                            )
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse Vehicles")
                        }
                    }
                }
            } else {
                // Vehicle info and ECU management
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Vehicle summary card
                    item {
                        VehicleSummaryCard(
                            vehicle = selectedVehicle!!,
                            summary = diagnosticSummary,
                            onChangeVehicle = { showVehicleSelector = true }
                        )
                    }
                    
                    // Progress indicators
                    if (testProgress != null || programmingProgress != null) {
                        item {
                            ProgressCard(
                                testProgress = testProgress,
                                programmingProgress = programmingProgress
                            )
                        }
                    }
                    
                    // ECU list
                    items(availableEcus) { ecu ->
                        EcuCard(
                            ecu = ecu,
                            onTestEcu = { 
                                selectedEcu = ecu
                                showTestDialog = true
                            },
                            onProgramEcu = {
                                selectedEcu = ecu
                                showProgrammingDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Vehicle selector dialog
    if (showVehicleSelector) {
        VehicleSelectorDialog(
            onVehicleSelected = { make, model, year, engine ->
                scope.launch {
                    viewModel.selectVehicle(make, model, year, engine)
                    showVehicleSelector = false
                }
            },
            onDismiss = { showVehicleSelector = false }
        )
    }
    
    // ECU test dialog
    if (showTestDialog && selectedEcu != null) {
        EcuTestDialog(
            ecu = selectedEcu!!,
            onExecuteTest = { testId, parameters ->
                scope.launch {
                    viewModel.executeEcuTest(selectedEcu!!.ecuId, testId, parameters)
                    showTestDialog = false
                }
            },
            onDismiss = { showTestDialog = false }
        )
    }
    
    // ECU programming dialog
    if (showProgrammingDialog && selectedEcu != null) {
        EcuProgrammingDialog(
            ecu = selectedEcu!!,
            onProgramEcu = { firmwareFile, programmingType, options ->
                scope.launch {
                    viewModel.programEcu(selectedEcu!!.ecuId, firmwareFile, programmingType, options)
                    showProgrammingDialog = false
                }
            },
            onDismiss = { showProgrammingDialog = false }
        )
    }
}

@Composable
private fun VehicleSummaryCard(
    vehicle: VehicleEcuProfile,
    summary: VehicleDiagnosticSummary?,
    onChangeVehicle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E3A8A).copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${vehicle.make} ${vehicle.model}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${vehicle.yearRange} • ${vehicle.engine}",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                TextButton(onClick = onChangeVehicle) {
                    Text("Change", color = Color(0xFF3B82F6))
                }
            }
            
            if (summary != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryItem(
                        icon = Icons.Default.Memory,
                        label = "ECUs",
                        value = summary.ecuCount.toString()
                    )
                    SummaryItem(
                        icon = Icons.Default.Science,
                        label = "Tests",
                        value = summary.totalAvailableTests.toString()
                    )
                    SummaryItem(
                        icon = Icons.Default.Upload,
                        label = "Programmable",
                        value = summary.programmableEcuCount.toString()
                    )
                    SummaryItem(
                        icon = Icons.Default.Security,
                        label = "Secured",
                        value = summary.securityProtectedEcuCount.toString()
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ProgressCard(
    testProgress: TestProgress?,
    programmingProgress: ProgrammingProgress?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF059669).copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (testProgress != null) {
                Text(
                    text = "Test in Progress",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = testProgress.stepDescription,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { testProgress.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF10B981)
                )
            }
            
            if (programmingProgress != null) {
                Text(
                    text = "Programming in Progress",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = programmingProgress.stepDescription,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { programmingProgress.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF3B82F6)
                )
            }
        }
    }
}

@Composable
private fun EcuCard(
    ecu: EcuCapability,
    onTestEcu: () -> Unit,
    onProgramEcu: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF374151).copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = ecu.ecuName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "ID: ${ecu.ecuId} • ${ecu.supportedProtocols.joinToString(", ")}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                Row {
                    if (ecu.supportedTests.isNotEmpty()) {
                        IconButton(onClick = onTestEcu) {
                            Icon(
                                Icons.Default.Science,
                                contentDescription = "Test ECU",
                                tint = Color(0xFF10B981)
                            )
                        }
                    }
                    
                    if (ecu.programmingSupport.flashSupported) {
                        IconButton(onClick = onProgramEcu) {
                            Icon(
                                Icons.Default.Upload,
                                contentDescription = "Program ECU",
                                tint = Color(0xFF3B82F6)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${ecu.supportedTests.size} tests available",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                if (ecu.securityAccess != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFFF59E0B)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Secured",
                            fontSize = 12.sp,
                            color = Color(0xFFF59E0B)
                        )
                    }
                }
            }
        }
    }
}

// Placeholder composables for dialogs (to be implemented)
@Composable
private fun VehicleSelectorDialog(
    onVehicleSelected: (String, String, Int, String?) -> Unit,
    onDismiss: () -> Unit
) {
    // Implementation for vehicle selection dialog
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Vehicle") },
        text = { Text("Vehicle selection dialog implementation needed") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun EcuTestDialog(
    ecu: EcuCapability,
    onExecuteTest: (String, Map<String, Any>) -> Unit,
    onDismiss: () -> Unit
) {
    // Implementation for ECU test dialog
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Test ${ecu.ecuName}") },
        text = { Text("ECU test dialog implementation needed") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun EcuProgrammingDialog(
    ecu: EcuCapability,
    onProgramEcu: (File, ProgrammingType, ProgrammingOptions) -> Unit,
    onDismiss: () -> Unit
) {
    // Implementation for ECU programming dialog
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Program ${ecu.ecuName}") },
        text = { Text("ECU programming dialog implementation needed") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

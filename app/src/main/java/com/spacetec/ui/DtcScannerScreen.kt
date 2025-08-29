package com.spacetec.ui

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateContentSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DtcScannerScreen(
    onNavigateBack: () -> Unit,
    onCodeSelected: (String) -> Unit = {},
    viewModel: com.spacetec.ui.viewmodel.DiagnosticViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    // Get real DTCs from ViewModel
    val dtcCodes by viewModel.dtcCodes.collectAsState()
    val uiState by viewModel.uiState
    val isConnected = uiState.isConnected
    
    // Convert string DTC codes to DTC objects with descriptions
    val dtcs = remember(dtcCodes) {
        dtcCodes.map { code ->
            DTC(
                code = code,
                shortDescription = getDtcDescription(code),
                longDescription = getDtcLongDescription(code)
            )
        }
    }

    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var selectedDtc by remember { mutableStateOf<DTC?>(null) }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            // Simulate scanning progress
            while (scanProgress < 1f) {
                scanProgress = (scanProgress + 0.1f).coerceAtMost(1f)
                kotlinx.coroutines.delay(200)
            }
            isScanning = false
        } else {
            scanProgress = 0f
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("DTC Scanner") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(onClick = { isScanning = true; scanProgress = 0f }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Rescan"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // Scan progress bar
                if (isScanning) {
                    LinearProgressIndicator(
                        progress = { scanProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "Scanning vehicle systems... ${(scanProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.clearDtcCodes() },
                        modifier = Modifier.weight(1f),
                        enabled = dtcs.isNotEmpty() && !isScanning && isConnected,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Clear All Codes")
                    }
                    
                    Button(
                        onClick = { 
                            isScanning = true
                            viewModel.readDtcCodes()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isScanning && isConnected,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Scan Again")
                    }
                }
            }
        }
    ) { padding ->
        if (dtcs.isEmpty()) {
            // No DTCs found state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Trouble Codes Found",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your vehicle's systems are operating normally. No diagnostic trouble codes were found during the scan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // DTCs list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dtcs) { dtc ->
                    DtcItem(
                        dtc = dtc,
                        isSelected = selectedDtc == dtc,
                        onClick = { selectedDtc = if (selectedDtc == dtc) null else dtc }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DtcItem(
    dtc: DTC,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when (dtc.code.first()) {
        'P' -> MaterialTheme.colorScheme.errorContainer.copy(alpha = if (isSelected) 0.8f else 0.5f)
        'C' -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (isSelected) 0.8f else 0.5f)
        'B' -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isSelected) 0.8f else 0.5f)
        'U' -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isSelected) 0.8f else 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isSelected) 0.8f else 0.5f)
    }
    
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = if (isSelected) CardDefaults.elevatedCardElevation() else CardDefaults.cardElevation(),
        modifier = Modifier.animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with code and description
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // DTC Code
                Text(
                    text = dtc.code,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Severity indicator
                Box(
                    modifier = Modifier
                        .background(
                            color = when (dtc.code.first()) {
                                'P' -> MaterialTheme.colorScheme.error
                                'C' -> MaterialTheme.colorScheme.tertiary
                                'B' -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when (dtc.code.first()) {
                            'P' -> "POWERTRAIN"
                            'C' -> "CHASSIS"
                            'B' -> "BODY"
                            'U' -> "NETWORK"
                            else -> "UNKNOWN"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
            
            // Short description
            Text(
                text = dtc.shortDescription,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            // Expanded details
            if (isSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = dtc.longDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                // Possible causes
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Possible causes:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• Faulty sensor\n• Wiring issues\n• Mechanical problem\n• Software update needed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                
                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { /* View freeze frame data */ }) {
                        Text("View Freeze Frame")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { /* Clear this code */ }) {
                        Text("Clear This Code")
                    }
                }
            }
        }
    }
}

data class DTC(
    val code: String,
    val shortDescription: String,
    val longDescription: String
)

// Helper functions to get DTC descriptions
fun getDtcDescription(code: String): String {
    return when (code) {
        "P0000" -> "No DTCs Detected"
        "P0100" -> "Mass Air Flow Circuit Malfunction"
        "P0101" -> "Mass Air Flow Circuit Range/Performance"
        "P0102" -> "Mass Air Flow Circuit Low Input"
        "P0103" -> "Mass Air Flow Circuit High Input"
        "P0104" -> "Mass Air Flow Circuit Intermittent"
        "P0105" -> "Manifold Absolute Pressure/Barometric Pressure Circuit Malfunction"
        "P0106" -> "Manifold Absolute Pressure/Barometric Pressure Circuit Range/Performance Problem"
        "P0107" -> "Manifold Absolute Pressure/Barometric Pressure Circuit Low Input"
        "P0108" -> "Manifold Absolute Pressure/Barometric Pressure Circuit High Input"
        "P0109" -> "Manifold Absolute Pressure/Barometric Pressure Circuit Intermittent"
        "P0110" -> "Intake Air Temperature Circuit Malfunction"
        "P0111" -> "Intake Air Temperature Circuit Range/Performance Problem"
        "P0112" -> "Intake Air Temperature Circuit Low Input"
        "P0113" -> "Intake Air Temperature Circuit High Input"
        "P0114" -> "Intake Air Temperature Circuit Intermittent"
        "P0115" -> "Engine Coolant Temperature Circuit Malfunction"
        "P0116" -> "Engine Coolant Temperature Circuit Range/Performance Problem"
        "P0117" -> "Engine Coolant Temperature Circuit Low Input"
        "P0118" -> "Engine Coolant Temperature Circuit High Input"
        "P0119" -> "Engine Coolant Temperature Circuit Intermittent"
        "P0120" -> "Throttle/Pedal Position Sensor/Switch A Circuit Malfunction"
        "P0121" -> "Throttle/Pedal Position Sensor/Switch A Circuit Range/Performance Problem"
        "P0122" -> "Throttle/Pedal Position Sensor/Switch A Circuit Low Input"
        "P0123" -> "Throttle/Pedal Position Sensor/Switch A Circuit High Input"
        "P0124" -> "Throttle/Pedal Position Sensor/Switch A Circuit Intermittent"
        "P0125" -> "Insufficient Coolant Temperature For Closed Loop Fuel Control"
        "P0126" -> "Insufficient Coolant Temperature For Stable Operation"
        "P0130" -> "O2 Circuit Malfunction (Bank 1 Sensor 1)"
        "P0131" -> "O2 Sensor Circuit Low Voltage (Bank 1 Sensor 1)"
        "P0132" -> "O2 Sensor Circuit High Voltage (Bank 1 Sensor 1)"
        "P0133" -> "O2 Sensor Circuit Slow Response (Bank 1 Sensor 1)"
        "P0134" -> "O2 Sensor Circuit No Activity Detected (Bank 1 Sensor 1)"
        "P0135" -> "O2 Sensor Heater Circuit Malfunction (Bank 1 Sensor 1)"
        "P0136" -> "O2 Sensor Circuit Malfunction (Bank 1 Sensor 2)"
        "P0137" -> "O2 Sensor Circuit Low Voltage (Bank 1 Sensor 2)"
        "P0138" -> "O2 Sensor Circuit High Voltage (Bank 1 Sensor 2)"
        "P0139" -> "O2 Sensor Circuit Slow Response (Bank 1 Sensor 2)"
        "P0140" -> "O2 Sensor Circuit No Activity Detected (Bank 1 Sensor 2)"
        "P0141" -> "O2 Sensor Heater Circuit Malfunction (Bank 1 Sensor 2)"
        "P0150" -> "O2 Circuit Malfunction (Bank 2 Sensor 1)"
        "P0151" -> "O2 Sensor Circuit Low Voltage (Bank 2 Sensor 1)"
        "P0152" -> "O2 Sensor Circuit High Voltage (Bank 2 Sensor 1)"
        "P0153" -> "O2 Sensor Circuit Slow Response (Bank 2 Sensor 1)"
        "P0154" -> "O2 Sensor Circuit No Activity Detected (Bank 2 Sensor 1)"
        "P0155" -> "O2 Sensor Heater Circuit Malfunction (Bank 2 Sensor 1)"
        "P0156" -> "O2 Sensor Circuit Malfunction (Bank 2 Sensor 2)"
        "P0157" -> "O2 Sensor Circuit Low Voltage (Bank 2 Sensor 2)"
        "P0158" -> "O2 Sensor Circuit High Voltage (Bank 2 Sensor 2)"
        "P0159" -> "O2 Sensor Circuit Slow Response (Bank 2 Sensor 2)"
        "P0160" -> "O2 Sensor Circuit No Activity Detected (Bank 2 Sensor 2)"
        "P0161" -> "O2 Sensor Heater Circuit Malfunction (Bank 2 Sensor 2)"
        "P0171" -> "System Too Lean (Bank 1)"
        "P0172" -> "System Too Rich (Bank 1)"
        "P0173" -> "Fuel Trim Malfunction (Bank 2)"
        "P0174" -> "System Too Lean (Bank 2)"
        "P0175" -> "System Too Rich (Bank 2)"
        "P0300" -> "Random/Multiple Cylinder Misfire Detected"
        "P0301" -> "Cylinder 1 Misfire Detected"
        "P0302" -> "Cylinder 2 Misfire Detected"
        "P0303" -> "Cylinder 3 Misfire Detected"
        "P0304" -> "Cylinder 4 Misfire Detected"
        "P0305" -> "Cylinder 5 Misfire Detected"
        "P0306" -> "Cylinder 6 Misfire Detected"
        "P0307" -> "Cylinder 7 Misfire Detected"
        "P0308" -> "Cylinder 8 Misfire Detected"
        "P0420" -> "Catalyst System Efficiency Below Threshold (Bank 1)"
        "P0421" -> "Warm Up Catalyst Efficiency Below Threshold (Bank 1)"
        "P0430" -> "Catalyst System Efficiency Below Threshold (Bank 2)"
        "P0431" -> "Warm Up Catalyst Efficiency Below Threshold (Bank 2)"
        "P0440" -> "Evaporative Emission Control System Malfunction"
        "P0441" -> "Evaporative Emission Control System Incorrect Purge Flow"
        "P0442" -> "Evaporative Emission Control System Leak Detected (Small Leak)"
        "P0443" -> "Evaporative Emission Control System Purge Control Valve Circuit Malfunction"
        "P0446" -> "Evaporative Emission Control System Vent Control Circuit Malfunction"
        "P0455" -> "Evaporative Emission Control System Leak Detected (Large Leak)"
        "P0500" -> "Vehicle Speed Sensor Malfunction"
        "P0501" -> "Vehicle Speed Sensor Range/Performance"
        "P0502" -> "Vehicle Speed Sensor Circuit Low Input"
        "P0503" -> "Vehicle Speed Sensor Intermittent/Erratic/High"
        "P0505" -> "Idle Control System Malfunction"
        "P0506" -> "Idle Control System RPM Lower Than Expected"
        "P0507" -> "Idle Control System RPM Higher Than Expected"
        "P0510" -> "Closed Throttle Position Switch Malfunction"
        "P0550" -> "Power Steering Pressure Sensor Circuit Malfunction"
        "P0560" -> "System Voltage Malfunction"
        "P0562" -> "System Voltage Low"
        "P0563" -> "System Voltage High"
        "P0601" -> "Internal Control Module Memory Check Sum Error"
        "P0602" -> "Control Module Programming Error"
        "P0603" -> "Internal Control Module Keep Alive Memory (KAM) Error"
        "P0604" -> "Internal Control Module Random Access Memory (RAM) Error"
        "P0605" -> "Internal Control Module Read Only Memory (ROM) Error"
        "P0606" -> "PCM Processor Fault"
        "P0607" -> "Control Module Performance"
        "P0700" -> "Transmission Control System Malfunction"
        "P0701" -> "Transmission Control System Range/Performance"
        "P0702" -> "Transmission Control System Electrical"
        "P0703" -> "Torque Converter/Brake Switch B Circuit Malfunction"
        "P0704" -> "Clutch Switch Input Circuit Malfunction"
        "P0705" -> "Transmission Range Sensor Circuit Malfunction (PRNDL Input)"
        "B1000" -> "ECU Malfunction"
        "C1234" -> "ABS Wheel Speed Sensor Circuit Malfunction"
        "U0073" -> "Control Module Communication Bus A Off"
        "U0100" -> "Lost Communication With ECM/PCM A"
        "U0101" -> "Lost Communication With TCM"
        "U0155" -> "Lost Communication With Instrument Panel Control Module"
        else -> "Unknown DTC Code - $code"
    }
}

fun getDtcLongDescription(code: String): String {
    return when (code) {
        "P0000" -> "This code indicates that no diagnostic trouble codes are currently stored in the vehicle's computer memory."
        "P0100" -> "The Mass Air Flow (MAF) sensor circuit has a malfunction. The MAF sensor measures the amount of air entering the engine."
        "P0171" -> "The fuel system is running too lean on Bank 1. This means there's too much air and not enough fuel in the air/fuel mixture."
        "P0172" -> "The fuel system is running too rich on Bank 1. This means there's too much fuel and not enough air in the air/fuel mixture."
        "P0300" -> "The engine control module has detected random or multiple cylinder misfires. This can cause rough idling, poor acceleration, and increased emissions."
        "P0301" -> "Cylinder 1 is misfiring. This means the air/fuel mixture in cylinder 1 is not igniting properly."
        "P0302" -> "Cylinder 2 is misfiring. This means the air/fuel mixture in cylinder 2 is not igniting properly."
        "P0420" -> "The catalytic converter on Bank 1 is not operating efficiently. The converter may be contaminated or failing."
        "P0440" -> "There's a malfunction in the evaporative emission control system, which prevents fuel vapors from escaping to the atmosphere."
        "P0500" -> "The vehicle speed sensor is malfunctioning or sending an incorrect signal to the engine control module."
        "P0700" -> "The transmission control system has detected a malfunction. This code often accompanies other transmission-related codes."
        "B1000" -> "The Engine Control Unit (ECU) has detected an internal malfunction or communication error."
        "C1234" -> "The ABS wheel speed sensor circuit has a malfunction, which can affect the anti-lock braking system operation."
        "U0100" -> "Communication has been lost with the Engine Control Module (ECM) or Powertrain Control Module (PCM)."
        else -> "This diagnostic trouble code indicates a problem with your vehicle that should be diagnosed by a qualified technician. The code $code provides specific information about the malfunction that can help in troubleshooting and repair."
    }
}

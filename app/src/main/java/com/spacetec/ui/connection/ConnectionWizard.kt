package com.spacetec.ui.connection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import android.util.Log

/**
 * Step-by-step connection wizard for OBD setup
 */
@Composable
fun ConnectionWizard(
    currentStep: ConnectionStep,
    onStepComplete: (ConnectionStep) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress indicator
        ConnectionProgressIndicator(
            currentStep = currentStep,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Step content
        when (currentStep) {
            ConnectionStep.CheckObd -> CheckObdStep(onStepComplete)
            ConnectionStep.SearchDevices -> SearchDevicesStep(onStepComplete)
            ConnectionStep.PairDevice -> PairDeviceStep(onStepComplete)
            ConnectionStep.TestConnection -> TestConnectionStep(onStepComplete)
            ConnectionStep.Complete -> CompleteStep()
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
            
            if (currentStep != ConnectionStep.Complete) {
                Button(
                    onClick = { onStepComplete(currentStep.next()) }
                ) {
                    Text("Next")
                }
            }
        }
    }
}

@Composable
private fun ConnectionProgressIndicator(
    currentStep: ConnectionStep,
    modifier: Modifier = Modifier
) {
    val steps = ConnectionStep.values()
    val currentIndex = steps.indexOf(currentStep)
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            StepIndicator(
                step = step,
                isActive = index == currentIndex,
                isCompleted = index < currentIndex,
                modifier = Modifier.weight(1f)
            )
            
            if (index < steps.size - 1) {
                Divider(
                    modifier = Modifier
                        .width(32.dp)
                        .height(2.dp),
                    color = if (index < currentIndex) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(
    step: ConnectionStep,
    isActive: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = if (isActive) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.outline
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = step.title,
                            tint = if (isActive) 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = step.title,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CheckObdStep(onStepComplete: (ConnectionStep) -> Unit) {
    var isChecking by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isChecking = true
        delay(2000) // Simulate OBD check
        isChecking = false
        onStepComplete(ConnectionStep.SearchDevices)
    }
    
    StepContent(
        title = "Checking OBD Port",
        description = "Please ensure your vehicle's OBD-II port is accessible and the ignition is on.",
        icon = Icons.Default.Search,
        isLoading = isChecking
    ) {
        ObdCheckInstructions()
    }
}

@Composable
private fun SearchDevicesStep(onStepComplete: (ConnectionStep) -> Unit) {
    var isSearching by remember { mutableStateOf(false) }
    var foundDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        isSearching = true
        delay(3000) // Simulate device search
        foundDevices = listOf(
            BluetoothDevice("OBD-II Scanner", "00:11:22:33:44:55"),
            BluetoothDevice("ELM327 v1.5", "AA:BB:CC:DD:EE:FF")
        )
        isSearching = false
    }
    
    StepContent(
        title = "Searching for Devices",
        description = "Looking for nearby OBD-II adapters...",
        icon = Icons.Default.Bluetooth,
        isLoading = isSearching
    ) {
        if (foundDevices.isNotEmpty()) {
            DeviceList(
                devices = foundDevices,
                onDeviceSelected = { onStepComplete(ConnectionStep.PairDevice) }
            )
        }
    }
}

@Composable
private fun PairDeviceStep(onStepComplete: (ConnectionStep) -> Unit) {
    var isPairing by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isPairing = true
        delay(2500) // Simulate pairing
        isPairing = false
        onStepComplete(ConnectionStep.TestConnection)
    }
    
    StepContent(
        title = "Pairing Device",
        description = "Establishing connection with your OBD-II adapter...",
        icon = Icons.Default.Link,
        isLoading = isPairing
    ) {
        PairingInstructions()
    }
}

@Composable
private fun TestConnectionStep(onStepComplete: (ConnectionStep) -> Unit) {
    var isTesting by remember { mutableStateOf(false) }
    var testResults by remember { mutableStateOf<List<TestResult>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        isTesting = true
        delay(3000) // Simulate connection test
        testResults = listOf(
            TestResult("Protocol Detection", true, "ISO 15765-4 (CAN)"),
            TestResult("ECU Communication", true, "Engine ECU responding"),
            TestResult("PID Support", true, "Standard PIDs available")
        )
        isTesting = false
        onStepComplete(ConnectionStep.Complete)
    }
    
    StepContent(
        title = "Testing Connection",
        description = "Verifying communication with your vehicle's ECU...",
        icon = Icons.Default.Speed,
        isLoading = isTesting
    ) {
        if (testResults.isNotEmpty()) {
            TestResultsList(results = testResults)
        }
    }
}

@Composable
private fun CompleteStep() {
    StepContent(
        title = "Setup Complete!",
        description = "Your OBD-II connection is ready. You can now start monitoring your vehicle.",
        icon = Icons.Default.CheckCircle,
        isLoading = false
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Connection Established",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Ready to start vehicle diagnostics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StepContent(
    title: String,
    description: String,
    icon: ImageVector,
    isLoading: Boolean,
    content: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            content()
        }
    }
}

@Composable
private fun ObdCheckInstructions() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Before we begin:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val instructions = listOf(
                "Turn on your vehicle's ignition (engine can be off)",
                "Locate the OBD-II port (usually under the dashboard)",
                "Ensure your OBD-II adapter is properly connected",
                "Enable Bluetooth on your device"
            )
            
            instructions.forEach { instruction ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<BluetoothDevice>,
    onDeviceSelected: (BluetoothDevice) -> Unit
) {
    LazyColumn {
        items(devices) { device ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                onClick = { onDeviceSelected(device) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "Bluetooth Device",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Text(
                            text = device.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Select",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PairingInstructions() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pairing in progress...",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "If prompted, enter PIN: 1234 or 0000",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TestResultsList(results: List<TestResult>) {
    LazyColumn {
        items(results) { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = if (result.success) "Success" else "Error",
                        tint = if (result.success) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = result.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Text(
                            text = result.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Connection wizard steps
 */
sealed class ConnectionStep(
    val title: String,
    val icon: ImageVector
) {
    object CheckObd : ConnectionStep("Check OBD", Icons.Default.Search)
    object SearchDevices : ConnectionStep("Search", Icons.Default.Bluetooth)
    object PairDevice : ConnectionStep("Pair", Icons.Default.Link)
    object TestConnection : ConnectionStep("Test", Icons.Default.Speed)
    object Complete : ConnectionStep("Complete", Icons.Default.CheckCircle)
    
    fun next(): ConnectionStep {
        return when (this) {
            CheckObd -> SearchDevices
            SearchDevices -> PairDevice
            PairDevice -> TestConnection
            TestConnection -> Complete
            Complete -> Complete
        }
    }
    
    companion object {
        fun values(): Array<ConnectionStep> = arrayOf(
            CheckObd, SearchDevices, PairDevice, TestConnection, Complete
        )
    }
}

/**
 * Data classes
 */
data class BluetoothDevice(
    val name: String,
    val address: String
)

data class TestResult(
    val name: String,
    val success: Boolean,
    val details: String
)

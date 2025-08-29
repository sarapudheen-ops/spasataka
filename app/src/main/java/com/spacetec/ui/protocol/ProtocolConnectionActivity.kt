package com.spacetec.ui.protocol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spacetec.diagnostic.protocol.AutelProtocolHandler
import com.spacetec.ui.theme.SpaceTecTheme

class ProtocolConnectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpaceTecTheme {
                ProtocolConnectionScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocolConnectionScreen(
    viewModel: ProtocolConnectionViewModel = viewModel()
) {
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val protocolVersion by viewModel.protocolVersion.collectAsState()
    val bluetoothDevices by viewModel.bluetoothDevices.collectAsState()
    val connectionInfo by viewModel.connectionInfo.collectAsState()
    
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1B263B).copy(alpha = 0.8f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Cable,
                    contentDescription = "Protocol Connection",
                    tint = Color(0xFF64B5F6),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Protocol Connection",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Advanced Communication Protocols",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF90CAF9)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Connection Status
        ConnectionStatusCard(
            status = connectionStatus,
            protocolVersion = protocolVersion,
            connectionInfo = connectionInfo
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Connection Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // TCP Connection
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2E3B4E).copy(alpha = 0.9f)
                ),
                onClick = { /* Show TCP connection dialog */ }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "TCP Connection",
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TCP/IP",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Network Connection",
                        color = Color(0xFF90CAF9),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Bluetooth Connection
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2E3B4E).copy(alpha = 0.9f)
                ),
                onClick = { viewModel.scanBluetoothDevices() }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "Bluetooth Connection",
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bluetooth",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Wireless Connection",
                        color = Color(0xFF90CAF9),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Bluetooth Devices List
        if (bluetoothDevices.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2E3B4E).copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Available Devices",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(bluetoothDevices) { device ->
                            BluetoothDeviceCard(
                                deviceName = device,
                                onConnect = { viewModel.connectBluetooth(device) }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Protocol Actions
        if (connectionStatus == AutelProtocolHandler.ConnectionStatus.AUTHENTICATED) {
            ProtocolActionsCard(
                onSendDiagnostic = { viewModel.sendDiagnosticRequest("ECM", "READ_DTC") },
                onRequestLiveData = { viewModel.requestLiveData(listOf("0105", "010C", "010D")) },
                onReadDtc = { viewModel.readDtcCodes() },
                onClearDtc = { viewModel.clearDtcCodes() }
            )
        }
    }
}

@Composable
fun ConnectionStatusCard(
    status: AutelProtocolHandler.ConnectionStatus,
    protocolVersion: String?,
    connectionInfo: Map<String, String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                AutelProtocolHandler.ConnectionStatus.CONNECTED,
                AutelProtocolHandler.ConnectionStatus.AUTHENTICATED -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                AutelProtocolHandler.ConnectionStatus.ERROR -> Color(0xFFE53E3E).copy(alpha = 0.2f)
                AutelProtocolHandler.ConnectionStatus.CONNECTING,
                AutelProtocolHandler.ConnectionStatus.AUTHENTICATING -> Color(0xFFFF9800).copy(alpha = 0.2f)
                else -> Color(0xFF2E3B4E).copy(alpha = 0.9f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (status) {
                        AutelProtocolHandler.ConnectionStatus.CONNECTED,
                        AutelProtocolHandler.ConnectionStatus.AUTHENTICATED -> Icons.Default.CheckCircle
                        AutelProtocolHandler.ConnectionStatus.ERROR -> Icons.Default.Error
                        AutelProtocolHandler.ConnectionStatus.CONNECTING,
                        AutelProtocolHandler.ConnectionStatus.AUTHENTICATING -> Icons.Default.Sync
                        else -> Icons.Default.SignalWifiOff
                    },
                    contentDescription = status.name,
                    tint = when (status) {
                        AutelProtocolHandler.ConnectionStatus.CONNECTED,
                        AutelProtocolHandler.ConnectionStatus.AUTHENTICATED -> Color(0xFF4CAF50)
                        AutelProtocolHandler.ConnectionStatus.ERROR -> Color(0xFFE53E3E)
                        AutelProtocolHandler.ConnectionStatus.CONNECTING,
                        AutelProtocolHandler.ConnectionStatus.AUTHENTICATING -> Color(0xFFFF9800)
                        else -> Color(0xFF64B5F6)
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Status: ${status.name.replace("_", " ")}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (protocolVersion != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Protocol Version: $protocolVersion",
                    color = Color(0xFF90CAF9),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (connectionInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                connectionInfo.forEach { (key, value) ->
                    Text(
                        text = "${key.replace("_", " ")}: $value",
                        color = Color(0xFF90CAF9),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun BluetoothDeviceCard(
    deviceName: String,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF415A77).copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = "Bluetooth Device",
                tint = Color(0xFF64B5F6),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = deviceName,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF64B5F6)
                )
            ) {
                Text("Connect", color = Color.White)
            }
        }
    }
}

@Composable
fun ProtocolActionsCard(
    onSendDiagnostic: () -> Unit,
    onRequestLiveData: () -> Unit,
    onReadDtc: () -> Unit,
    onClearDtc: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2E3B4E).copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Protocol Actions",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSendDiagnostic,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF64B5F6)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Diagnostic",
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Diagnostic", style = MaterialTheme.typography.labelSmall)
                    }
                }
                
                Button(
                    onClick = onRequestLiveData,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Live Data",
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Live Data", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onReadDtc,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Read DTC",
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Read DTC", style = MaterialTheme.typography.labelSmall)
                    }
                }
                
                Button(
                    onClick = onClearDtc,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53E3E)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear DTC",
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Clear DTC", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

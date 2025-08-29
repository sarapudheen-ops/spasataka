package com.spacetec.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController = rememberNavController()
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Satellite,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Welcome to SpaceTec",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Professional Vehicle Diagnostics",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        item {
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "Connect OBD",
                    icon = Icons.Default.Bluetooth,
                    onClick = { 
                        navController.navigate("obd_connection")
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "Scan Vehicle",
                    icon = Icons.Default.Search,
                    onClick = { 
                        navController.navigate("diagnostics")
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    title = "View Dashboard",
                    icon = Icons.Default.Assessment,
                    onClick = { 
                        navController.navigate("dashboard")
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "Settings",
                    icon = Icons.Default.Settings,
                    onClick = { 
                        navController.navigate("settings")
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                "System Status",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            StatusCard(navController)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
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
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusCard(navController: NavController) {
    var bluetoothStatus by remember { mutableStateOf("Ready") }
    var obdStatus by remember { mutableStateOf("Disconnected") }
    var vehicleLibStatus by remember { mutableStateOf("Loaded") }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = { navController.navigate("system_status") }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            StatusItem("Bluetooth", bluetoothStatus, Icons.Default.Bluetooth, bluetoothStatus == "Ready")
            StatusItem("OBD Connection", obdStatus, Icons.Default.Cable, obdStatus == "Connected")
            StatusItem("Vehicle Library", vehicleLibStatus, Icons.Default.LibraryBooks, vehicleLibStatus == "Loaded")
            
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { navController.navigate("vehicle_library") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.LibraryBooks, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Browse Vehicle Library")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tap for detailed system information",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatusItem(
    label: String,
    status: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isHealthy: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            status,
            style = MaterialTheme.typography.bodySmall,
            color = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

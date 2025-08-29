package com.spacetec.ui.screens.diagnostics

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
import androidx.compose.ui.unit.dp
import com.spacetec.vehicle.DiagnosticTroubleCode
import com.spacetec.vehicle.DtcSeverity
import com.spacetec.vehicle.DtcStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit
) {
    // Sample DTCs - in a real app, these would come from a ViewModel
    val dtcs = remember {
        listOf(
            DiagnosticTroubleCode(
                code = "P0171",
                description = "System Too Lean (Bank 1)",
                status = DtcStatus.ACTIVE,
                severity = DtcSeverity.MEDIUM,
                timestamp = System.currentTimeMillis() - 3600000
            ),
            DiagnosticTroubleCode(
                code = "P0420",
                description = "Catalyst System Efficiency Below Threshold",
                status = DtcStatus.PENDING,
                severity = DtcSeverity.HIGH,
                timestamp = System.currentTimeMillis() - 1800000
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Refresh DTCs */ }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* Scan for DTCs */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Scan DTCs")
                }
                Button(
                    onClick = { /* Clear DTCs */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear DTCs")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DTC List
            Text(
                "Diagnostic Trouble Codes",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (dtcs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No trouble codes found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dtcs) { dtc ->
                        DtcCard(dtc = dtc)
                    }
                }
            }
        }
    }
}

@Composable
private fun DtcCard(dtc: DiagnosticTroubleCode) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (dtc.severity) {
                DtcSeverity.HIGH -> MaterialTheme.colorScheme.errorContainer
                DtcSeverity.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = dtc.code,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Status Chip
                Surface(
                    color = when (dtc.status) {
                        DtcStatus.ACTIVE -> MaterialTheme.colorScheme.error
                        DtcStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = dtc.status.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = dtc.description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Severity: ${dtc.severity.toString()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

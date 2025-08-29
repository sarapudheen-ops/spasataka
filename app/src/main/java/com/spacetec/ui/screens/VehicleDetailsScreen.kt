package com.spacetec.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun VehicleDetailsScreen(
    brand: String,
    model: String,
    year: String,
    navController: NavController = rememberNavController()
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Vehicle Details",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        item {
            VehicleHeaderCard(brand, model, year)
        }
        
        item {
            DiagnosticCapabilitiesCard()
        }
        
        item {
            TechnicalSpecificationsCard(brand, model, year)
        }
        
        item {
            SupportedProtocolsCard()
        }
        
        item {
            CommonIssuesCard(brand, model)
        }
        
        item {
            ActionButtonsCard(navController)
        }
    }
}

@Composable
fun VehicleHeaderCard(brand: String, model: String, year: String) {
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
                Icons.Default.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "$year $brand $model",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Professional Diagnostic Support",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun DiagnosticCapabilitiesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Diagnostic Capabilities",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            val capabilities = listOf(
                "Engine Control Module (ECM)",
                "Transmission Control Module (TCM)",
                "Anti-lock Braking System (ABS)",
                "Airbag Control Module (ACM)",
                "Body Control Module (BCM)",
                "Climate Control System",
                "Instrument Cluster",
                "Power Steering Module"
            )
            
            capabilities.forEach { capability ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        capability,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun TechnicalSpecificationsCard(brand: String, model: String, year: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Engineering,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Technical Specifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            val specs = getVehicleSpecs(brand, model, year.toIntOrNull() ?: 2020)
            specs.forEach { (label, value) ->
                SpecificationRow(label, value)
            }
        }
    }
}

@Composable
fun SupportedProtocolsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Cable,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Supported Protocols",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            val protocols = listOf(
                "CAN (Controller Area Network)" to "High-speed data communication",
                "ISO 15765" to "Diagnostic communication over CAN",
                "J1939" to "Heavy-duty vehicle protocol",
                "KWP2000" to "Keyword Protocol 2000",
                "ISO 9141" to "K-Line communication"
            )
            
            protocols.forEach { (protocol, description) ->
                ProtocolRow(protocol, description)
            }
        }
    }
}

@Composable
fun CommonIssuesCard(brand: String, model: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Common Issues & Solutions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            val issues = getCommonIssues(brand, model)
            issues.forEach { issue ->
                IssueRow(issue)
            }
        }
    }
}

@Composable
fun ActionButtonsCard(navController: NavController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { navController.navigate("engine_diagnostics") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Diagnose")
                }
                
                OutlinedButton(
                    onClick = { navController.navigate("dashboard") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Dashboard, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Monitor")
                }
            }
        }
    }
}

@Composable
fun SpecificationRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ProtocolRow(protocol: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            protocol,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun IssueRow(issue: VehicleIssue) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    when (issue.severity) {
                        "High" -> Icons.Default.Error
                        "Medium" -> Icons.Default.Warning
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = when (issue.severity) {
                        "High" -> MaterialTheme.colorScheme.error
                        "Medium" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    issue.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                issue.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper functions
fun getVehicleSpecs(brand: String, model: String, year: Int): List<Pair<String, String>> {
    return listOf(
        "Engine Type" to when {
            year >= 2020 -> "Hybrid/Turbo"
            year >= 2010 -> "4-Cylinder Turbo"
            else -> "V6"
        },
        "Transmission" to when {
            year >= 2015 -> "CVT/8-Speed Auto"
            year >= 2005 -> "6-Speed Auto"
            else -> "4-Speed Auto"
        },
        "Fuel System" to when {
            year >= 2010 -> "Direct Injection"
            else -> "Multi-Port Injection"
        },
        "OBD Standard" to when {
            year >= 2008 -> "OBD-II CAN"
            year >= 1996 -> "OBD-II"
            else -> "OBD-I"
        },
        "ECU Count" to when {
            year >= 2015 -> "15-25 modules"
            year >= 2005 -> "8-15 modules"
            else -> "3-8 modules"
        }
    )
}

fun getCommonIssues(brand: String, model: String): List<VehicleIssue> {
    return when (brand.lowercase()) {
        "toyota" -> listOf(
            VehicleIssue("Oil Consumption", "Some models may consume oil faster than normal", "Medium"),
            VehicleIssue("Carbon Buildup", "Direct injection engines may develop carbon deposits", "Low"),
            VehicleIssue("Hybrid Battery", "Hybrid models may need battery replacement after 8-10 years", "Medium")
        )
        "honda" -> listOf(
            VehicleIssue("CVT Issues", "Some CVT transmissions may have reliability concerns", "High"),
            VehicleIssue("AC Compressor", "Air conditioning compressor may fail prematurely", "Medium"),
            VehicleIssue("VTC Actuator", "Variable timing control issues in some engines", "Medium")
        )
        "ford" -> listOf(
            VehicleIssue("Transmission", "Some automatic transmissions may have shifting issues", "High"),
            VehicleIssue("Cooling System", "Coolant leaks from plastic components", "Medium"),
            VehicleIssue("Electrical", "Various electrical gremlins in older models", "Low")
        )
        else -> listOf(
            VehicleIssue("General Maintenance", "Follow manufacturer maintenance schedule", "Low"),
            VehicleIssue("Sensor Issues", "Various sensors may need replacement over time", "Medium"),
            VehicleIssue("Wear Items", "Brake pads, filters, and fluids need regular service", "Low")
        )
    }
}

data class VehicleIssue(
    val title: String,
    val description: String,
    val severity: String
)

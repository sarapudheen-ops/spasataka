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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun ProfessionalScreen(navController: NavController) {
    val professionalFeatures = listOf(
        ProfessionalFeature(
            "Service Functions",
            "36+ Professional service procedures including oil reset, TPMS, EPB, DPF regeneration",
            Icons.Filled.Build,
            Color(0xFF2196F3),
            "professional_services"
        ),
        ProfessionalFeature(
            "Bi-Directional Controls",
            "Active testing of vehicle components - injectors, pumps, actuators",
            Icons.Filled.ControlPoint,
            Color(0xFFFF9800),
            "bidirectional_controls"
        ),
        ProfessionalFeature(
            "ECU Coding",
            "Offline ECU coding, component matching, personalization, key programming",
            Icons.Filled.Memory,
            Color(0xFF9C27B0),
            "ecu_coding"
        ),
        ProfessionalFeature(
            "Auto VIN Detection",
            "Automatic vehicle identification with guided repair procedures",
            Icons.Filled.Search,
            Color(0xFF2196F3),
            "auto_vin_detection"
        ),
        ProfessionalFeature(
            "Modern Protocols",
            "CAN-FD and DoIP support for latest vehicle communication",
            Icons.Filled.Cable,
            Color(0xFF607D8B),
            "modern_protocols"
        ),
        ProfessionalFeature(
            "Vehicle Database",
            "Comprehensive offline database covering all major brands and models",
            Icons.Filled.Storage,
            Color(0xFF795548),
            "vehicle_library"
        )
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Professional Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Professional Diagnostics",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Advanced features matching Autel MaxiSys & Launch X431",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        
        LazyColumn {
            items(professionalFeatures) { feature ->
                ProfessionalFeatureCard(feature, navController)
            }
            item {
                ProfessionalFeatureCard(
                    ProfessionalFeature(
                        "Vehicle Database",
                        "Browse offline vehicle library",
                        Icons.Filled.LibraryBooks,
                        Color(0xFF795548),
                        "vehicle_library"
                    ),
                    navController
                )
            }
            item {
                ProfessionalFeatureCard(
                    ProfessionalFeature(
                        "Offline ECUs",
                        "View and manage cached ECU data",
                        Icons.Filled.Storage,
                        Color(0xFF9C27B0),
                        "offline_ecus"
                    ),
                    navController
                )
            }
        }
    }
}

@Composable
fun ProfessionalFeatureCard(feature: ProfessionalFeature, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = feature.title,
                tint = feature.color,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = feature.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = feature.color
                )
                Text(
                    text = feature.description,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            Button(
                onClick = { navController.navigate(feature.route) },
                colors = ButtonDefaults.buttonColors(containerColor = feature.color)
            ) {
                Text("Open", color = Color.White)
            }
        }
    }
}

data class ProfessionalFeature(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val route: String
)

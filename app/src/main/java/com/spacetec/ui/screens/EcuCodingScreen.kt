package com.spacetec.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.spacetec.diagnostic.professional.OfflineEcuCoding
import kotlinx.coroutines.launch

@Composable
fun EcuCodingScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val codingManager = remember {
        OfflineEcuCoding(context, null, null)
    }
    
    val codingStatus by codingManager.codingStatus.collectAsState()
    val currentCoding by codingManager.currentCoding.collectAsState()
    val progress by codingManager.progress.collectAsState()
    
    val codingCategories = listOf(
        "Component Matching" to listOf("Throttle Body", "Fuel Injectors", "Mass Airflow", "Oxygen Sensors"),
        "Personalization" to listOf("Comfort Settings", "Lighting", "Door Locks", "Climate Control"),
        "Retrofit Coding" to listOf("New Modules", "Feature Activation", "Option Coding"),
        "Calibration" to listOf("Steering Angle", "Yaw Rate", "Accelerometer", "Wheel Speed"),
        "Key Programming" to listOf("New Keys", "Remote Controls", "Immobilizer", "Start/Stop")
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF9C27B0))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "ECU Coding & Programming",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Offline coding, component matching & key programming",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
        
        if (codingStatus != OfflineEcuCoding.CodingStatus.IDLE) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Coding Status: ${codingStatus.name}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    currentCoding?.let { coding ->
                        Text(
                            text = "Coding: ${coding.type.name}",
                            fontSize = 14.sp
                        )
                    }
                    if (progress > 0) {
                        LinearProgressIndicator(
                            progress = progress / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    }
                }
            }
        }
        
        LazyColumn {
            items(codingCategories) { (category, functions) ->
                EcuCodingCategoryCard(
                    category = category,
                    functions = functions,
                    onCodingClick = { functionName ->
                        scope.launch {
                            val vehicleInfo = mapOf("make" to "BMW", "model" to "3 Series", "year" to "2021")
                            when (functionName) {
                                "Throttle Body" -> codingManager.executeCoding("component_matching_throttle", vehicleInfo)
                                "New Keys" -> codingManager.executeCoding("key_programming_new", vehicleInfo)
                                "Comfort Settings" -> codingManager.executeCoding("personalization_comfort", vehicleInfo)
                                "Feature Activation" -> codingManager.executeCoding("retrofit_feature_activation", vehicleInfo)
                                else -> codingManager.executeCoding("calibration_generic", vehicleInfo)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun EcuCodingCategoryCard(
    category: String,
    functions: List<String>,
    onCodingClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = category,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9C27B0)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            functions.forEach { function ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = function,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = { onCodingClick(function) },
                        modifier = Modifier.size(width = 80.dp, height = 32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                    ) {
                        Text("Code", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

package com.spacetec.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AstronautHUD(
    speed: Int,
    rpm: Int,
    coolant: Int,
    fuel: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text("👨‍🚀 Astronaut HUD", color = Color.Cyan, fontSize = 20.sp)

        GaugeRow("Warp Speed", "$speed light units")
        GaugeRow("Thruster Power", "$rpm %")
        GaugeRow("Engine Core Temp", "$coolant °C")
        GaugeRow("Oxygen Levels", "$fuel %")
    }
}

@Composable
fun GaugeRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White)
        Text(value, color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
    }
}

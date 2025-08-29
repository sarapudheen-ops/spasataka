package com.spacetec.vehicle.model

/**
 * Vehicle recall information
 */
data class RecallInfo(
    val id: String,
    val title: String,
    val description: String,
    val severity: String = "Medium",
    val affectedVehicles: List<String> = emptyList()
)

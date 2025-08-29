package com.spacetec.vehicle.model

/**
 * Service procedure information
 */
data class ServiceProcedure(
    val id: String,
    val name: String,
    val description: String,
    val steps: List<String> = emptyList(),
    val requiredTools: List<String> = emptyList(),
    val estimatedTime: String = "Unknown",
    val difficulty: String = "Medium"
)

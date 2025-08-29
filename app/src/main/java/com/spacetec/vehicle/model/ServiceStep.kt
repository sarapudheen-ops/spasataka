package com.spacetec.vehicle.model

/**
 * Individual service step
 */
data class ServiceStep(
    val id: String,
    val stepNumber: Int,
    val description: String,
    val estimatedTime: String = "5 min",
    val requiredTools: List<String> = emptyList()
)

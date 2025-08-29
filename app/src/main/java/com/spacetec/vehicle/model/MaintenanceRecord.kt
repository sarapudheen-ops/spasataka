package com.spacetec.vehicle.model

/**
 * Maintenance record information
 */
data class MaintenanceRecord(
    val id: String,
    val date: String,
    val mileage: Int,
    val serviceType: String,
    val description: String,
    val cost: Double = 0.0
)

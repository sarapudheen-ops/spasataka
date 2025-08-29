package com.spacetec.vehicle.library

import kotlinx.serialization.Serializable

/**
 * Data classes for vehicle library system
 */

@Serializable
data class VehicleBrand(
    val brand: String,
    val models: List<VehicleModelInfo>
) {
    fun getAllYears(): List<Int> {
        return models.flatMap { it.years }.distinct().sorted()
    }
    
    fun getModelsByYear(year: Int): List<String> {
        return models.filter { it.years.contains(year) }.map { it.model }
    }
}

@Serializable
data class VehicleModelInfo(
    val model: String,
    val years: List<Int>
) {
    fun isAvailableInYear(year: Int): Boolean = years.contains(year)
    
    fun getYearRange(): Pair<Int, Int>? {
        return if (years.isNotEmpty()) {
            years.minOrNull()!! to years.maxOrNull()!!
        } else null
    }
}

// VehicleSpec is defined in VehicleSpec.kt

/**
 * Vehicle search criteria
 */
data class VehicleSearchCriteria(
    val brand: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val yearRange: Pair<Int, Int>? = null,
    val engineType: String? = null,
    val fuelType: String? = null
)

/**
 * Vehicle library statistics
 */
data class VehicleLibraryStats(
    val totalBrands: Int,
    val totalModels: Int,
    val totalYears: Int,
    val yearRange: Pair<Int, Int>,
    val mostPopularBrand: String?,
    val newestModel: VehicleSpec?,
    val oldestModel: VehicleSpec?
)

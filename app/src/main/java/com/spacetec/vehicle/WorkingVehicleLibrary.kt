package com.spacetec.vehicle

import com.spacetec.vehicle.model.VehicleBrand
import com.spacetec.vehicle.model.VehicleType

/**
 * Working Vehicle Library implementation without complex dependencies
 */
class WorkingVehicleLibrary {
    
    private val sampleBrands = listOf(
        VehicleBrand(
            id = "toyota",
            name = "Toyota",
            country = "Japan",
            yearFounded = 1937,
            vehicleTypes = listOf(VehicleType.PASSENGER, VehicleType.HYBRID, VehicleType.OFFROAD),
            isPremium = false,
            supportedProtocols = listOf("OBD2", "CAN", "ISO15765")
        ),
        VehicleBrand(
            id = "tesla",
            name = "Tesla",
            country = "USA",
            yearFounded = 2003,
            vehicleTypes = listOf(VehicleType.ELECTRIC, VehicleType.LUXURY, VehicleType.AUTONOMOUS),
            isPremium = true,
            supportedProtocols = listOf("CAN", "UDS")
        ),
        VehicleBrand(
            id = "bmw",
            name = "BMW",
            country = "Germany",
            yearFounded = 1916,
            vehicleTypes = listOf(VehicleType.PASSENGER, VehicleType.LUXURY, VehicleType.PERFORMANCE),
            isPremium = true,
            supportedProtocols = listOf("OBD2", "CAN", "UDS", "BMW-ISTA")
        ),
        VehicleBrand(
            id = "ford",
            name = "Ford",
            country = "USA",
            yearFounded = 1903,
            vehicleTypes = listOf(VehicleType.PASSENGER, VehicleType.COMMERCIAL, VehicleType.OFFROAD),
            isPremium = false,
            supportedProtocols = listOf("OBD2", "CAN", "Ford-IDS")
        ),
        VehicleBrand(
            id = "mercedes",
            name = "Mercedes-Benz",
            country = "Germany",
            yearFounded = 1926,
            vehicleTypes = listOf(VehicleType.PASSENGER, VehicleType.LUXURY, VehicleType.COMMERCIAL),
            isPremium = true,
            supportedProtocols = listOf("OBD2", "CAN", "UDS", "Mercedes-DAS")
        ),
        VehicleBrand(
            id = "audi",
            name = "Audi",
            country = "Germany",
            yearFounded = 1909,
            vehicleTypes = listOf(VehicleType.PASSENGER, VehicleType.LUXURY, VehicleType.PERFORMANCE),
            isPremium = true,
            supportedProtocols = listOf("OBD2", "CAN", "UDS", "VAG-COM")
        ),
        VehicleBrand(
            id = "honda",
            name = "Honda",
            country = "Japan",
            yearFounded = 1948,
            vehicleTypes = listOf(VehicleType.PASSENGER, VehicleType.HYBRID, VehicleType.MOTORCYCLE),
            isPremium = false,
            supportedProtocols = listOf("OBD2", "CAN", "Honda-HDS")
        ),
        VehicleBrand(
            id = "nissan",
            name = "Nissan",
            country = "Japan",
            yearFounded = 1933,
            vehicleTypes = listOf(VehicleType.PASSENGER, VehicleType.ELECTRIC, VehicleType.COMMERCIAL),
            isPremium = false,
            supportedProtocols = listOf("OBD2", "CAN", "Nissan-CONSULT")
        ),
        VehicleBrand(
            id = "volkswagen",
            name = "Volkswagen",
            country = "Germany",
            yearFounded = 1937,
            vehicleTypes = listOf(VehicleType.PASSENGER, VehicleType.ELECTRIC, VehicleType.COMMERCIAL),
            isPremium = false,
            supportedProtocols = listOf("OBD2", "CAN", "UDS", "VAG-COM")
        ),
        VehicleBrand(
            id = "porsche",
            name = "Porsche",
            country = "Germany",
            yearFounded = 1931,
            vehicleTypes = listOf(VehicleType.PERFORMANCE, VehicleType.LUXURY, VehicleType.SPORTS),
            isPremium = true,
            supportedProtocols = listOf("OBD2", "CAN", "UDS", "Porsche-PIWIS")
        )
    )
    
    fun getAllBrands(): List<VehicleBrand> = sampleBrands
    
    fun searchBrands(query: String): List<VehicleBrand> {
        if (query.isBlank()) return sampleBrands
        
        return sampleBrands.filter { brand ->
            brand.name.contains(query, ignoreCase = true) ||
            brand.country.contains(query, ignoreCase = true) ||
            brand.vehicleTypes.any { it.name.contains(query, ignoreCase = true) }
        }
    }
    
    fun getBrandsByType(type: VehicleType): List<VehicleBrand> {
        return sampleBrands.filter { brand ->
            brand.vehicleTypes.contains(type)
        }
    }
    
    fun getBrandsByCountry(country: String): List<VehicleBrand> {
        return sampleBrands.filter { brand ->
            brand.country.equals(country, ignoreCase = true)
        }
    }
    
    fun getPremiumBrands(): List<VehicleBrand> {
        return sampleBrands.filter { it.isPremium }
    }
    
    fun getBrandsByProtocol(protocol: String): List<VehicleBrand> {
        return sampleBrands.filter { brand ->
            brand.supportedProtocols.any { it.contains(protocol, ignoreCase = true) }
        }
    }
    
    fun getLibraryStats(): VehicleLibraryStats {
        return VehicleLibraryStats(
            totalBrands = sampleBrands.size,
            totalCountries = sampleBrands.map { it.country }.distinct().size,
            premiumBrands = sampleBrands.count { it.isPremium },
            supportedProtocols = sampleBrands.flatMap { it.supportedProtocols }.distinct().size
        )
    }
}

data class VehicleLibraryStats(
    val totalBrands: Int,
    val totalCountries: Int,
    val premiumBrands: Int,
    val supportedProtocols: Int
)

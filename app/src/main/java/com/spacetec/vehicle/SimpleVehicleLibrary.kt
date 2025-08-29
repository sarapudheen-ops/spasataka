package com.spacetec.vehicle

import com.spacetec.vehicle.model.VehicleBrand
import com.spacetec.vehicle.model.VehicleType

/**
 * Simplified Vehicle Library for basic functionality
 */
object SimpleVehicleLibrary {
    
    fun getAllSupportedBrands(): List<String> {
        return listOf(
            "Toyota", "Honda", "Ford", "Chevrolet", "Nissan", "BMW", "Mercedes-Benz", 
            "Audi", "Volkswagen", "Hyundai", "Kia", "Mazda", "Subaru", "Lexus",
            "Acura", "Infiniti", "Cadillac", "Lincoln", "Buick", "GMC", "Ram",
            "Jeep", "Chrysler", "Dodge", "Mitsubishi", "Volvo", "Jaguar", 
            "Land Rover", "Porsche", "Ferrari", "Lamborghini", "Maserati",
            "Bentley", "Rolls-Royce", "McLaren", "Aston Martin", "Tesla",
            "Rivian", "Lucid", "Polestar", "Genesis", "Alfa Romeo", "Fiat"
        ).sorted()
    }
    
    fun getSampleBrands(): List<VehicleBrand> {
        return listOf(
            VehicleBrand(
                id = "toyota",
                name = "Toyota",
                country = "Japan",
                yearFounded = 1937,
                vehicleTypes = listOf(VehicleType.PASSENGER, VehicleType.HYBRID, VehicleType.OFFROAD),
                isPremium = false
            ),
            VehicleBrand(
                id = "tesla",
                name = "Tesla",
                country = "USA",
                yearFounded = 2003,
                vehicleTypes = listOf(VehicleType.ELECTRIC, VehicleType.LUXURY, VehicleType.AUTONOMOUS),
                isPremium = true
            ),
            VehicleBrand(
                id = "bmw",
                name = "BMW",
                country = "Germany",
                yearFounded = 1916,
                vehicleTypes = listOf(VehicleType.PASSENGER, VehicleType.LUXURY, VehicleType.PERFORMANCE),
                isPremium = true
            ),
            VehicleBrand(
                id = "ford",
                name = "Ford",
                country = "USA",
                yearFounded = 1903,
                vehicleTypes = listOf(VehicleType.PASSENGER, VehicleType.COMMERCIAL, VehicleType.OFFROAD),
                isPremium = false
            ),
            VehicleBrand(
                id = "harley_davidson",
                name = "Harley-Davidson",
                country = "USA",
                yearFounded = 1903,
                vehicleTypes = listOf(VehicleType.MOTORCYCLE, VehicleType.CLASSIC),
                isPremium = true
            )
        )
    }
    
    fun searchBrands(query: String): List<VehicleBrand> {
        if (query.isBlank()) return getSampleBrands()
        
        return getSampleBrands().filter { brand ->
            brand.name.contains(query, ignoreCase = true) ||
            brand.country.contains(query, ignoreCase = true)
        }
    }
    
    fun getBrandsByType(type: VehicleType): List<VehicleBrand> {
        return getSampleBrands().filter { brand ->
            brand.vehicleTypes.contains(type)
        }
    }
}

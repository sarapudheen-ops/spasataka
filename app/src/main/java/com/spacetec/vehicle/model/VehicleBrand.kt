package com.spacetec.vehicle.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a vehicle manufacturer/brand
 */
data class VehicleBrand(
    val id: String,
    val name: String,
    val logoRes: String? = null,
    val country: String,
    val yearFounded: Int,
    val vehicleTypes: List<VehicleType>,
    val isPremium: Boolean = false,
    val supportedProtocols: List<String> = listOf("OBD2"),
    val models: List<VehicleModel> = emptyList()
) {
    val icon: ImageVector
        get() = when {
            vehicleTypes.any { it == VehicleType.ELECTRIC || it == VehicleType.HYDROGEN } -> Icons.Default.ElectricCar
            vehicleTypes.contains(VehicleType.MOTORCYCLE) -> Icons.Default.TwoWheeler
            vehicleTypes.contains(VehicleType.COMMERCIAL) -> Icons.Default.LocalShipping
            vehicleTypes.contains(VehicleType.ATV) || vehicleTypes.contains(VehicleType.UTV) -> Icons.Default.DirectionsCar
            vehicleTypes.contains(VehicleType.SNOWMOBILE) -> Icons.Default.AcUnit
            vehicleTypes.contains(VehicleType.WATER) -> Icons.Default.DirectionsCar
            vehicleTypes.contains(VehicleType.AGRICULTURAL) -> Icons.Default.DirectionsCar
            vehicleTypes.contains(VehicleType.CONSTRUCTION) -> Icons.Default.Build
            vehicleTypes.contains(VehicleType.MILITARY) -> Icons.Default.Security
            vehicleTypes.contains(VehicleType.EMERGENCY) -> Icons.Default.LocalHospital
            vehicleTypes.contains(VehicleType.SPORTS) || vehicleTypes.contains(VehicleType.PERFORMANCE) -> Icons.Default.DirectionsCar
            vehicleTypes.contains(VehicleType.OFFROAD) -> Icons.Default.DirectionsCar
            vehicleTypes.contains(VehicleType.KART) -> Icons.Default.DirectionsCar
            vehicleTypes.contains(VehicleType.CLASSIC) -> Icons.Default.Star
            vehicleTypes.contains(VehicleType.AUTONOMOUS) -> Icons.Default.SmartToy
            vehicleTypes.contains(VehicleType.FLYING) -> Icons.Default.Flight
            vehicleTypes.contains(VehicleType.KIT_CAR) || vehicleTypes.contains(VehicleType.PROTOTYPE) -> Icons.Default.Build
            else -> Icons.Default.DirectionsCar
        }
}

enum class VehicleType {
    // Main Categories
    PASSENGER,      // Standard passenger cars
    COMMERCIAL,     // Vans, trucks, and commercial vehicles
    MOTORCYCLE,     // All types of motorcycles and scooters
    ELECTRIC,       // Fully electric vehicles
    HYBRID,         // Hybrid electric vehicles (HEV)
    PLUGIN_HYBRID,  // Plug-in hybrid electric vehicles (PHEV)
    LUXURY,         // Luxury vehicles
    PERFORMANCE,    // High-performance vehicles
    
    // Specialized Types
    OFFROAD,        // 4x4, SUVs, and off-road vehicles
    SPORTS,         // Sports cars and supercars
    CLASSIC,        // Classic and vintage vehicles
    KART,           // Go-karts and racing karts
    ATV,            // All-terrain vehicles (quads)
    UTV,            // Utility terrain vehicles (side-by-sides)
    SNOWMOBILE,     // Snowmobiles and snow vehicles
    WATER,          // Marine vehicles (boats, jet skis)
    AGRICULTURAL,   // Tractors and agricultural machinery
    CONSTRUCTION,   // Construction and heavy equipment
    MILITARY,       // Military vehicles
    EMERGENCY,      // Emergency and service vehicles
    
    // Future-Proofing
    AUTONOMOUS,     // Self-driving/autonomous vehicles
    HYDROGEN,       // Hydrogen fuel cell vehicles
    FLYING,         // eVTOL and flying vehicles
    
    // Other
    KIT_CAR,        // Kit cars and custom builds
    PROTOTYPE,      // Prototype and concept vehicles
    OTHER           // Any other vehicle type not listed
}

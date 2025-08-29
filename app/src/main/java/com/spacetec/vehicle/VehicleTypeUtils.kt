package com.spacetec.vehicle

import com.spacetec.vehicle.model.VehicleBrand
import com.spacetec.vehicle.model.VehicleType

/**
 * Utility class for vehicle type categorization and sample data
 */
object VehicleTypeUtils {
    
    /**
     * Get a list of vehicle type categories for filtering
     */
    fun getVehicleCategories(): List<Pair<String, List<VehicleType>>> = listOf(
        "Main Types" to listOf(
            VehicleType.PASSENGER,
            VehicleType.COMMERCIAL,
            VehicleType.MOTORCYCLE,
            VehicleType.ELECTRIC,
            VehicleType.HYBRID,
            VehicleType.PLUGIN_HYBRID
        ),
        "Specialty" to listOf(
            VehicleType.LUXURY,
            VehicleType.PERFORMANCE,
            VehicleType.OFFROAD,
            VehicleType.SPORTS,
            VehicleType.CLASSIC
        ),
        "Recreational" to listOf(
            VehicleType.KART,
            VehicleType.ATV,
            VehicleType.UTV,
            VehicleType.SNOWMOBILE,
            VehicleType.WATER
        ),
        "Industrial" to listOf(
            VehicleType.AGRICULTURAL,
            VehicleType.CONSTRUCTION,
            VehicleType.MILITARY,
            VehicleType.EMERGENCY
        ),
        "Future" to listOf(
            VehicleType.AUTONOMOUS,
            VehicleType.HYDROGEN,
            VehicleType.FLYING
        ),
        "Other" to listOf(
            VehicleType.KIT_CAR,
            VehicleType.PROTOTYPE,
            VehicleType.OTHER
        )
    )

    /**
     * Create sample vehicle brands with appropriate types
     */
    fun createSampleBrands(): List<VehicleBrand> = listOf(
        // Passenger Vehicles
        createBrand("Toyota", listOf(VehicleType.PASSENGER, VehicleType.HYBRID, VehicleType.OFFROAD)),
        createBrand("Tesla", listOf(VehicleType.ELECTRIC, VehicleType.LUXURY, VehicleType.AUTONOMOUS)),
        
        // Luxury & Performance
        createBrand("Mercedes-Benz", listOf(VehicleType.LUXURY, VehicleType.PERFORMANCE, VehicleType.COMMERCIAL)),
        createBrand("Porsche", listOf(VehicleType.PERFORMANCE, VehicleType.LUXURY, VehicleType.ELECTRIC)),
        
        // Motorcycles
        createBrand("Harley-Davidson", listOf(VehicleType.MOTORCYCLE, VehicleType.CLASSIC)),
        createBrand("Ducati", listOf(VehicleType.MOTORCYCLE, VehicleType.PERFORMANCE)),
        
        // Commercial
        createBrand("Volvo Trucks", listOf(VehicleType.COMMERCIAL, VehicleType.CONSTRUCTION)),
        createBrand("Mercedes-Benz Vans", listOf(VehicleType.COMMERCIAL, VehicleType.EMERGENCY)),
        
        // Recreational
        createBrand("Polaris", listOf(VehicleType.ATV, VehicleType.UTV, VehicleType.SNOWMOBILE)),
        createBrand("Sea-Doo", listOf(VehicleType.WATER)),
        
        // Industrial
        createBrand("John Deere", listOf(VehicleType.AGRICULTURAL, VehicleType.CONSTRUCTION)),
        createBrand("Caterpillar", listOf(VehicleType.CONSTRUCTION)),
        
        // Future
        createBrand("Rivian", listOf(VehicleType.ELECTRIC, VehicleType.OFFROAD, VehicleType.AUTONOMOUS)),
        createBrand("Lucid", listOf(VehicleType.ELECTRIC, VehicleType.LUXURY))
    )

    private fun createBrand(
        name: String, 
        types: List<VehicleType>,
        country: String = ""
    ): VehicleBrand {
        val id = name.lowercase().replace(" ", "_")
        return VehicleBrand(
            id = id,
            name = name,
            country = country,
            yearFounded = 1900, // Default, should be set properly
            vehicleTypes = types.distinct(),
            isPremium = types.any { it in listOf(VehicleType.LUXURY, VehicleType.PERFORMANCE) },
            supportedProtocols = getDefaultProtocolsForTypes(types)
        )
    }

    private fun getDefaultProtocolsForTypes(types: List<VehicleType>): List<String> {
        val protocols = mutableListOf("OBD2")
        
        if (types.contains(VehicleType.ELECTRIC) || 
            types.contains(VehicleType.HYBRID) || 
            types.contains(VehicleType.PLUGIN_HYBRID)) {
            protocols.add("UDS")
        }
        
        if (types.any { it in listOf(
                VehicleType.COMMERCIAL, 
                VehicleType.AGRICULTURAL, 
                VehicleType.CONSTRUCTION) }) {
            protocols.add("J1939")
        }
        
        if (types.any { it in listOf(
                VehicleType.MOTORCYCLE,
                VehicleType.ATV,
                VehicleType.UTV) }) {
            protocols.add("ISO 14230")
        }
        
        return protocols.distinct()
    }
}

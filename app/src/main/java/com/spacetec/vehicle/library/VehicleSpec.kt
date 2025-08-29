package com.spacetec.vehicle.library


/**
 * Comprehensive vehicle specification data class
 */
data class VehicleSpec(
    // Basic Identification
    val id: String,
    val vin: String? = null,
    val make: String,
    val model: String,
    val year: Int,
    val trim: String = "",
    val generation: String = "",
    
    // Engine Information
    val engineCode: String = "",
    val engineType: String = "",  // e.g., "I4", "V6", "Electric"
    val displacement: Double = 0.0, // in liters
    val fuelType: String = "",     // e.g., "Gasoline", "Diesel", "Hybrid"
    val powerHP: Int? = null,
    val powerKW: Int? = null,
    val torqueNM: Int? = null,
    val transmission: String = "",  // e.g., "6-Speed Automatic"
    val driveType: String = "",     // e.g., "FWD", "AWD", "RWD"
    
    // OBD/Diagnostic Information
    val obdProtocol: String = "OBD2",  // e.g., "ISO 15765-4 (CAN)"
    val supportedPIDs: List<String> = emptyList(),
    val ecuList: List<com.spacetec.vehicle.model.EcuInfo> = emptyList(),
    
    // Vehicle Dimensions
    val length: Int? = null,  // in mm
    val width: Int? = null,
    val height: Int? = null,
    val wheelbase: Int? = null,
    val weight: Int? = null,  // in kg
    
    // Maintenance Information
    val serviceIntervals: Map<String, Int> = emptyMap(), // km/mile intervals
    val commonIssues: List<CommonIssue> = emptyList(),
    
    // Additional Metadata
    val market: String = "",  // e.g., "US", "EU", "JP"
    val bodyType: String = "", // e.g., "Sedan", "SUV"
    val imageUrl: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = "$year $make $model" + if (trim.isNotEmpty()) " $trim" else ""
}

// EcuInfo class removed to avoid duplicates - using the one in VehicleSpecDetails.kt

data class CommonIssue(
    val code: String = "",      // e.g., "P0300"
    val description: String,
    val symptoms: List<String> = emptyList(),
    val possibleCauses: List<String> = emptyList(),
    val solutions: List<String> = emptyList(),
    val severity: Severity = Severity.MEDIUM,
    val frequency: Int = 1  // 1-5, 5 being most common
)

enum class Severity {
    LOW,        // Non-critical issue
    MEDIUM,     // Should be addressed
    HIGH,       // Critical issue, should be fixed immediately
    SAFETY      // Safety-related issue, do not drive
}

// Extension function to get vehicle age
fun VehicleSpec.getAge(currentYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)): Int {
    return currentYear - year
}

// Extension function to check if vehicle is electric
fun VehicleSpec.isElectric(): Boolean {
    return fuelType.equals("electric", ignoreCase = true) || 
           fuelType.equals("ev", ignoreCase = true)
}

// Extension function to get compatible protocols
fun VehicleSpec.getCompatibleProtocols(): List<String> {
    return when (year) {
        in 1996..2003 -> listOf("OBD2", "ISO 9141-2", "ISO 14230-4 (KWP2000)")
        in 2004..2007 -> listOf("ISO 15765-4 (CAN)", "ISO 14230-4 (KWP2000)")
        else -> listOf("ISO 15765-4 (CAN)", "UDS")
    }
}

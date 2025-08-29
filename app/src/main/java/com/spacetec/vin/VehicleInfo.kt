package com.spacetec.vin

/**
 * Data class containing comprehensive vehicle information decoded from VIN
 */
data class VehicleInfo(
    val vin: String,
    val worldManufacturerIdentifier: String,
    val vehicleDescriptorSection: String,
    val vehicleIdentifierSection: String,
    val manufacturer: String,
    val country: String,
    val modelYear: Int,
    val assemblyPlant: String,
    val serialNumber: String,
    val engineType: String,
    val bodyStyle: String,
    val driveType: String,
    val restraintSystem: String
) {
    /**
     * Get formatted vehicle description
     */
    fun getVehicleDescription(): String {
        return "$modelYear $manufacturer $bodyStyle"
    }
    
    /**
     * Get comprehensive vehicle summary
     */
    fun getDetailedSummary(): String {
        return buildString {
            appendLine("🚗 $modelYear $manufacturer")
            appendLine("🏭 Made in: $country")
            appendLine("🔧 Engine: $engineType")
            appendLine("🚙 Body: $bodyStyle")
            appendLine("⚙️ Drive: $driveType")
            appendLine("🛡️ Safety: $restraintSystem")
            appendLine("🏢 Plant: $assemblyPlant")
            appendLine("🔢 Serial: $serialNumber")
        }
    }
    
    /**
     * Get vehicle age in years
     */
    fun getVehicleAge(): Int {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return currentYear - modelYear
    }
    
    /**
     * Check if vehicle is considered vintage (25+ years old)
     */
    fun isVintage(): Boolean = getVehicleAge() >= 25
    
    /**
     * Check if vehicle is considered classic (20+ years old)
     */
    fun isClassic(): Boolean = getVehicleAge() >= 20
    
    /**
     * Get space-themed vehicle classification
     */
    fun getSpaceClassification(): String {
        return when {
            isVintage() -> "🛸 Vintage Spacecraft"
            isClassic() -> "🚀 Classic Starship"
            getVehicleAge() >= 10 -> "🛰️ Seasoned Vessel"
            getVehicleAge() >= 5 -> "⭐ Mature Cruiser"
            else -> "🌟 Modern Spacecraft"
        }
    }
}

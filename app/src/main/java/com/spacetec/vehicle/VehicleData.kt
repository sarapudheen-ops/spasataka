package com.spacetec.vehicle

import com.spacetec.vehicle.model.*
import java.time.Year

/**
 * Comprehensive vehicle data model combining real-time OBD data with vehicle specifications
 */
data class VehicleData(
    // Real-time OBD parameters
    val speed: Int = 0,              // Current speed in km/h
    val rpm: Int = 0,                // Engine RPM (0-8000)
    val coolantTemp: Int = 0,        // Coolant temperature in °C
    val fuelLevel: Int = 0,          // Fuel level percentage (0-100%)
    val throttlePosition: Int = 0,   // Throttle position percentage (0-100%)
    val engineLoad: Int = 0,         // Calculated engine load percentage (0-100%)
    val intakeAirTemp: Int = 0,      // Intake air temperature in °C
    val mafRate: Float = 0f,         // Mass Air Flow rate in g/s
    val ambientAirTemp: Int = 0,     // Ambient air temperature in °C
    val barometricPressure: Float = 0f, // Barometric pressure in kPa
    val controlModuleVoltage: Float = 0f, // Control module voltage in V
    val timingAdvance: Float = 0f,   // Ignition timing advance in degrees
    val runTime: Int = 0,            // Time since engine start in seconds
    val distanceWithMIL: Int = 0,    // Distance traveled with MIL on in km
    val fuelRailPressure: Float = 0f, // Fuel rail pressure in kPa
    val fuelType: String = "",       // Type of fuel (e.g., "Gasoline", "Diesel")
    val odometer: Int = 0,           // Vehicle odometer reading in km
    val vin: String = "",            // Vehicle Identification Number
    val dtcCount: Int = 0,           // Number of stored Diagnostic Trouble Codes
    val isEngineRunning: Boolean = false,
    val warpSpeed: Int = 0,          // Warp speed (for UI display)
    val oxygenLevels: Int = 100,     // Oxygen levels (for UI display)
    val timestamp: Long = System.currentTimeMillis(),
    
    // Vehicle identification and specifications
    val vehicleInfo: VehicleInfo? = null,
    val dtcList: List<DiagnosticTroubleCode> = emptyList(),
    val freezeFrameData: Map<String, String> = emptyMap(),
    val supportedPIDs: List<String> = emptyList(),
    val ecuInfo: List<EcuInfo> = emptyList()
) {
    // UI compatibility aliases
    val thrusterPower: Int get() = rpm
    val engineCoreTemp: Int get() = coolantTemp
    /**
     * Check if any critical thresholds are exceeded
     */
    fun hasCriticalAlerts(): Boolean {
        return coolantTemp > 110 ||      // Overheating
               fuelLevel < 10 ||         // Low fuel
               rpm > 7000 ||             // Over-revving
               controlModuleVoltage < 11f ||  // Low system voltage
               dtcCount > 0              // Any DTCs present
    }
    
    /**
     * Get vehicle status message
     */
    fun getStatusMessage(): String {
        return when {
            !isEngineRunning -> "🔋 Ignition Off"
            hasCriticalAlerts() -> when {
                coolantTemp > 110 -> "⚠️ Engine Overheating!"
                fuelLevel < 10 -> "⛽ Low Fuel"
                rpm > 7000 -> "⚠️ High RPM!"
                dtcCount > 0 -> "⚠️ $dtcCount DTCs Detected"
                else -> "⚠️ Check Vehicle"
            }
            speed > 120 -> "🚀 High Speed"
            rpm > 3000 -> "⚡ High RPM"
            else -> "✅ All Systems Normal"
        }
    }

    /**
     * Check if the vehicle is in a safe operating state
     */
    fun isSafeToDrive(): Boolean {
        return isEngineRunning && 
               coolantTemp in 80..100 && 
               controlModuleVoltage in 12f..15f &&
               dtcCount == 0
    }
}

/**
 * Vehicle identification and specification data
 */
data class VehicleInfo(
    val make: String,
    val model: String,
    val year: Int,
    val engineCode: String,
    val engineSize: String = "2.0L",
    val transmissionType: String,
    val fuelType: String,
    val vin: String,
    val ecuInfo: List<EcuInfo> = emptyList()
)

/**
 * Diagnostic Trouble Code (DTC) information
 */
data class DiagnosticTroubleCode(
    val code: String,
    val description: String,
    val status: DtcStatus,
    val severity: DtcSeverity,
    val timestamp: Long = System.currentTimeMillis()
)

enum class DtcStatus {
    ACTIVE,                            // Currently present
    PENDING,                          // Detected but not yet confirmed
    STORED,                           // Stored in memory but not currently present
    PERMANENT                         // Permanent DTC that cannot be cleared by scanner
}

enum class DtcSeverity {
    LOW,                              // Non-emission related
    MEDIUM,                           // Emission related but not critical
    HIGH,                             // Emission related and critical
    SAFETY                            // Safety critical
}

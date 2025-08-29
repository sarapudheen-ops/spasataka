package com.spacetec.obd

/**
 * Space-themed HUD utility for PID information and critical value detection
 */
object SpaceHud {
    
    /**
     * Get space-themed display information for a PID
     */
    fun getSpacePidInfo(pidName: String): SpacePidInfo {
        return when (pidName) {
            "0C" -> SpacePidInfo(
                displayName = "Thruster Power",
                icon = "🚀",
                description = "Engine RPM"
            )
            "0D" -> SpacePidInfo(
                displayName = "Warp Speed",
                icon = "⚡",
                description = "Vehicle Speed"
            )
            "05" -> SpacePidInfo(
                displayName = "Core Temperature",
                icon = "🌡️",
                description = "Engine Coolant Temperature"
            )
            "2F" -> SpacePidInfo(
                displayName = "Fuel Reserves",
                icon = "⛽",
                description = "Fuel Tank Level"
            )
            "0F" -> SpacePidInfo(
                displayName = "Intake Air Temp",
                icon = "🌬️",
                description = "Intake Air Temperature"
            )
            "04" -> SpacePidInfo(
                displayName = "Engine Load",
                icon = "⚙️",
                description = "Calculated Engine Load"
            )
            "11" -> SpacePidInfo(
                displayName = "Throttle Position",
                icon = "🎚️",
                description = "Throttle Position Sensor"
            )
            "0A" -> SpacePidInfo(
                displayName = "Fuel Pressure",
                icon = "💨",
                description = "Fuel Rail Pressure"
            )
            "0B" -> SpacePidInfo(
                displayName = "Manifold Pressure",
                icon = "🔧",
                description = "Intake Manifold Pressure"
            )
            else -> SpacePidInfo(
                displayName = "Unknown Signal",
                icon = "❓",
                description = "Unknown PID: $pidName"
            )
        }
    }
    
    /**
     * Check if a value is critical for the given PID
     */
    fun isValueCritical(pidName: String, value: Double): Boolean {
        return when (pidName) {
            "0C" -> value > 6000 || value < 0 // RPM too high or invalid
            "0D" -> value > 200 // Speed too high (km/h)
            "05" -> value > 110 || value < -40 // Coolant temp too high or too low
            "2F" -> value < 10 // Fuel level too low (%)
            "0F" -> value > 80 || value < -40 // Intake air temp extreme
            "04" -> value > 95 // Engine load too high (%)
            "11" -> value > 100 || value < 0 // Invalid throttle position
            "0A" -> value > 1000 || value < 0 // Fuel pressure extreme
            "0B" -> value > 255 || value < 0 // Manifold pressure extreme
            else -> false
        }
    }
    
    /**
     * Get warning threshold for a PID
     */
    fun getWarningThreshold(pidName: String): Double? {
        return when (pidName) {
            "0C" -> 5000.0 // RPM warning at 5000
            "0D" -> 120.0 // Speed warning at 120 km/h
            "05" -> 95.0 // Coolant temp warning at 95°C
            "2F" -> 20.0 // Fuel warning at 20%
            "0F" -> 60.0 // Intake air temp warning at 60°C
            "04" -> 85.0 // Engine load warning at 85%
            "11" -> 90.0 // Throttle position warning at 90%
            "0A" -> 800.0 // Fuel pressure warning
            "0B" -> 200.0 // Manifold pressure warning
            else -> null
        }
    }
    
    /**
     * Get critical threshold for a PID
     */
    fun getCriticalThreshold(pidName: String): Double? {
        return when (pidName) {
            "0C" -> 6000.0 // RPM critical at 6000
            "0D" -> 200.0 // Speed critical at 200 km/h
            "05" -> 110.0 // Coolant temp critical at 110°C
            "2F" -> 10.0 // Fuel critical at 10%
            "0F" -> 80.0 // Intake air temp critical at 80°C
            "04" -> 95.0 // Engine load critical at 95%
            "11" -> 100.0 // Throttle position critical at 100%
            "0A" -> 1000.0 // Fuel pressure critical
            "0B" -> 255.0 // Manifold pressure critical
            else -> null
        }
    }
}

/**
 * Space-themed PID information
 */
data class SpacePidInfo(
    val displayName: String,
    val icon: String,
    val description: String
)

package com.spacetec.diagnostic.obd

object SpaceHud {
    
    data class SpacePidInfo(
        val displayName: String,
        val icon: String,
        val category: String,
        val criticalThreshold: Double? = null
    )
    
    private val spacePidMap = mapOf(
        // Engine Performance
        "Engine RPM" to SpacePidInfo("Thruster Power", "🚀", "Propulsion"),
        "Vehicle Speed" to SpacePidInfo("Warp Velocity", "⚡", "Navigation"),
        "Throttle Position" to SpacePidInfo("Thrust Control", "🎚️", "Propulsion"),
        "Engine Load" to SpacePidInfo("Core Load", "⚙️", "Propulsion"),
        
        // Temperature Systems
        "Coolant Temperature" to SpacePidInfo("Reactor Core Temp", "🌡️", "Thermal", 105.0),
        "Intake Air Temperature" to SpacePidInfo("Intake Manifold Temp", "❄️", "Thermal"),
        "Oil Temperature" to SpacePidInfo("Lubricant Temp", "🛢️", "Thermal"),
        
        // Fuel & Air Systems
        "Fuel Level" to SpacePidInfo("Energy Reserves", "⛽", "Resources", 15.0),
        "Fuel Pressure" to SpacePidInfo("Fuel Injection Pressure", "💧", "Resources"),
        "Air Flow Rate" to SpacePidInfo("Atmospheric Intake", "💨", "Intake"),
        "Oxygen Sensor" to SpacePidInfo("O2 Sensor Array", "🫁", "Emissions"),
        
        // Electrical Systems
        "Battery Voltage" to SpacePidInfo("Power Cell Voltage", "🔋", "Electrical", 11.5),
        "Alternator" to SpacePidInfo("Generator Output", "⚡", "Electrical"),
        
        // Pressure Systems
        "Manifold Pressure" to SpacePidInfo("Intake Manifold Pressure", "📊", "Intake"),
        "Barometric Pressure" to SpacePidInfo("Atmospheric Pressure", "🌍", "Environmental"),
        
        // Emissions & Diagnostics
        "Catalyst Temperature" to SpacePidInfo("Emission Control Temp", "🧪", "Emissions"),
        "EGR" to SpacePidInfo("Exhaust Recirculation", "♻️", "Emissions"),
        "Timing Advance" to SpacePidInfo("Ignition Timing", "⏰", "Ignition")
    )
    
    fun getSpacePidInfo(pidName: String): SpacePidInfo {
        // Try exact match first
        spacePidMap[pidName]?.let { return it }
        
        // Try partial matches
        spacePidMap.entries.find { (key, _) -> 
            pidName.contains(key, ignoreCase = true) || key.contains(pidName, ignoreCase = true)
        }?.value?.let { return it }
        
        // Default fallback with smart categorization
        val category = when {
            pidName.contains("temp", true) -> "Thermal"
            pidName.contains("pressure", true) -> "Pressure"
            pidName.contains("voltage", true) || pidName.contains("current", true) -> "Electrical"
            pidName.contains("fuel", true) || pidName.contains("oil", true) -> "Resources"
            pidName.contains("speed", true) || pidName.contains("rpm", true) -> "Performance"
            pidName.contains("sensor", true) -> "Sensors"
            else -> "System"
        }
        
        val icon = when (category) {
            "Thermal" -> "🌡️"
            "Pressure" -> "📊"
            "Electrical" -> "⚡"
            "Resources" -> "⛽"
            "Performance" -> "🚀"
            "Sensors" -> "📡"
            else -> "⚙️"
        }
        
        return SpacePidInfo(pidName, icon, category)
    }
    
    fun fancyLabel(name: String, unit: String?): String {
        val spaceInfo = getSpacePidInfo(name)
        return "${spaceInfo.icon} ${spaceInfo.displayName}" + (unit?.let { " ($it)" } ?: "")
    }
    
    fun getCategoryColor(category: String): String = when (category) {
        "Propulsion" -> "@color/icon_live_data"
        "Thermal" -> "@color/warning"
        "Resources" -> "@color/success"
        "Electrical" -> "@color/icon_bluetooth"
        "Emissions" -> "@color/icon_diagnostics"
        "Navigation" -> "@color/md_theme_tertiary"
        else -> "@color/icon_default"
    }
    
    fun isValueCritical(pidName: String, value: Double): Boolean {
        val info = getSpacePidInfo(pidName)
        return info.criticalThreshold?.let { threshold ->
            when (info.category) {
                "Thermal" -> value > threshold
                "Resources" -> value < threshold
                "Electrical" -> value < threshold
                else -> false
            }
        } ?: false
    }
}

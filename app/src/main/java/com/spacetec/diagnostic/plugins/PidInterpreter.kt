package com.spacetec.diagnostic.plugins

import android.util.Log

/**
 * Interface for PID interpretation plugins
 */
interface PidInterpreter {
    fun canHandle(pid: String): Boolean
    fun interpret(pid: String, rawData: ByteArray): InterpretedValue
    fun getDisplayInfo(): PidDisplayInfo
}

/**
 * Manages PID interpreter plugins
 */
class PluginManager {
    private val interpreters = mutableListOf<PidInterpreter>()
    
    companion object {
        private const val TAG = "PluginManager"
    }
    
    /**
     * Register a new PID interpreter
     */
    fun registerInterpreter(interpreter: PidInterpreter) {
        interpreters.add(interpreter)
        Log.i(TAG, "Registered interpreter: ${interpreter.getDisplayInfo().name}")
    }
    
    /**
     * Unregister a PID interpreter
     */
    fun unregisterInterpreter(interpreter: PidInterpreter) {
        interpreters.remove(interpreter)
        Log.i(TAG, "Unregistered interpreter: ${interpreter.getDisplayInfo().name}")
    }
    
    /**
     * Interpret a PID using registered interpreters
     */
    fun interpretPid(pid: String, rawData: ByteArray): InterpretedValue? {
        return interpreters.firstOrNull { it.canHandle(pid) }
            ?.interpret(pid, rawData)
    }
    
    /**
     * Get all registered interpreters
     */
    fun getRegisteredInterpreters(): List<PidInterpreter> = interpreters.toList()
    
    /**
     * Get interpreters that can handle a specific PID
     */
    fun getInterpretersForPid(pid: String): List<PidInterpreter> {
        return interpreters.filter { it.canHandle(pid) }
    }
    
    /**
     * Clear all registered interpreters
     */
    fun clearInterpreters() {
        interpreters.clear()
        Log.i(TAG, "Cleared all interpreters")
    }
}

/**
 * Interpreted value result
 */
data class InterpretedValue(
    val pid: String,
    val value: Double,
    val unit: String,
    val formattedValue: String,
    val isValid: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Display information for PID interpreter
 */
data class PidDisplayInfo(
    val name: String,
    val description: String,
    val supportedPids: List<String>,
    val version: String = "1.0"
)

/**
 * Standard OBD-II PID interpreter
 */
class StandardObdInterpreter : PidInterpreter {
    
    override fun canHandle(pid: String): Boolean {
        return pid.uppercase() in supportedPids
    }
    
    override fun interpret(pid: String, rawData: ByteArray): InterpretedValue {
        val pidUpper = pid.uppercase()
        
        return when (pidUpper) {
            "0C" -> interpretRpm(rawData)
            "0D" -> interpretSpeed(rawData)
            "05" -> interpretCoolantTemp(rawData)
            "2F" -> interpretFuelLevel(rawData)
            "11" -> interpretThrottlePosition(rawData)
            "0F" -> interpretIntakeAirTemp(rawData)
            "04" -> interpretEngineLoad(rawData)
            "06" -> interpretShortTermFuelTrim(rawData)
            "07" -> interpretLongTermFuelTrim(rawData)
            "0A" -> interpretFuelPressure(rawData)
            "0B" -> interpretIntakeManifoldPressure(rawData)
            else -> InterpretedValue(pid, 0.0, "", "Unknown PID", false)
        }
    }
    
    override fun getDisplayInfo(): PidDisplayInfo {
        return PidDisplayInfo(
            name = "Standard OBD-II Interpreter",
            description = "Interprets standard OBD-II PIDs",
            supportedPids = supportedPids
        )
    }
    
    private val supportedPids = listOf(
        "0C", "0D", "05", "2F", "11", "0F", "04", "06", "07", "0A", "0B"
    )
    
    private fun interpretRpm(data: ByteArray): InterpretedValue {
        if (data.size < 4) return InterpretedValue("0C", 0.0, "RPM", "Invalid", false)
        
        val a = data[2].toInt() and 0xFF
        val b = data[3].toInt() and 0xFF
        val rpm = ((a * 256) + b) / 4.0
        
        return InterpretedValue("0C", rpm, "RPM", "${rpm.toInt()} RPM")
    }
    
    private fun interpretSpeed(data: ByteArray): InterpretedValue {
        if (data.size < 3) return InterpretedValue("0D", 0.0, "km/h", "Invalid", false)
        
        val speed = (data[2].toInt() and 0xFF).toDouble()
        return InterpretedValue("0D", speed, "km/h", "${speed.toInt()} km/h")
    }
    
    private fun interpretCoolantTemp(data: ByteArray): InterpretedValue {
        if (data.size < 3) return InterpretedValue("05", 0.0, "°C", "Invalid", false)
        
        val temp = (data[2].toInt() and 0xFF) - 40.0
        return InterpretedValue("05", temp, "°C", "${temp.toInt()}°C")
    }
    
    private fun interpretFuelLevel(data: ByteArray): InterpretedValue {
        if (data.size < 3) return InterpretedValue("2F", 0.0, "%", "Invalid", false)
        
        val level = ((data[2].toInt() and 0xFF) * 100.0) / 255.0
        return InterpretedValue("2F", level, "%", "${level.toInt()}%")
    }
    
    private fun interpretThrottlePosition(data: ByteArray): InterpretedValue {
        if (data.size < 3) return InterpretedValue("11", 0.0, "%", "Invalid", false)
        
        val position = ((data[2].toInt() and 0xFF) * 100.0) / 255.0
        return InterpretedValue("11", position, "%", "${position.toInt()}%")
    }
    
    private fun interpretIntakeAirTemp(data: ByteArray): InterpretedValue {
        if (data.size < 3) return InterpretedValue("0F", 0.0, "°C", "Invalid", false)
        
        val temp = (data[2].toInt() and 0xFF) - 40.0
        return InterpretedValue("0F", temp, "°C", "${temp.toInt()}°C")
    }
    
    private fun interpretEngineLoad(data: ByteArray): InterpretedValue {
        if (data.size < 3) return InterpretedValue("04", 0.0, "%", "Invalid", false)
        
        val load = ((data[2].toInt() and 0xFF) * 100.0) / 255.0
        return InterpretedValue("04", load, "%", "${load.toInt()}%")
    }
    
    private fun interpretShortTermFuelTrim(data: ByteArray): InterpretedValue {
        if (data.size < 3) return InterpretedValue("06", 0.0, "%", "Invalid", false)
        
        val trim = ((data[2].toInt() and 0xFF) - 128.0) * 100.0 / 128.0
        return InterpretedValue("06", trim, "%", "${trim.toInt()}%")
    }
    
    private fun interpretLongTermFuelTrim(data: ByteArray): InterpretedValue {
        if (data.size < 3) return InterpretedValue("07", 0.0, "%", "Invalid", false)
        
        val trim = ((data[2].toInt() and 0xFF) - 128.0) * 100.0 / 128.0
        return InterpretedValue("07", trim, "%", "${trim.toInt()}%")
    }
    
    private fun interpretFuelPressure(data: ByteArray): InterpretedValue {
        if (data.size < 3) return InterpretedValue("0A", 0.0, "kPa", "Invalid", false)
        
        val pressure = (data[2].toInt() and 0xFF) * 3.0
        return InterpretedValue("0A", pressure, "kPa", "${pressure.toInt()} kPa")
    }
    
    private fun interpretIntakeManifoldPressure(data: ByteArray): InterpretedValue {
        if (data.size < 3) return InterpretedValue("0B", 0.0, "kPa", "Invalid", false)
        
        val pressure = (data[2].toInt() and 0xFF).toDouble()
        return InterpretedValue("0B", pressure, "kPa", "${pressure.toInt()} kPa")
    }
}

/**
 * Enhanced PID interpreter for manufacturer-specific PIDs
 */
class EnhancedPidInterpreter : PidInterpreter {
    
    override fun canHandle(pid: String): Boolean {
        return pid.uppercase() in supportedPids
    }
    
    override fun interpret(pid: String, rawData: ByteArray): InterpretedValue {
        val pidUpper = pid.uppercase()
        
        return when (pidUpper) {
            "22F190" -> interpretVin(rawData)
            "221234" -> interpretCustom1(rawData)
            "225678" -> interpretCustom2(rawData)
            else -> InterpretedValue(pid, 0.0, "", "Unknown Enhanced PID", false)
        }
    }
    
    override fun getDisplayInfo(): PidDisplayInfo {
        return PidDisplayInfo(
            name = "Enhanced PID Interpreter",
            description = "Interprets manufacturer-specific and enhanced PIDs",
            supportedPids = supportedPids
        )
    }
    
    private val supportedPids = listOf("22F190", "221234", "225678")
    
    private fun interpretVin(data: ByteArray): InterpretedValue {
        val vin = String(data.drop(3).toByteArray())
        return InterpretedValue("22F190", 0.0, "", vin)
    }
    
    private fun interpretCustom1(data: ByteArray): InterpretedValue {
        if (data.size < 5) return InterpretedValue("221234", 0.0, "", "Invalid", false)
        
        val value = ((data[3].toInt() and 0xFF) shl 8) or (data[4].toInt() and 0xFF)
        return InterpretedValue("221234", value.toDouble(), "units", "$value units")
    }
    
    private fun interpretCustom2(data: ByteArray): InterpretedValue {
        if (data.size < 4) return InterpretedValue("225678", 0.0, "", "Invalid", false)
        
        val value = (data[3].toInt() and 0xFF).toDouble()
        return InterpretedValue("225678", value, "custom", "$value custom")
    }
}

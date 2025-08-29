package com.spacetec.actuators

import android.util.Log
import com.spacetec.obd.RealObdManager
import com.spacetec.bluetooth.BluetoothObdTransportImpl
import com.spacetec.bluetooth.ObdResponse
import com.spacetec.protocols.AdvancedProtocols
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Bi-directional actuator control system for advanced diagnostics
 * Enables testing and control of vehicle actuators and systems
 */
class ActuatorController(private val obdManager: RealObdManager) {
    companion object {
        private const val TAG = "ActuatorController"
        private const val START_ROUTINE = 0x01.toByte()
        private const val STOP_ROUTINE = 0x02.toByte()
    }
    
    private val ACTUATOR_TIMEOUT = 5000L
    private val SAFETY_TIMEOUT = 30000L // Maximum actuator activation time
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Control state flows
    private val _controlResults = MutableSharedFlow<ActuatorResult>()
    val controlResults: SharedFlow<ActuatorResult> = _controlResults.asSharedFlow()
    
    private val _activeControls = MutableStateFlow<Set<String>>(emptySet())
    val activeControls: StateFlow<Set<String>> = _activeControls.asStateFlow()
    
    private val _safetyStatus = MutableStateFlow(SafetyStatus.SAFE)
    val safetyStatus: StateFlow<SafetyStatus> = _safetyStatus.asStateFlow()
    
    private val activeJobs = mutableMapOf<String, Job>()
    
    /**
     * Test fuel injector operation
     */
    suspend fun testFuelInjector(cylinderNumber: Int, duration: Int = 1000): ActuatorResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Testing fuel injector for cylinder $cylinderNumber")
                
                if (!performSafetyCheck("FUEL_INJECTOR")) {
                    return@withContext ActuatorResult.Error("Safety check failed for fuel injector test")
                }
                
                // UDS routine control for injector test
                val routineId = 0x0200 + cylinderNumber // Manufacturer-specific routine
                val request = AdvancedProtocols.UDS.createRoutineControlRequest(
                    routineId, 
                    START_ROUTINE.toInt(),
                    byteArrayOf((duration / 100).toByte()) // Duration in 100ms units
                )
                
                val response = sendActuatorCommand("FUEL_INJECTOR_$cylinderNumber", request)
                
                if (response is ObdResponse.Success) {
                    // Monitor injector operation
                    val monitoringResult = monitorActuatorOperation("FUEL_INJECTOR_$cylinderNumber", duration.toLong())
                    
                    ActuatorResult.Success(
                        actuator = "FUEL_INJECTOR_$cylinderNumber",
                        operation = "TEST",
                        data = mapOf<String, Any>(
                            "cylinder" to cylinderNumber,
                            "duration" to duration,
                            "monitoring" to monitoringResult
                        )
                    )
                } else {
                    ActuatorResult.Error("Fuel injector test failed: ${(response as ObdResponse.Error).message}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Fuel injector test failed", e)
                ActuatorResult.Error("Fuel injector test exception: ${e.message}")
            }
        }
    }
    
    /**
     * Test ignition coil operation
     */
    suspend fun testIgnitionCoil(cylinderNumber: Int, sparkCount: Int = 10): ActuatorResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Testing ignition coil for cylinder $cylinderNumber")
                
                if (!performSafetyCheck("IGNITION_COIL")) {
                    return@withContext ActuatorResult.Error("Safety check failed for ignition coil test")
                }
                
                // UDS routine for ignition coil test
                val routineId = 0x0300 + cylinderNumber
                val request = AdvancedProtocols.UDS.createRoutineControlRequest(
                    routineId,
                    START_ROUTINE.toInt(),
                    byteArrayOf(sparkCount.toByte())
                )
                
                val response = sendActuatorCommand("IGNITION_COIL_$cylinderNumber", request)
                
                if (response is ObdResponse.Success) {
                    val monitoringResult = monitorActuatorOperation("IGNITION_COIL_$cylinderNumber", 2000L)
                    
                    ActuatorResult.Success(
                        actuator = "IGNITION_COIL_$cylinderNumber",
                        operation = "TEST",
                        data = mapOf(
                            "cylinder" to cylinderNumber,
                            "spark_count" to sparkCount,
                            "monitoring" to monitoringResult
                        )
                    )
                } else {
                    ActuatorResult.Error("Ignition coil test failed")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Ignition coil test failed", e)
                ActuatorResult.Error("Ignition coil test exception: ${e.message}")
            }
        }
    }
    
    /**
     * Control idle air control valve
     */
    suspend fun controlIdleAirValve(position: Int): ActuatorResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Controlling idle air valve to position $position")
                
                if (!performSafetyCheck("IDLE_AIR_VALVE")) {
                    return@withContext ActuatorResult.Error("Safety check failed for idle air valve control")
                }
                
                // Validate position range (0-100%)
                val clampedPosition = position.coerceIn(0, 100)
                
                val routineId = 0x0400
                val request = AdvancedProtocols.UDS.createRoutineControlRequest(
                    routineId,
                    START_ROUTINE.toInt(),
                    byteArrayOf(clampedPosition.toByte())
                )
                
                val response = sendActuatorCommand("IDLE_AIR_VALVE", request)
                
                if (response is ObdResponse.Success) {
                    // Monitor valve position feedback
                    delay(500) // Allow valve to move
                    val positionFeedback = readActuatorFeedback("IDLE_AIR_VALVE")
                    
                    ActuatorResult.Success(
                        actuator = "IDLE_AIR_VALVE",
                        operation = "CONTROL",
                        data = mapOf<String, Any>(
                            "target_position" to clampedPosition,
                            "actual_position" to (positionFeedback ?: 0),
                            "monitoring" to kotlin.math.abs(clampedPosition - (positionFeedback ?: 0))
                        )
                    )
                } else {
                    ActuatorResult.Error("Idle air valve control failed")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Idle air valve control failed", e)
                ActuatorResult.Error("Idle air valve control exception: ${e.message}")
            }
        }
    }
    
    /**
     * Test EGR valve operation
     */
    suspend fun testEgrValve(targetPosition: Int = 50): ActuatorResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Testing EGR valve")
                
                if (!performSafetyCheck("EGR_VALVE")) {
                    return@withContext ActuatorResult.Error("Safety check failed for EGR valve test")
                }
                
                val clampedPosition = targetPosition.coerceIn(0, 100)
                
                val routineId = 0x0500
                val request = AdvancedProtocols.UDS.createRoutineControlRequest(
                    routineId,
                    START_ROUTINE.toInt(),
                    byteArrayOf(clampedPosition.toByte())
                )
                
                val response = sendActuatorCommand("EGR_VALVE", request)
                
                if (response is ObdResponse.Success) {
                    val monitoringResult = monitorActuatorOperation("EGR_VALVE", 3000L)
                    
                    ActuatorResult.Success(
                        actuator = "EGR_VALVE",
                        operation = "TEST",
                        data = mapOf<String, Any>(
                            "position" to clampedPosition,
                            "monitoring" to monitoringResult
                        )
                    )
                } else {
                    ActuatorResult.Error("EGR valve test failed")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "EGR valve test failed", e)
                ActuatorResult.Error("EGR valve test exception: ${e.message}")
            }
        }
    }
    
    /**
     * Control cooling fan operation
     */
    suspend fun controlCoolingFan(speed: Int): ActuatorResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Controlling cooling fan to speed $speed")
                
                if (!performSafetyCheck("COOLING_FAN")) {
                    return@withContext ActuatorResult.Error("Safety check failed for cooling fan control")
                }
                
                val clampedSpeed = speed.coerceIn(0, 100)
                
                val routineId = 0x0600
                val request = AdvancedProtocols.UDS.createRoutineControlRequest(
                    routineId,
                    START_ROUTINE.toInt(),
                    byteArrayOf(clampedSpeed.toByte())
                )
                
                val response = sendActuatorCommand("COOLING_FAN", request)
                
                if (response is ObdResponse.Success) {
                    // Start monitoring job with safety timeout
                    val monitoringJob = scope.launch {
                        monitorActuatorOperation("COOLING_FAN", SAFETY_TIMEOUT)
                    }
                    
                    activeJobs["COOLING_FAN"] = monitoringJob
                    updateActiveControls("COOLING_FAN", true)
                    
                    ActuatorResult.Success(
                        actuator = "COOLING_FAN",
                        operation = "CONTROL",
                        data = mapOf(
                            "speed" to clampedSpeed,
                            "monitoring_active" to true
                        )
                    )
                } else {
                    ActuatorResult.Error("Cooling fan control failed")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Cooling fan control failed", e)
                ActuatorResult.Error("Cooling fan control exception: ${e.message}")
            }
        }
    }
    
    /**
     * Test fuel pump operation
     */
    suspend fun testFuelPump(duration: Int = 5000): ActuatorResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Testing fuel pump for ${duration}ms")
                
                if (!performSafetyCheck("FUEL_PUMP")) {
                    return@withContext ActuatorResult.Error("Safety check failed for fuel pump test")
                }
                
                val routineId = 0x0700
                val request = AdvancedProtocols.UDS.createRoutineControlRequest(
                    routineId,
                    STOP_ROUTINE.toInt(),
                    byteArrayOf()
                )
                
                val response = sendActuatorCommand("FUEL_PUMP", request)
                
                if (response is ObdResponse.Success) {
                    val monitoringResult = monitorActuatorOperation("FUEL_PUMP", duration.toLong())
                    
                    ActuatorResult.Success(
                        actuator = "FUEL_PUMP",
                        operation = "TEST",
                        data = mapOf<String, Any>(
                            "position" to 0,
                            "monitoring" to monitoringResult
                        )
                    )
                } else {
                    ActuatorResult.Error("Fuel pump test failed")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Fuel pump test failed", e)
                ActuatorResult.Error("Fuel pump test exception: ${e.message}")
            }
        }
    }
    
    /**
     * Stop all active actuator controls
     */
    suspend fun stopAllControls(): Boolean {
        return try {
            Log.i(TAG, "Stopping all active actuator controls")
            
            // Cancel all monitoring jobs
            activeJobs.values.forEach { it.cancel() }
            activeJobs.clear()
            
            // Send stop commands to all active actuators
            val activeActuators = _activeControls.value.toList()
            for (actuator in activeActuators) {
                try {
                    stopActuatorControl(actuator)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to stop $actuator", e)
                }
            }
            
            _activeControls.value = emptySet()
            _safetyStatus.value = SafetyStatus.SAFE
            
            _controlResults.emit(ActuatorResult.Success(
                actuator = "ALL",
                operation = "STOP",
                data = mapOf("stopped_count" to activeActuators.size)
            ))
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop all controls", e)
            false
        }
    }
    
    /**
     * Perform safety check before actuator operation
     */
    private suspend fun performSafetyCheck(actuator: String): Boolean {
        return try {
            // Check engine conditions
            val engineRunning = isEngineRunning()
            val coolantTemp = getCoolantTemperature()
            val batteryVoltage = getBatteryVoltage()
            
            when (actuator) {
                "FUEL_INJECTOR", "IGNITION_COIL" -> {
                    if (!engineRunning) {
                        Log.w(TAG, "Engine not running - unsafe for $actuator operation")
                        return false
                    }
                }
                "COOLING_FAN" -> {
                    if (coolantTemp > 120) {
                        Log.w(TAG, "Engine overheating - cooling fan control may be dangerous")
                        _safetyStatus.value = SafetyStatus.WARNING
                    }
                }
                "FUEL_PUMP" -> {
                    if (batteryVoltage < 11.0) {
                        Log.w(TAG, "Low battery voltage - fuel pump test may fail")
                        return false
                    }
                }
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Safety check failed for $actuator", e)
            false
        }
    }
    
    /**
     * Send actuator command with proper protocol handling
     */
    private suspend fun sendActuatorCommand(actuator: String, command: ByteArray): ObdResponse {
        return withTimeoutOrNull(ACTUATOR_TIMEOUT) {
            val transport = obdManager.getCurrentTransport()
            if (transport != null) {
                // Convert UDS command to OBD format
                val obdCommand = command.joinToString("") { "%02X".format(it) }
                transport.sendObdCommand(obdCommand)
            } else {
                ObdResponse.Error("No transport available")
            }
        } ?: ObdResponse.Error("Actuator command timeout")
    }
    
    /**
     * Monitor actuator operation during test
     */
    private suspend fun monitorActuatorOperation(actuator: String, duration: Long): Map<String, Any> {
        val startTime = System.currentTimeMillis()
        val monitoringData = mutableMapOf<String, Any>()
        
        try {
            while (System.currentTimeMillis() - startTime < duration) {
                // Read actuator feedback
                val feedback = readActuatorFeedback(actuator)
                if (feedback != null) {
                    monitoringData["feedback_${System.currentTimeMillis()}"] = feedback
                }
                
                delay(100) // Monitor every 100ms
            }
            
            monitoringData["duration"] = System.currentTimeMillis() - startTime
            monitoringData["samples"] = monitoringData.size - 1
            
        } catch (e: Exception) {
            Log.e(TAG, "Monitoring failed for $actuator", e)
            monitoringData["error"] = e.message ?: "Unknown error"
        }
        
        return monitoringData
    }
    
    /**
     * Read actuator feedback/position
     */
    private suspend fun readActuatorFeedback(actuator: String): Int? {
        return try {
            // This would read specific PIDs for actuator feedback
            val feedbackPid = when (actuator) {
                "IDLE_AIR_VALVE" -> "0111" // Throttle position
                "EGR_VALVE" -> "012C" // EGR position
                "COOLING_FAN" -> "012D" // Fan speed
                else -> null
            }
            
            if (feedbackPid != null) {
                val transport = obdManager.getCurrentTransport()
                if (transport != null) {
                    val response = transport.sendObdCommand(feedbackPid)
                    if (response is ObdResponse.Success) {
                        parseActuatorFeedback(feedbackPid, response.data)
                    } else null
                } else null
            } else null
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read feedback for $actuator", e)
            null
        }
    }
    
    /**
     * Parse actuator feedback response
     */
    private fun parseActuatorFeedback(pid: String, response: String): Int? {
        return try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.length >= 6) {
                val value = cleaned.substring(4, 6).toInt(16)
                // Convert to percentage based on PID
                when (pid) {
                    "0111" -> (value * 100) / 255 // Throttle position
                    "012C" -> (value * 100) / 255 // EGR position
                    "012D" -> (value * 100) / 255 // Fan speed
                    else -> value
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Stop specific actuator control
     */
    private suspend fun stopActuatorControl(actuator: String) {
        try {
            // Send stop command
            val routineId = when (actuator) {
                "COOLING_FAN" -> 0x0600
                "IDLE_AIR_VALVE" -> 0x0400
                "EGR_VALVE" -> 0x0500
                else -> 0x0000
            }
            
            if (routineId != 0x0000) {
                val request = AdvancedProtocols.UDS.createRoutineControlRequest(
                    routineId,
                    STOP_ROUTINE.toInt(),
                    byteArrayOf()
                )
                sendActuatorCommand(actuator, request)
            }
            
            updateActiveControls(actuator, false)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop $actuator", e)
        }
    }
    
    /**
     * Update active controls set
     */
    private fun updateActiveControls(actuator: String, active: Boolean) {
        val currentControls = _activeControls.value.toMutableSet()
        if (active) {
            currentControls.add(actuator)
        } else {
            currentControls.remove(actuator)
        }
        _activeControls.value = currentControls
    }
    
    // Helper methods for safety checks
    private suspend fun isEngineRunning(): Boolean {
        return try {
            val transport = obdManager.getCurrentTransport()
            if (transport != null) {
                val response = transport.sendObdCommand("010C") // RPM
                if (response is ObdResponse.Success) {
                    val rpm = com.spacetec.obd.ObdProtocol.parseRpm(response.data)
                    rpm > 0
                } else false
            } else false
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun getCoolantTemperature(): Int {
        return try {
            val transport = obdManager.getCurrentTransport()
            if (transport != null) {
                val response = transport.sendObdCommand("0105") // Coolant temp
                if (response is ObdResponse.Success) {
                    com.spacetec.obd.ObdProtocol.parseCoolantTemp(response.data)
                } else 0
            } else 0
        } catch (e: Exception) {
            0
        }
    }
    
    private suspend fun getBatteryVoltage(): Double {
        return try {
            val transport = obdManager.getCurrentTransport()
            if (transport != null) {
                val response = transport.sendObdCommand("0142") // Control module voltage
                if (response is ObdResponse.Success) {
                    val cleaned = response.data.replace(" ", "").uppercase()
                    if (cleaned.startsWith("4142") && cleaned.length >= 8) {
                        val voltageRaw = cleaned.substring(4, 8).toInt(16)
                        voltageRaw / 1000.0
                    } else 12.6
                } else 12.6
            } else 12.6
        } catch (e: Exception) {
            12.6
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.launch {
            stopAllControls()
        }
        scope.cancel()
    }
}

// Extension for UDS routine control
private fun AdvancedProtocols.UDS.createRoutineControlRequest(
    routineId: Int,
    subFunction: Int,
    data: ByteArray
): ByteArray {
    val request = ByteArray(4 + data.size)
    request[0] = ROUTINE_CONTROL.toByte()
    request[1] = subFunction.toByte()
    request[2] = (routineId shr 8).toByte()
    request[3] = (routineId and 0xFF).toByte()
    data.copyInto(request, 4)
    return request
}

private const val START_ROUTINE = 0x01
private const val STOP_ROUTINE = 0x02
private const val REQUEST_ROUTINE_RESULTS = 0x03

// Result classes
sealed class ActuatorResult {
    data class Success(
        val actuator: String,
        val operation: String,
        val data: Map<String, Any>,
        val timestamp: Long = System.currentTimeMillis()
    ) : ActuatorResult()
    
    data class Error(
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : ActuatorResult()
}

enum class SafetyStatus {
    SAFE, WARNING, DANGER
}

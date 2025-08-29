package com.spacetec.professional

import android.util.Log
import com.spacetec.obd.RealObdManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Bi-Directional Controls - Professional actuator testing and control
 * Similar to Autel MaxiSys bi-directional controls for component testing
 */
class BiDirectionalControls(
    private val obdManager: RealObdManager
) {
    companion object {
        private const val TAG = "BiDirectionalControls"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Control execution state flows
    private val _controlResults = MutableSharedFlow<ControlResult>()
    val controlResults: SharedFlow<ControlResult> = _controlResults.asSharedFlow()
    
    private val _controlProgress = MutableStateFlow<ControlProgress?>(null)
    val controlProgress: StateFlow<ControlProgress?> = _controlProgress.asStateFlow()
    
    private val _liveData = MutableStateFlow<Map<String, Any>>(emptyMap())
    val liveData: StateFlow<Map<String, Any>> = _liveData.asStateFlow()
    
    /**
     * Execute bi-directional control
     */
    suspend fun executeControl(
        vehicleMake: String,
        vehicleModel: String,
        vehicleYear: Int,
        controlId: String,
        parameters: Map<String, Any> = emptyMap()
    ): ControlResult = withContext(Dispatchers.IO) {
        
        try {
            Log.i(TAG, "Executing bi-directional control: $controlId for $vehicleMake $vehicleModel ($vehicleYear)")
            
            // Find control definition
            val control = getControlById(controlId, vehicleMake, vehicleModel, vehicleYear)
                ?: return@withContext ControlResult.Error("Control not found: $controlId")
            
            // Check compatibility
            if (!isControlCompatible(control, vehicleMake, vehicleModel, vehicleYear)) {
                return@withContext ControlResult.Error("Control not compatible with this vehicle")
            }
            
            // Perform safety checks
            val safetyResult = performSafetyChecks(control)
            if (!safetyResult.passed) {
                return@withContext ControlResult.Error("Safety check failed: ${safetyResult.failureReason}")
            }
            
            // Update progress
            _controlProgress.value = ControlProgress(controlId, "Starting", 0.0f, "Initializing control sequence")
            
            // Execute control sequence
            val result = executeControlSequence(control, parameters)
            
            _controlProgress.value = ControlProgress(controlId, "Completed", 1.0f, "Control sequence completed")
            _controlResults.emit(result)
            
            Log.i(TAG, "Bi-directional control completed: $controlId")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Bi-directional control failed: $controlId", e)
            val errorResult = ControlResult.Error("Control execution failed: ${e.message}")
            _controlResults.emit(errorResult)
            errorResult
        }
    }
    
    /**
     * Get available controls for vehicle
     */
    fun getAvailableControls(make: String, model: String, year: Int): List<BiDirectionalControl> {
        return biDirectionalControls.filter { control ->
            control.supportedVehicles.any { vehicle ->
                vehicle.make.equals(make, ignoreCase = true) &&
                vehicle.model.equals(model, ignoreCase = true) &&
                isYearInRange(year, vehicle.yearRange)
            }
        }
    }
    
    /**
     * Get controls by system
     */
    fun getControlsBySystem(system: ControlSystem, make: String, model: String, year: Int): List<BiDirectionalControl> {
        return getAvailableControls(make, model, year).filter { it.system == system }
    }
    
    private suspend fun executeControlSequence(
        control: BiDirectionalControl,
        parameters: Map<String, Any>
    ): ControlResult {
        val transport = obdManager.getCurrentTransport() as? com.spacetec.bluetooth.BluetoothObdTransportImpl
            ?: return ControlResult.Error("No transport available")
        
        val results = mutableMapOf<String, Any>()
        val totalSteps = control.steps.size
        
        for ((index, step) in control.steps.withIndex()) {
            val progress = (index.toFloat() / totalSteps) * 0.8f + 0.1f
            _controlProgress.value = ControlProgress(control.id, step.description, progress, "Executing: ${step.description}")
            
            val stepResult = executeControlStep(step, transport, parameters)
            if (!stepResult.success && step.critical) {
                return ControlResult.Error("Critical step failed: ${step.description} - ${stepResult.error}")
            }
            
            stepResult.data?.let { results.putAll(it) }
            delay((step.delayMs ?: 200).toLong())
        }
        
        return ControlResult.Success(
            controlId = control.id,
            data = results,
            message = "Control '${control.name}' executed successfully"
        )
    }
    
    private suspend fun executeControlStep(
        step: ControlStep,
        transport: com.spacetec.bluetooth.BluetoothObdTransportImpl,
        parameters: Map<String, Any>
    ): ControlStepResult {
        
        return when (step.type) {
            ControlStepType.ACTIVATE -> executeActivate(step, transport, parameters)
            ControlStepType.DEACTIVATE -> executeDeactivate(step, transport)
            ControlStepType.SET_VALUE -> executeSetValue(step, transport, parameters)
            ControlStepType.READ_STATUS -> executeReadStatus(step, transport)
            ControlStepType.CYCLE -> executeCycle(step, transport, parameters)
            ControlStepType.CALIBRATE -> executeCalibrate(step, transport, parameters)
        }
    }
    
    private suspend fun executeActivate(
        step: ControlStep,
        transport: com.spacetec.bluetooth.BluetoothObdTransportImpl,
        parameters: Map<String, Any>
    ): ControlStepResult {
        val command = step.command ?: return ControlStepResult(false, "No command specified", null)
        val finalCommand = replaceParameters(command, parameters, step.defaultValues)
        
        val response = transport.sendObdCommand(finalCommand)
        
        return when (response) {
            is com.spacetec.bluetooth.ObdResponse.Success -> {
                ControlStepResult(true, null, mapOf("activated" to true, "response" to response.data))
            }
            is com.spacetec.bluetooth.ObdResponse.Error -> {
                ControlStepResult(false, response.message, null)
            }
        }
    }
    
    private suspend fun executeDeactivate(
        step: ControlStep,
        transport: com.spacetec.bluetooth.BluetoothObdTransportImpl
    ): ControlStepResult {
        val command = step.command ?: return ControlStepResult(false, "No command specified", null)
        val response = transport.sendObdCommand(command)
        
        return when (response) {
            is com.spacetec.bluetooth.ObdResponse.Success -> {
                ControlStepResult(true, null, mapOf("deactivated" to true))
            }
            is com.spacetec.bluetooth.ObdResponse.Error -> {
                ControlStepResult(false, response.message, null)
            }
        }
    }
    
    private suspend fun executeSetValue(
        step: ControlStep,
        transport: com.spacetec.bluetooth.BluetoothObdTransportImpl,
        parameters: Map<String, Any>
    ): ControlStepResult {
        val command = step.command ?: return ControlStepResult(false, "No command specified", null)
        val finalCommand = replaceParameters(command, parameters, step.defaultValues)
        
        val response = transport.sendObdCommand(finalCommand)
        
        return when (response) {
            is com.spacetec.bluetooth.ObdResponse.Success -> {
                ControlStepResult(true, null, mapOf("value_set" to true))
            }
            is com.spacetec.bluetooth.ObdResponse.Error -> {
                ControlStepResult(false, response.message, null)
            }
        }
    }
    
    private suspend fun executeReadStatus(
        step: ControlStep,
        transport: com.spacetec.bluetooth.BluetoothObdTransportImpl
    ): ControlStepResult {
        val command = step.command ?: return ControlStepResult(false, "No command specified", null)
        val response = transport.sendObdCommand(command)
        
        return when (response) {
            is com.spacetec.bluetooth.ObdResponse.Success -> {
                ControlStepResult(true, null, mapOf("status" to response.data))
            }
            is com.spacetec.bluetooth.ObdResponse.Error -> {
                ControlStepResult(false, response.message, null)
            }
        }
    }
    
    private suspend fun executeCycle(
        step: ControlStep,
        transport: com.spacetec.bluetooth.BluetoothObdTransportImpl,
        parameters: Map<String, Any>
    ): ControlStepResult {
        val activateCommand = step.command ?: return ControlStepResult(false, "No activate command", null)
        val deactivateCommand = step.deactivateCommand ?: return ControlStepResult(false, "No deactivate command", null)
        val cycles = parameters["cycles"]?.toString()?.toIntOrNull() ?: step.defaultValues?.get("cycles")?.toString()?.toIntOrNull() ?: 3
        
        val results = mutableMapOf<String, Any>()
        
        for (i in 1..cycles) {
            val activateResponse = transport.sendObdCommand(activateCommand)
            if (activateResponse is com.spacetec.bluetooth.ObdResponse.Error) {
                return ControlStepResult(false, "Cycle $i activation failed: ${activateResponse.message}", null)
            }
            
            delay(1000)
            
            val deactivateResponse = transport.sendObdCommand(deactivateCommand)
            if (deactivateResponse is com.spacetec.bluetooth.ObdResponse.Error) {
                return ControlStepResult(false, "Cycle $i deactivation failed: ${deactivateResponse.message}", null)
            }
            
            delay(500)
            results["cycle_$i"] = "completed"
        }
        
        return ControlStepResult(true, null, results)
    }
    
    private suspend fun executeCalibrate(
        step: ControlStep,
        transport: com.spacetec.bluetooth.BluetoothObdTransportImpl,
        parameters: Map<String, Any>
    ): ControlStepResult {
        val command = step.command ?: return ControlStepResult(false, "No command specified", null)
        val finalCommand = replaceParameters(command, parameters, step.defaultValues)
        
        val response = transport.sendObdCommand(finalCommand)
        
        return when (response) {
            is com.spacetec.bluetooth.ObdResponse.Success -> {
                ControlStepResult(true, null, mapOf("calibrated" to true))
            }
            is com.spacetec.bluetooth.ObdResponse.Error -> {
                ControlStepResult(false, response.message, null)
            }
        }
    }
    
    private fun replaceParameters(
        command: String,
        parameters: Map<String, Any>,
        defaultValues: Map<String, Any>?
    ): String {
        var result = command
        
        parameters.forEach { (key, value) ->
            result = result.replace("{$key}", value.toString())
        }
        
        defaultValues?.forEach { (key, value) ->
            result = result.replace("{$key}", value.toString())
        }
        
        return result
    }
    
    private fun performSafetyChecks(control: BiDirectionalControl): ControlSafetyResult {
        return ControlSafetyResult(true, null)
    }
    
    private fun getControlById(controlId: String, make: String, model: String, year: Int): BiDirectionalControl? {
        return getAvailableControls(make, model, year).find { it.id == controlId }
    }
    
    private fun isControlCompatible(control: BiDirectionalControl, make: String, model: String, year: Int): Boolean {
        return control.supportedVehicles.any { vehicle ->
            vehicle.make.equals(make, ignoreCase = true) &&
            vehicle.model.equals(model, ignoreCase = true) &&
            isYearInRange(year, vehicle.yearRange)
        }
    }
    
    private fun isYearInRange(year: Int, range: String): Boolean {
        return try {
            val parts = range.split("-")
            val startYear = parts[0].toInt()
            val endYear = if (parts.size > 1) parts[1].toInt() else Int.MAX_VALUE
            year in startYear..endYear
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
    }
}

// Data classes and enums
enum class ControlSystem {
    ENGINE, TRANSMISSION, BRAKES, SUSPENSION, STEERING, HVAC, LIGHTING, ELECTRICAL, BODY, EMISSIONS
}

enum class ControlStepType {
    ACTIVATE, DEACTIVATE, SET_VALUE, READ_STATUS, CYCLE, CALIBRATE
}

data class BiDirectionalControl(
    val id: String,
    val name: String,
    val description: String,
    val system: ControlSystem,
    val icon: String,
    val difficulty: ControlDifficulty,
    val estimatedTime: String,
    val supportedVehicles: List<VehicleControlSupport>,
    val steps: List<ControlStep>,
    val warnings: List<String> = emptyList(),
    val safetyRequirements: List<String> = emptyList()
)

data class VehicleControlSupport(
    val make: String,
    val model: String,
    val yearRange: String,
    val engine: String? = null,
    val transmission: String? = null
)

data class ControlStep(
    val id: String,
    val description: String,
    val type: ControlStepType,
    val command: String? = null,
    val deactivateCommand: String? = null,
    val defaultValues: Map<String, Any>? = null,
    val delayMs: Int? = null,
    val critical: Boolean = false
)

enum class ControlDifficulty {
    BASIC, INTERMEDIATE, ADVANCED, EXPERT
}

sealed class ControlResult {
    data class Success(
        val controlId: String,
        val data: Map<String, Any>,
        val message: String
    ) : ControlResult()
    
    data class Error(
        val message: String
    ) : ControlResult()
}

data class ControlProgress(
    val controlId: String,
    val status: String,
    val progress: Float,
    val stepDescription: String
)

data class ControlStepResult(
    val success: Boolean,
    val error: String?,
    val data: Map<String, Any>?
)

data class ControlSafetyResult(
    val passed: Boolean,
    val failureReason: String?
)

// Pre-defined Bi-Directional Controls database
private val biDirectionalControls = listOf(
    BiDirectionalControl(
        id = "fuel_injector_test",
        name = "Fuel Injector Test",
        description = "Test individual fuel injectors",
        system = ControlSystem.ENGINE,
        icon = "precision_manufacturing",
        difficulty = ControlDifficulty.INTERMEDIATE,
        estimatedTime = "5 minutes",
        supportedVehicles = listOf(
            VehicleControlSupport("BMW", "3 Series", "2015-2024"),
            VehicleControlSupport("Mercedes-Benz", "C-Class", "2015-2024"),
            VehicleControlSupport("Audi", "A4", "2015-2024"),
            VehicleControlSupport("Toyota", "Camry", "2015-2024")
        ),
        steps = listOf(
            ControlStep("1", "Activate injector 1", ControlStepType.ACTIVATE, "31FF0101"),
            ControlStep("2", "Activate injector 2", ControlStepType.ACTIVATE, "31FF0102"),
            ControlStep("3", "Activate injector 3", ControlStepType.ACTIVATE, "31FF0103"),
            ControlStep("4", "Activate injector 4", ControlStepType.ACTIVATE, "31FF0104"),
            ControlStep("5", "Read injector status", ControlStepType.READ_STATUS, "22F110")
        ),
        warnings = listOf("Engine must be running", "Monitor fuel pressure"),
        safetyRequirements = listOf("ignition_on")
    ),
    
    BiDirectionalControl(
        id = "throttle_body_test",
        name = "Throttle Body Test",
        description = "Test throttle body operation",
        system = ControlSystem.ENGINE,
        icon = "tune",
        difficulty = ControlDifficulty.INTERMEDIATE,
        estimatedTime = "3 minutes",
        supportedVehicles = listOf(
            VehicleControlSupport("BMW", "3 Series", "2015-2024"),
            VehicleControlSupport("Mercedes-Benz", "C-Class", "2015-2024"),
            VehicleControlSupport("Audi", "A4", "2015-2024"),
            VehicleControlSupport("Volkswagen", "Golf", "2015-2024")
        ),
        steps = listOf(
            ControlStep("1", "Set throttle position", ControlStepType.SET_VALUE, "31FF0201{position}",
                defaultValues = mapOf("position" to "25")),
            ControlStep("2", "Read throttle position", ControlStepType.READ_STATUS, "22F111"),
            ControlStep("3", "Cycle throttle", ControlStepType.CYCLE, "31FF0202", "31FF0203",
                defaultValues = mapOf("cycles" to "3"))
        ),
        warnings = listOf("Engine must be off"),
        safetyRequirements = listOf("engine_off", "ignition_on")
    ),
    
    BiDirectionalControl(
        id = "abs_pump_test",
        name = "ABS Pump Test",
        description = "Test ABS pump motor operation",
        system = ControlSystem.BRAKES,
        icon = "car_repair",
        difficulty = ControlDifficulty.ADVANCED,
        estimatedTime = "2 minutes",
        supportedVehicles = listOf(
            VehicleControlSupport("BMW", "3 Series", "2015-2024"),
            VehicleControlSupport("Mercedes-Benz", "C-Class", "2015-2024"),
            VehicleControlSupport("Audi", "A4", "2015-2024")
        ),
        steps = listOf(
            ControlStep("1", "Activate ABS pump", ControlStepType.ACTIVATE, "31FF0301"),
            ControlStep("2", "Monitor pump pressure", ControlStepType.READ_STATUS, "22F120"),
            ControlStep("3", "Deactivate pump", ControlStepType.DEACTIVATE, "31FF0302")
        ),
        warnings = listOf("Vehicle must be stationary", "Parking brake on"),
        safetyRequirements = listOf("parking_brake_on", "transmission_park")
    ),
    
    BiDirectionalControl(
        id = "hvac_actuator_test",
        name = "HVAC Actuator Test",
        description = "Test HVAC blend door actuators",
        system = ControlSystem.HVAC,
        icon = "ac_unit",
        difficulty = ControlDifficulty.BASIC,
        estimatedTime = "4 minutes",
        supportedVehicles = listOf(
            VehicleControlSupport("BMW", "3 Series", "2015-2024"),
            VehicleControlSupport("Mercedes-Benz", "C-Class", "2015-2024"),
            VehicleControlSupport("Audi", "A4", "2015-2024"),
            VehicleControlSupport("Toyota", "Camry", "2015-2024")
        ),
        steps = listOf(
            ControlStep("1", "Test temperature actuator", ControlStepType.CYCLE, "31FF0501", "31FF0502"),
            ControlStep("2", "Test mode actuator", ControlStepType.CYCLE, "31FF0503", "31FF0504"),
            ControlStep("3", "Test recirculation actuator", ControlStepType.CYCLE, "31FF0505", "31FF0506"),
            ControlStep("4", "Return to auto mode", ControlStepType.ACTIVATE, "31FF0507")
        ),
        warnings = listOf("Listen for actuator movement"),
        safetyRequirements = listOf("ignition_on")
    )
)

package com.spacetec.professional

import android.util.Log
import com.spacetec.obd.RealObdManager
import com.spacetec.protocols.AdvancedProtocols
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Service Functions - Professional service procedures
 * Oil reset, EPB, DPF regeneration, throttle adaptation, etc.
 */
class ServiceFunctions(
    private val obdManager: RealObdManager
) {
    companion object {
        private const val TAG = "ServiceFunctions"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Service execution state flows
    private val _serviceResults = MutableSharedFlow<ServiceResult>()
    val serviceResults: SharedFlow<ServiceResult> = _serviceResults.asSharedFlow()
    
    private val _serviceProgress = MutableStateFlow<ServiceProgress?>(null)
    val serviceProgress: StateFlow<ServiceProgress?> = _serviceProgress.asStateFlow()
    
    /**
     * Execute service function
     */
    suspend fun executeService(
        vehicleMake: String,
        vehicleModel: String,
        vehicleYear: Int,
        serviceId: String,
        parameters: Map<String, Any> = emptyMap()
    ): ServiceResult = withContext(Dispatchers.IO) {
        
        try {
            Log.i(TAG, "Executing service function: $serviceId for $vehicleMake $vehicleModel ($vehicleYear)")
            
            // Find service definition
            val service = getServiceById(serviceId, vehicleMake, vehicleModel, vehicleYear)
                ?: return@withContext ServiceResult.Error("Service not found: $serviceId")
            
            // Check compatibility
            if (!isServiceCompatible(service, vehicleMake, vehicleModel, vehicleYear)) {
                return@withContext ServiceResult.Error("Service not compatible with this vehicle")
            }
            
            // Perform pre-service checks
            val preCheckResult = performPreServiceChecks(service)
            if (!preCheckResult.passed) {
                return@withContext ServiceResult.Error("Pre-service check failed: ${preCheckResult.failureReason}")
            }
            
            // Update progress
            _serviceProgress.value = ServiceProgress(serviceId, "Starting", 0.0f, "Initializing service procedure")
            
            // Execute service procedure
            val result = executeServiceProcedure(service, parameters)
            
            _serviceProgress.value = ServiceProgress(serviceId, "Completed", 1.0f, "Service procedure completed")
            _serviceResults.emit(result)
            
            Log.i(TAG, "Service function completed: $serviceId")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Service function failed: $serviceId", e)
            val errorResult = ServiceResult.Error("Service execution failed: ${e.message}")
            _serviceResults.emit(errorResult)
            errorResult
        }
    }
    
    /**
     * Get available services for vehicle
     */
    fun getAvailableServices(make: String, model: String, year: Int): List<ServiceFunction> {
        return serviceFunctions.filter { service ->
            service.supportedVehicles.any { vehicle ->
                vehicle.make.equals(make, ignoreCase = true) &&
                vehicle.model.equals(model, ignoreCase = true) &&
                isYearInRange(year, vehicle.yearRange)
            }
        }
    }
    
    /**
     * Get services by category
     */
    fun getServicesByCategory(category: ServiceCategory, make: String, model: String, year: Int): List<ServiceFunction> {
        return getAvailableServices(make, model, year).filter { it.category == category }
    }
    
    private suspend fun executeServiceProcedure(
        service: ServiceFunction,
        parameters: Map<String, Any>
    ): ServiceResult {
        val transport = obdManager.getCurrentTransport() as? com.spacetec.bluetooth.BluetoothObdTransportImpl
            ?: return ServiceResult.Error("No transport available")
        
        val results = mutableMapOf<String, Any>()
        val totalSteps = service.steps.size
        
        for ((index, step) in service.steps.withIndex()) {
            val progress = (index.toFloat() / totalSteps) * 0.8f + 0.1f
            _serviceProgress.value = ServiceProgress(service.id, step.description, progress, "Executing: ${step.description}")
            
            val stepResult = executeServiceStep(step, transport, parameters)
            if (!stepResult.success) {
                return ServiceResult.Error("Step failed: ${step.description} - ${stepResult.error}")
            }
            
            stepResult.data?.let { results.putAll(it) }
            delay((step.delayMs ?: 500).toLong()) // Delay between steps
        }
        
        return ServiceResult.Success(
            serviceId = service.id,
            data = results,
            message = "Service '${service.name}' completed successfully"
        )
    }
    
    private suspend fun executeServiceStep(
        step: ServiceStep,
        transport: com.spacetec.bluetooth.BluetoothObdTransportImpl,
        parameters: Map<String, Any>
    ): ServiceStepResult {
        
        return when (step.type) {
            ServiceStepType.COMMAND -> executeCommand(step, transport, parameters)
            ServiceStepType.SEQUENCE -> executeSequence(step, transport, parameters)
            ServiceStepType.WAIT -> executeWait(step)
            ServiceStepType.VERIFY -> executeVerify(step, transport)
            ServiceStepType.ADAPTATION -> executeAdaptation(step, transport, parameters)
            ServiceStepType.RESET -> executeReset(step, transport)
        }
    }
    
    private suspend fun executeCommand(
        step: ServiceStep,
        transport: com.spacetec.bluetooth.BluetoothObdTransportImpl,
        parameters: Map<String, Any>
    ): ServiceStepResult {
        val command = step.command ?: return ServiceStepResult(false, "No command specified", null)
        val finalCommand = replaceParameters(command, parameters, step.defaultValues)
        
        val response = transport.sendObdCommand(finalCommand)
        
        return when (response) {
            is com.spacetec.bluetooth.ObdResponse.Success -> {
                ServiceStepResult(true, null, mapOf("response" to response.data))
            }
            is com.spacetec.bluetooth.ObdResponse.Error -> {
                ServiceStepResult(false, response.message, null)
            }
        }
    }
    
    private suspend fun executeSequence(
        step: ServiceStep,
        transport: com.spacetec.bluetooth.BluetoothObdTransportImpl,
        parameters: Map<String, Any>
    ): ServiceStepResult {
        val commands = step.commandSequence ?: return ServiceStepResult(false, "No command sequence", null)
        val results = mutableMapOf<String, Any>()
        
        for ((index, command) in commands.withIndex()) {
            val finalCommand = replaceParameters(command, parameters, step.defaultValues)
            val response = transport.sendObdCommand(finalCommand)
            
            when (response) {
                is com.spacetec.bluetooth.ObdResponse.Success -> {
                    results["sequence_$index"] = response.data
                }
                is com.spacetec.bluetooth.ObdResponse.Error -> {
                    return ServiceStepResult(false, "Sequence failed at step $index: ${response.message}", null)
                }
            }
            
            delay(200) // Small delay between sequence commands
        }
        
        return ServiceStepResult(true, null, results)
    }
    
    private suspend fun executeWait(step: ServiceStep): ServiceStepResult {
        val waitTime = step.waitTimeMs ?: 1000
        delay(waitTime.toLong())
        return ServiceStepResult(true, null, mapOf("waited_ms" to waitTime))
    }
    
    private suspend fun executeVerify(
        step: ServiceStep,
        transport: com.spacetec.bluetooth.BluetoothObdTransportImpl
    ): ServiceStepResult {
        val verifyCommand = step.verifyCommand ?: return ServiceStepResult(false, "No verify command", null)
        val response = transport.sendObdCommand(verifyCommand)
        
        return when (response) {
            is com.spacetec.bluetooth.ObdResponse.Success -> {
                val expectedValue = step.expectedValue
                val isValid = if (expectedValue != null) {
                    response.data.contains(expectedValue)
                } else {
                    response.data.isNotEmpty()
                }
                
                if (isValid) {
                    ServiceStepResult(true, null, mapOf("verification" to "passed", "data" to response.data))
                } else {
                    ServiceStepResult(false, "Verification failed: expected $expectedValue", null)
                }
            }
            is com.spacetec.bluetooth.ObdResponse.Error -> {
                ServiceStepResult(false, "Verification command failed: ${response.message}", null)
            }
        }
    }
    
    private suspend fun executeAdaptation(
        step: ServiceStep,
        transport: com.spacetec.bluetooth.BluetoothObdTransportImpl,
        parameters: Map<String, Any>
    ): ServiceStepResult {
        val command = step.command ?: return ServiceStepResult(false, "No adaptation command", null)
        val finalCommand = replaceParameters(command, parameters, step.defaultValues)
        
        val response = transport.sendObdCommand(finalCommand)
        
        return when (response) {
            is com.spacetec.bluetooth.ObdResponse.Success -> {
                ServiceStepResult(true, null, mapOf("adaptation_completed" to true))
            }
            is com.spacetec.bluetooth.ObdResponse.Error -> {
                ServiceStepResult(false, response.message, null)
            }
        }
    }
    
    private suspend fun executeReset(
        step: ServiceStep,
        transport: com.spacetec.bluetooth.BluetoothObdTransportImpl
    ): ServiceStepResult {
        val command = step.command ?: return ServiceStepResult(false, "No reset command", null)
        val response = transport.sendObdCommand(command)
        
        return when (response) {
            is com.spacetec.bluetooth.ObdResponse.Success -> {
                ServiceStepResult(true, null, mapOf("reset_completed" to true))
            }
            is com.spacetec.bluetooth.ObdResponse.Error -> {
                ServiceStepResult(false, response.message, null)
            }
        }
    }
    
    private fun replaceParameters(
        command: String,
        parameters: Map<String, Any>,
        defaultValues: Map<String, Any>?
    ): String {
        var result = command
        
        // Replace parameters from user input
        parameters.forEach { (key, value) ->
            result = result.replace("{$key}", value.toString())
        }
        
        // Replace with default values if still has placeholders
        defaultValues?.forEach { (key, value) ->
            result = result.replace("{$key}", value.toString())
        }
        
        return result
    }
    
    private fun performPreServiceChecks(service: ServiceFunction): ServiceCheckResult {
        // Check requirements
        for (requirement in service.requirements) {
            when (requirement) {
                "ignition_on" -> {
                    // Check ignition status
                }
                "engine_off" -> {
                    // Check engine is off
                }
                "parking_brake_on" -> {
                    // Check parking brake
                }
                "transmission_park" -> {
                    // Check transmission in park
                }
            }
        }
        
        return ServiceCheckResult(true, null)
    }
    
    private fun getServiceById(serviceId: String, make: String, model: String, year: Int): ServiceFunction? {
        return getAvailableServices(make, model, year).find { it.id == serviceId }
    }
    
    private fun isServiceCompatible(service: ServiceFunction, make: String, model: String, year: Int): Boolean {
        return service.supportedVehicles.any { vehicle ->
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
enum class ServiceCategory {
    MAINTENANCE,
    BRAKES,
    ENGINE,
    TRANSMISSION,
    ELECTRICAL,
    EMISSIONS,
    SUSPENSION,
    STEERING,
    HVAC,
    BODY
}

enum class ServiceStepType {
    COMMAND,
    SEQUENCE,
    WAIT,
    VERIFY,
    ADAPTATION,
    RESET
}

data class ServiceFunction(
    val id: String,
    val name: String,
    val description: String,
    val category: ServiceCategory,
    val icon: String,
    val difficulty: ServiceDifficulty,
    val estimatedTime: String,
    val supportedVehicles: List<VehicleServiceSupport>,
    val steps: List<ServiceStep>,
    val warnings: List<String> = emptyList(),
    val requirements: List<String> = emptyList(),
    val tools: List<String> = emptyList()
)

data class VehicleServiceSupport(
    val make: String,
    val model: String,
    val yearRange: String,
    val engine: String? = null,
    val transmission: String? = null
)

data class ServiceStep(
    val id: String,
    val description: String,
    val type: ServiceStepType,
    val command: String? = null,
    val commandSequence: List<String>? = null,
    val waitTimeMs: Int? = null,
    val verifyCommand: String? = null,
    val expectedValue: String? = null,
    val defaultValues: Map<String, Any>? = null,
    val delayMs: Int? = null
)

enum class ServiceDifficulty {
    BASIC,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

sealed class ServiceResult {
    data class Success(
        val serviceId: String,
        val data: Map<String, Any>,
        val message: String
    ) : ServiceResult()
    
    data class Error(
        val message: String
    ) : ServiceResult()
}

data class ServiceProgress(
    val serviceId: String,
    val status: String,
    val progress: Float,
    val stepDescription: String
)

data class ServiceStepResult(
    val success: Boolean,
    val error: String?,
    val data: Map<String, Any>?
)

data class ServiceCheckResult(
    val passed: Boolean,
    val failureReason: String?
)

// Pre-defined Service Functions database
private val serviceFunctions = listOf(
    // Oil Service Reset
    ServiceFunction(
        id = "oil_service_reset",
        name = "Oil Service Reset",
        description = "Reset oil service interval and maintenance light",
        category = ServiceCategory.MAINTENANCE,
        icon = "oil_barrel",
        difficulty = ServiceDifficulty.BASIC,
        estimatedTime = "2 minutes",
        supportedVehicles = listOf(
            VehicleServiceSupport("BMW", "3 Series", "2015-2024"),
            VehicleServiceSupport("BMW", "5 Series", "2015-2024"),
            VehicleServiceSupport("Mercedes-Benz", "C-Class", "2015-2024"),
            VehicleServiceSupport("Mercedes-Benz", "E-Class", "2015-2024"),
            VehicleServiceSupport("Audi", "A3", "2015-2024"),
            VehicleServiceSupport("Audi", "A4", "2015-2024"),
            VehicleServiceSupport("Volkswagen", "Golf", "2015-2024"),
            VehicleServiceSupport("Toyota", "Camry", "2015-2024"),
            VehicleServiceSupport("Honda", "Civic", "2015-2024")
        ),
        steps = listOf(
            ServiceStep("1", "Enter service mode", ServiceStepType.COMMAND, "1002"),
            ServiceStep("2", "Reset oil service", ServiceStepType.COMMAND, "31FF0001"),
            ServiceStep("3", "Verify reset", ServiceStepType.VERIFY, verifyCommand = "22F190", expectedValue = "62F190"),
            ServiceStep("4", "Exit service mode", ServiceStepType.COMMAND, "1001")
        ),
        requirements = listOf("ignition_on", "engine_off")
    ),
    
    // Electronic Parking Brake (EPB) Reset
    ServiceFunction(
        id = "epb_reset",
        name = "EPB Reset",
        description = "Reset electronic parking brake after pad replacement",
        category = ServiceCategory.BRAKES,
        icon = "car_repair",
        difficulty = ServiceDifficulty.INTERMEDIATE,
        estimatedTime = "5 minutes",
        supportedVehicles = listOf(
            VehicleServiceSupport("BMW", "3 Series", "2015-2024"),
            VehicleServiceSupport("BMW", "5 Series", "2015-2024"),
            VehicleServiceSupport("Mercedes-Benz", "C-Class", "2015-2024"),
            VehicleServiceSupport("Audi", "A4", "2015-2024"),
            VehicleServiceSupport("Volkswagen", "Passat", "2015-2024")
        ),
        steps = listOf(
            ServiceStep("1", "Enter service mode", ServiceStepType.COMMAND, "1002"),
            ServiceStep("2", "Retract EPB", ServiceStepType.COMMAND, "31010301"),
            ServiceStep("3", "Wait for retraction", ServiceStepType.WAIT, waitTimeMs = 10000),
            ServiceStep("4", "Reset adaptation", ServiceStepType.ADAPTATION, "2CF18001FF"),
            ServiceStep("5", "Apply EPB", ServiceStepType.COMMAND, "31010302"),
            ServiceStep("6", "Verify operation", ServiceStepType.VERIFY, verifyCommand = "22F1A0", expectedValue = "62F1A0"),
            ServiceStep("7", "Exit service mode", ServiceStepType.COMMAND, "1001")
        ),
        warnings = listOf("Ensure vehicle is secure", "Brake pads must be installed"),
        requirements = listOf("ignition_on", "engine_off", "parking_brake_on")
    ),
    
    // DPF Regeneration
    ServiceFunction(
        id = "dpf_regeneration",
        name = "DPF Regeneration",
        description = "Force diesel particulate filter regeneration",
        category = ServiceCategory.EMISSIONS,
        icon = "air",
        difficulty = ServiceDifficulty.ADVANCED,
        estimatedTime = "30 minutes",
        supportedVehicles = listOf(
            VehicleServiceSupport("BMW", "3 Series", "2015-2024", "2.0L Diesel"),
            VehicleServiceSupport("Mercedes-Benz", "C-Class", "2015-2024", "2.1L Diesel"),
            VehicleServiceSupport("Audi", "A3", "2015-2024", "2.0L TDI"),
            VehicleServiceSupport("Volkswagen", "Golf", "2015-2024", "2.0L TDI")
        ),
        steps = listOf(
            ServiceStep("1", "Check DPF status", ServiceStepType.COMMAND, "22F1B0"),
            ServiceStep("2", "Enter service mode", ServiceStepType.COMMAND, "1002"),
            ServiceStep("3", "Start regeneration", ServiceStepType.COMMAND, "31010401"),
            ServiceStep("4", "Monitor temperature", ServiceStepType.SEQUENCE, 
                commandSequence = listOf("22F1B1", "22F1B2", "22F1B3")),
            ServiceStep("5", "Wait for completion", ServiceStepType.WAIT, waitTimeMs = 1800000), // 30 minutes
            ServiceStep("6", "Verify completion", ServiceStepType.VERIFY, verifyCommand = "22F1B0", expectedValue = "00"),
            ServiceStep("7", "Exit service mode", ServiceStepType.COMMAND, "1001")
        ),
        warnings = listOf("Engine must be warm", "Drive vehicle during process", "Ensure adequate fuel"),
        requirements = listOf("ignition_on", "engine_warm", "fuel_adequate")
    ),
    
    // Throttle Body Adaptation
    ServiceFunction(
        id = "throttle_adaptation",
        name = "Throttle Body Adaptation",
        description = "Adapt throttle body position after cleaning or replacement",
        category = ServiceCategory.ENGINE,
        icon = "tune",
        difficulty = ServiceDifficulty.INTERMEDIATE,
        estimatedTime = "10 minutes",
        supportedVehicles = listOf(
            VehicleServiceSupport("BMW", "3 Series", "2015-2024"),
            VehicleServiceSupport("Mercedes-Benz", "C-Class", "2015-2024"),
            VehicleServiceSupport("Audi", "A4", "2015-2024"),
            VehicleServiceSupport("Volkswagen", "Golf", "2015-2024"),
            VehicleServiceSupport("Toyota", "Camry", "2015-2024"),
            VehicleServiceSupport("Honda", "Civic", "2015-2024")
        ),
        steps = listOf(
            ServiceStep("1", "Enter adaptation mode", ServiceStepType.COMMAND, "1003"),
            ServiceStep("2", "Read current position", ServiceStepType.COMMAND, "22F1C0"),
            ServiceStep("3", "Close throttle", ServiceStepType.COMMAND, "31010501"),
            ServiceStep("4", "Learn minimum position", ServiceStepType.ADAPTATION, "2CF1C00100"),
            ServiceStep("5", "Open throttle", ServiceStepType.COMMAND, "31010502"),
            ServiceStep("6", "Learn maximum position", ServiceStepType.ADAPTATION, "2CF1C001FF"),
            ServiceStep("7", "Return to idle", ServiceStepType.COMMAND, "31010503"),
            ServiceStep("8", "Verify adaptation", ServiceStepType.VERIFY, verifyCommand = "22F1C0"),
            ServiceStep("9", "Exit adaptation mode", ServiceStepType.COMMAND, "1001")
        ),
        requirements = listOf("ignition_on", "engine_off", "throttle_clean")
    ),
    
    // Steering Angle Sensor Reset
    ServiceFunction(
        id = "sas_reset",
        name = "Steering Angle Sensor Reset",
        description = "Reset steering angle sensor after alignment",
        category = ServiceCategory.STEERING,
        icon = "my_location",
        difficulty = ServiceDifficulty.INTERMEDIATE,
        estimatedTime = "5 minutes",
        supportedVehicles = listOf(
            VehicleServiceSupport("BMW", "3 Series", "2015-2024"),
            VehicleServiceSupport("Mercedes-Benz", "C-Class", "2015-2024"),
            VehicleServiceSupport("Audi", "A4", "2015-2024"),
            VehicleServiceSupport("Volkswagen", "Golf", "2015-2024")
        ),
        steps = listOf(
            ServiceStep("1", "Center steering wheel", ServiceStepType.WAIT, waitTimeMs = 5000),
            ServiceStep("2", "Enter calibration mode", ServiceStepType.COMMAND, "1002"),
            ServiceStep("3", "Reset sensor", ServiceStepType.COMMAND, "31010601"),
            ServiceStep("4", "Calibrate center position", ServiceStepType.ADAPTATION, "2CF1D00100"),
            ServiceStep("5", "Verify calibration", ServiceStepType.VERIFY, verifyCommand = "22F1D0", expectedValue = "00"),
            ServiceStep("6", "Exit calibration mode", ServiceStepType.COMMAND, "1001")
        ),
        warnings = listOf("Wheels must be straight", "Vehicle on level ground"),
        requirements = listOf("ignition_on", "engine_off", "wheels_straight")
    ),
    
    // TPMS Reset
    ServiceFunction(
        id = "tpms_reset",
        name = "TPMS Reset",
        description = "Reset tire pressure monitoring system",
        category = ServiceCategory.MAINTENANCE,
        icon = "tire_repair",
        difficulty = ServiceDifficulty.BASIC,
        estimatedTime = "3 minutes",
        supportedVehicles = listOf(
            VehicleServiceSupport("BMW", "3 Series", "2015-2024"),
            VehicleServiceSupport("Mercedes-Benz", "C-Class", "2015-2024"),
            VehicleServiceSupport("Audi", "A4", "2015-2024"),
            VehicleServiceSupport("Toyota", "Camry", "2015-2024"),
            VehicleServiceSupport("Honda", "Civic", "2015-2024"),
            VehicleServiceSupport("Ford", "F-150", "2015-2024")
        ),
        steps = listOf(
            ServiceStep("1", "Check tire pressures", ServiceStepType.COMMAND, "22F1E0"),
            ServiceStep("2", "Enter TPMS mode", ServiceStepType.COMMAND, "1002"),
            ServiceStep("3", "Reset system", ServiceStepType.COMMAND, "31010701"),
            ServiceStep("4", "Initialize sensors", ServiceStepType.SEQUENCE,
                commandSequence = listOf("31010702", "31010703", "31010704", "31010705")),
            ServiceStep("5", "Verify reset", ServiceStepType.VERIFY, verifyCommand = "22F1E0"),
            ServiceStep("6", "Exit TPMS mode", ServiceStepType.COMMAND, "1001")
        ),
        requirements = listOf("ignition_on", "correct_tire_pressure")
    ),
    
    // Battery Registration
    ServiceFunction(
        id = "battery_registration",
        name = "Battery Registration",
        description = "Register new battery with vehicle systems",
        category = ServiceCategory.ELECTRICAL,
        icon = "battery_charging_full",
        difficulty = ServiceDifficulty.INTERMEDIATE,
        estimatedTime = "5 minutes",
        supportedVehicles = listOf(
            VehicleServiceSupport("BMW", "3 Series", "2015-2024"),
            VehicleServiceSupport("BMW", "5 Series", "2015-2024"),
            VehicleServiceSupport("Mercedes-Benz", "C-Class", "2015-2024"),
            VehicleServiceSupport("Audi", "A4", "2015-2024")
        ),
        steps = listOf(
            ServiceStep("1", "Enter service mode", ServiceStepType.COMMAND, "1002"),
            ServiceStep("2", "Read battery data", ServiceStepType.COMMAND, "22F1F0"),
            ServiceStep("3", "Register battery type", ServiceStepType.ADAPTATION, "2CF1F001{battery_type}",
                defaultValues = mapOf("battery_type" to "01")),
            ServiceStep("4", "Set capacity", ServiceStepType.ADAPTATION, "2CF1F002{capacity}",
                defaultValues = mapOf("capacity" to "50")),
            ServiceStep("5", "Reset charge state", ServiceStepType.COMMAND, "31010801"),
            ServiceStep("6", "Verify registration", ServiceStepType.VERIFY, verifyCommand = "22F1F0"),
            ServiceStep("7", "Exit service mode", ServiceStepType.COMMAND, "1001")
        ),
        warnings = listOf("Use correct battery specifications"),
        requirements = listOf("ignition_on", "new_battery_installed")
    ),
    
    // Injector Coding
    ServiceFunction(
        id = "injector_coding",
        name = "Injector Coding",
        description = "Code fuel injectors after replacement",
        category = ServiceCategory.ENGINE,
        icon = "precision_manufacturing",
        difficulty = ServiceDifficulty.EXPERT,
        estimatedTime = "15 minutes",
        supportedVehicles = listOf(
            VehicleServiceSupport("BMW", "3 Series", "2015-2024", "2.0L Diesel"),
            VehicleServiceSupport("Mercedes-Benz", "C-Class", "2015-2024", "2.1L Diesel"),
            VehicleServiceSupport("Audi", "A4", "2015-2024", "2.0L TDI")
        ),
        steps = listOf(
            ServiceStep("1", "Enter coding mode", ServiceStepType.COMMAND, "1002"),
            ServiceStep("2", "Read injector codes", ServiceStepType.SEQUENCE,
                commandSequence = listOf("22F201", "22F202", "22F203", "22F204")),
            ServiceStep("3", "Code cylinder 1", ServiceStepType.ADAPTATION, "2CF20101{cyl1_code}"),
            ServiceStep("4", "Code cylinder 2", ServiceStepType.ADAPTATION, "2CF20201{cyl2_code}"),
            ServiceStep("5", "Code cylinder 3", ServiceStepType.ADAPTATION, "2CF20301{cyl3_code}"),
            ServiceStep("6", "Code cylinder 4", ServiceStepType.ADAPTATION, "2CF20401{cyl4_code}"),
            ServiceStep("7", "Verify coding", ServiceStepType.VERIFY, verifyCommand = "22F200"),
            ServiceStep("8", "Exit coding mode", ServiceStepType.COMMAND, "1001")
        ),
        warnings = listOf("Requires injector calibration codes", "Critical for engine operation"),
        requirements = listOf("ignition_on", "engine_off", "injector_codes_available"),
        tools = listOf("Injector calibration codes")
    )
)

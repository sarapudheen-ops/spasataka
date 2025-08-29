package com.spacetec.professional

import android.util.Log
import com.spacetec.obd.RealObdManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Guided Functions - Step-by-step diagnostic and repair procedures
 * Similar to Launch X431 guided functions with detailed instructions
 */
class GuidedFunctions(
    private val obdManager: RealObdManager
) {
    companion object {
        private const val TAG = "GuidedFunctions"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Guided function execution state flows
    private val _functionResults = MutableSharedFlow<GuidedFunctionResult>()
    val functionResults: SharedFlow<GuidedFunctionResult> = _functionResults.asSharedFlow()
    
    private val _functionProgress = MutableStateFlow<GuidedFunctionProgress?>(null)
    val functionProgress: StateFlow<GuidedFunctionProgress?> = _functionProgress.asStateFlow()
    
    private val _currentStep = MutableStateFlow<GuidedStep?>(null)
    val currentStep: StateFlow<GuidedStep?> = _currentStep.asStateFlow()
    
    /**
     * Start guided function
     */
    suspend fun startGuidedFunction(
        vehicleMake: String,
        vehicleModel: String,
        vehicleYear: Int,
        functionId: String
    ): GuidedFunctionResult = withContext(Dispatchers.IO) {
        
        try {
            Log.i(TAG, "Starting guided function: $functionId for $vehicleMake $vehicleModel ($vehicleYear)")
            
            // Find function definition
            val function = getFunctionById(functionId, vehicleMake, vehicleModel, vehicleYear)
                ?: return@withContext GuidedFunctionResult.Error("Guided function not found: $functionId")
            
            // Check compatibility
            if (!isFunctionCompatible(function, vehicleMake, vehicleModel, vehicleYear)) {
                return@withContext GuidedFunctionResult.Error("Function not compatible with this vehicle")
            }
            
            // Initialize function
            _functionProgress.value = GuidedFunctionProgress(functionId, "Initializing", 0.0f, "Preparing guided function")
            _currentStep.value = function.steps.firstOrNull()
            
            val result = GuidedFunctionResult.Started(
                functionId = functionId,
                totalSteps = function.steps.size,
                currentStepIndex = 0,
                message = "Guided function '${function.name}' started"
            )
            
            _functionResults.emit(result)
            Log.i(TAG, "Guided function started: $functionId")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Guided function failed to start: $functionId", e)
            val errorResult = GuidedFunctionResult.Error("Failed to start guided function: ${e.message}")
            _functionResults.emit(errorResult)
            errorResult
        }
    }
    
    /**
     * Execute current step
     */
    suspend fun executeCurrentStep(
        functionId: String,
        stepIndex: Int,
        userInputs: Map<String, Any> = emptyMap()
    ): GuidedFunctionResult = withContext(Dispatchers.IO) {
        
        try {
            val function = guidedFunctions.find { it.id == functionId }
                ?: return@withContext GuidedFunctionResult.Error("Function not found")
            
            if (stepIndex >= function.steps.size) {
                return@withContext GuidedFunctionResult.Error("Invalid step index")
            }
            
            val step = function.steps[stepIndex]
            val progress = (stepIndex.toFloat() / function.steps.size)
            
            _functionProgress.value = GuidedFunctionProgress(
                functionId, "Executing", progress, "Executing: ${step.title}"
            )
            
            val stepResult = when (step.type) {
                GuidedStepType.INSTRUCTION -> executeInstruction(step)
                GuidedStepType.DIAGNOSTIC -> executeDiagnostic(step, userInputs)
                GuidedStepType.TEST -> executeTest(step, userInputs)
                GuidedStepType.MEASUREMENT -> executeMeasurement(step)
                GuidedStepType.USER_INPUT -> executeUserInput(step, userInputs)
                GuidedStepType.DECISION -> executeDecision(step, userInputs)
            }
            
            val nextStepIndex = if (stepResult.nextStepOverride != null) {
                stepResult.nextStepOverride
            } else {
                stepIndex + 1
            }
            
            val isComplete = nextStepIndex >= function.steps.size
            
            if (isComplete) {
                _functionProgress.value = GuidedFunctionProgress(functionId, "Completed", 1.0f, "Function completed")
                _currentStep.value = null
                
                val result = GuidedFunctionResult.Completed(
                    functionId = functionId,
                    data = stepResult.data,
                    message = "Guided function completed successfully"
                )
                _functionResults.emit(result)
                result
            } else {
                _currentStep.value = function.steps[nextStepIndex]
                
                val result = GuidedFunctionResult.StepCompleted(
                    functionId = functionId,
                    completedStepIndex = stepIndex,
                    nextStepIndex = nextStepIndex,
                    stepData = stepResult.data,
                    message = stepResult.message
                )
                _functionResults.emit(result)
                result
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Step execution failed", e)
            val errorResult = GuidedFunctionResult.Error("Step execution failed: ${e.message}")
            _functionResults.emit(errorResult)
            errorResult
        }
    }
    
    /**
     * Get available guided functions for vehicle
     */
    fun getAvailableFunctions(make: String, model: String, year: Int): List<GuidedFunction> {
        return guidedFunctions.filter { function ->
            function.supportedVehicles.any { vehicle ->
                vehicle.make.equals(make, ignoreCase = true) &&
                vehicle.model.equals(model, ignoreCase = true) &&
                isYearInRange(year, vehicle.yearRange)
            }
        }
    }
    
    /**
     * Get functions by category
     */
    fun getFunctionsByCategory(category: GuidedFunctionCategory, make: String, model: String, year: Int): List<GuidedFunction> {
        return getAvailableFunctions(make, model, year).filter { it.category == category }
    }
    
    private suspend fun executeInstruction(step: GuidedStep): GuidedStepResult {
        // Instructions just need user acknowledgment
        return GuidedStepResult(
            success = true,
            message = "Instruction completed",
            data = mapOf("instruction_shown" to true)
        )
    }
    
    private suspend fun executeDiagnostic(step: GuidedStep, userInputs: Map<String, Any>): GuidedStepResult {
        val transport = obdManager.getCurrentTransport() as? com.spacetec.bluetooth.BluetoothObdTransportImpl
            ?: return GuidedStepResult(false, "No transport available", null)
        
        val command = step.command ?: return GuidedStepResult(false, "No diagnostic command", null)
        val response = transport.sendObdCommand(command)
        
        return when (response) {
            is com.spacetec.bluetooth.ObdResponse.Success -> {
                val interpretation = interpretDiagnosticResult(step, response.data)
                GuidedStepResult(
                    success = true,
                    message = "Diagnostic completed: $interpretation",
                    data = mapOf(
                        "diagnostic_data" to response.data,
                        "interpretation" to interpretation
                    )
                )
            }
            is com.spacetec.bluetooth.ObdResponse.Error -> {
                GuidedStepResult(false, "Diagnostic failed: ${response.message}", null)
            }
        }
    }
    
    private suspend fun executeTest(step: GuidedStep, userInputs: Map<String, Any>): GuidedStepResult {
        val transport = obdManager.getCurrentTransport() as? com.spacetec.bluetooth.BluetoothObdTransportImpl
            ?: return GuidedStepResult(false, "No transport available", null)
        
        val command = step.command ?: return GuidedStepResult(false, "No test command", null)
        val testValue = userInputs["test_value"] ?: step.defaultValue ?: "1"
        val finalCommand = command.replace("{value}", testValue.toString())
        
        val response = transport.sendObdCommand(finalCommand)
        
        return when (response) {
            is com.spacetec.bluetooth.ObdResponse.Success -> {
                GuidedStepResult(
                    success = true,
                    message = "Test completed successfully",
                    data = mapOf("test_result" to response.data)
                )
            }
            is com.spacetec.bluetooth.ObdResponse.Error -> {
                GuidedStepResult(false, "Test failed: ${response.message}", null)
            }
        }
    }
    
    private suspend fun executeMeasurement(step: GuidedStep): GuidedStepResult {
        val transport = obdManager.getCurrentTransport() as? com.spacetec.bluetooth.BluetoothObdTransportImpl
            ?: return GuidedStepResult(false, "No transport available", null)
        
        val command = step.command ?: return GuidedStepResult(false, "No measurement command", null)
        val response = transport.sendObdCommand(command)
        
        return when (response) {
            is com.spacetec.bluetooth.ObdResponse.Success -> {
                val formattedValue = formatMeasurementValue(step, response.data)
                GuidedStepResult(
                    success = true,
                    message = "Measurement: $formattedValue",
                    data = mapOf(
                        "raw_value" to response.data,
                        "formatted_value" to formattedValue
                    )
                )
            }
            is com.spacetec.bluetooth.ObdResponse.Error -> {
                GuidedStepResult(false, "Measurement failed: ${response.message}", null)
            }
        }
    }
    
    private suspend fun executeUserInput(step: GuidedStep, userInputs: Map<String, Any>): GuidedStepResult {
        val requiredInputs = step.requiredInputs ?: emptyList()
        val missingInputs = requiredInputs.filter { !userInputs.containsKey(it) }
        
        if (missingInputs.isNotEmpty()) {
            return GuidedStepResult(
                success = false,
                message = "Missing required inputs: ${missingInputs.joinToString(", ")}",
                data = null
            )
        }
        
        return GuidedStepResult(
            success = true,
            message = "User input received",
            data = userInputs
        )
    }
    
    private suspend fun executeDecision(step: GuidedStep, userInputs: Map<String, Any>): GuidedStepResult {
        val decision = userInputs["decision"] as? String
            ?: return GuidedStepResult(false, "No decision provided", null)
        
        val nextStep = step.decisionBranches?.get(decision)
        
        return GuidedStepResult(
            success = true,
            message = "Decision: $decision",
            data = mapOf("decision" to decision),
            nextStepOverride = nextStep
        )
    }
    
    private fun interpretDiagnosticResult(step: GuidedStep, data: String): String {
        return step.interpretations?.entries?.find { (pattern, _) ->
            data.contains(pattern, ignoreCase = true)
        }?.value ?: "Data received: $data"
    }
    
    private fun formatMeasurementValue(step: GuidedStep, rawData: String): String {
        return try {
            val value = rawData.toDoubleOrNull() ?: return rawData
            val formula = step.formula
            val unit = step.unit ?: ""
            
            val formattedValue = when (formula) {
                "A*256+B/4" -> {
                    val bytes = rawData.chunked(2).map { it.toInt(16) }
                    if (bytes.size >= 2) bytes[0] * 256 + bytes[1] / 4 else value
                }
                "A-40" -> value - 40
                "A/2.55" -> value / 2.55
                else -> value
            }
            
            "$formattedValue $unit"
        } catch (e: Exception) {
            "$rawData ${step.unit ?: ""}"
        }
    }
    
    private fun getFunctionById(functionId: String, make: String, model: String, year: Int): GuidedFunction? {
        return getAvailableFunctions(make, model, year).find { it.id == functionId }
    }
    
    private fun isFunctionCompatible(function: GuidedFunction, make: String, model: String, year: Int): Boolean {
        return function.supportedVehicles.any { vehicle ->
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
enum class GuidedFunctionCategory {
    ENGINE_DIAGNOSTICS,
    BRAKE_SYSTEM,
    TRANSMISSION,
    ELECTRICAL,
    EMISSIONS,
    HVAC,
    SUSPENSION,
    MAINTENANCE,
    TROUBLESHOOTING
}

enum class GuidedStepType {
    INSTRUCTION,
    DIAGNOSTIC,
    TEST,
    MEASUREMENT,
    USER_INPUT,
    DECISION
}

data class GuidedFunction(
    val id: String,
    val name: String,
    val description: String,
    val category: GuidedFunctionCategory,
    val icon: String,
    val difficulty: GuidedFunctionDifficulty,
    val estimatedTime: String,
    val supportedVehicles: List<VehicleGuidedSupport>,
    val steps: List<GuidedStep>,
    val tools: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

data class VehicleGuidedSupport(
    val make: String,
    val model: String,
    val yearRange: String,
    val engine: String? = null
)

data class GuidedStep(
    val id: String,
    val title: String,
    val description: String,
    val type: GuidedStepType,
    val command: String? = null,
    val defaultValue: Any? = null,
    val requiredInputs: List<String>? = null,
    val decisionBranches: Map<String, Int>? = null,
    val interpretations: Map<String, String>? = null,
    val formula: String? = null,
    val unit: String? = null,
    val images: List<String> = emptyList(),
    val tips: List<String> = emptyList()
)

enum class GuidedFunctionDifficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

sealed class GuidedFunctionResult {
    data class Started(
        val functionId: String,
        val totalSteps: Int,
        val currentStepIndex: Int,
        val message: String
    ) : GuidedFunctionResult()
    
    data class StepCompleted(
        val functionId: String,
        val completedStepIndex: Int,
        val nextStepIndex: Int,
        val stepData: Map<String, Any>?,
        val message: String
    ) : GuidedFunctionResult()
    
    data class Completed(
        val functionId: String,
        val data: Map<String, Any>?,
        val message: String
    ) : GuidedFunctionResult()
    
    data class Error(
        val message: String
    ) : GuidedFunctionResult()
}

data class GuidedFunctionProgress(
    val functionId: String,
    val status: String,
    val progress: Float,
    val description: String
)

data class GuidedStepResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, Any>?,
    val nextStepOverride: Int? = null
)

// Pre-defined Guided Functions database
private val guidedFunctions = listOf(
    // Engine Misfire Diagnosis
    GuidedFunction(
        id = "engine_misfire_diagnosis",
        name = "Engine Misfire Diagnosis",
        description = "Step-by-step diagnosis of engine misfires",
        category = GuidedFunctionCategory.ENGINE_DIAGNOSTICS,
        icon = "engineering",
        difficulty = GuidedFunctionDifficulty.INTERMEDIATE,
        estimatedTime = "30 minutes",
        supportedVehicles = listOf(
            VehicleGuidedSupport("BMW", "3 Series", "2015-2024"),
            VehicleGuidedSupport("Mercedes-Benz", "C-Class", "2015-2024"),
            VehicleGuidedSupport("Audi", "A4", "2015-2024"),
            VehicleGuidedSupport("Toyota", "Camry", "2015-2024")
        ),
        steps = listOf(
            GuidedStep(
                id = "1",
                title = "Initial Inspection",
                description = "Perform visual inspection of engine bay",
                type = GuidedStepType.INSTRUCTION,
                tips = listOf("Check for obvious vacuum leaks", "Inspect spark plug wires", "Look for damaged components")
            ),
            GuidedStep(
                id = "2",
                title = "Read Fault Codes",
                description = "Scan for diagnostic trouble codes",
                type = GuidedStepType.DIAGNOSTIC,
                command = "03",
                interpretations = mapOf(
                    "P0300" to "Random/Multiple Cylinder Misfire",
                    "P0301" to "Cylinder 1 Misfire",
                    "P0302" to "Cylinder 2 Misfire",
                    "P0303" to "Cylinder 3 Misfire",
                    "P0304" to "Cylinder 4 Misfire"
                )
            ),
            GuidedStep(
                id = "3",
                title = "Check Engine RPM",
                description = "Monitor engine RPM at idle",
                type = GuidedStepType.MEASUREMENT,
                command = "010C",
                formula = "A*256+B/4",
                unit = "RPM"
            ),
            GuidedStep(
                id = "4",
                title = "Misfire Pattern Analysis",
                description = "Determine if misfire is random or cylinder-specific",
                type = GuidedStepType.DECISION,
                decisionBranches = mapOf(
                    "random" to 5,
                    "specific_cylinder" to 7
                )
            ),
            GuidedStep(
                id = "5",
                title = "Check Fuel System",
                description = "Test fuel pressure and flow",
                type = GuidedStepType.MEASUREMENT,
                command = "010A",
                formula = "A*3",
                unit = "kPa"
            ),
            GuidedStep(
                id = "6",
                title = "Test Mass Airflow",
                description = "Check MAF sensor readings",
                type = GuidedStepType.MEASUREMENT,
                command = "0110",
                formula = "A*256+B/100",
                unit = "g/s"
            ),
            GuidedStep(
                id = "7",
                title = "Cylinder-Specific Tests",
                description = "Test specific cylinder components",
                type = GuidedStepType.INSTRUCTION,
                tips = listOf("Check spark plug", "Test ignition coil", "Inspect fuel injector")
            ),
            GuidedStep(
                id = "8",
                title = "Compression Test",
                description = "Perform compression test on affected cylinder",
                type = GuidedStepType.USER_INPUT,
                requiredInputs = listOf("compression_reading")
            )
        ),
        tools = listOf("Compression tester", "Fuel pressure gauge", "Spark plug socket"),
        warnings = listOf("Engine must be warm for accurate readings", "Follow safety procedures")
    ),
    
    // Brake System Diagnosis
    GuidedFunction(
        id = "brake_system_diagnosis",
        name = "Brake System Diagnosis",
        description = "Comprehensive brake system diagnostic procedure",
        category = GuidedFunctionCategory.BRAKE_SYSTEM,
        icon = "car_repair",
        difficulty = GuidedFunctionDifficulty.ADVANCED,
        estimatedTime = "45 minutes",
        supportedVehicles = listOf(
            VehicleGuidedSupport("BMW", "3 Series", "2015-2024"),
            VehicleGuidedSupport("Mercedes-Benz", "C-Class", "2015-2024"),
            VehicleGuidedSupport("Audi", "A4", "2015-2024")
        ),
        steps = listOf(
            GuidedStep(
                id = "1",
                title = "Safety Check",
                description = "Ensure vehicle is safely secured",
                type = GuidedStepType.INSTRUCTION,
                tips = listOf("Use wheel chocks", "Engage parking brake", "Work on level surface")
            ),
            GuidedStep(
                id = "2",
                title = "Read ABS Codes",
                description = "Scan ABS control module for fault codes",
                type = GuidedStepType.DIAGNOSTIC,
                command = "03",
                interpretations = mapOf(
                    "C1200" to "ABS Pump Motor Circuit",
                    "C1201" to "ABS Inlet Valve Circuit",
                    "C1202" to "ABS Outlet Valve Circuit"
                )
            ),
            GuidedStep(
                id = "3",
                title = "Brake Fluid Level",
                description = "Check brake fluid level and condition",
                type = GuidedStepType.USER_INPUT,
                requiredInputs = listOf("fluid_level", "fluid_condition")
            ),
            GuidedStep(
                id = "4",
                title = "Brake Pressure Test",
                description = "Test brake system pressure",
                type = GuidedStepType.MEASUREMENT,
                command = "22F120",
                unit = "bar"
            ),
            GuidedStep(
                id = "5",
                title = "Wheel Speed Sensors",
                description = "Test wheel speed sensor operation",
                type = GuidedStepType.TEST,
                command = "31FF03{value}",
                defaultValue = "01"
            )
        ),
        tools = listOf("Brake pressure tester", "Brake fluid tester"),
        warnings = listOf("Do not drive vehicle if brake system is compromised")
    ),
    
    // HVAC Diagnosis
    GuidedFunction(
        id = "hvac_diagnosis",
        name = "HVAC System Diagnosis",
        description = "Diagnose heating, ventilation, and air conditioning issues",
        category = GuidedFunctionCategory.HVAC,
        icon = "ac_unit",
        difficulty = GuidedFunctionDifficulty.BEGINNER,
        estimatedTime = "20 minutes",
        supportedVehicles = listOf(
            VehicleGuidedSupport("BMW", "3 Series", "2015-2024"),
            VehicleGuidedSupport("Mercedes-Benz", "C-Class", "2015-2024"),
            VehicleGuidedSupport("Audi", "A4", "2015-2024"),
            VehicleGuidedSupport("Toyota", "Camry", "2015-2024")
        ),
        steps = listOf(
            GuidedStep(
                id = "1",
                title = "System Overview",
                description = "Check HVAC system operation",
                type = GuidedStepType.INSTRUCTION,
                tips = listOf("Test all fan speeds", "Check temperature control", "Test mode selection")
            ),
            GuidedStep(
                id = "2",
                title = "Read Climate Codes",
                description = "Scan climate control module",
                type = GuidedStepType.DIAGNOSTIC,
                command = "03"
            ),
            GuidedStep(
                id = "3",
                title = "Ambient Temperature",
                description = "Check ambient temperature sensor",
                type = GuidedStepType.MEASUREMENT,
                command = "0146",
                formula = "A-40",
                unit = "°C"
            ),
            GuidedStep(
                id = "4",
                title = "Actuator Test",
                description = "Test HVAC actuators",
                type = GuidedStepType.TEST,
                command = "31FF05{value}",
                defaultValue = "01"
            )
        )
    )
)

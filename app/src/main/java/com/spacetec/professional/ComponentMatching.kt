package com.spacetec.professional

import android.util.Log
import com.spacetec.obd.RealObdManager
import com.spacetec.obd.sendCommand
import com.spacetec.obd.sendService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// Transport abstraction
interface ObdTransport {
    suspend fun sendObdCommand(command: String): ObdResponse
}

// Unified OBD responses
sealed class ObdResponse {
    data class Success(val rawData: ByteArray) : ObdResponse()
    data class Error(val code: Int, val message: String) : ObdResponse()
}

/**
 * Component Matching and Calibrations - Professional component replacement procedures
 */
class ComponentMatching(
    private val obdManager: RealObdManager,
    private val transport: ObdTransport // inject transport
) {
    companion object {
        private const val TAG = "ComponentMatching"

        // UDS services
        private const val THROTTLE_ADAPTATION_SERVICE = 0x28
        private const val THROTTLE_ADAPTATION_SUBFUNCTION = 0x04
        private const val INJECTOR_CODING_SERVICE = 0x2E
        private const val INJECTOR_CODING_SUBFUNCTION = 0x01
        private const val STEERING_ANGLE_RESET_SERVICE = 0x31
        private const val BATTERY_REGISTRATION_SERVICE = 0x2E
        private const val BATTERY_REGISTRATION_SUBFUNCTION = 0x80
        private const val BI_DIRECTIONAL_CONTROL_SERVICE = 0x2F
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Component matching state flows
    private val _matchingResults = MutableSharedFlow<MatchingResult>()
    val matchingResults: SharedFlow<MatchingResult> = _matchingResults.asSharedFlow()

    private val _matchingProgress = MutableStateFlow<MatchingProgress?>(null)
    val matchingProgress: StateFlow<MatchingProgress?> = _matchingProgress.asStateFlow()

    /**
     * Execute component matching procedure
     */
    suspend fun matchComponent(
        vehicleMake: String,
        vehicleModel: String,
        vehicleYear: Int,
        componentId: String,
        componentData: Map<String, Any> = emptyMap()
    ): MatchingResult = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting component matching: $componentId for $vehicleMake $vehicleModel ($vehicleYear)")

            val component = getComponentById(componentId, vehicleMake, vehicleModel, vehicleYear)
                ?: return@withContext MatchingResult.Error("Component procedure not found: $componentId")

            if (!isComponentCompatible(component, vehicleMake, vehicleModel, vehicleYear)) {
                return@withContext MatchingResult.Error("Component not compatible with this vehicle")
            }

            val missingData = component.requiredData.filter { !componentData.containsKey(it) }
            if (missingData.isNotEmpty()) {
                return@withContext MatchingResult.Error("Missing required data: ${missingData.joinToString(", ")}")
            }

            _matchingProgress.value =
                MatchingProgress(componentId, "Starting", 0.0f, "Initializing component matching")

            val result = executeMatchingProcedure(component, componentData)

            _matchingProgress.value =
                MatchingProgress(componentId, "Completed", 1.0f, "Component matching completed")
            _matchingResults.emit(result)

            Log.i(TAG, "Component matching completed: $componentId")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Component matching failed: $componentId", e)
            val errorResult = MatchingResult.Error("Component matching failed: ${e.message}")
            _matchingResults.emit(errorResult)
            errorResult
        }
    }

    private suspend fun executeMatchingProcedure(
        component: ComponentProcedure,
        componentData: Map<String, Any>
    ): MatchingResult {
        val results = mutableMapOf<String, Any>()
        val totalSteps = component.steps.size

        for ((index, step) in component.steps.withIndex()) {
            val progress = (index.toFloat() / totalSteps) * 0.8f + 0.1f
            _matchingProgress.value =
                MatchingProgress(component.id, step.description, progress, "Executing: ${step.description}")

            val stepResult = executeMatchingStep(step, componentData)
            if (!stepResult.success) {
                return MatchingResult.Error("Step failed: ${step.description} - ${stepResult.error}")
            }

            stepResult.data?.let { results.putAll(it) }
            delay((step.delayMs ?: 1000).toLong())
        }

        return MatchingResult.Success(
            componentId = component.id,
            data = results,
            message = "Component '${component.name}' matched successfully"
        )
    }

    private suspend fun executeMatchingStep(
        step: MatchingStep,
        componentData: Map<String, Any>
    ): MatchingStepResult {
        return when (step.type) {
            MatchingStepType.PROGRAM_COMPONENT -> sendCommand(step, componentData, "programmed")
            MatchingStepType.CALIBRATE -> sendCommand(step, componentData, "calibrated")
            MatchingStepType.ADAPT -> sendCommand(step, componentData, "adapted")
            MatchingStepType.VERIFY -> verifyCommand(step)
            MatchingStepType.INITIALIZE -> sendCommand(step, componentData, "initialized")
            MatchingStepType.FINALIZE -> sendCommand(step, componentData, "finalized")
        }
    }

    private suspend fun sendCommand(
        step: MatchingStep,
        componentData: Map<String, Any>,
        resultKey: String
    ): MatchingStepResult {
        val command = step.command ?: return MatchingStepResult(false, "No command", null)
        val value = step.dataKey?.let { componentData[it] } ?: step.defaultValue
        val finalCommand = if (value != null) command.replace("{value}", value.toString()) else command

        val response = transport.sendObdCommand(finalCommand)
        return when (response) {
            is ObdResponse.Success -> MatchingStepResult(true, null, mapOf(resultKey to (value ?: true)))
            is ObdResponse.Error -> MatchingStepResult(false, response.message, null)
        }
    }

    private suspend fun verifyCommand(step: MatchingStep): MatchingStepResult {
        val command = step.command ?: return MatchingStepResult(false, "No verification command", null)
        val response = transport.sendObdCommand(command)

        return when (response) {
            is ObdResponse.Success -> {
                val isValid = step.expectedValue?.let {
                    val hexResponse = response.rawData.joinToString("") { b -> "%02X".format(b) }
                    hexResponse.contains(it, ignoreCase = true)
                } ?: true
                if (isValid) MatchingStepResult(true, null, mapOf("verification" to "passed"))
                else MatchingStepResult(false, "Verification failed", null)
            }

            is ObdResponse.Error -> MatchingStepResult(false, response.message, null)
        }
    }

    // === Special Procedures ===

    suspend fun performThrottleBodyAdaptation(timeoutMs: Long = 30000): Boolean {
        return try {
            withTimeout(timeoutMs) {
                Log.d(TAG, "Starting throttle body adaptation")
                obdManager.sendCommand("AT SP 3")
                val response = obdManager.sendService(
                    THROTTLE_ADAPTATION_SERVICE,
                    THROTTLE_ADAPTATION_SUBFUNCTION
                )
                response.isPositiveResponse(THROTTLE_ADAPTATION_SERVICE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Throttle adaptation failed", e)
            false
        }
    }

    suspend fun programInjectorCorrection(cylinderNumber: Int, correctionValue: Float): Boolean {
        require(cylinderNumber in 1..8) { "Invalid cylinder number" }
        require(correctionValue in -25f..25f) { "Correction value out of range" }

        return try {
            val data = byteArrayOf(cylinderNumber.toByte(), (correctionValue * 10).toInt().toByte())
            val response = obdManager.sendService(
                INJECTOR_CODING_SERVICE,
                INJECTOR_CODING_SUBFUNCTION,
                data
            )
            response.isPositiveResponse(INJECTOR_CODING_SERVICE)
        } catch (e: Exception) {
            Log.e(TAG, "Injector coding failed", e)
            false
        }
    }

    suspend fun resetSteeringAngleSensor(): Boolean {
        return try {
            val response = obdManager.sendService(
                STEERING_ANGLE_RESET_SERVICE,
                subFunction = null,
                data = "00".toByteArray()
            )
            if (response.isPositiveResponse(STEERING_ANGLE_RESET_SERVICE)) {
                obdManager.sendCommand("AT WS")
                delay(2000)
                obdManager.sendCommand("AT Z")
                true
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Steering angle reset failed", e)
            false
        }
    }

    suspend fun registerBattery(batteryType: String, capacity: Int, serialNumber: String): Boolean {
        require(capacity in 30..200) { "Invalid capacity" }
        return try {
            val data = "$batteryType,$capacity,$serialNumber".toByteArray()
            val response = obdManager.sendService(
                BATTERY_REGISTRATION_SERVICE,
                BATTERY_REGISTRATION_SUBFUNCTION,
                data
            )
            response.isPositiveResponse(BATTERY_REGISTRATION_SERVICE)
        } catch (e: Exception) {
            Log.e(TAG, "Battery registration failed", e)
            false
        }
    }

    suspend fun activateComponent(component: String, duration: Long = 0): Boolean {
        return try {
            val response = obdManager.sendService(
                BI_DIRECTIONAL_CONTROL_SERVICE,
                data = "$component,$duration".toByteArray()
            )
            response.isPositiveResponse(BI_DIRECTIONAL_CONTROL_SERVICE)
        } catch (e: Exception) {
            Log.e(TAG, "Component activation failed", e)
            false
        }
    }

    // === Helpers ===
    private fun getComponentById(componentId: String, make: String, model: String, year: Int): ComponentProcedure? {
        return getAvailableComponents(make, model, year).find { it.id == componentId }
    }

    private fun isComponentCompatible(component: ComponentProcedure, make: String, model: String, year: Int): Boolean {
        return component.supportedVehicles.any { v ->
            v.make.equals(make, true) && v.model.equals(model, true) && isYearInRange(year, v.yearRange)
        }
    }

    private fun isYearInRange(year: Int, range: String): Boolean {
        return try {
            val parts = range.split("-")
            val start = parts[0].toInt()
            val end = if (parts.size > 1) parts[1].toInt() else Int.MAX_VALUE
            year in start..end
        } catch (e: Exception) {
            false
        }
    }

    fun cleanup() {
        scope.cancel()
    }
}

// === Data Models ===

enum class ComponentCategory { ENGINE_COMPONENTS, TRANSMISSION_COMPONENTS, BRAKE_COMPONENTS, ELECTRICAL_COMPONENTS, BODY_COMPONENTS, SUSPENSION_COMPONENTS, HVAC_COMPONENTS }
enum class MatchingStepType { PROGRAM_COMPONENT, CALIBRATE, ADAPT, VERIFY, INITIALIZE, FINALIZE }
enum class ComponentDifficulty { BASIC, INTERMEDIATE, ADVANCED, EXPERT }

data class ComponentProcedure(
    val id: String,
    val name: String,
    val description: String,
    val category: ComponentCategory,
    val icon: String,
    val difficulty: ComponentDifficulty,
    val estimatedTime: String,
    val supportedVehicles: List<VehicleComponentSupport>,
    val steps: List<MatchingStep>,
    val requiredData: List<String>,
    val warnings: List<String> = emptyList(),
    val tools: List<String> = emptyList()
)

data class VehicleComponentSupport(val make: String, val model: String, val yearRange: String, val engine: String? = null)
data class MatchingStep(val id: String, val description: String, val type: MatchingStepType, val command: String? = null, val dataKey: String? = null, val defaultValue: Any? = null, val expectedValue: String? = null, val delayMs: Int? = null)

sealed class MatchingResult {
    data class Success(val componentId: String, val data: Map<String, Any>, val message: String) : MatchingResult()
    data class Error(val message: String) : MatchingResult()
}

data class MatchingProgress(val componentId: String, val status: String, val progress: Float, val description: String)
data class MatchingStepResult(val success: Boolean, val error: String?, val data: Map<String, Any>?)

// === UDS Response Extensions ===
fun com.spacetec.obd.ObdServiceResponse.isPositiveResponse(serviceId: Int): Boolean {
    val raw = data ?: return false
    if (raw.length < 2) return false
    val firstByte = raw.substring(0, 2)
    val expected = String.format("%02X", serviceId + 0x40)
    return firstByte.equals(expected, ignoreCase = true)
}

// Minimal stub to satisfy references; replace with real component catalog
private fun getAvailableComponents(make: String, model: String, year: Int): List<ComponentProcedure> {
    return emptyList()
}

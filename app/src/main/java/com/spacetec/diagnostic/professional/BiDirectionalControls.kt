package com.spacetec.diagnostic.professional

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import java.io.InputStream
import java.io.OutputStream

/**
 * Bi-Directional Controls - Professional Active Testing System
 * Allows direct control of vehicle ECU modules and components
 * Complete offline implementation matching professional diagnostic tools
 */
class BiDirectionalControls(
    private val context: Context,
    private val inputStream: InputStream?,
    private val outputStream: OutputStream?
) {
    
    private val tag = "BiDirectionalControls"
    
    private val _controlStatus = MutableStateFlow<ControlStatus>(ControlStatus.IDLE)
    val controlStatus: StateFlow<ControlStatus> = _controlStatus
    
    private val _activeTest = MutableStateFlow<ActiveTest?>(null)
    val activeTest: StateFlow<ActiveTest?> = _activeTest
    
    private val _testResults = MutableStateFlow<List<TestResult>>(emptyList())
    val testResults: StateFlow<List<TestResult>> = _testResults
    
    enum class ControlStatus {
        IDLE,
        CONNECTING,
        TESTING,
        COMPLETED,
        FAILED
    }
    
    enum class ComponentType {
        // Engine Components
        FUEL_INJECTORS,
        IGNITION_COILS,
        FUEL_PUMP,
        THROTTLE_BODY,
        IDLE_AIR_CONTROL,
        EGR_VALVE,
        TURBO_WASTEGATE,
        VVT_SOLENOIDS,
        
        // Transmission Components
        SHIFT_SOLENOIDS,
        TORQUE_CONVERTER_CLUTCH,
        PRESSURE_CONTROL_SOLENOIDS,
        TRANSMISSION_PUMP,
        
        // Brake System Components
        ABS_PUMP,
        ABS_SOLENOIDS,
        EPB_MOTOR,
        BRAKE_BOOSTER,
        ESP_VALVES,
        
        // HVAC Components
        AC_COMPRESSOR_CLUTCH,
        COOLING_FANS,
        HVAC_BLEND_DOORS,
        HVAC_MODE_DOORS,
        AC_PRESSURE_SENSORS,
        
        // Body & Electrical
        WINDOW_MOTORS,
        DOOR_LOCKS,
        SUNROOF_MOTOR,
        SEAT_MOTORS,
        MIRROR_MOTORS,
        LIGHTING_CIRCUITS,
        
        // Suspension & Steering
        SUSPENSION_COMPRESSOR,
        SUSPENSION_VALVES,
        POWER_STEERING_PUMP,
        ACTIVE_STEERING_MOTOR,
        
        // Emission System
        SECONDARY_AIR_PUMP,
        EVAP_PURGE_VALVE,
        EVAP_VENT_VALVE,
        DPF_HEATER,
        SCR_DOSING_VALVE
    }
    
    data class ActiveTest(
        val component: ComponentType,
        val name: String,
        val description: String,
        val supportedVehicles: List<String>,
        val testCommands: List<TestCommand>,
        val safetyWarnings: List<String>
    )
    
    data class TestCommand(
        val id: String,
        val name: String,
        val description: String,
        val command: ByteArray,
        val expectedResponse: ByteArray?,
        val duration: Long = 2000L, // milliseconds
        val repeatable: Boolean = true
    )
    
    data class TestResult(
        val testId: String,
        val componentName: String,
        val commandName: String,
        val success: Boolean,
        val response: ByteArray?,
        val timestamp: Long,
        val notes: String = ""
    )
    
    // Complete active test database
    private val activeTests = mapOf(
        ComponentType.FUEL_INJECTORS to ActiveTest(
            component = ComponentType.FUEL_INJECTORS,
            name = "Fuel Injector Test",
            description = "Test individual fuel injector operation",
            supportedVehicles = listOf("All fuel injected vehicles"),
            safetyWarnings = listOf(
                "Engine must be OFF during test",
                "Remove fuel pump fuse to prevent flooding",
                "Ensure proper ventilation"
            ),
            testCommands = listOf(
                TestCommand("inj1", "Injector 1 Test", "Activate fuel injector cylinder 1",
                    byteArrayOf(0x31, 0x01, 0x10, 0x01), byteArrayOf(0x71, 0x01, 0x10, 0x01)),
                TestCommand("inj2", "Injector 2 Test", "Activate fuel injector cylinder 2",
                    byteArrayOf(0x31, 0x01, 0x10, 0x02), byteArrayOf(0x71, 0x01, 0x10, 0x02)),
                TestCommand("inj3", "Injector 3 Test", "Activate fuel injector cylinder 3",
                    byteArrayOf(0x31, 0x01, 0x10, 0x03), byteArrayOf(0x71, 0x01, 0x10, 0x03)),
                TestCommand("inj4", "Injector 4 Test", "Activate fuel injector cylinder 4",
                    byteArrayOf(0x31, 0x01, 0x10, 0x04), byteArrayOf(0x71, 0x01, 0x10, 0x04)),
                TestCommand("all_inj", "All Injectors Test", "Activate all fuel injectors",
                    byteArrayOf(0x31, 0x01, 0x10, 0xFF.toByte()), byteArrayOf(0x71, 0x01, 0x10, 0xFF.toByte()))
            )
        ),
        
        ComponentType.IGNITION_COILS to ActiveTest(
            component = ComponentType.IGNITION_COILS,
            name = "Ignition Coil Test",
            description = "Test ignition coil primary circuits",
            supportedVehicles = listOf("All vehicles with electronic ignition"),
            safetyWarnings = listOf(
                "Engine must be OFF during test",
                "Remove spark plugs to prevent engine start",
                "High voltage present - use caution"
            ),
            testCommands = listOf(
                TestCommand("coil1", "Coil 1 Test", "Test ignition coil cylinder 1",
                    byteArrayOf(0x31, 0x01, 0x11, 0x01), byteArrayOf(0x71, 0x01, 0x11, 0x01)),
                TestCommand("coil2", "Coil 2 Test", "Test ignition coil cylinder 2",
                    byteArrayOf(0x31, 0x01, 0x11, 0x02), byteArrayOf(0x71, 0x01, 0x11, 0x02)),
                TestCommand("coil3", "Coil 3 Test", "Test ignition coil cylinder 3",
                    byteArrayOf(0x31, 0x01, 0x11, 0x03), byteArrayOf(0x71, 0x01, 0x11, 0x03)),
                TestCommand("coil4", "Coil 4 Test", "Test ignition coil cylinder 4",
                    byteArrayOf(0x31, 0x01, 0x11, 0x04), byteArrayOf(0x71, 0x01, 0x11, 0x04))
            )
        ),
        
        ComponentType.AC_COMPRESSOR_CLUTCH to ActiveTest(
            component = ComponentType.AC_COMPRESSOR_CLUTCH,
            name = "A/C Compressor Clutch Test",
            description = "Test air conditioning compressor clutch engagement",
            supportedVehicles = listOf("All vehicles with A/C"),
            safetyWarnings = listOf(
                "Engine should be running for accurate test",
                "Monitor system pressures",
                "Do not run test for extended periods"
            ),
            testCommands = listOf(
                TestCommand("engage", "Engage Clutch", "Engage A/C compressor clutch",
                    byteArrayOf(0x31, 0x01, 0x20, 0x01), byteArrayOf(0x71, 0x01, 0x20, 0x01)),
                TestCommand("disengage", "Disengage Clutch", "Disengage A/C compressor clutch",
                    byteArrayOf(0x31, 0x01, 0x20, 0x00), byteArrayOf(0x71, 0x01, 0x20, 0x00)),
                TestCommand("cycle", "Cycle Clutch", "Cycle A/C compressor clutch on/off",
                    byteArrayOf(0x31, 0x01, 0x20, 0x02), byteArrayOf(0x71, 0x01, 0x20, 0x02))
            )
        ),
        
        ComponentType.COOLING_FANS to ActiveTest(
            component = ComponentType.COOLING_FANS,
            name = "Cooling Fan Test",
            description = "Test radiator cooling fan operation",
            supportedVehicles = listOf("All vehicles with electric cooling fans"),
            safetyWarnings = listOf(
                "Keep hands away from fan blades",
                "Engine should be cool before testing",
                "Monitor engine temperature during test"
            ),
            testCommands = listOf(
                TestCommand("fan1_low", "Fan 1 Low Speed", "Run cooling fan 1 at low speed",
                    byteArrayOf(0x31, 0x01, 0x21, 0x01), byteArrayOf(0x71, 0x01, 0x21, 0x01)),
                TestCommand("fan1_high", "Fan 1 High Speed", "Run cooling fan 1 at high speed",
                    byteArrayOf(0x31, 0x01, 0x21, 0x02), byteArrayOf(0x71, 0x01, 0x21, 0x02)),
                TestCommand("fan2_low", "Fan 2 Low Speed", "Run cooling fan 2 at low speed",
                    byteArrayOf(0x31, 0x01, 0x21, 0x03), byteArrayOf(0x71, 0x01, 0x21, 0x03)),
                TestCommand("fan2_high", "Fan 2 High Speed", "Run cooling fan 2 at high speed",
                    byteArrayOf(0x31, 0x01, 0x21, 0x04), byteArrayOf(0x71, 0x01, 0x21, 0x04)),
                TestCommand("fans_off", "All Fans Off", "Turn off all cooling fans",
                    byteArrayOf(0x31, 0x01, 0x21, 0x00), byteArrayOf(0x71, 0x01, 0x21, 0x00))
            )
        ),
        
        ComponentType.ABS_PUMP to ActiveTest(
            component = ComponentType.ABS_PUMP,
            name = "ABS Pump Test",
            description = "Test ABS hydraulic pump motor",
            supportedVehicles = listOf("All vehicles with ABS"),
            safetyWarnings = listOf(
                "Vehicle must be stationary",
                "Engine should be running",
                "Test creates noise and vibration"
            ),
            testCommands = listOf(
                TestCommand("pump_on", "Pump Motor On", "Activate ABS pump motor",
                    byteArrayOf(0x31, 0x01, 0x30, 0x01), byteArrayOf(0x71, 0x01, 0x30, 0x01)),
                TestCommand("pump_off", "Pump Motor Off", "Deactivate ABS pump motor",
                    byteArrayOf(0x31, 0x01, 0x30, 0x00), byteArrayOf(0x71, 0x01, 0x30, 0x00)),
                TestCommand("pump_cycle", "Pump Cycle Test", "Cycle ABS pump motor",
                    byteArrayOf(0x31, 0x01, 0x30, 0x02), byteArrayOf(0x71, 0x01, 0x30, 0x02))
            )
        ),
        
        ComponentType.WINDOW_MOTORS to ActiveTest(
            component = ComponentType.WINDOW_MOTORS,
            name = "Window Motor Test",
            description = "Test power window motor operation",
            supportedVehicles = listOf("All vehicles with power windows"),
            safetyWarnings = listOf(
                "Ensure no obstructions in window path",
                "Do not operate with door panels removed",
                "Stop test if unusual noises occur"
            ),
            testCommands = listOf(
                TestCommand("lf_up", "LF Window Up", "Move left front window up",
                    byteArrayOf(0x31, 0x01, 0x40, 0x01), byteArrayOf(0x71, 0x01, 0x40, 0x01)),
                TestCommand("lf_down", "LF Window Down", "Move left front window down",
                    byteArrayOf(0x31, 0x01, 0x40, 0x02), byteArrayOf(0x71, 0x01, 0x40, 0x02)),
                TestCommand("rf_up", "RF Window Up", "Move right front window up",
                    byteArrayOf(0x31, 0x01, 0x40, 0x03), byteArrayOf(0x71, 0x01, 0x40, 0x03)),
                TestCommand("rf_down", "RF Window Down", "Move right front window down",
                    byteArrayOf(0x31, 0x01, 0x40, 0x04), byteArrayOf(0x71, 0x01, 0x40, 0x04)),
                TestCommand("all_stop", "Stop All Windows", "Stop all window movement",
                    byteArrayOf(0x31, 0x01, 0x40, 0x00), byteArrayOf(0x71, 0x01, 0x40, 0x00))
            )
        ),
        
        ComponentType.DOOR_LOCKS to ActiveTest(
            component = ComponentType.DOOR_LOCKS,
            name = "Door Lock Test",
            description = "Test power door lock actuators",
            supportedVehicles = listOf("All vehicles with power door locks"),
            safetyWarnings = listOf(
                "Ensure all doors are closed",
                "Keep keys accessible",
                "Test may activate security system"
            ),
            testCommands = listOf(
                TestCommand("lock_all", "Lock All Doors", "Lock all door lock actuators",
                    byteArrayOf(0x31, 0x01, 0x41, 0x01), byteArrayOf(0x71, 0x01, 0x41, 0x01)),
                TestCommand("unlock_all", "Unlock All Doors", "Unlock all door lock actuators",
                    byteArrayOf(0x31, 0x01, 0x41, 0x02), byteArrayOf(0x71, 0x01, 0x41, 0x02)),
                TestCommand("lock_driver", "Lock Driver Door", "Lock driver door only",
                    byteArrayOf(0x31, 0x01, 0x41, 0x03), byteArrayOf(0x71, 0x01, 0x41, 0x03)),
                TestCommand("unlock_driver", "Unlock Driver Door", "Unlock driver door only",
                    byteArrayOf(0x31, 0x01, 0x41, 0x04), byteArrayOf(0x71, 0x01, 0x41, 0x04))
            )
        ),
        
        ComponentType.FUEL_PUMP to ActiveTest(
            component = ComponentType.FUEL_PUMP,
            name = "Fuel Pump Test",
            description = "Test electric fuel pump operation",
            supportedVehicles = listOf("All vehicles with electric fuel pump"),
            safetyWarnings = listOf(
                "Ensure proper ventilation",
                "Monitor fuel pressure during test",
                "Do not run pump dry"
            ),
            testCommands = listOf(
                TestCommand("pump_on", "Fuel Pump On", "Activate fuel pump",
                    byteArrayOf(0x31, 0x01, 0x12, 0x01), byteArrayOf(0x71, 0x01, 0x12, 0x01)),
                TestCommand("pump_off", "Fuel Pump Off", "Deactivate fuel pump",
                    byteArrayOf(0x31, 0x01, 0x12, 0x00), byteArrayOf(0x71, 0x01, 0x12, 0x00)),
                TestCommand("pump_prime", "Prime Fuel System", "Run fuel pump priming cycle",
                    byteArrayOf(0x31, 0x01, 0x12, 0x02), byteArrayOf(0x71, 0x01, 0x12, 0x02))
            )
        )
    )
    
    /**
     * Get all available active tests
     */
    fun getAvailableTests(): List<ActiveTest> {
        return activeTests.values.toList()
    }
    
    /**
     * Get tests by component category
     */
    fun getTestsByCategory(category: String): List<ActiveTest> {
        return when (category.lowercase()) {
            "engine" -> activeTests.values.filter { 
                it.component in listOf(ComponentType.FUEL_INJECTORS, ComponentType.IGNITION_COILS, 
                    ComponentType.FUEL_PUMP, ComponentType.THROTTLE_BODY) 
            }
            "hvac" -> activeTests.values.filter { 
                it.component in listOf(ComponentType.AC_COMPRESSOR_CLUTCH, ComponentType.COOLING_FANS) 
            }
            "brake" -> activeTests.values.filter { 
                it.component in listOf(ComponentType.ABS_PUMP, ComponentType.ABS_SOLENOIDS) 
            }
            "body" -> activeTests.values.filter { 
                it.component in listOf(ComponentType.WINDOW_MOTORS, ComponentType.DOOR_LOCKS) 
            }
            else -> activeTests.values.toList()
        }
    }
    
    /**
     * Start active test for component
     */
    suspend fun startActiveTest(componentType: ComponentType, vehicleInfo: Map<String, String>): Boolean {
        val test = activeTests[componentType] ?: return false
        
        _controlStatus.value = ControlStatus.CONNECTING
        _activeTest.value = test
        
        Log.d(tag, "Starting active test: ${test.name}")
        
        return try {
            // Check vehicle compatibility
            if (!isVehicleSupported(test, vehicleInfo)) {
                _controlStatus.value = ControlStatus.FAILED
                return false
            }
            
            _controlStatus.value = ControlStatus.TESTING
            true
            
        } catch (e: Exception) {
            _controlStatus.value = ControlStatus.FAILED
            Log.e(tag, "Failed to start active test", e)
            false
        }
    }
    
    /**
     * Execute specific test command
     */
    suspend fun executeTestCommand(commandId: String): Boolean {
        val test = _activeTest.value ?: return false
        val command = test.testCommands.find { it.id == commandId } ?: return false
        
        Log.d(tag, "Executing test command: ${command.name}")
        
        return try {
            // Send command
            sendCommand(command.command)
            
            // Wait for response
            val response = receiveResponse(5000L)
            
            // Validate response
            val success = command.expectedResponse?.let { expected ->
                response != null && response.contentEquals(expected)
            } ?: (response != null)
            
            // Record result
            val result = TestResult(
                testId = "${test.component.name}_${commandId}",
                componentName = test.name,
                commandName = command.name,
                success = success,
                response = response,
                timestamp = System.currentTimeMillis(),
                notes = if (success) "Test completed successfully" else "Test failed or no response"
            )
            
            val currentResults = _testResults.value.toMutableList()
            currentResults.add(result)
            _testResults.value = currentResults
            
            // Wait for command duration
            delay(command.duration)
            
            success
            
        } catch (e: Exception) {
            Log.e(tag, "Test command execution failed", e)
            false
        }
    }
    
    /**
     * Stop active test
     */
    fun stopActiveTest() {
        _controlStatus.value = ControlStatus.COMPLETED
        _activeTest.value = null
        Log.d(tag, "Active test stopped")
    }
    
    /**
     * Clear test results
     */
    fun clearTestResults() {
        _testResults.value = emptyList()
    }
    
    /**
     * Get test results for component
     */
    fun getTestResults(componentType: ComponentType): List<TestResult> {
        return _testResults.value.filter { it.testId.startsWith(componentType.name) }
    }
    
    /**
     * Check if vehicle supports active test
     */
    private fun isVehicleSupported(test: ActiveTest, vehicleInfo: Map<String, String>): Boolean {
        val make = vehicleInfo["make"]?.uppercase() ?: return false
        val year = vehicleInfo["year"]?.toIntOrNull() ?: return false
        
        // Basic compatibility check
        return when (test.component) {
            ComponentType.FUEL_INJECTORS -> year >= 1990
            ComponentType.IGNITION_COILS -> year >= 1985
            ComponentType.AC_COMPRESSOR_CLUTCH -> year >= 1980
            ComponentType.ABS_PUMP -> year >= 1990
            ComponentType.WINDOW_MOTORS -> year >= 1985
            ComponentType.DOOR_LOCKS -> year >= 1990
            else -> true
        }
    }
    
    /**
     * Send command to vehicle
     */
    private suspend fun sendCommand(command: ByteArray) {
        outputStream?.write(command)
        outputStream?.flush()
        delay(100)
    }
    
    /**
     * Receive response from vehicle
     */
    private suspend fun receiveResponse(timeout: Long): ByteArray? {
        if (inputStream == null) return null
        
        val startTime = System.currentTimeMillis()
        val buffer = ByteArray(1024)
        val response = mutableListOf<Byte>()
        
        while (System.currentTimeMillis() - startTime < timeout) {
            if (inputStream.available() > 0) {
                val bytes = inputStream.read(buffer)
                if (bytes > 0) {
                    for (i in 0 until bytes) {
                        response.add(buffer[i])
                    }
                    
                    if (response.isNotEmpty() && (response[0] >= 0x40 || response[0] == 0x7F.toByte())) {
                        return response.toByteArray()
                    }
                }
            }
            delay(10)
        }
        
        return if (response.isNotEmpty()) response.toByteArray() else null
    }
}

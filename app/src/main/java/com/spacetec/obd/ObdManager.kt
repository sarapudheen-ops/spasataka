package com.spacetec.obd

import com.spacetec.bluetooth.SpaceBluetoothManager
import com.spacetec.vehicle.VehicleData
import com.spacetec.vin.VinDecoder
import com.spacetec.vin.VehicleInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

/**
 * Manages OBD-II communication and data collection
 */
class ObdManager(private val bluetoothManager: SpaceBluetoothManager) {
    
    private val _vehicleData = MutableStateFlow(VehicleData())
    val vehicleData: StateFlow<VehicleData> = _vehicleData.asStateFlow()
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _vehicleInfo = MutableStateFlow<VehicleInfo?>(null)
    val vehicleInfo: StateFlow<VehicleInfo?> = _vehicleInfo.asStateFlow()
    
    private val _vinNumber = MutableStateFlow<String?>(null)
    val vinNumber: StateFlow<String?> = _vinNumber.asStateFlow()
    
    private val _ecuName = MutableStateFlow<String?>(null)
    val ecuName: StateFlow<String?> = _ecuName.asStateFlow()
    
    private var monitoringJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Initialize OBD-II connection
     */
    suspend fun initialize(): Boolean {
        if (!bluetoothManager.isConnected()) {
            _errorMessage.value = "🛸 No satellite connection detected"
            return false
        }
        
        try {
            // Send initialization commands
            val initCommands = ObdProtocol.getInitCommands()
            for (command in initCommands) {
                if (!sendCommand(command)) {
                    _errorMessage.value = "🔧 Failed to initialize OBD-II protocol"
                    return false
                }
                delay(100); // Wait between commands
            }
            
            // Test basic connectivity with a simple PID
            val testResponse = sendCommandAndWaitForResponse(ObdProtocol.PID.ENGINE_RPM)
            if (testResponse == null || ObdProtocol.isErrorResponse(testResponse)) {
                _errorMessage.value = "⚠️ Vehicle computer not responding"
                return false
            }
            
            // Auto-detect VIN number and vehicle information
            detectVehicleInformation()
            
            _isInitialized.value = true
            _errorMessage.value = null
            return true
            
        } catch (e: Exception) {
            _errorMessage.value = "🚨 Initialization failed: ${e.message}"
            return false
        }
    }
    
    /**
     * Start continuous vehicle data monitoring
     */
    fun startMonitoring() {
        if (!_isInitialized.value) {
            _errorMessage.value = "🛰️ OBD-II not initialized"
            return
        }
        
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    collectVehicleData()
                    delay(1000); // Update every second
                } catch (e: Exception) {
                    _errorMessage.value = "📡 Data transmission error: ${e.message}"
                    delay(2000); // Wait longer on error
                }
            }
        }
    }
    
    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    /**
     * Collect all vehicle data from OBD-II
     */
    private suspend fun collectVehicleData() {
        val currentData = _vehicleData.value
        var updatedData = currentData.copy(timestamp = System.currentTimeMillis())
        
        // Collect RPM (Engine RPM)
        sendCommandAndWaitForResponse(ObdProtocol.PID.ENGINE_RPM)?.let { response ->
            if (ObdProtocol.isValidResponse(response, ObdProtocol.PID.ENGINE_RPM)) {
                val rpm = ObdProtocol.parseRpm(response)
                updatedData = updatedData.copy(
                    rpm = rpm,
                    isEngineRunning = rpm > 0
                )
            }
        }
        
        delay(50); // Small delay between commands
        
        // Collect Speed
        sendCommandAndWaitForResponse(ObdProtocol.PID.VEHICLE_SPEED)?.let { response ->
            if (ObdProtocol.isValidResponse(response, ObdProtocol.PID.VEHICLE_SPEED)) {
                val speed = ObdProtocol.parseSpeed(response)
                updatedData = updatedData.copy(speed = speed)
            }
        }
        
        delay(50)
        
        // Collect Coolant Temperature
        sendCommandAndWaitForResponse(ObdProtocol.PID.COOLANT_TEMP)?.let { response ->
            if (ObdProtocol.isValidResponse(response, ObdProtocol.PID.COOLANT_TEMP)) {
                val temp = ObdProtocol.parseCoolantTemp(response)
                updatedData = updatedData.copy(coolantTemp = temp)
            }
        }
        
        delay(50)
        
        // Collect Fuel Level
        sendCommandAndWaitForResponse(ObdProtocol.PID.FUEL_LEVEL)?.let { response ->
            if (ObdProtocol.isValidResponse(response, ObdProtocol.PID.FUEL_LEVEL)) {
                val fuel = ObdProtocol.parseFuelLevel(response)
                updatedData = updatedData.copy(fuelLevel = fuel)
            }
        }
        
        _vehicleData.value = updatedData
    }
    
    /**
     * Send command and wait for response with enhanced timeout handling
     */
    private suspend fun sendCommandAndWaitForResponse(command: String): String? {
        return try {
            // Use enhanced command sending with proper response reading
            val response = bluetoothManager.sendCommandAndReadResponse(command)
            
            // Validate response format
            if (response != null && response.startsWith("41") && !ObdProtocol.isErrorResponse(response)) {
                response.trim()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Send command without waiting for response
     */
    private fun sendCommand(command: String): Boolean {
        return bluetoothManager.sendData(command)
    }
    
    /**
     * Get current vehicle status message
     */
    fun getCurrentStatusMessage(): String {
        return _vehicleData.value.getStatusMessage()
    }
    
    /**
     * Check if vehicle has critical alerts
     */
    fun hasCriticalAlerts(): Boolean {
        return _vehicleData.value.hasCriticalAlerts()
    }
    
    /**
     * Auto-detect VIN and vehicle information
     */
    private suspend fun detectVehicleInformation() {
        try {
            // Request VIN number
            val vinResponses = mutableListOf<String>()
            
            // VIN is usually returned in multiple frames, collect all responses
            for (i in 1..5) { // Try up to 5 frames
                val response = sendCommandAndWaitForResponse("0902${"0$i".takeLast(2)}")
                if (response != null && !ObdProtocol.isErrorResponse(response)) {
                    vinResponses.add(response)
                    delay(100)
                } else {
                    break
                }
            }
            
            // Parse VIN from collected responses
            if (vinResponses.isNotEmpty()) {
                val vin = ObdProtocol.parseVin(vinResponses)
                if (vin != null && VinDecoder.validateVin(vin)) {
                    _vinNumber.value = vin
                    
                    // Decode comprehensive vehicle information
                    val vehicleInfo = VinDecoder.decodeVin(vin)
                    _vehicleInfo.value = vehicleInfo
                    
                    _errorMessage.value = "🚀 Spacecraft identified: ${vehicleInfo?.getVehicleDescription() ?: vin}"
                }
            }
            
            delay(200)
            
            // Request ECU name
            val ecuResponse = sendCommandAndWaitForResponse(ObdProtocol.PID.ECU_NAME)
            if (ecuResponse != null && !ObdProtocol.isErrorResponse(ecuResponse)) {
                val ecuName = ObdProtocol.parseEcuName(ecuResponse)
                if (!ecuName.isNullOrBlank()) {
                    _ecuName.value = ecuName
                }
            }
            
        } catch (e: Exception) {
            _errorMessage.value = "📡 Vehicle identification partially failed: ${e.message}"
        }
    }
    
    /**
     * Get comprehensive vehicle summary
     */
    fun getVehicleSummary(): String {
        val info = _vehicleInfo.value
        val vin = _vinNumber.value
        val ecu = _ecuName.value
        
        return buildString {
            if (info != null) {
                appendLine("🚗 ${info.getVehicleDescription()}")
                appendLine("🏭 Origin: ${info.country}")
                appendLine("🔧 Engine: ${info.engineType}")
                appendLine("🚙 Type: ${info.bodyStyle}")
                appendLine("⚙️ Drive: ${info.driveType}")
                appendLine("🌟 Class: ${info.getSpaceClassification()}")
            } else if (vin != null) {
                appendLine("🚗 VIN: $vin")
            }
            
            if (ecu != null) {
                appendLine("🖥️ ECU: $ecu")
            }
            
            if (info == null && vin == null && ecu == null) {
                appendLine("🛸 Unknown Spacecraft")
                appendLine("📡 Vehicle identification unavailable")
            }
        }.trim()
    }
    
    /**
     * Get vehicle age if available
     */
    fun getVehicleAge(): Int? {
        return _vehicleInfo.value?.getVehicleAge()
    }
    
    /**
     * Check if vehicle information is available
     */
    fun hasVehicleInfo(): Boolean {
        return _vehicleInfo.value != null || _vinNumber.value != null
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopMonitoring()
    }
}

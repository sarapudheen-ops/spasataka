package com.spacetec.streaming

import android.util.Log
import com.spacetec.bluetooth.BluetoothObdTransportImpl
import com.spacetec.bluetooth.ObdResponse
import com.spacetec.obd.ObdProtocol
import com.spacetec.protocols.AdvancedProtocols
import com.spacetec.vehicle.VehicleData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.collections.mutableMapOf

/**
 * Advanced real-time data streaming from multiple ECUs
 * Supports simultaneous monitoring of engine, transmission, ABS, airbag, and other systems
 */
class LiveDataStreamer(
    private val transport: BluetoothObdTransportImpl
) {
    companion object {
        private const val TAG = "LiveDataStreamer"
        private const val STREAM_INTERVAL = 100L // 10Hz update rate
        private const val ECU_TIMEOUT = 2000L
        private const val MAX_CONCURRENT_REQUESTS = 8
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Data streams for different ECU systems
    private val _engineData = MutableSharedFlow<EngineData>()
    val engineData: SharedFlow<EngineData> = _engineData.asSharedFlow()
    
    private val _transmissionData = MutableSharedFlow<TransmissionData>()
    val transmissionData: SharedFlow<TransmissionData> = _transmissionData.asSharedFlow()
    
    private val _absData = MutableSharedFlow<AbsData>()
    val absData: SharedFlow<AbsData> = _absData.asSharedFlow()
    
    private val _airbagData = MutableSharedFlow<AirbagData>()
    val airbagData: SharedFlow<AirbagData> = _airbagData.asSharedFlow()
    
    private val _climateData = MutableSharedFlow<ClimateData>()
    val climateData: SharedFlow<ClimateData> = _climateData.asSharedFlow()
    
    private val _bodyControlData = MutableSharedFlow<BodyControlData>()
    val bodyControlData: SharedFlow<BodyControlData> = _bodyControlData.asSharedFlow()
    
    private val _combinedVehicleData = MutableSharedFlow<VehicleData>()
    val combinedVehicleData: SharedFlow<VehicleData> = _combinedVehicleData.asSharedFlow()
    
    // Streaming control
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()
    
    private val _streamingErrors = MutableSharedFlow<StreamingError>()
    val streamingErrors: SharedFlow<StreamingError> = _streamingErrors.asSharedFlow()
    
    private var streamingJob: Job? = null
    private val ecuAddresses = mutableMapOf<String, Int>()
    private val supportedPids = mutableSetOf<String>()
    
    /**
     * Start live data streaming from all available ECUs
     */
    suspend fun startStreaming(
        ecuFilter: Set<String> = setOf("ENGINE", "TRANSMISSION", "ABS", "AIRBAG", "CLIMATE", "BODY"),
        streamRate: Long = STREAM_INTERVAL
    ): Boolean {
        if (_isStreaming.value) {
            Log.w(TAG, "Streaming already active")
            return true
        }
        
        try {
            Log.i(TAG, "Starting live data streaming...")
            
            // Discover available ECUs
            val discoveredEcus = discoverEcus()
            if (discoveredEcus.isEmpty()) {
                Log.w(TAG, "No ECUs discovered")
                return false
            }
            
            // Get supported PIDs for each ECU
            for (ecu in discoveredEcus) {
                val pids = getSupportedPidsForEcu(ecu)
                supportedPids.addAll(pids)
                Log.i(TAG, "ECU ${ecu.name} supports ${pids.size} PIDs")
            }
            
            _isStreaming.value = true
            
            // Start streaming job
            streamingJob = scope.launch {
                streamLiveData(discoveredEcus.filter { ecuFilter.contains(it.type) }, streamRate)
            }
            
            Log.i(TAG, "Live data streaming started for ${discoveredEcus.size} ECUs")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start streaming", e)
            _streamingErrors.emit(StreamingError("Failed to start streaming: ${e.message}", e))
            return false
        }
    }
    
    /**
     * Stop live data streaming
     */
    fun stopStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        _isStreaming.value = false
        Log.i(TAG, "Live data streaming stopped")
    }
    
    /**
     * Discover available ECUs on the vehicle network
     */
    private suspend fun discoverEcus(): List<EcuInfo> {
        val discoveredEcus = mutableListOf<EcuInfo>()
        
        // Common ECU addresses and their typical functions
        val commonEcuAddresses = mapOf(
            0x7E0 to EcuInfo("ENGINE", "Engine Control Module", 0x7E0, 0x7E8),
            0x7E1 to EcuInfo("TRANSMISSION", "Transmission Control Module", 0x7E1, 0x7E9),
            0x7E2 to EcuInfo("ABS", "Anti-lock Braking System", 0x7E2, 0x7EA),
            0x7E3 to EcuInfo("AIRBAG", "Supplemental Restraint System", 0x7E3, 0x7EB),
            0x7E4 to EcuInfo("CLIMATE", "Climate Control Module", 0x7E4, 0x7EC),
            0x7E5 to EcuInfo("BODY", "Body Control Module", 0x7E5, 0x7ED),
            0x7DF to EcuInfo("BROADCAST", "OBD-II Broadcast", 0x7DF, 0x7E8)
        )
        
        for ((address, ecuInfo) in commonEcuAddresses) {
            try {
                // Send a simple diagnostic request to check if ECU responds
                val response = sendEcuRequest(ecuInfo, "0100") // Request supported PIDs
                if (response != null && response is ObdResponse.Success) {
                    discoveredEcus.add(ecuInfo)
                    ecuAddresses[ecuInfo.type] = address
                    Log.d(TAG, "Discovered ECU: ${ecuInfo.name} at address 0x${address.toString(16)}")
                }
            } catch (e: Exception) {
                Log.d(TAG, "ECU at address 0x${address.toString(16)} not responding")
            }
            
            delay(50) // Small delay between discovery attempts
        }
        
        return discoveredEcus
    }
    
    /**
     * Get supported PIDs for a specific ECU
     */
    private suspend fun getSupportedPidsForEcu(ecu: EcuInfo): Set<String> {
        val supportedPids = mutableSetOf<String>()
        
        try {
            // Check PIDs 01-20
            val response1 = sendEcuRequest(ecu, "0100")
            if (response1 is ObdResponse.Success) {
                parseSupportedPids(response1.data, 1, supportedPids)
            }
            
            // Check PIDs 21-40
            val response2 = sendEcuRequest(ecu, "0120")
            if (response2 is ObdResponse.Success) {
                parseSupportedPids(response2.data, 21, supportedPids)
            }
            
            // Check PIDs 41-60
            val response3 = sendEcuRequest(ecu, "0140")
            if (response3 is ObdResponse.Success) {
                parseSupportedPids(response3.data, 41, supportedPids)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get supported PIDs for ${ecu.name}", e)
        }
        
        return supportedPids
    }
    
    /**
     * Parse supported PIDs from response data
     */
    private fun parseSupportedPids(response: String, startPid: Int, supportedPids: MutableSet<String>) {
        try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.length >= 10) {
                val dataHex = cleaned.substring(4, 12)
                val pidSupport = dataHex.toLong(16)
                
                for (i in 0..31) {
                    if ((pidSupport and (1L shl (31 - i))) != 0L) {
                        val pidNumber = startPid + i
                        supportedPids.add(String.format("%02X", pidNumber))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse supported PIDs", e)
        }
    }
    
    /**
     * Main streaming loop
     */
    private suspend fun streamLiveData(ecus: List<EcuInfo>, streamRate: Long) {
        val pidQueues = createPidQueues(ecus)
        var lastCombinedUpdate = System.currentTimeMillis()
        val combinedData = VehicleData()
        
        while (_isStreaming.value) {
            val startTime = System.currentTimeMillis()
            
            // Process requests for each ECU concurrently
            val jobs = ecus.map { ecu ->
                scope.async {
                    processEcuData(ecu, pidQueues[ecu.type] ?: emptyList())
                }
            }
            
            // Wait for all ECU data collection to complete
            jobs.awaitAll().forEach { /* Process results if needed */ }
            
            // Update combined vehicle data every 500ms
            if (System.currentTimeMillis() - lastCombinedUpdate > 500) {
                updateCombinedVehicleData(combinedData)
                _combinedVehicleData.emit(combinedData.copy(timestamp = System.currentTimeMillis()))
                lastCombinedUpdate = System.currentTimeMillis()
            }
            
            // Maintain streaming rate
            val elapsed = System.currentTimeMillis() - startTime
            val sleepTime = maxOf(0, streamRate - elapsed)
            if (sleepTime > 0) {
                delay(sleepTime)
            }
        }
    }
    
    /**
     * Create PID request queues for each ECU type
     */
    private fun createPidQueues(ecus: List<EcuInfo>): Map<String, List<String>> {
        val pidQueues = mutableMapOf<String, List<String>>()
        
        for (ecu in ecus) {
            pidQueues[ecu.type] = when (ecu.type) {
                "ENGINE" -> listOf("010C", "010D", "0105", "0106", "0107", "010B", "010F", "0111", "0114", "011F")
                "TRANSMISSION" -> listOf("0122", "0123", "0124", "0125", "0126", "0127", "0128", "0129")
                "ABS" -> listOf("0130", "0131", "0132", "0133", "0134", "0135")
                "AIRBAG" -> listOf("0140", "0141", "0142", "0143", "0144")
                "CLIMATE" -> listOf("0150", "0151", "0152", "0153", "0154")
                "BODY" -> listOf("0160", "0161", "0162", "0163", "0164")
                else -> listOf("0100") // Default to supported PIDs request
            }.filter { supportedPids.contains(it.substring(2)) }
        }
        
        return pidQueues
    }
    
    /**
     * Process data for a specific ECU
     */
    private suspend fun processEcuData(ecu: EcuInfo, pids: List<String>) {
        if (pids.isEmpty()) return
        
        try {
            val ecuData = mutableMapOf<String, Any>()
            
            // Request data from ECU using round-robin approach
            for (pid in pids.take(3)) { // Limit concurrent requests per ECU
                try {
                    val response = sendEcuRequest(ecu, pid)
                    if (response is ObdResponse.Success) {
                        val parsedValue = parseEcuResponse(pid, response.data)
                        if (parsedValue != null) {
                            ecuData[pid] = parsedValue
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get PID $pid from ${ecu.name}", e)
                }
            }
            
            // Emit ECU-specific data
            when (ecu.type) {
                "ENGINE" -> emitEngineData(ecuData)
                "TRANSMISSION" -> emitTransmissionData(ecuData)
                "ABS" -> emitAbsData(ecuData)
                "AIRBAG" -> emitAirbagData(ecuData)
                "CLIMATE" -> emitClimateData(ecuData)
                "BODY" -> emitBodyControlData(ecuData)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing data for ${ecu.name}", e)
            _streamingErrors.emit(StreamingError("ECU ${ecu.name} error: ${e.message}", e))
        }
    }
    
    /**
     * Send request to specific ECU
     */
    private suspend fun sendEcuRequest(ecu: EcuInfo, command: String): ObdResponse? {
        return withTimeoutOrNull(ECU_TIMEOUT) {
            transport.sendObdCommand(command)
        }
    }
    
    /**
     * Parse ECU response based on PID
     */
    private fun parseEcuResponse(pid: String, response: String): Any? {
        return try {
            when (pid) {
                "010C" -> ObdProtocol.parseRpm(response)
                "010D" -> ObdProtocol.parseSpeed(response)
                "0105" -> ObdProtocol.parseCoolantTemp(response)
                "0106" -> parseShortTermFuelTrim(response)
                "0107" -> parseLongTermFuelTrim(response)
                "010B" -> parseIntakeManifoldPressure(response)
                "010F" -> parseIntakeAirTemp(response)
                "0111" -> ObdProtocol.parseThrottlePosition(response)
                "0114" -> parseO2Sensor(response)
                "011F" -> parseRunTime(response)
                else -> parseGenericResponse(response)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse response for PID $pid", e)
            null
        }
    }
    
    private fun parseGenericResponse(response: String): Int? {
        return try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.length >= 6) {
                cleaned.substring(4, 6).toInt(16)
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    // Additional parsing methods for PIDs not in ObdProtocol
    private fun parseShortTermFuelTrim(response: String): Double? {
        return try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.length >= 6) {
                val value = cleaned.substring(4, 6).toInt(16)
                (value - 128) * 100.0 / 128.0 // Convert to percentage
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseLongTermFuelTrim(response: String): Double? {
        return try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.length >= 6) {
                val value = cleaned.substring(4, 6).toInt(16)
                (value - 128) * 100.0 / 128.0 // Convert to percentage
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseIntakeManifoldPressure(response: String): Int? {
        return try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.length >= 6) {
                cleaned.substring(4, 6).toInt(16) // kPa
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseIntakeAirTemp(response: String): Int? {
        return try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.length >= 6) {
                val value = cleaned.substring(4, 6).toInt(16)
                value - 40 // Convert to Celsius
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseO2Sensor(response: String): Double? {
        return try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.length >= 8) {
                val voltage = cleaned.substring(4, 6).toInt(16)
                voltage / 200.0 // Convert to volts
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseRunTime(response: String): Int? {
        return try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.length >= 8) {
                val highByte = cleaned.substring(4, 6).toInt(16)
                val lowByte = cleaned.substring(6, 8).toInt(16)
                (highByte * 256 + lowByte) // Seconds
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Emit engine-specific data
     */
    private suspend fun emitEngineData(data: Map<String, Any>) {
        val engineData = EngineData(
            rpm = data["010C"] as? Int ?: 0,
            speed = data["010D"] as? Int ?: 0,
            coolantTemp = data["0105"] as? Int ?: 0,
            intakeTemp = data["010F"] as? Int ?: 0,
            throttlePosition = data["0111"] as? Int ?: 0,
            manifoldPressure = data["010B"] as? Int ?: 0,
            fuelTrimShort = data["0106"] as? Double ?: 0.0,
            fuelTrimLong = data["0107"] as? Double ?: 0.0,
            o2Sensor = data["0114"] as? Double ?: 0.0,
            runTime = data["011F"] as? Int ?: 0,
            timestamp = System.currentTimeMillis()
        )
        _engineData.emit(engineData)
    }
    
    /**
     * Emit transmission-specific data
     */
    private suspend fun emitTransmissionData(data: Map<String, Any>) {
        val transmissionData = TransmissionData(
            gearPosition = data["0122"] as? Int ?: 0,
            fluidTemp = data["0123"] as? Int ?: 0,
            pressure = data["0124"] as? Int ?: 0,
            torqueConverter = data["0125"] as? Int ?: 0,
            timestamp = System.currentTimeMillis()
        )
        _transmissionData.emit(transmissionData)
    }
    
    /**
     * Emit ABS-specific data
     */
    private suspend fun emitAbsData(data: Map<String, Any>) {
        val absData = AbsData(
            wheelSpeedFL = data["0130"] as? Int ?: 0,
            wheelSpeedFR = data["0131"] as? Int ?: 0,
            wheelSpeedRL = data["0132"] as? Int ?: 0,
            wheelSpeedRR = data["0133"] as? Int ?: 0,
            brakePressure = data["0134"] as? Int ?: 0,
            absActive = (data["0135"] as? Int ?: 0) > 0,
            timestamp = System.currentTimeMillis()
        )
        _absData.emit(absData)
    }
    
    /**
     * Emit airbag system data
     */
    private suspend fun emitAirbagData(data: Map<String, Any>) {
        val airbagData = AirbagData(
            systemStatus = data["0140"] as? Int ?: 0,
            crashSensorStatus = data["0141"] as? Int ?: 0,
            seatbeltStatus = data["0142"] as? Int ?: 0,
            occupancyStatus = data["0143"] as? Int ?: 0,
            timestamp = System.currentTimeMillis()
        )
        _airbagData.emit(airbagData)
    }
    
    /**
     * Emit climate control data
     */
    private suspend fun emitClimateData(data: Map<String, Any>) {
        val climateData = ClimateData(
            cabinTemp = data["0150"] as? Int ?: 0,
            outsideTemp = data["0151"] as? Int ?: 0,
            hvacStatus = data["0152"] as? Int ?: 0,
            fanSpeed = data["0153"] as? Int ?: 0,
            acCompressor = (data["0154"] as? Int ?: 0) > 0,
            timestamp = System.currentTimeMillis()
        )
        _climateData.emit(climateData)
    }
    
    /**
     * Emit body control data
     */
    private suspend fun emitBodyControlData(data: Map<String, Any>) {
        val bodyControlData = BodyControlData(
            lightStatus = data["0160"] as? Int ?: 0,
            doorStatus = data["0161"] as? Int ?: 0,
            windowStatus = data["0162"] as? Int ?: 0,
            lockStatus = data["0163"] as? Int ?: 0,
            timestamp = System.currentTimeMillis()
        )
        _bodyControlData.emit(bodyControlData)
    }
    
    /**
     * Update combined vehicle data from all ECU sources
     */
    private fun updateCombinedVehicleData(combinedData: VehicleData) {
        // This would be updated with the latest data from all ECUs
        // Implementation depends on how VehicleData is structured
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopStreaming()
        scope.cancel()
    }
}

// Data classes for different ECU systems
data class EcuInfo(
    val type: String,
    val name: String,
    val requestAddress: Int,
    val responseAddress: Int
)

data class EngineData(
    val rpm: Int,
    val speed: Int,
    val coolantTemp: Int,
    val intakeTemp: Int,
    val throttlePosition: Int,
    val manifoldPressure: Int,
    val fuelTrimShort: Double,
    val fuelTrimLong: Double,
    val o2Sensor: Double,
    val runTime: Int,
    val timestamp: Long
)

data class TransmissionData(
    val gearPosition: Int,
    val fluidTemp: Int,
    val pressure: Int,
    val torqueConverter: Int,
    val timestamp: Long
)

data class AbsData(
    val wheelSpeedFL: Int,
    val wheelSpeedFR: Int,
    val wheelSpeedRL: Int,
    val wheelSpeedRR: Int,
    val brakePressure: Int,
    val absActive: Boolean,
    val timestamp: Long
)

data class AirbagData(
    val systemStatus: Int,
    val crashSensorStatus: Int,
    val seatbeltStatus: Int,
    val occupancyStatus: Int,
    val timestamp: Long
)

data class ClimateData(
    val cabinTemp: Int,
    val outsideTemp: Int,
    val hvacStatus: Int,
    val fanSpeed: Int,
    val acCompressor: Boolean,
    val timestamp: Long
)

data class BodyControlData(
    val lightStatus: Int,
    val doorStatus: Int,
    val windowStatus: Int,
    val lockStatus: Int,
    val timestamp: Long
)

data class StreamingError(
    val message: String,
    val cause: Throwable? = null,
    val timestamp: Long = System.currentTimeMillis()
)

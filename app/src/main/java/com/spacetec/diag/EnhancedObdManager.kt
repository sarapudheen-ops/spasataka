package com.spacetec.diag

import com.spacetec.diagnostic.transport.GenericTransport
import com.spacetec.diagnostic.transport.ObdTransport
import com.spacetec.vehicle.VehicleData
import com.spacetec.vin.VinDecoder
import com.spacetec.vin.VehicleInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay

/**
 * Enhanced OBD Manager that works with any compatible OBD scanner
 * Provides professional diagnostics without brand-specific dependencies
 */
class EnhancedObdManager(private val context: Any) {
    
    // Using GenericTransport for universal OBD-II compatibility
    // Can be extended with manufacturer-specific transports in the future
    private val transport: ObdTransport = GenericTransport(deviceId = "universal")
    
    private val _vehicleData = MutableStateFlow(VehicleData())
    val vehicleData: StateFlow<VehicleData> = _vehicleData.asStateFlow()
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    private val _ecuList = MutableStateFlow<List<String>>(emptyList())
    val ecuList: StateFlow<List<String>> = _ecuList.asStateFlow()
    
    private val _dtcList = MutableStateFlow<List<String>>(emptyList())
    val dtcList: StateFlow<List<String>> = _dtcList.asStateFlow()
    
    private val _vehicleInfo = MutableStateFlow<VehicleInfo?>(null)
    val vehicleInfo: StateFlow<VehicleInfo?> = _vehicleInfo.asStateFlow()
    
    private var monitoringJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    suspend fun initialize(): Boolean {
        return try {
            transport.connect()
            _isInitialized.value = true
            true
        } catch (e: Exception) {
            _isInitialized.value = false
            false
        }
    }
    
    suspend fun startMonitoring() {
        if (!_isInitialized.value) return
        
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            _isMonitoring.value = true
            
            while (isActive) {
                try {
                    // Read basic OBD data
                    val rpm = readRpm()
                    val speed = readSpeed()
                    val coolantTemp = readCoolantTemp()
                    val fuelLevel = readFuelLevel()
                    
                    _vehicleData.value = _vehicleData.value.copy(
                        rpm = rpm,
                        speed = speed,
                        coolantTemp = coolantTemp,
                        fuelLevel = fuelLevel.toInt(),
                        isEngineRunning = rpm > 0,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    delay(1000) // Update every second
                } catch (e: Exception) {
                    // Handle monitoring errors gracefully
                    delay(2000)
                }
            }
        }
    }
    
    suspend fun stopMonitoring() {
        monitoringJob?.cancel()
        _isMonitoring.value = false
    }
    
    suspend fun scanEcus(): List<String> {
        if (!_isInitialized.value) return emptyList()
        
        return try {
            val ecus = transport.scanEcus()
            _ecuList.value = ecus
            ecus
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun readDtcs(): List<String> {
        if (!_isInitialized.value) return emptyList()
        
        return try {
            val dtcs = transport.readDtcs()
            _dtcList.value = dtcs
            dtcs
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun clearDtcs(): Boolean {
        if (!_isInitialized.value) return false
        
        return try {
            transport.clearDtcs()
            _dtcList.value = emptyList()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun readVin(): String? {
        if (!_isInitialized.value) return null
        
        return try {
            val vin = transport.readVin()
            if (vin != null) {
                _vehicleInfo.value = VinDecoder.decodeVin(vin)
            }
            vin
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun readRpm(): Int {
        return try {
            transport.readPid("010C")?.let { response ->
                // Parse RPM from response (PID 0C)
                if (response.length >= 4) {
                    val a = response.substring(0, 2).toInt(16)
                    val b = response.substring(2, 4).toInt(16)
                    ((a * 256) + b) / 4
                } else 0
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    private suspend fun readSpeed(): Int {
        return try {
            transport.readPid("010D")?.let { response ->
                // Parse speed from response (PID 0D)
                if (response.length >= 2) {
                    response.substring(0, 2).toInt(16)
                } else 0
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    private suspend fun readCoolantTemp(): Int {
        return try {
            transport.readPid("0105")?.let { response ->
                // Parse coolant temp from response (PID 05)
                if (response.length >= 2) {
                    response.substring(0, 2).toInt(16) - 40
                } else 0
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    private suspend fun readFuelLevel(): Float {
        return try {
            transport.readPid("012F")?.let { response ->
                // Parse fuel level from response (PID 2F)
                if (response.length >= 2) {
                    response.substring(0, 2).toInt(16) * 100.0f / 255.0f
                } else 0.0f
            } ?: 0.0f
        } catch (e: Exception) {
            0.0f
        }
    }
    
    suspend fun cleanup() {
        stopMonitoring()
        try {
            transport.disconnect()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
        _isInitialized.value = false
    }
}

package com.spacetec.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import android.util.Log
import com.spacetec.diagnostic.obd.RobustObdClient
import com.spacetec.vehicle.VehicleData
import com.spacetec.diagnostic.data.TripRecorder
import com.spacetec.diagnostic.config.ConfigManager

/**
 * Mission Control ViewModel with proper lifecycle management
 */
class MissionControlViewModel(
    private val obdClient: RobustObdClient,
    private val tripRecorder: TripRecorder,
    private val configManager: ConfigManager
) : ViewModel() {
    
    private val vmScope = viewModelScope + SupervisorJob()
    
    // State flows for UI
    private val _vehicleData = MutableStateFlow(VehicleData())
    val vehicleData: StateFlow<VehicleData> = _vehicleData.asStateFlow()
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()
    
    // Jobs for lifecycle management
    private var monitoringJob: Job? = null
    private var connectionJob: Job? = null
    
    companion object {
        private const val TAG = "MissionControlViewModel"
    }
    
    init {
        // Initialize configuration
        vmScope.launch {
            try {
                configManager.loadConfig()
                Log.i(TAG, "Configuration loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load configuration: ${e.message}")
            }
        }
    }
    
    /**
     * Start OBD monitoring with proper lifecycle management
     */
    fun startMonitoring() {
        if (_isMonitoring.value) return
        
        monitoringJob?.cancel()
        monitoringJob = vmScope.launch {
            try {
                _isMonitoring.value = true
                _connectionState.value = ConnectionState.CONNECTING
                
                if (obdClient.testConnection()) {
                    _connectionState.value = ConnectionState.CONNECTED
                    startDataCollection()
                } else {
                    _connectionState.value = ConnectionState.ERROR
                    _errorMessage.emit("Failed to establish OBD connection")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting monitoring: ${e.message}")
                _connectionState.value = ConnectionState.ERROR
                _errorMessage.emit("Monitoring error: ${e.message}")
            }
        }
    }
    
    /**
     * Stop monitoring and cleanup resources
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        _isMonitoring.value = false
        _connectionState.value = ConnectionState.DISCONNECTED
        
        vmScope.launch {
            try {
                obdClient.close()
                Log.i(TAG, "Monitoring stopped and resources cleaned up")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping monitoring: ${e.message}")
            }
        }
    }
    
    /**
     * Start trip recording
     */
    fun startTrip(tripName: String) {
        vmScope.launch {
            try {
                tripRecorder.startTrip(tripName)
                Log.i(TAG, "Trip recording started: $tripName")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting trip: ${e.message}")
                _errorMessage.emit("Failed to start trip recording")
            }
        }
    }
    
    /**
     * End trip recording
     */
    fun endTrip() {
        vmScope.launch {
            try {
                val summary = tripRecorder.endTrip()
                Log.i(TAG, "Trip ended: ${summary.trip.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error ending trip: ${e.message}")
                _errorMessage.emit("Failed to end trip recording")
            }
        }
    }
    
    /**
     * Reconnect to OBD device
     */
    fun reconnect() {
        connectionJob?.cancel()
        connectionJob = vmScope.launch {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                
                if (obdClient.reconnect()) {
                    _connectionState.value = ConnectionState.CONNECTED
                    if (_isMonitoring.value) {
                        startDataCollection()
                    }
                } else {
                    _connectionState.value = ConnectionState.ERROR
                    _errorMessage.emit("Reconnection failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Reconnection error: ${e.message}")
                _connectionState.value = ConnectionState.ERROR
                _errorMessage.emit("Reconnection error: ${e.message}")
            }
        }
    }
    
    private suspend fun startDataCollection() {
        val pids = listOf("0C", "0D", "05", "2F") // RPM, Speed, Temp, Fuel
        
        while (_isMonitoring.value && currentCoroutineContext().isActive) {
            try {
                val results = obdClient.readMultiplePids(pids)
                val vehicleData = parseVehicleData(results)
                
                _vehicleData.value = vehicleData
                
                // Record data point if trip is active
                if (tripRecorder.isRecording()) {
                    results.forEach { (pid, result) ->
                        result.getOrNull()?.let { value ->
                            tripRecorder.recordDataPoint(pid, parseNumericValue(value), System.currentTimeMillis())
                        }
                    }
                }
                
                delay(configManager.getConfig().pidPollingInterval)
            } catch (e: CancellationException) {
                Log.d(TAG, "Data collection cancelled")
                break
            } catch (e: Exception) {
                Log.e(TAG, "Data collection error: ${e.message}")
                _errorMessage.emit("Data collection error: ${e.message}")
                delay(1000) // Wait before retrying
            }
        }
    }
    
    private fun parseVehicleData(results: Map<String, Result<String>>): VehicleData {
        return VehicleData(
            speed = results["0D"]?.getOrNull()?.let { parseSpeed(it) } ?: 0,
            rpm = results["0C"]?.getOrNull()?.let { parseRpm(it) } ?: 0,
            coolantTemp = results["05"]?.getOrNull()?.let { parseTemp(it) } ?: 0,
            engineLoad = results["2F"]?.getOrNull()?.let { parseFuel(it) } ?: 0,
            isEngineRunning = results["0C"]?.getOrNull()?.let { parseRpm(it) > 0 } ?: false,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun parseSpeed(response: String): Int {
        // Parse OBD speed response: "41 0D XX"
        return try {
            val bytes = response.split(" ")
            if (bytes.size >= 3) {
                Integer.parseInt(bytes[2], 16)
            } else 0
        } catch (e: Exception) {
            0
        }
    }
    
    private fun parseRpm(response: String): Int {
        // Parse OBD RPM response: "41 0C XX YY"
        return try {
            val bytes = response.split(" ")
            if (bytes.size >= 4) {
                val a = Integer.parseInt(bytes[2], 16)
                val b = Integer.parseInt(bytes[3], 16)
                ((a * 256) + b) / 4
            } else 0
        } catch (e: Exception) {
            0
        }
    }
    
    private fun parseTemp(response: String): Int {
        // Parse OBD temperature response: "41 05 XX"
        return try {
            val bytes = response.split(" ")
            if (bytes.size >= 3) {
                Integer.parseInt(bytes[2], 16) - 40
            } else 0
        } catch (e: Exception) {
            0
        }
    }
    
    private fun parseFuel(response: String): Int {
        // Parse OBD fuel level response: "41 2F XX"
        return try {
            val bytes = response.split(" ")
            if (bytes.size >= 3) {
                (Integer.parseInt(bytes[2], 16) * 100) / 255
            } else 0
        } catch (e: Exception) {
            0
        }
    }
    
    private fun parseNumericValue(response: String): Double {
        return try {
            val bytes = response.split(" ")
            if (bytes.size >= 3) {
                Integer.parseInt(bytes[2], 16).toDouble()
            } else 0.0
        } catch (e: Exception) {
            0.0
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        
        // Cancel all jobs
        monitoringJob?.cancel()
        connectionJob?.cancel()
        vmScope.cancel()
        
        // Cleanup resources
        runBlocking {
            try {
                obdClient.close()
                tripRecorder.endTrip()
                Log.i(TAG, "ViewModel cleared and resources cleaned up")
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup: ${e.message}")
            }
        }
    }
}

/**
 * Connection state enum
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

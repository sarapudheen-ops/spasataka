package com.spacetec.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spacetec.obd.RealObdManager
import com.spacetec.vehicle.VehicleData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import javax.inject.Inject

/**
 * Data class representing the current UI state
 */
data class DiagnosticUiState(
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdateTime: Long = 0,
    val isDataStale: Boolean = true,
    val obdProtocol: String = "ISO 15765-4 (CAN)"
)

/**
 * ViewModel for handling diagnostic data and OBD communication
 */
@HiltViewModel
class DiagnosticViewModel @Inject constructor(
    application: Application,
    private val obdManager: RealObdManager
) : AndroidViewModel(application) {
    
    private var dataCollectionJob: Job? = null
    
    // UI State
    private val _uiState = mutableStateOf(DiagnosticUiState())
    val uiState: State<DiagnosticUiState> = _uiState
    
    // Connection state
    val connectionStatus = obdManager.connectionStatus
    
    // Vehicle data flows from RealObdManager
    val vehicleData = obdManager.vehicleData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VehicleData()
        )
    
    val dtcCodes = obdManager.dtcCodes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val supportedPids = obdManager.supportedPids
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )
    
    val vehicleInfo = obdManager.vehicleInfo
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    // Connection state derived from connection status
    val isConnected = connectionStatus
        .map { it.contains("Connected") }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    init {
        viewModelScope.launch {
            // Monitor connection status changes
            connectionStatus.collect { status ->
                _uiState.value = when {
                    status.contains("Connecting") -> _uiState.value.copy(
                        isConnected = false,
                        isLoading = true,
                        error = null
                    )
                    status.contains("Connected") -> _uiState.value.copy(
                        isConnected = true,
                        isLoading = false,
                        error = null,
                        isDataStale = false
                    )
                    status.contains("Disconnected") -> _uiState.value.copy(
                        isConnected = false,
                        isLoading = false,
                        isDataStale = true
                    )
                    status.contains("error") || status.contains("Failed") -> _uiState.value.copy(
                        isConnected = false,
                        isLoading = false,
                        error = status,
                        isDataStale = true
                    )
                    else -> _uiState.value
                }
            }
        }
    }
    
    /**
     * Start collecting data from OBD adapter
     */
    fun startDataCollection() {
        // Data collection is handled automatically by RealObdManager
        // This method is kept for compatibility with UI
        _uiState.value = _uiState.value.copy(
            lastUpdateTime = System.currentTimeMillis(),
            isDataStale = false
        )
    }
    
    /**
     * Stop collecting data
     */
    fun stopDataCollection() {
        dataCollectionJob?.cancel()
        dataCollectionJob = null
    }
    
    /**
     * Refresh all data
     */
    fun refreshData() {
        _uiState.value = _uiState.value.copy(
            lastUpdateTime = System.currentTimeMillis(),
            isDataStale = false,
            error = null
        )
    }
    
    /**
     * Clear any error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Connect to OBD adapter
     */
    fun connectToObd(deviceAddress: String = "00:1A:79:12:34:56") {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val success = obdManager.connect(deviceAddress)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isConnected = true,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isConnected = false,
                        isLoading = false,
                        error = "Failed to connect to OBD adapter"
                    )
                }
            } catch (e: Exception) {
                Log.e("DiagnosticViewModel", "Error connecting to OBD", e)
                _uiState.value = _uiState.value.copy(
                    error = "Connection failed: ${e.message}",
                    isLoading = false,
                    isConnected = false
                )
            }
        }
    }
    
    /**
     * Connect to OBD adapter with device address
     */
    fun connect(deviceAddress: String) {
        connectToObd(deviceAddress)
    }
    
    /**
     * Disconnect from OBD adapter
     */
    fun disconnectFromObd() {
        stopDataCollection()
        viewModelScope.launch {
            try {
                obdManager.disconnect()
                _uiState.value = _uiState.value.copy(
                    isConnected = false,
                    isLoading = false,
                    error = null,
                    isDataStale = true
                )
            } catch (e: Exception) {
                Log.e("DiagnosticViewModel", "Error disconnecting from OBD", e)
                _uiState.value = _uiState.value.copy(
                    error = "Disconnect failed: ${e.message}",
                    isConnected = false
                )
            }
        }
    }
    
    /**
     * Disconnect from OBD adapter (legacy method)
     */
    fun disconnect() {
        disconnectFromObd()
    }
    
    /**
     * Read DTC codes from the vehicle
     */
    fun readDtcCodes() {
        viewModelScope.launch {
            try {
                obdManager.readDtcCodes()
            } catch (e: Exception) {
                Log.e("DiagnosticViewModel", "Error reading DTC codes", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to read DTC codes: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear DTC codes from the vehicle
     */
    fun clearDtcCodes() {
        viewModelScope.launch {
            try {
                obdManager.clearDtcCodes()
                // Refresh the codes after clearing
                readDtcCodes()
            } catch (e: Exception) {
                Log.e("DiagnosticViewModel", "Error clearing DTC codes", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to clear DTC codes: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Read freeze frame data for a specific DTC
     */
    fun readFreezeFrameData(dtc: String) {
        viewModelScope.launch {
            try {
                val freezeFrameData = obdManager.readFreezeFrameData(dtc)
                // Handle freeze frame data if needed
                // For now, we'll just log it
                Log.d("DiagnosticViewModel", "Freeze frame data: $freezeFrameData")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to read freeze frame: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Read vehicle information
     */
    fun readVehicleInfo() {
        viewModelScope.launch {
            try {
                obdManager.readVehicleInfo()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to read vehicle info: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Reset the error message
     */
    fun resetError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Cleanup when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        stopDataCollection()
    }
}


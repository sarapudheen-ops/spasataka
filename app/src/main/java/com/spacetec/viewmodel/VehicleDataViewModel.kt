package com.spacetec.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetec.obd.ObdManager
import com.spacetec.vehicle.VehicleData
import com.spacetec.vehicle.VehicleDataProvider
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VehicleDataViewModel(
    private val obdManager: ObdManager
) : ViewModel() {
    private val _vehicleData = MutableStateFlow(VehicleDataProvider.getSampleVehicleData())
    val vehicleData: StateFlow<VehicleData> = _vehicleData.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private var dataCollectionJob: kotlinx.coroutines.Job? = null
    
    init {
        connectToObd()
    }
    
    fun connectToObd() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Initialize OBD connection
                obdManager.initialize()
                _isConnected.value = true
                
                // Start monitoring for data updates
                startDataPolling()
            } catch (e: Exception) {
                _isConnected.value = false
                _errorMessage.value = "Connection error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun startDataPolling() {
        dataCollectionJob?.cancel() // Cancel any existing job
        dataCollectionJob = viewModelScope.launch {
            while (isActive) {
                try {
                    if (_isConnected.value) {
                        // The ObdManager already updates its vehicleData StateFlow
                        // Just forward the latest data
                        val latestData = obdManager.vehicleData.value
                        _vehicleData.value = latestData
                        
                        // Update connection status based on data freshness
                        val isDataFresh = (System.currentTimeMillis() - latestData.timestamp) < 5000 // 5 seconds threshold
                        _isConnected.value = isDataFresh && (obdManager.isInitialized?.value ?: false)
                    }
                    kotlinx.coroutines.delay(1000) // Update UI every second
                } catch (e: Exception) {
                    _isConnected.value = false
                    _errorMessage.value = "Data polling error: ${e.message}"
                }
            }
        }
    }
    
    fun disconnectFromObd() {
        viewModelScope.launch {
            try {
                // Stop any ongoing data collection
                dataCollectionJob?.cancel()
                
                // Disconnect from OBD adapter
                (obdManager as? AutoCloseable)?.close()
                
                // Reset state
                _isConnected.value = false
                _vehicleData.value = VehicleData()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error disconnecting: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

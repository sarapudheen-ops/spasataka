package com.spacetec.ui.adas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.spacetec.diagnostic.adas.AdasCalibrationSystem

class AdasCalibrationViewModel(application: Application) : AndroidViewModel(application) {
    
    private val adasSystem = AdasCalibrationSystem(application)
    
    private val _selectedVehicle = MutableStateFlow<Map<String, String>?>(null)
    val selectedVehicle: StateFlow<Map<String, String>?> = _selectedVehicle.asStateFlow()
    
    private val _supportedSystems = MutableStateFlow<List<AdasCalibrationSystem.AdasSystem>>(emptyList())
    val supportedSystems: StateFlow<List<AdasCalibrationSystem.AdasSystem>> = _supportedSystems.asStateFlow()
    
    val calibrationStatus = adasSystem.calibrationStatus
    val currentProcedure = adasSystem.currentProcedure
    
    fun selectVehicle(make: String, model: String, year: String) {
        val vehicleInfo = mapOf(
            "make" to make,
            "model" to model,
            "year" to year
        )
        _selectedVehicle.value = vehicleInfo
        
        // Get supported ADAS systems for this vehicle
        val systems = adasSystem.getSupportedSystems(vehicleInfo)
        _supportedSystems.value = systems
    }
    
    fun clearVehicle() {
        _selectedVehicle.value = null
        _supportedSystems.value = emptyList()
    }
    
    fun startCalibration(system: AdasCalibrationSystem.AdasSystem) {
        viewModelScope.launch {
            _selectedVehicle.value?.let { vehicleInfo ->
                adasSystem.startCalibration(system, vehicleInfo)
            }
        }
    }
    
    fun completeCalibration() {
        adasSystem.completeCalibration()
    }
    
    fun cancelCalibration() {
        adasSystem.cancelCalibration()
    }
}

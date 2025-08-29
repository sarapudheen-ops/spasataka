package com.spacetec.ui.commercial

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.spacetec.diagnostic.commercial.CommercialVehicleSupport

class CommercialVehicleViewModel(application: Application) : AndroidViewModel(application) {
    
    private val commercialVehicleSupport = CommercialVehicleSupport(application)
    
    private val _selectedVehicle = MutableStateFlow<CommercialVehicleSupport.CommercialVehicle?>(null)
    val selectedVehicle: StateFlow<CommercialVehicleSupport.CommercialVehicle?> = _selectedVehicle.asStateFlow()
    
    private val _selectedType = MutableStateFlow<CommercialVehicleSupport.VehicleType?>(null)
    val selectedType: StateFlow<CommercialVehicleSupport.VehicleType?> = _selectedType.asStateFlow()
    
    private val _vehiclesByType = MutableStateFlow<List<CommercialVehicleSupport.CommercialVehicle>>(emptyList())
    val vehiclesByType: StateFlow<List<CommercialVehicleSupport.CommercialVehicle>> = _vehiclesByType.asStateFlow()
    
    fun selectVehicleType(type: CommercialVehicleSupport.VehicleType) {
        _selectedType.value = type
        _vehiclesByType.value = commercialVehicleSupport.getVehiclesByType(type)
    }
    
    fun selectVehicle(vehicle: CommercialVehicleSupport.CommercialVehicle) {
        _selectedVehicle.value = vehicle
        commercialVehicleSupport.selectVehicle(vehicle)
    }
    
    fun clearSelection() {
        _selectedVehicle.value = null
    }
    
    fun getSupportedProtocols(): List<String> {
        return commercialVehicleSupport.getSupportedProtocols()
    }
    
    fun getAvailableEcus(): List<CommercialVehicleSupport.CommercialEcu> {
        return commercialVehicleSupport.getAvailableEcus()
    }
    
    fun getSpecialFunctions(): List<String> {
        return commercialVehicleSupport.getSpecialFunctions()
    }
}

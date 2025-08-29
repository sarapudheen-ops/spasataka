package com.spacetec.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spacetec.data.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VehicleSelectionViewModel(private val repository: VehicleRepository) : ViewModel() {
    
    private val _vehicleMakes = MutableStateFlow<List<String>>(emptyList())
    val vehicleMakes: StateFlow<List<String>> = _vehicleMakes.asStateFlow()
    
    private val _vehicleModels = MutableStateFlow<List<String>>(emptyList())
    val vehicleModels: StateFlow<List<String>> = _vehicleModels.asStateFlow()
    
    private val _selectedMake = MutableStateFlow<String?>(null)
    val selectedMake: StateFlow<String?> = _selectedMake.asStateFlow()
    
    private val _selectedModel = MutableStateFlow<String?>(null)
    val selectedModel: StateFlow<String?> = _selectedModel.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadVehicleMakes()
    }
    
    private fun loadVehicleMakes() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val makes = repository.getVehicleMakes()
                _vehicleMakes.value = makes
            } catch (e: Exception) {
                // Handle error - could emit error state
                _vehicleMakes.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectMake(make: String) {
        _selectedMake.value = make
        _selectedModel.value = null // Reset model selection
        loadModelsForMake(make)
    }
    
    private fun loadModelsForMake(make: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val models = repository.getModelsForMake(make)
                _vehicleModels.value = models
            } catch (e: Exception) {
                _vehicleModels.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectModel(model: String) {
        _selectedModel.value = model
    }
    
    fun searchVehicles(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = repository.searchVehicles(query)
                // Update makes with search results
                _vehicleMakes.value = results.keys.toList().sorted()
                // Clear models if search is active
                if (query.isNotEmpty()) {
                    _vehicleModels.value = emptyList()
                    _selectedMake.value = null
                    _selectedModel.value = null
                }
            } catch (e: Exception) {
                // Handle search error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearSelection() {
        _selectedMake.value = null
        _selectedModel.value = null
        _vehicleModels.value = emptyList()
        loadVehicleMakes() // Reload all makes
    }
}

class VehicleSelectionViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VehicleSelectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VehicleSelectionViewModel(VehicleRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

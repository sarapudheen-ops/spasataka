// Temporarily disabled due to compilation errors
/*
package com.spacetec.vehicle.library.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetec.vehicle.library.VehicleLibrary
import com.spacetec.vehicle.library.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing the vehicle library UI state and interactions
 */
@HiltViewModel
class VehicleLibraryViewModel @Inject constructor(
    private val vehicleLibrary: VehicleLibrary
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<VehicleLibraryUiState>(VehicleLibraryUiState.Loading)
    val uiState: StateFlow<VehicleLibraryUiState> = _uiState.asStateFlow()

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filter state
    private val _filters = MutableStateFlow(VehicleSearchCriteria())
    val filters: StateFlow<VehicleSearchCriteria> = _filters.asStateFlow()

    // Selected vehicle
    private val _selectedVehicle = MutableStateFlow<VehicleSpec?>(null)
    val selectedVehicle: StateFlow<VehicleSpec?> = _selectedVehicle.asStateFlow()

    // Available filters
    private val _availableMakes = MutableStateFlow<List<String>>(emptyList())
    val availableMakes: StateFlow<List<String>> = _availableMakes.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private val _availableYears = MutableStateFlow<List<Int>>(emptyList())
    val availableYears: StateFlow<List<Int>> = _availableYears.asStateFlow()

    init {
        // Initialize the library and load initial data
        initializeLibrary()
        
        // Observe search query changes
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.length >= 2) {
                        searchVehicles(query)
                    } else {
                        // Reset search results if query is too short
                        if (_uiState.value is VehicleLibraryUiState.Success) {
                            _uiState.value = VehicleLibraryUiState.Success(emptyList())
                        }
                    }
                }
        }
        
        // Observe filter changes
        viewModelScope.launch {
            _filters
                .debounce(500)
                .distinctUntilChanged()
                .collect { filters ->
                    applyFilters(filters)
                }
        }
    }

    private fun initializeLibrary() {
        viewModelScope.launch {
            _uiState.value = VehicleLibraryUiState.Loading
            try {
                // Initialize the library
                val success = vehicleLibrary.initialize()
                if (success) {
                    // Load available makes
                    _availableMakes.value = vehicleLibrary.getSupportedMakes()
                    _uiState.value = VehicleLibraryUiState.Success(emptyList())
                } else {
                    _uiState.value = VehicleLibraryUiState.Error("Failed to initialize vehicle library")
                }
            } catch (e: Exception) {
                _uiState.value = VehicleLibraryUiState.Error("Error initializing vehicle library: ${e.message}")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onMakeSelected(make: String?) {
        _filters.update { it.copy(brand = make) }
        _availableModels.value = if (make != null) {
            vehicleLibrary.getModelsForMake(make)
        } else {
            emptyList()
        }
        // Reset dependent filters
        _filters.update { it.copy(model = null, year = null) }
        _availableYears.value = emptyList()
    }

    fun onModelSelected(model: String?) {
        val currentMake = _filters.value.brand
        _filters.update { it.copy(model = model) }
        
        // Update available years
        _availableYears.value = if (currentMake != null && model != null) {
            vehicleLibrary.getYearsForModel(currentMake, model)
        } else {
            emptyList()
        }
        
        // Reset year filter
        _filters.update { it.copy(year = null) }
    }

    fun onYearSelected(year: Int?) {
        _filters.update { it.copy(year = year) }
    }

    fun onFuelTypeSelected(fuelType: String, selected: Boolean) {
        val currentTypes = _filters.value.fuelType.toMutableList()
        if (selected && !currentTypes.contains(fuelType)) {
            currentTypes.add(fuelType)
        } else if (!selected) {
            currentTypes.remove(fuelType)
        }
        _filters.update { it.copy(fuelType = currentTypes) }
    }

    fun selectVehicle(vehicle: VehicleSpec) {
        _selectedVehicle.value = vehicle
    }

    fun clearSelectedVehicle() {
        _selectedVehicle.value = null
    }

    private fun searchVehicles(query: String) {
        viewModelScope.launch {
            _uiState.value = VehicleLibraryUiState.Loading
            try {
                val results = vehicleLibrary.searchVehicles(
                    VehicleSearchCriteria(
                        model = query
                    )
                )
                _uiState.value = VehicleLibraryUiState.Success(results)
            } catch (e: Exception) {
                _uiState.value = VehicleLibraryUiState.Error("Search failed: ${e.message}")
            }
        }
    }

    private fun applyFilters(filters: VehicleSearchCriteria) {
        viewModelScope.launch {
            _uiState.value = VehicleLibraryUiState.Loading
            try {
                val results = vehicleLibrary.searchVehicles(filters)
                _uiState.value = VehicleLibraryUiState.Success(results)
            } catch (e: Exception) {
                _uiState.value = VehicleLibraryUiState.Error("Failed to apply filters: ${e.message}")
            }
        }
    }

    fun lookupVehicleByVin(vin: String) {
        viewModelScope.launch {
            _uiState.value = VehicleLibraryUiState.Loading
            try {
                val vehicle = vehicleLibrary.getVehicleByVin(vin)
                if (vehicle != null) {
                    _selectedVehicle.value = vehicle
                    _uiState.value = VehicleLibraryUiState.Success(listOf(vehicle))
                } else {
                    _uiState.value = VehicleLibraryUiState.Error("Vehicle not found for VIN: $vin")
                }
            } catch (e: Exception) {
                _uiState.value = VehicleLibraryUiState.Error("VIN lookup failed: ${e.message}")
            }
        }
    }
}
*/

// UI State moved to avoid compilation errors
/*
sealed class VehicleLibraryUiState {
    object Loading : VehicleLibraryUiState()
    data class Success(val vehicles: List<VehicleSpec>) : VehicleLibraryUiState()
    data class Error(val message: String) : VehicleLibraryUiState()
}
*/

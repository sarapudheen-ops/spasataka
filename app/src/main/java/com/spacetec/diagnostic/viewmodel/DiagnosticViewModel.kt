package com.spacetec.diagnostic.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetec.diagnostic.vehicle.VehicleBrand
import com.spacetec.diagnostic.vehicle.VehicleEcu
import com.spacetec.diagnostic.vehicle.VehicleLibrary
import com.spacetec.diagnostic.vehicle.VehicleModel
import com.spacetec.diagnostic.vehicle.VehiclePid
import kotlinx.coroutines.launch

class DiagnosticViewModel(
    private val vehicleLibrary: VehicleLibrary
) : ViewModel() {

    private val _brands = MutableLiveData<List<VehicleBrand>>()
    val brands: LiveData<List<VehicleBrand>> = _brands

    private val _models = MutableLiveData<List<VehicleModel>>()
    val models: LiveData<List<VehicleModel>> = _models

    private val _ecus = MutableLiveData<List<VehicleEcu>>()
    val ecus: LiveData<List<VehicleEcu>> = _ecus

    private val _pids = MutableLiveData<List<VehiclePid>>()
    val pids: LiveData<List<VehiclePid>> = _pids

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _selectedBrand = MutableLiveData<VehicleBrand?>()
    val selectedBrand: LiveData<VehicleBrand?> = _selectedBrand

    private val _selectedModel = MutableLiveData<VehicleModel?>()
    val selectedModel: LiveData<VehicleModel?> = _selectedModel

    private val _selectedEcu = MutableLiveData<VehicleEcu?>()
    val selectedEcu: LiveData<VehicleEcu?> = _selectedEcu

    fun loadBrands(dbName: String = "vehicle_library.db") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val brands = vehicleLibrary.listBrands()
                _brands.value = brands.map { 
                    VehicleBrand(it.id.toInt(), it.name) 
                }
            } catch (e: Exception) {
                _error.value = "Failed to load vehicle brands: ${e.message}"
                // Fallback to empty list
                _brands.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadModelsForBrand(brandId: Int, dbName: String = "vehicle_library.db") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val models = vehicleLibrary.listModels(brandId.toLong())
                _models.value = models.map { 
                    VehicleModel(it.id.toInt(), it.brandId.toInt(), it.name, 
                        when {
                            it.yearFrom != null && it.yearTo != null -> "${it.yearFrom}-${it.yearTo}"
                            it.yearFrom != null -> "${it.yearFrom}+"
                            else -> "Unknown"
                        }
                    )
                }
            } catch (e: Exception) {
                _error.value = "Failed to load vehicle models: ${e.message}"
                _models.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadEcusForModel(modelId: Int, dbName: String = "vehicle_library.db") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val ecus = vehicleLibrary.listEcus(modelId.toLong())
                _ecus.value = ecus.map { 
                    VehicleEcu(it.id.toInt(), it.modelId.toInt(), it.name, it.protocol ?: "OBD-II")
                }
            } catch (e: Exception) {
                _error.value = "Failed to load ECUs: ${e.message}"
                _ecus.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadPidsForEcu(ecuId: Int, dbName: String = "vehicle_library.db") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val pids = vehicleLibrary.listPids(ecuId.toLong())
                _pids.value = pids.map { 
                    VehiclePid(it.pid, it.name, it.unit ?: "", it.formula ?: "")
                }
            } catch (e: Exception) {
                _error.value = "Failed to load PIDs: ${e.message}"
                // Fallback to standard PIDs
                _pids.value = vehicleLibrary.getStandardPids()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadStandardPids() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _pids.value = vehicleLibrary.getStandardPids()
            } catch (e: Exception) {
                _error.value = "Failed to load standard PIDs: ${e.message}"
                _pids.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectBrand(brand: VehicleBrand) {
        _selectedBrand.value = brand
        _selectedModel.value = null
        _selectedEcu.value = null
        _models.value = emptyList()
        _ecus.value = emptyList()
        _pids.value = emptyList()
        loadModelsForBrand(brand.id)
    }

    fun selectModel(model: VehicleModel) {
        _selectedModel.value = model
        _selectedEcu.value = null
        _ecus.value = emptyList()
        _pids.value = emptyList()
        loadEcusForModel(model.id)
    }

    fun selectEcu(ecu: VehicleEcu) {
        _selectedEcu.value = ecu
        _pids.value = emptyList()
        loadPidsForEcu(ecu.id)
    }

    fun clearError() {
        _error.value = null
    }

    fun reset() {
        _selectedBrand.value = null
        _selectedModel.value = null
        _selectedEcu.value = null
        _brands.value = emptyList()
        _models.value = emptyList()
        _ecus.value = emptyList()
        _pids.value = emptyList()
        _error.value = null
        _isLoading.value = false
    }
}

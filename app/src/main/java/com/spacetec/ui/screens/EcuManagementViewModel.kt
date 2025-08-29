package com.spacetec.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetec.obd.RealObdManager
import com.spacetec.actuators.ActuatorController
import com.spacetec.vehicle.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class EcuManagementViewModel(
    private val context: Context,
    private val obdManager: RealObdManager,
    private val actuatorController: ActuatorController
) : ViewModel() {
    
    private val vehicleEcuManager = VehicleEcuManager(context, obdManager, actuatorController)
    
    // Expose state flows from VehicleEcuManager
    val selectedVehicle: StateFlow<VehicleEcuProfile?> = vehicleEcuManager.selectedVehicle
    val availableEcus: StateFlow<List<EcuCapability>> = vehicleEcuManager.availableEcus
    val availableTests: StateFlow<Map<String, List<EcuTest>>> = vehicleEcuManager.availableTests
    val programmingCapabilities: StateFlow<Map<String, ProgrammingCapability>> = vehicleEcuManager.programmingCapabilities
    val testResults: SharedFlow<EcuTestResult> = vehicleEcuManager.testResults
    val testProgress: StateFlow<TestProgress?> = vehicleEcuManager.testProgress
    val programmingResults: SharedFlow<ProgrammingResult> = vehicleEcuManager.programmingResults
    val programmingProgress: StateFlow<ProgrammingProgress?> = vehicleEcuManager.programmingProgress
    
    // Additional state for UI
    private val _diagnosticSummary = MutableStateFlow<VehicleDiagnosticSummary?>(null)
    val diagnosticSummary: StateFlow<VehicleDiagnosticSummary?> = _diagnosticSummary.asStateFlow()
    
    private val _supportedBrands = MutableStateFlow<List<String>>(emptyList())
    val supportedBrands: StateFlow<List<String>> = _supportedBrands.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        // Load supported brands on initialization
        viewModelScope.launch {
            loadSupportedBrands()
        }
        
        // Observe vehicle selection changes to update diagnostic summary
        viewModelScope.launch {
            selectedVehicle.collect { vehicle ->
                if (vehicle != null) {
                    updateDiagnosticSummary()
                } else {
                    _diagnosticSummary.value = null
                }
            }
        }
        
        // Observe test and programming results
        viewModelScope.launch {
            testResults.collect { result ->
                when (result) {
                    is EcuTestResult.Success -> {
                        // Handle successful test result
                        clearError()
                    }
                    is EcuTestResult.Error -> {
                        _errorMessage.value = "Test failed: ${result.message}"
                    }
                }
            }
        }
        
        viewModelScope.launch {
            programmingResults.collect { result ->
                when (result) {
                    is ProgrammingResult.Success -> {
                        // Handle successful programming result
                        clearError()
                    }
                    is ProgrammingResult.Error -> {
                        _errorMessage.value = "Programming failed: ${result.message}"
                    }
                }
            }
        }
    }
    
    /**
     * Load supported vehicle brands
     */
    private suspend fun loadSupportedBrands() {
        try {
            _isLoading.value = true
            val brands = vehicleEcuManager.getSupportedBrands()
            _supportedBrands.value = brands
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load vehicle brands: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Select vehicle and load ECU capabilities
     */
    suspend fun selectVehicle(make: String, model: String, year: Int, engine: String? = null) {
        try {
            _isLoading.value = true
            clearError()
            vehicleEcuManager.selectVehicle(make, model, year, engine)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to select vehicle: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Get models for specific brand
     */
    suspend fun getModelsForBrand(brandName: String): List<String> {
        return try {
            vehicleEcuManager.getModelsForBrand(brandName)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load models: ${e.message}"
            emptyList()
        }
    }
    
    /**
     * Search vehicles by criteria
     */
    suspend fun searchVehicles(
        query: String,
        year: Int? = null,
        engine: String? = null
    ): List<VehicleSearchResult> {
        return try {
            vehicleEcuManager.searchVehicles(query, year, engine)
        } catch (e: Exception) {
            _errorMessage.value = "Search failed: ${e.message}"
            emptyList()
        }
    }
    
    /**
     * Execute ECU test
     */
    suspend fun executeEcuTest(
        ecuId: String,
        testId: String,
        parameters: Map<String, Any> = emptyMap()
    ) {
        try {
            clearError()
            vehicleEcuManager.executeEcuTest(ecuId, testId, parameters)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to execute test: ${e.message}"
        }
    }
    
    /**
     * Program ECU
     */
    suspend fun programEcu(
        ecuId: String,
        firmwareFile: File,
        programmingType: ProgrammingType,
        options: ProgrammingOptions = ProgrammingOptions()
    ) {
        try {
            clearError()
            vehicleEcuManager.programEcu(ecuId, firmwareFile, programmingType, options)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to program ECU: ${e.message}"
        }
    }
    
    /**
     * Get ECU details by ID
     */
    fun getEcuById(ecuId: String): EcuCapability? {
        return vehicleEcuManager.getEcuById(ecuId)
    }
    
    /**
     * Get available tests for specific ECU
     */
    fun getTestsForEcu(ecuId: String): List<EcuTest> {
        return vehicleEcuManager.getTestsForEcu(ecuId)
    }
    
    /**
     * Get programming capability for specific ECU
     */
    fun getProgrammingCapabilityForEcu(ecuId: String): ProgrammingCapability? {
        return vehicleEcuManager.getProgrammingCapabilityForEcu(ecuId)
    }
    
    /**
     * Refresh vehicle data
     */
    suspend fun refreshVehicleData() {
        val currentVehicle = selectedVehicle.value
        if (currentVehicle != null) {
            selectVehicle(
                currentVehicle.make,
                currentVehicle.model,
                extractYearFromRange(currentVehicle.yearRange),
                currentVehicle.engine
            )
        }
        loadSupportedBrands()
    }
    
    /**
     * Update diagnostic summary
     */
    private suspend fun updateDiagnosticSummary() {
        try {
            val summary = vehicleEcuManager.getVehicleDiagnosticSummary()
            _diagnosticSummary.value = summary
        } catch (e: Exception) {
            _errorMessage.value = "Failed to update diagnostic summary: ${e.message}"
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Extract year from year range string
     */
    private fun extractYearFromRange(range: String): Int {
        return try {
            range.split("-")[0].toInt()
        } catch (e: Exception) {
            2020 // Default year
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        vehicleEcuManager.cleanup()
    }
}

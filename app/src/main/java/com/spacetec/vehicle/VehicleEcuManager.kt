// # Vehicle ECU Management System

package com.spacetec.vehicle

import android.content.Context
import android.util.Log
import com.spacetec.diagnostic.vehicle.VehicleLibrary
import com.spacetec.obd.RealObdManager
import com.spacetec.actuators.ActuatorController
import com.spacetec.database.VehicleDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

/**
 * Comprehensive Vehicle ECU Management System
 * Integrates vehicle database, ECU testing, and programming capabilities
 */
class VehicleEcuManager(
    private val context: Context,
    private val obdManager: RealObdManager,
    private val actuatorController: ActuatorController
) {
    companion object {
        private const val TAG = "VehicleEcuManager"
    }

    private val vehicleLibrary = VehicleLibrary(context)
    private val ecuTestEngine = EcuTestEngine(obdManager, actuatorController)
    private val ecuProgrammingEngine = EcuProgrammingEngine(obdManager)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // State flows for vehicle and ECU information
    private val _selectedVehicle = MutableStateFlow<VehicleEcuProfile?>(null)
    val selectedVehicle: StateFlow<VehicleEcuProfile?> = _selectedVehicle.asStateFlow()

    private val _availableEcus = MutableStateFlow<List<EcuCapability>>(emptyList())
    val availableEcus: StateFlow<List<EcuCapability>> = _availableEcus.asStateFlow()

    private val _availableTests = MutableStateFlow<Map<String, List<EcuTest>>>(emptyMap())
    val availableTests: StateFlow<Map<String, List<EcuTest>>> = _availableTests.asStateFlow()

    private val _programmingCapabilities = MutableStateFlow<Map<String, ProgrammingCapability>>(emptyMap())
    val programmingCapabilities: StateFlow<Map<String, ProgrammingCapability>> = _programmingCapabilities.asStateFlow()

    // Forward test and programming flows
    val testResults: SharedFlow<EcuTestResult> = ecuTestEngine.testResults
    val testProgress: StateFlow<TestProgress?> = ecuTestEngine.testProgress
    val programmingResults: SharedFlow<ProgrammingResult> = ecuProgrammingEngine.programmingResults
    val programmingProgress: StateFlow<ProgrammingProgress?> = ecuProgrammingEngine.programmingProgress

    /**
     * Get all supported vehicle brands
     * @return List of brand names as strings
     */
    suspend fun getSupportedBrands(): List<String> {
        return VehicleDatabase(context).getAllBrands()
    }

    /**
     * Get models for a specific brand
     * @param brand The name of the brand to get models for
     * @return List of model names as strings
     */
    suspend fun getModelsForBrand(brand: String): List<String> {
        return VehicleDatabase(context).getModelsForBrand(brand)
    }

    /**
     * Select vehicle and load ECU capabilities
     */
    suspend fun selectVehicle(make: String, model: String, year: Int, engine: String? = null) {
        try {
            Log.i(TAG, "Selecting vehicle: $make $model $year")

            val db = VehicleDatabase(context)
            val vehicles = db.searchByMake(make)
            val vehicle = vehicles.find { it.model.equals(model, ignoreCase = true) && it.year == year }

            if (vehicle != null) {
                val profile = VehicleEcuProfile(
                    make = vehicle.make,
                    model = vehicle.model,
                    yearRange = "${vehicle.year}-${vehicle.year}",
                    engine = vehicle.engine,
                    transmission = vehicle.transmission,
                    market = "",
                    ecuList = vehicle.ecuAddresses.map { (name, address) ->
                        EcuCapability(
                            ecuId = name,
                            ecuName = name,
                            ecuType = EcuType.ENGINE,
                            supportedProtocols = vehicle.supportedProtocols,
                            supportedTests = emptyList(),
                            programmingSupport = ProgrammingCapability(
                                flashSupported = true,
                                eepromSupported = false,
                                calibrationSupported = false,
                                keyProgramming = false,
                                immobilizerSupport = false,
                                supportedFileFormats = emptyList(),
                                programmingMethods = emptyList()
                            ),
                            diagnosticAddress = address.toString(),
                            memoryLayout = null,
                            securityAccess = null
                        )
                    },
                    specialFeatures = vehicle.availableServices,
                    knownIssues = emptyList(),
                    recommendedTools = emptyList()
                )

                _selectedVehicle.value = profile
                _availableEcus.value = profile.ecuList

                val testsMap = profile.ecuList.associate { ecu ->
                    ecu.ecuId to ecu.supportedTests
                }
                _availableTests.value = testsMap

                val programmingMap = profile.ecuList.associate { ecu ->
                    ecu.ecuId to ecu.programmingSupport
                }
                _programmingCapabilities.value = programmingMap

                Log.i(TAG, "Vehicle selected: ${profile.ecuList.size} ECUs available")
            } else {
                Log.w(TAG, "No vehicle found for $make $model $year")
                _selectedVehicle.value = null
                _availableEcus.value = emptyList()
                _availableTests.value = emptyMap()
                _programmingCapabilities.value = emptyMap()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to select vehicle", e)
        }
    }

    /**
     * Get ECU details by ID
     */
    fun getEcuById(ecuId: String): EcuCapability? {
        return _availableEcus.value.find { it.ecuId == ecuId }
    }

    /**
     * Get available tests for specific ECU
     */
    fun getTestsForEcu(ecuId: String): List<EcuTest> {
        return _availableTests.value[ecuId] ?: emptyList()
    }

    /**
     * Get programming capability for specific ECU
     */
    fun getProgrammingCapabilityForEcu(ecuId: String): ProgrammingCapability? {
        return _programmingCapabilities.value[ecuId]
    }

    /**
     * Execute ECU test
     */
    suspend fun executeEcuTest(
        ecuId: String,
        testId: String,
        parameters: Map<String, Any> = emptyMap()
    ): EcuTestResult {
        val vehicle = _selectedVehicle.value
            ?: return EcuTestResult.Error("No vehicle selected")

        return ecuTestEngine.executeTest(
            vehicleMake = vehicle.make,
            vehicleModel = vehicle.model,
            vehicleYear = vehicle.yearRange.split("-")[0].toInt(),
            ecuId = ecuId,
            testId = testId,
            parameters = parameters
        )
    }

    /**
     * Program ECU
     */
    suspend fun programEcu(
        ecuId: String,
        firmwareFile: File,
        programmingType: ProgrammingType,
        options: ProgrammingOptions = ProgrammingOptions()
    ): ProgrammingResult {
        val vehicle = _selectedVehicle.value
            ?: return ProgrammingResult.Error("No vehicle selected")

        return ecuProgrammingEngine.programEcu(
            vehicleMake = vehicle.make,
            vehicleModel = vehicle.model,
            vehicleYear = vehicle.yearRange.split("-")[0].toInt(),
            ecuId = ecuId,
            firmwareFile = firmwareFile,
            programmingType = programmingType,
            options = options
        )
    }

    /**
     * Get vehicle diagnostic summary
     */
    suspend fun getVehicleDiagnosticSummary(): VehicleDiagnosticSummary? {
        val vehicle = _selectedVehicle.value ?: return null
        val ecus = _availableEcus.value

        return VehicleDiagnosticSummary(
            vehicleInfo = "${vehicle.make} ${vehicle.model} (${vehicle.yearRange})",
            engine = vehicle.engine,
            transmission = vehicle.transmission,
            market = vehicle.market,
            ecuCount = ecus.size,
            totalAvailableTests = ecus.sumOf { it.supportedTests.size },
            programmableEcuCount = ecus.count { it.programmingSupport.flashSupported },
            securityProtectedEcuCount = ecus.count { it.securityAccess != null },
            specialFeatures = vehicle.specialFeatures,
            knownIssues = vehicle.knownIssues,
            recommendedTools = vehicle.recommendedTools,
            supportedProtocols = ecus.flatMap { it.supportedProtocols }.distinct()
        )
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
            val db = VehicleDatabase(context)
            val vehicles = db.getAllVehicles()

            val results = vehicles.filter { vehicle ->
                val matchesQuery = query.isBlank() ||
                    vehicle.make.contains(query, ignoreCase = true) ||
                    vehicle.model.contains(query, ignoreCase = true)

                val matchesYear = year == null || vehicle.year == year
                val matchesEngine = engine == null || vehicle.engine.equals(engine, ignoreCase = true)

                matchesQuery && matchesYear && matchesEngine
            }.map { vehicle ->
                VehicleSearchResult(
                    id = "${vehicle.make}_${vehicle.model}_${vehicle.year}",
                    brandName = vehicle.make,
                    modelName = vehicle.model,
                    year = vehicle.year,
                    engine = vehicle.engine,
                    matchScore = 1.0f
                )
            }

            results.sortedWith(compareBy({ it.brandName }, { it.modelName }, { it.year }))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search vehicles", e)
            emptyList()
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        ecuTestEngine.cleanup()
        ecuProgrammingEngine.cleanup()
    }
}

// Data classes for enhanced vehicle information
data class VehicleModelWithEcu(
    val id: Int,
    val brandId: Int,
    val name: String,
    val yearRange: String,
    val hasEcuSupport: Boolean,
    val ecuCount: Int
)

data class VehicleDiagnosticSummary(
    val vehicleInfo: String,
    val engine: String,
    val transmission: String,
    val market: String,
    val ecuCount: Int,
    val totalAvailableTests: Int,
    val programmableEcuCount: Int,
    val securityProtectedEcuCount: Int,
    val specialFeatures: List<String>,
    val knownIssues: List<String>,
    val recommendedTools: List<String>,
    val supportedProtocols: List<String>
)

// VehicleSearchResult is already defined in CachedVehicleLibrary.kt

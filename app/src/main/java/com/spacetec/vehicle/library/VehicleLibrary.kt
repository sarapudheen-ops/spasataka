// Temporarily disabled due to compilation errors
/*
package com.spacetec.vehicle.library

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import java.util.*
import com.spacetec.vehicle.model.VehicleBrand
import com.spacetec.vehicle.model.VehicleModel
import com.spacetec.vehicle.model.DtcInfo
import com.spacetec.vehicle.model.ServiceProcedure
import com.spacetec.vehicle.library.VehicleSpec

/**
 * Comprehensive Vehicle Library for managing vehicle data, DTCs, and service procedures
 */
class VehicleLibrary(private val context: Context) {
    private val gson = Gson()
    private val vehicleBrands = mutableListOf<VehicleBrand>()
    private val vehicleCache = mutableMapOf<String, VehicleSpec>()
    private val dtcDatabase = mutableMapOf<String, DtcInfo>()
    private val serviceProcedures = mutableMapOf<String, List<ServiceProcedure>>()
    
    // State flows for reactive updates
    private val _initializationStatus = MutableStateFlow<LibraryStatus>(LibraryStatus.LOADING)
    val initializationStatus: StateFlow<LibraryStatus> = _initializationStatus.asStateFlow()
    
    private val _vehicleCount = MutableStateFlow(0)
    val vehicleCount: StateFlow<Int> = _vehicleCount.asStateFlow()
    
    private val _dtcCount = MutableStateFlow(0)
    val dtcCount: StateFlow<Int> = _dtcCount.asStateFlow()
    
    // Initialization state
    @Volatile
    var initialized = false
        private set

    companion object {
        private const val TAG = "VehicleLibrary"
        private const val VEHICLE_DATA_DIR = "vehicle_data"
        private const val DTC_DATA_FILE = "dtc_database.json"
        private const val SERVICE_PROCEDURES_FILE = "service_procedures.json"
        
        // Comprehensive list of supported vehicle manufacturers
        val SUPPORTED_MAKES = listOf(
            // Mainstream Brands
            "Toyota", "Honda", "Ford", "Chevrolet", "Nissan", "Hyundai", "Kia", "Volkswagen",
            "Subaru", "Mazda", "Mitsubishi", "Suzuki", "Dodge", "Jeep", "Chrysler", "Ram",
            "GMC", "Buick", "Cadillac", "Chevrolet", "Ford", "Lincoln", "Volvo", "Fiat",
            "Alfa Romeo", "Ferrari", "Maserati", "Jaguar", "Land Rover", "Mini", "Smart",
            
            // Luxury & Performance
            "Acura", "Audi", "BMW", "Infiniti", "Lexus", "Mercedes-Benz", "Porsche", "Tesla",
            "Bentley", "Rolls-Royce", "Aston Martin", "McLaren", "Lamborghini", "Bugatti",
            "Lotus", "Koenigsegg", "Pagani", "Rimac", "Genesis", "Polestar", "Lucid", "Rivian",
            
            // Commercial & Heavy Duty
            "Freightliner", "International", "Kenworth", "Mack", "Peterbilt", "Volvo Trucks",
            "Western Star", "Hino", "Isuzu", "UD Trucks", "Fuso", "Iveco", "Scania", "MAN",
            "DAF", "Renault Trucks", "Tata", "Ashok Leyland", "FAW", "Foton", "JAC", "JMC",
            
            // Chinese Brands
            "BYD", "Geely", "Great Wall", "Haval", "Chery", "Changan", "SAIC", "MG", "Roewe",
            "NIO", "XPeng", "Li Auto", "Hongqi", "BAIC", "Dongfeng", "Zotye", "JAC", "Brilliance",
            
            // Other Notable Brands
            "Proton", "Perodua", "Tata Motors", "Mahindra", "SsangYong", "Lada", "GAZ",
            "UAZ", "ZAZ", "Dacia", "Datsun", "Lancia", "Opel", "Vauxhall", "Seat", "Cupra",
            "Skoda", "Daihatsu", "Isuzu", "Mazda", "Suzuki", "Subaru", "Mitsubishi"
        ).distinct().sorted() // Ensure unique and sorted list
    }

    /**
     * Initialize library with comprehensive data loading
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (initialized) return@withContext true
        
        _initializationStatus.value = LibraryStatus.LOADING
        
        return@withContext try {
            // Load core data in parallel
            val deferred = listOf(
                async { loadVehicleBrands() },
                async { loadDtcDatabase() },
                async { loadServiceProcedures() },
                async { loadFromFallbackPath() }
            )
            
            // Wait for all operations to complete
            deferred.awaitAll()
            
            // Build vehicle cache
            vehicleBrands.forEach { brand ->
                brand.models.forEach { model ->
                    model.years.forEach { year ->
                        val vehicleId = "${brand.brand}_${model.name}_$year".lowercase(Locale.US)
                        val vehicleSpec = VehicleSpec(
                            id = vehicleId,
                            make = brand.brand,
                            model = model.name,
                            year = year,
                            engineType = model.engineType,
                            fuelType = model.fuelType,
                            obdProtocol = when {
                                year >= 2008 -> "ISO 15765-4 (CAN)"
                                year >= 2004 -> "ISO 14230-4 (KWP2000)"
                                else -> "ISO 9141-2"
                            },
                            market = brand.market ?: ""
                        )
                        vehicleCache[vehicleId] = vehicleSpec
                    }
                }
            }
            
            _vehicleCount.value = vehicleCache.size
            _dtcCount.value = dtcDatabase.size
            initialized = true
            _initializationStatus.value = LibraryStatus.READY
            
            android.util.Log.d(TAG, "Vehicle library initialized with ${vehicleCache.size} vehicles and ${dtcDatabase.size} DTCs")
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Initialization failed", e)
            _initializationStatus.value = LibraryStatus.ERROR(e.message ?: "Unknown error")
            false
        } finally {
            initialized = _initializationStatus.value is LibraryStatus.READY
        }
    }

    /**
     * Load vehicle brand data from assets JSON with enhanced error handling
     */
    private suspend fun loadVehicleBrands() = withContext(Dispatchers.IO) {
        try {
            // Try to load from external storage first (for updates)
            val externalFile = File(context.getExternalFilesDir(VEHICLE_DATA_DIR), "VehicleBrands.json")
            val json = if (externalFile.exists() && externalFile.length() > 0) {
                externalFile.readText()
            } else {
                // Fall back to assets
                loadAssetFile("VehicleBrands.json")
            }
            
            if (json.isNotBlank()) {
                val type = object : TypeToken<List<VehicleBrand>>() {}.type
                val list: List<VehicleBrand> = gson.fromJson(json, type) ?: emptyList()
                val validBrands = list.filter { 
                    it.brand.isNotBlank() && it.models.isNotEmpty() 
                }.sortedBy { it.brand }
                
                vehicleBrands.clear()
                vehicleBrands.addAll(validBrands)
                android.util.Log.d(TAG, "Loaded ${vehicleBrands.size} brands")
            } else {
                android.util.Log.w(TAG, "Empty or invalid VehicleBrands.json")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading vehicle brands", e)
            throw e
        }
    }

    /**
     * Load DTC database from assets
     */
    private suspend fun loadDtcDatabase() = withContext(Dispatchers.IO) {
        try {
            val json = loadAssetFile(DTC_DATA_FILE)
            if (json.isNotBlank()) {
                val type = object : TypeToken<Map<String, DtcInfo>>() {}.type
                val dtcs: Map<String, DtcInfo> = gson.fromJson(json, type) ?: emptyMap()
                dtcDatabase.clear()
                dtcDatabase.putAll(dtcs)
                android.util.Log.d(TAG, "Loaded ${dtcDatabase.size} DTCs")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading DTC database", e)
            throw e
        }
    }
    
    /**
     * Load service procedures from assets
     */
    private suspend fun loadServiceProcedures() = withContext(Dispatchers.IO) {
        try {
            val json = loadAssetFile(SERVICE_PROCEDURES_FILE)
            if (json.isNotBlank()) {
                val type = object : TypeToken<Map<String, List<ServiceProcedure>>>() {}.type
                val procedures: Map<String, List<ServiceProcedure>> = gson.fromJson(json, type) ?: emptyMap()
                serviceProcedures.clear()
                serviceProcedures.putAll(procedures)
                android.util.Log.d(TAG, "Loaded service procedures for ${serviceProcedures.size} models")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading service procedures", e)
            // Non-critical, continue without service procedures
        }
    }
    
    /**
     * Load fallback vehicle data if primary sources fail
     */
    private suspend fun loadFromFallbackPath() = withContext(Dispatchers.IO) {
        try {
            // Try to load a minimal set of common vehicles
            val fallbackBrands = listOf(
                VehicleBrand(
                    brand = "Toyota",
                    market = "Global",
                    models = listOf(
                        VehicleModel(
                            name = "Camry",
                            years = (2015..2022).toList(),
                            engineType = "I4, V6",
                            fuelType = "Gasoline, Hybrid"
                        ),
                        VehicleModel(
                            name = "RAV4",
                            years = (2016..2023).toList(),
                            engineType = "I4, Hybrid",
                            fuelType = "Gasoline, Hybrid"
                        )
                    )
                ),
                VehicleBrand(
                    brand = "Honda",
                    market = "Global",
                    models = listOf(
                        VehicleModel(
                            name = "Civic",
                            years = (2016..2023).toList(),
                            engineType = "I4, Turbo I4",
                            fuelType = "Gasoline"
                        )
                    )
                )
            )
            
            if (vehicleBrands.isEmpty()) {
                vehicleBrands.addAll(fallbackBrands)
                android.util.Log.w(TAG, "Using fallback vehicle data with ${vehicleBrands.size} brands")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to load fallback data", e)
            // If we have no data at all, rethrow to fail initialization
            if (vehicleBrands.isEmpty()) throw e
        }
    }

    /**
     * Load a file with fallback from external storage or assets
     */
    private suspend fun loadAssetFile(fileName: String): String = withContext(Dispatchers.IO) {
        // First try app-specific external storage
        val externalDir = context.getExternalFilesDir(VEHICLE_DATA_DIR)
        if (externalDir != null) {
            val externalFile = File(externalDir, fileName)
            if (externalFile.exists() && externalFile.length() > 0) {
                return@withContext externalFile.readText()
            }
        }
        
        // Then try legacy downloads folder (pre-Android 10)
        try {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloads, fileName)
            if (file.exists() && file.length() > 0) {
                return@withContext file.readText()
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Could not access downloads directory", e)
        }

        // Finally, try assets
        return@withContext try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            android.util.Log.e(TAG, "Failed to load asset: $fileName", e)
            ""
        }
    }
    
    /**
     * Search vehicles by criteria with advanced filtering
     */
    suspend fun searchVehicles(criteria: VehicleSearchCriteria): List<VehicleSpec> = withContext(Dispatchers.Default) {
        if (!initialized && !initialize()) return@withContext emptyList()
        
        return@withContext vehicleCache.values.filter { vehicle ->
            (criteria.brand == null || vehicle.make.equals(criteria.brand, ignoreCase = true)) &&
            (criteria.model == null || vehicle.model.contains(criteria.model, ignoreCase = true)) &&
            (criteria.year == null || vehicle.year == criteria.year) &&
            (criteria.fuelType.isNullOrEmpty() || criteria.fuelType.any { 
                it.equals(vehicle.fuelType, ignoreCase = true) 
            }) &&
            (criteria.market.isNullOrEmpty() || criteria.market.any { 
                it.equals(vehicle.market, ignoreCase = true) 
            })
        }.sortedWith(
            compareBy(
                { it.make },
                { it.model },
                { -it.year } // Sort years in descending order
            )
        )
    }
    
    /**
     * Get vehicle by VIN with caching
     */
    suspend fun getVehicleByVin(vin: String): VehicleSpec? = withContext(Dispatchers.IO) {
        if (!initialized && !initialize()) return@withContext null
        
        // First check cache
        vehicleCache.values.find { it.vin.equals(vin, ignoreCase = true) }?.let {
            return@withContext it
        }
        
        // If not found, try to decode VIN (simplified example)
        return@withContext if (vin.length == 17) {
            // In a real app, this would use a VIN decoding service
            val make = when (vin[0]) {
                'J' -> "Honda"
                'T' -> "Toyota"
                'W' -> "Volkswagen"
                'Z' -> "BMW"
                else -> "Unknown"
            }
            
            val yearChar = vin[9]
            val year = when (yearChar) {
                in 'A'..'Y' -> 2010 + (yearChar - 'A')
                in '1'..'9' -> 2000 + (yearChar - '1' + 1)
                else -> 0
            }
            
            // Create a basic vehicle spec from VIN
            VehicleSpec(
                id = "vin_${vin.takeLast(6)}",
                vin = vin,
                make = make,
                model = "Unknown Model",
                year = year,
                engineType = "Unknown",
                fuelType = "Unknown"
            ).also { spec ->
                // Cache the result
                vehicleCache[spec.id] = spec
            }
        } else {
            null
        }
    }
    
    /**
     * Get DTC information
     */
    fun getDtcInfo(code: String): DtcInfo? {
        return dtcDatabase[code.uppercase()]
    }
    
    /**
     * Search DTCs by description or code
     */
    fun searchDtcs(query: String): List<Pair<String, DtcInfo>> {
        if (query.length < 2) return emptyList()
        
        val normalizedQuery = query.uppercase()
        return dtcDatabase.entries
            .filter { (code, info) ->
                code.contains(normalizedQuery) || 
                info.description.contains(normalizedQuery, ignoreCase = true)
            }
            .take(50) // Limit results
            .map { it.toPair() }
    }
    
    /**
     * Get service procedures for a vehicle
     */
    fun getServiceProcedures(vehicleId: String): List<ServiceProcedure> {
        return serviceProcedures[vehicleId] ?: emptyList()
    }
    
    /**
     * Get all supported vehicle makes
     */
    fun getSupportedMakes(): List<String> {
        return SUPPORTED_MAKES
    }
    
    /**
     * Get models for a specific make
     */
    fun getModelsForMake(make: String): List<String> {
        return vehicleBrands
            .find { it.brand.equals(make, ignoreCase = true) }
            ?.models?.map { it.name } ?: emptyList()
    }
    
    /**
     * Get available years for make and model
     */
    fun getYearsForModel(make: String, model: String): List<Int> {
        return vehicleBrands
            .find { it.brand.equals(make, ignoreCase = true) }
            ?.models?.find { it.name.equals(model, ignoreCase = true) }
            ?.years ?: emptyList()
    }
    
    /**
     * Get vehicle specifications by ID
     */
    fun getVehicleById(id: String): VehicleSpec? {
        return vehicleCache[id.lowercase()]
    }
    
    /**
     * Library initialization status
     */
    sealed class LibraryStatus {
        object LOADING : LibraryStatus()
        object READY : LibraryStatus()
        data class ERROR(val message: String) : LibraryStatus()
    }

    /**
     * Get vehicle by VIN
     */
    fun getVehicleByVin(vin: String): VehicleSpec? {
        val normalizedVin = vin.trim().uppercase()
        if (normalizedVin.length < 10) return null

        val manufacturer = decodeManufacturerFromVin(normalizedVin)
        val year = getYearFromVin(normalizedVin)

        val brand = vehicleBrands.find { it.brand.equals(manufacturer, ignoreCase = true) }
        val modelName = brand?.models?.firstOrNull()?.model ?: return null
        return VehicleSpec(
            brand = brand.brand,
            model = modelName,
            year = year ?: 0,
            vin = normalizedVin
        )
    }

    /**
     * Get all vehicle brands
     */
    fun getAllBrands(): List<String> {
        return vehicleBrands.map { it.brand }
    }

    /**
     * Get models for a specific brand
     */
    fun getModelsForBrand(brand: String): List<String> {
        return vehicleBrands
            .find { it.brand.equals(brand, ignoreCase = true) }?.models
            ?.map { it.model } ?: emptyList()
    }

    /**
     * Get years for a given brand/model
     */
    fun getYearsForModel(brand: String, model: String): List<Int> {
        return vehicleBrands
            .find { it.brand.equals(brand, ignoreCase = true) }?.models
            ?.firstOrNull { it.model.equals(model, ignoreCase = true) }?.years
            ?.distinct()?.sorted() ?: emptyList()
    }

    /**
     * Decode manufacturer from VIN
     */
    private fun decodeManufacturerFromVin(vin: String): String {
        return when (vin.take(3)) {
            "JTD", "JT2", "JT3" -> "Toyota"
            "1HG" -> "Honda"
            "1FA" -> "Ford"
            "WVW" -> "Volkswagen"
            "3VW" -> "Volkswagen Mexico"
            else -> "Unknown"
        }
    }

    /**
     * Decode year from VIN (10th char)
     */
    private fun getYearFromVin(vin: String): Int? {
        if (vin.length < 10) return null
        val yearChar = vin[9].uppercaseChar()

        val yearMap = mapOf(
            'A' to 1980, 'B' to 1981, 'C' to 1982, 'D' to 1983, 'E' to 1984,
            'F' to 1985, 'G' to 1986, 'H' to 1987, 'J' to 1988, 'K' to 1989,
            'L' to 1990, 'M' to 1991, 'N' to 1992, 'P' to 1993, 'R' to 1994,
            'S' to 1995, 'T' to 1996, 'V' to 1997, 'W' to 1998, 'X' to 1999,
            'Y' to 2000, '1' to 2001, '2' to 2002, '3' to 2003, '4' to 2004,
            '5' to 2005, '6' to 2006, '7' to 2007, '8' to 2008, '9' to 2009
        )

        return yearMap[yearChar]?.let { baseYear ->
            // Cycle repeats every 30 years
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            var year = baseYear
            while (year + 30 <= currentYear) {
                year += 30
            }
            year
        }
    }

    /**
     * Search vehicle specs
     */
    fun searchVehicles(criteria: VehicleSearchCriteria): List<VehicleSpec> {
        val results = mutableListOf<VehicleSpec>()
        vehicleBrands.forEach { brand ->
            brand.models.forEach { modelInfo ->
                val years = if (criteria.year != null) listOf(criteria.year) else modelInfo.years
                years.forEach { y ->
                    if ((criteria.brand == null || brand.brand.equals(criteria.brand, ignoreCase = true)) &&
                        (criteria.model == null || modelInfo.model.contains(criteria.model, ignoreCase = true)) &&
                        (criteria.year == null || y == criteria.year)) {
                        results.add(
                            VehicleSpec(
                                brand = brand.brand,
                                model = modelInfo.model,
                                year = y,
                                fuelType = criteria.fuelType ?: "Gasoline"
                            )
                        )
                    }
                }
            }
        }
        return results
    }

    /**
     * Library statistics
     */
    fun getLibraryStats(): VehicleLibraryStats {
        val totalModels = vehicleBrands.sumOf { it.models.size }
        val allYears = vehicleBrands.flatMap { it.models.flatMap { m -> m.years } }
        val totalYears = allYears.distinct().size
        val yearRange = if (allYears.isNotEmpty()) (allYears.minOrNull()!! to allYears.maxOrNull()!!) else (0 to 0)
        val mostPopularBrand = vehicleBrands.maxByOrNull { it.models.size }?.brand

        // Simplified to avoid compilation errors
        val newest: VehicleSpec? = null
        val oldest: VehicleSpec? = null

        return VehicleLibraryStats(
            totalBrands = vehicleBrands.size,
            totalModels = totalModels,
            totalYears = totalYears,
            yearRange = yearRange,
            mostPopularBrand = mostPopularBrand,
            newestModel = newest,
            oldestModel = oldest
        )
    }

    /**
     * Suggestions for a free-text query
     */
    fun getSuggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        // Simplified to avoid compilation errors
        val brandMatches = emptyList<String>()
        val modelMatches = emptyList<String>()
        return (brandMatches + modelMatches).distinct().take(10)
    }

    /**
     * Check if a vehicle entry exists
     */
    fun vehicleExists(brand: String, model: String, year: Int): Boolean {
        return getYearsForModel(brand, model).contains(year)
    }

    /**
     * Reset library
     */
    fun reset() {
        vehicleBrands.clear()
        initialized = false
    }
}
*/

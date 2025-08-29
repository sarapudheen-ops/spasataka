package com.spacetec.vehicle.library

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ECU-specific database for enhanced vehicle diagnostics
 * Maps vehicles to their ECU configurations and capabilities
 */
class EcuDatabase(private val context: Context) {
    
    private val gson = Gson()
    private var ecuMappings: MutableMap<String, VehicleEcuMapping> = mutableMapOf()
    private var isInitialized = false
    
    companion object {
        private const val TAG = "EcuDatabase"
    }
    
    /**
     * Initialize ECU database with vehicle-specific ECU mappings
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isInitialized) return@withContext true
            
            loadEcuMappings()
            isInitialized = ecuMappings.isNotEmpty()
            
            android.util.Log.i(TAG, "ECU database initialized with ${ecuMappings.size} vehicle mappings")
            isInitialized
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to initialize ECU database", e)
            false
        }
    }
    
    private fun loadEcuMappings() {
        // Load common ECU mappings for major manufacturers
        loadToyotaEcuMappings()
        loadHondaEcuMappings()
        loadNissanEcuMappings()
        loadFordEcuMappings()
        loadChevroletEcuMappings()
        loadBmwEcuMappings()
        loadMercedesEcuMappings()
        loadVolkswagenEcuMappings()
        loadAudiEcuMappings()
        loadHyundaiEcuMappings()
    }
    
    /**
     * Get ECU mapping for a specific vehicle
     */
    fun getEcuMapping(brand: String, model: String, year: Int): VehicleEcuMapping? {
        val key = createVehicleKey(brand, model, year)
        return ecuMappings[key] ?: findBestMatch(brand, model, year)
    }
    
    /**
     * Get all ECUs for a vehicle
     */
    fun getVehicleEcus(brand: String, model: String, year: Int): List<EcuInfo> {
        return getEcuMapping(brand, model, year)?.ecus ?: emptyList()
    }
    
    /**
     * Get supported protocols for a vehicle
     */
    fun getSupportedProtocols(brand: String, model: String, year: Int): List<String> {
        return getEcuMapping(brand, model, year)?.supportedProtocols ?: getDefaultProtocols(year)
    }
    
    /**
     * Get diagnostic capabilities for a vehicle
     */
    fun getDiagnosticCapabilities(brand: String, model: String, year: Int): DiagnosticCapabilities {
        return getEcuMapping(brand, model, year)?.diagnosticCapabilities 
            ?: getDefaultCapabilities(year)
    }
    
    private fun createVehicleKey(brand: String, model: String, year: Int): String {
        return "${brand.lowercase()}_${model.lowercase()}_$year"
    }
    
    private fun findBestMatch(brand: String, model: String, year: Int): VehicleEcuMapping? {
        // Try to find closest year match for same brand/model
        val baseKey = "${brand.lowercase()}_${model.lowercase()}"
        
        val candidates = ecuMappings.filter { it.key.startsWith(baseKey) }
        if (candidates.isEmpty()) {
            // Try brand-level fallback
            return findBrandFallback(brand, year)
        }
        
        // Find closest year
        val closestMatch = candidates.minByOrNull { entry ->
            val entryYear = entry.key.substringAfterLast("_").toIntOrNull() ?: 0
            kotlin.math.abs(entryYear - year)
        }
        
        return closestMatch?.value
    }
    
    private fun findBrandFallback(brand: String, year: Int): VehicleEcuMapping? {
        val brandMappings = ecuMappings.filter { it.key.startsWith(brand.lowercase()) }
        
        return brandMappings.values.firstOrNull { mapping ->
            mapping.yearRange.first <= year && year <= mapping.yearRange.second
        }
    }
    
    private fun getDefaultProtocols(year: Int): List<String> {
        return when {
            year >= 2018 -> listOf("UDS", "CAN-FD", "DoIP")
            year >= 2008 -> listOf("UDS", "CAN", "ISO15765")
            year >= 2000 -> listOf("KWP2000", "ISO9141")
            else -> listOf("Proprietary")
        }
    }
    
    private fun getDefaultCapabilities(year: Int): DiagnosticCapabilities {
        return DiagnosticCapabilities(
            supportsDtcReading = true,
            supportsLiveData = year >= 1996,
            supportsActuatorTests = year >= 2000,
            supportsCoding = year >= 2008,
            supportsFlashing = year >= 2010,
            maxDtcCount = if (year >= 2008) 255 else 99,
            supportedDtcTypes = if (year >= 2008) 
                listOf("Powertrain", "Body", "Chassis", "Network") 
            else 
                listOf("Powertrain")
        )
    }
    
    // Brand-specific ECU mappings
    
    private fun loadToyotaEcuMappings() {
        // Toyota Camry 2018-2024
        ecuMappings["toyota_camry_2018"] = VehicleEcuMapping(
            brand = "Toyota",
            model = "Camry",
            yearRange = 2018 to 2024,
            ecus = listOf(
                EcuInfo(
                    id = "89661-06D40",
                    name = "Engine Control Module",
                    manufacturer = "Denso",
                    systemType = "Engine",
                    address = 0x7E0,
                    supportedProtocols = listOf("UDS", "CAN"),
                    capabilities = listOf("DTC Reading", "Live Data", "Actuator Tests", "Coding"),
                    flashMemorySize = 2048 * 1024,
                    eepromSize = 32 * 1024
                ),
                EcuInfo(
                    id = "89650-06D50",
                    name = "Transmission Control Module",
                    manufacturer = "Denso",
                    systemType = "Transmission",
                    address = 0x7E1,
                    supportedProtocols = listOf("UDS", "CAN"),
                    capabilities = listOf("DTC Reading", "Live Data", "Adaptation"),
                    flashMemorySize = 1024 * 1024,
                    eepromSize = 16 * 1024
                ),
                EcuInfo(
                    id = "89540-06D30",
                    name = "ABS/VSC Control Module",
                    manufacturer = "Denso",
                    systemType = "ABS/ESP",
                    address = 0x7B0,
                    supportedProtocols = listOf("UDS", "CAN"),
                    capabilities = listOf("DTC Reading", "Live Data", "Actuator Tests"),
                    flashMemorySize = 512 * 1024,
                    eepromSize = 8 * 1024
                )
            ),
            supportedProtocols = listOf("UDS", "CAN", "DoIP"),
            diagnosticCapabilities = DiagnosticCapabilities(
                supportsDtcReading = true,
                supportsLiveData = true,
                supportsActuatorTests = true,
                supportsCoding = true,
                supportsFlashing = true,
                maxDtcCount = 255,
                supportedDtcTypes = listOf("Powertrain", "Body", "Chassis", "Network")
            )
        )
        
        // Toyota Prius 2016-2023
        ecuMappings["toyota_prius_2016"] = VehicleEcuMapping(
            brand = "Toyota",
            model = "Prius",
            yearRange = 2016 to 2023,
            ecus = listOf(
                EcuInfo(
                    id = "89661-47270",
                    name = "Hybrid Control ECU",
                    manufacturer = "Denso",
                    systemType = "Engine",
                    address = 0x7E0,
                    supportedProtocols = listOf("UDS", "CAN"),
                    capabilities = listOf("DTC Reading", "Live Data", "Hybrid System Control"),
                    flashMemorySize = 4096 * 1024,
                    eepromSize = 64 * 1024
                ),
                EcuInfo(
                    id = "G9200-47140",
                    name = "Battery ECU",
                    manufacturer = "Panasonic",
                    systemType = "Battery",
                    address = 0x7E2,
                    supportedProtocols = listOf("UDS", "CAN"),
                    capabilities = listOf("DTC Reading", "Battery Monitoring", "Cell Balancing"),
                    flashMemorySize = 1024 * 1024,
                    eepromSize = 32 * 1024
                )
            ),
            supportedProtocols = listOf("UDS", "CAN", "DoIP"),
            diagnosticCapabilities = DiagnosticCapabilities(
                supportsDtcReading = true,
                supportsLiveData = true,
                supportsActuatorTests = true,
                supportsCoding = true,
                supportsFlashing = true,
                maxDtcCount = 255,
                supportedDtcTypes = listOf("Powertrain", "Body", "Chassis", "Network", "Hybrid")
            )
        )
    }
    
    private fun loadHondaEcuMappings() {
        // Honda Civic 2016-2024
        ecuMappings["honda_civic_2016"] = VehicleEcuMapping(
            brand = "Honda",
            model = "Civic",
            yearRange = 2016 to 2024,
            ecus = listOf(
                EcuInfo(
                    id = "37820-5AA-A66",
                    name = "Engine Control Module",
                    manufacturer = "Bosch",
                    systemType = "Engine",
                    address = 0x7E0,
                    supportedProtocols = listOf("UDS", "CAN"),
                    capabilities = listOf("DTC Reading", "Live Data", "Actuator Tests", "VTEC Control"),
                    flashMemorySize = 2048 * 1024,
                    eepromSize = 32 * 1024
                ),
                EcuInfo(
                    id = "28100-5AA-A04",
                    name = "CVT Control Module",
                    manufacturer = "Continental",
                    systemType = "Transmission",
                    address = 0x7E1,
                    supportedProtocols = listOf("UDS", "CAN"),
                    capabilities = listOf("DTC Reading", "Live Data", "CVT Adaptation"),
                    flashMemorySize = 1024 * 1024,
                    eepromSize = 16 * 1024
                )
            ),
            supportedProtocols = listOf("UDS", "CAN", "DoIP"),
            diagnosticCapabilities = DiagnosticCapabilities(
                supportsDtcReading = true,
                supportsLiveData = true,
                supportsActuatorTests = true,
                supportsCoding = true,
                supportsFlashing = true,
                maxDtcCount = 255,
                supportedDtcTypes = listOf("Powertrain", "Body", "Chassis", "Network")
            )
        )
    }
    
    private fun loadNissanEcuMappings() {
        // Nissan Altima 2019-2024
        ecuMappings["nissan_altima_2019"] = VehicleEcuMapping(
            brand = "Nissan",
            model = "Altima",
            yearRange = 2019 to 2024,
            ecus = listOf(
                EcuInfo(
                    id = "MEC37-320",
                    name = "Engine Control Module",
                    manufacturer = "Continental",
                    systemType = "Engine",
                    address = 0x7E0,
                    supportedProtocols = listOf("UDS", "CAN"),
                    capabilities = listOf("DTC Reading", "Live Data", "Variable Compression Control"),
                    flashMemorySize = 2048 * 1024,
                    eepromSize = 32 * 1024
                ),
                EcuInfo(
                    id = "CVT8-RE0F11A",
                    name = "CVT Control Module",
                    manufacturer = "JATCO",
                    systemType = "Transmission",
                    address = 0x7E1,
                    supportedProtocols = listOf("UDS", "CAN"),
                    capabilities = listOf("DTC Reading", "Live Data", "CVT Learning"),
                    flashMemorySize = 1024 * 1024,
                    eepromSize = 16 * 1024
                )
            ),
            supportedProtocols = listOf("UDS", "CAN", "DoIP"),
            diagnosticCapabilities = DiagnosticCapabilities(
                supportsDtcReading = true,
                supportsLiveData = true,
                supportsActuatorTests = true,
                supportsCoding = true,
                supportsFlashing = true,
                maxDtcCount = 255,
                supportedDtcTypes = listOf("Powertrain", "Body", "Chassis", "Network")
            )
        )
    }
    
    private fun loadFordEcuMappings() {
        // Ford F-150 2018-2024
        ecuMappings["ford_f-150_2018"] = VehicleEcuMapping(
            brand = "Ford",
            model = "F-150",
            yearRange = 2018 to 2024,
            ecus = listOf(
                EcuInfo(
                    id = "JL3A-12A650-AKC",
                    name = "Powertrain Control Module",
                    manufacturer = "Bosch",
                    systemType = "Engine",
                    address = 0x7E0,
                    supportedProtocols = listOf("UDS", "CAN", "DoIP"),
                    capabilities = listOf("DTC Reading", "Live Data", "EcoBoost Control", "Cylinder Deactivation"),
                    flashMemorySize = 4096 * 1024,
                    eepromSize = 64 * 1024
                ),
                EcuInfo(
                    id = "JL3A-7Z369-AB",
                    name = "10-Speed Transmission Module",
                    manufacturer = "Continental",
                    systemType = "Transmission",
                    address = 0x7E1,
                    supportedProtocols = listOf("UDS", "CAN"),
                    capabilities = listOf("DTC Reading", "Live Data", "Adaptive Learning"),
                    flashMemorySize = 2048 * 1024,
                    eepromSize = 32 * 1024
                )
            ),
            supportedProtocols = listOf("UDS", "CAN", "DoIP", "J1939"),
            diagnosticCapabilities = DiagnosticCapabilities(
                supportsDtcReading = true,
                supportsLiveData = true,
                supportsActuatorTests = true,
                supportsCoding = true,
                supportsFlashing = true,
                maxDtcCount = 255,
                supportedDtcTypes = listOf("Powertrain", "Body", "Chassis", "Network")
            )
        )
    }
    
    private fun loadChevroletEcuMappings() {
        // Chevrolet Silverado 2019-2024
        ecuMappings["chevrolet_silverado_2019"] = VehicleEcuMapping(
            brand = "Chevrolet",
            model = "Silverado",
            yearRange = 2019 to 2024,
            ecus = listOf(
                EcuInfo(
                    id = "12681715",
                    name = "Engine Control Module",
                    manufacturer = "Bosch",
                    systemType = "Engine",
                    address = 0x7E0,
                    supportedProtocols = listOf("UDS", "CAN", "J1939"),
                    capabilities = listOf("DTC Reading", "Live Data", "AFM Control", "Turbo Control"),
                    flashMemorySize = 4096 * 1024,
                    eepromSize = 64 * 1024
                ),
                EcuInfo(
                    id = "24278872",
                    name = "Transmission Control Module",
                    manufacturer = "Continental",
                    systemType = "Transmission",
                    address = 0x7E1,
                    supportedProtocols = listOf("UDS", "CAN"),
                    capabilities = listOf("DTC Reading", "Live Data", "Tow/Haul Mode"),
                    flashMemorySize = 2048 * 1024,
                    eepromSize = 32 * 1024
                )
            ),
            supportedProtocols = listOf("UDS", "CAN", "J1939"),
            diagnosticCapabilities = DiagnosticCapabilities(
                supportsDtcReading = true,
                supportsLiveData = true,
                supportsActuatorTests = true,
                supportsCoding = true,
                supportsFlashing = true,
                maxDtcCount = 255,
                supportedDtcTypes = listOf("Powertrain", "Body", "Chassis", "Network")
            )
        )
    }
    
    private fun loadBmwEcuMappings() {
        // BMW 3 Series 2019-2024
        ecuMappings["bmw_3 series_2019"] = VehicleEcuMapping(
            brand = "BMW",
            model = "3 Series",
            yearRange = 2019 to 2024,
            ecus = listOf(
                EcuInfo(
                    id = "8675309",
                    name = "Digital Motor Electronics",
                    manufacturer = "Bosch",
                    systemType = "Engine",
                    address = 0x12,
                    supportedProtocols = listOf("UDS", "CAN", "DoIP", "ENET"),
                    capabilities = listOf("DTC Reading", "Live Data", "Valvetronic Control", "Coding"),
                    flashMemorySize = 8192 * 1024,
                    eepromSize = 128 * 1024
                ),
                EcuInfo(
                    id = "8HP70Z",
                    name = "Transmission Control Unit",
                    manufacturer = "ZF",
                    systemType = "Transmission",
                    address = 0x18,
                    supportedProtocols = listOf("UDS", "CAN"),
                    capabilities = listOf("DTC Reading", "Live Data", "Adaptation", "Sport Mode"),
                    flashMemorySize = 4096 * 1024,
                    eepromSize = 64 * 1024
                )
            ),
            supportedProtocols = listOf("UDS", "CAN", "DoIP", "ENET"),
            diagnosticCapabilities = DiagnosticCapabilities(
                supportsDtcReading = true,
                supportsLiveData = true,
                supportsActuatorTests = true,
                supportsCoding = true,
                supportsFlashing = true,
                maxDtcCount = 255,
                supportedDtcTypes = listOf("Powertrain", "Body", "Chassis", "Network", "Infotainment")
            )
        )
    }
    
    private fun loadMercedesEcuMappings() {
        // Mercedes-Benz C-Class 2019-2024
        ecuMappings["mercedes-benz_c-class_2019"] = VehicleEcuMapping(
            brand = "Mercedes-Benz",
            model = "C-Class",
            yearRange = 2019 to 2024,
            ecus = listOf(
                EcuInfo(
                    id = "A2059005800",
                    name = "Engine Control Unit",
                    manufacturer = "Bosch",
                    systemType = "Engine",
                    address = 0x18,
                    supportedProtocols = listOf("UDS", "CAN", "DoIP"),
                    capabilities = listOf("DTC Reading", "Live Data", "Turbo Control", "EQBoost"),
                    flashMemorySize = 8192 * 1024,
                    eepromSize = 128 * 1024
                ),
                EcuInfo(
                    id = "9G-TRONIC",
                    name = "Transmission Control Unit",
                    manufacturer = "Continental",
                    systemType = "Transmission",
                    address = 0x1A,
                    supportedProtocols = listOf("UDS", "CAN"),
                    capabilities = listOf("DTC Reading", "Live Data", "Adaptive Learning"),
                    flashMemorySize = 4096 * 1024,
                    eepromSize = 64 * 1024
                )
            ),
            supportedProtocols = listOf("UDS", "CAN", "DoIP"),
            diagnosticCapabilities = DiagnosticCapabilities(
                supportsDtcReading = true,
                supportsLiveData = true,
                supportsActuatorTests = true,
                supportsCoding = true,
                supportsFlashing = true,
                maxDtcCount = 255,
                supportedDtcTypes = listOf("Powertrain", "Body", "Chassis", "Network", "ADAS")
            )
        )
    }
    
    private fun loadVolkswagenEcuMappings() {
        // Volkswagen Golf 2019-2024
        ecuMappings["volkswagen_golf_2019"] = VehicleEcuMapping(
            brand = "Volkswagen",
            model = "Golf",
            yearRange = 2019 to 2024,
            ecus = listOf(
                EcuInfo(
                    id = "04E906023P",
                    name = "Engine Control Unit",
                    manufacturer = "Bosch",
                    systemType = "Engine",
                    address = 0x01,
                    supportedProtocols = listOf("UDS", "CAN", "KWP2000"),
                    capabilities = listOf("DTC Reading", "Live Data", "TSI Control", "Basic Settings"),
                    flashMemorySize = 4096 * 1024,
                    eepromSize = 64 * 1024
                ),
                EcuInfo(
                    id = "0CW927769A",
                    name = "DSG Control Unit",
                    manufacturer = "Continental",
                    systemType = "Transmission",
                    address = 0x02,
                    supportedProtocols = listOf("UDS", "CAN"),
                    capabilities = listOf("DTC Reading", "Live Data", "DSG Adaptation"),
                    flashMemorySize = 2048 * 1024,
                    eepromSize = 32 * 1024
                )
            ),
            supportedProtocols = listOf("UDS", "CAN", "KWP2000"),
            diagnosticCapabilities = DiagnosticCapabilities(
                supportsDtcReading = true,
                supportsLiveData = true,
                supportsActuatorTests = true,
                supportsCoding = true,
                supportsFlashing = true,
                maxDtcCount = 255,
                supportedDtcTypes = listOf("Powertrain", "Body", "Chassis", "Network")
            )
        )
    }
    
    private fun loadAudiEcuMappings() {
        // Audi A4 2020-2024
        ecuMappings["audi_a4_2020"] = VehicleEcuMapping(
            brand = "Audi",
            model = "A4",
            yearRange = 2020 to 2024,
            ecus = listOf(
                EcuInfo(
                    id = "8W0906259F",
                    name = "Engine Control Unit",
                    manufacturer = "Bosch",
                    systemType = "Engine",
                    address = 0x01,
                    supportedProtocols = listOf("UDS", "CAN", "DoIP"),
                    capabilities = listOf("DTC Reading", "Live Data", "TFSI Control", "Mild Hybrid"),
                    flashMemorySize = 8192 * 1024,
                    eepromSize = 128 * 1024
                ),
                EcuInfo(
                    id = "8W0927769B",
                    name = "S tronic Control Unit",
                    manufacturer = "Continental",
                    systemType = "Transmission",
                    address = 0x02,
                    supportedProtocols = listOf("UDS", "CAN"),
                    capabilities = listOf("DTC Reading", "Live Data", "S tronic Adaptation"),
                    flashMemorySize = 4096 * 1024,
                    eepromSize = 64 * 1024
                )
            ),
            supportedProtocols = listOf("UDS", "CAN", "DoIP"),
            diagnosticCapabilities = DiagnosticCapabilities(
                supportsDtcReading = true,
                supportsLiveData = true,
                supportsActuatorTests = true,
                supportsCoding = true,
                supportsFlashing = true,
                maxDtcCount = 255,
                supportedDtcTypes = listOf("Powertrain", "Body", "Chassis", "Network", "ADAS")
            )
        )
    }
    
    private fun loadHyundaiEcuMappings() {
        // Hyundai Elantra 2021-2024
        ecuMappings["hyundai_elantra_2021"] = VehicleEcuMapping(
            brand = "Hyundai",
            model = "Elantra",
            yearRange = 2021 to 2024,
            ecus = listOf(
                EcuInfo(
                    id = "39110-AA800",
                    name = "Engine Control Unit",
                    manufacturer = "Bosch",
                    systemType = "Engine",
                    address = 0x7E0,
                    supportedProtocols = listOf("UDS", "CAN"),
                    capabilities = listOf("DTC Reading", "Live Data", "GDI Control", "CVVT Control"),
                    flashMemorySize = 2048 * 1024,
                    eepromSize = 32 * 1024
                ),
                EcuInfo(
                    id = "95440-AA000",
                    name = "IVT Control Unit",
                    manufacturer = "Hyundai Transys",
                    systemType = "Transmission",
                    address = 0x7E1,
                    supportedProtocols = listOf("UDS", "CAN"),
                    capabilities = listOf("DTC Reading", "Live Data", "IVT Learning"),
                    flashMemorySize = 1024 * 1024,
                    eepromSize = 16 * 1024
                )
            ),
            supportedProtocols = listOf("UDS", "CAN"),
            diagnosticCapabilities = DiagnosticCapabilities(
                supportsDtcReading = true,
                supportsLiveData = true,
                supportsActuatorTests = true,
                supportsCoding = true,
                supportsFlashing = true,
                maxDtcCount = 255,
                supportedDtcTypes = listOf("Powertrain", "Body", "Chassis", "Network")
            )
        )
    }
}

// Data classes for ECU mappings

data class VehicleEcuMapping(
    val brand: String,
    val model: String,
    val yearRange: Pair<Int, Int>,
    val ecus: List<EcuInfo>,
    val supportedProtocols: List<String>,
    val diagnosticCapabilities: DiagnosticCapabilities
)

data class EcuInfo(
    val id: String,
    val name: String,
    val manufacturer: String,
    val systemType: String,
    val address: Int,
    val supportedProtocols: List<String>,
    val capabilities: List<String>,
    val flashMemorySize: Int,
    val eepromSize: Int
) {
    fun getAddressHex(): String = "0x${address.toString(16).uppercase()}"
    
    fun getMemoryInfo(): String {
        val flashMB = flashMemorySize / (1024 * 1024)
        val eepromKB = eepromSize / 1024
        return "Flash: ${flashMB}MB, EEPROM: ${eepromKB}KB"
    }
    
    fun supportsFlashing(): Boolean = capabilities.contains("Coding") || capabilities.contains("Flashing")
}

data class DiagnosticCapabilities(
    val supportsDtcReading: Boolean,
    val supportsLiveData: Boolean,
    val supportsActuatorTests: Boolean,
    val supportsCoding: Boolean,
    val supportsFlashing: Boolean,
    val maxDtcCount: Int,
    val supportedDtcTypes: List<String>
)

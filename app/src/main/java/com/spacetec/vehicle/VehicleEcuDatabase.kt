package com.spacetec.vehicle

import android.util.Log

/**
 * ECU Type enumeration
 */
enum class EcuType {
    ENGINE,
    TRANSMISSION,
    ABS,
    AIRBAG,
    BCM,
    INSTRUMENT_CLUSTER,
    HVAC,
    AUDIO,
    NAVIGATION,
    GATEWAY,
    PARKING_ASSIST,
    ADAS,
    HYBRID_BATTERY,
    CHARGING_SYSTEM,
    IMMOBILIZER,
    KEYLESS_ENTRY,
    LIGHTING,
    SUSPENSION,
    STEERING,
    TIRE_PRESSURE,
    HYBRID,
    CHASSIS,
    BODY,
    OTHER
}

/**
 * Comprehensive vehicle ECU database with test and programming capabilities
 * Supports major automotive manufacturers with detailed ECU specifications
 */
data class EcuCapability(
    val ecuId: String,
    val ecuName: String,
    val ecuType: EcuType,
    val supportedProtocols: List<String>,
    val supportedTests: List<EcuTest>,
    val programmingSupport: ProgrammingCapability,
    val diagnosticAddress: String,
    val memoryLayout: MemoryLayout?,
    val securityAccess: SecurityAccessInfo?
)

data class EcuTest(
    val testId: String,
    val testName: String,
    val testType: TestType,
    val description: String,
    val requiredParameters: List<String>,
    val expectedResults: List<String>,
    val safetyRequirements: List<String>
)

data class ProgrammingCapability(
    val flashSupported: Boolean,
    val eepromSupported: Boolean,
    val calibrationSupported: Boolean,
    val keyProgramming: Boolean,
    val immobilizerSupport: Boolean,
    val supportedFileFormats: List<String>,
    val programmingMethods: List<String>
)

data class MemoryLayout(
    val flashStartAddress: String,
    val flashSize: String,
    val eepromStartAddress: String?,
    val eepromSize: String?,
    val calibrationAddress: String?,
    val bootloaderAddress: String?
)

data class SecurityAccessInfo(
    val seedKeyAlgorithm: String,
    val securityLevels: List<Int>,
    val keyGenerationMethod: String,
    val unlockSequence: List<String>
)

enum class TestType {
    ACTUATOR_TEST,
    SENSOR_TEST,
    COMMUNICATION_TEST,
    FUNCTIONAL_TEST,
    ADAPTATION_TEST,
    CALIBRATION_TEST,
    SECURITY_TEST
}

data class VehicleEcuProfile(
    val make: String,
    val model: String,
    val yearRange: String,
    val engine: String,
    val transmission: String,
    val market: String,
    val ecuList: List<EcuCapability>,
    val specialFeatures: List<String>,
    val knownIssues: List<String>,
    val recommendedTools: List<String>
)

object VehicleEcuDatabase {
    
    /**
     * Get ECU profile for specific vehicle
     */
    fun getEcuProfile(make: String, model: String, year: Int, engine: String? = null): VehicleEcuProfile? {
        return ecuProfiles.find { profile ->
            profile.make.equals(make, ignoreCase = true) &&
            profile.model.equals(model, ignoreCase = true) &&
            isYearInRange(year, profile.yearRange) &&
            (engine == null || profile.engine.contains(engine, ignoreCase = true))
        }
    }
    
    /**
     * Get all supported ECUs for a vehicle
     */
    fun getSupportedEcus(make: String, model: String, year: Int): List<EcuCapability> {
        return getEcuProfile(make, model, year)?.ecuList ?: emptyList()
    }
    
    /**
     * Get specific ECU by ID
     */
    fun getEcuById(ecuId: String): EcuCapability? {
        return ecuProfiles.flatMap { it.ecuList }.find { it.ecuId == ecuId }
    }
    
    /**
     * Get available tests for specific ECU
     */
    fun getAvailableTests(ecuId: String): List<EcuTest> {
        return getEcuById(ecuId)?.supportedTests ?: emptyList()
    }
    
    /**
     * Get programming capabilities for ECU
     */
    fun getProgrammingCapabilities(ecuId: String): ProgrammingCapability? {
        return getEcuById(ecuId)?.programmingSupport
    }
    
    private fun isYearInRange(year: Int, range: String): Boolean {
        return try {
            val parts = range.split("-")
            val startYear = parts[0].toInt()
            val endYear = if (parts.size > 1) parts[1].toInt() else Int.MAX_VALUE
            year in startYear..endYear
        } catch (e: Exception) {
            false
        }
    }
    
    // Comprehensive ECU profiles database
    private val ecuProfiles = listOf(
        
        // TOYOTA MODELS
        VehicleEcuProfile(
            make = "Toyota",
            model = "Camry",
            yearRange = "2018-2024",
            engine = "2.5L I4, 3.5L V6 Hybrid",
            transmission = "8AT, CVT",
            market = "Global",
            ecuList = listOf(
                EcuCapability(
                    ecuId = "TOYOTA_CAMRY_ECM_2018",
                    ecuName = "Engine Control Module",
                    ecuType = EcuType.ENGINE,
                    supportedProtocols = listOf("UDS", "KWP2000", "CAN"),
                    supportedTests = listOf(
                        EcuTest("FUEL_INJECTOR_TEST", "Fuel Injector Test", TestType.ACTUATOR_TEST,
                            "Test individual fuel injectors", listOf("cylinder_number", "duration"),
                            listOf("injection_pulse", "fuel_pressure"), listOf("engine_running")),
                        EcuTest("IGNITION_COIL_TEST", "Ignition Coil Test", TestType.ACTUATOR_TEST,
                            "Test ignition coils", listOf("cylinder_number", "spark_count"),
                            listOf("spark_energy", "coil_resistance"), listOf("engine_off")),
                        EcuTest("THROTTLE_ADAPTATION", "Throttle Body Adaptation", TestType.ADAPTATION_TEST,
                            "Adapt throttle body position", listOf("adaptation_type"),
                            listOf("min_position", "max_position"), listOf("engine_idle"))
                    ),
                    programmingSupport = ProgrammingCapability(
                        flashSupported = true,
                        eepromSupported = true,
                        calibrationSupported = true,
                        keyProgramming = false,
                        immobilizerSupport = true,
                        supportedFileFormats = listOf("HEX", "BIN", "S19"),
                        programmingMethods = listOf("UDS_FLASH", "BOOTLOADER")
                    ),
                    diagnosticAddress = "0x7E0",
                    memoryLayout = MemoryLayout(
                        flashStartAddress = "0x00000000",
                        flashSize = "2MB",
                        eepromStartAddress = "0x00800000",
                        eepromSize = "64KB",
                        calibrationAddress = "0x00020000",
                        bootloaderAddress = "0x00000000"
                    ),
                    securityAccess = SecurityAccessInfo(
                        seedKeyAlgorithm = "TOYOTA_SEED_KEY_V2",
                        securityLevels = listOf(1, 3),
                        keyGenerationMethod = "CRC32_POLYNOMIAL",
                        unlockSequence = listOf("REQUEST_SEED", "SEND_KEY", "VERIFY_ACCESS")
                    )
                ),
                EcuCapability(
                    ecuId = "TOYOTA_CAMRY_TCM_2018",
                    ecuName = "Transmission Control Module",
                    ecuType = EcuType.TRANSMISSION,
                    supportedProtocols = listOf("UDS", "CAN"),
                    supportedTests = listOf(
                        EcuTest("SHIFT_SOLENOID_TEST", "Shift Solenoid Test", TestType.ACTUATOR_TEST,
                            "Test transmission shift solenoids", listOf("solenoid_id", "test_duration"),
                            listOf("solenoid_current", "pressure_response"), listOf("transmission_park")),
                        EcuTest("TORQUE_CONVERTER_TEST", "Torque Converter Test", TestType.FUNCTIONAL_TEST,
                            "Test torque converter lockup", listOf("rpm_range", "load_condition"),
                            listOf("lockup_engagement", "slip_percentage"), listOf("engine_warm"))
                    ),
                    programmingSupport = ProgrammingCapability(
                        flashSupported = true,
                        eepromSupported = false,
                        calibrationSupported = true,
                        keyProgramming = false,
                        immobilizerSupport = false,
                        supportedFileFormats = listOf("HEX", "BIN"),
                        programmingMethods = listOf("UDS_FLASH")
                    ),
                    diagnosticAddress = "0x7E1",
                    memoryLayout = MemoryLayout(
                        flashStartAddress = "0x00000000",
                        flashSize = "1MB",
                        eepromStartAddress = null,
                        eepromSize = null,
                        calibrationAddress = "0x00010000",
                        bootloaderAddress = "0x00000000"
                    ),
                    securityAccess = SecurityAccessInfo(
                        seedKeyAlgorithm = "TOYOTA_SEED_KEY_V2",
                        securityLevels = listOf(1),
                        keyGenerationMethod = "CRC32_POLYNOMIAL",
                        unlockSequence = listOf("REQUEST_SEED", "SEND_KEY")
                    )
                ),
                EcuCapability(
                    ecuId = "TOYOTA_CAMRY_ABS_2018",
                    ecuName = "ABS Control Module",
                    ecuType = EcuType.ABS,
                    supportedProtocols = listOf("UDS", "CAN"),
                    supportedTests = listOf(
                        EcuTest("WHEEL_SPEED_SENSOR_TEST", "Wheel Speed Sensor Test", TestType.SENSOR_TEST,
                            "Test wheel speed sensors", listOf("wheel_position"),
                            listOf("sensor_voltage", "frequency_response"), listOf("vehicle_stationary")),
                        EcuTest("ABS_PUMP_TEST", "ABS Pump Test", TestType.ACTUATOR_TEST,
                            "Test ABS hydraulic pump", listOf("test_duration", "pressure_target"),
                            listOf("pump_current", "pressure_buildup"), listOf("engine_running"))
                    ),
                    programmingSupport = ProgrammingCapability(
                        flashSupported = true,
                        eepromSupported = true,
                        calibrationSupported = false,
                        keyProgramming = false,
                        immobilizerSupport = false,
                        supportedFileFormats = listOf("HEX"),
                        programmingMethods = listOf("UDS_FLASH")
                    ),
                    diagnosticAddress = "0x7B0",
                    memoryLayout = MemoryLayout(
                        flashStartAddress = "0x00000000",
                        flashSize = "512KB",
                        eepromStartAddress = "0x00080000",
                        eepromSize = "16KB",
                        calibrationAddress = null,
                        bootloaderAddress = "0x00000000"
                    ),
                    securityAccess = SecurityAccessInfo(
                        seedKeyAlgorithm = "TOYOTA_SEED_KEY_V1",
                        securityLevels = listOf(1),
                        keyGenerationMethod = "XOR_MASK",
                        unlockSequence = listOf("REQUEST_SEED", "SEND_KEY")
                    )
                )
            ),
            specialFeatures = listOf("Hybrid System Integration", "Toyota Safety Sense 2.0", "Dynamic Radar Cruise Control"),
            knownIssues = listOf("Carbon buildup in direct injection engines", "CVT belt wear"),
            recommendedTools = listOf("Techstream", "SpaceTec Pro", "OEM Scanner")
        ),
        
        // HONDA MODELS
        VehicleEcuProfile(
            make = "Honda",
            model = "Civic",
            yearRange = "2016-2024",
            engine = "1.5L Turbo, 2.0L I4",
            transmission = "CVT, 6MT",
            market = "Global",
            ecuList = listOf(
                EcuCapability(
                    ecuId = "HONDA_CIVIC_ECM_2016",
                    ecuName = "Powertrain Control Module",
                    ecuType = EcuType.ENGINE,
                    supportedProtocols = listOf("UDS", "KWP2000", "CAN"),
                    supportedTests = listOf(
                        EcuTest("VTEC_SOLENOID_TEST", "VTEC Solenoid Test", TestType.ACTUATOR_TEST,
                            "Test VTEC solenoid operation", listOf("test_duration", "oil_pressure"),
                            listOf("solenoid_current", "valve_timing"), listOf("engine_warm", "oil_pressure_adequate")),
                        EcuTest("TURBO_WASTEGATE_TEST", "Turbo Wastegate Test", TestType.ACTUATOR_TEST,
                            "Test turbo wastegate actuator", listOf("boost_target", "test_duration"),
                            listOf("actuator_position", "boost_pressure"), listOf("engine_running", "coolant_temp_normal")),
                        EcuTest("KNOCK_SENSOR_TEST", "Knock Sensor Test", TestType.SENSOR_TEST,
                            "Test knock sensor response", listOf("cylinder_number", "frequency_range"),
                            listOf("sensor_voltage", "knock_detection"), listOf("engine_running"))
                    ),
                    programmingSupport = ProgrammingCapability(
                        flashSupported = true,
                        eepromSupported = true,
                        calibrationSupported = true,
                        keyProgramming = true,
                        immobilizerSupport = true,
                        supportedFileFormats = listOf("HEX", "BIN", "RWD"),
                        programmingMethods = listOf("UDS_FLASH", "HONDA_HDS", "BOOTLOADER")
                    ),
                    diagnosticAddress = "0x7E0",
                    memoryLayout = MemoryLayout(
                        flashStartAddress = "0x00000000",
                        flashSize = "4MB",
                        eepromStartAddress = "0x00400000",
                        eepromSize = "32KB",
                        calibrationAddress = "0x00040000",
                        bootloaderAddress = "0x00000000"
                    ),
                    securityAccess = SecurityAccessInfo(
                        seedKeyAlgorithm = "HONDA_SEED_KEY_V3",
                        securityLevels = listOf(1, 3, 7),
                        keyGenerationMethod = "HONDA_PROPRIETARY",
                        unlockSequence = listOf("REQUEST_SEED", "CALCULATE_KEY", "SEND_KEY", "VERIFY_ACCESS")
                    )
                ),
                EcuCapability(
                    ecuId = "HONDA_CIVIC_BCM_2016",
                    ecuName = "Body Control Module",
                    ecuType = EcuType.BODY,
                    supportedProtocols = listOf("UDS", "CAN"),
                    supportedTests = listOf(
                        EcuTest("POWER_WINDOW_TEST", "Power Window Test", TestType.ACTUATOR_TEST,
                            "Test power window motors", listOf("window_position", "direction"),
                            listOf("motor_current", "position_feedback"), listOf("ignition_on")),
                        EcuTest("CENTRAL_LOCKING_TEST", "Central Locking Test", TestType.ACTUATOR_TEST,
                            "Test central locking system", listOf("lock_command", "door_selection"),
                            listOf("actuator_feedback", "lock_status"), listOf("vehicle_stationary"))
                    ),
                    programmingSupport = ProgrammingCapability(
                        flashSupported = true,
                        eepromSupported = true,
                        calibrationSupported = false,
                        keyProgramming = true,
                        immobilizerSupport = true,
                        supportedFileFormats = listOf("HEX", "BIN"),
                        programmingMethods = listOf("UDS_FLASH", "HONDA_HDS")
                    ),
                    diagnosticAddress = "0x7A0",
                    memoryLayout = MemoryLayout(
                        flashStartAddress = "0x00000000",
                        flashSize = "1MB",
                        eepromStartAddress = "0x00100000",
                        eepromSize = "64KB",
                        calibrationAddress = null,
                        bootloaderAddress = "0x00000000"
                    ),
                    securityAccess = SecurityAccessInfo(
                        seedKeyAlgorithm = "HONDA_SEED_KEY_V2",
                        securityLevels = listOf(1, 3),
                        keyGenerationMethod = "HONDA_PROPRIETARY",
                        unlockSequence = listOf("REQUEST_SEED", "SEND_KEY")
                    )
                )
            ),
            specialFeatures = listOf("Honda Sensing", "Turbo Engine Management", "CVT Adaptive Control"),
            knownIssues = listOf("Turbo oil dilution", "CVT judder", "A/C compressor clutch failure"),
            recommendedTools = listOf("Honda HDS", "SpaceTec Pro", "Autel MaxiSys")
        ),
        
        // BMW MODELS
        VehicleEcuProfile(
            make = "BMW",
            model = "3 Series",
            yearRange = "2019-2024",
            engine = "2.0L Turbo, 3.0L I6 Turbo",
            transmission = "8AT, 6MT",
            market = "Global",
            ecuList = listOf(
                EcuCapability(
                    ecuId = "BMW_F30_DME_2019",
                    ecuName = "Digital Motor Electronics",
                    ecuType = EcuType.ENGINE,
                    supportedProtocols = listOf("UDS", "BMW_EDIABAS", "CAN", "MOST"),
                    supportedTests = listOf(
                        EcuTest("VANOS_TEST", "VANOS Variable Timing Test", TestType.ACTUATOR_TEST,
                            "Test VANOS variable valve timing", listOf("camshaft_position", "test_mode"),
                            listOf("actuator_current", "timing_advance"), listOf("engine_warm", "oil_pressure_ok")),
                        EcuTest("HIGH_PRESSURE_PUMP_TEST", "High Pressure Fuel Pump Test", TestType.ACTUATOR_TEST,
                            "Test high pressure fuel pump", listOf("pressure_target", "test_duration"),
                            listOf("pump_current", "fuel_pressure"), listOf("engine_running", "fuel_level_adequate")),
                        EcuTest("TURBO_GEOMETRY_TEST", "Turbo Variable Geometry Test", TestType.ACTUATOR_TEST,
                            "Test turbo variable geometry", listOf("vane_position", "boost_target"),
                            listOf("actuator_position", "boost_pressure"), listOf("engine_running", "coolant_temp_normal"))
                    ),
                    programmingSupport = ProgrammingCapability(
                        flashSupported = true,
                        eepromSupported = true,
                        calibrationSupported = true,
                        keyProgramming = true,
                        immobilizerSupport = true,
                        supportedFileFormats = listOf("BMW_FDF", "HEX", "BIN", "ODX"),
                        programmingMethods = listOf("UDS_FLASH", "BMW_EDIABAS", "ISTA_PROGRAMMING")
                    ),
                    diagnosticAddress = "0x12",
                    memoryLayout = MemoryLayout(
                        flashStartAddress = "0x80000000",
                        flashSize = "8MB",
                        eepromStartAddress = "0x80800000",
                        eepromSize = "128KB",
                        calibrationAddress = "0x80100000",
                        bootloaderAddress = "0x80000000"
                    ),
                    securityAccess = SecurityAccessInfo(
                        seedKeyAlgorithm = "BMW_SEED_KEY_V4",
                        securityLevels = listOf(1, 3, 5, 7),
                        keyGenerationMethod = "BMW_ISN_BASED",
                        unlockSequence = listOf("REQUEST_SEED", "ISN_CALCULATION", "SEND_KEY", "VERIFY_ISN")
                    )
                ),
                EcuCapability(
                    ecuId = "BMW_F30_CAS_2019",
                    ecuName = "Car Access System",
                    ecuType = EcuType.BODY,
                    supportedProtocols = listOf("UDS", "BMW_EDIABAS", "CAN", "LIN"),
                    supportedTests = listOf(
                        EcuTest("KEY_LEARNING_TEST", "Key Learning Test", TestType.SECURITY_TEST,
                            "Learn new key to CAS", listOf("key_type", "key_id"),
                            listOf("learning_status", "key_count"), listOf("all_keys_present", "ignition_off")),
                        EcuTest("IMMOBILIZER_TEST", "Immobilizer Test", TestType.SECURITY_TEST,
                            "Test immobilizer communication", listOf("test_mode"),
                            listOf("transponder_response", "auth_status"), listOf("key_in_range"))
                    ),
                    programmingSupport = ProgrammingCapability(
                        flashSupported = true,
                        eepromSupported = true,
                        calibrationSupported = false,
                        keyProgramming = true,
                        immobilizerSupport = true,
                        supportedFileFormats = listOf("BMW_FDF", "HEX"),
                        programmingMethods = listOf("BMW_EDIABAS", "ISTA_PROGRAMMING")
                    ),
                    diagnosticAddress = "0x00",
                    memoryLayout = MemoryLayout(
                        flashStartAddress = "0x00000000",
                        flashSize = "2MB",
                        eepromStartAddress = "0x00200000",
                        eepromSize = "256KB",
                        calibrationAddress = null,
                        bootloaderAddress = "0x00000000"
                    ),
                    securityAccess = SecurityAccessInfo(
                        seedKeyAlgorithm = "BMW_CAS_SEED_KEY",
                        securityLevels = listOf(1, 3, 5),
                        keyGenerationMethod = "BMW_CAS_PROPRIETARY",
                        unlockSequence = listOf("REQUEST_SEED", "CAS_CALCULATION", "SEND_KEY")
                    )
                )
            ),
            specialFeatures = listOf("BMW ConnectedDrive", "Adaptive Suspension", "xDrive AWD", "BMW Efficient Dynamics"),
            knownIssues = listOf("VANOS solenoid failure", "High pressure fuel pump failure", "Timing chain stretch"),
            recommendedTools = listOf("BMW ISTA", "INPA", "SpaceTec Pro", "Autologic")
        ),
        
        // MERCEDES-BENZ MODELS
        VehicleEcuProfile(
            make = "Mercedes-Benz",
            model = "C-Class",
            yearRange = "2015-2024",
            engine = "2.0L Turbo, 3.0L V6 Turbo",
            transmission = "9G-TRONIC, 7G-TRONIC",
            market = "Global",
            ecuList = listOf(
                EcuCapability(
                    ecuId = "MB_W205_ME97_2015",
                    ecuName = "Motor Electronics ME9.7",
                    ecuType = EcuType.ENGINE,
                    supportedProtocols = listOf("UDS", "KWP2000", "CAN", "MERCEDES_DAS"),
                    supportedTests = listOf(
                        EcuTest("CAMTRONIC_TEST", "CAMTRONIC Variable Timing Test", TestType.ACTUATOR_TEST,
                            "Test CAMTRONIC variable valve timing", listOf("camshaft_bank", "timing_target"),
                            listOf("actuator_current", "timing_actual"), listOf("engine_warm", "oil_pressure_ok")),
                        EcuTest("PIEZO_INJECTOR_TEST", "Piezo Injector Test", TestType.ACTUATOR_TEST,
                            "Test piezo fuel injectors", listOf("cylinder_number", "injection_quantity"),
                            listOf("injector_current", "injection_duration"), listOf("engine_running", "fuel_pressure_ok")),
                        EcuTest("TURBO_ACTUATOR_TEST", "Turbo Actuator Test", TestType.ACTUATOR_TEST,
                            "Test turbo wastegate/VGT actuator", listOf("actuator_position", "boost_target"),
                            listOf("position_feedback", "boost_actual"), listOf("engine_running"))
                    ),
                    programmingSupport = ProgrammingCapability(
                        flashSupported = true,
                        eepromSupported = true,
                        calibrationSupported = true,
                        keyProgramming = false,
                        immobilizerSupport = true,
                        supportedFileFormats = listOf("MERCEDES_DFL", "HEX", "BIN", "CVN"),
                        programmingMethods = listOf("UDS_FLASH", "MERCEDES_DAS", "XENTRY_PROGRAMMING")
                    ),
                    diagnosticAddress = "0x7E0",
                    memoryLayout = MemoryLayout(
                        flashStartAddress = "0x80000000",
                        flashSize = "4MB",
                        eepromStartAddress = "0x80400000",
                        eepromSize = "64KB",
                        calibrationAddress = "0x80080000",
                        bootloaderAddress = "0x80000000"
                    ),
                    securityAccess = SecurityAccessInfo(
                        seedKeyAlgorithm = "MERCEDES_SEED_KEY_V3",
                        securityLevels = listOf(1, 3),
                        keyGenerationMethod = "MERCEDES_PROPRIETARY",
                        unlockSequence = listOf("REQUEST_SEED", "CALCULATE_KEY", "SEND_KEY")
                    )
                )
            ),
            specialFeatures = listOf("Mercedes-Benz User Experience (MBUX)", "AIRMATIC Suspension", "4MATIC AWD"),
            knownIssues = listOf("Balance shaft failure", "Timing chain stretch", "Turbo actuator failure"),
            recommendedTools = listOf("Mercedes Xentry", "DAS", "SpaceTec Pro", "Launch X431")
        ),
        
        // FORD MODELS
        VehicleEcuProfile(
            make = "Ford",
            model = "F-150",
            yearRange = "2015-2024",
            engine = "2.7L EcoBoost, 3.5L EcoBoost, 5.0L V8",
            transmission = "10R80, 6R80",
            market = "North America",
            ecuList = listOf(
                EcuCapability(
                    ecuId = "FORD_F150_PCM_2015",
                    ecuName = "Powertrain Control Module",
                    ecuType = EcuType.ENGINE,
                    supportedProtocols = listOf("UDS", "CAN", "FORD_SCP"),
                    supportedTests = listOf(
                        EcuTest("ECOBOOST_TURBO_TEST", "EcoBoost Turbo Test", TestType.ACTUATOR_TEST,
                            "Test EcoBoost turbo system", listOf("turbo_number", "boost_target"),
                            listOf("wastegate_position", "boost_pressure"), listOf("engine_running", "coolant_temp_normal")),
                        EcuTest("GDI_INJECTOR_TEST", "GDI Injector Test", TestType.ACTUATOR_TEST,
                            "Test gasoline direct injection", listOf("cylinder_number", "injection_pressure"),
                            listOf("injector_current", "fuel_delivery"), listOf("engine_running", "fuel_pressure_adequate")),
                        EcuTest("VCT_SOLENOID_TEST", "Variable Cam Timing Test", TestType.ACTUATOR_TEST,
                            "Test VCT solenoids", listOf("bank_number", "timing_advance"),
                            listOf("solenoid_current", "cam_position"), listOf("engine_warm", "oil_pressure_ok"))
                    ),
                    programmingSupport = ProgrammingCapability(
                        flashSupported = true,
                        eepromSupported = false,
                        calibrationSupported = true,
                        keyProgramming = true,
                        immobilizerSupport = true,
                        supportedFileFormats = listOf("FORD_CAL", "HEX", "BIN"),
                        programmingMethods = listOf("UDS_FLASH", "FORD_IDS", "J2534_PASSTHRU")
                    ),
                    diagnosticAddress = "0x7E0",
                    memoryLayout = MemoryLayout(
                        flashStartAddress = "0x00000000",
                        flashSize = "2MB",
                        eepromStartAddress = null,
                        eepromSize = null,
                        calibrationAddress = "0x00040000",
                        bootloaderAddress = "0x00000000"
                    ),
                    securityAccess = SecurityAccessInfo(
                        seedKeyAlgorithm = "FORD_SEED_KEY_V2",
                        securityLevels = listOf(1, 3),
                        keyGenerationMethod = "FORD_PROPRIETARY",
                        unlockSequence = listOf("REQUEST_SEED", "SEND_KEY")
                    )
                )
            ),
            specialFeatures = listOf("Ford Co-Pilot360", "Terrain Management System", "Pro Trailer Backup Assist"),
            knownIssues = listOf("Timing chain stretch", "Turbo failure", "Transmission shudder"),
            recommendedTools = listOf("Ford IDS", "FDRS", "SpaceTec Pro", "Snap-on VERUS")
        )
    )
}

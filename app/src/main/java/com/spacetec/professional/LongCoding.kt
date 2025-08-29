package com.spacetec.professional

import android.util.Log
import com.spacetec.obd.RealObdManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Long Coding and Adaptations System - Professional byte-level vehicle coding
 * Similar to VCDS Long Coding Helper with byte-level control and adaptations
 */
class LongCoding(
    private val obdManager: RealObdManager
) {
    companion object {
        private const val TAG = "LongCoding"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Coding execution state flows
    private val _codingResults = MutableSharedFlow<CodingResult>()
    val codingResults: SharedFlow<CodingResult> = _codingResults.asSharedFlow()
    
    private val _codingProgress = MutableStateFlow<CodingProgress?>(null)
    val codingProgress: StateFlow<CodingProgress?> = _codingProgress.asStateFlow()
    
    /**
     * Read current coding from ECU
     */
    suspend fun readCoding(
        vehicleMake: String,
        vehicleModel: String,
        vehicleYear: Int,
        ecuId: String,
        codingAddress: String
    ): CodingResult = withContext(Dispatchers.IO) {
        
        try {
            Log.i(TAG, "Reading coding from ECU: $ecuId, Address: $codingAddress")
            
            val transport = obdManager.getCurrentTransport()
                ?: return@withContext CodingResult.Error("No transport available")
            
            _codingProgress.value = CodingProgress("read_coding", "Reading", 0.5f, "Reading current coding data")
            
            val readCommand = "22$codingAddress"
            val response = transport.sendObdCommand(readCommand)
            
            when (response) {
                is com.spacetec.bluetooth.ObdResponse.Success -> {
                    val codingData = parseCodingData(response.data)
                    _codingProgress.value = CodingProgress("read_coding", "Completed", 1.0f, "Coding data read successfully")
                    
                    val result = CodingResult.Success(
                        operation = "read_coding",
                        data = mapOf(
                            "coding_address" to codingAddress,
                            "raw_data" to response.data,
                            "parsed_data" to codingData
                        ),
                        message = "Coding data read successfully"
                    )
                    _codingResults.emit(result)
                    result
                }
                is com.spacetec.bluetooth.ObdResponse.Error -> {
                    val errorResult = CodingResult.Error("Failed to read coding: ${response.message}")
                    _codingResults.emit(errorResult)
                    errorResult
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Read coding failed", e)
            val errorResult = CodingResult.Error("Read coding failed: ${e.message}")
            _codingResults.emit(errorResult)
            errorResult
        }
    }
    
    /**
     * Write coding to ECU
     */
    suspend fun writeCoding(
        vehicleMake: String,
        vehicleModel: String,
        vehicleYear: Int,
        ecuId: String,
        codingAddress: String,
        codingData: String
    ): CodingResult = withContext(Dispatchers.IO) {
        
        try {
            Log.i(TAG, "Writing coding to ECU: $ecuId, Address: $codingAddress, Data: $codingData")
            
            val transport = obdManager.getCurrentTransport()
                ?: return@withContext CodingResult.Error("No transport available")
            
            // Validate coding data
            if (!isValidCodingData(codingData)) {
                return@withContext CodingResult.Error("Invalid coding data format")
            }
            
            _codingProgress.value = CodingProgress("write_coding", "Writing", 0.3f, "Preparing to write coding")
            
            // Enter programming session
            val sessionResponse = transport.sendObdCommand("1002")
            if (sessionResponse is com.spacetec.bluetooth.ObdResponse.Error) {
                return@withContext CodingResult.Error("Failed to enter programming session")
            }
            
            _codingProgress.value = CodingProgress("write_coding", "Writing", 0.6f, "Writing coding data")
            
            // Write coding data
            val writeCommand = "2E$codingAddress$codingData"
            val response = transport.sendObdCommand(writeCommand)
            
            when (response) {
                is com.spacetec.bluetooth.ObdResponse.Success -> {
                    _codingProgress.value = CodingProgress("write_coding", "Verifying", 0.8f, "Verifying written data")
                    
                    // Verify written data
                    val verifyResponse = transport.sendObdCommand("22$codingAddress")
                    val verificationSuccess = when (verifyResponse) {
                        is com.spacetec.bluetooth.ObdResponse.Success -> {
                            verifyResponse.data.contains(codingData)
                        }
                        is com.spacetec.bluetooth.ObdResponse.Error -> false
                    }
                    
                    // Exit programming session
                    transport.sendObdCommand("1001")
                    
                    _codingProgress.value = CodingProgress("write_coding", "Completed", 1.0f, "Coding written successfully")
                    
                    val result = CodingResult.Success(
                        operation = "write_coding",
                        data = mapOf(
                            "coding_address" to codingAddress,
                            "written_data" to codingData,
                            "verification_success" to verificationSuccess
                        ),
                        message = "Coding written successfully"
                    )
                    _codingResults.emit(result)
                    result
                }
                is com.spacetec.bluetooth.ObdResponse.Error -> {
                    transport.sendObdCommand("1001") // Exit session
                    val errorResult = CodingResult.Error("Failed to write coding: ${response.message}")
                    _codingResults.emit(errorResult)
                    errorResult
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Write coding failed", e)
            val errorResult = CodingResult.Error("Write coding failed: ${e.message}")
            _codingResults.emit(errorResult)
            errorResult
        }
    }
    
    /**
     * Perform adaptation
     */
    suspend fun performAdaptation(
        vehicleMake: String,
        vehicleModel: String,
        vehicleYear: Int,
        ecuId: String,
        adaptationChannel: String,
        adaptationValue: String
    ): CodingResult = withContext(Dispatchers.IO) {
        
        try {
            Log.i(TAG, "Performing adaptation: ECU=$ecuId, Channel=$adaptationChannel, Value=$adaptationValue")
            
            val transport = obdManager.getCurrentTransport()
                ?: return@withContext CodingResult.Error("No transport available")
            
            _codingProgress.value = CodingProgress("adaptation", "Starting", 0.2f, "Initializing adaptation")
            
            // Enter adaptation mode
            val sessionResponse = transport.sendObdCommand("1003")
            if (sessionResponse is com.spacetec.bluetooth.ObdResponse.Error) {
                return@withContext CodingResult.Error("Failed to enter adaptation mode")
            }
            
            _codingProgress.value = CodingProgress("adaptation", "Adapting", 0.6f, "Performing adaptation")
            
            // Perform adaptation
            val adaptCommand = "2C$adaptationChannel$adaptationValue"
            val response = transport.sendObdCommand(adaptCommand)
            
            when (response) {
                is com.spacetec.bluetooth.ObdResponse.Success -> {
                    _codingProgress.value = CodingProgress("adaptation", "Verifying", 0.8f, "Verifying adaptation")
                    
                    // Read back adaptation value
                    val verifyResponse = transport.sendObdCommand("21$adaptationChannel")
                    val verificationData = when (verifyResponse) {
                        is com.spacetec.bluetooth.ObdResponse.Success -> verifyResponse.data
                        is com.spacetec.bluetooth.ObdResponse.Error -> "Verification failed"
                    }
                    
                    // Exit adaptation mode
                    transport.sendObdCommand("1001")
                    
                    _codingProgress.value = CodingProgress("adaptation", "Completed", 1.0f, "Adaptation completed")
                    
                    val result = CodingResult.Success(
                        operation = "adaptation",
                        data = mapOf(
                            "adaptation_channel" to adaptationChannel,
                            "adaptation_value" to adaptationValue,
                            "verification_data" to verificationData
                        ),
                        message = "Adaptation completed successfully"
                    )
                    _codingResults.emit(result)
                    result
                }
                is com.spacetec.bluetooth.ObdResponse.Error -> {
                    transport.sendObdCommand("1001") // Exit session
                    val errorResult = CodingResult.Error("Adaptation failed: ${response.message}")
                    _codingResults.emit(errorResult)
                    errorResult
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Adaptation failed", e)
            val errorResult = CodingResult.Error("Adaptation failed: ${e.message}")
            _codingResults.emit(errorResult)
            errorResult
        }
    }
    
    /**
     * Get available coding maps for vehicle
     */
    fun getAvailableCodingMaps(make: String, model: String, year: Int): List<CodingMap> {
        return codingMaps.filter { map ->
            map.supportedVehicles.any { vehicle ->
                vehicle.make.equals(make, ignoreCase = true) &&
                vehicle.model.equals(model, ignoreCase = true) &&
                isYearInRange(year, vehicle.yearRange)
            }
        }
    }
    
    /**
     * Get coding map by ECU
     */
    fun getCodingMapByEcu(ecuId: String, make: String, model: String, year: Int): CodingMap? {
        return getAvailableCodingMaps(make, model, year).find { it.ecuId == ecuId }
    }
    
    /**
     * Decode coding byte
     */
    fun decodeCodingByte(codingMap: CodingMap, bytePosition: Int, byteValue: String): Map<String, Any> {
        val byte = codingMap.bytes.find { it.position == bytePosition }
            ?: return mapOf("error" to "Byte position not found")
        
        val intValue = try {
            byteValue.toInt(16)
        } catch (e: Exception) {
            return mapOf("error" to "Invalid byte value")
        }
        
        val decodedBits = mutableMapOf<String, Any>()
        
        byte.bits.forEach { bit ->
            val bitValue = (intValue shr bit.position) and 1
            decodedBits[bit.name] = when (bit.type) {
                BitType.BOOLEAN -> bitValue == 1
                BitType.VALUE -> bitValue
                BitType.ENUM -> bit.enumValues?.get(bitValue) ?: "Unknown"
            }
        }
        
        return decodedBits
    }
    
    /**
     * Encode coding byte
     */
    fun encodeCodingByte(codingMap: CodingMap, bytePosition: Int, bitValues: Map<String, Any>): String? {
        val byte = codingMap.bytes.find { it.position == bytePosition } ?: return null
        
        var byteValue = 0
        
        byte.bits.forEach { bit ->
            val value = bitValues[bit.name]
            val bitVal = when (bit.type) {
                BitType.BOOLEAN -> if (value as? Boolean == true) 1 else 0
                BitType.VALUE -> (value as? Number)?.toInt() ?: 0
                BitType.ENUM -> bit.enumValues?.entries?.find { it.value == value }?.key ?: 0
            }
            
            if (bitVal != 0) {
                byteValue = byteValue or (1 shl bit.position)
            }
        }
        
        return String.format("%02X", byteValue)
    }
    
    private fun parseCodingData(rawData: String): Map<String, String> {
        val parsed = mutableMapOf<String, String>()
        
        // Remove response header and split into bytes
        val cleanData = rawData.removePrefix("62").removePrefix("F1").removePrefix("90")
        val bytes = cleanData.chunked(2)
        
        bytes.forEachIndexed { index, byte ->
            parsed["byte_$index"] = byte
        }
        
        return parsed
    }
    
    private fun isValidCodingData(data: String): Boolean {
        return data.matches(Regex("^[0-9A-Fa-f]+$")) && data.length % 2 == 0
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
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
    }
}

// Data classes and enums
enum class BitType {
    BOOLEAN, VALUE, ENUM
}

data class CodingMap(
    val id: String,
    val name: String,
    val ecuId: String,
    val description: String,
    val supportedVehicles: List<VehicleCodingSupport>,
    val codingAddress: String,
    val bytes: List<CodingByte>
)

data class VehicleCodingSupport(
    val make: String,
    val model: String,
    val yearRange: String,
    val engine: String? = null
)

data class CodingByte(
    val position: Int,
    val name: String,
    val description: String,
    val bits: List<CodingBit>
)

data class CodingBit(
    val position: Int,
    val name: String,
    val description: String,
    val type: BitType,
    val enumValues: Map<Int, String>? = null
)

sealed class CodingResult {
    data class Success(
        val operation: String,
        val data: Map<String, Any>,
        val message: String
    ) : CodingResult()
    
    data class Error(
        val message: String
    ) : CodingResult()
}

data class CodingProgress(
    val operation: String,
    val status: String,
    val progress: Float,
    val description: String
)

// Pre-defined Coding Maps database
private val codingMaps = listOf(
    // VAG Group Central Electronics (BCM)
    CodingMap(
        id = "vag_bcm_09",
        name = "Central Electronics",
        ecuId = "09",
        description = "Body Control Module coding for VAG vehicles",
        supportedVehicles = listOf(
            VehicleCodingSupport("Volkswagen", "Golf", "2015-2024"),
            VehicleCodingSupport("Volkswagen", "Passat", "2015-2024"),
            VehicleCodingSupport("Audi", "A3", "2015-2024"),
            VehicleCodingSupport("Audi", "A4", "2015-2024")
        ),
        codingAddress = "F190",
        bytes = listOf(
            CodingByte(
                position = 0,
                name = "Byte 0 - Basic Functions",
                description = "Basic vehicle functions",
                bits = listOf(
                    CodingBit(0, "daytime_running_lights", "Daytime Running Lights", BitType.BOOLEAN),
                    CodingBit(1, "coming_home", "Coming Home Function", BitType.BOOLEAN),
                    CodingBit(2, "leaving_home", "Leaving Home Function", BitType.BOOLEAN),
                    CodingBit(3, "auto_headlights", "Automatic Headlights", BitType.BOOLEAN),
                    CodingBit(4, "rain_sensor", "Rain Sensor", BitType.BOOLEAN),
                    CodingBit(5, "comfort_turn_signals", "Comfort Turn Signals", BitType.BOOLEAN),
                    CodingBit(6, "needle_sweep", "Needle Sweep", BitType.BOOLEAN),
                    CodingBit(7, "reserved", "Reserved", BitType.VALUE)
                )
            ),
            CodingByte(
                position = 1,
                name = "Byte 1 - Lighting",
                description = "Lighting configuration",
                bits = listOf(
                    CodingBit(0, "drl_as_turn_signal", "DRL as Turn Signal", BitType.BOOLEAN),
                    CodingBit(1, "led_drl", "LED DRL", BitType.BOOLEAN),
                    CodingBit(2, "xenon_headlights", "Xenon Headlights", BitType.BOOLEAN),
                    CodingBit(3, "adaptive_lighting", "Adaptive Front Lighting", BitType.BOOLEAN),
                    CodingBit(4, "cornering_lights", "Cornering Lights", BitType.BOOLEAN),
                    CodingBit(5, "high_beam_assist", "High Beam Assist", BitType.BOOLEAN),
                    CodingBit(6, "dynamic_turn_signals", "Dynamic Turn Signals", BitType.BOOLEAN),
                    CodingBit(7, "matrix_led", "Matrix LED", BitType.BOOLEAN)
                )
            ),
            CodingByte(
                position = 2,
                name = "Byte 2 - Comfort",
                description = "Comfort and convenience features",
                bits = listOf(
                    CodingBit(0, "auto_lock", "Automatic Door Lock", BitType.BOOLEAN),
                    CodingBit(1, "auto_unlock", "Automatic Door Unlock", BitType.BOOLEAN),
                    CodingBit(2, "mirror_fold", "Mirror Folding", BitType.BOOLEAN),
                    CodingBit(3, "mirror_tilt", "Mirror Tilt in Reverse", BitType.BOOLEAN),
                    CodingBit(4, "window_comfort", "Window Comfort Function", BitType.BOOLEAN),
                    CodingBit(5, "sunroof_comfort", "Sunroof Comfort Function", BitType.BOOLEAN),
                    CodingBit(6, "memory_seats", "Memory Seats", BitType.BOOLEAN),
                    CodingBit(7, "easy_entry", "Easy Entry Function", BitType.BOOLEAN)
                )
            )
        )
    ),
    
    // BMW Light Module (LCM)
    CodingMap(
        id = "bmw_lcm_a0",
        name = "Light Control Module",
        ecuId = "A0",
        description = "BMW Light Control Module coding",
        supportedVehicles = listOf(
            VehicleCodingSupport("BMW", "3 Series", "2015-2024"),
            VehicleCodingSupport("BMW", "5 Series", "2015-2024"),
            VehicleCodingSupport("BMW", "X3", "2015-2024")
        ),
        codingAddress = "F1A0",
        bytes = listOf(
            CodingByte(
                position = 0,
                name = "Byte 0 - Angel Eyes",
                description = "Angel Eyes configuration",
                bits = listOf(
                    CodingBit(0, "angel_eyes_enable", "Angel Eyes Enable", BitType.BOOLEAN),
                    CodingBit(1, "angel_eyes_dimming", "Angel Eyes Dimming", BitType.BOOLEAN),
                    CodingBit(2, "angel_eyes_color", "Angel Eyes Color", BitType.ENUM,
                        mapOf(0 to "White", 1 to "Blue")),
                    CodingBit(3, "corona_rings", "Corona Rings", BitType.BOOLEAN),
                    CodingBit(4, "welcome_light", "Welcome Light", BitType.BOOLEAN),
                    CodingBit(5, "goodbye_light", "Goodbye Light", BitType.BOOLEAN),
                    CodingBit(6, "brightness_control", "Brightness Control", BitType.BOOLEAN),
                    CodingBit(7, "reserved", "Reserved", BitType.VALUE)
                )
            ),
            CodingByte(
                position = 1,
                name = "Byte 1 - Adaptive Light",
                description = "Adaptive lighting functions",
                bits = listOf(
                    CodingBit(0, "adaptive_headlights", "Adaptive Headlights", BitType.BOOLEAN),
                    CodingBit(1, "cornering_light", "Cornering Light", BitType.BOOLEAN),
                    CodingBit(2, "high_beam_assist", "High Beam Assistant", BitType.BOOLEAN),
                    CodingBit(3, "auto_high_beam", "Automatic High Beam", BitType.BOOLEAN),
                    CodingBit(4, "curve_light", "Curve Light", BitType.BOOLEAN),
                    CodingBit(5, "city_light", "City Light", BitType.BOOLEAN),
                    CodingBit(6, "highway_light", "Highway Light", BitType.BOOLEAN),
                    CodingBit(7, "bad_weather_light", "Bad Weather Light", BitType.BOOLEAN)
                )
            )
        )
    ),
    
    // Mercedes SAM (Signal Acquisition Module)
    CodingMap(
        id = "mb_sam_a1",
        name = "Signal Acquisition Module",
        ecuId = "A1",
        description = "Mercedes-Benz SAM coding",
        supportedVehicles = listOf(
            VehicleCodingSupport("Mercedes-Benz", "C-Class", "2015-2024"),
            VehicleCodingSupport("Mercedes-Benz", "E-Class", "2015-2024"),
            VehicleCodingSupport("Mercedes-Benz", "S-Class", "2015-2024")
        ),
        codingAddress = "F1B0",
        bytes = listOf(
            CodingByte(
                position = 0,
                name = "Byte 0 - Lighting Control",
                description = "Basic lighting control",
                bits = listOf(
                    CodingBit(0, "drl_enable", "DRL Enable", BitType.BOOLEAN),
                    CodingBit(1, "auto_headlights", "Automatic Headlights", BitType.BOOLEAN),
                    CodingBit(2, "coming_home", "Coming Home", BitType.BOOLEAN),
                    CodingBit(3, "leaving_home", "Leaving Home", BitType.BOOLEAN),
                    CodingBit(4, "intelligent_light", "Intelligent Light System", BitType.BOOLEAN),
                    CodingBit(5, "adaptive_highbeam", "Adaptive Highbeam Assist", BitType.BOOLEAN),
                    CodingBit(6, "led_headlights", "LED Headlights", BitType.BOOLEAN),
                    CodingBit(7, "multibeam_led", "Multibeam LED", BitType.BOOLEAN)
                )
            ),
            CodingByte(
                position = 1,
                name = "Byte 1 - Comfort Functions",
                description = "Comfort and convenience",
                bits = listOf(
                    CodingBit(0, "keyless_go", "Keyless Go", BitType.BOOLEAN),
                    CodingBit(1, "auto_lock", "Automatic Locking", BitType.BOOLEAN),
                    CodingBit(2, "crash_unlock", "Crash Unlock", BitType.BOOLEAN),
                    CodingBit(3, "panic_alarm", "Panic Alarm", BitType.BOOLEAN),
                    CodingBit(4, "remote_start", "Remote Engine Start", BitType.BOOLEAN),
                    CodingBit(5, "pre_safe", "Pre-Safe System", BitType.BOOLEAN),
                    CodingBit(6, "attention_assist", "Attention Assist", BitType.BOOLEAN),
                    CodingBit(7, "parktronic", "Parktronic", BitType.BOOLEAN)
                )
            )
        )
    )
)

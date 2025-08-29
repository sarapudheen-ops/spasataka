package com.spacetec.obd

/**
 * OBD-II Protocol Implementation for Space-Tec
 * Handles PID commands and response parsing for vehicle diagnostics
 */
object ObdProtocol {
    
    // Standard OBD-II PIDs (Parameter IDs)
    object PID {
        const val ENGINE_RPM = "010C"           // Engine RPM
        const val VEHICLE_SPEED = "010D"        // Vehicle speed
        const val COOLANT_TEMP = "0105"         // Engine coolant temperature
        const val FUEL_LEVEL = "012F"           // Fuel tank level input
        const val ENGINE_LOAD = "0104"          // Calculated engine load
        const val THROTTLE_POSITION = "0111"    // Throttle position
        const val INTAKE_TEMP = "010F"          // Intake air temperature
        const val MAF_RATE = "0110"             // Mass air flow rate
        const val FUEL_PRESSURE = "010A"        // Fuel system pressure
        const val RUNTIME = "011F"              // Runtime since engine start
        
        // VIN and Vehicle Information PIDs
        const val VIN_REQUEST = "0902"          // Vehicle Identification Number
        const val CALIBRATION_ID = "0904"       // Calibration ID
        const val CVN = "0906"                  // Calibration Verification Number
        const val ECU_NAME = "090A"             // ECU Name
        const val PERFORMANCE_TRACKING = "090B" // Performance tracking
    }
    
    /**
     * Initialize OBD-II connection with enhanced ELM327 commands
     */
    fun getInitCommands(): List<String> = listOf(
        "ATZ",      // Reset adapter
        "ATE0",     // Echo off - critical for clean responses
        "ATL0",     // Line feeds off - prevents extra characters
        "ATH0",     // Headers off - cleaner responses
        "ATSP0"     // Set protocol to auto detection
    )
    
    /**
     * Parse OBD-II response for Engine RPM
     * Format: 41 0C XX XX (XX XX = RPM data)
     */
    fun parseRpm(response: String): Int {
        return try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.startsWith("410C") && cleaned.length >= 8) {
                val a = cleaned.substring(4, 6).toInt(16)
                val b = cleaned.substring(6, 8).toInt(16)
                (a * 256 + b) / 4
            } else 0
        } catch (e: Exception) { 0 }
    }
    
    /**
     * Parse OBD-II response for Vehicle Speed
     * Format: 41 0D XX (XX = speed in km/h)
     */
    fun parseSpeed(response: String): Int {
        return try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.startsWith("410D") && cleaned.length >= 6) {
                cleaned.substring(4, 6).toInt(16)
            } else 0
        } catch (e: Exception) { 0 }
    }
    
    /**
     * Parse OBD-II response for Coolant Temperature
     * Format: 41 05 XX (XX = temp data, -40°C offset)
     */
    fun parseCoolantTemp(response: String): Int {
        return try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.startsWith("4105") && cleaned.length >= 6) {
                cleaned.substring(4, 6).toInt(16) - 40
            } else 0
        } catch (e: Exception) { 0 }
    }
    
    /**
     * Parse OBD-II response for Fuel Level
     * Format: 41 2F XX (XX = fuel level percentage)
     */
    fun parseFuelLevel(response: String): Int {
        return try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.startsWith("412F") && cleaned.length >= 6) {
                (cleaned.substring(4, 6).toInt(16) * 100) / 255
            } else 0
        } catch (e: Exception) { 0 }
    }
    
    /**
     * Parse OBD-II response for Engine Load
     * Format: 41 04 XX (XX = load percentage)
     */
    fun parseEngineLoad(response: String): Int {
        return try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.startsWith("4104") && cleaned.length >= 6) {
                (cleaned.substring(4, 6).toInt(16) * 100) / 255
            } else 0
        } catch (e: Exception) { 0 }
    }
    
    /**
     * Parse OBD-II response for Throttle Position
     * Format: 41 11 XX (XX = throttle percentage)
     */
    fun parseThrottlePosition(response: String): Int {
        return try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.startsWith("4111") && cleaned.length >= 6) {
                (cleaned.substring(4, 6).toInt(16) * 100) / 255
            } else 0
        } catch (e: Exception) { 0 }
    }
    
    /**
     * Check if response indicates an error
     */
    fun isErrorResponse(response: String): Boolean {
        val cleaned = response.uppercase().trim()
        return cleaned.contains("NO DATA") || 
               cleaned.contains("ERROR") || 
               cleaned.contains("?") ||
               cleaned.contains("UNABLE TO CONNECT") ||
               cleaned.contains("BUS INIT") ||
               cleaned.contains("CAN ERROR") ||
               cleaned.startsWith("7F") ||
               cleaned.isEmpty()
    }
    
    /**
     * Enhanced response reading with timeout and availability check
     */
    fun readResponseWithTimeout(inputStream: java.io.InputStream?, timeoutMs: Long = 5000): String? {
        if (inputStream == null) return null
        
        val startTime = System.currentTimeMillis()
        val buffer = ByteArray(1024)
        val response = StringBuilder()
        
        try {
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (inputStream.available() > 0) {
                    val bytes = inputStream.read(buffer)
                    if (bytes > 0) {
                        val chunk = String(buffer, 0, bytes)
                        response.append(chunk)
                        
                        // Check if we have a complete response (ends with > or contains error)
                        val responseStr = response.toString()
                        if (responseStr.contains(">") || isErrorResponse(responseStr)) {
                            return responseStr.replace(">", "").trim()
                        }
                    }
                }
                Thread.sleep(50)
            }
        } catch (e: Exception) {
            return null
        }
        
        return if (response.isNotEmpty()) response.toString().trim() else null
    }
    
    /**
     * Validate OBD-II response format
     */
    fun isValidResponse(response: String, expectedPid: String): Boolean {
        val cleaned = response.replace(" ", "").uppercase()
        val expectedResponse = "41" + expectedPid.substring(2)
        return cleaned.startsWith(expectedResponse) && !isErrorResponse(response)
    }
    
    /**
     * Parse VIN from OBD-II response
     * VIN is returned in multiple frames, need to concatenate
     */
    fun parseVin(responses: List<String>): String? {
        return try {
            val vinBuilder = StringBuilder()
            
            for (response in responses) {
                val cleaned = response.replace(" ", "").uppercase()
                if (cleaned.startsWith("4902")) {
                    // Extract VIN data from response
                    // Format: 49 02 XX [VIN bytes in hex]
                    val vinHex = cleaned.substring(6) // Skip "4902XX"
                    
                    // Convert hex pairs to ASCII characters
                    for (i in vinHex.indices step 2) {
                        if (i + 1 < vinHex.length) {
                            val hexPair = vinHex.substring(i, i + 2)
                            val charCode = hexPair.toInt(16)
                            if (charCode in 32..126) { // Printable ASCII range
                                vinBuilder.append(charCode.toChar())
                            }
                        }
                    }
                }
            }
            
            val vin = vinBuilder.toString().trim()
            if (vin.length == 17) vin else null
            
        } catch (e: Exception) { 
            null 
        }
    }
    
    /**
     * Parse ECU name from response
     */
    fun parseEcuName(response: String): String? {
        return try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.startsWith("490A")) {
                val nameHex = cleaned.substring(6)
                val nameBuilder = StringBuilder()
                
                for (i in nameHex.indices step 2) {
                    if (i + 1 < nameHex.length) {
                        val hexPair = nameHex.substring(i, i + 2)
                        val charCode = hexPair.toInt(16)
                        if (charCode in 32..126) {
                            nameBuilder.append(charCode.toChar())
                        }
                    }
                }
                
                nameBuilder.toString().trim().takeIf { it.isNotEmpty() }
            } else null
        } catch (e: Exception) { 
            null 
        }
    }
    
    /**
     * Parse DTC codes from OBD response
     */
    fun parseDtcCodes(response: String): List<String> {
        return try {
            val dtcList = mutableListOf<String>()
            val cleaned = response.replace(" ", "").uppercase()
            
            // DTCs are typically returned as 2-byte codes after mode response
            if (cleaned.startsWith("43")) { // Mode 3 response for stored DTCs
                var i = 2 // Skip mode byte
                while (i < cleaned.length - 3) {
                    try {
                        val firstByte = cleaned.substring(i, i + 2).toInt(16)
                        val secondByte = cleaned.substring(i + 2, i + 4).toInt(16)
                        
                        if (firstByte != 0 || secondByte != 0) {
                            val dtc = formatDtcCode(firstByte, secondByte)
                            dtcList.add(dtc)
                        }
                    } catch (e: Exception) {
                        // Skip invalid bytes
                    }
                    i += 4
                }
            }
            
            dtcList
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Format DTC code from raw bytes
     */
    private fun formatDtcCode(firstByte: Int, secondByte: Int): String {
        val dtcPrefix = when ((firstByte shr 6) and 0x03) {
            0 -> "P"  // Powertrain
            1 -> "C"  // Chassis
            2 -> "B"  // Body
            3 -> "U"  // Network
            else -> "P"
        }
        
        val dtcNumber = String.format("%04X", ((firstByte and 0x3F) shl 8) or secondByte)
        return "$dtcPrefix$dtcNumber"
    }
    
    /**
     * Get VIN request command sequence
     */
    fun getVinRequestCommands(): List<String> = listOf(
        PID.VIN_REQUEST,
        "0902", // Request all VIN data
    )
    
    /**
     * Get vehicle information PIDs
     */
    fun getVehicleInfoPids(): List<String> = listOf(
        PID.VIN_REQUEST,
        PID.CALIBRATION_ID,
        PID.ECU_NAME
    )
    
    /**
     * Get all supported PIDs for continuous monitoring
     */
    fun getMonitoringPids(): List<String> = listOf(
        PID.ENGINE_RPM,
        PID.VEHICLE_SPEED,
        PID.COOLANT_TEMP,
        PID.FUEL_LEVEL,
        PID.ENGINE_LOAD,
        PID.THROTTLE_POSITION
    )
}

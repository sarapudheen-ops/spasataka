package com.spacetec.diagnostic.protocol

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.mutableListOf

/**
 * Professional ELM327 Protocol Implementation
 * Handles all AT commands, protocol selection, and advanced features
 * Compatible with ELM327 v1.0 to v2.3
 */
class Elm327Protocol(
    private val transport: suspend (String) -> String,
    private val logger: (String) -> Unit = {}
) {
    private val initialized = AtomicBoolean(false)
    private var currentProtocol: Protocol = Protocol.AUTO
    private var headers = true
    private var echo = false
    private var adaptiveTiming = true
    private var timeout = 200 // x4ms units
    
    // Performance optimization
    private val responseCache = mutableMapOf<String, Pair<String, Long>>()
    private val cacheTimeout = 60000L // 60 seconds
    
    enum class Protocol(val value: String, val description: String) {
        AUTO("0", "Automatic"),
        J1850_PWM("1", "SAE J1850 PWM (41.6 kbaud)"),
        J1850_VPW("2", "SAE J1850 VPW (10.4 kbaud)"),
        ISO9141_2("3", "ISO 9141-2 (5 baud init)"),
        ISO14230_4_SLOW("4", "ISO 14230-4 KWP (5 baud init)"),
        ISO14230_4_FAST("5", "ISO 14230-4 KWP (fast init)"),
        ISO15765_4_11BIT_500K("6", "ISO 15765-4 CAN (11 bit ID, 500 kbaud)"),
        ISO15765_4_29BIT_500K("7", "ISO 15765-4 CAN (29 bit ID, 500 kbaud)"),
        ISO15765_4_11BIT_250K("8", "ISO 15765-4 CAN (11 bit ID, 250 kbaud)"),
        ISO15765_4_29BIT_250K("9", "ISO 15765-4 CAN (29 bit ID, 250 kbaud)"),
        J1939_29BIT_250K("A", "SAE J1939 CAN (29 bit ID, 250 kbaud)"),
        USER1_CAN("B", "User1 CAN (11 bit ID, 125 kbaud)"),
        USER2_CAN("C", "User2 CAN (11 bit ID, 50 kbaud)")
    }
    
    /**
     * Initialize ELM327 adapter with optimal settings
     */
    suspend fun initialize(): Result<ElmInfo> = coroutineScope {
        try {
            // Reset
            sendCommand("ATZ")
            delay(1000) // ELM327 needs time after reset
            
            // Get version info
            val version = sendCommand("ATI")
            val elmInfo = parseElmVersion(version)
            
            // Configure for maximum performance
            val initCommands = listOf(
                "ATE0",    // Echo off
                "ATH1",    // Headers on
                "ATL0",    // Linefeeds off
                "ATS0",    // Spaces off
                "ATAT2",   // Adaptive timing aggressive
                "ATSP0",   // Auto protocol
                "ATDP",    // Display protocol
                "ATST${timeout.toString(16).uppercase()}", // Set timeout
                "ATCAF0",  // CAN auto-formatting off (we handle it)
                "ATFC"     // Flow control
            )
            
            initCommands.forEach { cmd ->
                val response = sendCommand(cmd)
                if (!response.contains("OK") && !response.contains("?")) {
                    logger("Warning: $cmd returned: $response")
                }
            }
            
            // Set protocol if specified
            val protocolResponse = sendCommand("ATDPN")
            currentProtocol = parseProtocol(protocolResponse)
            
            initialized.set(true)
            
            Result.success(elmInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send OBD command with intelligent parsing
     */
    suspend fun sendObdCommand(
        command: String,
        expectedLines: Int = 1,
        timeout: Long = 1000L
    ): Result<ObdResponse> = coroutineScope {
        if (!initialized.get()) {
            return@coroutineScope Result.failure(
                IllegalStateException("ELM327 not initialized")
            )
        }
        
        try {
            // Check cache first
            val cached = responseCache[command]
            if (cached != null && System.currentTimeMillis() - cached.second < cacheTimeout) {
                return@coroutineScope Result.success(parseObdResponse(command, cached.first))
            }
            
            // Send command
            val response = withTimeout(timeout) {
                sendCommand(command)
            }
            
            // Cache response
            responseCache[command] = Pair(response, System.currentTimeMillis())
            
            // Parse response
            Result.success(parseObdResponse(command, response))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Advanced protocol detection with fallback
     */
    suspend fun detectProtocol(): Result<Protocol> = coroutineScope {
        try {
            // Try auto-detect first
            sendCommand("ATSP0")
            delay(100)
            
            // Send test command
            val response = sendCommand("0100")
            
            if (response.contains("NO DATA") || response.contains("UNABLE")) {
                // Manual protocol search
                for (protocol in Protocol.values()) {
                    if (protocol == Protocol.AUTO) continue
                    
                    sendCommand("ATSP${protocol.value}")
                    delay(100)
                    
                    val testResponse = sendCommand("0100")
                    if (!testResponse.contains("NO DATA") && 
                        !testResponse.contains("UNABLE") &&
                        testResponse.contains("41")) {
                        currentProtocol = protocol
                        return@coroutineScope Result.success(protocol)
                    }
                }
                
                Result.failure(Exception("No compatible protocol found"))
            } else {
                // Get detected protocol
                val protocolNum = sendCommand("ATDPN")
                currentProtocol = parseProtocol(protocolNum)
                Result.success(currentProtocol)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Read all supported PIDs efficiently
     */
    suspend fun readSupportedPids(): Result<Map<Int, List<Int>>> = coroutineScope {
        val supportedPids = mutableMapOf<Int, MutableList<Int>>()
        
        try {
            // Check each mode
            for (mode in listOf(0x01, 0x02, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A)) {
                val modePids = mutableListOf<Int>()
                
                // Start with PID 00
                var pidToCheck = 0x00
                
                while (pidToCheck <= 0xE0) {
                    val command = "%02X%02X".format(mode, pidToCheck)
                    val response = sendObdCommand(command).getOrNull()
                    
                    if (response != null && response.success) {
                        val data = response.data
                        
                        // Parse supported PIDs bitmap
                        for (byteIdx in data.indices) {
                            val byte = data[byteIdx]
                            for (bit in 0..7) {
                                if ((byte.toInt() and (1 shl (7 - bit))) != 0) {
                                    val supportedPid = pidToCheck + 1 + (byteIdx * 8) + bit
                                    modePids.add(supportedPid)
                                }
                            }
                        }
                        
                        // Check if next block is supported
                        pidToCheck += 0x20
                        if (!modePids.contains(pidToCheck)) break
                    } else {
                        break
                    }
                }
                
                if (modePids.isNotEmpty()) {
                    supportedPids[mode] = modePids
                }
            }
            
            Result.success(supportedPids)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Read DTCs with enhanced parsing
     */
    suspend fun readDtcs(): Result<List<Dtc>> = coroutineScope {
        try {
            val dtcs = mutableListOf<Dtc>()
            
            // Mode 03 - Confirmed DTCs
            val mode03Response = sendObdCommand("03", expectedLines = 10).getOrNull()
            if (mode03Response?.success == true) {
                dtcs.addAll(parseDtcs(mode03Response.data, DtcStatus.CONFIRMED))
            }
            
            // Mode 07 - Pending DTCs
            val mode07Response = sendObdCommand("07", expectedLines = 10).getOrNull()
            if (mode07Response?.success == true) {
                dtcs.addAll(parseDtcs(mode07Response.data, DtcStatus.PENDING))
            }
            
            // Mode 0A - Permanent DTCs
            val mode0AResponse = sendObdCommand("0A", expectedLines = 10).getOrNull()
            if (mode0AResponse?.success == true) {
                dtcs.addAll(parseDtcs(mode0AResponse.data, DtcStatus.PERMANENT))
            }
            
            Result.success(dtcs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Clear DTCs and reset monitors
     */
    suspend fun clearDtcs(): Result<Boolean> = coroutineScope {
        try {
            val response = sendObdCommand("04").getOrThrow()
            Result.success(response.success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Read VIN with multi-line support
     */
    suspend fun readVin(): Result<String> = coroutineScope {
        try {
            val response = sendObdCommand("0902", expectedLines = 5).getOrThrow()
            
            if (!response.success || response.data.isEmpty()) {
                return@coroutineScope Result.failure(Exception("VIN not available"))
            }
            
            // Parse VIN from multi-line response
            val vinBytes = mutableListOf<Byte>()
            var lineCount = 0
            
            // Skip first byte (line count)
            var dataIndex = 1
            
            while (dataIndex < response.data.size && vinBytes.size < 17) {
                val byte = response.data[dataIndex]
                if (byte in 0x20..0x7E) { // Valid ASCII
                    vinBytes.add(byte)
                }
                dataIndex++
            }
            
            val vin = String(vinBytes.toByteArray())
            
            if (vin.length == 17 && vin.matches(Regex("[A-HJ-NPR-Z0-9]{17}"))) {
                Result.success(vin)
            } else {
                Result.failure(Exception("Invalid VIN format: $vin"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send raw AT command
     */
    private suspend fun sendCommand(command: String): String {
        logger("TX: $command")
        val response = transport(command).trim()
        logger("RX: $response")
        return response
    }
    
    /**
     * Parse OBD response intelligently
     */
    private fun parseObdResponse(command: String, response: String): ObdResponse {
        val lines = response.split("\r", "\n").filter { it.isNotBlank() }
        
        // Check for errors
        if (lines.any { it.contains("NO DATA") }) {
            return ObdResponse(false, command, byteArrayOf(), "No data available")
        }
        
        if (lines.any { it.contains("UNABLE TO CONNECT") }) {
            return ObdResponse(false, command, byteArrayOf(), "Unable to connect to ECU")
        }
        
        if (lines.any { it.contains("CAN ERROR") || it.contains("BUS ERROR") }) {
            return ObdResponse(false, command, byteArrayOf(), "Communication error")
        }
        
        // Parse data
        val dataBytes = mutableListOf<Byte>()
        val mode = command.substring(0, 2).toInt(16)
        val pid = if (command.length >= 4) command.substring(2, 4).toInt(16) else 0
        val expectedResponseMode = mode + 0x40
        
        for (line in lines) {
            val cleanLine = line.replace(" ", "").uppercase()
            
            // Skip non-data lines
            if (cleanLine.length < 4 || !cleanLine.all { it in '0'..'9' || it in 'A'..'F' }) {
                continue
            }
            
            // Parse based on headers setting
            if (headers) {
                // With headers: 7E8 06 41 00 BE 7F B8 13
                val parts = line.split(" ")
                if (parts.size >= 4) {
                    // Skip header and length byte
                    val dataStart = if (parts[0].length == 3) 2 else 1
                    for (i in dataStart until parts.size) {
                        val byte = parts[i].toIntOrNull(16)?.toByte()
                        if (byte != null) dataBytes.add(byte)
                    }
                }
            } else {
                // Without headers: 41 00 BE 7F B8 13
                val bytes = cleanLine.chunked(2)
                bytes.forEach { 
                    val byte = it.toIntOrNull(16)?.toByte()
                    if (byte != null) dataBytes.add(byte)
                }
            }
        }
        
        // Validate response
        if (dataBytes.size >= 2) {
            val responseMode = dataBytes[0].toInt() and 0xFF
            val responsePid = dataBytes[1].toInt() and 0xFF
            
            if (responseMode == expectedResponseMode) {
                // Remove mode and PID from data
                val actualData = dataBytes.drop(2).toByteArray()
                return ObdResponse(true, command, actualData, "OK")
            }
        }
        
        return ObdResponse(false, command, byteArrayOf(), "Invalid response format")
    }
    
    /**
     * Parse DTCs from response bytes
     */
    private fun parseDtcs(data: ByteArray, status: DtcStatus): List<Dtc> {
        val dtcs = mutableListOf<Dtc>()
        var i = 0
        
        while (i + 1 < data.size) {
            val byte1 = data[i].toInt() and 0xFF
            val byte2 = data[i + 1].toInt() and 0xFF
            
            // Skip empty DTCs
            if (byte1 == 0 && byte2 == 0) {
                i += 2
                continue
            }
            
            val type = when ((byte1 shr 6) and 0x03) {
                0 -> 'P' // Powertrain
                1 -> 'C' // Chassis
                2 -> 'B' // Body
                else -> 'U' // Network
            }
            
            val code = "%c%01X%03X".format(
                type,
                (byte1 shr 4) and 0x03,
                ((byte1 and 0x0F) shl 8) or byte2
            )
            
            dtcs.add(Dtc(code, status, getStandardDtcDescription(code)))
            i += 2
        }
        
        return dtcs
    }
    
    private fun parseElmVersion(response: String): ElmInfo {
        // Parse "ELM327 v2.2" or similar
        val version = Regex("ELM327 v([0-9.]+)").find(response)?.groupValues?.get(1) ?: "Unknown"
        return ElmInfo(version, response.contains("ELM327"))
    }
    
    private fun parseProtocol(response: String): Protocol {
        val protocolNum = response.filter { it.isDigit() || it in 'A'..'C' }.firstOrNull()
        return Protocol.values().find { it.value == protocolNum.toString() } ?: Protocol.AUTO
    }
    
    /**
     * Get standard DTC description
     */
    private fun getStandardDtcDescription(code: String): String {
        return when (code) {
            "P0000" -> "No fault"
            "P0001" -> "Fuel Volume Regulator Control Circuit/Open"
            "P0002" -> "Fuel Volume Regulator Control Circuit Range/Performance"
            "P0003" -> "Fuel Volume Regulator Control Circuit Low"
            "P0004" -> "Fuel Volume Regulator Control Circuit High"
            // Add hundreds more...
            else -> "Unknown DTC - check service manual"
        }
    }
    
    data class ObdResponse(
        val success: Boolean,
        val command: String,
        val data: ByteArray,
        val message: String
    )
    
    data class ElmInfo(
        val version: String,
        val genuine: Boolean
    )
    
    data class Dtc(
        val code: String,
        val status: DtcStatus,
        val description: String
    )
    
    enum class DtcStatus {
        CONFIRMED,
        PENDING,
        PERMANENT
    }
}

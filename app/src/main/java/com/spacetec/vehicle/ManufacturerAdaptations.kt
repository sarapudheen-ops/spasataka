package com.spacetec.vehicle

import android.util.Log
import com.spacetec.bluetooth.BluetoothObdTransportImpl
import com.spacetec.bluetooth.ObdResponse
import kotlinx.coroutines.delay

/**
 * Vehicle manufacturer-specific adaptations for OBD communication
 * Handles different protocols, timing, and command variations
 */
class ManufacturerAdaptations(private val transport: BluetoothObdTransportImpl) {
    
    companion object {
        private const val TAG = "ManufacturerAdaptations"
        
        // Manufacturer detection patterns from VIN
        private val MANUFACTURER_PATTERNS = mapOf(
            "1G" to Manufacturer.GENERAL_MOTORS,
            "1F" to Manufacturer.FORD,
            "1C" to Manufacturer.CHRYSLER,
            "JH" to Manufacturer.HONDA,
            "1H" to Manufacturer.HONDA,
            "JT" to Manufacturer.TOYOTA,
            "4T" to Manufacturer.TOYOTA,
            "JN" to Manufacturer.NISSAN,
            "1N" to Manufacturer.NISSAN,
            "WB" to Manufacturer.BMW,
            "4U" to Manufacturer.BMW,
            "WD" to Manufacturer.MERCEDES,
            "4J" to Manufacturer.MERCEDES,
            "WV" to Manufacturer.VOLKSWAGEN,
            "1V" to Manufacturer.VOLKSWAGEN,
            "WA" to Manufacturer.AUDI,
            "KM" to Manufacturer.HYUNDAI,
            "KN" to Manufacturer.KIA
        )
    }
    
    private var detectedManufacturer: Manufacturer = Manufacturer.GENERIC
    private var protocolSettings: ProtocolSettings = ProtocolSettings.default()
    
    /**
     * Detect manufacturer from VIN and adapt protocols
     */
    suspend fun adaptToVehicle(vin: String?) {
        if (vin != null && vin.length >= 3) {
            val wmi = vin.substring(0, 3)
            detectedManufacturer = detectManufacturer(wmi)
            protocolSettings = getProtocolSettings(detectedManufacturer)
            
            Log.i(TAG, "Detected manufacturer: $detectedManufacturer")
            Log.i(TAG, "Applied protocol settings: $protocolSettings")
            
            // Apply manufacturer-specific initialization
            applyManufacturerInit()
        }
    }
    
    private fun detectManufacturer(wmi: String): Manufacturer {
        // Try 2-character patterns first
        val twoChar = wmi.substring(0, 2)
        MANUFACTURER_PATTERNS[twoChar]?.let { return it }
        
        // Try 3-character patterns
        return when (wmi) {
            "1G1", "1G3", "1G6" -> Manufacturer.GENERAL_MOTORS
            "1FA", "1FB", "1FC" -> Manufacturer.FORD
            "1C3", "1C4", "1C6" -> Manufacturer.CHRYSLER
            "JH4", "JHM" -> Manufacturer.HONDA
            "JT2", "JT3", "JT4" -> Manufacturer.TOYOTA
            "JN1", "JN6", "JN8" -> Manufacturer.NISSAN
            "WBA", "WBS", "WBY" -> Manufacturer.BMW
            "WDB", "WDC", "WDD" -> Manufacturer.MERCEDES
            "WVW", "WVG", "WV1" -> Manufacturer.VOLKSWAGEN
            "WAU", "WA1" -> Manufacturer.AUDI
            "KMH", "KMF" -> Manufacturer.HYUNDAI
            "KND", "KNA", "KNB" -> Manufacturer.KIA
            else -> Manufacturer.GENERIC
        }
    }
    
    private fun getProtocolSettings(manufacturer: Manufacturer): ProtocolSettings {
        return when (manufacturer) {
            Manufacturer.BMW -> ProtocolSettings(
                commandDelay = 150L,
                responseTimeout = 3000L,
                initProtocol = "ATSP6", // ISO 15765-4 CAN (11 bit ID, 500 kbaud)
                headerFormat = "7E0",
                requiresExtendedAddressing = true,
                customInitCommands = listOf("ATSH7E0", "ATCF7E8", "ATCM7FF")
            )
            
            Manufacturer.MERCEDES -> ProtocolSettings(
                commandDelay = 200L,
                responseTimeout = 4000L,
                initProtocol = "ATSP6",
                headerFormat = "7E0",
                requiresExtendedAddressing = true,
                customInitCommands = listOf("ATSH7E0", "ATCF7E8")
            )
            
            Manufacturer.VOLKSWAGEN, Manufacturer.AUDI -> ProtocolSettings(
                commandDelay = 100L,
                responseTimeout = 2500L,
                initProtocol = "ATSP6",
                headerFormat = "7E0",
                requiresExtendedAddressing = false,
                customInitCommands = listOf("ATSH7E0", "ATCF7E8", "ATH1") // Headers on for VAG
            )
            
            Manufacturer.TOYOTA -> ProtocolSettings(
                commandDelay = 80L,
                responseTimeout = 2000L,
                initProtocol = "ATSP0", // Auto detect
                headerFormat = "7E0",
                requiresExtendedAddressing = false,
                customInitCommands = listOf("ATIB10") // Set baud rate
            )
            
            Manufacturer.HONDA -> ProtocolSettings(
                commandDelay = 120L,
                responseTimeout = 3000L,
                initProtocol = "ATSP6",
                headerFormat = "7E0",
                requiresExtendedAddressing = false,
                customInitCommands = listOf("ATSH7E0")
            )
            
            Manufacturer.FORD -> ProtocolSettings(
                commandDelay = 100L,
                responseTimeout = 2500L,
                initProtocol = "ATSP6",
                headerFormat = "7E0",
                requiresExtendedAddressing = false,
                customInitCommands = listOf("ATSH7E0", "ATCF7E8")
            )
            
            Manufacturer.GENERAL_MOTORS -> ProtocolSettings(
                commandDelay = 90L,
                responseTimeout = 2000L,
                initProtocol = "ATSP6",
                headerFormat = "7E0",
                requiresExtendedAddressing = false,
                customInitCommands = listOf("ATSH7E0")
            )
            
            Manufacturer.CHRYSLER -> ProtocolSettings(
                commandDelay = 110L,
                responseTimeout = 2500L,
                initProtocol = "ATSP6",
                headerFormat = "7E0",
                requiresExtendedAddressing = false,
                customInitCommands = listOf("ATSH7E0")
            )
            
            Manufacturer.NISSAN -> ProtocolSettings(
                commandDelay = 100L,
                responseTimeout = 2500L,
                initProtocol = "ATSP6",
                headerFormat = "7E0",
                requiresExtendedAddressing = false,
                customInitCommands = listOf("ATSH7E0")
            )
            
            Manufacturer.HYUNDAI, Manufacturer.KIA -> ProtocolSettings(
                commandDelay = 120L,
                responseTimeout = 3000L,
                initProtocol = "ATSP6",
                headerFormat = "7E0",
                requiresExtendedAddressing = false,
                customInitCommands = listOf("ATSH7E0", "ATCF7E8")
            )
            
            Manufacturer.GENERIC -> ProtocolSettings.default()
        }
    }
    
    private suspend fun applyManufacturerInit() {
        try {
            // Set protocol
            if (protocolSettings.initProtocol != "ATSP0") {
                transport.sendObdCommand(protocolSettings.initProtocol)
                delay(protocolSettings.commandDelay)
            }
            
            // Apply custom init commands
            for (command in protocolSettings.customInitCommands) {
                transport.sendObdCommand(command)
                delay(protocolSettings.commandDelay)
            }
            
            Log.i(TAG, "Applied manufacturer-specific initialization for $detectedManufacturer")
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply manufacturer initialization", e)
        }
    }
    
    /**
     * Send command with manufacturer-specific adaptations
     */
    suspend fun sendAdaptedCommand(command: String): ObdResponse {
        try {
            // Apply command delay
            delay(protocolSettings.commandDelay)
            
            // Send command with manufacturer-specific handling
            val response = when (detectedManufacturer) {
                Manufacturer.BMW, Manufacturer.MERCEDES -> sendGermanCommand(command)
                Manufacturer.VOLKSWAGEN, Manufacturer.AUDI -> sendVagCommand(command)
                Manufacturer.TOYOTA -> sendToyotaCommand(command)
                else -> transport.sendObdCommand(command)
            }
            
            return response
            
        } catch (e: Exception) {
            Log.e(TAG, "Adapted command failed: $command", e)
            return ObdResponse.Error("Command failed: ${e.message}")
        }
    }
    
    private suspend fun sendGermanCommand(command: String): ObdResponse {
        // German cars often need extended addressing
        return if (protocolSettings.requiresExtendedAddressing) {
            // Try with extended addressing first
            val extendedCommand = when {
                command.startsWith("01") -> "02$command" // Add service identifier
                else -> command
            }
            transport.sendObdCommand(extendedCommand)
        } else {
            transport.sendObdCommand(command)
        }
    }
    
    private suspend fun sendVagCommand(command: String): ObdResponse {
        // VAG group specific handling
        val response = transport.sendObdCommand(command)
        
        // VAG sometimes returns additional data, handle accordingly
        if (response is ObdResponse.Success && response.data.contains("SEARCHING")) {
            delay(500) // Wait for search to complete
            return transport.sendObdCommand(command) // Retry
        }
        
        return response
    }
    
    private suspend fun sendToyotaCommand(command: String): ObdResponse {
        // Toyota specific timing and retry logic
        var response = transport.sendObdCommand(command)
        
        // Toyota sometimes needs a retry with different timing
        if (response is ObdResponse.Error && response.message.contains("NO DATA")) {
            delay(200)
            response = transport.sendObdCommand(command)
        }
        
        return response
    }
    
    /**
     * Get manufacturer-specific PIDs
     */
    fun getManufacturerSpecificPids(): List<String> {
        return when (detectedManufacturer) {
            Manufacturer.BMW -> listOf(
                "22F190", // VIN
                "22F194", // Software version
                "22F18C", // Serial number
                "22F1A0", // Vehicle identification
                "221000"  // Engine data
            )
            
            Manufacturer.MERCEDES -> listOf(
                "22F190", // VIN
                "22F194", // Software version
                "22F18C", // Serial number
                "22F1AA"  // Vehicle data
            )
            
            Manufacturer.VOLKSWAGEN, Manufacturer.AUDI -> listOf(
                "22F190", // VIN
                "22F194", // Software version
                "22F18C", // Serial number
                "221000", // Engine data
                "221001"  // Transmission data
            )
            
            Manufacturer.TOYOTA -> listOf(
                "0902",   // VIN
                "0904",   // Calibration ID
                "090A"    // ECU name
            )
            
            else -> emptyList()
        }
    }
    
    /**
     * Get current manufacturer
     */
    fun getDetectedManufacturer(): Manufacturer = detectedManufacturer
    
    /**
     * Get current protocol settings
     */
    fun getProtocolSettings(): ProtocolSettings = protocolSettings
}

/**
 * Supported manufacturers
 */
enum class Manufacturer {
    GENERIC,
    BMW,
    MERCEDES,
    VOLKSWAGEN,
    AUDI,
    TOYOTA,
    HONDA,
    NISSAN,
    FORD,
    GENERAL_MOTORS,
    CHRYSLER,
    HYUNDAI,
    KIA
}

/**
 * Protocol settings for different manufacturers
 */
data class ProtocolSettings(
    val commandDelay: Long = 100L,
    val responseTimeout: Long = 2000L,
    val initProtocol: String = "ATSP0",
    val headerFormat: String = "7E0",
    val requiresExtendedAddressing: Boolean = false,
    val customInitCommands: List<String> = emptyList()
) {
    companion object {
        fun default() = ProtocolSettings()
    }
}

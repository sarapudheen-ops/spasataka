package com.spacetec.ecu

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * ECU Programming and Flashing System
 * Supports firmware updates, calibration programming, and ECU configuration
 */
class EcuProgramming(
    private val inputStream: InputStream?,
    private val outputStream: OutputStream?
) {
    
    private val _programmingState = MutableStateFlow(ProgrammingState.IDLE)
    val programmingState: StateFlow<ProgrammingState> = _programmingState.asStateFlow()
    
    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()
    
    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()
    
    private val _supportedEcus = MutableStateFlow<List<EcuInfo>>(emptyList())
    val supportedEcus: StateFlow<List<EcuInfo>> = _supportedEcus.asStateFlow()
    
    companion object {
        // Programming session timeouts
        const val SESSION_TIMEOUT_MS = 30000L
        const val BLOCK_TIMEOUT_MS = 5000L
        const val RESPONSE_TIMEOUT_MS = 2000L
        
        // Flash memory parameters
        const val FLASH_BLOCK_SIZE = 256
        const val EEPROM_BLOCK_SIZE = 64
        const val VERIFICATION_RETRIES = 3
    }
    
    /**
     * Initialize ECU programming session
     */
    suspend fun initializeProgrammingSession(ecuInfo: EcuInfo): Boolean {
        _programmingState.value = ProgrammingState.INITIALIZING
        _statusMessage.value = "Initializing programming session..."
        
        return try {
            // Step 1: Enter programming mode
            if (!enterProgrammingMode(ecuInfo)) {
                _statusMessage.value = "Failed to enter programming mode"
                _programmingState.value = ProgrammingState.ERROR
                return false
            }
            
            // Step 2: Authenticate with ECU
            if (!authenticateEcu(ecuInfo)) {
                _statusMessage.value = "ECU authentication failed"
                _programmingState.value = ProgrammingState.ERROR
                return false
            }
            
            // Step 3: Verify ECU compatibility
            if (!verifyEcuCompatibility(ecuInfo)) {
                _statusMessage.value = "ECU compatibility check failed"
                _programmingState.value = ProgrammingState.ERROR
                return false
            }
            
            _statusMessage.value = "Programming session initialized successfully"
            _programmingState.value = ProgrammingState.READY
            true
            
        } catch (e: Exception) {
            _statusMessage.value = "Initialization error: ${e.message}"
            _programmingState.value = ProgrammingState.ERROR
            false
        }
    }
    
    /**
     * Flash firmware to ECU
     */
    suspend fun flashFirmware(firmwareFile: File, ecuInfo: EcuInfo): Boolean {
        if (_programmingState.value != ProgrammingState.READY) {
            _statusMessage.value = "Programming session not ready"
            return false
        }
        
        _programmingState.value = ProgrammingState.PROGRAMMING
        _progress.value = 0
        
        return try {
            val firmwareData = firmwareFile.readBytes()
            _statusMessage.value = "Preparing firmware data (${firmwareData.size} bytes)..."
            
            // Step 1: Erase flash memory
            if (!eraseFlashMemory(ecuInfo)) {
                _statusMessage.value = "Flash erase failed"
                _programmingState.value = ProgrammingState.ERROR
                return false
            }
            
            // Step 2: Program firmware in blocks
            val totalBlocks = (firmwareData.size + FLASH_BLOCK_SIZE - 1) / FLASH_BLOCK_SIZE
            
            for (blockIndex in 0 until totalBlocks) {
                val startOffset = blockIndex * FLASH_BLOCK_SIZE
                val endOffset = minOf(startOffset + FLASH_BLOCK_SIZE, firmwareData.size)
                val blockData = firmwareData.sliceArray(startOffset until endOffset)
                
                _statusMessage.value = "Programming block ${blockIndex + 1}/$totalBlocks..."
                
                if (!programFlashBlock(startOffset, blockData, ecuInfo)) {
                    _statusMessage.value = "Programming failed at block ${blockIndex + 1}"
                    _programmingState.value = ProgrammingState.ERROR
                    return false
                }
                
                _progress.value = ((blockIndex + 1) * 100) / totalBlocks
                delay(10) // Allow UI updates
            }
            
            // Step 3: Verify programmed data
            _statusMessage.value = "Verifying programmed firmware..."
            if (!verifyFirmware(firmwareData, ecuInfo)) {
                _statusMessage.value = "Firmware verification failed"
                _programmingState.value = ProgrammingState.ERROR
                return false
            }
            
            // Step 4: Finalize programming
            if (!finalizeProgramming(ecuInfo)) {
                _statusMessage.value = "Programming finalization failed"
                _programmingState.value = ProgrammingState.ERROR
                return false
            }
            
            _statusMessage.value = "Firmware programming completed successfully"
            _programmingState.value = ProgrammingState.COMPLETED
            _progress.value = 100
            true
            
        } catch (e: Exception) {
            _statusMessage.value = "Programming error: ${e.message}"
            _programmingState.value = ProgrammingState.ERROR
            false
        }
    }
    
    /**
     * Program calibration data to ECU
     */
    suspend fun programCalibration(calibrationFile: File, ecuInfo: EcuInfo): Boolean {
        if (_programmingState.value != ProgrammingState.READY) {
            _statusMessage.value = "Programming session not ready"
            return false
        }
        
        _programmingState.value = ProgrammingState.PROGRAMMING
        _progress.value = 0
        
        return try {
            val calibrationData = calibrationFile.readBytes()
            _statusMessage.value = "Programming calibration data (${calibrationData.size} bytes)..."
            
            // Program calibration in smaller blocks
            val totalBlocks = (calibrationData.size + EEPROM_BLOCK_SIZE - 1) / EEPROM_BLOCK_SIZE
            
            for (blockIndex in 0 until totalBlocks) {
                val startOffset = blockIndex * EEPROM_BLOCK_SIZE
                val endOffset = minOf(startOffset + EEPROM_BLOCK_SIZE, calibrationData.size)
                val blockData = calibrationData.sliceArray(startOffset until endOffset)
                
                _statusMessage.value = "Programming calibration block ${blockIndex + 1}/$totalBlocks..."
                
                if (!programCalibrationBlock(startOffset, blockData, ecuInfo)) {
                    _statusMessage.value = "Calibration programming failed at block ${blockIndex + 1}"
                    _programmingState.value = ProgrammingState.ERROR
                    return false
                }
                
                _progress.value = ((blockIndex + 1) * 100) / totalBlocks
                delay(10)
            }
            
            _statusMessage.value = "Calibration programming completed successfully"
            _programmingState.value = ProgrammingState.COMPLETED
            _progress.value = 100
            true
            
        } catch (e: Exception) {
            _statusMessage.value = "Calibration programming error: ${e.message}"
            _programmingState.value = ProgrammingState.ERROR
            false
        }
    }
    
    /**
     * Read ECU information and identification
     */
    suspend fun readEcuInfo(): EcuInfo? {
        return try {
            _statusMessage.value = "Reading ECU information..."
            
            // Read basic ECU identification
            val ecuId = readEcuIdentification()
            val softwareVersion = readSoftwareVersion()
            val hardwareVersion = readHardwareVersion()
            val serialNumber = readSerialNumber()
            val calibrationId = readCalibrationId()
            
            if (ecuId != null) {
                EcuInfo(
                    id = ecuId,
                    name = getEcuName(ecuId),
                    softwareVersion = softwareVersion ?: "Unknown",
                    hardwareVersion = hardwareVersion ?: "Unknown",
                    serialNumber = serialNumber ?: "Unknown",
                    calibrationId = calibrationId ?: "Unknown",
                    supportedProtocols = getSupportedProtocols(ecuId),
                    flashMemorySize = getFlashMemorySize(ecuId),
                    eepromSize = getEepromSize(ecuId)
                )
            } else null
            
        } catch (e: Exception) {
            _statusMessage.value = "Error reading ECU info: ${e.message}"
            null
        }
    }
    
    /**
     * Load supported ECU database
     */
    fun loadSupportedEcus() {
        val ecuList = mutableListOf<EcuInfo>()
        
        // Add common ECU types
        ecuList.addAll(getCommonEcus())
        
        _supportedEcus.value = ecuList
    }
    
    // Private helper methods
    
    private suspend fun enterProgrammingMode(ecuInfo: EcuInfo): Boolean {
        return try {
            // Send programming mode request based on ECU type
            val request = when (ecuInfo.getManufacturer()) {
                "Bosch" -> byteArrayOf(0x10, 0x02) // UDS Programming Session
                "Continental" -> byteArrayOf(0x10, 0x85.toByte()) // KWP Programming Session
                "Delphi" -> byteArrayOf(0x10, 0x03) // Extended Session
                else -> byteArrayOf(0x10, 0x02) // Default UDS
            }
            
            sendRequest(request)
            val response = receiveResponse(RESPONSE_TIMEOUT_MS)
            
            response != null && response.isNotEmpty() && response[0] == 0x50.toByte()
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun authenticateEcu(ecuInfo: EcuInfo): Boolean {
        return try {
            // Request seed for security access
            val seedRequest = byteArrayOf(0x27, 0x01)
            sendRequest(seedRequest)
            
            val seedResponse = receiveResponse(RESPONSE_TIMEOUT_MS)
            if (seedResponse == null || seedResponse.size < 6) return false
            
            // Extract seed from response
            val seed = seedResponse.sliceArray(2 until seedResponse.size)
            
            // Calculate key based on seed and ECU-specific algorithm
            val key = calculateSecurityKey(seed, ecuInfo)
            
            // Send key
            val keyRequest = byteArrayOf(0x27, 0x02) + key
            sendRequest(keyRequest)
            
            val keyResponse = receiveResponse(RESPONSE_TIMEOUT_MS)
            keyResponse != null && keyResponse.isNotEmpty() && keyResponse[0] == 0x67.toByte()
            
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun verifyEcuCompatibility(ecuInfo: EcuInfo): Boolean {
        // Check if ECU supports programming
        return ecuInfo.supportedProtocols.contains("UDS") || 
               ecuInfo.supportedProtocols.contains("KWP2000")
    }
    
    private suspend fun eraseFlashMemory(ecuInfo: EcuInfo): Boolean {
        return try {
            // Send erase request
            val eraseRequest = byteArrayOf(0x31, 0x01, 0xFF.toByte(), 0x00) // Routine Control - Erase
            sendRequest(eraseRequest)
            
            val response = receiveResponse(SESSION_TIMEOUT_MS) // Erase takes longer
            response != null && response.isNotEmpty() && response[0] == 0x71.toByte()
            
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun programFlashBlock(address: Int, data: ByteArray, ecuInfo: EcuInfo): Boolean {
        return try {
            // Send download request for this block
            val downloadRequest = byteArrayOf(0x34, 0x00) + 
                                 intToByteArray(address) + 
                                 intToByteArray(data.size)
            sendRequest(downloadRequest)
            
            var response = receiveResponse(RESPONSE_TIMEOUT_MS)
            if (response == null || response[0] != 0x74.toByte()) return false
            
            // Send data
            val dataRequest = byteArrayOf(0x36, 0x01) + data
            sendRequest(dataRequest)
            
            response = receiveResponse(BLOCK_TIMEOUT_MS)
            response != null && response.isNotEmpty() && response[0] == 0x76.toByte()
            
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun programCalibrationBlock(address: Int, data: ByteArray, ecuInfo: EcuInfo): Boolean {
        return try {
            // Write data by address for calibration
            val writeRequest = byteArrayOf(0x3D) + 
                              intToByteArray(address) + 
                              byteArrayOf(data.size.toByte()) + 
                              data
            sendRequest(writeRequest)
            
            val response = receiveResponse(RESPONSE_TIMEOUT_MS)
            response != null && response.isNotEmpty() && response[0] == 0x7D.toByte()
            
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun verifyFirmware(originalData: ByteArray, ecuInfo: EcuInfo): Boolean {
        return try {
            // Read back programmed data and compare
            val readRequest = byteArrayOf(0x23, 0x00, 0x00, 0x00, 0x00) + 
                             intToByteArray(originalData.size)
            sendRequest(readRequest)
            
            val response = receiveResponse(SESSION_TIMEOUT_MS)
            if (response == null || response.size < originalData.size + 2) return false
            
            val readData = response.sliceArray(2 until response.size)
            readData.contentEquals(originalData)
            
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun finalizeProgramming(ecuInfo: EcuInfo): Boolean {
        return try {
            // Send transfer exit
            val exitRequest = byteArrayOf(0x37)
            sendRequest(exitRequest)
            
            val response = receiveResponse(RESPONSE_TIMEOUT_MS)
            response != null && response.isNotEmpty() && response[0] == 0x77.toByte()
            
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun readEcuIdentification(): String? {
        return try {
            val request = byteArrayOf(0x22, 0xF1.toByte(), 0x90.toByte()) // VIN
            sendRequest(request)
            
            val response = receiveResponse(RESPONSE_TIMEOUT_MS)
            if (response != null && response.size > 3) {
                String(response.sliceArray(3 until response.size))
            } else null
            
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun readSoftwareVersion(): String? {
        return try {
            val request = byteArrayOf(0x22, 0xF1.toByte(), 0x94.toByte()) // Software Number
            sendRequest(request)
            
            val response = receiveResponse(RESPONSE_TIMEOUT_MS)
            if (response != null && response.size > 3) {
                String(response.sliceArray(3 until response.size))
            } else null
            
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun readHardwareVersion(): String? {
        return try {
            val request = byteArrayOf(0x22, 0xF1.toByte(), 0x91.toByte()) // Hardware Number
            sendRequest(request)
            
            val response = receiveResponse(RESPONSE_TIMEOUT_MS)
            if (response != null && response.size > 3) {
                String(response.sliceArray(3 until response.size))
            } else null
            
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun readSerialNumber(): String? {
        return try {
            val request = byteArrayOf(0x22, 0xF1.toByte(), 0x8C.toByte()) // Serial Number
            sendRequest(request)
            
            val response = receiveResponse(RESPONSE_TIMEOUT_MS)
            if (response != null && response.size > 3) {
                response.sliceArray(3 until response.size).joinToString("") { "%02X".format(it) }
            } else null
            
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun readCalibrationId(): String? {
        return try {
            val request = byteArrayOf(0x22, 0xF1.toByte(), 0x8A.toByte()) // Calibration ID
            sendRequest(request)
            
            val response = receiveResponse(RESPONSE_TIMEOUT_MS)
            if (response != null && response.size > 3) {
                String(response.sliceArray(3 until response.size))
            } else null
            
        } catch (e: Exception) {
            null
        }
    }
    
    private fun calculateSecurityKey(seed: ByteArray, ecuInfo: EcuInfo): ByteArray {
        // Simplified key calculation - real implementation would use ECU-specific algorithms
        val key = ByteArray(seed.size)
        for (i in seed.indices) {
            key[i] = ((seed[i].toInt() and 0xFF) xor 0xAA).toByte()
        }
        return key
    }
    
    private suspend fun sendRequest(data: ByteArray) {
        outputStream?.write(data)
        outputStream?.flush()
    }
    
    private suspend fun receiveResponse(timeoutMs: Long): ByteArray? {
        if (inputStream == null) return null
        
        val startTime = System.currentTimeMillis()
        val buffer = ByteArray(1024)
        val response = mutableListOf<Byte>()
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (inputStream.available() > 0) {
                val bytes = inputStream.read(buffer)
                if (bytes > 0) {
                    for (i in 0 until bytes) {
                        response.add(buffer[i])
                    }
                    
                    // Check for complete response
                    if (response.isNotEmpty() && (response[0] >= 0x40 || response[0] == 0x7F.toByte())) {
                        return response.toByteArray()
                    }
                }
            }
            delay(10)
        }
        
        return if (response.isNotEmpty()) response.toByteArray() else null
    }
    
    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte()
        )
    }
    
    private fun getEcuName(ecuId: String): String {
        return when {
            ecuId.contains("0281") -> "Bosch EDC17"
            ecuId.contains("0261") -> "Bosch ME17"
            ecuId.contains("5WP4") -> "Continental EMS3"
            ecuId.contains("28RT") -> "Delphi MT92"
            else -> "Unknown ECU"
        }
    }
    
    private fun getSupportedProtocols(ecuId: String): List<String> {
        return when {
            ecuId.contains("0281") || ecuId.contains("0261") -> listOf("UDS", "CAN")
            ecuId.contains("5WP4") -> listOf("KWP2000", "CAN")
            else -> listOf("OBD2")
        }
    }
    
    private fun getFlashMemorySize(ecuId: String): Int {
        return when {
            ecuId.contains("EDC17") -> 2048 * 1024 // 2MB
            ecuId.contains("ME17") -> 1536 * 1024 // 1.5MB
            ecuId.contains("EMS3") -> 1024 * 1024 // 1MB
            else -> 512 * 1024 // 512KB default
        }
    }
    
    private fun getEepromSize(ecuId: String): Int {
        return when {
            ecuId.contains("EDC17") -> 32 * 1024 // 32KB
            ecuId.contains("ME17") -> 16 * 1024 // 16KB
            else -> 8 * 1024 // 8KB default
        }
    }
    
    private fun getCommonEcus(): List<EcuInfo> {
        return listOf(
            EcuInfo(
                id = "0281020055",
                name = "Bosch EDC17C46",
                softwareVersion = "1037394584",
                hardwareVersion = "0281020055",
                serialNumber = "00000000",
                calibrationId = "1037394584",
                supportedProtocols = listOf("UDS", "CAN"),
                flashMemorySize = 2048 * 1024,
                eepromSize = 32 * 1024
            ),
            EcuInfo(
                id = "0261S07770",
                name = "Bosch ME17.5.26",
                softwareVersion = "1037516953",
                hardwareVersion = "0261S07770",
                serialNumber = "00000000",
                calibrationId = "1037516953",
                supportedProtocols = listOf("UDS", "CAN"),
                flashMemorySize = 1536 * 1024,
                eepromSize = 16 * 1024
            ),
            EcuInfo(
                id = "5WP45318HT",
                name = "Continental EMS3134",
                softwareVersion = "SW1234567890",
                hardwareVersion = "5WP45318HT",
                serialNumber = "00000000",
                calibrationId = "CAL1234567890",
                supportedProtocols = listOf("KWP2000", "CAN"),
                flashMemorySize = 1024 * 1024,
                eepromSize = 16 * 1024
            )
        )
    }
    
    fun cleanup() {
        _programmingState.value = ProgrammingState.IDLE
        _progress.value = 0
        _statusMessage.value = ""
    }
}

enum class ProgrammingState {
    IDLE,
    INITIALIZING,
    READY,
    PROGRAMMING,
    COMPLETED,
    ERROR
}

data class EcuInfo(
    val id: String,
    val name: String,
    val softwareVersion: String,
    val hardwareVersion: String,
    val serialNumber: String,
    val calibrationId: String,
    val supportedProtocols: List<String>,
    val flashMemorySize: Int,
    val eepromSize: Int
) {
    fun getManufacturer(): String {
        return when {
            id.startsWith("0281") || id.startsWith("0261") -> "Bosch"
            id.startsWith("5WP4") -> "Continental"
            id.startsWith("28RT") -> "Delphi"
            id.startsWith("A2C") -> "Continental"
            else -> "Unknown"
        }
    }
    
    fun supportsFlashing(): Boolean {
        return supportedProtocols.contains("UDS") || supportedProtocols.contains("KWP2000")
    }
    
    fun getMemoryInfo(): String {
        val flashMB = flashMemorySize / (1024 * 1024)
        val eepromKB = eepromSize / 1024
        return "Flash: ${flashMB}MB, EEPROM: ${eepromKB}KB"
    }
}

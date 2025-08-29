package com.spacetec.ecu

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * ECU Programmer - High-performance, reliable ECU programming implementation
 */
class EcuProgrammer(
    private val transport: EcuTransport,
    private val securityProvider: EcuSecurityProvider = DefaultEcuSecurityProvider(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val timeout: Duration = 5.seconds
) : Closeable {
    private val scope = CoroutineScope(ioDispatcher + Job())
    private var isInitialized = false

    /**
     * Program firmware to ECU
     * @param firmware The firmware data to program
     * @param options Programming options
     * @return Result indicating success/failure
     */
    suspend fun programFirmware(
        firmware: ByteArray,
        options: ProgramOptions = ProgramOptions()
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            require(firmware.isNotEmpty()) { "Firmware data cannot be empty" }
            
            withTimeoutOrNull(timeout) {
                initialize()
                authenticate()
                
                val memoryLayout = readMemoryLayout()
                validateFirmware(firmware, memoryLayout)
                
                programMemory(firmware, memoryLayout, options)
                verifyProgramming(firmware, memoryLayout)
                
                finalizeProgramming()
            } ?: throw TimeoutCancellationException("Programming operation timed out")
        }.onFailure { error ->
            handleError(error)
        }
    }

    private suspend fun initialize() {
        if (isInitialized) return
        
        transport.sendCommand(Command.INITIALIZE)
        val response = transport.receiveResponse()
        
        if (response.status != ResponseStatus.SUCCESS) {
            throw EcuException("Failed to initialize ECU: ${response.data?.toString(Charsets.UTF_8)}")
        }
        
        isInitialized = true
    }

    private suspend fun authenticate() {
        val challenge = securityProvider.generateChallenge()
        transport.sendCommand(Command.AUTHENTICATE, challenge)
        
        val response = transport.receiveResponse()
        if (response.status != ResponseStatus.AUTH_REQUIRED) {
            throw SecurityException("Unexpected authentication response")
        }
        
        val encryptedResponse = response.data ?: 
            throw SecurityException("No authentication data received")
            
        if (!securityProvider.verifyResponse(encryptedResponse)) {
            throw SecurityException("Authentication failed")
        }
    }

    private suspend fun readMemoryLayout(): MemoryLayout {
        transport.sendCommand(Command.READ_MEMORY_LAYOUT)
        val response = transport.receiveResponse()
        
        return response.data?.let { MemoryLayout.fromBytes(it) } ?: 
            throw EcuException("Failed to read memory layout")
    }

    private fun validateFirmware(firmware: ByteArray, layout: MemoryLayout) {
        if (firmware.size > layout.totalSize) {
            throw IllegalArgumentException("Firmware size (${firmware.size}) exceeds available memory (${layout.totalSize})")
        }
        
        // Verify firmware checksum using the specified algorithm
        val checksumAlgorithm = when (layout.checksumType) {
            "CRC16" -> ChecksumAlgorithm.CRC16_CCITT
            "CRC32" -> ChecksumAlgorithm.CRC32
            "SUM8" -> ChecksumAlgorithm.SUM8
            "SUM16" -> ChecksumAlgorithm.SUM16
            "XOR" -> ChecksumAlgorithm.XOR
            else -> throw SecurityException("Unsupported checksum type: ${layout.checksumType}")
        }
        
        if (!verifyChecksum(firmware, checksumAlgorithm)) {
            throw SecurityException("Firmware checksum validation failed using ${checksumAlgorithm.name}")
        }
    }

    private suspend fun programMemory(
        data: ByteArray,
        layout: MemoryLayout,
        options: ProgramOptions
    ) {
        val blockSize = options.blockSize
        val totalBlocks = ceil(data.size.toDouble() / blockSize).toInt()
        
        // Erase required memory regions
        eraseMemoryRegions(layout)
        
        // Program in blocks
        for (blockIndex in 0 until totalBlocks) {
            val start = blockIndex * blockSize
            val end = minOf(start + blockSize, data.size)
            val block = data.copyOfRange(start, end)
            
            programBlock(block, start, options.retryCount)
            
            // Notify progress
            val progress = (blockIndex + 1) * 100 / totalBlocks
            onProgressUpdate(progress)
        }
    }

    private suspend fun programBlock(
        data: ByteArray,
        address: Int,
        maxRetries: Int
    ) {
        var retryCount = 0
        var success = false
        
        while (!success && retryCount <= maxRetries) {
            try {
                transport.sendCommand(Command.WRITE_BLOCK, address.toByteArray() + data)
                val response = transport.receiveResponse()
                
                if (response.status != ResponseStatus.SUCCESS) {
                    throw IOException("Block write failed: ${response.status}")
                }
                
                success = true
            } catch (e: Exception) {
                if (++retryCount > maxRetries) throw e
                delay(100 * retryCount) // Exponential backoff
            }
        }
    }

    private suspend fun verifyProgramming(
        expected: ByteArray,
        layout: MemoryLayout
    ) {
        // Read back and verify critical sections
        val verificationSize = minOf(expected.size, 4096) // Verify first 4KB
        val verificationData = ByteArray(verificationSize)
        
        transport.sendCommand(Command.READ_BLOCK, 0.toByteArray() + verificationSize.toByte())
        val response = transport.receiveResponse()
        
        if (response.status != ResponseStatus.SUCCESS || response.data == null) {
            throw IOException("Failed to read back data for verification")
        }
        
        if (!expected.copyOf(verificationSize).contentEquals(response.data)) {
            throw SecurityException("Verification failed: Data mismatch")
        }
    }

    private suspend fun finalizeProgramming() {
        transport.sendCommand(Command.FINALIZE)
        val response = transport.receiveResponse()
        
        if (response.status != ResponseStatus.SUCCESS) {
            throw IOException("Finalization failed: ${response.status}")
        }
        
        // Reset connection
        transport.reset()
        isInitialized = false
    }

    private fun handleError(error: Throwable): Result<Unit> {
        return when (error) {
            is TimeoutCancellationException -> 
                Result.failure(EcuException("Operation timed out", error))
            is SecurityException -> 
                Result.failure(EcuException("Security violation: ${error.message}", error))
            is IOException -> 
                Result.failure(EcuException("I/O error: ${error.message}", error))
            else -> 
                Result.failure(EcuException("Programming failed: ${error.message}", error))
        }
    }

    private fun onProgressUpdate(progress: Int) {
        // Implement progress reporting (e.g., Flow, callback, etc.)
    }

    override fun close() {
        scope.cancel()
        transport.close()
    }

    companion object {
        private const val DEFAULT_BLOCK_SIZE = 1024
        
        /**
         * Calculate checksum for data using the specified algorithm
         * @param data The data to calculate checksum for
         * @param algorithm The checksum algorithm to use
         * @return The calculated checksum bytes
         */
        fun calculateChecksum(data: ByteArray, algorithm: ChecksumAlgorithm): ByteArray {
            return when (algorithm) {
                ChecksumAlgorithm.SUM8 -> calculateSum8(data)
                ChecksumAlgorithm.SUM16 -> calculateSum16(data)
                ChecksumAlgorithm.CRC8 -> calculateCrc8(data)
                ChecksumAlgorithm.CRC16_CCITT -> calculateCrc16Ccitt(data)
                ChecksumAlgorithm.CRC32 -> calculateCrc32(data)
                ChecksumAlgorithm.XOR -> calculateXorChecksum(data)
            }
        }
    }
}

/**
 * ECU Transport interface for communication
 */
interface EcuTransport : Closeable {
    suspend fun sendCommand(command: Command, data: ByteArray = byteArrayOf())
    suspend fun receiveResponse(): Response
    fun reset()
}

/**
 * ECU Security Provider interface
 */
interface EcuSecurityProvider {
    fun generateChallenge(): ByteArray
    fun verifyResponse(response: ByteArray): Boolean
}

/**
 * Default security provider implementation
 */
class DefaultEcuSecurityProvider : EcuSecurityProvider {
    override fun generateChallenge(): ByteArray {
        return ByteArray(16).also { Random().nextBytes(it) }
    }

    override fun verifyResponse(response: ByteArray): Boolean {
        // Implement proper security verification
        return response.isNotEmpty()
    }
}

/**
 * ECU Memory Layout
 */
/**
 * Represents the memory layout of an ECU
 * @property regions List of memory regions
 * @property checksumType Type of checksum used (CRC16, CRC32, SUM8, SUM16, XOR)
 * @property totalSize Total size of all memory regions in bytes
 */
data class MemoryLayout(
    val regions: List<MemoryRegion>,
    val checksumType: String = "CRC16",
    val totalSize: Int = regions.sumOf { it.size }
) {
    companion object {
        fun fromBytes(data: ByteArray): MemoryLayout {
            // Parse memory layout from bytes
            // Implementation depends on ECU-specific format
            return MemoryLayout(emptyList())
        }
    }
}

data class MemoryRegion(
    val start: Int,
    val size: Int,
    val type: MemoryType,
    val isWritable: Boolean = true
)

enum class MemoryType {
    FLASH, EEPROM, RAM, BOOTLOADER
}

enum class Command(val value: Byte) {
    INITIALIZE(0x01),
    AUTHENTICATE(0x02),
    READ_MEMORY_LAYOUT(0x10),
    READ_BLOCK(0x11),
    WRITE_BLOCK(0x12),
    ERASE_SECTOR(0x20),
    FINALIZE(0xFF)
}

data class Response(
    val status: ResponseStatus,
    val data: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Response

        if (status != other.status) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = status.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        return result
    }
}

enum class ResponseStatus(val value: Byte) {
    SUCCESS(0x00),
    AUTH_REQUIRED(0x01),
    INVALID_COMMAND(0x02),
    INVALID_ADDRESS(0x03),
    VERIFICATION_FAILED(0x04),
    TIMEOUT(0x05),
    BUSY(0x06),
    ERROR(0xFF)
}

data class ProgramOptions(
    val blockSize: Int = 1024,
    val retryCount: Int = 3,
    val verifyAfterWrite: Boolean = true
)

class EcuException(message: String, cause: Throwable? = null) : 
    IOException(message, cause)

// Extension functions
private fun Int.toByteArray(): ByteArray {
    return byteArrayOf(
        (this shr 24).toByte(),
        (this shr 16).toByte(),
        (this shr 8).toByte(),
        this.toByte()
    )
}

/**
 * Verify checksum of ECU data using common algorithms
 * @param data The data to verify (including checksum bytes)
 * @param algorithm The checksum algorithm to use
 * @return true if checksum is valid, false otherwise
 */
private fun verifyChecksum(
    data: ByteArray,
    algorithm: ChecksumAlgorithm = ChecksumAlgorithm.CRC16_CCITT
): Boolean {
    if (data.size < algorithm.checksumSize) return false
    
    // Extract payload and checksum
    val payload = data.copyOfRange(0, data.size - algorithm.checksumSize)
    val expectedChecksum = data.copyOfRange(data.size - algorithm.checksumSize, data.size)
    
    // Calculate actual checksum
    val actualChecksum = when (algorithm) {
        ChecksumAlgorithm.SUM8 -> calculateSum8(payload)
        ChecksumAlgorithm.SUM16 -> calculateSum16(payload)
        ChecksumAlgorithm.CRC8 -> calculateCrc8(payload)
        ChecksumAlgorithm.CRC16_CCITT -> calculateCrc16Ccitt(payload)
        ChecksumAlgorithm.CRC32 -> calculateCrc32(payload)
        ChecksumAlgorithm.XOR -> calculateXorChecksum(payload)
    }
    
    return expectedChecksum.contentEquals(actualChecksum)
}

/**
 * Common checksum algorithms used in ECUs
 */
enum class ChecksumAlgorithm(val checksumSize: Int) {
    SUM8(1),          // 8-bit sum
    SUM16(2),         // 16-bit sum
    CRC8(1),          // 8-bit CRC
    CRC16_CCITT(2),   // 16-bit CRC-CCITT (used in UDS)
    CRC32(4),         // 32-bit CRC
    XOR(1)            // 8-bit XOR
}

// Checksum calculation implementations
private fun calculateSum8(data: ByteArray): ByteArray {
    var sum: UByte = 0u
    data.forEach { byte -> sum = (sum + byte.toUByte()).toUByte() }
    return byteArrayOf(sum.toByte())
}

private fun calculateSum16(data: ByteArray): ByteArray {
    var sum: UShort = 0u
    var i = 0
    while (i < data.size) {
        val value = if (i + 1 < data.size) {
            ((data[i].toUByte().toInt() shl 8) or data[i + 1].toUByte().toInt()).toUShort()
        } else {
            data[i].toUByte().toUShort()
        }
        sum = (sum + value).toUShort()
        i += 2
    }
    return byteArrayOf((sum.toInt() shr 8).toByte(), sum.toByte())
}

private fun calculateCrc8(data: ByteArray, polynomial: UByte = 0x07u): ByteArray {
    var crc: UByte = 0u
    for (b in data) {
        crc = crc xor b.toUByte()
        for (i in 0 until 8) {
            crc = if ((crc and 0x80u) != 0u) {
                ((crc.toInt() shl 1).toUByte()) xor polynomial
            } else {
                (crc.toInt() shl 1).toUByte()
            }
        }
    }
    return byteArrayOf(crc.toByte())
}

private fun calculateCrc16Ccitt(data: ByteArray): ByteArray {
    var crc: UShort = 0xFFFFu
    for (b in data) {
        crc = crc xor (b.toUShort() shl 8)
        for (i in 0 until 8) {
            crc = if ((crc and 0x8000u) != 0u) {
                ((crc.toInt() shl 1).toUShort()) xor 0x1021u
            } else {
                (crc.toInt() shl 1).toUShort()
            }
        }
    }
    return byteArrayOf((crc.toInt() shr 8).toByte(), crc.toByte())
}

private fun calculateCrc32(data: ByteArray): ByteArray {
    var crc = 0xFFFFFFFFuL
    for (b in data) {
        crc = crc xor (b.toULong() and 0xFFu)
        for (i in 0 until 8) {
            crc = if ((crc and 0x00000001u) != 0uL) {
                (crc shr 1) xor 0xEDB88320uL
            } else {
                crc shr 1
            }
        }
    }
    crc = crc xor 0xFFFFFFFFuL
    return byteArrayOf(
        ((crc shr 24) and 0xFFu).toByte(),
        ((crc shr 16) and 0xFFu).toByte(),
        ((crc shr 8) and 0xFFu).toByte(),
        (crc and 0xFFu).toByte()
    )
}

private fun calculateXorChecksum(data: ByteArray): ByteArray {
    var checksum: UByte = 0u
    data.forEach { byte -> checksum = checksum xor byte.toUByte() }
    return byteArrayOf(checksum.toByte())
}

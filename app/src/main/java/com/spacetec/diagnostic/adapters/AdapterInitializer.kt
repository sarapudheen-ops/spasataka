package com.spacetec.diagnostic.adapters

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.util.*

/**
 * Comprehensive adapter initialization system for SpaceTec OBD diagnostics
 */

interface AdapterInitializer {
    suspend fun initialize(): InitializationResult
    suspend fun cleanup()
    fun getAdapterInfo(): AdapterInfo
}

data class InitializationResult(
    val success: Boolean,
    val message: String,
    val adapterType: AdapterType,
    val supportedProtocols: List<ObdProtocol> = emptyList(),
    val deviceInfo: Map<String, String> = emptyMap()
)

data class AdapterInfo(
    val name: String,
    val type: AdapterType,
    val version: String,
    val capabilities: Set<AdapterCapability>
)

enum class AdapterType {
    BLUETOOTH_CLASSIC,
    BLUETOOTH_LE,
    WIFI,
    USB_SERIAL,
    NATIVE_AUTEL,
    NATIVE_J2534
}

enum class ObdProtocol {
    ISO_9141_2,
    KWP_2000_SLOW,
    KWP_2000_FAST,
    CAN_11BIT_500K,
    CAN_29BIT_500K,
    CAN_11BIT_250K,
    CAN_29BIT_250K,
    SAE_J1850_PWM,
    SAE_J1850_VPW,
    ISO_14230_4_KWP_SLOW,
    ISO_14230_4_KWP_FAST
}

enum class AdapterCapability {
    BASIC_OBD,
    AUTO_PROTOCOL_DETECTION,
    ADVANCED_DIAGNOSTICS,
    BIDIRECTIONAL_CONTROL,
    HIGH_SPEED_CAN,
    MULTI_ECU_SUPPORT,
    REAL_TIME_DATA,
    DTC_MANAGEMENT,
    VEHICLE_IDENTIFICATION,
    MANUFACTURER_SPECIFIC
}

/**
 * ELM327 Bluetooth Classic Adapter Initialization
 */
class Elm327BluetoothClassicInitializer(
    private val context: Context,
    private val targetDevice: BluetoothDevice
) : AdapterInitializer {
    
    private var bluetoothSocket: BluetoothSocket? = null
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID
    
    companion object {
        private const val TAG = "ELM327BTClassic"
        private const val INIT_TIMEOUT = 10000L
    }
    
    override suspend fun initialize(): InitializationResult {
        return try {
            Log.i(TAG, "Starting ELM327 Bluetooth Classic initialization...")
            
            // Step 1: Establish Bluetooth connection
            val connectionResult = establishConnection()
            if (!connectionResult.success) {
                return connectionResult
            }
            
            // Step 2: Initialize ELM327 with AT commands
            val elmInitResult = initializeElm327()
            if (!elmInitResult.success) {
                cleanup()
                return elmInitResult
            }
            
            // Step 3: Detect OBD protocol
            val protocolResult = detectObdProtocol()
            if (!protocolResult.success) {
                cleanup()
                return protocolResult
            }
            
            // Step 4: Verify connection with basic OBD command
            val verificationResult = verifyConnection()
            if (!verificationResult.success) {
                cleanup()
                return verificationResult
            }
            
            Log.i(TAG, "ELM327 Bluetooth Classic initialization completed successfully")
            InitializationResult(
                success = true,
                message = "ELM327 adapter initialized successfully",
                adapterType = AdapterType.BLUETOOTH_CLASSIC,
                supportedProtocols = listOf(
                    ObdProtocol.CAN_11BIT_500K,
                    ObdProtocol.CAN_29BIT_500K,
                    ObdProtocol.ISO_9141_2,
                    ObdProtocol.KWP_2000_SLOW,
                    ObdProtocol.KWP_2000_FAST
                ),
                deviceInfo = mapOf(
                    "device_name" to (targetDevice.name ?: "Unknown"),
                    "device_address" to targetDevice.address,
                    "adapter_type" to "ELM327",
                    "connection_type" to "Bluetooth Classic"
                )
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "ELM327 initialization failed: ${e.message}")
            cleanup()
            InitializationResult(
                success = false,
                message = "Initialization failed: ${e.message}",
                adapterType = AdapterType.BLUETOOTH_CLASSIC
            )
        }
    }
    
    private suspend fun establishConnection(): InitializationResult {
        return withTimeout(INIT_TIMEOUT) {
            try {
                Log.d(TAG, "Connecting to device: ${targetDevice.name} (${targetDevice.address})")
                
                bluetoothSocket = targetDevice.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()
                
                if (bluetoothSocket?.isConnected == true) {
                    Log.i(TAG, "Bluetooth connection established")
                    InitializationResult(
                        success = true,
                        message = "Bluetooth connection established",
                        adapterType = AdapterType.BLUETOOTH_CLASSIC
                    )
                } else {
                    throw IOException("Failed to establish connection")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Bluetooth connection failed: ${e.message}")
                InitializationResult(
                    success = false,
                    message = "Connection failed: ${e.message}",
                    adapterType = AdapterType.BLUETOOTH_CLASSIC
                )
            }
        }
    }
    
    private suspend fun initializeElm327(): InitializationResult {
        val initCommands = listOf(
            "ATZ",        // Reset
            "ATE0",       // Echo off
            "ATL0",       // Linefeeds off
            "ATS0",       // Spaces off
            "ATH0",       // Headers off
            "ATSP0",      // Auto protocol selection
            "0100"        // Test OBD availability
        )
        
        try {
            for ((index, command) in initCommands.withIndex()) {
                Log.d(TAG, "Sending init command ${index + 1}/${initCommands.size}: $command")
                
                val response = sendCommandAndWaitForResponse(command)
                
                when (command) {
                    "ATZ" -> {
                        if (!response.contains("ELM327")) {
                            return InitializationResult(
                                success = false,
                                message = "ELM327 identification failed",
                                adapterType = AdapterType.BLUETOOTH_CLASSIC
                            )
                        }
                    }
                    "0100" -> {
                        if (!response.startsWith("41 00")) {
                            return InitializationResult(
                                success = false,
                                message = "OBD protocol not supported by vehicle",
                                adapterType = AdapterType.BLUETOOTH_CLASSIC
                            )
                        }
                    }
                }
                
                delay(200) // Brief delay between commands
            }
            
            return InitializationResult(
                success = true,
                message = "ELM327 initialized",
                adapterType = AdapterType.BLUETOOTH_CLASSIC
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "ELM327 initialization failed: ${e.message}")
            return InitializationResult(
                success = false,
                message = "ELM327 init failed: ${e.message}",
                adapterType = AdapterType.BLUETOOTH_CLASSIC
            )
        }
    }
    
    private suspend fun detectObdProtocol(): InitializationResult {
        try {
            val protocolResponse = sendCommandAndWaitForResponse("ATDPN")
            val protocol = when {
                protocolResponse.contains("6") -> ObdProtocol.CAN_11BIT_500K
                protocolResponse.contains("7") -> ObdProtocol.CAN_29BIT_500K
                protocolResponse.contains("8") -> ObdProtocol.CAN_11BIT_250K
                protocolResponse.contains("9") -> ObdProtocol.CAN_29BIT_250K
                protocolResponse.contains("3") -> ObdProtocol.ISO_9141_2
                protocolResponse.contains("4") -> ObdProtocol.KWP_2000_SLOW
                protocolResponse.contains("5") -> ObdProtocol.KWP_2000_FAST
                else -> ObdProtocol.CAN_11BIT_500K // Default
            }
            
            Log.i(TAG, "Detected OBD protocol: $protocol")
            return InitializationResult(
                success = true,
                message = "Protocol detected: $protocol",
                adapterType = AdapterType.BLUETOOTH_CLASSIC,
                supportedProtocols = listOf(protocol)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Protocol detection failed: ${e.message}")
            return InitializationResult(
                success = false,
                message = "Protocol detection failed: ${e.message}",
                adapterType = AdapterType.BLUETOOTH_CLASSIC
            )
        }
    }
    
    private suspend fun verifyConnection(): InitializationResult {
        try {
            // Test with VIN request
            val vinResponse = sendCommandAndWaitForResponse("0902")
            
            if (vinResponse.startsWith("49 02") || vinResponse.contains("41")) {
                Log.i(TAG, "OBD connection verified successfully")
                return InitializationResult(
                    success = true,
                    message = "OBD connection verified",
                    adapterType = AdapterType.BLUETOOTH_CLASSIC
                )
            } else {
                return InitializationResult(
                    success = false,
                    message = "OBD verification failed",
                    adapterType = AdapterType.BLUETOOTH_CLASSIC
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Connection verification failed: ${e.message}")
            return InitializationResult(
                success = false,
                message = "Verification failed: ${e.message}",
                adapterType = AdapterType.BLUETOOTH_CLASSIC
            )
        }
    }
    
    private suspend fun sendCommandAndWaitForResponse(
        command: String,
        timeout: Long = 3000L
    ): String {
        return withTimeout(timeout) {
            val socket = bluetoothSocket ?: throw IOException("Socket not connected")
            val outputStream = socket.outputStream
            val inputStream = socket.inputStream
            
            // Send command
            val commandBytes = (command + "\r").toByteArray()
            outputStream.write(commandBytes)
            outputStream.flush()
            
            // Wait for response
            val buffer = ByteArray(1024)
            val response = StringBuilder()
            var totalBytesRead = 0
            
            while (totalBytesRead < buffer.size) {
                if (inputStream.available() > 0) {
                    val bytesRead = inputStream.read(buffer, totalBytesRead, buffer.size - totalBytesRead)
                    if (bytesRead > 0) {
                        val chunk = String(buffer, totalBytesRead, bytesRead)
                        response.append(chunk)
                        totalBytesRead += bytesRead
                        
                        if (response.contains(">")) {
                            break // ELM327 prompt indicates end of response
                        }
                    }
                } else {
                    delay(50) // Brief delay before checking again
                }
            }
            
            response.toString().trim().replace(">", "")
        }
    }
    
    override suspend fun cleanup() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
            Log.i(TAG, "ELM327 Bluetooth Classic adapter cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error: ${e.message}")
        }
    }
    
    override fun getAdapterInfo(): AdapterInfo {
        return AdapterInfo(
            name = "ELM327 Bluetooth Classic",
            type = AdapterType.BLUETOOTH_CLASSIC,
            version = "1.5",
            capabilities = setOf(
                AdapterCapability.AUTO_PROTOCOL_DETECTION,
                AdapterCapability.REAL_TIME_DATA,
                AdapterCapability.DTC_MANAGEMENT,
                AdapterCapability.VEHICLE_IDENTIFICATION
            )
        )
    }
}

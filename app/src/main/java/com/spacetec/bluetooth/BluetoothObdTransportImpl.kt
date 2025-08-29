package com.spacetec.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.spacetec.connection.transport.ObdTransport
import com.spacetec.obd.ObdProtocol
import com.spacetec.bluetooth.ObdResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * Real Bluetooth OBD-II adapter transport implementation
 * Supports ELM327 and compatible adapters
 */
@SuppressLint("MissingPermission")
class BluetoothObdTransportImpl(
    private val device: BluetoothDevice,
    private val bluetoothAdapter: BluetoothAdapter
) : ObdTransport {
    
    companion object {
        private const val TAG = "BluetoothObdTransport"
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val CONNECTION_TIMEOUT = 10000L
        private const val READ_TIMEOUT = 5000L
        private const val COMMAND_DELAY = 100L
        private const val MAX_RETRIES = 3
    }
    
    override val config = ObdTransport.TransportConfig(
        type = ObdTransport.TransportType.BLUETOOTH_CLASSIC,
        address = device.address,
        name = device.name
    )
    
    private val _connectionState = MutableStateFlow(ObdTransport.ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ObdTransport.ConnectionState> = _connectionState.asStateFlow()
    
    private val _dataStream = MutableSharedFlow<ByteArray>()
    private val _errorStream = MutableSharedFlow<ObdTransport.ConnectionError>()
    
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isInitialized = false
    private var lastActivity = System.currentTimeMillis()
    
    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Attempting to connect to OBD device: ${device.name} (${device.address})")
            _connectionState.value = ObdTransport.ConnectionState.CONNECTING
            
            // Cancel discovery to improve connection reliability
            try {
                if (bluetoothAdapter.isDiscovering) {
                    bluetoothAdapter.cancelDiscovery()
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Permission issue canceling discovery", e)
            }
            
            var connectionSuccess = false
            var lastException: Exception? = null
            
            // Try multiple connection methods
            val connectionMethods = listOf(
                { device.createRfcommSocketToServiceRecord(SPP_UUID) },
                { device.createInsecureRfcommSocketToServiceRecord(SPP_UUID) },
                { 
                    // Fallback reflection method for problematic devices
                    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    method.invoke(device, 1) as BluetoothSocket
                }
            )
            
            for ((index, createSocket) in connectionMethods.withIndex()) {
                try {
                    Log.d(TAG, "Trying connection method ${index + 1}")
                    
                    // Create socket
                    socket = createSocket()
                    
                    // Connect with timeout
                    val connectResult = withTimeoutOrNull(CONNECTION_TIMEOUT) {
                        socket?.connect()
                        true
                    }
                    
                    if (connectResult == true && socket?.isConnected == true) {
                        connectionSuccess = true
                        break
                    } else {
                        socket?.close()
                        socket = null
                    }
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Connection method ${index + 1} failed", e)
                    lastException = e
                    socket?.close()
                    socket = null
                    
                    if (index < connectionMethods.size - 1) {
                        kotlinx.coroutines.delay(500) // Brief delay between attempts
                    }
                }
            }
            
            if (!connectionSuccess) {
                Log.e(TAG, "All connection methods failed")
                cleanup()
                _connectionState.value = ObdTransport.ConnectionState.ERROR
                return@withContext Result.failure(lastException ?: IOException("Connection failed"))
            }
            
            // Get streams
            inputStream = socket?.inputStream
            outputStream = socket?.outputStream
            
            if (inputStream == null || outputStream == null) {
                Log.e(TAG, "Failed to get I/O streams")
                cleanup()
                _connectionState.value = ObdTransport.ConnectionState.ERROR
                return@withContext Result.failure(IOException("Failed to get I/O streams"))
            }
            
            Log.i(TAG, "Successfully connected to OBD device")
            
            // Initialize ELM327 with retries
            var initSuccess = false
            for (attempt in 1..3) {
                if (initializeElm327()) {
                    initSuccess = true
                    break
                }
                if (attempt < 3) {
                    Log.w(TAG, "ELM327 init attempt $attempt failed, retrying...")
                    kotlinx.coroutines.delay(1000)
                }
            }
            
            if (initSuccess) {
                isInitialized = true
                _connectionState.value = ObdTransport.ConnectionState.CONNECTED
                Log.i(TAG, "ELM327 initialized successfully")
                Result.success(Unit)
            } else {
                Log.e(TAG, "Failed to initialize ELM327 after 3 attempts")
                cleanup()
                _connectionState.value = ObdTransport.ConnectionState.ERROR
                Result.failure(IOException("Failed to initialize ELM327"))
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Connection failed", e)
            cleanup()
            _connectionState.value = ObdTransport.ConnectionState.ERROR
            Result.failure(e)
        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth permission denied", e)
            cleanup()
            _connectionState.value = ObdTransport.ConnectionState.ERROR
            Result.failure(e)
        }
    }
    
    override suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            cleanup()
            _connectionState.value = ObdTransport.ConnectionState.DISCONNECTED
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun reconnect(): Result<Unit> {
        disconnect()
        return connect()
    }
    
    override suspend fun sendCommand(command: ByteArray): Result<ByteArray> = withContext(Dispatchers.IO) {
        return@withContext try {
            val commandString = String(command)
            val response = sendObdCommand(commandString)
            if (response is ObdResponse.Success) {
                lastActivity = System.currentTimeMillis()
                _dataStream.emit(response.data.toByteArray())
                Result.success(response.data.toByteArray())
            } else {
                val error = ObdTransport.ConnectionError(
                    code = 1001,
                    message = (response as ObdResponse.Error).message,
                    isRecoverable = true
                )
                _errorStream.emit(error)
                Result.failure(IOException(error.message))
            }
        } catch (e: Exception) {
            val error = ObdTransport.ConnectionError(
                code = 1002,
                message = "Command failed: ${e.message}",
                cause = e,
                isRecoverable = true
            )
            _errorStream.emit(error)
            Result.failure(e)
        }
    }
    
    override suspend fun sendCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = sendObdCommand(command)
            if (response is ObdResponse.Success) {
                lastActivity = System.currentTimeMillis()
                _dataStream.emit(response.data.toByteArray())
                Result.success(response.data)
            } else {
                val error = ObdTransport.ConnectionError(
                    code = 1001,
                    message = (response as ObdResponse.Error).message,
                    isRecoverable = true
                )
                _errorStream.emit(error)
                Result.failure(IOException(error.message))
            }
        } catch (e: Exception) {
            val error = ObdTransport.ConnectionError(
                code = 1002,
                message = "Command failed: ${e.message}",
                cause = e,
                isRecoverable = true
            )
            _errorStream.emit(error)
            Result.failure(e)
        }
    }
    
    override fun getDataStream(): Flow<ByteArray> = _dataStream.asSharedFlow()
    
    override fun getErrorStream(): Flow<ObdTransport.ConnectionError> = _errorStream.asSharedFlow()
    
    override suspend fun ping(): Boolean {
        return try {
            val result = sendCommand("ATI")
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }
    
    override fun getConnectionQuality(): Float {
        return if (isConnected) {
            val timeSinceActivity = System.currentTimeMillis() - lastActivity
            when {
                timeSinceActivity < 1000 -> 1.0f
                timeSinceActivity < 5000 -> 0.8f
                timeSinceActivity < 10000 -> 0.6f
                timeSinceActivity < 30000 -> 0.4f
                else -> 0.2f
            }
        } else 0.0f
    }
    
    override fun getLastActivity(): Long = lastActivity
    
    override fun cleanup() {
        try {
            Log.d(TAG, "Closing OBD connection")
            
            inputStream?.close()
            outputStream?.close()
            socket?.close()
            
            inputStream = null
            outputStream = null
            socket = null
            
            isInitialized = false
            
            Log.i(TAG, "OBD connection closed")
            
        } catch (e: IOException) {
            Log.e(TAG, "Error closing connection", e)
        }
    }
    
    private suspend fun initializeElm327(): Boolean = withContext(Dispatchers.IO) {
        try {
            val initCommands = ObdProtocol.getInitCommands()
            
            for (command in initCommands) {
                Log.d(TAG, "Sending init command: $command")
                
                if (!sendRawCommand(command)) {
                    Log.e(TAG, "Failed to send init command: $command")
                    return@withContext false
                }
                
                val response = readResponse()
                Log.d(TAG, "Init response: $response")
                
                if (response == null || ObdProtocol.isErrorResponse(response)) {
                    Log.e(TAG, "Invalid response to init command $command: $response")
                    return@withContext false
                }
                
                kotlinx.coroutines.delay(COMMAND_DELAY)
            }
            
            // Test with a simple PID request
            if (sendRawCommand("0100")) {
                val testResponse = readResponse()
                if (testResponse != null && !ObdProtocol.isErrorResponse(testResponse)) {
                    Log.i(TAG, "ELM327 test successful: $testResponse")
                    return@withContext true
                }
            }
            
            Log.e(TAG, "ELM327 test failed")
            return@withContext false
            
        } catch (e: Exception) {
            Log.e(TAG, "ELM327 initialization failed", e)
            return@withContext false
        }
    }
    
    suspend fun sendObdCommand(command: String): ObdResponse = withContext(Dispatchers.IO) {
        try {
            writeCommand(command)
            kotlinx.coroutines.delay(COMMAND_DELAY)
            val response = readResponse()
            
            return@withContext when {
                response == null -> ObdResponse.Error("No response received")
                ObdProtocol.isErrorResponse(response) -> {
                    ObdResponse.Error("OBD error response: $response")
                }
                ObdProtocol.isValidResponse(response, command) -> {
                    ObdResponse.Success(response)
                }
                else -> {
                    ObdResponse.Error("Invalid response format: $response")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Command failed: $command", e)
            return@withContext ObdResponse.Error("Command failed: ${e.message}")
        }
    }
    
    private suspend fun writeCommand(command: String) = withContext(Dispatchers.IO) {
        if (!isConnected || !isInitialized) {
            throw IllegalStateException("OBD transport not connected or initialized")
        }
        
        var retries = 0
        while (retries < MAX_RETRIES) {
            try {
                if (sendRawCommand(command)) {
                    return@withContext
                }
                retries++
                kotlinx.coroutines.delay(COMMAND_DELAY)
            } catch (e: IOException) {
                Log.w(TAG, "Write attempt $retries failed", e)
                retries++
                if (retries >= MAX_RETRIES) {
                    throw e
                }
                kotlinx.coroutines.delay(COMMAND_DELAY * retries)
            }
        }
        
        throw IOException("Failed to write command after $MAX_RETRIES attempts")
    }
    
    private suspend fun sendRawCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val commandBytes = "$command\r".toByteArray()
            outputStream?.write(commandBytes)
            outputStream?.flush()
            Log.d(TAG, "Sent command: $command")
            return@withContext true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to send command: $command", e)
            return@withContext false
        }
    }
    
    private suspend fun readResponse(): String? = withContext(Dispatchers.IO) {
        try {
            val response = ObdProtocol.readResponseWithTimeout(inputStream, READ_TIMEOUT)
            Log.d(TAG, "Received response: $response")
            return@withContext response
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read response", e)
            return@withContext null
        }
    }
    
    /**
     * Get supported PIDs from vehicle
     */
    suspend fun getSupportedPids(): Set<String> = withContext(Dispatchers.IO) {
        val supportedPids = mutableSetOf<String>()
        
        try {
            // Check PIDs 01-20
            val response1 = sendObdCommand("0100")
            if (response1 is ObdResponse.Success) {
                parseSupportedPids(response1.data, 1, supportedPids)
            }
            
            // Check PIDs 21-40
            val response2 = sendObdCommand("0120")
            if (response2 is ObdResponse.Success) {
                parseSupportedPids(response2.data, 21, supportedPids)
            }
            
            // Check PIDs 41-60
            val response3 = sendObdCommand("0140")
            if (response3 is ObdResponse.Success) {
                parseSupportedPids(response3.data, 41, supportedPids)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get supported PIDs", e)
        }
        
        return@withContext supportedPids
    }
    
    private fun parseSupportedPids(response: String, startPid: Int, supportedPids: MutableSet<String>) {
        try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.length >= 10) {
                val dataHex = cleaned.substring(4, 12) // Extract 4 bytes of PID support data
                val pidSupport = dataHex.toLong(16)
                
                for (i in 0..31) {
                    if ((pidSupport and (1L shl (31 - i))) != 0L) {
                        val pidNumber = startPid + i
                        supportedPids.add(String.format("%02X", pidNumber))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse supported PIDs", e)
        }
    }
    
    /**
     * Get vehicle VIN
     */
    suspend fun getVehicleVin(): String? = withContext(Dispatchers.IO) {
        try {
            val vinResponses = mutableListOf<String>()
            
            // Request VIN (may come in multiple frames)
            val response = sendObdCommand("0902")
            if (response is ObdResponse.Success) {
                vinResponses.add(response.data)
                
                // Check for additional frames
                for (i in 1..5) {
                    kotlinx.coroutines.delay(100)
                    try {
                        val additionalResponse = readResponse()
                        if (additionalResponse != null && !ObdProtocol.isErrorResponse(additionalResponse)) {
                            vinResponses.add(additionalResponse)
                        } else {
                            break
                        }
                    } catch (e: Exception) {
                        break
                    }
                }
            }
            
            return@withContext ObdProtocol.parseVin(vinResponses)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get VIN", e)
            return@withContext null
        }
    }
    
    /**
     * Get device information
     */
    fun getDeviceInfo(): String = "${device.name} (${device.address})"
}


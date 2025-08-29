package com.spacetec.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Manages Bluetooth communication with OBD-II adapters
 * Provides space-themed interface for vehicle diagnostics
 */
class SpaceBluetoothManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SpaceBluetoothManager"
        private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        private const val COMMAND_TIMEOUT = 3000L
        private const val CONNECTION_TIMEOUT = 10000L
    }
    
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false
    
    // Device discovery properties
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _statusMessage = MutableStateFlow("🛰️ Ready for spacecraft connection")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()
    
    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR, SCANNING
    }
    
    init {
        initializeBluetooth()
    }
    
    @SuppressLint("MissingPermission")
    private fun initializeBluetooth() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        
        if (bluetoothAdapter == null) {
            Log.e(TAG, "🚨 Bluetooth not supported on this spacecraft")
        } else if (!bluetoothAdapter!!.isEnabled) {
            Log.w(TAG, "🔋 Bluetooth communications offline")
        }
    }
    
    /**
     * Check if Bluetooth is available and enabled
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * Get list of paired OBD devices
     */
    @SuppressLint("MissingPermission")
    fun getPairedObdDevices(): List<BluetoothDevice> {
        if (!isBluetoothEnabled()) return emptyList()
        
        return bluetoothAdapter?.bondedDevices?.filter { device ->
            device.name?.contains("OBD", ignoreCase = true) == true ||
            device.name?.contains("ELM", ignoreCase = true) == true ||
            device.name?.contains("OBDII", ignoreCase = true) == true
        } ?: emptyList()
    }
    
    /**
     * Connect to OBD device
     */
    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "🛰️ Establishing connection to ${device.name}")
            
            // Close any existing connection
            disconnect()
            
            // Create socket
            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID))
            
            // Connect with timeout
            val connectJob = launch {
                bluetoothSocket?.connect()
            }
            
            val timeoutJob = launch {
                delay(CONNECTION_TIMEOUT)
                if (connectJob.isActive) {
                    connectJob.cancel()
                    throw Exception("Connection timeout")
                }
            }
            
            try {
                connectJob.join()
                timeoutJob.cancel()
            } catch (e: Exception) {
                timeoutJob.cancel()
                throw e
            }
            
            // Setup streams
            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream
            
            isConnected = true
            Log.i(TAG, "✅ Connected to spacecraft systems via ${device.name}")
            
            // Initialize OBD protocol
            delay(500)
            if (initializeObdProtocol()) {
                Log.i(TAG, "🚀 OBD-II protocol initialized successfully")
                return@withContext true
            } else {
                disconnect()
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "🚨 Connection failed: ${e.message}", e)
            disconnect()
            return@withContext false
        }
    }
    
    /**
     * Initialize OBD-II protocol
     */
    private suspend fun initializeObdProtocol(): Boolean {
        try {
            // Reset adapter
            if (!sendCommand("ATZ")) return false
            delay(1000)
            
            // Turn off echo
            if (!sendCommand("ATE0")) return false
            delay(100)
            
            // Set line feeds off
            if (!sendCommand("ATL0")) return false
            delay(100)
            
            // Set spaces off
            if (!sendCommand("ATS0")) return false
            delay(100)
            
            // Set headers off
            if (!sendCommand("ATH0")) return false
            delay(100)
            
            // Auto protocol selection
            if (!sendCommand("ATSP0")) return false
            delay(500)
            
            Log.i(TAG, "🛸 OBD-II spacecraft interface ready")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "🚨 Failed to initialize OBD protocol: ${e.message}")
            return false
        }
    }
    
    /**
     * Send command without waiting for response
     */
    private fun sendCommand(command: String): Boolean {
        return sendData(command)
    }
    
    /**
     * Send command and wait for response
     */
    suspend fun sendCommandAndReadResponse(command: String): String? = withContext(Dispatchers.IO) {
        if (!isConnected) {
            Log.w(TAG, "📡 Cannot send command - no connection")
            return@withContext null
        }
        
        try {
            // Clear input buffer
            clearInputBuffer()
            
            // Send command
            val commandWithCR = "$command\r"
            outputStream?.write(commandWithCR.toByteArray())
            outputStream?.flush()
            
            Log.d(TAG, "📤 Sent: $command")
            
            // Read response with timeout
            val response = withTimeout(COMMAND_TIMEOUT) {
                readResponse()
            }
            
            Log.d(TAG, "📥 Received: $response")
            return@withContext response?.trim()
            
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "⏰ Command timeout: $command")
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "🚨 Command failed: $command - ${e.message}")
            return@withContext null
        }
    }
    
    /**
     * Send command without expecting response
     */
    fun sendData(command: String): Boolean {
        if (!isConnected) return false
        
        return try {
            val commandWithCR = "$command\r"
            outputStream?.write(commandWithCR.toByteArray())
            outputStream?.flush()
            true
        } catch (e: Exception) {
            Log.e(TAG, "🚨 Failed to send data: ${e.message}")
            false
        }
    }
    
    /**
     * Read response from OBD adapter
     */
    private suspend fun readResponse(): String? {
        val buffer = ByteArray(1024)
        val response = StringBuilder()
        
        while (true) {
            if (inputStream?.available() ?: 0 > 0) {
                val bytesRead = inputStream?.read(buffer) ?: 0
                if (bytesRead > 0) {
                    val data = String(buffer, 0, bytesRead)
                    response.append(data)
                    
                    // Check if we have a complete response
                    val responseStr = response.toString()
                    if (responseStr.contains(">") || responseStr.contains("OK") || 
                        responseStr.contains("ERROR") || responseStr.contains("NO DATA") ||
                        responseStr.contains("UNABLE TO CONNECT")) {
                        break
                    }
                }
            }
            delay(50) // Small delay to prevent busy waiting
        }
        
        return response.toString().replace(">", "").trim()
    }
    
    /**
     * Clear input buffer
     */
    private fun clearInputBuffer() {
        try {
            while ((inputStream?.available() ?: 0) > 0) {
                inputStream?.read()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear input buffer: ${e.message}")
        }
    }
    
    /**
     * Check if connected to OBD device
     */
    fun isConnected(): Boolean {
        return isConnected && bluetoothSocket?.isConnected == true
    }
    
    /**
     * Disconnect from OBD device
     */
    fun disconnect() {
        try {
            isConnected = false
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
            
            inputStream = null
            outputStream = null
            bluetoothSocket = null
            
            Log.i(TAG, "🛸 Disconnected from spacecraft systems")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect: ${e.message}")
        }
    }
    
    /**
     * Get connection status message
     */
    fun getConnectionStatus(): String {
        return when {
            !isBluetoothEnabled() -> "🔋 Bluetooth communications offline"
            !isConnected() -> "📡 No spacecraft connection"
            else -> "🚀 Connected to spacecraft systems"
        }
    }
    
    /**
     * Start device discovery
     */
    @SuppressLint("MissingPermission")
    fun startDeviceDiscovery() {
        if (!isBluetoothEnabled()) {
            Log.w(TAG, "🔋 Bluetooth not enabled for discovery")
            return
        }
        
        _isScanning.value = true
        _connectionState.value = ConnectionState.SCANNING
        _statusMessage.value = "🔍 Scanning for spacecraft adapters..."
        
        // Get paired devices first
        val pairedDevices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        _discoveredDevices.value = pairedDevices
        
        Log.i(TAG, "🛸 Found ${pairedDevices.size} paired spacecraft adapters")
        
        // In a full implementation, you would also start Bluetooth discovery here
        // bluetoothAdapter?.startDiscovery()
    }
    
    /**
     * Stop device discovery
     */
    @SuppressLint("MissingPermission")
    fun stopDeviceDiscovery() {
        _isScanning.value = false
        _connectionState.value = ConnectionState.DISCONNECTED
        _statusMessage.value = "🛰️ Scan complete"
        
        // Stop discovery if it was started
        bluetoothAdapter?.cancelDiscovery()
        
        Log.i(TAG, "🛸 Device discovery stopped")
    }
    
    /**
     * Check if has Bluetooth permissions
     */
    fun hasBluetoothPermissions(): Boolean {
        return isBluetoothEnabled() // Simplified check
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopDeviceDiscovery()
        disconnect()
        Log.i(TAG, "🛸 Cleanup completed")
    }
}

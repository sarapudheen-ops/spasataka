package com.spacetec.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.spacetec.bluetooth.BluetoothObdTransportImpl

/**
 * Manages Bluetooth OBD adapter discovery and connection
 */
@SuppressLint("MissingPermission")
class BluetoothObdManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BluetoothObdManager"
        
        // Common OBD adapter names/prefixes
        private val OBD_DEVICE_NAMES = setOf(
            "OBDII", "OBD2", "ELM327", "ELM", "VGATE", "VEEPEAK", 
            "BAFX", "FOSEAL", "KONNWEI", "LAUNCH", "AUTEL", "INNOVA"
        )
    }
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    
    private val _discoveredDevices = MutableStateFlow<List<ObdDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<ObdDevice>> = _discoveredDevices.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private var currentTransport: BluetoothObdTransportImpl? = null
    private var discoveryReceiver: BroadcastReceiver? = null
    
    /**
     * Check if Bluetooth is available and enabled
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }
    
    /**
     * Start scanning for OBD devices
     */
    suspend fun startScanning(): Boolean = withContext(Dispatchers.Main) {
        if (!isBluetoothAvailable()) {
            Log.e(TAG, "Bluetooth not available or not enabled")
            return@withContext false
        }
        
        if (_isScanning.value) {
            Log.w(TAG, "Already scanning")
            return@withContext true
        }
        
        try {
            // Clear previous results
            _discoveredDevices.value = emptyList()
            
            // Get paired devices first
            val pairedDevices = getPairedObdDevices()
            _discoveredDevices.value = pairedDevices
            
            // Register discovery receiver
            registerDiscoveryReceiver()
            
            // Start discovery
            val discoveryStarted = bluetoothAdapter.startDiscovery()
            if (discoveryStarted) {
                _isScanning.value = true
                Log.i(TAG, "Started Bluetooth discovery")
                return@withContext true
            } else {
                Log.e(TAG, "Failed to start Bluetooth discovery")
                unregisterDiscoveryReceiver()
                return@withContext false
            }
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth permission denied", e)
            return@withContext false
        }
    }
    
    /**
     * Stop scanning for devices
     */
    fun stopScanning() {
        try {
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            unregisterDiscoveryReceiver()
            _isScanning.value = false
            Log.i(TAG, "Stopped Bluetooth discovery")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to stop discovery", e)
        }
    }
    
    /**
     * Get paired OBD devices
     */
    private fun getPairedObdDevices(): List<ObdDevice> {
        return try {
            bluetoothAdapter.bondedDevices
                ?.filter { isObdDevice(it) }
                ?.map { ObdDevice(it, isPaired = true) }
                ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied getting paired devices", e)
            emptyList()
        }
    }
    
    /**
     * Check if device is likely an OBD adapter
     */
    private fun isObdDevice(device: BluetoothDevice): Boolean {
        val name = device.name?.uppercase() ?: return false
        return OBD_DEVICE_NAMES.any { name.contains(it) } ||
               name.contains("DIAGNOSTIC") ||
               name.contains("SCANNER")
    }
    
    /**
     * Register broadcast receiver for device discovery
     */
    private fun registerDiscoveryReceiver() {
        discoveryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        device?.let { handleDeviceFound(it) }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        _isScanning.value = false
                        Log.i(TAG, "Bluetooth discovery finished")
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        
        context.registerReceiver(discoveryReceiver, filter)
    }
    
    /**
     * Unregister discovery receiver
     */
    private fun unregisterDiscoveryReceiver() {
        discoveryReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                // Receiver not registered
            }
            discoveryReceiver = null
        }
    }
    
    /**
     * Handle discovered device
     */
    private fun handleDeviceFound(device: BluetoothDevice) {
        if (isObdDevice(device)) {
            val currentDevices = _discoveredDevices.value.toMutableList()
            val existingDevice = currentDevices.find { it.device.address == device.address }
            
            if (existingDevice == null) {
                val obdDevice = ObdDevice(device, isPaired = device.bondState == BluetoothDevice.BOND_BONDED)
                currentDevices.add(obdDevice)
                _discoveredDevices.value = currentDevices
                Log.i(TAG, "Found OBD device: ${device.name} (${device.address})")
            }
        }
    }
    
    /**
     * Connect to OBD device
     */
    suspend fun connectToDevice(obdDevice: ObdDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.Connecting
            
            // Disconnect current transport if any
            currentTransport?.cleanup()
            
            // Create new transport
            val transport = BluetoothObdTransportImpl(obdDevice.device, bluetoothAdapter)
            
            // Attempt connection
            val connectResult = transport.connect()
            val connected = connectResult.isSuccess
            
            if (connected) {
                currentTransport = transport
                _connectionState.value = ConnectionState.Connected(transport)
                Log.i(TAG, "Successfully connected to ${obdDevice.device.name}")
                return@withContext true
            } else {
                _connectionState.value = ConnectionState.Error("Failed to connect to ${obdDevice.device.name}")
                Log.e(TAG, "Failed to connect to ${obdDevice.device.name}")
                return@withContext false
            }
            
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Connection error: ${e.message}")
            Log.e(TAG, "Connection error", e)
            return@withContext false
        }
    }
    
    /**
     * Disconnect from current device
     */
    fun disconnect() {
        currentTransport?.cleanup()
        currentTransport = null
        _connectionState.value = ConnectionState.Disconnected
        Log.i(TAG, "Disconnected from OBD device")
    }
    
    /**
     * Get current transport if connected
     */
    fun getCurrentTransport(): BluetoothObdTransportImpl? = currentTransport
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        stopScanning()
        disconnect()
    }
}

/**
 * OBD device wrapper
 */
data class ObdDevice(
    val device: BluetoothDevice,
    val isPaired: Boolean,
    val rssi: Int? = null
) {
    val name: String get() = device.name ?: "Unknown Device"
    val address: String get() = device.address
    
    override fun toString(): String = "$name ($address)"
}

/**
 * Connection state
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val transport: BluetoothObdTransportImpl) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

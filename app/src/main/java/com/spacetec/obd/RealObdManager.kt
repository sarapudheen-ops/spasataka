package com.spacetec.obd

import android.content.Context
import android.util.Log
import com.spacetec.bluetooth.BluetoothObdManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.spacetec.connection.transport.ObdTransport
import com.spacetec.bluetooth.ConnectionState
import com.spacetec.bluetooth.ObdDevice
import com.spacetec.bluetooth.ObdResponse
import com.spacetec.vehicle.ManufacturerAdaptations
import com.spacetec.vehicle.VehicleData
import com.spacetec.vin.VinDecoder
import com.spacetec.vin.VehicleInfo
import com.spacetec.obd.ObdProtocol
import com.spacetec.diagnostic.transport.J2534Transport
import com.spacetec.diagnostic.transport.ConnectionToDiagnosticAdapter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.random.Random

/**
 * Real OBD manager that communicates with actual hardware
 * Provides genuine vehicle diagnostics from OBD-II port
 */
@Singleton
class RealObdManager @Inject constructor(@ApplicationContext private val context: Context) {
    
    companion object {
        private const val TAG = "RealObdManager"
        private const val DATA_UPDATE_INTERVAL = 1000L
        private const val CONNECTION_RETRY_DELAY = 5000L
        private const val MAX_CONNECTION_RETRIES = 3
    }
    
    private val bluetoothManager = BluetoothObdManager(context)
    private var currentTransport: com.spacetec.diagnostic.transport.ObdTransport? = null
    private var manufacturerAdaptations: ManufacturerAdaptations? = null
    private var dataCollectionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // State flows
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    private val _vehicleData = MutableStateFlow(VehicleData())
    val vehicleData: StateFlow<VehicleData> = _vehicleData.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()
    
    private val _supportedPids = MutableStateFlow<Set<String>>(emptySet())
    val supportedPids: StateFlow<Set<String>> = _supportedPids.asStateFlow()
    
    private val _vehicleInfo = MutableStateFlow<VehicleInfo?>(null)
    val vehicleInfo: StateFlow<VehicleInfo?> = _vehicleInfo.asStateFlow()
    
    private val _dtcCodes = MutableStateFlow<List<String>>(emptyList())
    val dtcCodes: StateFlow<List<String>> = _dtcCodes.asStateFlow()
    
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()
    
    // Device discovery
    val discoveredDevices = bluetoothManager.discoveredDevices
    val isScanning = bluetoothManager.isScanning
    val connectionState = bluetoothManager.connectionState
    
    /**
     * Initialize the OBD manager
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Initializing Real OBD Manager")
            
            if (!bluetoothManager.isBluetoothAvailable()) {
                _lastError.value = "Bluetooth not available or not enabled"
                return@withContext false
            }
            
            _isInitialized.value = true
            Log.i(TAG, "Real OBD Manager initialized successfully")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize OBD manager", e)
            _lastError.value = "Initialization failed: ${e.message}"
            return@withContext false
        }
    }
    
    /**
     * Start scanning for OBD devices
     */
    suspend fun startDeviceScanning(): Boolean {
        return bluetoothManager.startScanning()
    }
    
    /**
     * Stop scanning for devices
     */
    fun stopDeviceScanning() {
        bluetoothManager.stopScanning()
    }
    
    /**
     * Connect to an OBD device (Bluetooth)
     */
    suspend fun connectToDevice(device: ObdDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Connecting to Bluetooth device: ${device.name}")
            _connectionStatus.value = "Connecting to ${device.name}..."
            
            val connected = bluetoothManager.connectToDevice(device)
            
            if (connected) {
                val connectionTransport = bluetoothManager.getCurrentTransport()
                connectionTransport?.let { transport ->
                    // Create diagnostic adapter for the connection transport
                    currentTransport = ConnectionToDiagnosticAdapter(transport)
                    
                    // Initialize manufacturer adaptations
                    manufacturerAdaptations = ManufacturerAdaptations(transport)
                    
                    // Initialize vehicle data and get VIN/PIDs
                    initializeVehicleData(transport)
                    
                    // Start continuous data collection
                    startDataCollection()
                    
                    _connectionStatus.value = "Connected to ${device.name}"
                    Log.i(TAG, "Successfully connected and initialized")
                    return@withContext true
                }
            }
            
            _connectionStatus.value = "Failed to connect to ${device.name}"
            _lastError.value = "Connection failed"
            return@withContext false
            
        } catch (e: Exception) {
            Log.e(TAG, "Connection error", e)
            _connectionStatus.value = "Connection error"
            _lastError.value = "Connection error: ${e.message}"
            return@withContext false
        }
    }
    
    /**
     * Connect to a J2534 device
     */
    suspend fun connectToJ2534Device(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Connecting to J2534 device")
            _connectionStatus.value = "Connecting to J2534 device..."
            
            // Create J2534 adapter and convert to diagnostic transport
            val j2534Adapter = com.spacetec.diagnostic.core.J2534Adapter()
            val connected = j2534Adapter.connect()
            
            if (connected) {
                currentTransport = com.spacetec.diagnostic.transport.DiagnosticAdapterToObdTransport(j2534Adapter)
                
                // For J2534, we don't have manufacturer adaptations yet
                manufacturerAdaptations = null
                
                // Initialize vehicle data
                initializeVehicleDataForJ2534(null)
                
                // Start continuous data collection
                startDataCollection()
                
                _connectionStatus.value = "Connected to J2534 device"
                Log.i(TAG, "Successfully connected to J2534 device")
                return@withContext true
            }
            
            _connectionStatus.value = "Failed to connect to J2534 device"
            _lastError.value = "Connection failed"
            return@withContext false
            
        } catch (e: Exception) {
            Log.e(TAG, "J2534 Connection error", e)
            _connectionStatus.value = "Connection error"
            _lastError.value = "Connection error: ${e.message}"
            return@withContext false
        }
    }
    
    /**
     * Initialize vehicle data after connection (Bluetooth)
     */
    private suspend fun initializeVehicleData(transport: com.spacetec.connection.transport.ObdTransport) {
        try {
            Log.i(TAG, "Initializing vehicle data...")
            
            // For now, we'll use the manufacturer adaptations approach for Bluetooth
            // In a real implementation, we would need to handle this differently for different transports
            
            // Get initial DTC codes
            readDtcCodes()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize vehicle data", e)
            _lastError.value = "Vehicle initialization failed: ${e.message}"
        }
    }
    
    /**
     * Initialize vehicle data after J2534 connection
     */
    private suspend fun initializeVehicleDataForJ2534(transport: J2534Transport) {
        try {
            Log.i(TAG, "Initializing vehicle data for J2534...")
            
            // For J2534, we'll implement basic initialization
            // In a real implementation, this would be more comprehensive
            
            // Get initial DTC codes
            readDtcCodes()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize vehicle data for J2534", e)
            _lastError.value = "Vehicle initialization failed: ${e.message}"
        }
    }
    
    /**
     * Start continuous data collection
     */
    private fun startDataCollection() {
        dataCollectionJob?.cancel()
        dataCollectionJob = scope.launch {
            Log.i(TAG, "Starting data collection")
            
            while (isActive) {
                try {
                    // Check if transport is connected
                    val transport = currentTransport
                    if (transport == null || !transport.isConnected()) {
                        Log.w(TAG, "Transport disconnected, stopping data collection")
                        break
                    }
                    
                    collectVehicleData()
                    delay(DATA_UPDATE_INTERVAL)
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Data collection error", e)
                    _lastError.value = "Data collection error: ${e.message}"
                    delay(CONNECTION_RETRY_DELAY)
                }
            }
            
            Log.i(TAG, "Data collection stopped")
        }
    }
    
    /**
     * Collect real-time vehicle data
     */
    private suspend fun collectVehicleData() {
        val transport = currentTransport ?: return
        val currentData = _vehicleData.value
        
        try {
            // Collect basic OBD data using the current transport
            val newData = currentData.copy(
                speed = readSpeedFromTransport(transport),
                rpm = readRpmFromTransport(transport),
                coolantTemp = readCoolantTempFromTransport(transport),
                fuelLevel = readFuelLevelFromTransport(transport),
                isEngineRunning = readRpmFromTransport(transport) > 0,
                timestamp = System.currentTimeMillis()
            )
            
            _vehicleData.value = newData
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to collect some vehicle data", e)
        }
    }
    
    private suspend fun readSpeed(transport: com.spacetec.connection.transport.ObdTransport, adaptations: ManufacturerAdaptations?): Int {
        return try {
            // For now, we'll only support Bluetooth transport for these methods
            // In a real implementation, we would need to handle this differently for different transports
            0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read speed", e)
            0
        }
    }
    
    private suspend fun readSpeedFromTransport(transport: com.spacetec.diagnostic.transport.ObdTransport): Int {
        return try {
            val response = transport.readPid("010D") // Vehicle speed PID
            if (response != null) {
                response.toInt(16) / 256 // Convert from km/h raw value
            } else {
                0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read speed from transport", e)
            0
        }
    }
    
    private suspend fun readRpmFromTransport(transport: com.spacetec.diagnostic.transport.ObdTransport): Int {
        return try {
            val response = transport.readPid("010C") // Engine RPM PID
            if (response != null && response.length >= 4) {
                val highByte = response.substring(0, 2).toInt(16)
                val lowByte = response.substring(2, 4).toInt(16)
                ((highByte * 256) + lowByte) / 4 // Convert from raw value
            } else {
                0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read RPM from transport", e)
            0
        }
    }
    
    private suspend fun readCoolantTempFromTransport(transport: com.spacetec.diagnostic.transport.ObdTransport): Int {
        return try {
            val response = transport.readPid("0105") // Coolant temperature PID
            if (response != null) {
                response.toInt(16) - 40 // Convert from raw value to Celsius
            } else {
                0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read coolant temperature from transport", e)
            0
        }
    }
    
    private suspend fun readFuelLevelFromTransport(transport: com.spacetec.diagnostic.transport.ObdTransport): Int {
        return try {
            val response = transport.readPid("012F") // Fuel level PID
            if (response != null) {
                (response.toInt(16) * 100) / 255 // Convert from raw value to percentage
            } else {
                0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read fuel level from transport", e)
            0
        }
    }
    
    private suspend fun readRpm(transport: com.spacetec.connection.transport.ObdTransport, adaptations: ManufacturerAdaptations?): Int {
        return try {
            // For now, we'll only support Bluetooth transport for these methods
            // In a real implementation, we would need to handle this differently for different transports
            0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read RPM", e)
            0
        }
    }
    
    private suspend fun readCoolantTemp(transport: com.spacetec.connection.transport.ObdTransport, adaptations: ManufacturerAdaptations?): Int {
        return try {
            // For now, we'll only support Bluetooth transport for these methods
            // In a real implementation, we would need to handle this differently for different transports
            0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read coolant temperature", e)
            0
        }
    }
    
    private suspend fun readFuelLevel(transport: com.spacetec.connection.transport.ObdTransport, adaptations: ManufacturerAdaptations?): Int {
        return try {
            // For now, we'll only support Bluetooth transport for these methods
            // In a real implementation, we would need to handle this differently for different transports
            0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read fuel level", e)
            0
        }
    }
    
    private suspend fun readVoltage(transport: com.spacetec.connection.transport.ObdTransport, adaptations: ManufacturerAdaptations?): Double {
        return try {
            // For now, we'll only support Bluetooth transport for these methods
            // In a real implementation, we would need to handle this differently for different transports
            12.6
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read voltage", e)
            12.6
        }
    }
    
    private suspend fun readEngineLoad(transport: com.spacetec.connection.transport.ObdTransport, adaptations: ManufacturerAdaptations?): Int {
        return try {
            // For now, we'll only support Bluetooth transport for these methods
            // In a real implementation, we would need to handle this differently for different transports
            0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read engine load", e)
            0
        }
    }
    
    private suspend fun readThrottlePosition(transport: com.spacetec.connection.transport.ObdTransport, adaptations: ManufacturerAdaptations?): Int {
        return try {
            // For now, we'll only support Bluetooth transport for these methods
            // In a real implementation, we would need to handle this differently for different transports
            0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read throttle position", e)
            0
        }
    }
    
    /**
     * Read diagnostic trouble codes
     */
    suspend fun readDtcCodes(): List<String> = withContext(Dispatchers.IO) {
        try {
            val transport = currentTransport ?: return@withContext emptyList()
            
            // Use the diagnostic transport to read DTCs
            val dtcs = transport.readDtcs()
            
            _dtcCodes.value = dtcs
            return@withContext dtcs
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read DTC codes", e)
            _lastError.value = "DTC read failed: ${e.message}"
            return@withContext emptyList()
        }
    }
    
    private fun parseDtcCodes(response: String): List<String> {
        val dtcs = mutableListOf<String>()
        try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.startsWith("43") && cleaned.length >= 4) {
                val numCodes = cleaned.substring(2, 4).toInt(16)
                if (numCodes > 0 && cleaned.length >= 4 + (numCodes * 4)) {
                    for (i in 0 until numCodes) {
                        val startIndex = 4 + (i * 4)
                        val dtcHex = cleaned.substring(startIndex, startIndex + 4)
                        val dtcCode = convertHexToDtc(dtcHex)
                        if (dtcCode.isNotEmpty()) {
                            dtcs.add(dtcCode)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse DTC codes", e)
        }
        return dtcs
    }
    
    private fun convertHexToDtc(hex: String): String {
        try {
            val value = hex.toInt(16)
            val firstDigit = (value shr 14) and 0x03
            val secondDigit = (value shr 12) and 0x03
            val thirdDigit = (value shr 8) and 0x0F
            val fourthFifthDigit = value and 0xFF
            
            val prefix = when (firstDigit) {
                0 -> "P0"
                1 -> "P1"
                2 -> "P2"
                3 -> "P3"
                else -> "P"
            }
            
            return "$prefix${secondDigit}${String.format("%02X", thirdDigit)}${String.format("%02X", fourthFifthDigit)}"
        } catch (e: Exception) {
            return ""
        }
    }
    
    suspend fun connect(deviceAddress: String): Boolean {
        return try {
            // Create a mock Bluetooth device for connection
            val mockDevice = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                ?.getRemoteDevice(deviceAddress)
            if (mockDevice != null) {
                val device = ObdDevice(mockDevice, true, null)
                connectToDevice(device)
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to device: $deviceAddress", e)
            false
        }
    }
    
    /**
     * Read freeze frame data for a specific DTC
     */
    suspend fun readFreezeFrameData(dtc: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val transport = currentTransport ?: return@withContext null
            Log.i(TAG, "Reading freeze frame data for DTC: $dtc")
            
            // Send freeze frame data request (Mode 02)
            val response = transport.sendObdCommand("02${dtc.substring(1)}")
            when (response) {
                is ObdResponse.Success -> {
                    Log.i(TAG, "Freeze frame data: ${response.data}")
                    response.data
                }
                else -> {
                    Log.w(TAG, "Failed to read freeze frame data")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading freeze frame data", e)
            null
        }
    }
    
    /**
     * Read vehicle information
     */
    suspend fun readVehicleInfo() {
        try {
            val transport = currentTransport ?: return
            Log.i(TAG, "Reading vehicle information")
            
            // Get VIN if not already available
            if (_vehicleInfo.value == null) {
                val vin = transport.readVin()
                if (vin != null) {
                    val vehicleInfo = VinDecoder.decodeVin(vin)
                    _vehicleInfo.value = vehicleInfo
                    Log.i(TAG, "Vehicle info updated: ${vehicleInfo?.manufacturer} ${vehicleInfo?.modelYear}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading vehicle info", e)
            _lastError.value = "Failed to read vehicle info: ${e.message}"
        }
    }
    
    /**
     * Disconnect from current device
     */
    fun disconnect() {
        dataCollectionJob?.cancel()
        bluetoothManager.disconnect()
        currentTransport = null
        manufacturerAdaptations = null
        _connectionStatus.value = "Disconnected"
        _vehicleData.value = VehicleData()
        _supportedPids.value = emptySet()
        _vehicleInfo.value = null
        _dtcCodes.value = emptyList()
        Log.i(TAG, "Disconnected from OBD device")
    }
    
    /**
     * Get current connection status
     */
    fun isConnected(): Boolean = currentTransport?.isConnected == true
    
    /**
     * Get current transport for actuator control
     */
    fun getCurrentTransport(): com.spacetec.diagnostic.transport.ObdTransport? {
        return currentTransport
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        runBlocking {
            currentTransport?.disconnect()
        }
        scope.cancel()
        bluetoothManager.cleanup()
    }
}

// Lightweight response wrapper for UDS/service calls used by feature modules
data class ObdServiceResponse(val data: String?)

// Public helper APIs to satisfy feature modules expecting direct command/service methods
@Suppress("unused")
fun RealObdManager.sendCommand(command: String): Boolean {
    return try {
        val transport = this.getCurrentTransport() ?: return false
        val resp = runBlocking { transport.sendObdCommand(command) }
        resp is com.spacetec.bluetooth.ObdResponse.Success
    } catch (_: Exception) { false }
}

@Suppress("unused")
suspend fun RealObdManager.sendService(service: Int, subFunction: Int? = null, data: ByteArray? = null): ObdServiceResponse {
    return try {
        val transport = this.getCurrentTransport() ?: return ObdServiceResponse(null)
        val cmd = buildString {
            append(String.format("%02X", service))
            subFunction?.let { append(String.format("%02X", it)) }
            data?.let { bytes -> append(bytes.joinToString("") { b -> "%02X".format(b) }) }
        }
        when (val resp = transport.sendObdCommand(cmd)) {
            is com.spacetec.bluetooth.ObdResponse.Success -> ObdServiceResponse(resp.data)
            else -> ObdServiceResponse(null)
        }
    } catch (_: Exception) { ObdServiceResponse(null) }
}

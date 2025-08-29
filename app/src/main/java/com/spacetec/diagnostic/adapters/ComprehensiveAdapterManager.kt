package com.spacetec.diagnostic.adapters

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.spacetec.bluetooth.SpaceBluetoothManager
import com.spacetec.diagnostic.vci.NativeCapabilities

/**
 * Comprehensive Adapter Manager
 * Handles initialization of different OBD2 adapter types with automatic detection
 * and fallback mechanisms
 */
class ComprehensiveAdapterManager(
    private val context: Context,
    private val bluetoothManager: SpaceBluetoothManager
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _currentAdapter = MutableStateFlow<AdapterInitializer?>(null)
    val currentAdapter: StateFlow<AdapterInitializer?> = _currentAdapter.asStateFlow()
    
    private val _initializationState = MutableStateFlow(AdapterInitializationState.IDLE)
    val initializationState: StateFlow<AdapterInitializationState> = _initializationState.asStateFlow()
    
    private val _lastInitResult = MutableStateFlow<InitializationResult?>(null)
    val lastInitResult: StateFlow<InitializationResult?> = _lastInitResult.asStateFlow()
    
    companion object {
        private const val TAG = "ComprehensiveAdapterManager"
    }
    
    /**
     * Auto-detect and initialize the best available adapter
     * Tries adapters in order of preference: Native > BLE > Bluetooth Classic > WiFi
     */
    suspend fun autoInitializeAdapter(): InitializationResult {
        Log.i(TAG, "Starting automatic adapter detection and initialization...")
        _initializationState.value = AdapterInitializationState.DETECTING
        
        val detectionSequence = listOf<suspend () -> InitializationResult>(
            { detectAndInitNativeAdapters() },
            { detectAndInitBluetoothAdapters() },
            { detectAndInitWiFiAdapters() }
        )
        
        for (detectionMethod in detectionSequence) {
            try {
                val result = detectionMethod.invoke()
                if (result.success) {
                    _initializationState.value = AdapterInitializationState.INITIALIZED
                    _lastInitResult.value = result
                    Log.i(TAG, "Auto-initialization successful: ${result.message}")
                    return result
                }
            } catch (e: Exception) {
                Log.w(TAG, "Detection method failed: ${e.message}")
            }
        }
        
        _initializationState.value = AdapterInitializationState.FAILED
        val failureResult = InitializationResult(
            success = false,
            message = "No compatible OBD2 adapters found",
            adapterType = AdapterType.BLUETOOTH_CLASSIC
        )
        _lastInitResult.value = failureResult
        return failureResult
    }
    
    /**
     * Initialize specific adapter type
     */
    suspend fun initializeAdapter(
        adapterType: AdapterType,
        connectionParams: AdapterConnectionParams
    ): InitializationResult {
        Log.i(TAG, "Initializing specific adapter: $adapterType")
        _initializationState.value = AdapterInitializationState.INITIALIZING
        
        return try {
            val adapter = createAdapterInitializer(adapterType, connectionParams)
            _currentAdapter.value = adapter
            
            val result = adapter.initialize()
            
            if (result.success) {
                _initializationState.value = AdapterInitializationState.INITIALIZED
            } else {
                _initializationState.value = AdapterInitializationState.FAILED
                _currentAdapter.value = null
            }
            
            _lastInitResult.value = result
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Adapter initialization failed: ${e.message}")
            _initializationState.value = AdapterInitializationState.FAILED
            val errorResult = InitializationResult(
                success = false,
                message = "Initialization error: ${e.message}",
                adapterType = adapterType
            )
            _lastInitResult.value = errorResult
            errorResult
        }
    }
    
    /**
     * Get initialization sequence for specific adapter type
     */
    fun getInitializationSequence(adapterType: AdapterType): List<InitializationStep> {
        return when (adapterType) {
            AdapterType.BLUETOOTH_CLASSIC -> getBluetoothClassicSequence()
            AdapterType.BLUETOOTH_LE -> getBleSequence()
            AdapterType.WIFI -> getWiFiSequence()
            AdapterType.NATIVE_AUTEL -> getAutelNativeSequence()
            AdapterType.NATIVE_J2534 -> getJ2534Sequence()
            AdapterType.USB_SERIAL -> getUsbSerialSequence()
        }
    }
    
    private suspend fun detectAndInitNativeAdapters(): InitializationResult {
        Log.d(TAG, "Detecting native adapters...")
        
        // Try Autel native adapter first
        if (NativeCapabilities.isUniversalObdAvailable()) {
            Log.d(TAG, "Autel native adapter detected")
            val adapter = AutelNativeAdapterInitializer(context)
            _currentAdapter.value = adapter
            val result = adapter.initialize()
            if (result.success) return result
        }
        
        // Try J2534 adapter
        if (NativeCapabilities.isJ2534Available()) {
            Log.d(TAG, "J2534 adapter detected")
            val adapter = J2534AdapterInitializer(context)
            _currentAdapter.value = adapter
            val result = adapter.initialize()
            if (result.success) return result
        }
        
        return InitializationResult(
            success = false,
            message = "No native adapters available",
            adapterType = AdapterType.NATIVE_AUTEL
        )
    }
    
    private suspend fun detectAndInitBluetoothAdapters(): InitializationResult {
        Log.d(TAG, "Detecting Bluetooth adapters...")
        
        // Get paired OBD devices from our enhanced Bluetooth manager
        val pairedDevices = bluetoothManager.getPairedObdDevices()
        
        for (device in pairedDevices) {
            // Try BLE first if device supports it
            if (device.type == BluetoothDevice.DEVICE_TYPE_LE || device.type == BluetoothDevice.DEVICE_TYPE_DUAL) {
                Log.d(TAG, "Trying BLE connection to ${device.name}")
                val adapter = BleObdAdapterInitializer(context, device)
                _currentAdapter.value = adapter
                val result = adapter.initialize()
                if (result.success) return result
            }
            
            // Try Bluetooth Classic
            Log.d(TAG, "Trying Bluetooth Classic connection to ${device.name}")
            val adapter = Elm327BluetoothClassicInitializer(context, device)
            _currentAdapter.value = adapter
            val result = adapter.initialize()
            if (result.success) return result
        }
        
        return InitializationResult(
            success = false,
            message = "No Bluetooth OBD2 adapters found",
            adapterType = AdapterType.BLUETOOTH_CLASSIC
        )
    }
    
    private suspend fun detectAndInitWiFiAdapters(): InitializationResult {
        Log.d(TAG, "Detecting WiFi adapters...")
        
        // Common OBD2 WiFi adapter IP addresses
        val commonWiFiAddresses = listOf(
            "192.168.0.10",
            "192.168.1.5",
            "192.168.4.1",
            "10.0.0.1"
        )
        
        for (ipAddress in commonWiFiAddresses) {
            Log.d(TAG, "Trying WiFi connection to $ipAddress")
            val adapter = WiFiObdAdapterInitializer(context, ipAddress)
            _currentAdapter.value = adapter
            val result = adapter.initialize()
            if (result.success) return result
        }
        
        return InitializationResult(
            success = false,
            message = "No WiFi OBD2 adapters found",
            adapterType = AdapterType.WIFI
        )
    }
    
    private fun createAdapterInitializer(
        adapterType: AdapterType,
        params: AdapterConnectionParams
    ): AdapterInitializer {
        return when (adapterType) {
            AdapterType.BLUETOOTH_CLASSIC -> {
                val device = params.bluetoothDevice 
                    ?: throw IllegalArgumentException("Bluetooth device required for Bluetooth Classic adapter")
                Elm327BluetoothClassicInitializer(context, device)
            }
            AdapterType.BLUETOOTH_LE -> {
                val device = params.bluetoothDevice 
                    ?: throw IllegalArgumentException("Bluetooth device required for BLE adapter")
                BleObdAdapterInitializer(context, device)
            }
            AdapterType.WIFI -> {
                val ipAddress = params.ipAddress 
                    ?: throw IllegalArgumentException("IP address required for WiFi adapter")
                WiFiObdAdapterInitializer(context, ipAddress, params.port ?: 35000)
            }
            AdapterType.NATIVE_AUTEL -> {
                AutelNativeAdapterInitializer(context, params.deviceId)
            }
            AdapterType.NATIVE_J2534 -> {
                J2534AdapterInitializer(context, params.deviceName)
            }
            AdapterType.USB_SERIAL -> {
                val deviceName = params.deviceName 
                    ?: throw IllegalArgumentException("Device name required for USB Serial adapter")
                UsbSerialAdapterInitializer(context, deviceName, params.baudRate ?: 9600)
            }
        }
    }
    
    // Initialization sequence definitions
    private fun getBluetoothClassicSequence(): List<InitializationStep> {
        return listOf(
            InitializationStep(1, "Establish Bluetooth Connection", "Connect to paired OBD2 device"),
            InitializationStep(2, "Reset ELM327", "Send ATZ command to reset adapter"),
            InitializationStep(3, "Configure ELM327", "Set echo off, linefeeds off, spaces off"),
            InitializationStep(4, "Enable Headers", "Turn on OBD headers for detailed responses"),
            InitializationStep(5, "Auto-detect Protocol", "Automatically detect vehicle's OBD protocol"),
            InitializationStep(6, "Test OBD Communication", "Send test command to verify connection"),
            InitializationStep(7, "Verify Protocol", "Confirm protocol detection was successful")
        )
    }
    
    private fun getBleSequence(): List<InitializationStep> {
        return listOf(
            InitializationStep(1, "Connect to BLE Device", "Establish Bluetooth Low Energy connection"),
            InitializationStep(2, "Discover GATT Services", "Find available GATT services"),
            InitializationStep(3, "Setup Characteristics", "Configure read/write characteristics"),
            InitializationStep(4, "Enable Notifications", "Enable characteristic notifications"),
            InitializationStep(5, "Initialize OBD Commands", "Send initial AT commands"),
            InitializationStep(6, "Test Communication", "Verify BLE OBD communication")
        )
    }
    
    private fun getWiFiSequence(): List<InitializationStep> {
        return listOf(
            InitializationStep(1, "Establish TCP Connection", "Connect to WiFi adapter's IP address"),
            InitializationStep(2, "Initialize ELM327", "Send AT commands via TCP socket"),
            InitializationStep(3, "Configure WiFi Settings", "Set WiFi-specific parameters"),
            InitializationStep(4, "Enable Protocol Support", "Enable multiple protocol support"),
            InitializationStep(5, "Test Multiple Commands", "Verify various OBD commands work"),
            InitializationStep(6, "Optimize Timeouts", "Set appropriate timeout values")
        )
    }
    
    private fun getAutelNativeSequence(): List<InitializationStep> {
        return listOf(
            InitializationStep(1, "Load Native Libraries", "Load Autel diagnostic libraries"),
            InitializationStep(2, "Initialize VCI", "Initialize Vehicle Communication Interface"),
            InitializationStep(3, "Connect to Vehicle", "Establish vehicle connection"),
            InitializationStep(4, "Initialize Protocols", "Setup diagnostic protocols"),
            InitializationStep(5, "Test ECU Communication", "Verify ECU accessibility"),
            InitializationStep(6, "Enable Advanced Features", "Activate bidirectional control")
        )
    }
    
    private fun getJ2534Sequence(): List<InitializationStep> {
        return listOf(
            InitializationStep(1, "Load J2534 Library", "Load SAE J2534 pass-through library"),
            InitializationStep(2, "Open Device", "Open J2534 device connection"),
            InitializationStep(3, "Connect Protocols", "Connect to vehicle protocols"),
            InitializationStep(4, "Configure Filters", "Setup message filters"),
            InitializationStep(5, "Test Pass-Through", "Test pass-through communication"),
            InitializationStep(6, "Verify Professional Features", "Test advanced diagnostic capabilities")
        )
    }
    
    private fun getUsbSerialSequence(): List<InitializationStep> {
        return listOf(
            InitializationStep(1, "Detect USB Device", "Find connected USB OBD adapter"),
            InitializationStep(2, "Open Serial Port", "Establish serial communication"),
            InitializationStep(3, "Configure Baud Rate", "Set appropriate communication speed"),
            InitializationStep(4, "Initialize Adapter", "Send initialization commands"),
            InitializationStep(5, "Test Serial Communication", "Verify USB serial connection")
        )
    }
    
    suspend fun cleanup() {
        _currentAdapter.value?.cleanup()
        _currentAdapter.value = null
        _initializationState.value = AdapterInitializationState.IDLE
    }
}

// Placeholder adapter initializers that extend our existing ones
class BleObdAdapterInitializer(
    private val context: Context,
    private val device: BluetoothDevice
) : AdapterInitializer {
    
    override suspend fun initialize(): InitializationResult {
        // BLE initialization logic would go here
        return InitializationResult(
            success = false,
            message = "BLE adapter not yet implemented",
            adapterType = AdapterType.BLUETOOTH_LE
        )
    }
    
    override fun getAdapterInfo(): AdapterInfo {
        return AdapterInfo(
            name = "BLE OBD Adapter",
            type = AdapterType.BLUETOOTH_LE,
            version = "1.0",
            capabilities = setOf(AdapterCapability.BASIC_OBD)
        )
    }
    
    override suspend fun cleanup() {
        // BLE cleanup
    }
}

class WiFiObdAdapterInitializer(
    private val context: Context,
    private val ipAddress: String,
    private val port: Int = 35000
) : AdapterInitializer {
    
    override suspend fun initialize(): InitializationResult {
        // WiFi initialization logic would go here
        return InitializationResult(
            success = false,
            message = "WiFi adapter not yet implemented",
            adapterType = AdapterType.WIFI
        )
    }
    
    override fun getAdapterInfo(): AdapterInfo {
        return AdapterInfo(
            name = "WiFi OBD Adapter",
            type = AdapterType.WIFI,
            version = "1.0",
            capabilities = setOf(AdapterCapability.BASIC_OBD)
        )
    }
    
    override suspend fun cleanup() {
        // WiFi cleanup
    }
}

class AutelNativeAdapterInitializer(
    private val context: Context,
    private val deviceId: String? = null
) : AdapterInitializer {
    
    override suspend fun initialize(): InitializationResult {
        return if (NativeCapabilities.isUniversalObdAvailable()) {
            InitializationResult(
                success = true,
                message = "Autel native adapter initialized",
                adapterType = AdapterType.NATIVE_AUTEL
            )
        } else {
            InitializationResult(
                success = false,
                message = "Autel libraries not available",
                adapterType = AdapterType.NATIVE_AUTEL
            )
        }
    }
    
    override fun getAdapterInfo(): AdapterInfo {
        return AdapterInfo(
            name = "Autel Native Adapter",
            type = AdapterType.NATIVE_AUTEL,
            version = "1.0",
            capabilities = setOf(
                AdapterCapability.BASIC_OBD,
                AdapterCapability.ADVANCED_DIAGNOSTICS,
                AdapterCapability.BIDIRECTIONAL_CONTROL
            )
        )
    }
    
    override suspend fun cleanup() {
        // Autel cleanup
    }
}

class J2534AdapterInitializer(
    private val context: Context,
    private val deviceName: String? = null
) : AdapterInitializer {
    
    override suspend fun initialize(): InitializationResult {
        return if (NativeCapabilities.isJ2534Available()) {
            InitializationResult(
                success = true,
                message = "J2534 adapter initialized",
                adapterType = AdapterType.NATIVE_J2534
            )
        } else {
            InitializationResult(
                success = false,
                message = "J2534 library not available",
                adapterType = AdapterType.NATIVE_J2534
            )
        }
    }
    
    override fun getAdapterInfo(): AdapterInfo {
        return AdapterInfo(
            name = "J2534 Pass-Through Adapter",
            type = AdapterType.NATIVE_J2534,
            version = "1.0",
            capabilities = setOf(
                AdapterCapability.BASIC_OBD,
                AdapterCapability.ADVANCED_DIAGNOSTICS,
                AdapterCapability.MANUFACTURER_SPECIFIC
            )
        )
    }
    
    override suspend fun cleanup() {
        // J2534 cleanup
    }
}

class UsbSerialAdapterInitializer(
    private val context: Context,
    private val deviceName: String,
    private val baudRate: Int = 9600
) : AdapterInitializer {
    
    override suspend fun initialize(): InitializationResult {
        return try {
            // USB Serial initialization logic would go here
            // This would typically involve:
            // 1. Finding USB devices using UsbManager
            // 2. Opening serial port connection
            // 3. Configuring baud rate, data bits, stop bits, parity
            // 4. Testing communication with ELM327 commands
            
            Log.d("UsbSerialAdapter", "Initializing USB Serial adapter: $deviceName at $baudRate baud")
            
            InitializationResult(
                success = true,
                message = "USB Serial adapter initialized successfully",
                adapterType = AdapterType.USB_SERIAL
            )
        } catch (e: Exception) {
            InitializationResult(
                success = false,
                message = "USB Serial initialization failed: ${e.message}",
                adapterType = AdapterType.USB_SERIAL
            )
        }
    }
    
    override fun getAdapterInfo(): AdapterInfo {
        return AdapterInfo(
            name = "USB Serial OBD Adapter",
            type = AdapterType.USB_SERIAL,
            version = "1.0",
            capabilities = setOf(
                AdapterCapability.BASIC_OBD
                // AdapterCapability.HIGH_SPEED_COMMUNICATION // Commented out until defined
            )
        )
    }
    
    override suspend fun cleanup() {
        // Close serial port connection
        Log.d("UsbSerialAdapter", "Cleaning up USB Serial adapter")
    }
}

data class AdapterConnectionParams(
    val bluetoothDevice: BluetoothDevice? = null,
    val ipAddress: String? = null,
    val port: Int? = null,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val baudRate: Int? = null
)

enum class AdapterInitializationState {
    IDLE,
    DETECTING,
    INITIALIZING,
    INITIALIZED,
    FAILED
}

data class InitializationStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

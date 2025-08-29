package com.spacetec.connection.transport

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * 🔵 Bluetooth Low Energy OBD Transport Implementation
 * Handles BLE connections for modern OBD adapters
 */
class BluetoothLeTransport(
    override val config: ObdTransport.TransportConfig,
    private val context: Context
) : ObdTransport, CoroutineScope {

    companion object {
        private const val OBD_SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb"
        private const val OBD_CHARACTERISTIC_UUID = "0000fff1-0000-1000-8000-00805f9b34fb"
        private const val CONNECTION_TIMEOUT = 15000L
        private const val SCAN_TIMEOUT = 10000L
        private const val TAG = "BluetoothLeTransport"
    }

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.Main + job

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null
    private var obdCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow(ObdTransport.ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ObdTransport.ConnectionState> = _connectionState.asStateFlow()

    private val _dataStream = MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 100)
    private val _errorStream = MutableSharedFlow<ObdTransport.ConnectionError>(replay = 1, extraBufferCapacity = 10)

    private var reconnectAttempts = 0
    private var lastActivity = System.currentTimeMillis()
    private var connectionQuality = 1.0f
    private var pendingCommands = mutableMapOf<String, CompletableDeferred<ByteArray>>()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    launch {
                        _connectionState.value = ObdTransport.ConnectionState.CONNECTED
                        if (hasBluetoothPermissions()) {
                            gatt?.discoverServices()
                        }
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    launch {
                        _connectionState.value = ObdTransport.ConnectionState.DISCONNECTED
                        if (config.autoReconnect && reconnectAttempts < config.maxReconnectAttempts) {
                            reconnect()
                        }
                    }
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    launch { _connectionState.value = ObdTransport.ConnectionState.CONNECTING }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(UUID.fromString(OBD_SERVICE_UUID))
                obdCharacteristic = service?.getCharacteristic(UUID.fromString(OBD_CHARACTERISTIC_UUID))
                
                obdCharacteristic?.let { characteristic ->
                    if (hasBluetoothPermissions()) {
                        gatt?.setCharacteristicNotification(characteristic, true)
                        
                        // Enable notifications
                        val descriptor = characteristic.getDescriptor(
                            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                        )
                        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt?.writeDescriptor(descriptor)
                    }
                }
                
                launch {
                    _connectionState.value = ObdTransport.ConnectionState.CONNECTED
                    reconnectAttempts = 0
                    lastActivity = System.currentTimeMillis()
                    connectionQuality = 1.0f
                }
            } else {
                launch {
                    _errorStream.emit(
                        ObdTransport.ConnectionError(
                            code = 3001,
                            message = "Service discovery failed: $status",
                            isRecoverable = true
                        )
                    )
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            
            characteristic?.value?.let { data ->
                launch {
                    _dataStream.emit(data)
                    lastActivity = System.currentTimeMillis()
                    connectionQuality = minOf(1.0f, connectionQuality + 0.01f)
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            
            if (status != BluetoothGatt.GATT_SUCCESS) {
                launch {
                    connectionQuality *= 0.9f
                    _errorStream.emit(
                        ObdTransport.ConnectionError(
                            code = 3002,
                            message = "Write failed: $status",
                            isRecoverable = true
                        )
                    )
                }
            }
        }
    }

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            if (_connectionState.value == ObdTransport.ConnectionState.CONNECTED) {
                return@withContext Result.success(Unit)
            }

            _connectionState.value = ObdTransport.ConnectionState.CONNECTING

            if (!hasBluetoothPermissions()) {
                throw SecurityException("Missing Bluetooth permissions")
            }

            val device = bluetoothAdapter?.getRemoteDevice(config.address)
                ?: throw IllegalStateException("Cannot get Bluetooth device")

            bluetoothGatt = device.connectGatt(context, false, gattCallback)

            // Wait for connection with timeout
            withTimeout(CONNECTION_TIMEOUT) {
                while (_connectionState.value != ObdTransport.ConnectionState.CONNECTED) {
                    delay(100)
                }
            }

            Result.success(Unit)

        } catch (e: Exception) {
            _connectionState.value = ObdTransport.ConnectionState.ERROR
            val error = ObdTransport.ConnectionError(
                code = when (e) {
                    is SecurityException -> 3003
                    is TimeoutCancellationException -> 3004
                    else -> 3000
                },
                message = "BLE connection failed: ${e.message}",
                cause = e,
                isRecoverable = e !is SecurityException
            )
            _errorStream.emit(error)
            cleanup()
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            _connectionState.value = ObdTransport.ConnectionState.DISCONNECTED
            cleanup()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reconnect(): Result<Unit> {
        if (reconnectAttempts >= config.maxReconnectAttempts) {
            _errorStream.emit(
                ObdTransport.ConnectionError(
                    code = 3005,
                    message = "Max BLE reconnection attempts reached",
                    isRecoverable = false
                )
            )
            return Result.failure(Exception("Max reconnection attempts reached"))
        }

        _connectionState.value = ObdTransport.ConnectionState.RECONNECTING
        reconnectAttempts++

        disconnect()
        delay(2000L * reconnectAttempts) // Exponential backoff
        return connect()
    }

    override suspend fun sendCommand(command: ByteArray): Result<ByteArray> = withContext(Dispatchers.Main) {
        try {
            if (!isConnected || obdCharacteristic == null) {
                throw IllegalStateException("Not connected or characteristic not available")
            }

            if (!hasBluetoothPermissions()) {
                throw SecurityException("Missing Bluetooth permissions")
            }

            obdCharacteristic?.value = command
            val success = bluetoothGatt?.writeCharacteristic(obdCharacteristic) ?: false

            if (!success) {
                throw Exception("Failed to write characteristic")
            }

            lastActivity = System.currentTimeMillis()

            // For BLE, we typically get response via notification
            // Wait for response with timeout
            val response = withTimeout(config.timeout) {
                _dataStream.first()
            }

            Result.success(response)

        } catch (e: Exception) {
            connectionQuality *= 0.9f
            _errorStream.emit(
                ObdTransport.ConnectionError(
                    code = 3006,
                    message = "BLE command failed: ${e.message}",
                    cause = e
                )
            )
            Result.failure(e)
        }
    }

    override suspend fun sendCommand(command: String): Result<String> {
        val result = sendCommand(command.toByteArray())
        return result.map { String(it) }
    }

    override fun getDataStream(): Flow<ByteArray> = _dataStream.asSharedFlow()

    override fun getErrorStream(): Flow<ObdTransport.ConnectionError> = _errorStream.asSharedFlow()

    override suspend fun ping(): Boolean {
        return try {
            val result = sendCommand("01 00\r".toByteArray())
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    override fun getConnectionQuality(): Float = connectionQuality

    override fun getLastActivity(): Long = lastActivity

    override fun cleanup() {
        try {
            if (hasBluetoothPermissions()) {
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        } finally {
            bluetoothGatt = null
            obdCharacteristic = null
            pendingCommands.clear()
            job.cancel()
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }
}

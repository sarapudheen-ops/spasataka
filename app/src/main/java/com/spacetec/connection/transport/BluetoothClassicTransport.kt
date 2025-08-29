package com.spacetec.connection.transport

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * 🔵 Bluetooth Classic OBD Transport Implementation
 * Handles traditional Bluetooth SPP (Serial Port Profile) connections
 */
class BluetoothClassicTransport(
    override val config: ObdTransport.TransportConfig,
    private val context: Context
) : ObdTransport, CoroutineScope {

    companion object {
        private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        private const val CONNECTION_TIMEOUT = 10000L
        private const val READ_TIMEOUT = 5000L
        private const val PING_COMMAND = "01 00\r"
        private const val TAG = "BluetoothClassicTransport"
    }

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.IO + job

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val _connectionState = MutableStateFlow(ObdTransport.ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ObdTransport.ConnectionState> = _connectionState.asStateFlow()

    private val _dataStream = MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 100)
    private val _errorStream = MutableSharedFlow<ObdTransport.ConnectionError>(replay = 1, extraBufferCapacity = 10)

    private var reconnectAttempts = 0
    private var lastActivity = System.currentTimeMillis()
    private var connectionQuality = 1.0f

    init {
        if (bluetoothAdapter == null) {
            launch {
                _errorStream.emit(
                    ObdTransport.ConnectionError(
                        code = -1,
                        message = "Bluetooth not supported on this device",
                        isRecoverable = false
                    )
                )
            }
        }
    }

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (_connectionState.value == ObdTransport.ConnectionState.CONNECTED) {
                return@withContext Result.success(Unit)
            }

            _connectionState.value = ObdTransport.ConnectionState.CONNECTING

            // Check permissions
            if (!hasBluetoothPermissions()) {
                throw SecurityException("Missing Bluetooth permissions")
            }

            // Get device
            val device = bluetoothAdapter?.getRemoteDevice(config.address)
                ?: throw IllegalStateException("Cannot get Bluetooth device")

            // Create socket
            bluetoothSocket = if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID))
            } else {
                throw SecurityException("Missing BLUETOOTH_CONNECT permission")
            }

            // Cancel discovery to improve connection speed
            bluetoothAdapter?.cancelDiscovery()

            // Connect with timeout
            withTimeout(CONNECTION_TIMEOUT) {
                bluetoothSocket?.connect()
            }

            // Setup streams
            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream

            _connectionState.value = ObdTransport.ConnectionState.CONNECTED
            reconnectAttempts = 0
            lastActivity = System.currentTimeMillis()
            connectionQuality = 1.0f

            // Start data reading coroutine
            startDataReading()

            Result.success(Unit)

        } catch (e: Exception) {
            _connectionState.value = ObdTransport.ConnectionState.ERROR
            val error = ObdTransport.ConnectionError(
                code = when (e) {
                    is IOException -> 1001
                    is SecurityException -> 1002
                    is TimeoutCancellationException -> 1003
                    else -> 1000
                },
                message = "Connection failed: ${e.message}",
                cause = e,
                isRecoverable = e !is SecurityException
            )
            _errorStream.emit(error)
            cleanup()
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
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
                    code = 1004,
                    message = "Max reconnection attempts reached",
                    isRecoverable = false
                )
            )
            return Result.failure(Exception("Max reconnection attempts reached"))
        }

        _connectionState.value = ObdTransport.ConnectionState.RECONNECTING
        reconnectAttempts++

        disconnect()
        delay(1000L * reconnectAttempts) // Exponential backoff
        return connect()
    }

    override suspend fun sendCommand(command: ByteArray): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            if (!isConnected) {
                throw IllegalStateException("Not connected")
            }

            outputStream?.write(command)
            outputStream?.flush()
            lastActivity = System.currentTimeMillis()

            // Read response with timeout
            val response = withTimeout(READ_TIMEOUT) {
                readResponse()
            }

            Result.success(response)

        } catch (e: Exception) {
            connectionQuality *= 0.9f // Degrade quality on errors
            _errorStream.emit(
                ObdTransport.ConnectionError(
                    code = 2001,
                    message = "Command failed: ${e.message}",
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
            val result = sendCommand(PING_COMMAND)
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    override fun getConnectionQuality(): Float = connectionQuality

    override fun getLastActivity(): Long = lastActivity

    override fun cleanup() {
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            // Ignore cleanup errors
        } finally {
            inputStream = null
            outputStream = null
            bluetoothSocket = null
            job.cancel()
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startDataReading() {
        launch {
            try {
                val buffer = ByteArray(1024)
                while (isConnected && inputStream != null) {
                    val bytesRead = inputStream!!.read(buffer)
                    if (bytesRead > 0) {
                        val data = buffer.copyOf(bytesRead)
                        _dataStream.emit(data)
                        lastActivity = System.currentTimeMillis()
                        connectionQuality = minOf(1.0f, connectionQuality + 0.01f)
                    }
                }
            } catch (e: Exception) {
                if (isConnected && config.autoReconnect) {
                    launch { reconnect() }
                }
            }
        }
    }

    private suspend fun readResponse(): ByteArray {
        val buffer = ByteArray(1024)
        val bytesRead = inputStream?.read(buffer) ?: 0
        return buffer.copyOf(bytesRead)
    }
}

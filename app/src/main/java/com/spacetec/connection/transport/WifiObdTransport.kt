package com.spacetec.connection.transport

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.net.*
import kotlin.coroutines.CoroutineContext

/**
 * 📶 Wi-Fi OBD Transport Implementation
 * Handles TCP/IP connections to Wi-Fi OBD adapters (ELM327 Wi-Fi, etc.)
 */
class WifiObdTransport(
    override val config: ObdTransport.TransportConfig
) : ObdTransport, CoroutineScope {

    companion object {
        private const val DEFAULT_PORT = 35000
        private const val CONNECTION_TIMEOUT = 10000
        private const val READ_TIMEOUT = 5000
        private const val SOCKET_TIMEOUT = 30000
        private const val PING_COMMAND = "01 00\r\n"
        private const val TAG = "WifiObdTransport"
    }

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.IO + job

    private var socket: Socket? = null
    private var inputStream: BufferedReader? = null
    private var outputStream: PrintWriter? = null

    private val _connectionState = MutableStateFlow(ObdTransport.ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ObdTransport.ConnectionState> = _connectionState.asStateFlow()

    private val _dataStream = MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 100)
    private val _errorStream = MutableSharedFlow<ObdTransport.ConnectionError>(replay = 1, extraBufferCapacity = 10)

    private var reconnectAttempts = 0
    private var lastActivity = System.currentTimeMillis()
    private var connectionQuality = 1.0f

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (_connectionState.value == ObdTransport.ConnectionState.CONNECTED) {
                return@withContext Result.success(Unit)
            }

            _connectionState.value = ObdTransport.ConnectionState.CONNECTING

            val port = config.port ?: DEFAULT_PORT
            val address = InetSocketAddress(config.address, port)

            socket = Socket().apply {
                soTimeout = SOCKET_TIMEOUT
                tcpNoDelay = true
                keepAlive = true
            }

            // Connect with timeout
            withTimeout(CONNECTION_TIMEOUT.toLong()) {
                socket?.connect(address, CONNECTION_TIMEOUT)
            }

            // Setup streams
            inputStream = BufferedReader(InputStreamReader(socket?.getInputStream()))
            outputStream = PrintWriter(OutputStreamWriter(socket?.getOutputStream()), true)

            _connectionState.value = ObdTransport.ConnectionState.CONNECTED
            reconnectAttempts = 0
            lastActivity = System.currentTimeMillis()
            connectionQuality = 1.0f

            // Start data reading coroutine
            startDataReading()

            // Initialize ELM327 if needed
            initializeElm327()

            Result.success(Unit)

        } catch (e: Exception) {
            _connectionState.value = ObdTransport.ConnectionState.ERROR
            val error = ObdTransport.ConnectionError(
                code = when (e) {
                    is SocketTimeoutException -> 4001
                    is ConnectException -> 4002
                    is UnknownHostException -> 4003
                    is TimeoutCancellationException -> 4004
                    else -> 4000
                },
                message = "Wi-Fi connection failed: ${e.message}",
                cause = e,
                isRecoverable = true
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
                    code = 4005,
                    message = "Max Wi-Fi reconnection attempts reached",
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

            val commandStr = String(command)
            outputStream?.println(commandStr)
            outputStream?.flush()
            lastActivity = System.currentTimeMillis()

            // Read response with timeout
            val response = withTimeout(READ_TIMEOUT.toLong()) {
                readResponse()
            }

            Result.success(response.toByteArray())

        } catch (e: Exception) {
            connectionQuality *= 0.9f
            _errorStream.emit(
                ObdTransport.ConnectionError(
                    code = 4006,
                    message = "Wi-Fi command failed: ${e.message}",
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
            socket?.close()
        } catch (e: Exception) {
            // Ignore cleanup errors
        } finally {
            inputStream = null
            outputStream = null
            socket = null
            job.cancel()
        }
    }

    private fun startDataReading() {
        launch {
            try {
                while (isConnected && inputStream != null) {
                    val line = inputStream?.readLine()
                    if (line != null) {
                        _dataStream.emit(line.toByteArray())
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

    private suspend fun readResponse(): String {
        val response = StringBuilder()
        var line: String?
        
        do {
            line = inputStream?.readLine()
            if (line != null) {
                response.append(line).append("\n")
            }
        } while (line != null && !line.contains(">") && !line.contains("OK"))
        
        return response.toString().trim()
    }

    private suspend fun initializeElm327() {
        try {
            // Basic ELM327 initialization sequence
            val initCommands = listOf(
                "ATZ",      // Reset
                "ATE0",     // Echo off
                "ATL0",     // Linefeeds off
                "ATS0",     // Spaces off
                "ATH1",     // Headers on
                "ATSP0"     // Auto protocol
            )

            for (command in initCommands) {
                sendCommand("$command\r")
                delay(100) // Small delay between commands
            }
        } catch (e: Exception) {
            _errorStream.emit(
                ObdTransport.ConnectionError(
                    code = 4007,
                    message = "ELM327 initialization failed: ${e.message}",
                    cause = e,
                    isRecoverable = true
                )
            )
        }
    }
}

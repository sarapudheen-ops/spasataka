package com.spacetec.connection

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.spacetec.R
import com.spacetec.connection.transport.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

/**
 * 🚀 SpaceTec OBD Connection Foreground Service
 * Maintains persistent OBD connections with automatic reconnection
 */
class ObdConnectionService : Service(), CoroutineScope {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "obd_connection_channel"
        private const val HEALTH_CHECK_INTERVAL = 30000L // 30 seconds
        private const val TAG = "ObdConnectionService"
    }

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.Main + job

    private val binder = ObdConnectionBinder()
    private var currentTransport: ObdTransport? = null
    private var connectionManager: ConnectionManager? = null

    // Service state
    private val _serviceState = MutableStateFlow(ServiceState.IDLE)
    val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>(replay = 1, extraBufferCapacity = 10)
    val connectionEvents: SharedFlow<ConnectionEvent> = _connectionEvents.asSharedFlow()

    enum class ServiceState {
        IDLE,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        ERROR,
        STOPPING
    }

    sealed class ConnectionEvent {
        object Connected : ConnectionEvent()
        object Disconnected : ConnectionEvent()
        data class Error(val error: ObdTransport.ConnectionError) : ConnectionEvent()
        data class DataReceived(val data: ByteArray) : ConnectionEvent()
        data class QualityChanged(val quality: Float) : ConnectionEvent()
    }

    inner class ObdConnectionBinder : Binder() {
        fun getService(): ObdConnectionService = this@ObdConnectionService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        connectionManager = ConnectionManager(this)
        startHealthMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("SpaceTec OBD Service", "Ready to connect")
        startForeground(NOTIFICATION_ID, notification)
        
        return START_STICKY // Restart if killed
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        _serviceState.value = ServiceState.STOPPING
        currentTransport?.cleanup()
        job.cancel()
    }

    /**
     * Connect to OBD adapter with specified configuration
     */
    suspend fun connectToAdapter(config: ObdTransport.TransportConfig): Result<Unit> {
        return try {
            _serviceState.value = ServiceState.CONNECTING
            updateNotification("Connecting to ${config.name ?: config.address}")

            // Disconnect existing transport
            currentTransport?.disconnect()

            // Create new transport
            currentTransport = ObdTransportFactory.createTransport(config, this)
            
            // Setup event monitoring
            setupTransportMonitoring()

            // Attempt connection
            val result = currentTransport?.connect()
            
            if (result?.isSuccess == true) {
                _serviceState.value = ServiceState.CONNECTED
                updateNotification("Connected to ${config.name ?: config.address}")
                _connectionEvents.emit(ConnectionEvent.Connected)
            } else {
                _serviceState.value = ServiceState.ERROR
                updateNotification("Connection failed")
                result?.exceptionOrNull()?.let { exception ->
                    _connectionEvents.emit(
                        ConnectionEvent.Error(
                            ObdTransport.ConnectionError(
                                code = 6001,
                                message = "Service connection failed: ${exception.message}",
                                cause = exception
                            )
                        )
                    )
                }
            }

            result ?: Result.failure(Exception("Transport creation failed"))

        } catch (e: Exception) {
            _serviceState.value = ServiceState.ERROR
            updateNotification("Connection error")
            _connectionEvents.emit(
                ConnectionEvent.Error(
                    ObdTransport.ConnectionError(
                        code = 6000,
                        message = "Service error: ${e.message}",
                        cause = e
                    )
                )
            )
            Result.failure(e)
        }
    }

    /**
     * Disconnect from current OBD adapter
     */
    suspend fun disconnect(): Result<Unit> {
        return try {
            currentTransport?.disconnect()
            _serviceState.value = ServiceState.IDLE
            updateNotification("Disconnected")
            _connectionEvents.emit(ConnectionEvent.Disconnected)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send command to OBD adapter
     */
    suspend fun sendCommand(command: String): Result<String> {
        return currentTransport?.sendCommand(command) 
            ?: Result.failure(IllegalStateException("No active transport"))
    }

    /**
     * Get current connection state
     */
    fun getConnectionState(): ObdTransport.ConnectionState? {
        return currentTransport?.connectionState?.value
    }

    /**
     * Get connection quality (0.0 to 1.0)
     */
    fun getConnectionQuality(): Float {
        return currentTransport?.getConnectionQuality() ?: 0.0f
    }

    /**
     * Get data stream from current transport
     */
    fun getDataStream(): Flow<ByteArray>? {
        return currentTransport?.getDataStream()
    }

    private fun setupTransportMonitoring() {
        currentTransport?.let { transport ->
            // Monitor connection state changes
            launch {
                transport.connectionState.collect { state ->
                    when (state) {
                        ObdTransport.ConnectionState.CONNECTED -> {
                            _serviceState.value = ServiceState.CONNECTED
                            _connectionEvents.emit(ConnectionEvent.Connected)
                        }
                        ObdTransport.ConnectionState.DISCONNECTED -> {
                            _serviceState.value = ServiceState.IDLE
                            _connectionEvents.emit(ConnectionEvent.Disconnected)
                        }
                        ObdTransport.ConnectionState.RECONNECTING -> {
                            _serviceState.value = ServiceState.RECONNECTING
                            updateNotification("Reconnecting...")
                        }
                        ObdTransport.ConnectionState.ERROR -> {
                            _serviceState.value = ServiceState.ERROR
                            updateNotification("Connection error")
                        }
                        else -> { /* Handle other states */ }
                    }
                }
            }

            // Monitor data stream
            launch {
                transport.getDataStream().collect { data ->
                    _connectionEvents.emit(ConnectionEvent.DataReceived(data))
                }
            }

            // Monitor errors
            launch {
                transport.getErrorStream().collect { error ->
                    _connectionEvents.emit(ConnectionEvent.Error(error))
                }
            }

            // Monitor connection quality
            launch {
                while (isActive) {
                    val quality = transport.getConnectionQuality()
                    _connectionEvents.emit(ConnectionEvent.QualityChanged(quality))
                    delay(5000) // Check every 5 seconds
                }
            }
        }
    }

    private fun startHealthMonitoring() {
        launch {
            while (isActive) {
                delay(HEALTH_CHECK_INTERVAL)
                
                currentTransport?.let { transport ->
                    if (transport.isConnected) {
                        // Perform health check
                        val isHealthy = transport.ping()
                        if (!isHealthy) {
                            // Connection seems unhealthy, attempt reconnection
                            launch { transport.reconnect() }
                        }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OBD Connection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains OBD adapter connections"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_bluetooth) // You'll need to add this icon
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification("SpaceTec OBD Service", content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

/**
 * 🔧 Connection Manager for handling multiple transport types
 */
class ConnectionManager(private val context: Context) {
    
    fun getSupportedTransports(): List<ObdTransport.TransportType> {
        return ObdTransportFactory.getSupportedTransports()
    }
    
    fun createTransportConfig(
        type: ObdTransport.TransportType,
        address: String,
        name: String? = null,
        port: Int? = null
    ): ObdTransport.TransportConfig {
        return ObdTransport.TransportConfig(
            type = type,
            address = address,
            name = name,
            port = port,
            timeout = 5000L,
            autoReconnect = true,
            maxReconnectAttempts = 3
        )
    }
}

package com.spacetec.connection.transport

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 🚀 SpaceTec OBD Transport Abstraction
 * Universal interface for all OBD connection types (Bluetooth Classic, BLE, Wi-Fi)
 */
interface ObdTransport {
    
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        ERROR
    }
    
    enum class TransportType {
        BLUETOOTH_CLASSIC,
        BLUETOOTH_LE,
        WIFI,
        USB_SERIAL
    }
    
    data class TransportConfig(
        val type: TransportType,
        val address: String,
        val name: String? = null,
        val port: Int? = null,
        val timeout: Long = 5000L,
        val autoReconnect: Boolean = true,
        val maxReconnectAttempts: Int = 3
    )
    
    data class ConnectionError(
        val code: Int,
        val message: String,
        val cause: Throwable? = null,
        val isRecoverable: Boolean = true
    )
    
    // Connection state management
    val connectionState: StateFlow<ConnectionState>
    val config: TransportConfig
    val isConnected: Boolean get() = connectionState.value == ConnectionState.CONNECTED
    
    // Connection lifecycle
    suspend fun connect(): Result<Unit>
    suspend fun disconnect(): Result<Unit>
    suspend fun reconnect(): Result<Unit>
    
    // Data transmission
    suspend fun sendCommand(command: ByteArray): Result<ByteArray>
    suspend fun sendCommand(command: String): Result<String>
    
    // Data streaming
    fun getDataStream(): Flow<ByteArray>
    fun getErrorStream(): Flow<ConnectionError>
    
    // Connection health
    suspend fun ping(): Boolean
    fun getConnectionQuality(): Float // 0.0 to 1.0
    fun getLastActivity(): Long // timestamp
    
    // Cleanup
    fun cleanup()
}

/**
 * 🔧 Transport Factory for creating appropriate transport instances
 */
object ObdTransportFactory {
    
    fun createTransport(config: ObdTransport.TransportConfig, context: android.content.Context): ObdTransport {
        return when (config.type) {
            ObdTransport.TransportType.BLUETOOTH_CLASSIC -> BluetoothClassicTransport(config, context)
            ObdTransport.TransportType.BLUETOOTH_LE -> BluetoothLeTransport(config, context)
            ObdTransport.TransportType.WIFI -> WifiObdTransport(config)
            ObdTransport.TransportType.USB_SERIAL -> UsbSerialTransport(config)
        }
    }
    
    fun getSupportedTransports(): List<ObdTransport.TransportType> {
        return listOf(
            ObdTransport.TransportType.BLUETOOTH_CLASSIC,
            ObdTransport.TransportType.BLUETOOTH_LE,
            ObdTransport.TransportType.WIFI,
            ObdTransport.TransportType.USB_SERIAL
        )
    }
}

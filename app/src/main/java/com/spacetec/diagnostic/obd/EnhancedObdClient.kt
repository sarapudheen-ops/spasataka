package com.spacetec.diagnostic.obd

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.Log
import kotlin.time.Duration.Companion.milliseconds
import com.spacetec.obd.ObdProtocol

/**
 * Enhanced OBD Client with robust error handling, connection recovery,
 * and intelligent retry mechanisms
 */
class EnhancedObdClient(
    private val transport: ObdTransport,
    private val config: ObdConfig = ObdConfig()
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val commandMutex = Mutex()
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()
    
    private var retryCount = 0
    private var lastSuccessfulCommand = System.currentTimeMillis()
    
    companion object {
        private const val TAG = "EnhancedObdClient"
    }
    
    /**
     * Read PID with automatic retry and error recovery
     */
    suspend fun readPidWithRecovery(pidHex: String): Result<String> {
        return commandMutex.withLock {
            retry(config.maxRetries) { attempt ->
                try {
                    Log.d(TAG, "Reading PID $pidHex (attempt ${attempt + 1})")
                    
                    val result = withTimeout(config.commandTimeout) {
                        val command = "01$pidHex"
                        transport.write(command)
                        transport.read()
                    }
                    
                    if (result.isNotEmpty() && !ObdProtocol.isErrorResponse(result)) {
                        lastSuccessfulCommand = System.currentTimeMillis()
                        retryCount = 0
                        _connectionState.value = ConnectionState.CONNECTED
                        Result.success(result)
                    } else {
                        throw ObdException.EmptyResponse(pidHex)
                    }
                    
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "Timeout reading PID $pidHex")
                    handleTimeout(pidHex, attempt)
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading PID $pidHex: ${e.message}")
                    handleError(e, pidHex, attempt)
                }
            }
        }
    }
    
    /**
     * Batch read multiple PIDs for efficiency
     */
    suspend fun readPidsBatch(pids: List<String>): Map<String, Result<String>> {
        val results = mutableMapOf<String, Result<String>>()
        
        // Group PIDs by priority
        val highPriorityPids = pids.filter { it in config.highPriorityPids }
        val normalPriorityPids = pids - highPriorityPids.toSet()
        
        // Read high priority PIDs first
        highPriorityPids.forEach { pid ->
            results[pid] = readPidWithRecovery(pid)
            if (results[pid]!!.isFailure) {
                // If high priority PID fails, skip others to save time
                Log.w(TAG, "High priority PID $pid failed, skipping batch")
                return results
            }
        }
        
        // Read remaining PIDs
        normalPriorityPids.forEach { pid ->
            results[pid] = readPidWithRecovery(pid)
        }
        
        return results
    }
    
    /**
     * Start monitoring PIDs with adaptive polling
     */
    fun startAdaptiveMonitoring(
        pids: List<String>,
        onUpdate: (Map<String, String>) -> Unit
    ): Job {
        return scope.launch {
            val pidGroups = groupPidsByPriority(pids)
            
            while (isActive) {
                try {
                    // High priority PIDs (RPM, Speed) - every 100ms
                    if (pidGroups.high.isNotEmpty()) {
                        val results = readPidsBatch(pidGroups.high)
                        val successfulResults = results.mapNotNull { (pid, result) ->
                            result.getOrNull()?.let { pid to it }
                        }.toMap()
                        
                        if (successfulResults.isNotEmpty()) {
                            onUpdate(successfulResults)
                        }
                    }
                    
                    delay(100.milliseconds)
                    
                    // Normal priority PIDs (Temperature, Fuel) - every 500ms
                    if (System.currentTimeMillis() % 500 < 100 && pidGroups.normal.isNotEmpty()) {
                        val results = readPidsBatch(pidGroups.normal)
                        val successfulResults = results.mapNotNull { (pid, result) ->
                            result.getOrNull()?.let { pid to it }
                        }.toMap()
                        
                        if (successfulResults.isNotEmpty()) {
                            onUpdate(successfulResults)
                        }
                    }
                    
                    // Low priority PIDs - every 2 seconds
                    if (System.currentTimeMillis() % 2000 < 100 && pidGroups.low.isNotEmpty()) {
                        val results = readPidsBatch(pidGroups.low)
                        val successfulResults = results.mapNotNull { (pid, result) ->
                            result.getOrNull()?.let { pid to it }
                        }.toMap()
                        
                        if (successfulResults.isNotEmpty()) {
                            onUpdate(successfulResults)
                        }
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error in adaptive monitoring: ${e.message}")
                    _connectionState.value = ConnectionState.ERROR
                    delay(1000) // Wait before retrying
                }
            }
        }
    }
    
    private suspend fun handleTimeout(pidHex: String, attempt: Int): Result<String> {
        retryCount++
        
        return if (attempt < config.maxRetries - 1) {
            Log.i(TAG, "Retrying PID $pidHex after timeout (attempt ${attempt + 1})")
            delay(config.retryDelay)
            throw ObdException.Timeout(pidHex) // Will trigger retry
        } else {
            _connectionState.value = ConnectionState.TIMEOUT
            Result.failure(ObdException.Timeout(pidHex))
        }
    }
    
    private suspend fun handleError(error: Exception, pidHex: String, attempt: Int): Result<String> {
        return when (error) {
            is ObdException.ConnectionLost -> {
                Log.w(TAG, "Connection lost, attempting to reconnect")
                if (attemptReconnection()) {
                    throw error // Will trigger retry
                } else {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    Result.failure(error)
                }
            }
            is ObdException.EmptyResponse -> {
                if (attempt < config.maxRetries - 1) {
                    delay(config.retryDelay)
                    throw error // Will trigger retry
                } else {
                    Result.failure(error)
                }
            }
            else -> {
                _connectionState.value = ConnectionState.ERROR
                Result.failure(error)
            }
        }
    }
    
    private suspend fun attemptReconnection(): Boolean {
        return try {
            Log.i(TAG, "Attempting to reconnect...")
            transport.close()
            delay(1000) // Wait before reconnecting
            val success = transport.open()
            if (success) {
                _connectionState.value = ConnectionState.CONNECTED
                Log.i(TAG, "Reconnection successful")
            } else {
                Log.w(TAG, "Reconnection failed")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Reconnection error: ${e.message}")
            false
        }
    }
    
    private suspend fun <T> retry(
        maxAttempts: Int,
        delayBetweenAttempts: Long = config.retryDelay,
        block: suspend (attempt: Int) -> Result<T>
    ): Result<T> {
        var lastException: Exception? = null
        
        repeat(maxAttempts) { attempt ->
            try {
                val result = block(attempt)
                if (result.isSuccess) {
                    return result
                }
                lastException = result.exceptionOrNull() as? Exception
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxAttempts - 1) {
                    delay(delayBetweenAttempts)
                }
            }
        }
        
        return Result.failure(
            lastException ?: ObdException.MaxRetriesExceeded(maxAttempts)
        )
    }
    
    private fun groupPidsByPriority(pids: List<String>): PidGroups {
        return PidGroups(
            high = pids.filter { it in config.highPriorityPids },
            normal = pids.filter { it in config.normalPriorityPids },
            low = pids - config.highPriorityPids.toSet() - config.normalPriorityPids.toSet()
        )
    }
    
    fun cleanup() {
        scope.cancel()
        transport.close()
    }
}

/**
 * Transport interface for OBD communication
 */
interface ObdTransport {
    suspend fun open(): Boolean
    suspend fun write(command: String)
    suspend fun read(): String
    fun close()
}

data class ObdConfig(
    val maxRetries: Int = 3,
    val commandTimeout: Long = 2000,
    val retryDelay: Long = 500,
    val highPriorityPids: List<String> = listOf("0C", "0D"), // RPM, Speed
    val normalPriorityPids: List<String> = listOf("05", "2F", "0F"), // Temp, Fuel, Intake
)

data class PidGroups(
    val high: List<String>,
    val normal: List<String>, 
    val low: List<String>
)

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, TIMEOUT, ERROR
}

sealed class ObdException(message: String) : Exception(message) {
    data class Timeout(val pid: String) : ObdException("Timeout reading PID $pid")
    data class ConnectionLost(val reason: String) : ObdException("Connection lost: $reason")
    data class EmptyResponse(val pid: String) : ObdException("Empty response for PID $pid")
    data class MaxRetriesExceeded(val attempts: Int) : ObdException("Max retries exceeded: $attempts")
    data class ParseError(val rawData: String) : ObdException("Failed to parse: $rawData")
}

package com.spacetec.diagnostic.obd

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.*
import android.util.Log
import com.spacetec.diagnostic.error.ObdError
import com.spacetec.diagnostic.error.ObdErrorHandler

/**
 * Thread-safe OBD manager with command queuing and concurrency control
 */
class SafeObdManager(
    private val robustClient: RobustObdClient,
    private val errorHandler: ObdErrorHandler
) {
    private val commandQueue = Channel<ObdCommand>(capacity = Channel.UNLIMITED)
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // State management
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _commandResults = MutableSharedFlow<CommandResult>()
    val commandResults: SharedFlow<CommandResult> = _commandResults.asSharedFlow()
    
    private var processingJob: Job? = null
    private var isProcessing = false
    
    companion object {
        private const val TAG = "SafeObdManager"
    }
    
    init {
        startCommandProcessor()
    }
    
    /**
     * Execute OBD command with thread safety
     */
    suspend fun executeCommand(command: ObdCommand): Result<String> {
        return mutex.withLock {
            try {
                Log.d(TAG, "Executing command: ${command.pid}")
                
                val result = when (command.type) {
                    CommandType.READ_PID -> robustClient.readPidWithRecovery(command.pid)
                    CommandType.BATCH_READ -> {
                        val results = robustClient.readMultiplePids(command.pids ?: listOf(command.pid))
                        val combinedResult = results.values.firstOrNull { it.isSuccess }
                        combinedResult ?: Result.failure(Exception("No successful results"))
                    }
                    CommandType.TEST_CONNECTION -> {
                        val isConnected = robustClient.testConnection()
                        if (isConnected) Result.success("OK") else Result.failure(Exception("Connection test failed"))
                    }
                }
                
                // Emit result for observers
                _commandResults.emit(CommandResult(command.id, command.pid, result))
                
                result
            } catch (e: Exception) {
                Log.e(TAG, "Command execution error: ${e.message}")
                val failureResult = Result.failure<String>(e)
                _commandResults.emit(CommandResult(command.id, command.pid, failureResult))
                failureResult
            }
        }
    }
    
    /**
     * Queue command for asynchronous execution
     */
    suspend fun queueCommand(command: ObdCommand) {
        try {
            commandQueue.send(command)
            Log.d(TAG, "Command queued: ${command.pid}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to queue command: ${e.message}")
        }
    }
    
    /**
     * Execute multiple commands in batch with optimal ordering
     */
    suspend fun executeBatch(pids: List<String>): Map<String, Result<String>> {
        return mutex.withLock {
            try {
                Log.d(TAG, "Executing batch: ${pids.joinToString()}")
                
                // Sort PIDs by priority (critical PIDs first)
                val sortedPids = pids.sortedBy { getPidPriority(it) }
                
                robustClient.readMultiplePids(sortedPids)
            } catch (e: Exception) {
                Log.e(TAG, "Batch execution error: ${e.message}")
                pids.associateWith { Result.failure(e) }
            }
        }
    }
    
    /**
     * Start connection with proper state management
     */
    suspend fun connect(): Boolean {
        return mutex.withLock {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                
                val connected = robustClient.testConnection()
                _connectionState.value = if (connected) {
                    ConnectionState.CONNECTED
                } else {
                    ConnectionState.ERROR
                }
                
                Log.i(TAG, "Connection state: ${_connectionState.value}")
                connected
            } catch (e: Exception) {
                Log.e(TAG, "Connection error: ${e.message}")
                _connectionState.value = ConnectionState.ERROR
                false
            }
        }
    }
    
    /**
     * Disconnect and cleanup
     */
    suspend fun disconnect() {
        mutex.withLock {
            try {
                robustClient.close()
                _connectionState.value = ConnectionState.DISCONNECTED
                Log.i(TAG, "Disconnected successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Disconnect error: ${e.message}")
            }
        }
    }
    
    /**
     * Get current connection status
     */
    fun isConnected(): Boolean {
        return _connectionState.value == ConnectionState.CONNECTED
    }
    
    /**
     * Start monitoring with adaptive polling
     */
    fun startAdaptiveMonitoring(
        highPriorityPids: List<String> = listOf("0C", "0D"), // RPM, Speed
        normalPriorityPids: List<String> = listOf("05", "2F"), // Temp, Fuel
        lowPriorityPids: List<String> = listOf("11", "0F") // Throttle, Intake Air Temp
    ): Job {
        return scope.launch {
            while (isActive && isConnected()) {
                try {
                    // High priority PIDs - every 100ms
                    launch {
                        while (isActive && isConnected()) {
                            executeBatch(highPriorityPids)
                            delay(100)
                        }
                    }
                    
                    // Normal priority PIDs - every 500ms
                    launch {
                        while (isActive && isConnected()) {
                            executeBatch(normalPriorityPids)
                            delay(500)
                        }
                    }
                    
                    // Low priority PIDs - every 2000ms
                    launch {
                        while (isActive && isConnected()) {
                            executeBatch(lowPriorityPids)
                            delay(2000)
                        }
                    }
                    
                    delay(Long.MAX_VALUE) // Keep the parent job alive
                } catch (e: CancellationException) {
                    Log.d(TAG, "Adaptive monitoring cancelled")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Adaptive monitoring error: ${e.message}")
                    delay(1000) // Wait before retrying
                }
            }
        }
    }
    
    private fun startCommandProcessor() {
        processingJob = scope.launch {
            isProcessing = true
            
            try {
                for (command in commandQueue) {
                    if (!isActive) break
                    
                    try {
                        executeCommand(command)
                    } catch (e: Exception) {
                        Log.e(TAG, "Command processing error: ${e.message}")
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Command processor cancelled")
            } finally {
                isProcessing = false
            }
        }
    }
    
    private fun getPidPriority(pid: String): Int {
        return when (pid.uppercase()) {
            "0C", "0D" -> 1 // RPM, Speed - highest priority
            "05", "2F" -> 2 // Temperature, Fuel - medium priority
            "11", "0F" -> 3 // Throttle, Intake Air Temp - low priority
            else -> 4 // Unknown PIDs - lowest priority
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.launch {
            try {
                processingJob?.cancel()
                commandQueue.close()
                disconnect()
                scope.cancel()
                Log.i(TAG, "SafeObdManager cleaned up")
            } catch (e: Exception) {
                Log.e(TAG, "Cleanup error: ${e.message}")
            }
        }
    }
}

/**
 * OBD command data class
 */
data class ObdCommand(
    val id: String = java.util.UUID.randomUUID().toString(),
    val pid: String,
    val type: CommandType = CommandType.READ_PID,
    val pids: List<String>? = null,
    val priority: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Command result data class
 */
data class CommandResult(
    val commandId: String,
    val pid: String,
    val result: Result<String>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Command types
 */
enum class CommandType {
    READ_PID,
    BATCH_READ,
    TEST_CONNECTION
}


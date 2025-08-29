package com.spacetec.diagnostic.obd

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.Log
import com.spacetec.diagnostic.error.ObdError
import com.spacetec.diagnostic.error.ObdErrorHandler

/**
 * Robust OBD client with enhanced error recovery and connection management
 */
class RobustObdClient(
    private val transport: ObdTransport,
    private val errorHandler: ObdErrorHandler
) {
    private var retryCount = 0
    private val maxRetries = 3
    private val commandMutex = Mutex()
    private var isConnected = false
    
    companion object {
        private const val TAG = "RobustObdClient"
        private const val DEFAULT_TIMEOUT = 1500L
    }
    
    /**
     * Read PID with automatic retry and recovery
     */
    suspend fun readPidWithRecovery(hex: String): Result<String> {
        return commandMutex.withLock {
            retry(maxRetries) { attempt ->
                try {
                    Log.d(TAG, "Reading PID $hex (attempt ${attempt + 1})")
                    
                    val result = withTimeout(DEFAULT_TIMEOUT) {
                        transport.write("01$hex")
                        transport.read()
                    }
                    
                    if (result.isNotEmpty() && !isErrorResponse(result)) {
                        isConnected = true
                        retryCount = 0
                        Result.success(result)
                    } else {
                        throw ObdTimeoutException("Empty or error response for PID $hex")
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "Timeout reading PID $hex")
                    handleError(ObdError.CommandTimeout(hex), attempt)
                    throw ObdTimeoutException("Timeout reading PID $hex")
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading PID $hex: ${e.message}")
                    handleError(ObdError.ConnectionLost, attempt)
                    throw e
                }
            }
        }
    }
    
    /**
     * Batch read multiple PIDs efficiently
     */
    suspend fun readMultiplePids(pids: List<String>): Map<String, Result<String>> {
        return commandMutex.withLock {
            val results = mutableMapOf<String, Result<String>>()
            
            for (pid in pids) {
                results[pid] = try {
                    readPidWithRecovery(pid)
                } catch (e: Exception) {
                    Result.failure(e)
                }
                
                // Small delay between commands to avoid overwhelming the ECU
                delay(50)
            }
            
            results
        }
    }
    
    /**
     * Test connection health
     */
    suspend fun testConnection(): Boolean {
        return try {
            val result = readPidWithRecovery("0C") // RPM is usually supported
            result.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed: ${e.message}")
            false
        }
    }
    
    /**
     * Reconnect to the OBD device
     */
    suspend fun reconnect(): Boolean {
        return try {
            Log.i(TAG, "Attempting to reconnect...")
            transport.close()
            delay(1000) // Wait before reconnecting
            
            val connected = transport.open()
            if (connected) {
                isConnected = true
                retryCount = 0
                Log.i(TAG, "Reconnection successful")
            } else {
                Log.w(TAG, "Reconnection failed")
            }
            connected
        } catch (e: Exception) {
            Log.e(TAG, "Reconnection error: ${e.message}")
            isConnected = false
            false
        }
    }
    
    /**
     * Get connection status
     */
    fun isConnected(): Boolean = isConnected
    
    /**
     * Close connection and cleanup
     */
    suspend fun close() {
        try {
            transport.close()
            isConnected = false
            Log.i(TAG, "Connection closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing connection: ${e.message}")
        }
    }
    
    private suspend fun handleError(error: ObdError, attempt: Int) {
        val action = errorHandler.handleError(error)
        
        when (action) {
            is com.spacetec.diagnostic.error.ErrorAction.Reconnect -> {
                if (attempt < maxRetries - 1) {
                    reconnect()
                }
            }
            is com.spacetec.diagnostic.error.ErrorAction.RetryCommand -> {
                // Will be retried by the retry mechanism
                delay((500 * (attempt + 1)).toLong()) // Exponential backoff
            }
            else -> {
                // Handle other error actions
                Log.w(TAG, "Error action: $action")
            }
        }
    }
    
    private fun shouldReconnect(exception: Exception): Boolean {
        return when (exception) {
            is ObdTimeoutException -> retryCount >= 2
            is java.io.IOException -> true
            else -> false
        }
    }
    
    private fun isErrorResponse(response: String): Boolean {
        val cleanResponse = response.trim().uppercase()
        return cleanResponse.startsWith("NO DATA") ||
                cleanResponse.startsWith("?") ||
                cleanResponse.startsWith("ERROR") ||
                cleanResponse.contains("UNABLE TO CONNECT") ||
                cleanResponse.contains("BUS INIT")
    }
    
    private suspend fun <T> retry(
        maxAttempts: Int,
        delayMs: Long = 500,
        block: suspend (attempt: Int) -> T
    ): T {
        repeat(maxAttempts - 1) { attempt ->
            try {
                return block(attempt)
            } catch (e: Exception) {
                Log.w(TAG, "Attempt ${attempt + 1} failed: ${e.message}")
                delay(delayMs * (attempt + 1)) // Exponential backoff
            }
        }
        
        // Last attempt
        return block(maxAttempts - 1)
    }
}

/**
 * Custom exception for OBD timeouts
 */
class ObdTimeoutException(message: String) : Exception(message)


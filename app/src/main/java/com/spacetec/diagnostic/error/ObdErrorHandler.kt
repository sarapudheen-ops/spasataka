package com.spacetec.diagnostic.error

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Sealed class representing all possible OBD errors
 */
sealed class ObdError {
    object DeviceNotFound : ObdError()
    object ConnectionLost : ObdError()
    object ProtocolMismatch : ObdError()
    data class CommandTimeout(val command: String) : ObdError()
    data class ParseError(val rawData: String) : ObdError()
    data class InvalidResponse(val response: String) : ObdError()
    data class AdapterNotSupported(val adapterType: String) : ObdError()
    data class BluetoothError(val message: String) : ObdError()
    data class PermissionDenied(val permission: String) : ObdError()
    object UnknownError : ObdError()
}

/**
 * Actions that can be taken in response to errors
 */
sealed class ErrorAction {
    object Reconnect : ErrorAction()
    object ShowDeviceSelector : ErrorAction()
    data class RetryCommand(val command: String) : ErrorAction()
    object ShowErrorDialog : ErrorAction()
    object RequestPermissions : ErrorAction()
    data class ShowMessage(val message: String) : ErrorAction()
    object RestartAdapter : ErrorAction()
    object SwitchProtocol : ErrorAction()
}

/**
 * Handles OBD errors and determines appropriate recovery actions
 */
class ObdErrorHandler {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _errorEvents = MutableSharedFlow<ErrorEvent>()
    val errorEvents = _errorEvents.asSharedFlow()
    
    private var retryCount = mutableMapOf<String, Int>()
    private val maxRetries = 3
    
    companion object {
        private const val TAG = "ObdErrorHandler"
    }
    
    /**
     * Handle an OBD error and return appropriate action
     */
    fun handleError(error: ObdError): ErrorAction {
        Log.w(TAG, "Handling error: $error")
        
        val action = when (error) {
            ObdError.ConnectionLost -> {
                incrementRetryCount("connection")
                if (getRetryCount("connection") <= maxRetries) {
                    ErrorAction.Reconnect
                } else {
                    resetRetryCount("connection")
                    ErrorAction.ShowDeviceSelector
                }
            }
            
            ObdError.DeviceNotFound -> {
                ErrorAction.ShowDeviceSelector
            }
            
            is ObdError.CommandTimeout -> {
                incrementRetryCount(error.command)
                if (getRetryCount(error.command) <= maxRetries) {
                    ErrorAction.RetryCommand(error.command)
                } else {
                    resetRetryCount(error.command)
                    ErrorAction.ShowMessage("Command ${error.command} failed after $maxRetries attempts")
                }
            }
            
            is ObdError.ParseError -> {
                Log.e(TAG, "Parse error with data: ${error.rawData}")
                ErrorAction.ShowMessage("Invalid data received: ${error.rawData.take(20)}...")
            }
            
            ObdError.ProtocolMismatch -> {
                ErrorAction.SwitchProtocol
            }
            
            is ObdError.AdapterNotSupported -> {
                ErrorAction.ShowMessage("Adapter ${error.adapterType} is not supported")
            }
            
            is ObdError.BluetoothError -> {
                ErrorAction.ShowMessage("Bluetooth error: ${error.message}")
            }
            
            is ObdError.PermissionDenied -> {
                ErrorAction.RequestPermissions
            }
            
            is ObdError.InvalidResponse -> {
                ErrorAction.ShowMessage("Invalid response: ${error.response}")
            }
            
            ObdError.UnknownError -> {
                ErrorAction.ShowErrorDialog
            }
        }
        
        // Emit error event for UI handling
        scope.launch {
            _errorEvents.emit(ErrorEvent(error, action, System.currentTimeMillis()))
        }
        
        return action
    }
    
    /**
     * Handle error with automatic recovery attempt
     */
    suspend fun handleErrorWithRecovery(
        error: ObdError,
        recoveryAction: suspend () -> Boolean
    ): Boolean {
        val action = handleError(error)
        
        return when (action) {
            ErrorAction.Reconnect -> {
                Log.i(TAG, "Attempting automatic reconnection")
                try {
                    recoveryAction()
                } catch (e: Exception) {
                    Log.e(TAG, "Recovery failed: ${e.message}")
                    false
                }
            }
            
            is ErrorAction.RetryCommand -> {
                Log.i(TAG, "Retrying command: ${action.command}")
                try {
                    recoveryAction()
                } catch (e: Exception) {
                    Log.e(TAG, "Command retry failed: ${e.message}")
                    false
                }
            }
            
            ErrorAction.RestartAdapter -> {
                Log.i(TAG, "Restarting adapter")
                try {
                    recoveryAction()
                } catch (e: Exception) {
                    Log.e(TAG, "Adapter restart failed: ${e.message}")
                    false
                }
            }
            
            else -> false // No automatic recovery for other actions
        }
    }
    
    /**
     * Get error statistics
     */
    fun getErrorStatistics(): ErrorStatistics {
        return ErrorStatistics(
            totalRetries = retryCount.values.sum(),
            commandRetries = retryCount.toMap(),
            mostFailedCommand = retryCount.maxByOrNull { it.value }?.key
        )
    }
    
    /**
     * Reset all retry counters
     */
    fun resetAllRetries() {
        retryCount.clear()
        Log.i(TAG, "Reset all retry counters")
    }
    
    /**
     * Check if error is recoverable
     */
    fun isRecoverable(error: ObdError): Boolean {
        return when (error) {
            ObdError.ConnectionLost,
            is ObdError.CommandTimeout,
            ObdError.ProtocolMismatch,
            is ObdError.BluetoothError -> true
            
            ObdError.DeviceNotFound,
            is ObdError.ParseError,
            is ObdError.AdapterNotSupported,
            is ObdError.PermissionDenied,
            is ObdError.InvalidResponse,
            ObdError.UnknownError -> false
        }
    }
    
    /**
     * Get user-friendly error message
     */
    fun getErrorMessage(error: ObdError): String {
        return when (error) {
            ObdError.DeviceNotFound -> "🔍 No OBD device found. Please check connection."
            ObdError.ConnectionLost -> "📡 Connection lost. Attempting to reconnect..."
            ObdError.ProtocolMismatch -> "⚙️ Protocol mismatch. Trying different protocol..."
            is ObdError.CommandTimeout -> "⏱️ Command timeout: ${error.command}"
            is ObdError.ParseError -> "❌ Data parsing error"
            is ObdError.InvalidResponse -> "⚠️ Invalid response received"
            is ObdError.AdapterNotSupported -> "🔧 Adapter not supported: ${error.adapterType}"
            is ObdError.BluetoothError -> "📶 Bluetooth error: ${error.message}"
            is ObdError.PermissionDenied -> "🔒 Permission required: ${error.permission}"
            ObdError.UnknownError -> "❓ Unknown error occurred"
        }
    }
    
    private fun incrementRetryCount(key: String) {
        retryCount[key] = (retryCount[key] ?: 0) + 1
    }
    
    private fun getRetryCount(key: String): Int {
        return retryCount[key] ?: 0
    }
    
    private fun resetRetryCount(key: String) {
        retryCount.remove(key)
    }
    
    fun cleanup() {
        scope.cancel()
    }
}

/**
 * Error event for UI handling
 */
data class ErrorEvent(
    val error: ObdError,
    val action: ErrorAction,
    val timestamp: Long
)

/**
 * Error statistics for monitoring
 */
data class ErrorStatistics(
    val totalRetries: Int,
    val commandRetries: Map<String, Int>,
    val mostFailedCommand: String?
)

/**
 * Extension functions for error handling
 */
fun ObdError.isTimeout(): Boolean = this is ObdError.CommandTimeout

fun ObdError.isConnectionIssue(): Boolean = when (this) {
    ObdError.DeviceNotFound,
    ObdError.ConnectionLost,
    is ObdError.BluetoothError -> true
    else -> false
}

fun ObdError.requiresUserAction(): Boolean = when (this) {
    ObdError.DeviceNotFound,
    is ObdError.AdapterNotSupported,
    is ObdError.PermissionDenied -> true
    else -> false
}

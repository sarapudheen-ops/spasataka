package com.spacetec.diagnostic.monitoring

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import android.util.Log
import com.spacetec.diagnostic.obd.SafeObdManager
import com.spacetec.diagnostic.config.ConfigManager

/**
 * Smart PID monitor with adaptive polling and priority-based scheduling
 */
class SmartPidMonitor(
    private val obdManager: SafeObdManager,
    private val configManager: ConfigManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // PID categories with different polling intervals
    private val highPriorityPids = listOf("0C", "0D") // RPM, Speed - 100ms
    private val normalPriorityPids = listOf("05", "2F") // Temp, Fuel - 500ms
    private val lowPriorityPids = listOf("11", "0F", "04") // Throttle, Intake Air Temp, Engine Load - 2000ms
    
    // Monitoring state
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    private val _pidData = MutableSharedFlow<PidDataPoint>()
    val pidData: SharedFlow<PidDataPoint> = _pidData.asSharedFlow()
    
    private var monitoringJobs = mutableListOf<Job>()
    
    companion object {
        private const val TAG = "SmartPidMonitor"
    }
    
    /**
     * Start adaptive monitoring with priority-based polling
     */
    fun startAdaptiveMonitoring() {
        if (_isMonitoring.value) return
        
        _isMonitoring.value = true
        Log.i(TAG, "Starting adaptive PID monitoring")
        
        // High priority monitoring (100ms)
        monitoringJobs.add(scope.launch {
            while (isActive && _isMonitoring.value) {
                try {
                    monitorPidGroup(highPriorityPids, PidPriority.HIGH)
                    delay(100)
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "High priority monitoring error: ${e.message}")
                    delay(500)
                }
            }
        })
        
        // Normal priority monitoring (500ms)
        monitoringJobs.add(scope.launch {
            while (isActive && _isMonitoring.value) {
                try {
                    monitorPidGroup(normalPriorityPids, PidPriority.NORMAL)
                    delay(500)
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Normal priority monitoring error: ${e.message}")
                    delay(1000)
                }
            }
        })
        
        // Low priority monitoring (2000ms)
        monitoringJobs.add(scope.launch {
            while (isActive && _isMonitoring.value) {
                try {
                    monitorPidGroup(lowPriorityPids, PidPriority.LOW)
                    delay(2000)
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Low priority monitoring error: ${e.message}")
                    delay(2000)
                }
            }
        })
    }
    
    /**
     * Stop monitoring and cleanup
     */
    fun stopMonitoring() {
        _isMonitoring.value = false
        monitoringJobs.forEach { it.cancel() }
        monitoringJobs.clear()
        Log.i(TAG, "Stopped adaptive PID monitoring")
    }
    
    /**
     * Monitor specific PID group
     */
    private suspend fun monitorPidGroup(pids: List<String>, priority: PidPriority) {
        if (!obdManager.isConnected()) return
        
        val results = obdManager.executeBatch(pids)
        
        results.forEach { (pid, result) ->
            result.onSuccess { response ->
                val dataPoint = PidDataPoint(
                    pid = pid,
                    rawResponse = response,
                    parsedValue = parseResponse(pid, response),
                    priority = priority,
                    timestamp = System.currentTimeMillis()
                )
                
                _pidData.tryEmit(dataPoint)
            }.onFailure { error ->
                Log.w(TAG, "Failed to read PID $pid: ${error.message}")
            }
        }
    }
    
    /**
     * Parse OBD response based on PID type
     */
    private fun parseResponse(pid: String, response: String): Double {
        return try {
            val bytes = response.trim().split(" ")
            if (bytes.size < 3) return 0.0
            
            when (pid.uppercase()) {
                "0C" -> { // RPM
                    val a = Integer.parseInt(bytes[2], 16)
                    val b = Integer.parseInt(bytes[3], 16)
                    ((a * 256) + b) / 4.0
                }
                "0D" -> { // Speed
                    Integer.parseInt(bytes[2], 16).toDouble()
                }
                "05" -> { // Coolant Temperature
                    Integer.parseInt(bytes[2], 16) - 40.0
                }
                "2F" -> { // Fuel Level
                    (Integer.parseInt(bytes[2], 16) * 100.0) / 255.0
                }
                "11" -> { // Throttle Position
                    (Integer.parseInt(bytes[2], 16) * 100.0) / 255.0
                }
                "0F" -> { // Intake Air Temperature
                    Integer.parseInt(bytes[2], 16) - 40.0
                }
                "04" -> { // Engine Load
                    (Integer.parseInt(bytes[2], 16) * 100.0) / 255.0
                }
                else -> Integer.parseInt(bytes[2], 16).toDouble()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing PID $pid response: ${e.message}")
            0.0
        }
    }
    
    /**
     * Get monitoring statistics
     */
    fun getMonitoringStats(): MonitoringStats {
        return MonitoringStats(
            isActive = _isMonitoring.value,
            activeJobs = monitoringJobs.size,
            highPriorityPids = highPriorityPids,
            normalPriorityPids = normalPriorityPids,
            lowPriorityPids = lowPriorityPids
        )
    }
    
    /**
     * Add custom PID to monitoring
     */
    fun addCustomPid(pid: String, priority: PidPriority) {
        when (priority) {
            PidPriority.HIGH -> {
                if (!highPriorityPids.contains(pid)) {
                    (highPriorityPids as MutableList).add(pid)
                }
            }
            PidPriority.NORMAL -> {
                if (!normalPriorityPids.contains(pid)) {
                    (normalPriorityPids as MutableList).add(pid)
                }
            }
            PidPriority.LOW -> {
                if (!lowPriorityPids.contains(pid)) {
                    (lowPriorityPids as MutableList).add(pid)
                }
            }
        }
        Log.i(TAG, "Added custom PID $pid with priority $priority")
    }
    
    /**
     * Remove PID from monitoring
     */
    fun removePid(pid: String) {
        (highPriorityPids as MutableList).remove(pid)
        (normalPriorityPids as MutableList).remove(pid)
        (lowPriorityPids as MutableList).remove(pid)
        Log.i(TAG, "Removed PID $pid from monitoring")
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopMonitoring()
        scope.cancel()
        Log.i(TAG, "SmartPidMonitor cleaned up")
    }
}

/**
 * PID data point with parsed value
 */
data class PidDataPoint(
    val pid: String,
    val rawResponse: String,
    val parsedValue: Double,
    val priority: PidPriority,
    val timestamp: Long
)

/**
 * PID priority levels
 */
enum class PidPriority {
    HIGH,    // 100ms interval
    NORMAL,  // 500ms interval
    LOW      // 2000ms interval
}

/**
 * Monitoring statistics
 */
data class MonitoringStats(
    val isActive: Boolean,
    val activeJobs: Int,
    val highPriorityPids: List<String>,
    val normalPriorityPids: List<String>,
    val lowPriorityPids: List<String>
)

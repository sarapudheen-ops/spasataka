package com.spacetec.diagnostic

import com.spacetec.connection.ObdConnectionService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

/**
 * 📊 Live PID Data Manager
 * Handles real-time Parameter ID (PID) data streaming from vehicle ECU
 */
class LivePidManager(
    private val connectionService: ObdConnectionService
) : CoroutineScope {

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.IO + job

    data class PidData(
        val pid: String,
        val name: String,
        val value: Double,
        val unit: String,
        val timestamp: Long = System.currentTimeMillis(),
        val rawValue: String = "",
        val isValid: Boolean = true
    )

    data class PidDefinition(
        val pid: String,
        val name: String,
        val unit: String,
        val formula: String,
        val minValue: Double = 0.0,
        val maxValue: Double = 255.0,
        val description: String = ""
    )

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _pidDataStream = MutableSharedFlow<PidData>(replay = 1, extraBufferCapacity = 100)
    val pidDataStream: SharedFlow<PidData> = _pidDataStream.asSharedFlow()

    private val _activePids = MutableStateFlow<Set<String>>(emptySet())
    val activePids: StateFlow<Set<String>> = _activePids.asStateFlow()

    private var streamingJob: Job? = null
    private val pidHistory = mutableMapOf<String, MutableList<PidData>>()
    private val maxHistorySize = 1000

    // Common OBD-II PIDs
    private val standardPids = mapOf(
        "01 0C" to PidDefinition("01 0C", "Engine RPM", "rpm", "((A*256)+B)/4", 0.0, 16383.75, "Engine revolutions per minute"),
        "01 0D" to PidDefinition("01 0D", "Vehicle Speed", "km/h", "A", 0.0, 255.0, "Vehicle speed"),
        "01 05" to PidDefinition("01 05", "Engine Coolant Temperature", "°C", "A-40", -40.0, 215.0, "Engine coolant temperature"),
        "01 0F" to PidDefinition("01 0F", "Intake Air Temperature", "°C", "A-40", -40.0, 215.0, "Intake air temperature"),
        "01 10" to PidDefinition("01 10", "MAF Air Flow Rate", "g/s", "((A*256)+B)/100", 0.0, 655.35, "Mass air flow sensor air flow rate"),
        "01 11" to PidDefinition("01 11", "Throttle Position", "%", "A*100/255", 0.0, 100.0, "Throttle position"),
        "01 04" to PidDefinition("01 04", "Engine Load", "%", "A*100/255", 0.0, 100.0, "Calculated engine load"),
        "01 06" to PidDefinition("01 06", "Short Term Fuel Trim Bank 1", "%", "(A-128)*100/128", -100.0, 99.22, "Short term fuel trim"),
        "01 07" to PidDefinition("01 07", "Long Term Fuel Trim Bank 1", "%", "(A-128)*100/128", -100.0, 99.22, "Long term fuel trim"),
        "01 0B" to PidDefinition("01 0B", "Intake Manifold Pressure", "kPa", "A", 0.0, 255.0, "Intake manifold absolute pressure"),
        "01 42" to PidDefinition("01 42", "Control Module Voltage", "V", "((A*256)+B)/1000", 0.0, 65.535, "Control module power supply voltage"),
        "01 2F" to PidDefinition("01 2F", "Fuel Tank Level", "%", "A*100/255", 0.0, 100.0, "Fuel tank level input"),
        "01 46" to PidDefinition("01 46", "Ambient Air Temperature", "°C", "A-40", -40.0, 215.0, "Ambient air temperature"),
        "01 5C" to PidDefinition("01 5C", "Engine Oil Temperature", "°C", "A-40", -40.0, 215.0, "Engine oil temperature"),
        "01 0E" to PidDefinition("01 0E", "Timing Advance", "°", "(A/2)-64", -64.0, 63.5, "Timing advance")
    )

    /**
     * Start streaming specified PIDs
     */
    suspend fun startStreaming(pids: Set<String>, intervalMs: Long = 500): Result<Unit> {
        return try {
            if (_isStreaming.value) {
                stopStreaming()
            }

            _activePids.value = pids
            _isStreaming.value = true

            streamingJob = launch {
                while (isActive && _isStreaming.value) {
                    pids.forEach { pid ->
                        if (isActive) {
                            readPid(pid)
                        }
                    }
                    delay(intervalMs)
                }
            }

            Result.success(Unit)

        } catch (e: Exception) {
            _isStreaming.value = false
            Result.failure(e)
        }
    }

    /**
     * Stop PID streaming
     */
    fun stopStreaming() {
        _isStreaming.value = false
        streamingJob?.cancel()
        streamingJob = null
    }

    /**
     * Read single PID value
     */
    suspend fun readSinglePid(pid: String): Result<PidData> {
        return try {
            val result = connectionService.sendCommand(pid)
            
            if (result.isSuccess) {
                val response = result.getOrNull() ?: ""
                val pidData = parsePidResponse(pid, response)
                
                if (pidData.isValid) {
                    _pidDataStream.emit(pidData)
                    addToHistory(pidData)
                    Result.success(pidData)
                } else {
                    Result.failure(Exception("Invalid PID response"))
                }
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("PID read failed"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get supported PIDs from vehicle
     */
    suspend fun getSupportedPids(): Result<Set<String>> {
        return try {
            val supportedPids = mutableSetOf<String>()

            // Check PID support groups
            val supportGroups = listOf("01 00", "01 20", "01 40", "01 60", "01 80", "01 A0", "01 C0")

            for (group in supportGroups) {
                val result = connectionService.sendCommand(group)
                if (result.isSuccess) {
                    val response = result.getOrNull() ?: ""
                    val pids = parseSupportedPids(group, response)
                    supportedPids.addAll(pids)
                }
            }

            Result.success(supportedPids)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get PID history for specific PID
     */
    fun getPidHistory(pid: String, maxEntries: Int = 100): List<PidData> {
        return pidHistory[pid]?.takeLast(maxEntries) ?: emptyList()
    }

    /**
     * Get all available PID definitions
     */
    fun getAvailablePids(): Map<String, PidDefinition> = standardPids

    /**
     * Clear PID history
     */
    fun clearHistory(pid: String? = null) {
        if (pid != null) {
            pidHistory[pid]?.clear()
        } else {
            pidHistory.clear()
        }
    }

    /**
     * Export PID data to CSV format
     */
    fun exportPidData(pids: Set<String>? = null, maxEntries: Int = 1000): String {
        val sb = StringBuilder()
        sb.appendLine("# SpaceTec Live PID Data Export")
        sb.appendLine("Timestamp,PID,Name,Value,Unit,Raw Value")

        val pidsToExport = pids ?: pidHistory.keys
        
        pidsToExport.forEach { pid ->
            val history = getPidHistory(pid, maxEntries)
            history.forEach { data ->
                sb.appendLine("${data.timestamp},${data.pid},\"${data.name}\",${data.value},${data.unit},\"${data.rawValue}\"")
            }
        }

        return sb.toString()
    }

    private suspend fun readPid(pid: String) {
        try {
            val result = readSinglePid(pid)
            // Result is already emitted in readSinglePid
        } catch (e: Exception) {
            // Log error but continue streaming
        }
    }

    private fun parsePidResponse(pid: String, response: String): PidData {
        try {
            val pidDef = standardPids[pid] ?: return createInvalidPidData(pid, response)
            
            val cleanResponse = response.replace(" ", "").replace("\r", "").replace("\n", "")
            
            // Remove response header (first 4 characters typically)
            val dataHex = if (cleanResponse.length > 4) {
                cleanResponse.substring(4)
            } else {
                return createInvalidPidData(pid, response)
            }

            val value = calculatePidValue(pidDef, dataHex)

            return PidData(
                pid = pid,
                name = pidDef.name,
                value = value,
                unit = pidDef.unit,
                rawValue = response,
                isValid = true
            )

        } catch (e: Exception) {
            return createInvalidPidData(pid, response)
        }
    }

    private fun calculatePidValue(pidDef: PidDefinition, dataHex: String): Double {
        return when (pidDef.pid) {
            "01 0C" -> { // Engine RPM
                if (dataHex.length >= 4) {
                    val a = Integer.parseInt(dataHex.substring(0, 2), 16)
                    val b = Integer.parseInt(dataHex.substring(2, 4), 16)
                    ((a * 256) + b) / 4.0
                } else 0.0
            }
            "01 0D" -> { // Vehicle Speed
                if (dataHex.length >= 2) {
                    Integer.parseInt(dataHex.substring(0, 2), 16).toDouble()
                } else 0.0
            }
            "01 05", "01 0F", "01 46", "01 5C" -> { // Temperature sensors
                if (dataHex.length >= 2) {
                    Integer.parseInt(dataHex.substring(0, 2), 16) - 40.0
                } else 0.0
            }
            "01 10" -> { // MAF Air Flow Rate
                if (dataHex.length >= 4) {
                    val a = Integer.parseInt(dataHex.substring(0, 2), 16)
                    val b = Integer.parseInt(dataHex.substring(2, 4), 16)
                    ((a * 256) + b) / 100.0
                } else 0.0
            }
            "01 11", "01 04", "01 2F" -> { // Percentage values
                if (dataHex.length >= 2) {
                    val a = Integer.parseInt(dataHex.substring(0, 2), 16)
                    (a * 100.0) / 255.0
                } else 0.0
            }
            "01 06", "01 07" -> { // Fuel trim
                if (dataHex.length >= 2) {
                    val a = Integer.parseInt(dataHex.substring(0, 2), 16)
                    ((a - 128) * 100.0) / 128.0
                } else 0.0
            }
            "01 0B" -> { // Intake Manifold Pressure
                if (dataHex.length >= 2) {
                    Integer.parseInt(dataHex.substring(0, 2), 16).toDouble()
                } else 0.0
            }
            "01 42" -> { // Control Module Voltage
                if (dataHex.length >= 4) {
                    val a = Integer.parseInt(dataHex.substring(0, 2), 16)
                    val b = Integer.parseInt(dataHex.substring(2, 4), 16)
                    ((a * 256) + b) / 1000.0
                } else 0.0
            }
            "01 0E" -> { // Timing Advance
                if (dataHex.length >= 2) {
                    val a = Integer.parseInt(dataHex.substring(0, 2), 16)
                    (a / 2.0) - 64.0
                } else 0.0
            }
            else -> {
                // Generic single byte value
                if (dataHex.length >= 2) {
                    Integer.parseInt(dataHex.substring(0, 2), 16).toDouble()
                } else 0.0
            }
        }
    }

    private fun parseSupportedPids(group: String, response: String): Set<String> {
        val supportedPids = mutableSetOf<String>()
        
        try {
            val cleanResponse = response.replace(" ", "").replace("\r", "").replace("\n", "")
            
            if (cleanResponse.length >= 12) { // 4 bytes response header + 4 bytes data
                val dataHex = cleanResponse.substring(4, 12)
                val supportBits = Integer.parseUnsignedInt(dataHex, 16)
                
                val baseGroup = when (group) {
                    "01 00" -> 0x01
                    "01 20" -> 0x21
                    "01 40" -> 0x41
                    "01 60" -> 0x61
                    "01 80" -> 0x81
                    "01 A0" -> 0xA1
                    "01 C0" -> 0xC1
                    else -> 0x01
                }
                
                for (i in 0..31) {
                    if ((supportBits and (1 shl (31 - i))) != 0) {
                        val pidNum = baseGroup + i
                        val pidHex = String.format("%02X", pidNum)
                        supportedPids.add("01 $pidHex")
                    }
                }
            }
            
        } catch (e: Exception) {
            // Handle parsing errors gracefully
        }
        
        return supportedPids
    }

    private fun createInvalidPidData(pid: String, response: String): PidData {
        return PidData(
            pid = pid,
            name = standardPids[pid]?.name ?: "Unknown PID",
            value = 0.0,
            unit = standardPids[pid]?.unit ?: "",
            rawValue = response,
            isValid = false
        )
    }

    private fun addToHistory(pidData: PidData) {
        val history = pidHistory.getOrPut(pidData.pid) { mutableListOf() }
        history.add(pidData)
        
        // Limit history size
        if (history.size > maxHistorySize) {
            history.removeAt(0)
        }
    }

    fun cleanup() {
        stopStreaming()
        job.cancel()
        pidHistory.clear()
    }
}

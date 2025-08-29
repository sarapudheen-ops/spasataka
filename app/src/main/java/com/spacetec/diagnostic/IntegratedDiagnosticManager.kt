package com.spacetec.diagnostic

import android.content.Context
import com.spacetec.connection.ObdConnectionService
import com.spacetec.connection.transport.ObdTransport
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

/**
 * 🚀 Integrated Diagnostic Manager
 * Orchestrates all Phase 1 diagnostic features: connection, DTCs, live PIDs, and logging
 */
class IntegratedDiagnosticManager(
    private val context: Context,
    private val connectionService: ObdConnectionService
) : CoroutineScope {

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.Main + job

    // Component managers
    private val dtcManager = DtcManager(connectionService)
    private val pidManager = LivePidManager(connectionService)
    private val dataLogger = DataLogger(context)

    // State management
    private val _diagnosticState = MutableStateFlow(DiagnosticState.IDLE)
    val diagnosticState: StateFlow<DiagnosticState> = _diagnosticState.asStateFlow()

    private val _diagnosticResults = MutableSharedFlow<DiagnosticResult>(replay = 1, extraBufferCapacity = 10)
    val diagnosticResults: SharedFlow<DiagnosticResult> = _diagnosticResults.asSharedFlow()

    enum class DiagnosticState {
        IDLE,
        CONNECTING,
        CONNECTED,
        SCANNING_DTCS,
        STREAMING_PIDS,
        EXPORTING_DATA,
        ERROR,
        DISCONNECTED
    }

    sealed class DiagnosticResult {
        data class ConnectionResult(val success: Boolean, val message: String) : DiagnosticResult()
        data class DtcScanResult(val dtcs: List<DtcManager.DiagnosticTroubleCode>) : DiagnosticResult()
        data class PidDataResult(val pidData: LivePidManager.PidData) : DiagnosticResult()
        data class ExportResult(val success: Boolean, val filePath: String?, val message: String) : DiagnosticResult()
        data class ErrorResult(val error: String, val isRecoverable: Boolean) : DiagnosticResult()
    }

    data class DiagnosticSession(
        val sessionId: String,
        val startTime: Long,
        val vehicleInfo: DataLogger.VehicleInfo? = null,
        val connectionConfig: ObdTransport.TransportConfig? = null,
        val activePids: Set<String> = emptySet(),
        val dtcCount: Int = 0,
        val isLogging: Boolean = false
    )

    private val _currentSession = MutableStateFlow<DiagnosticSession?>(null)
    val currentSession: StateFlow<DiagnosticSession?> = _currentSession.asStateFlow()

    init {
        setupEventMonitoring()
    }

    /**
     * Start comprehensive diagnostic session
     */
    suspend fun startDiagnosticSession(
        connectionConfig: ObdTransport.TransportConfig,
        vehicleInfo: DataLogger.VehicleInfo? = null,
        enableLogging: Boolean = true
    ): Result<String> {
        return try {
            _diagnosticState.value = DiagnosticState.CONNECTING

            // Start logging session if enabled
            val sessionId = if (enableLogging) {
                val logResult = dataLogger.startSession(vehicleInfo)
                if (logResult.isFailure) {
                    throw Exception("Failed to start logging: ${logResult.exceptionOrNull()?.message}")
                }
                logResult.getOrThrow()
            } else {
                "session_${System.currentTimeMillis()}"
            }

            // Create session object
            val session = DiagnosticSession(
                sessionId = sessionId,
                startTime = System.currentTimeMillis(),
                vehicleInfo = vehicleInfo,
                connectionConfig = connectionConfig,
                isLogging = enableLogging
            )
            _currentSession.value = session

            // Connect to OBD adapter
            val connectionResult = connectionService.connectToAdapter(connectionConfig)
            if (connectionResult.isFailure) {
                throw Exception("Connection failed: ${connectionResult.exceptionOrNull()?.message}")
            }

            _diagnosticState.value = DiagnosticState.CONNECTED
            dataLogger.logConnectionEvent("Connected to ${connectionConfig.name ?: connectionConfig.address}")

            _diagnosticResults.emit(
                DiagnosticResult.ConnectionResult(
                    success = true,
                    message = "Connected to ${connectionConfig.name ?: connectionConfig.address}"
                )
            )

            Result.success(sessionId)

        } catch (e: Exception) {
            _diagnosticState.value = DiagnosticState.ERROR
            dataLogger.logError("Session start failed", e)
            
            _diagnosticResults.emit(
                DiagnosticResult.ErrorResult(
                    error = "Failed to start diagnostic session: ${e.message}",
                    isRecoverable = true
                )
            )
            
            Result.failure(e)
        }
    }

    /**
     * Perform full DTC scan
     */
    suspend fun performDtcScan(): Result<List<DtcManager.DiagnosticTroubleCode>> {
        return try {
            if (_diagnosticState.value != DiagnosticState.CONNECTED) {
                throw IllegalStateException("Not connected to vehicle")
            }

            _diagnosticState.value = DiagnosticState.SCANNING_DTCS
            dataLogger.logUserAction("Started DTC scan")

            val dtcResult = dtcManager.readDtcs()
            if (dtcResult.isFailure) {
                throw Exception("DTC scan failed: ${dtcResult.exceptionOrNull()?.message}")
            }

            val dtcs = dtcResult.getOrThrow()
            
            // Log DTCs
            dataLogger.logDtcData(dtcs)
            
            // Update session
            _currentSession.value = _currentSession.value?.copy(dtcCount = dtcs.size)

            _diagnosticState.value = DiagnosticState.CONNECTED

            _diagnosticResults.emit(DiagnosticResult.DtcScanResult(dtcs))

            Result.success(dtcs)

        } catch (e: Exception) {
            _diagnosticState.value = DiagnosticState.ERROR
            dataLogger.logError("DTC scan failed", e)
            
            _diagnosticResults.emit(
                DiagnosticResult.ErrorResult(
                    error = "DTC scan failed: ${e.message}",
                    isRecoverable = true
                )
            )
            
            Result.failure(e)
        }
    }

    /**
     * Clear all DTCs
     */
    suspend fun clearDtcs(): Result<Unit> {
        return try {
            if (_diagnosticState.value != DiagnosticState.CONNECTED) {
                throw IllegalStateException("Not connected to vehicle")
            }

            dataLogger.logUserAction("Clearing DTCs")
            
            val result = dtcManager.clearDtcs()
            if (result.isFailure) {
                throw Exception("Failed to clear DTCs: ${result.exceptionOrNull()?.message}")
            }

            dataLogger.logUserAction("DTCs cleared successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            dataLogger.logError("Failed to clear DTCs", e)
            Result.failure(e)
        }
    }

    /**
     * Start live PID streaming
     */
    suspend fun startPidStreaming(pids: Set<String>, intervalMs: Long = 500): Result<Unit> {
        return try {
            if (_diagnosticState.value != DiagnosticState.CONNECTED) {
                throw IllegalStateException("Not connected to vehicle")
            }

            _diagnosticState.value = DiagnosticState.STREAMING_PIDS
            
            // Update session
            _currentSession.value = _currentSession.value?.copy(activePids = pids)

            dataLogger.logUserAction("Started PID streaming: ${pids.joinToString(", ")}")

            val result = pidManager.startStreaming(pids, intervalMs)
            if (result.isFailure) {
                throw Exception("Failed to start PID streaming: ${result.exceptionOrNull()?.message}")
            }

            Result.success(Unit)

        } catch (e: Exception) {
            _diagnosticState.value = DiagnosticState.ERROR
            dataLogger.logError("PID streaming failed", e)
            
            _diagnosticResults.emit(
                DiagnosticResult.ErrorResult(
                    error = "PID streaming failed: ${e.message}",
                    isRecoverable = true
                )
            )
            
            Result.failure(e)
        }
    }

    /**
     * Stop PID streaming
     */
    fun stopPidStreaming() {
        pidManager.stopStreaming()
        _currentSession.value = _currentSession.value?.copy(activePids = emptySet())
        
        if (_diagnosticState.value == DiagnosticState.STREAMING_PIDS) {
            _diagnosticState.value = DiagnosticState.CONNECTED
        }
        
        launch {
            dataLogger.logUserAction("Stopped PID streaming")
        }
    }

    /**
     * Export diagnostic data
     */
    suspend fun exportDiagnosticData(format: DataLogger.ExportFormat): Result<String> {
        return try {
            val session = _currentSession.value ?: throw IllegalStateException("No active session")
            
            _diagnosticState.value = DiagnosticState.EXPORTING_DATA
            dataLogger.logUserAction("Exporting data as ${format.name}")

            val exportResult = dataLogger.exportSession(session.sessionId, format)
            if (exportResult.isFailure) {
                throw Exception("Export failed: ${exportResult.exceptionOrNull()?.message}")
            }

            val exportFile = exportResult.getOrThrow()
            val filePath = exportFile.absolutePath

            _diagnosticState.value = DiagnosticState.CONNECTED

            _diagnosticResults.emit(
                DiagnosticResult.ExportResult(
                    success = true,
                    filePath = filePath,
                    message = "Data exported to: $filePath"
                )
            )

            Result.success(filePath)

        } catch (e: Exception) {
            _diagnosticState.value = DiagnosticState.ERROR
            dataLogger.logError("Export failed", e)
            
            _diagnosticResults.emit(
                DiagnosticResult.ExportResult(
                    success = false,
                    filePath = null,
                    message = "Export failed: ${e.message}"
                )
            )
            
            Result.failure(e)
        }
    }

    /**
     * End diagnostic session
     */
    suspend fun endDiagnosticSession(): Result<DataLogger.LogSession> {
        return try {
            // Stop PID streaming if active
            if (_diagnosticState.value == DiagnosticState.STREAMING_PIDS) {
                stopPidStreaming()
            }

            // Disconnect from adapter
            connectionService.disconnect()
            _diagnosticState.value = DiagnosticState.DISCONNECTED

            // End logging session
            val session = _currentSession.value
            val logResult = if (session?.isLogging == true) {
                dataLogger.stopSession()
            } else {
                Result.success(
                    DataLogger.LogSession(
                        sessionId = session?.sessionId ?: "unknown",
                        startTime = session?.startTime ?: 0L,
                        endTime = System.currentTimeMillis()
                    )
                )
            }

            _currentSession.value = null
            _diagnosticState.value = DiagnosticState.IDLE

            logResult

        } catch (e: Exception) {
            dataLogger.logError("Session end failed", e)
            Result.failure(e)
        }
    }

    /**
     * Get diagnostic summary
     */
    fun getDiagnosticSummary(): DiagnosticSummary {
        val session = _currentSession.value
        val dtcs = dtcManager.dtcList.value
        val connectionQuality = connectionService.getConnectionQuality()
        val storageStats = dataLogger.getStorageStats()

        return DiagnosticSummary(
            sessionId = session?.sessionId,
            isConnected = connectionService.getConnectionState() == ObdTransport.ConnectionState.CONNECTED,
            connectionQuality = connectionQuality,
            totalDtcs = dtcs.size,
            activeDtcs = dtcs.count { it.status == DtcManager.DtcStatus.ACTIVE },
            pendingDtcs = dtcs.count { it.status == DtcManager.DtcStatus.PENDING },
            activePids = session?.activePids?.size ?: 0,
            isStreaming = pidManager.isStreaming.value,
            isLogging = session?.isLogging ?: false,
            storageStats = storageStats
        )
    }

    data class DiagnosticSummary(
        val sessionId: String?,
        val isConnected: Boolean,
        val connectionQuality: Float,
        val totalDtcs: Int,
        val activeDtcs: Int,
        val pendingDtcs: Int,
        val activePids: Int,
        val isStreaming: Boolean,
        val isLogging: Boolean,
        val storageStats: DataLogger.StorageStats
    )

    private fun setupEventMonitoring() {
        // Monitor connection events
        launch {
            connectionService.connectionEvents.collect { event ->
                when (event) {
                    is ObdConnectionService.ConnectionEvent.Connected -> {
                        _diagnosticState.value = DiagnosticState.CONNECTED
                        dataLogger.logConnectionEvent("Connection established")
                    }
                    is ObdConnectionService.ConnectionEvent.Disconnected -> {
                        _diagnosticState.value = DiagnosticState.DISCONNECTED
                        dataLogger.logConnectionEvent("Connection lost")
                    }
                    is ObdConnectionService.ConnectionEvent.Error -> {
                        _diagnosticState.value = DiagnosticState.ERROR
                        dataLogger.logError("Connection error: ${event.error.message}")
                    }
                    is ObdConnectionService.ConnectionEvent.DataReceived -> {
                        // Data received - connection is healthy
                    }
                    is ObdConnectionService.ConnectionEvent.QualityChanged -> {
                        if (event.quality < 0.5f) {
                            dataLogger.logConnectionEvent("Connection quality degraded: ${event.quality}")
                        }
                    }
                }
            }
        }

        // Monitor PID data
        launch {
            pidManager.pidDataStream.collect { pidData ->
                dataLogger.logPidData(pidData)
                _diagnosticResults.emit(DiagnosticResult.PidDataResult(pidData))
            }
        }
    }

    fun cleanup() {
        launch {
            endDiagnosticSession()
        }
        dtcManager.cleanup()
        pidManager.cleanup()
        dataLogger.cleanup()
        job.cancel()
    }
}

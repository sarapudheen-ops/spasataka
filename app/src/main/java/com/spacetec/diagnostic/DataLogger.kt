package com.spacetec.diagnostic

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * 📝 Data Logger for OBD diagnostic sessions
 * Handles logging, storage, and export of diagnostic data
 */
class DataLogger(
    private val context: Context
) : CoroutineScope {

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.IO + job

    data class LogSession(
        val sessionId: String,
        val startTime: Long,
        val endTime: Long? = null,
        val vehicleInfo: VehicleInfo? = null,
        val pidDataCount: Int = 0,
        val dtcCount: Int = 0,
        val filePath: String = ""
    )

    data class VehicleInfo(
        val vin: String? = null,
        val make: String? = null,
        val model: String? = null,
        val year: String? = null,
        val engine: String? = null
    )

    data class LogEntry(
        val timestamp: Long,
        val type: LogType,
        val data: String,
        val sessionId: String
    )

    enum class LogType {
        PID_DATA,
        DTC_DATA,
        CONNECTION_EVENT,
        ERROR,
        SYSTEM_INFO,
        USER_ACTION
    }

    enum class ExportFormat {
        CSV,
        JSON,
        XML,
        TXT
    }

    private val _currentSession = MutableStateFlow<LogSession?>(null)
    val currentSession: StateFlow<LogSession?> = _currentSession.asStateFlow()

    private val _isLogging = MutableStateFlow(false)
    val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()

    private val logEntries = mutableListOf<LogEntry>()
    private val maxLogEntries = 10000
    private var currentLogFile: File? = null

    /**
     * Start a new logging session
     */
    suspend fun startSession(vehicleInfo: VehicleInfo? = null): Result<String> {
        return try {
            val sessionId = generateSessionId()
            val startTime = System.currentTimeMillis()
            
            val session = LogSession(
                sessionId = sessionId,
                startTime = startTime,
                vehicleInfo = vehicleInfo
            )

            _currentSession.value = session
            _isLogging.value = true

            // Create log file
            currentLogFile = createLogFile(sessionId)
            
            // Write session header
            writeSessionHeader(session)
            
            // Log session start
            logEntry(LogType.SYSTEM_INFO, "Session started: $sessionId")

            Result.success(sessionId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stop current logging session
     */
    suspend fun stopSession(): Result<LogSession> {
        return try {
            val session = _currentSession.value ?: return Result.failure(Exception("No active session"))
            
            val endTime = System.currentTimeMillis()
            val completedSession = session.copy(
                endTime = endTime,
                pidDataCount = logEntries.count { it.type == LogType.PID_DATA && it.sessionId == session.sessionId },
                dtcCount = logEntries.count { it.type == LogType.DTC_DATA && it.sessionId == session.sessionId },
                filePath = currentLogFile?.absolutePath ?: ""
            )

            _currentSession.value = completedSession
            _isLogging.value = false

            // Write session footer
            writeSessionFooter(completedSession)

            // Log session end
            logEntry(LogType.SYSTEM_INFO, "Session ended: ${session.sessionId}")

            Result.success(completedSession)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Log PID data
     */
    suspend fun logPidData(pidData: LivePidManager.PidData) {
        val session = _currentSession.value ?: return
        
        val logData = "${pidData.pid},${pidData.name},${pidData.value},${pidData.unit},${pidData.rawValue}"
        logEntry(LogType.PID_DATA, logData, session.sessionId)
    }

    /**
     * Log DTC data
     */
    suspend fun logDtcData(dtcs: List<DtcManager.DiagnosticTroubleCode>) {
        val session = _currentSession.value ?: return
        
        dtcs.forEach { dtc ->
            val logData = "${dtc.code},${dtc.description},${dtc.type.name},${dtc.status.name},${dtc.severity.name}"
            logEntry(LogType.DTC_DATA, logData, session.sessionId)
        }
    }

    /**
     * Log connection events
     */
    suspend fun logConnectionEvent(event: String) {
        val session = _currentSession.value ?: return
        logEntry(LogType.CONNECTION_EVENT, event, session.sessionId)
    }

    /**
     * Log errors
     */
    suspend fun logError(error: String, exception: Throwable? = null) {
        val session = _currentSession.value
        val errorData = if (exception != null) {
            "$error: ${exception.message}"
        } else {
            error
        }
        logEntry(LogType.ERROR, errorData, session?.sessionId ?: "no_session")
    }

    /**
     * Log user actions
     */
    suspend fun logUserAction(action: String) {
        val session = _currentSession.value ?: return
        logEntry(LogType.USER_ACTION, action, session.sessionId)
    }

    /**
     * Export session data
     */
    suspend fun exportSession(sessionId: String, format: ExportFormat): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val sessionEntries = logEntries.filter { it.sessionId == sessionId }
                if (sessionEntries.isEmpty()) {
                    return@withContext Result.failure(Exception("No data found for session: $sessionId"))
                }

                val exportFile = createExportFile(sessionId, format)
                
                when (format) {
                    ExportFormat.CSV -> exportToCsv(sessionEntries, exportFile)
                    ExportFormat.JSON -> exportToJson(sessionEntries, exportFile)
                    ExportFormat.XML -> exportToXml(sessionEntries, exportFile)
                    ExportFormat.TXT -> exportToText(sessionEntries, exportFile)
                }

                Result.success(exportFile)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get all sessions
     */
    fun getAllSessions(): List<LogSession> {
        return logEntries
            .groupBy { it.sessionId }
            .map { (sessionId, entries) ->
                val firstEntry = entries.minByOrNull { it.timestamp }
                val lastEntry = entries.maxByOrNull { it.timestamp }
                
                LogSession(
                    sessionId = sessionId,
                    startTime = firstEntry?.timestamp ?: 0L,
                    endTime = lastEntry?.timestamp,
                    pidDataCount = entries.count { it.type == LogType.PID_DATA },
                    dtcCount = entries.count { it.type == LogType.DTC_DATA }
                )
            }
            .sortedByDescending { it.startTime }
    }

    /**
     * Delete session data
     */
    suspend fun deleteSession(sessionId: String): Result<Unit> {
        return try {
            logEntries.removeAll { it.sessionId == sessionId }
            
            // Delete associated files
            val logDir = getLogDirectory()
            logDir.listFiles()?.forEach { file ->
                if (file.name.contains(sessionId)) {
                    file.delete()
                }
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get storage usage statistics
     */
    fun getStorageStats(): StorageStats {
        val logDir = getLogDirectory()
        val totalSize = logDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()

        return StorageStats(
            totalSessions = getAllSessions().size,
            totalLogEntries = logEntries.size,
            totalSizeBytes = totalSize,
            availableSpaceBytes = logDir.freeSpace
        )
    }

    data class StorageStats(
        val totalSessions: Int,
        val totalLogEntries: Int,
        val totalSizeBytes: Long,
        val availableSpaceBytes: Long
    )

    private suspend fun logEntry(type: LogType, data: String, sessionId: String = "default") {
        withContext(Dispatchers.IO) {
            val entry = LogEntry(
                timestamp = System.currentTimeMillis(),
                type = type,
                data = data,
                sessionId = sessionId
            )

            logEntries.add(entry)
            
            // Limit memory usage
            if (logEntries.size > maxLogEntries) {
                logEntries.removeAt(0)
            }

            // Write to file if logging is active
            if (_isLogging.value) {
                writeLogEntry(entry)
            }
        }
    }

    private fun generateSessionId(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val random = (1000..9999).random()
        return "session_${timestamp}_$random"
    }

    private fun getLogDirectory(): File {
        val logDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "spacetec_logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        return logDir
    }

    private fun createLogFile(sessionId: String): File {
        val logDir = getLogDirectory()
        return File(logDir, "${sessionId}.log")
    }

    private fun createExportFile(sessionId: String, format: ExportFormat): File {
        val logDir = getLogDirectory()
        val extension = when (format) {
            ExportFormat.CSV -> "csv"
            ExportFormat.JSON -> "json"
            ExportFormat.XML -> "xml"
            ExportFormat.TXT -> "txt"
        }
        return File(logDir, "${sessionId}_export.$extension")
    }

    private suspend fun writeSessionHeader(session: LogSession) {
        withContext(Dispatchers.IO) {
            currentLogFile?.let { file ->
                FileWriter(file, true).use { writer ->
                    writer.appendLine("# SpaceTec Diagnostic Session Log")
                    writer.appendLine("# Session ID: ${session.sessionId}")
                    writer.appendLine("# Start Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(session.startTime))}")
                    session.vehicleInfo?.let { info ->
                        writer.appendLine("# Vehicle: ${info.make} ${info.model} ${info.year}")
                        writer.appendLine("# VIN: ${info.vin}")
                        writer.appendLine("# Engine: ${info.engine}")
                    }
                    writer.appendLine("# Format: Timestamp,Type,Data")
                    writer.appendLine("#" + "=".repeat(50))
                }
            }
        }
    }

    private suspend fun writeSessionFooter(session: LogSession) {
        withContext(Dispatchers.IO) {
            currentLogFile?.let { file ->
                FileWriter(file, true).use { writer ->
                    writer.appendLine("#" + "=".repeat(50))
                    writer.appendLine("# Session ended: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(session.endTime ?: System.currentTimeMillis()))}")
                    writer.appendLine("# PID Data Points: ${session.pidDataCount}")
                    writer.appendLine("# DTCs Found: ${session.dtcCount}")
                    writer.appendLine("# Duration: ${(session.endTime ?: System.currentTimeMillis()) - session.startTime}ms")
                }
            }
        }
    }

    private suspend fun writeLogEntry(entry: LogEntry) {
        withContext(Dispatchers.IO) {
            currentLogFile?.let { file ->
                FileWriter(file, true).use { writer ->
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(entry.timestamp))
                    writer.appendLine("$timestamp,${entry.type.name},\"${entry.data}\"")
                }
            }
        }
    }

    private fun exportToCsv(entries: List<LogEntry>, file: File) {
        FileWriter(file).use { writer ->
            writer.appendLine("Timestamp,Type,Data,Session ID")
            entries.forEach { entry ->
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(entry.timestamp))
                writer.appendLine("$timestamp,${entry.type.name},\"${entry.data}\",${entry.sessionId}")
            }
        }
    }

    private fun exportToJson(entries: List<LogEntry>, file: File) {
        FileWriter(file).use { writer ->
            writer.appendLine("{")
            writer.appendLine("  \"export_info\": {")
            writer.appendLine("    \"generated\": \"${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())}\",")
            writer.appendLine("    \"total_entries\": ${entries.size}")
            writer.appendLine("  },")
            writer.appendLine("  \"log_entries\": [")
            
            entries.forEachIndexed { index, entry ->
                val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date(entry.timestamp))
                writer.appendLine("    {")
                writer.appendLine("      \"timestamp\": \"$timestamp\",")
                writer.appendLine("      \"type\": \"${entry.type.name}\",")
                writer.appendLine("      \"data\": \"${entry.data.replace("\"", "\\\"")}\",")
                writer.appendLine("      \"session_id\": \"${entry.sessionId}\"")
                writer.append("    }")
                if (index < entries.size - 1) writer.appendLine(",")
                else writer.appendLine()
            }
            
            writer.appendLine("  ]")
            writer.appendLine("}")
        }
    }

    private fun exportToXml(entries: List<LogEntry>, file: File) {
        FileWriter(file).use { writer ->
            writer.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            writer.appendLine("<spacetec_log>")
            writer.appendLine("  <export_info>")
            writer.appendLine("    <generated>${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())}</generated>")
            writer.appendLine("    <total_entries>${entries.size}</total_entries>")
            writer.appendLine("  </export_info>")
            writer.appendLine("  <log_entries>")
            
            entries.forEach { entry ->
                val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date(entry.timestamp))
                writer.appendLine("    <entry>")
                writer.appendLine("      <timestamp>$timestamp</timestamp>")
                writer.appendLine("      <type>${entry.type.name}</type>")
                writer.appendLine("      <data><![CDATA[${entry.data}]]></data>")
                writer.appendLine("      <session_id>${entry.sessionId}</session_id>")
                writer.appendLine("    </entry>")
            }
            
            writer.appendLine("  </log_entries>")
            writer.appendLine("</spacetec_log>")
        }
    }

    private fun exportToText(entries: List<LogEntry>, file: File) {
        FileWriter(file).use { writer ->
            writer.appendLine("🚀 SpaceTec Diagnostic Log Export")
            writer.appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            writer.appendLine("Total Entries: ${entries.size}")
            writer.appendLine("=".repeat(60))
            writer.appendLine()
            
            entries.forEach { entry ->
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(entry.timestamp))
                writer.appendLine("[$timestamp] ${entry.type.name}: ${entry.data}")
            }
        }
    }

    fun cleanup() {
        job.cancel()
        logEntries.clear()
        currentLogFile = null
    }
}

package com.spacetec.diagnostic

import com.spacetec.connection.ObdConnectionService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🔍 Diagnostic Trouble Code (DTC) Manager
 * Handles reading, clearing, and managing DTCs from vehicle ECUs
 */
class DtcManager(
    private val connectionService: ObdConnectionService
) {
    
    data class DiagnosticTroubleCode(
        val code: String,
        val description: String,
        val type: DtcType,
        val status: DtcStatus,
        val timestamp: Long = System.currentTimeMillis(),
        val ecuModule: String? = null,
        val severity: DtcSeverity = DtcSeverity.UNKNOWN
    )
    
    enum class DtcType {
        POWERTRAIN,     // P codes
        CHASSIS,        // C codes  
        BODY,           // B codes
        NETWORK,        // U codes
        UNKNOWN
    }
    
    enum class DtcStatus {
        ACTIVE,         // Currently present
        PENDING,        // Intermittent, not yet confirmed
        PERMANENT,      // Stored permanently
        HISTORY,        // Previously active, now cleared
        UNKNOWN
    }
    
    enum class DtcSeverity {
        CRITICAL,       // Immediate attention required
        HIGH,           // Important but not critical
        MEDIUM,         // Should be addressed
        LOW,            // Minor issue
        UNKNOWN
    }
    
    private val _dtcList = MutableStateFlow<List<DiagnosticTroubleCode>>(emptyList())
    val dtcList: StateFlow<List<DiagnosticTroubleCode>> = _dtcList.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    /**
     * Read all DTCs from vehicle
     */
    suspend fun readDtcs(): Result<List<DiagnosticTroubleCode>> {
        return try {
            _isScanning.value = true
            val dtcs = mutableListOf<DiagnosticTroubleCode>()
            
            // Read stored DTCs (Mode 03)
            val storedResult = connectionService.sendCommand("03")
            if (storedResult.isSuccess) {
                val storedDtcs = parseStoredDtcs(storedResult.getOrNull() ?: "")
                dtcs.addAll(storedDtcs)
            }
            
            // Read pending DTCs (Mode 07)
            val pendingResult = connectionService.sendCommand("07")
            if (pendingResult.isSuccess) {
                val pendingDtcs = parsePendingDtcs(pendingResult.getOrNull() ?: "")
                dtcs.addAll(pendingDtcs)
            }
            
            // Read permanent DTCs (Mode 0A) - OBD-II only
            val permanentResult = connectionService.sendCommand("0A")
            if (permanentResult.isSuccess) {
                val permanentDtcs = parsePermanentDtcs(permanentResult.getOrNull() ?: "")
                dtcs.addAll(permanentDtcs)
            }
            
            _dtcList.value = dtcs
            _isScanning.value = false
            
            Result.success(dtcs)
            
        } catch (e: Exception) {
            _isScanning.value = false
            Result.failure(e)
        }
    }
    
    /**
     * Clear all DTCs from vehicle
     */
    suspend fun clearDtcs(): Result<Unit> {
        return try {
            // Clear DTCs command (Mode 04)
            val result = connectionService.sendCommand("04")
            
            if (result.isSuccess) {
                // Wait a moment then re-read to confirm clearing
                delay(1000)
                readDtcs()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to clear DTCs: ${result.exceptionOrNull()?.message}"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get DTC count without full scan
     */
    suspend fun getDtcCount(): Result<Int> {
        return try {
            // Request number of DTCs (Mode 01, PID 01)
            val result = connectionService.sendCommand("01 01")
            
            if (result.isSuccess) {
                val response = result.getOrNull() ?: ""
                val count = parseDtcCount(response)
                Result.success(count)
            } else {
                Result.failure(Exception("Failed to get DTC count"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Export DTCs to formatted string
     */
    fun exportDtcs(format: ExportFormat = ExportFormat.TEXT): String {
        val dtcs = _dtcList.value
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        return when (format) {
            ExportFormat.TEXT -> exportAsText(dtcs, timestamp)
            ExportFormat.CSV -> exportAsCsv(dtcs, timestamp)
            ExportFormat.JSON -> exportAsJson(dtcs, timestamp)
        }
    }
    
    enum class ExportFormat {
        TEXT, CSV, JSON
    }
    
    private fun parseStoredDtcs(response: String): List<DiagnosticTroubleCode> {
        val dtcs = mutableListOf<DiagnosticTroubleCode>()
        
        try {
            // Parse hex response to DTC codes
            val cleanResponse = response.replace(" ", "").replace("\r", "").replace("\n", "")
            
            // Skip header bytes and parse DTC pairs
            var i = 4 // Skip "43" response header
            while (i + 3 < cleanResponse.length) {
                val dtcHex = cleanResponse.substring(i, i + 4)
                val dtcCode = hexToDtcCode(dtcHex)
                
                if (dtcCode.isNotEmpty() && dtcCode != "P0000") {
                    dtcs.add(
                        DiagnosticTroubleCode(
                            code = dtcCode,
                            description = getDtcDescription(dtcCode),
                            type = getDtcType(dtcCode),
                            status = DtcStatus.ACTIVE,
                            severity = getDtcSeverity(dtcCode)
                        )
                    )
                }
                i += 4
            }
            
        } catch (e: Exception) {
            // Handle parsing errors gracefully
        }
        
        return dtcs
    }
    
    private fun parsePendingDtcs(response: String): List<DiagnosticTroubleCode> {
        val dtcs = mutableListOf<DiagnosticTroubleCode>()
        
        try {
            val cleanResponse = response.replace(" ", "").replace("\r", "").replace("\n", "")
            
            var i = 4 // Skip "47" response header
            while (i + 3 < cleanResponse.length) {
                val dtcHex = cleanResponse.substring(i, i + 4)
                val dtcCode = hexToDtcCode(dtcHex)
                
                if (dtcCode.isNotEmpty() && dtcCode != "P0000") {
                    dtcs.add(
                        DiagnosticTroubleCode(
                            code = dtcCode,
                            description = getDtcDescription(dtcCode),
                            type = getDtcType(dtcCode),
                            status = DtcStatus.PENDING,
                            severity = getDtcSeverity(dtcCode)
                        )
                    )
                }
                i += 4
            }
            
        } catch (e: Exception) {
            // Handle parsing errors gracefully
        }
        
        return dtcs
    }
    
    private fun parsePermanentDtcs(response: String): List<DiagnosticTroubleCode> {
        val dtcs = mutableListOf<DiagnosticTroubleCode>()
        
        try {
            val cleanResponse = response.replace(" ", "").replace("\r", "").replace("\n", "")
            
            var i = 4 // Skip "4A" response header
            while (i + 3 < cleanResponse.length) {
                val dtcHex = cleanResponse.substring(i, i + 4)
                val dtcCode = hexToDtcCode(dtcHex)
                
                if (dtcCode.isNotEmpty() && dtcCode != "P0000") {
                    dtcs.add(
                        DiagnosticTroubleCode(
                            code = dtcCode,
                            description = getDtcDescription(dtcCode),
                            type = getDtcType(dtcCode),
                            status = DtcStatus.PERMANENT,
                            severity = getDtcSeverity(dtcCode)
                        )
                    )
                }
                i += 4
            }
            
        } catch (e: Exception) {
            // Handle parsing errors gracefully
        }
        
        return dtcs
    }
    
    private fun parseDtcCount(response: String): Int {
        return try {
            val cleanResponse = response.replace(" ", "").replace("\r", "").replace("\n", "")
            if (cleanResponse.length >= 6) {
                val countHex = cleanResponse.substring(4, 6)
                Integer.parseInt(countHex, 16) and 0x7F // Mask MIL bit
            } else 0
        } catch (e: Exception) {
            0
        }
    }
    
    private fun hexToDtcCode(hex: String): String {
        if (hex.length != 4) return ""
        
        try {
            val value = Integer.parseInt(hex, 16)
            val firstDigit = (value shr 14) and 0x03
            val secondDigit = (value shr 12) and 0x03
            val thirdDigit = (value shr 8) and 0x0F
            val fourthDigit = (value shr 4) and 0x0F
            val fifthDigit = value and 0x0F
            
            val prefix = when (firstDigit) {
                0 -> "P"
                1 -> "C"
                2 -> "B"
                3 -> "U"
                else -> "P"
            }
            
            return "$prefix$secondDigit$thirdDigit${fourthDigit.toString(16).uppercase()}${fifthDigit.toString(16).uppercase()}"
            
        } catch (e: Exception) {
            return ""
        }
    }
    
    private fun getDtcType(code: String): DtcType {
        return when (code.firstOrNull()) {
            'P' -> DtcType.POWERTRAIN
            'C' -> DtcType.CHASSIS
            'B' -> DtcType.BODY
            'U' -> DtcType.NETWORK
            else -> DtcType.UNKNOWN
        }
    }
    
    private fun getDtcSeverity(code: String): DtcSeverity {
        // Basic severity mapping - can be enhanced with comprehensive database
        return when {
            code.startsWith("P0") -> when {
                code.startsWith("P01") -> DtcSeverity.CRITICAL // Fuel/air issues
                code.startsWith("P02") -> DtcSeverity.HIGH     // Injector issues
                code.startsWith("P03") -> DtcSeverity.CRITICAL // Ignition/misfire
                else -> DtcSeverity.MEDIUM
            }
            code.startsWith("P1") -> DtcSeverity.MEDIUM // Manufacturer specific
            code.startsWith("C") -> DtcSeverity.HIGH    // Chassis issues
            code.startsWith("B") -> DtcSeverity.LOW     // Body issues
            code.startsWith("U") -> DtcSeverity.MEDIUM  // Network issues
            else -> DtcSeverity.UNKNOWN
        }
    }
    
    private fun getDtcDescription(code: String): String {
        // Basic DTC descriptions - can be enhanced with comprehensive database
        return when (code) {
            "P0300" -> "Random/Multiple Cylinder Misfire Detected"
            "P0301" -> "Cylinder 1 Misfire Detected"
            "P0302" -> "Cylinder 2 Misfire Detected"
            "P0303" -> "Cylinder 3 Misfire Detected"
            "P0304" -> "Cylinder 4 Misfire Detected"
            "P0171" -> "System Too Lean (Bank 1)"
            "P0172" -> "System Too Rich (Bank 1)"
            "P0174" -> "System Too Lean (Bank 2)"
            "P0175" -> "System Too Rich (Bank 2)"
            "P0420" -> "Catalyst System Efficiency Below Threshold (Bank 1)"
            "P0430" -> "Catalyst System Efficiency Below Threshold (Bank 2)"
            "P0441" -> "Evaporative Emission Control System Incorrect Purge Flow"
            "P0442" -> "Evaporative Emission Control System Leak Detected (Small Leak)"
            "P0455" -> "Evaporative Emission Control System Leak Detected (Large Leak)"
            else -> "Unknown DTC - $code"
        }
    }
    
    private fun exportAsText(dtcs: List<DiagnosticTroubleCode>, timestamp: String): String {
        val sb = StringBuilder()
        sb.appendLine("🚀 SpaceTec Diagnostic Report")
        sb.appendLine("Generated: $timestamp")
        sb.appendLine("Total DTCs: ${dtcs.size}")
        sb.appendLine("=".repeat(50))
        sb.appendLine()
        
        dtcs.groupBy { it.status }.forEach { (status, codes) ->
            sb.appendLine("${status.name} DTCs (${codes.size}):")
            codes.forEach { dtc ->
                sb.appendLine("  ${dtc.code} - ${dtc.description}")
                sb.appendLine("    Type: ${dtc.type.name}, Severity: ${dtc.severity.name}")
                sb.appendLine()
            }
        }
        
        return sb.toString()
    }
    
    private fun exportAsCsv(dtcs: List<DiagnosticTroubleCode>, timestamp: String): String {
        val sb = StringBuilder()
        sb.appendLine("# SpaceTec Diagnostic Report - $timestamp")
        sb.appendLine("Code,Description,Type,Status,Severity,Timestamp")
        
        dtcs.forEach { dtc ->
            val dtcTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(dtc.timestamp))
            sb.appendLine("${dtc.code},\"${dtc.description}\",${dtc.type.name},${dtc.status.name},${dtc.severity.name},$dtcTimestamp")
        }
        
        return sb.toString()
    }
    
    private fun exportAsJson(dtcs: List<DiagnosticTroubleCode>, timestamp: String): String {
        // Simple JSON export - could use Gson/Moshi for more complex scenarios
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"report\": {")
        sb.appendLine("    \"generated\": \"$timestamp\",")
        sb.appendLine("    \"total_dtcs\": ${dtcs.size},")
        sb.appendLine("    \"dtcs\": [")
        
        dtcs.forEachIndexed { index, dtc ->
            val dtcTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date(dtc.timestamp))
            sb.appendLine("      {")
            sb.appendLine("        \"code\": \"${dtc.code}\",")
            sb.appendLine("        \"description\": \"${dtc.description}\",")
            sb.appendLine("        \"type\": \"${dtc.type.name}\",")
            sb.appendLine("        \"status\": \"${dtc.status.name}\",")
            sb.appendLine("        \"severity\": \"${dtc.severity.name}\",")
            sb.appendLine("        \"timestamp\": \"$dtcTimestamp\"")
            sb.append("      }")
            if (index < dtcs.size - 1) sb.appendLine(",")
            else sb.appendLine()
        }
        
        sb.appendLine("    ]")
        sb.appendLine("  }")
        sb.appendLine("}")
        
        return sb.toString()
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        // No specific cleanup needed for DtcManager
        // This method exists for consistency with other managers
    }
}

package com.spacetec.diagnostic.transport

import com.spacetec.bluetooth.ObdResponse
import com.spacetec.diagnostic.core.DiagnosticAdapter

/**
 * Adapter that converts DiagnosticAdapter to ObdTransport
 * This allows using DiagnosticAdapter implementations (like J2534Adapter) with the ObdTransport interface
 */
class DiagnosticAdapterToObdTransport(
    private val diagnosticAdapter: DiagnosticAdapter
) : ObdTransport {
    
    override suspend fun connect(): Boolean {
        return diagnosticAdapter.connect()
    }
    
    override suspend fun isConnected(): Boolean {
        return diagnosticAdapter.isConnected()
    }
    
    override suspend fun disconnect() {
        diagnosticAdapter.disconnect()
    }
    
    override suspend fun readPid(pid: String): String? {
        // Convert hex PID to integer
        return try {
            val pidInt = pid.toInt(16)
            val response = diagnosticAdapter.requestPid(pidInt)
            if (response != null) {
                // Convert byte array to hex string
                response.joinToString("") { "%02X".format(it) }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun scanEcus(): List<String> {
        // For now, return empty list as ECU scanning is not implemented
        return emptyList()
    }
    
    override suspend fun readDtcs(): List<String> {
        return diagnosticAdapter.readDtcs()
    }
    
    override suspend fun clearDtcs(): Boolean {
        return diagnosticAdapter.clearDtcs()
    }
    
    override suspend fun readVin(): String? {
        return diagnosticAdapter.readVin()
    }
    
    override suspend fun sendObdCommand(command: String): ObdResponse {
        return diagnosticAdapter.sendRawCommand(command)
    }
    
    override suspend fun sendRawCommand(command: String): ObdResponse {
        return diagnosticAdapter.sendRawCommand(command)
    }
}

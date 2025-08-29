package com.spacetec.diagnostic.transport

import com.spacetec.connection.transport.ObdTransport as ConnectionObdTransport
import com.spacetec.bluetooth.ObdResponse
import com.spacetec.obd.ObdProtocol
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Adapter that converts connection-level ObdTransport to diagnostic-level ObdTransport
 * This allows using connection-level transports (Bluetooth, J2534, etc.) with diagnostic modules
 */
class ConnectionToDiagnosticAdapter(
    private val connectionTransport: ConnectionObdTransport
) : ObdTransport {
    
    companion object {
        private const val COMMAND_TIMEOUT = 5000L
    }
    
    override suspend fun connect(): Boolean {
        return connectionTransport.connect().isSuccess
    }
    
    override suspend fun isConnected(): Boolean {
        // For connection-level transport, we assume it's connected if we can send commands
        // In a real implementation, this would check the actual connection state
        return true
    }
    
    override suspend fun disconnect() {
        connectionTransport.disconnect()
    }
    
    override suspend fun readPid(pid: String): String? {
        return try {
            val result = withTimeoutOrNull(COMMAND_TIMEOUT) {
                connectionTransport.sendCommand(pid)
            }
            
            when (result) {
                is Result.Success -> {
                    val response = result.getOrNull()
                    if (response != null && !ObdProtocol.isErrorResponse(response)) {
                        response
                    } else {
                        null
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun scanEcus(): List<String> {
        // For now, return empty list as ECU scanning is not implemented
        // In a real implementation, this would scan for available ECUs
        return emptyList()
    }
    
    override suspend fun readDtcs(): List<String> {
        return try {
            val result = withTimeoutOrNull(COMMAND_TIMEOUT) {
                connectionTransport.sendCommand("03")
            }
            
            when (result) {
                is Result.Success -> {
                    val response = result.getOrNull()
                    if (response != null && !ObdProtocol.isErrorResponse(response)) {
                        parseDtcCodes(response)
                    } else {
                        emptyList()
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun clearDtcs(): Boolean {
        return try {
            val result = withTimeoutOrNull(COMMAND_TIMEOUT) {
                connectionTransport.sendCommand("04")
            }
            
            when (result) {
                is Result.Success -> {
                    val response = result.getOrNull()
                    response != null && !ObdProtocol.isErrorResponse(response)
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun readVin(): String? {
        return try {
            val result = withTimeoutOrNull(COMMAND_TIMEOUT) {
                connectionTransport.sendCommand("0902")
            }
            
            when (result) {
                is Result.Success -> {
                    val response = result.getOrNull()
                    if (response != null && !ObdProtocol.isErrorResponse(response)) {
                        ObdProtocol.parseVin(listOf(response))
                    } else {
                        null
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun sendObdCommand(command: String): ObdResponse {
        return try {
            val result = withTimeoutOrNull(COMMAND_TIMEOUT) {
                connectionTransport.sendCommand(command)
            }
            
            when (result) {
                is Result.Success -> {
                    val response = result.getOrNull()
                    if (response != null) {
                        if (ObdProtocol.isErrorResponse(response)) {
                            ObdResponse.Error(response)
                        } else {
                            ObdResponse.Success(response)
                        }
                    } else {
                        ObdResponse.Error("No response received")
                    }
                }
                else -> ObdResponse.Error("Command timeout or failure")
            }
        } catch (e: Exception) {
            ObdResponse.Error(e.message ?: "Unknown error")
        }
    }
    
    override suspend fun sendRawCommand(command: String): ObdResponse {
        // For connection-level transport, sendObdCommand and sendRawCommand are the same
        return sendObdCommand(command)
    }
    
    private fun parseDtcCodes(response: String): List<String> {
        val dtcs = mutableListOf<String>()
        try {
            val cleaned = response.replace(" ", "").uppercase()
            if (cleaned.startsWith("43") && cleaned.length >= 4) {
                val numCodes = cleaned.substring(2, 4).toInt(16)
                if (numCodes > 0 && cleaned.length >= 4 + (numCodes * 4)) {
                    for (i in 0 until numCodes) {
                        val startIndex = 4 + (i * 4)
                        val dtcHex = cleaned.substring(startIndex, startIndex + 4)
                        val dtcCode = convertHexToDtc(dtcHex)
                        if (dtcCode.isNotEmpty()) {
                            dtcs.add(dtcCode)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore parsing errors
        }
        return dtcs
    }
    
    private fun convertHexToDtc(hex: String): String {
        try {
            val value = hex.toInt(16)
            val firstDigit = (value shr 14) and 0x03
            val secondDigit = (value shr 12) and 0x03
            val thirdDigit = (value shr 8) and 0x0F
            val fourthFifthDigit = value and 0xFF
            
            val prefix = when (firstDigit) {
                0 -> "P0"
                1 -> "P1"
                2 -> "P2"
                3 -> "P3"
                else -> "P"
            }
            
            return "$prefix${secondDigit}${String.format("%02X", thirdDigit)}${String.format("%02X", fourthFifthDigit)}"
        } catch (e: Exception) {
            return ""
        }
    }
}

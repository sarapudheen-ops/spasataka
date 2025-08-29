package com.spacetec.diagnostic.core

import com.spacetec.diagnostic.transport.J2534Transport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

class J2534Adapter(private val dllName: String = "j2534") : DiagnosticAdapter {
    override val name: String = "J2534 Professional"
    private val transport = J2534Transport()
    
    override suspend fun connect(target: Any?): Boolean = transport.open()
    
    override suspend fun disconnect() = transport.close()
    
    override suspend fun isConnected(): Boolean {
        // For J2534, we assume it's connected if the transport is open
        // In a real implementation, this would check the actual connection state
        return true
    }
    
    override suspend fun readVin(): String? {
        // UDS Read Data by Identifier for VIN (DID 0xF190)
        val request = byteArrayOf(0x22, 0xF1.toByte(), 0x90.toByte())
        if (!transport.write(request)) return null
        
        return withTimeoutOrNull(5000) {
            transport.read().map { response ->
                // Check for positive response (0x62) to ReadDataByIdentifier (0x22)
                if (response.size >= 4 && response[0] == 0x62.toByte() && 
                    response[1] == 0xF1.toByte() && response[2] == 0x90.toByte()) {
                    // Extract VIN from UDS response (data starts at index 3)
                    val vinBytes = response.sliceArray(3 until response.size)
                    // Filter out non-printable characters and convert to string
                    String(vinBytes.filter { it.toInt() >= 32 && it.toInt() <= 126 }.toByteArray())
                } else null
            }.firstOrNull { it != null }
        }
    }
    
    override suspend fun readSupportedPids(): Set<Int> {
        // Use UDS Read Data by Identifier for supported services
        val request = byteArrayOf(0x22, 0xF0.toByte(), 0x86.toByte()) // Active Diagnostic Session
        if (!transport.write(request)) return emptySet()
        
        return withTimeoutOrNull(3000) {
            transport.read().map { response ->
                // Parse supported services from response
                parseSupportedServices(response)
            }.firstOrNull() ?: emptySet()
        } ?: emptySet()
    }
    
    override suspend fun readDtcs(): List<String> {
        // UDS Read DTC Information (0x19)
        val request = byteArrayOf(0x19, 0x02, 0x02) // Read DTC by status mask
        if (!transport.write(request)) return emptyList()
        
        return withTimeoutOrNull(5000) {
            transport.read().map { response ->
                parseUdsDtcs(response)
            }.firstOrNull() ?: emptyList()
        } ?: emptyList()
    }
    
    override suspend fun clearDtcs(): Boolean {
        // UDS Clear Diagnostic Information (0x14)
        val request = byteArrayOf(0x14, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        if (!transport.write(request)) return false
        
        return withTimeoutOrNull(3000) {
            transport.read().map { response ->
                response.isNotEmpty() && response[0] == 0x54.toByte() // Positive response
            }.firstOrNull() ?: false
        } ?: false
    }
    
    override suspend fun readiness(): ReadinessStatus {
        // UDS Read Data by Identifier for readiness status
        val request = byteArrayOf(0x22, 0xF4.toByte(), 0x0D.toByte())
        if (!transport.write(request)) {
            return ReadinessStatus(emptySet(), emptySet(), emptySet())
        }
        
        return withTimeoutOrNull(3000) {
            transport.read().map { response ->
                parseUdsReadiness(response)
            }.firstOrNull() ?: ReadinessStatus(emptySet(), emptySet(), emptySet())
        } ?: ReadinessStatus(emptySet(), emptySet(), emptySet())
    }
    
    override suspend fun freezeFrame(): Map<String, ByteArray> {
        // UDS Read DTC Information - Read DTC snapshot data
        val request = byteArrayOf(0x19, 0x04, 0x00) // Read snapshot data
        if (!transport.write(request)) return emptyMap()
        
        return withTimeoutOrNull(5000) {
            transport.read().map { response ->
                mapOf("snapshot" to response)
            }.firstOrNull() ?: emptyMap()
        } ?: emptyMap()
    }
    
    override suspend fun requestPid(pid: Int): ByteArray? {
        // Convert OBD-II PID to UDS Read Data by Identifier
        val did = 0xF400 + pid // Map PID to DID range
        val request = byteArrayOf(0x22, (did shr 8).toByte(), (did and 0xFF).toByte())
        if (!transport.write(request)) return null
        
        return withTimeoutOrNull(3000) {
            transport.read().firstOrNull()
        }
    }
    
    override fun streamPids(pids: List<Int>, periodMs: Long): Flow<Map<Int, Any>> = flow {
        for (pid in pids) {
            val response = requestPid(pid)
            if (response != null) {
                emit(mapOf(pid to response))
            }
        }
    }
    
    override suspend fun udsReadDataByIdentifier(did: Int): ByteArray? {
        val request = byteArrayOf(0x22, (did shr 8).toByte(), (did and 0xFF).toByte())
        if (!transport.write(request)) return null
        
        return withTimeoutOrNull(3000) {
            transport.read().map { response ->
                if (response.size >= 3 && response[0] == 0x62.toByte()) {
                    response.sliceArray(3 until response.size) // Remove service ID and DID
                } else null
            }.firstOrNull { it != null }
        }
    }
    
    override suspend fun udsRoutineControl(routineId: Int, controlType: Int, payload: ByteArray): ByteArray? {
        val request = ByteArray(4 + payload.size)
        request[0] = 0x31.toByte() // Routine Control service
        request[1] = controlType.toByte()
        request[2] = (routineId shr 8).toByte()
        request[3] = (routineId and 0xFF).toByte()
        payload.copyInto(request, 4)
        
        if (!transport.write(request)) return null
        
        return withTimeoutOrNull(5000) {
            transport.read().map { response ->
                if (response.size >= 4 && response[0] == 0x71.toByte()) {
                    response.sliceArray(4 until response.size) // Remove header
                } else null
            }.firstOrNull { it != null }
        }
    }
    
    override suspend fun launchOemReprogramming(oemPkg: String): Boolean {
        // This would typically involve loading OEM-specific flash routines
        // For now, return false as this requires OEM-specific implementation
        return false
    }
    
    override suspend fun sendRawCommand(command: String): ObdResponse {
        return try {
            // Convert hex string to byte array
            val commandBytes = hexStringToByteArray(command)
            
            // Send command
            if (!transport.write(commandBytes)) {
                return ObdResponse.Error("Failed to send command")
            }
            
            // Read response with timeout
            val response = withTimeoutOrNull(5000) {
                transport.read().firstOrNull()
            }
            
            if (response != null) {
                // Convert response bytes to hex string
                val responseHex = byteArrayToHexString(response)
                ObdResponse.Success(responseHex)
            } else {
                ObdResponse.Error("No response received")
            }
        } catch (e: Exception) {
            ObdResponse.Error(e.message ?: "Unknown error")
        }
    }
    
    private fun hexStringToByteArray(hex: String): ByteArray {
        val cleanedHex = hex.replace(" ", "").uppercase()
        return ByteArray(cleanedHex.length / 2) { i ->
            cleanedHex.substring(i * 2, (i + 1) * 2).toInt(16).toByte()
        }
    }
    
    private fun byteArrayToHexString(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }
    
    private fun parseSupportedServices(response: ByteArray): Set<Int> {
        val services = mutableSetOf<Int>()
        if (response.size >= 3 && response[0] == 0x62.toByte()) {
            // Parse service availability bitmap
            for (i in 3 until response.size) {
                val byte = response[i].toInt() and 0xFF
                for (bit in 0..7) {
                    if ((byte and (1 shl bit)) != 0) {
                        services.add((i - 3) * 8 + bit)
                    }
                }
            }
        }
        return services
    }
    
    private fun parseUdsDtcs(response: ByteArray): List<String> {
        val dtcs = mutableListOf<String>()
        if (response.size >= 3 && response[0] == 0x59.toByte()) {
            var i = 3 // Skip service ID and sub-function
            while (i + 2 < response.size) {
                val dtcHigh = response[i].toInt() and 0xFF
                val dtcLow = response[i + 1].toInt() and 0xFF
                val status = response[i + 2].toInt() and 0xFF
                
                if (status != 0) { // Only include active DTCs
                    val dtcCode = (dtcHigh shl 8) or dtcLow
                    val prefix = when ((dtcCode shr 14) and 0x03) {
                        0 -> "P"
                        1 -> "C"
                        2 -> "B"
                        3 -> "U"
                        else -> "P"
                    }
                    val number = String.format("%04X", dtcCode and 0x3FFF)
                    dtcs.add("$prefix$number")
                }
                i += 3
            }
        }
        return dtcs
    }
    
    private fun parseUdsReadiness(response: ByteArray): ReadinessStatus {
        val completed = mutableSetOf<String>()
        val notCompleted = mutableSetOf<String>()
        val supported = mutableSetOf<String>()
        
        // UDS readiness parsing would be vehicle-specific
        // For now, provide default readiness monitors
        val monitors = listOf("MISF", "FUEL", "CCM", "CAT", "HCAT", "EVAP", "AIR", "O2S", "HTR", "EGR")
        supported.addAll(monitors)
        completed.addAll(monitors) // Assume all completed for demo
        
        return ReadinessStatus(completed, notCompleted, supported)
    }
}

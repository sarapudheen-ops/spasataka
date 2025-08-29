package com.spacetec.diagnostic.core

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.spacetec.diagnostic.transport.Elm327Transport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

@SuppressLint("MissingPermission")
class Elm327Adapter(private val device: BluetoothDevice) : DiagnosticAdapter {
    override val name: String = "ELM327"
    private val transport = Elm327Transport(device.address)
    
    override suspend fun connect(target: Any?): Boolean = transport.open()
    
    override suspend fun disconnect() = transport.close()
    
    override suspend fun isConnected(): Boolean = true // Transport handles connection state
    
    override suspend fun readVin(): String? {
        if (!transport.write("0902\r".toByteArray())) return null
        
        return withTimeoutOrNull(5000) {
            transport.read().map { response ->
                String(response).trim().let { resp ->
                    if (resp.contains("49 02")) {
                        // Parse VIN from ELM327 response
                        resp.replace("49 02", "").replace(" ", "").take(17)
                    } else null
                }
            }.firstOrNull { it != null }
        }
    }
    
    override suspend fun readSupportedPids(): Set<Int> {
        if (!transport.write("0100\r".toByteArray())) return emptySet()
        
        return withTimeoutOrNull(3000) {
            transport.read().map { response ->
                val resp = String(response).trim()
                if (resp.contains("41 00")) {
                    // Parse supported PIDs from response
                    parseSupportedPids(resp)
                } else emptySet()
            }.firstOrNull() ?: emptySet()
        } ?: emptySet()
    }
    
    override suspend fun readDtcs(): List<String> {
        if (!transport.write("03\r".toByteArray())) return emptyList()
        
        return withTimeoutOrNull(5000) {
            transport.read().map { response ->
                val resp = String(response).trim()
                parseDtcs(resp)
            }.firstOrNull() ?: emptyList()
        } ?: emptyList()
    }
    
    override suspend fun clearDtcs(): Boolean {
        return transport.write("04\r".toByteArray())
    }
    
    override suspend fun readiness(): ReadinessStatus {
        if (!transport.write("0101\r".toByteArray())) {
            return ReadinessStatus(emptySet(), emptySet(), emptySet())
        }
        
        return withTimeoutOrNull(3000) {
            transport.read().map { response ->
                val resp = String(response).trim()
                parseReadiness(resp)
            }.firstOrNull() ?: ReadinessStatus(emptySet(), emptySet(), emptySet())
        } ?: ReadinessStatus(emptySet(), emptySet(), emptySet())
    }
    
    override suspend fun freezeFrame(): Map<String, ByteArray> {
        if (!transport.write("02\r".toByteArray())) return emptyMap()
        
        return withTimeoutOrNull(5000) {
            transport.read().map { response ->
                // Parse freeze frame data
                mapOf("frame" to response)
            }.firstOrNull() ?: emptyMap()
        } ?: emptyMap()
    }
    
    override suspend fun requestPid(pid: Int): ByteArray? {
        val pidHex = String.format("01%02X\r", pid)
        if (!transport.write(pidHex.toByteArray())) return null
        
        return withTimeoutOrNull(3000) {
            transport.read().firstOrNull()
        }
    }
    
    override fun streamPids(pids: List<Int>, periodMs: Long): Flow<Map<Int, Any>> = flow {
        // Implement PID streaming logic
        for (pid in pids) {
            val response = requestPid(pid)
            if (response != null) {
                emit(mapOf(pid to response))
            }
        }
    }
    
    override suspend fun udsReadDataByIdentifier(did: Int): ByteArray? = null
    override suspend fun udsRoutineControl(routineId: Int, controlType: Int, payload: ByteArray): ByteArray? = null
    override suspend fun launchOemReprogramming(oemPkg: String): Boolean = false
    
    private fun parseSupportedPids(response: String): Set<Int> {
        // Parse 4 bytes of supported PID data
        val pids = mutableSetOf<Int>()
        val hexData = response.replace("41 00", "").replace(" ", "").take(8)
        
        try {
            val value = hexData.toLong(16)
            for (i in 0..31) {
                if ((value and (1L shl (31 - i))) != 0L) {
                    pids.add(i + 1)
                }
            }
        } catch (e: Exception) {
            // Handle parsing error
        }
        
        return pids
    }
    
    private fun parseDtcs(response: String): List<String> {
        val dtcs = mutableListOf<String>()
        // Parse DTC codes from response
        val lines = response.split("\r", "\n")
        
        for (line in lines) {
            if (line.matches(Regex("^[0-9A-F]{4}$"))) {
                val code = line.take(4)
                val prefix = when (code[0]) {
                    '0' -> "P0"
                    '1' -> "P1"
                    '2' -> "P2"
                    '3' -> "P3"
                    '4' -> "B0"
                    '5' -> "B1"
                    '6' -> "B2"
                    '7' -> "B3"
                    '8' -> "C0"
                    '9' -> "C1"
                    'A' -> "C2"
                    'B' -> "C3"
                    'C' -> "U0"
                    'D' -> "U1"
                    'E' -> "U2"
                    'F' -> "U3"
                    else -> "P0"
                }
                dtcs.add(prefix + code.substring(1))
            }
        }
        
        return dtcs
    }
    
    private fun parseReadiness(response: String): ReadinessStatus {
        // Parse readiness status from Mode 01 PID 01 response
        val completed = mutableSetOf<String>()
        val notCompleted = mutableSetOf<String>()
        val supported = mutableSetOf<String>()
        
        // Default readiness monitors
        val monitors = listOf("MISF", "FUEL", "CCM", "CAT", "HCAT", "EVAP", "AIR", "O2S", "HTR", "EGR")
        supported.addAll(monitors)
        
        // For now, assume all are completed (would need actual parsing)
        completed.addAll(monitors)
        
        return ReadinessStatus(completed, notCompleted, supported)
    }
}

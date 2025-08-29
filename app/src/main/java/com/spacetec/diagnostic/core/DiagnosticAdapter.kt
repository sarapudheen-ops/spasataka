// core/DiagnosticAdapter.kt
package com.spacetec.diagnostic.core

import com.spacetec.bluetooth.ObdResponse
import kotlinx.coroutines.flow.Flow

interface DiagnosticAdapter {
    val name: String
    suspend fun connect(target: Any? = null): Boolean
    suspend fun disconnect()
    suspend fun isConnected(): Boolean

    // OBD-II essentials
    suspend fun readVin(): String?
    suspend fun readSupportedPids(): Set<Int>
    suspend fun readDtcs(): List<String>
    suspend fun clearDtcs(): Boolean
    suspend fun readiness(): ReadinessStatus
    suspend fun freezeFrame(): Map<String, ByteArray>

    // Mode 01 PID
    suspend fun requestPid(pid: Int): ByteArray?
    fun streamPids(pids: List<Int>, periodMs: Long = 500): Flow<Map<Int, Any>>

    // UDS/ISO-TP (advanced)
    suspend fun udsReadDataByIdentifier(did: Int): ByteArray?
    suspend fun udsRoutineControl(routineId: Int, controlType: Int, payload: ByteArray = byteArrayOf()): ByteArray?

    // Flashing via OEM (J2534 pass-through launcher, not custom reflash here)
    suspend fun launchOemReprogramming(oemPkg: String): Boolean
    
    // Raw command support for ECU programming
    suspend fun sendRawCommand(command: String): ObdResponse
}

data class ReadinessStatus(
    val completed: Set<String>,
    val notCompleted: Set<String>,
    val supported: Set<String>
)

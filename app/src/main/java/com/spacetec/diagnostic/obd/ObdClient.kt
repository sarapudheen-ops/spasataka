package com.spacetec.diagnostic.obd

import com.spacetec.diagnostic.transport.Transport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

class ObdClient(private val t: Transport) {

    suspend fun readPid(hex: String, timeoutMs: Long = 1500): String? {
        t.write(Obd.pid01(hex))
        return withTimeout(timeoutMs) {
            // For ELM: lines ending with '>' prompt; filter for "41 <pid>"
            t.read().first().toString(Charsets.UTF_8)
        }
    }

    suspend fun readDtcs(): List<String> {
        t.write(Obd.MODE_03)
        val resp = t.read().first().toString(Charsets.UTF_8)
        return DtcParser.fromMode03(resp)
    }

    suspend fun clearDtcs(): Boolean = t.write(Obd.MODE_04)

    suspend fun readVin(): String? {
        t.write(Obd.MODE_09_VIN)
        val resp = t.read().first().toString(Charsets.UTF_8)
        return Mode09Parser.parseVin(resp)
    }

    // Mode 01 PID 01: monitor readiness
    suspend fun readiness(): Readiness = ReadinessParser.parse(readPid("01") ?: "")
}

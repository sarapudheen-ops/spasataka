package com.spacetec.diagnostic.stack

import com.spacetec.diagnostic.transport.Transport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

class ObdService(private val t: Transport) {

    // Example: read PID 0C via ELM (Mode 01 PID 0C)
    suspend fun readPid(pidHex: String, timeoutMs: Long = 1500): ByteArray? {
        val cmd = "01$pidHex\r".encodeToByteArray()
        t.write(cmd)
        return withTimeout(timeoutMs) {
            // Filter for line starting with "41 <pid>"
            // (Implement your ELM line aggregation elsewhere)
            null // implement parser
        }
    }

    // Mode 03: Read DTCs (generic)
    suspend fun readDtcs(): List<String> {
        t.write("03\r".encodeToByteArray())
        // parse ELM lines into DTC list
        return emptyList()
    }

    suspend fun clearDtcs(): Boolean {
        t.write("04\r".encodeToByteArray())
        return true
    }
}

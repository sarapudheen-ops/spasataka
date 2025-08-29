package com.spacetec.diagnostic.isotp

import com.spacetec.diagnostic.transport.Transport
import kotlinx.coroutines.flow.*

class IsoTp(private val t: Transport) {
    suspend fun send(canId: Int, payload: ByteArray): Boolean {
        // For ELM: set headers/filters; for native: single call
        // Stub (single frame only if <=7)
        require(payload.size <= 7) { "Demo supports SF only; extend for FF/CF" }
        return t.write(byteArrayOf((0x02 + payload.size).toByte()) + payload) // placeholder
    }

    suspend fun receive(timeoutMs: Long = 1000): ByteArray? {
        // Reassemble (SF/FF/CF) — keep state machine; here placeholder
        return t.read().firstOrNull()?.copyOf()
    }
}

// Note: On J2534/Autel, prefer their native ISO-TP read/write. On ELM, use AT SH, AT CRA, AT STxx and implement FF/CF flow control.

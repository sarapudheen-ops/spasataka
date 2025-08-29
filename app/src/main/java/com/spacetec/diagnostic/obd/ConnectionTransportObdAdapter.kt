package com.spacetec.diagnostic.obd

import com.spacetec.connection.transport.ObdTransport as ConnObdTransport

/**
 * Adapter that bridges the connection-layer ObdTransport to the
 * simple string-based ObdTransport expected by EnhancedObdClient.
 */
class ConnectionTransportObdAdapter(
    private val conn: ConnObdTransport
) : ObdTransport {

    private var lastResponse: String = ""

    override suspend fun open(): Boolean {
        return conn.connect().isSuccess
    }

    override suspend fun write(command: String) {
        // Execute immediately and cache the response for read()
        val result = conn.sendCommand(command)
        lastResponse = result.getOrNull() ?: ""
    }

    override suspend fun read(): String {
        val resp = lastResponse
        lastResponse = ""
        return resp
    }

    override fun close() {
        // Best-effort disconnect
        try {
            // Use non-blocking cleanup path
            conn.cleanup()
        } catch (_: Exception) { }
    }
}

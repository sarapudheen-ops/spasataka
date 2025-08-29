package com.spacetec.diagnostic.transport

/**
 * Lightweight OBD transport abstraction used by EnhancedObdManager.
 * NOTE: This is distinct from connection-level transports in
 * `com.spacetec.connection.transport`. This interface reflects the
 * higher-level operations EnhancedObdManager currently calls.
 */
interface ObdTransport {
    /** Establishes a session with the adapter/vehicle. */
    suspend fun connect(): Boolean

    /** Ends the session and frees resources. */
    suspend fun disconnect()

    /**
     * Reads a single OBD-II PID. The input should be a hex command like
     * "010C", "010D", etc. Returns the raw hex payload (without spaces)
     * or null if unavailable.
     */
    suspend fun readPid(pid: String): String?

    /** Scans and returns a list of discovered ECU names or identifiers. */
    suspend fun scanEcus(): List<String>

    /** Reads stored Diagnostic Trouble Codes (DTCs). */
    suspend fun readDtcs(): List<String>

    /** Requests the clearing of DTCs. Returns true if accepted. */
    suspend fun clearDtcs(): Boolean

    /** Reads VIN if available. Returns VIN string or null. */
    suspend fun readVin(): String?
    
    /**
     * Checks if the transport is currently connected.
     */
    suspend fun isConnected(): Boolean
    
    /**
     * Sends a raw OBD command and returns the response.
     * This is used for advanced diagnostic operations like ECU programming.
     */
    suspend fun sendObdCommand(command: String): ObdResponse
    
    /**
     * Sends a raw command for ECU programming and returns the response.
     * This is a specialized method for ECU programming operations.
     */
    suspend fun sendRawCommand(command: String): ObdResponse
}

package com.spacetec.diagnostic.testing

import com.spacetec.diagnostic.obd.ObdTransport
import kotlinx.coroutines.delay
import android.util.Log

/**
 * Mock OBD transport for testing purposes
 */
class MockObdTransport : ObdTransport {
    private val responses = mutableMapOf<String, String>()
    private var isConnected = false
    private var simulateDelay = true
    private var connectionDelay = 100L
    private var responseDelay = 50L
    
    companion object {
        private const val TAG = "MockObdTransport"
    }
    
    /**
     * Add mock response for a specific command
     */
    fun addMockResponse(command: String, response: String) {
        responses[command.trim().uppercase()] = response
        Log.d(TAG, "Added mock response: $command -> $response")
    }
    
    /**
     * Add multiple mock responses
     */
    fun addMockResponses(responseMap: Map<String, String>) {
        responseMap.forEach { (command, response) ->
            addMockResponse(command, response)
        }
    }
    
    /**
     * Configure simulation settings
     */
    fun setSimulateDelay(enabled: Boolean) {
        simulateDelay = enabled
    }
    
    fun setConnectionDelay(delay: Long) {
        connectionDelay = delay
    }
    
    fun setResponseDelay(delay: Long) {
        responseDelay = delay
    }
    
    /**
     * Clear all mock responses
     */
    fun clearResponses() {
        responses.clear()
        Log.d(TAG, "Cleared all mock responses")
    }
    
    override suspend fun open(): Boolean {
        if (simulateDelay) {
            delay(connectionDelay)
        }
        
        isConnected = true
        Log.i(TAG, "Mock transport opened")
        return true
    }
    
    override suspend fun write(command: String) {
        if (!isConnected) {
            throw IllegalStateException("Transport not connected")
        }
        
        if (simulateDelay) {
            delay(responseDelay)
        }
        
        Log.d(TAG, "Mock write: $command")
    }
    
    override suspend fun read(): String {
        if (!isConnected) {
            throw IllegalStateException("Transport not connected")
        }
        
        // For testing, we'll use the last written command to determine response
        // In a real implementation, this would be handled differently
        val lastCommand = getLastCommand()
        val response = responses[lastCommand.trim().uppercase()] 
            ?: "NO DATA" // Default response if no mock set
        
        if (simulateDelay) {
            delay(responseDelay)
        }
        
        Log.d(TAG, "Mock read: $response")
        return response
    }
    
    override fun close() {
        isConnected = false
        Log.i(TAG, "Mock transport closed")
    }
    
    /**
     * Check if transport is connected
     */
    fun isConnected(): Boolean = isConnected
    
    /**
     * Get all configured responses
     */
    fun getConfiguredResponses(): Map<String, String> = responses.toMap()
    
    private var lastCommand = ""
    
    private fun getLastCommand(): String = lastCommand
    
    /**
     * Set the last command (for testing purposes)
     */
    fun setLastCommand(command: String) {
        lastCommand = command
    }
}

/**
 * Factory for creating pre-configured mock transports
 */
object MockTransportFactory {
    
    /**
     * Create mock transport with standard OBD-II responses
     */
    fun createStandardMock(): MockObdTransport {
        val mock = MockObdTransport()
        
        // Standard PID responses
        mock.addMockResponses(mapOf(
            "010C" to "41 0C 1A F8", // RPM: 1750
            "010D" to "41 0D 50",    // Speed: 80 km/h
            "0105" to "41 05 5A",    // Coolant temp: 50°C
            "012F" to "41 2F 32",    // Fuel level: 50%
            "0111" to "41 11 80",    // Throttle position: 50%
            "010F" to "41 0F 48",    // Intake air temp: 32°C
            "0104" to "41 04 80",    // Engine load: 50%
            "0106" to "41 06 80",    // Short term fuel trim: 0%
            "0107" to "41 07 80",    // Long term fuel trim: 0%
            "010A" to "41 0A 30",    // Fuel pressure: 48 kPa
            "010B" to "41 0B 20"     // Intake manifold pressure: 32 kPa
        ))
        
        return mock
    }
    
    /**
     * Create mock transport with high performance vehicle responses
     */
    fun createPerformanceMock(): MockObdTransport {
        val mock = MockObdTransport()
        
        mock.addMockResponses(mapOf(
            "010C" to "41 0C 2E E0", // RPM: 3000
            "010D" to "41 0D 78",    // Speed: 120 km/h
            "0105" to "41 05 69",    // Coolant temp: 65°C
            "012F" to "41 2F 28",    // Fuel level: 40%
            "0111" to "41 11 CC",    // Throttle position: 80%
            "010F" to "41 0F 50",    // Intake air temp: 40°C
            "0104" to "41 04 B3"     // Engine load: 70%
        ))
        
        return mock
    }
    
    /**
     * Create mock transport that simulates error conditions
     */
    fun createErrorMock(): MockObdTransport {
        val mock = MockObdTransport()
        
        mock.addMockResponses(mapOf(
            "010C" to "NO DATA",
            "010D" to "BUS INIT...ERROR",
            "0105" to "?",
            "012F" to "UNABLE TO CONNECT"
        ))
        
        return mock
    }
    
    /**
     * Create mock transport with diagnostic trouble codes
     */
    fun createDtcMock(): MockObdTransport {
        val mock = MockObdTransport()
        
        // Standard responses
        mock.addMockResponses(mapOf(
            "010C" to "41 0C 0F A0", // RPM: 1000
            "010D" to "41 0D 00",    // Speed: 0 km/h (idle)
            "0105" to "41 05 5F",    // Coolant temp: 55°C
            "012F" to "41 2F 40"     // Fuel level: 100%
        ))
        
        // DTC responses
        mock.addMockResponses(mapOf(
            "0101" to "41 01 07 E5 00 00", // MIL on, 2 DTCs
            "03" to "43 02 01 33 01 34",   // DTCs: P0133, P0134
            "04" to "44"                    // Clear DTCs response
        ))
        
        return mock
    }
}

/**
 * Test utilities for OBD testing
 */
object ObdTestUtils {
    
    /**
     * Convert hex string to expected integer value for testing
     */
    fun hexToRpm(hexValue: String): Int {
        val cleanHex = hexValue.replace(" ", "")
        val value = cleanHex.toInt(16)
        return value / 4
    }
    
    fun hexToSpeed(hexValue: String): Int {
        return hexValue.replace(" ", "").toInt(16)
    }
    
    fun hexToTemp(hexValue: String): Int {
        return hexValue.replace(" ", "").toInt(16) - 40
    }
    
    fun hexToPercent(hexValue: String): Int {
        return (hexValue.replace(" ", "").toInt(16) * 100) / 255
    }
    
    /**
     * Validate OBD response format
     */
    fun isValidObdResponse(response: String): Boolean {
        if (response.isBlank()) return false
        
        // Check for error responses
        val errorResponses = listOf("NO DATA", "?", "UNABLE TO CONNECT", "BUS INIT...ERROR")
        if (errorResponses.any { response.contains(it, ignoreCase = true) }) {
            return false
        }
        
        // Check for valid response format (starts with service + 40)
        val parts = response.split(" ")
        if (parts.size < 2) return false
        
        return try {
            val service = parts[0].toInt(16)
            service in 0x41..0x4F // Valid response service codes
        } catch (e: NumberFormatException) {
            false
        }
    }
}

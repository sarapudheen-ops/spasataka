package com.spacetec.features

import com.spacetec.obd.RealObdManager
import com.spacetec.obd.sendCommand
import com.spacetec.obd.sendService
import kotlinx.coroutines.delay

/**
 * Bi-directional controls for actuator testing
 */
class BiDirectionalControls(private val obdManager: RealObdManager) {
    
    /**
     * Test vehicle component (fuel pump, fan, etc.)
     */
    suspend fun testComponent(component: String, durationMs: Long = 5000): Boolean {
        return try {
            when (component.lowercase()) {
                "fuel pump" -> {
                    obdManager.sendCommand("AT SP 3")
                    obdManager.sendService(0x2F, 0x01) 
                    delay(durationMs)
                    obdManager.sendService(0x2F, 0x00)
                }
                "radiator fan" -> {
                    obdManager.sendCommand("AT SP 3")
                    obdManager.sendService(0x2F, 0x02)
                    delay(durationMs)
                    obdManager.sendService(0x2F, 0x00)
                }
                else -> return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

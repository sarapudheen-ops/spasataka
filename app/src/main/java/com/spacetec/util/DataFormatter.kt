package com.spacetec.util

import com.spacetec.vehicle.VehicleData
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for formatting OBD data for display
 */
object DataFormatter {
    
    // Date formatter for timestamps
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    /**
     * Format RPM for display
     */
    fun formatRpm(rpm: Int): String {
        return "${rpm.toFloat().toInt()} RPM"
    }
    
    /**
     * Format speed for display
     */
    fun formatSpeed(speed: Int): String {
        return "$speed km/h"
    }
    
    /**
     * Format temperature for display
     */
    fun formatTemperature(temp: Int): String {
        return "${temp}°C"
    }
    
    /**
     * Format percentage for display
     */
    fun formatPercentage(value: Int): String {
        return "$value%"
    }
    
    /**
     * Format pressure for display
     */
    fun formatPressure(kpa: Float): String {
        return String.format("%.1f kPa", kpa)
    }
    
    /**
     * Format voltage for display
     */
    fun formatVoltage(volts: Float): String {
        return String.format("%.1f V", volts)
    }
    
    /**
     * Format timestamp for display
     */
    fun formatTimestamp(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }
    
    /**
     * Get a status message based on vehicle data
     */
    fun getStatusMessage(data: VehicleData): String {
        return when {
            !data.isEngineRunning -> "Engine Off"
            data.coolantTemp > 110 -> "Engine Overheating!"
            data.fuelLevel < 10 -> "Low Fuel"
            data.rpm > 7000 -> "High RPM!"
            data.dtcCount > 0 -> "${data.dtcCount} DTCs Found"
            data.speed > 120 -> "High Speed"
            data.rpm > 3000 -> "High RPM"
            else -> "All Systems Normal"
        }
    }
    
    /**
     * Get status color based on vehicle data
     */
    fun getStatusColor(data: VehicleData): Long {
        return when {
            !data.isEngineRunning -> 0xFF9E9E9E // Grey
            data.coolantTemp > 110 || data.rpm > 7000 || data.dtcCount > 0 -> 0xFFE53935 // Red
            data.fuelLevel < 10 || data.speed > 120 || data.rpm > 3000 -> 0xFFFFA000 // Amber
            else -> 0xFF43A047 // Green
        }
    }
    
    /**
     * Format a DTC code for display (e.g., "P0172" -> "P0172 - System Too Rich (Bank 1)")
     */
    fun formatDtcCode(dtc: String): String {
        // In a real app, this would map DTC codes to their descriptions
        return when (dtc) {
            "P0172" -> "$dtc - System Too Rich (Bank 1)"
            "P0300" -> "$dtc - Random/Multiple Cylinder Misfire Detected"
            "P0420" -> "$dtc - Catalyst System Efficiency Below Threshold (Bank 1)"
            "P0442" -> "$dtc - Evaporative Emission Control System Leak Detected (small leak)"
            "P0507" -> "$dtc - Idle Air Control System RPM Higher Than Expected"
            else -> "$dtc - Unknown DTC"
        }
    }
    
    /**
     * Get a color for a DTC based on its severity
     */
    fun getDtcColor(dtc: String): Long {
        // In a real app, this would determine severity based on the DTC code
        return when (dtc.firstOrNull()) {
            'P' -> 0xFFE53935 // Powertrain - Red
            'C' -> 0xFF2196F3 // Chassis - Blue
            'B' -> 0xFF8E24AA // Body - Purple
            'U' -> 0xFFFFA000 // Network - Orange
            else -> 0xFF9E9E9E // Unknown - Grey
        }
    }
    
    /**
     * Format a map of freeze frame data for display
     */
    fun formatFreezeFrameData(data: Map<String, String>): List<Pair<String, String>> {
        return data.map { (key, value) ->
            val formattedKey = key.split("(?=[A-Z])".toRegex())
                .joinToString(" ") { it.lowercase().replaceFirstChar { it.uppercase() } }
            val formattedValue = when {
                key.contains("Temp", ignoreCase = true) -> "$value°C"
                key.contains("RPM", ignoreCase = true) -> "$value RPM"
                key.contains("Speed", ignoreCase = true) -> "$value km/h"
                key.contains("Voltage", ignoreCase = true) -> "$value V"
                key.contains("Pressure", ignoreCase = true) -> "$value kPa"
                else -> value
            }
            formattedKey to formattedValue
        }
    }
}

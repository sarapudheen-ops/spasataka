package com.spacetec.diagnostic.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.mutableListOf

/**
 * Records diagnostic trip data for analysis and export
 */
class TripRecorder {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentTrip: Trip? = null
    private val dataPoints = mutableListOf<TripDataPoint>()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _currentTrip = MutableStateFlow<Trip?>(null)
    val currentTripFlow: StateFlow<Trip?> = _currentTrip.asStateFlow()
    
    companion object {
        private const val TAG = "TripRecorder"
    }
    
    /**
     * Start recording a new trip
     */
    fun startTrip(tripName: String) {
        if (currentTrip != null) {
            Log.w(TAG, "Trip already in progress, ending current trip")
            endTrip()
        }
        
        currentTrip = Trip(
            id = UUID.randomUUID().toString(),
            name = tripName,
            startTime = System.currentTimeMillis(),
            endTime = null
        )
        
        dataPoints.clear()
        _isRecording.value = true
        _currentTrip.value = currentTrip
        Log.i(TAG, "Started trip: $tripName")
    }
    
    /**
     * Check if a trip is currently being recorded
     */
    fun isRecording(): Boolean = _isRecording.value
    
    val tripId: String
        get() = _currentTrip.value?.id ?: ""
    
    /**
     * Record a data point during the trip
     */
    fun recordDataPoint(pid: String, value: Double, timestamp: Long) {
        currentTrip?.let { trip ->
            val dataPoint = TripDataPoint(
                tripId = trip.id,
                pid = pid,
                value = value,
                timestamp = timestamp,
                formattedValue = formatPidValue(pid, value)
            )
            
            dataPoints.add(dataPoint)
            Log.d(TAG, "Recorded data point: $pid = $value")
        } ?: run {
            Log.w(TAG, "No active trip to record data point")
        }
    }
    
    /**
     * End the current trip and return summary
     */
    fun endTrip(): TripSummary {
        val trip = currentTrip ?: throw IllegalStateException("No active trip to end")
        
        val endTime = System.currentTimeMillis()
        val completedTrip = trip.copy(endTime = endTime)
        
        val summary = TripSummary(
            trip = completedTrip,
            totalDataPoints = dataPoints.size,
            duration = endTime - trip.startTime,
            averageSpeed = calculateAverageSpeed(),
            maxSpeed = calculateMaxSpeed(),
            fuelEfficiency = calculateFuelEfficiency(),
            distanceTraveled = calculateDistance(),
            alerts = detectAlerts()
        )
        
        currentTrip = null
        _isRecording.value = false
        _currentTrip.value = null
        Log.i(TAG, "Ended trip: ${trip.name}, Duration: ${summary.duration}ms")
        
        return summary
    }
    
    /**
     * Export trip data in specified format
     */
    fun exportTripData(format: ExportFormat) {
        val trip = currentTrip ?: throw IllegalStateException("No active trip to export")
        
        scope.launch {
            try {
                when (format) {
                    ExportFormat.JSON -> exportAsJson(trip)
                    ExportFormat.CSV -> exportAsCsv(trip)
                    ExportFormat.XML -> exportAsXml(trip)
                }
                Log.i(TAG, "Exported trip data as ${format.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export trip data: ${e.message}")
            }
        }
    }
    
    /**
     * Get current trip status
     */
    fun getCurrentTripStatus(): TripStatus? {
        return currentTrip?.let { trip ->
            TripStatus(
                tripId = trip.id,
                tripName = trip.name,
                startTime = trip.startTime,
                duration = System.currentTimeMillis() - trip.startTime,
                dataPointCount = dataPoints.size,
                isActive = true
            )
        }
    }
    
    /**
     * Get real-time trip statistics
     */
    fun getTripStatistics(): Flow<TripStatistics> = flow {
        while (currentTrip != null) {
            emit(TripStatistics(
                currentSpeed = getCurrentSpeed(),
                averageSpeed = calculateAverageSpeed(),
                maxSpeed = calculateMaxSpeed(),
                distance = calculateDistance(),
                fuelUsed = calculateFuelUsed(),
                duration = System.currentTimeMillis() - (currentTrip?.startTime ?: 0L)
            ))
            delay(1000) // Update every second
        }
    }
    
    private fun formatPidValue(pid: String, value: Double): String {
        return when (pid) {
            "0C" -> "${value.toInt()} RPM"
            "0D" -> "${value.toInt()} km/h"
            "05" -> "${value.toInt()}°C"
            "2F" -> "${value.toInt()}%"
            else -> value.toString()
        }
    }
    
    private fun calculateAverageSpeed(): Double {
        val speedPoints = dataPoints.filter { it.pid == "0D" }
        return if (speedPoints.isNotEmpty()) {
            speedPoints.map { it.value }.average()
        } else 0.0
    }
    
    private fun calculateMaxSpeed(): Double {
        return dataPoints.filter { it.pid == "0D" }.maxOfOrNull { it.value } ?: 0.0
    }
    
    private fun calculateDistance(): Double {
        // Simple distance calculation based on speed over time
        val speedPoints = dataPoints.filter { it.pid == "0D" }.sortedBy { it.timestamp }
        var distance = 0.0
        
        for (i in 1 until speedPoints.size) {
            val timeDiff = (speedPoints[i].timestamp - speedPoints[i-1].timestamp) / 1000.0 / 3600.0 // hours
            val avgSpeed = (speedPoints[i].value + speedPoints[i-1].value) / 2.0 // km/h
            distance += avgSpeed * timeDiff
        }
        
        return distance
    }
    
    private fun calculateFuelEfficiency(): Double {
        val distance = calculateDistance()
        val fuelUsed = calculateFuelUsed()
        return if (fuelUsed > 0) distance / fuelUsed else 0.0
    }
    
    private fun calculateFuelUsed(): Double {
        val fuelPoints = dataPoints.filter { it.pid == "2F" }
        if (fuelPoints.size < 2) return 0.0
        
        val initialFuel = fuelPoints.first().value
        val finalFuel = fuelPoints.last().value
        return maxOf(0.0, initialFuel - finalFuel)
    }
    
    private fun getCurrentSpeed(): Double {
        return dataPoints.filter { it.pid == "0D" }.lastOrNull()?.value ?: 0.0
    }
    
    private fun detectAlerts(): List<TripAlert> {
        val alerts = mutableListOf<TripAlert>()
        
        // High speed alert
        val maxSpeed = calculateMaxSpeed()
        if (maxSpeed > 120) {
            alerts.add(TripAlert.HighSpeed(maxSpeed))
        }
        
        // High RPM alert
        val maxRpm = dataPoints.filter { it.pid == "0C" }.maxOfOrNull { it.value } ?: 0.0
        if (maxRpm > 6000) {
            alerts.add(TripAlert.HighRpm(maxRpm))
        }
        
        // Temperature alert
        val maxTemp = dataPoints.filter { it.pid == "05" }.maxOfOrNull { it.value } ?: 0.0
        if (maxTemp > 100) {
            alerts.add(TripAlert.HighTemperature(maxTemp))
        }
        
        return alerts
    }
    
    private suspend fun exportAsJson(trip: Trip) {
        // Implementation for JSON export
        Log.d(TAG, "Exporting as JSON")
    }
    
    private suspend fun exportAsCsv(trip: Trip) {
        // Implementation for CSV export
        Log.d(TAG, "Exporting as CSV")
    }
    
    private suspend fun exportAsXml(trip: Trip) {
        // Implementation for XML export
        Log.d(TAG, "Exporting as XML")
    }
}

data class Trip(
    val id: String,
    val name: String,
    val startTime: Long,
    val endTime: Long?
)

data class TripDataPoint(
    val tripId: String,
    val pid: String,
    val value: Double,
    val timestamp: Long,
    val formattedValue: String
)

data class TripSummary(
    val trip: Trip,
    val totalDataPoints: Int,
    val duration: Long,
    val averageSpeed: Double,
    val maxSpeed: Double,
    val fuelEfficiency: Double,
    val distanceTraveled: Double,
    val alerts: List<TripAlert>
)

data class TripStatus(
    val tripId: String,
    val tripName: String,
    val startTime: Long,
    val duration: Long,
    val dataPointCount: Int,
    val isActive: Boolean
)

data class TripStatistics(
    val currentSpeed: Double,
    val averageSpeed: Double,
    val maxSpeed: Double,
    val distance: Double,
    val fuelUsed: Double,
    val duration: Long
)

enum class ExportFormat {
    JSON, CSV, XML
}

sealed class TripAlert {
    data class HighSpeed(val speed: Double) : TripAlert()
    data class HighRpm(val rpm: Double) : TripAlert()
    data class HighTemperature(val temperature: Double) : TripAlert()
}

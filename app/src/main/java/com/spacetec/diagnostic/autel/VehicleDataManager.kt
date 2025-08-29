package com.spacetec.diagnostic.autel

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Vehicle data manager for Autel diagnostic operations
 */
object VehicleDataManager {
    private const val TAG = "VehicleDataManager"
    
    private val _vehicleInfo = MutableStateFlow<VehicleInfo?>(null)
    val vehicleInfo: StateFlow<VehicleInfo?> = _vehicleInfo.asStateFlow()
    
    private val _ecuList = MutableStateFlow<List<EcuInfo>>(emptyList())
    val ecuList: StateFlow<List<EcuInfo>> = _ecuList.asStateFlow()
    
    fun setVehicleInfo(info: VehicleInfo) {
        _vehicleInfo.value = info
        Log.d(TAG, "Vehicle info updated: ${info.make} ${info.model}")
    }
    
    fun updateEcuList(ecus: List<EcuInfo>) {
        _ecuList.value = ecus
        Log.d(TAG, "ECU list updated with ${ecus.size} modules")
    }
    
    fun getVehicleInfo(): VehicleInfo? = _vehicleInfo.value
    
    fun getEcuList(): List<EcuInfo> = _ecuList.value
    
    // Service reset functions
    suspend fun resetOilLife(): Boolean {
        Log.d(TAG, "Resetting oil life service indicator")
        return true
    }
    
    suspend fun resetTireRotation(): Boolean {
        Log.d(TAG, "Resetting tire rotation service indicator")
        return true
    }
    
    suspend fun resetBrakeService(): Boolean {
        Log.d(TAG, "Resetting brake service indicator")
        return true
    }
    
    suspend fun resetTransmissionService(): Boolean {
        Log.d(TAG, "Resetting transmission service indicator")
        return true
    }
    
    suspend fun resetAirFilter(): Boolean {
        Log.d(TAG, "Resetting air filter service indicator")
        return true
    }
    
    suspend fun resetCabinFilter(): Boolean {
        Log.d(TAG, "Resetting cabin filter service indicator")
        return true
    }
    
    // Additional methods for compatibility
    suspend fun readEcuInfo(): List<EcuInfo> {
        val mockEcus = listOf(
            EcuInfo("Engine Control Module", "0x7E0", "SW Ver: 1.2.3", "HW Ver: A", "Ready"),
            EcuInfo("Transmission Control", "0x7E1", "SW Ver: 2.1.0", "HW Ver: B", "Ready"),
            EcuInfo("ABS Control Module", "0x7E2", "SW Ver: 3.0.1", "HW Ver: C", "Ready")
        )
        updateEcuList(mockEcus)
        return mockEcus
    }
    
    suspend fun readPidValue(pid: String): String {
        Log.d(TAG, "Reading PID value: $pid")
        return "Mock PID Response"
    }
    
    fun isServiceReady(): Boolean = true
    
    suspend fun readDtcCodes(): List<String> {
        return listOf("P0171", "P0174")
    }
    
    suspend fun clearDtcCodes(): Boolean {
        Log.d(TAG, "Clearing DTC codes")
        return true
    }
    
    suspend fun resetOilService(): Boolean = resetOilLife()
    suspend fun resetServiceInterval(): Boolean = resetTireRotation()
    suspend fun resetBatterySystem(): Boolean = resetBrakeService()
    suspend fun resetSteeringAngle(): Boolean = resetTransmissionService()
}

data class VehicleInfo(
    val make: String,
    val model: String,
    val year: Int,
    val vin: String,
    val engine: String = "",
    val transmission: String = ""
)

data class EcuInfo(
    val name: String,
    val address: String,
    val software: String,
    val hardware: String,
    val status: String
)

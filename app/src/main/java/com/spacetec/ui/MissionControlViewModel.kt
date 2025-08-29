package com.spacetec.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetec.bluetooth.SpaceBluetoothManager
import com.spacetec.obd.ObdManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MissionControlViewModel(
    private val bluetoothManager: SpaceBluetoothManager,
    private val obdManager: ObdManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MissionUiState())
    val uiState: StateFlow<MissionUiState> = _uiState

    init {
        // Map BluetoothManager state to MissionUiState
        viewModelScope.launch {
            combine(
                bluetoothManager.isScanning,
                bluetoothManager.discoveredDevices,
                bluetoothManager.connectionState,
                bluetoothManager.statusMessage
            ) { isScanning, devices, connectionState, statusMessage ->
                MissionUiState(
                    isScanning = isScanning,
                    devices = devices.mapNotNull { device ->
                        try {
                            device.name ?: "Unknown Device"
                        } catch (e: SecurityException) {
                            "Unknown Device"
                        }
                    },
                    connectionState = mapConnectionState(connectionState),
                    statusMessage = statusMessage
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun mapConnectionState(bluetoothState: SpaceBluetoothManager.ConnectionState): ConnectionState {
        return when (bluetoothState) {
            SpaceBluetoothManager.ConnectionState.DISCONNECTED -> ConnectionState.Disconnected
            SpaceBluetoothManager.ConnectionState.CONNECTING -> ConnectionState.Connecting
            SpaceBluetoothManager.ConnectionState.CONNECTED -> ConnectionState.Connected
            SpaceBluetoothManager.ConnectionState.ERROR -> ConnectionState.Error
            SpaceBluetoothManager.ConnectionState.SCANNING -> ConnectionState.Scanning
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (bluetoothManager.hasBluetoothPermissions()) {
            bluetoothManager.startDeviceDiscovery()
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToSatellite(deviceName: String) {
        // Find the device by name and connect
        val device = bluetoothManager.discoveredDevices.value.find { device ->
            try {
                device.name == deviceName
            } catch (e: SecurityException) {
                false
            }
        }
        device?.let { connectToDevice(it) }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            try {
                val connected = bluetoothManager.connectToDevice(device)
                if (connected) {
                    // Initialize OBD-II communication
                    val obdInitialized = obdManager.initialize()
                    if (obdInitialized) {
                        // Start monitoring vehicle data
                        obdManager.startMonitoring()
                    }
                }
            } catch (e: Exception) {
                // Handle connection error
            }
        }
    }

    fun disconnect() {
        obdManager.stopMonitoring()
        bluetoothManager.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        obdManager.cleanup()
        bluetoothManager.cleanup()
    }
}

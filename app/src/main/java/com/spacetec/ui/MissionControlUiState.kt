package com.spacetec.ui

import android.bluetooth.BluetoothDevice

data class MissionControlUiState(
    val isScanning: Boolean = false,
    val devices: List<BluetoothDevice> = emptyList(),
    val statusMessage: String = "Lost signal – drifting in space",
    val isConnected: Boolean = false,
    val connectedDevice: BluetoothDevice? = null
)

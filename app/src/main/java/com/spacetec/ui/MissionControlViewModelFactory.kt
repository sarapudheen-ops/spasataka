package com.spacetec.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.spacetec.bluetooth.SpaceBluetoothManager
import com.spacetec.obd.ObdManager

class MissionControlViewModelFactory(
    private val bluetoothManager: SpaceBluetoothManager,
    private val obdManager: ObdManager
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MissionControlViewModel::class.java)) {
            return MissionControlViewModel(bluetoothManager, obdManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

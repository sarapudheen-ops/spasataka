package com.spacetec.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.spacetec.obd.ObdManager
import com.spacetec.bluetooth.SpaceBluetoothManager

class VehicleDataViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VehicleDataViewModel::class.java)) {
            val bluetoothManager = SpaceBluetoothManager(context)
            val obdManager = ObdManager(bluetoothManager)
            return VehicleDataViewModel(obdManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

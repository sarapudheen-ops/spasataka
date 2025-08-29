package com.spacetec.diagnostic.core

import android.bluetooth.BluetoothDevice
import android.annotation.SuppressLint

// core/AdapterRegistry.kt
object AdapterRegistry {
    /**
     * Select an appropriate DiagnosticAdapter for a given Bluetooth device.
     * Currently we only support ELM327-like SPP devices.
     */
    @SuppressLint("MissingPermission")
    fun fromBluetoothDevice(device: BluetoothDevice): DiagnosticAdapter {
        val name = try {
            device.name?.lowercase() ?: device.address.lowercase()
        } catch (e: SecurityException) {
            device.address.lowercase()
        }
        // Future: add heuristics for Autel/VCI/J2534, etc.
        // Example:
        // if (name.contains("autel") || name.contains("vci")) return AutelAdapter(device)
        // if (name.contains("obdlink") || name.contains("elm")) return Elm327Adapter(device)
        return Elm327Adapter(device)
    }
}

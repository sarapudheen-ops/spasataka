package com.spacetec.diagnostic.adapters

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log

/**
 * Factory for creating appropriate adapter initializers based on device type and capabilities
 */
class AdapterFactory {
    
    companion object {
        private const val TAG = "AdapterFactory"
        
        /**
         * Create appropriate adapter initializer based on device characteristics
         */
        fun createAdapterInitializer(
            context: Context,
            device: BluetoothDevice
        ): AdapterInitializer? {
            return try {
                val deviceName = device.name?.uppercase() ?: ""
                
                when {
                    // ELM327 Bluetooth Classic devices
                    isElm327BluetoothDevice(deviceName) -> {
                        Log.i(TAG, "Creating ELM327 Bluetooth Classic initializer for: ${device.name}")
                        Elm327BluetoothClassicInitializer(context, device)
                    }
                    
                    // Generic OBD Bluetooth devices (fallback to ELM327 protocol)
                    isGenericObdDevice(deviceName) -> {
                        Log.i(TAG, "Creating generic OBD initializer for: ${device.name}")
                        Elm327BluetoothClassicInitializer(context, device)
                    }
                    
                    else -> {
                        Log.w(TAG, "No suitable adapter initializer found for device: ${device.name}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create adapter initializer: ${e.message}")
                null
            }
        }
        
        /**
         * Check if device is a known ELM327 adapter
         */
        private fun isElm327BluetoothDevice(deviceName: String): Boolean {
            val elm327Patterns = listOf(
                "ELM327", "ELM 327", "OBDII", "OBD-II", "OBD II",
                "V-LINK", "VLINK", "CAN-BT", "CANBT"
            )
            
            return elm327Patterns.any { pattern ->
                deviceName.contains(pattern, ignoreCase = true)
            }
        }
        
        /**
         * Check if device is a generic OBD adapter
         */
        private fun isGenericObdDevice(deviceName: String): Boolean {
            val genericObdPatterns = listOf(
                "OBD", "DIAGNOSTIC", "SCANNER", "VGATE", "KONNWEI",
                "LAUNCH", "AUTEL", "FOXWELL", "TORQUE", "BLUETOOTH"
            )
            
            return genericObdPatterns.any { pattern ->
                deviceName.contains(pattern, ignoreCase = true)
            }
        }
        
        /**
         * Get supported adapter types
         */
        fun getSupportedAdapterTypes(): List<AdapterType> {
            return listOf(
                AdapterType.BLUETOOTH_CLASSIC,
                AdapterType.NATIVE_AUTEL,
                AdapterType.NATIVE_J2534
            )
        }
        
        /**
         * Get adapter capabilities for a given type
         */
        fun getAdapterCapabilities(adapterType: AdapterType): Set<AdapterCapability> {
            return when (adapterType) {
                AdapterType.BLUETOOTH_CLASSIC -> setOf(
                    AdapterCapability.AUTO_PROTOCOL_DETECTION,
                    AdapterCapability.REAL_TIME_DATA,
                    AdapterCapability.DTC_MANAGEMENT,
                    AdapterCapability.VEHICLE_IDENTIFICATION
                )
                
                AdapterType.NATIVE_AUTEL -> setOf(
                    AdapterCapability.AUTO_PROTOCOL_DETECTION,
                    AdapterCapability.ADVANCED_DIAGNOSTICS,
                    AdapterCapability.BIDIRECTIONAL_CONTROL,
                    AdapterCapability.HIGH_SPEED_CAN,
                    AdapterCapability.MULTI_ECU_SUPPORT,
                    AdapterCapability.REAL_TIME_DATA,
                    AdapterCapability.DTC_MANAGEMENT,
                    AdapterCapability.VEHICLE_IDENTIFICATION
                )
                
                AdapterType.NATIVE_J2534 -> setOf(
                    AdapterCapability.AUTO_PROTOCOL_DETECTION,
                    AdapterCapability.ADVANCED_DIAGNOSTICS,
                    AdapterCapability.BIDIRECTIONAL_CONTROL,
                    AdapterCapability.HIGH_SPEED_CAN,
                    AdapterCapability.REAL_TIME_DATA,
                    AdapterCapability.DTC_MANAGEMENT,
                    AdapterCapability.VEHICLE_IDENTIFICATION
                )
                
                else -> emptySet()
            }
        }
    }
}

package com.spacetec.diagnostic.vci

import android.util.Log

object NativeCapabilities {
    private const val TAG = "NativeCapabilities"
    
    private var j2534Loaded = false
    private var obdLibLoaded = false
    
    fun loadUniversalLibraries() {
        try {
            if (!j2534Loaded) {
                System.loadLibrary("j2534")
                j2534Loaded = true
                Log.i(TAG, "J2534 library loaded successfully")
            }
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Failed to load J2534 library: ${e.message}")
        }
        
        try {
            if (!obdLibLoaded) {
                System.loadLibrary("obd_universal")
                obdLibLoaded = true
                Log.i(TAG, "Universal OBD library loaded successfully")
            }
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Failed to load universal OBD library: ${e.message}")
        }
    }
    
    fun isJ2534Available(): Boolean = j2534Loaded
    fun isUniversalObdAvailable(): Boolean = obdLibLoaded
    
    fun getAvailableProtocols(): List<String> {
        val protocols = mutableListOf<String>()
        if (j2534Loaded) protocols.add("J2534")
        if (obdLibLoaded) protocols.add("OBD-II")
        return protocols
    }
}

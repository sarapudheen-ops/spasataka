package com.spacetec.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages runtime permissions for Bluetooth and Location access
 */
class PermissionManager(private val activity: ComponentActivity) {
    
    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()
    
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    
    data class PermissionState(
        val bluetoothPermissionsGranted: Boolean = false,
        val locationPermissionsGranted: Boolean = false,
        val allPermissionsGranted: Boolean = false,
        val showRationale: Boolean = false,
        val deniedPermissions: List<String> = emptyList()
    )
    
    init {
        setupPermissionLauncher()
        checkCurrentPermissions()
    }
    
    private fun setupPermissionLauncher() {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionResults(permissions)
        }
    }
    
    /**
     * Get required permissions based on Android version
     */
    private fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }
    
    /**
     * Check current permission status
     */
    fun checkCurrentPermissions() {
        val bluetoothGranted = checkBluetoothPermissions()
        val locationGranted = checkLocationPermissions()
        
        _permissionState.value = _permissionState.value.copy(
            bluetoothPermissionsGranted = bluetoothGranted,
            locationPermissionsGranted = locationGranted,
            allPermissionsGranted = bluetoothGranted && locationGranted
        )
    }
    
    /**
     * Check Bluetooth permissions
     */
    private fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.BLUETOOTH) &&
            hasPermission(Manifest.permission.BLUETOOTH_ADMIN)
        }
    }
    
    /**
     * Check Location permissions
     */
    private fun checkLocationPermissions(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
               hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    
    /**
     * Check if specific permission is granted
     */
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Request all required permissions
     */
    fun requestPermissions() {
        val requiredPermissions = getRequiredPermissions()
        val missingPermissions = requiredPermissions.filter { !hasPermission(it) }
        
        if (missingPermissions.isEmpty()) {
            checkCurrentPermissions()
            return
        }
        
        // Check if we should show rationale for any permission
        val shouldShowRationale = missingPermissions.any { permission ->
            activity.shouldShowRequestPermissionRationale(permission)
        }
        
        _permissionState.value = _permissionState.value.copy(
            showRationale = shouldShowRationale,
            deniedPermissions = missingPermissions
        )
        
        permissionLauncher.launch(missingPermissions.toTypedArray())
    }
    
    /**
     * Handle permission request results
     */
    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        val deniedPermissions = permissions.filter { !it.value }.keys.toList()
        
        checkCurrentPermissions()
        
        _permissionState.value = _permissionState.value.copy(
            deniedPermissions = deniedPermissions,
            showRationale = false
        )
    }
    
    /**
     * Get user-friendly permission explanation
     */
    fun getPermissionExplanation(): String {
        val currentState = _permissionState.value
        
        return when {
            !currentState.bluetoothPermissionsGranted && !currentState.locationPermissionsGranted -> {
                "🛰️ Space-Tec needs Bluetooth and Location permissions to:\n\n" +
                "• 📡 Connect to your vehicle's OBD-II port\n" +
                "• 🔍 Scan for nearby diagnostic devices\n" +
                "• 🚀 Monitor your spacecraft's vital systems\n\n" +
                "These permissions are essential for mission success!"
            }
            !currentState.bluetoothPermissionsGranted -> {
                "📡 Bluetooth permission is required to establish communication with your vehicle's diagnostic system."
            }
            !currentState.locationPermissionsGranted -> {
                "🗺️ Location permission is needed to scan for nearby Bluetooth devices (Android requirement)."
            }
            else -> {
                "✅ All systems ready for launch!"
            }
        }
    }
    
    /**
     * Get missing permissions for display
     */
    fun getMissingPermissionsText(): String {
        val missing = mutableListOf<String>()
        
        if (!_permissionState.value.bluetoothPermissionsGranted) {
            missing.add("Bluetooth")
        }
        if (!_permissionState.value.locationPermissionsGranted) {
            missing.add("Location")
        }
        
        return when (missing.size) {
            0 -> ""
            1 -> missing.first()
            else -> missing.joinToString(" and ")
        }
    }
}

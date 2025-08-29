package com.spacetec.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.spacetec.R
import com.spacetec.bluetooth.SpaceBluetoothManager
import kotlinx.coroutines.launch
import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.collectLatest

class BluetoothConnectionActivity : AppCompatActivity() {
    
    private lateinit var bluetoothManager: SpaceBluetoothManager
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var btnScan: Button
    private lateinit var btnConnect: Button
    private lateinit var recyclerViewDevices: RecyclerView
    private lateinit var devicesAdapter: BluetoothDevicesAdapter
    
    private val discoveredDevices = mutableListOf<BluetoothDevice>()
    private var selectedDevice: BluetoothDevice? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_connection)
        
        // Setup toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Bluetooth Connection"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        
        // Initialize views
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)
        btnScan = findViewById(R.id.btnScan)
        btnConnect = findViewById(R.id.btnConnect)
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices)
        
        // Initialize Bluetooth manager
        bluetoothManager = SpaceBluetoothManager(this)
        
        // Setup RecyclerView
        devicesAdapter = BluetoothDevicesAdapter(discoveredDevices) { device ->
            selectedDevice = device
            val name = try { device.name } catch (_: SecurityException) { null }
            updateStatus("Selected device: ${name ?: device.address}")
            btnConnect.isEnabled = true
        }
        
        recyclerViewDevices.apply {
            layoutManager = LinearLayoutManager(this@BluetoothConnectionActivity)
            adapter = devicesAdapter
        }
        
        // Set up button listeners
        setupButtonListeners()
        
        // Update initial status
        updateStatus("Ready to scan for Bluetooth devices")
        btnConnect.isEnabled = false
    }
    
    private fun setupButtonListeners() {
        btnScan.setOnClickListener {
            if (checkBluetoothPermissions()) {
                startBluetoothScan()
            } else {
                requestBluetoothPermissions()
            }
        }
        
        btnConnect.setOnClickListener {
            selectedDevice?.let { device ->
                connectToDevice(device)
            }
        }
    }
    
    private fun checkBluetoothPermissions(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestBluetoothPermissions() {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
        
        ActivityCompat.requestPermissions(this, permissions, 1001)
    }
    
    private fun startBluetoothScan() {
        showProgress(true)
        updateStatus("Scanning for Bluetooth devices...")
        discoveredDevices.clear()
        devicesAdapter.notifyDataSetChanged()
        
        lifecycleScope.launch {
            try {
                // Start Bluetooth scan using manager
                bluetoothManager.startDeviceDiscovery()

                // Collect discovered devices from StateFlow
                lifecycleScope.launch {
                    bluetoothManager.discoveredDevices.collectLatest { devices ->
                        runOnUiThread {
                            discoveredDevices.clear()
                            discoveredDevices.addAll(devices)
                            devicesAdapter.notifyDataSetChanged()
                            updateStatus("Found ${discoveredDevices.size} devices")
                        }
                    }
                }
                
                // Stop scan after 10 seconds
                kotlinx.coroutines.delay(10000)
                bluetoothManager.stopDeviceDiscovery()
                
                updateStatus("Scan completed. Found ${discoveredDevices.size} devices")
            } catch (e: Exception) {
                Log.e("BluetoothConnection", "Scan error", e)
                updateStatus("Scan error: ${e.message}")
            } finally {
                showProgress(false)
            }
        }
    }
    
    private fun connectToDevice(device: BluetoothDevice) {
        showProgress(true)
        val display = try { device.name } catch (_: SecurityException) { null } ?: device.address
        updateStatus("Connecting to $display...")
        
        lifecycleScope.launch {
            try {
                val success = bluetoothManager.connectToDevice(device)
                if (success) {
                    updateStatus("Connected to $display successfully")
                } else {
                    updateStatus("Failed to connect to $display")
                }
            } catch (e: Exception) {
                Log.e("BluetoothConnection", "Connection error", e)
                updateStatus("Connection error: ${e.message}")
            } finally {
                showProgress(false)
            }
        }
    }
    
    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnScan.isEnabled = !show
        btnConnect.isEnabled = !show && selectedDevice != null
    }
    
    private fun updateStatus(message: String) {
        runOnUiThread {
            tvStatus.text = message
            Log.d("BluetoothConnection", message)
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                updateStatus("Permissions granted. Ready to scan.")
            } else {
                updateStatus("Bluetooth permissions required for scanning")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.cleanup()
    }
}

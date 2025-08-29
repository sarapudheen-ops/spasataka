package com.spacetec.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.spacetec.R
import kotlinx.coroutines.launch

data class DiagnosticFeature(
    val id: String,
    val title: String,
    val description: String,
    val iconResId: Int = android.R.drawable.ic_menu_manage,
    val requiresConnection: Boolean = true
)

class DiagnosticsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var featuresAdapter: DiagnosticFeaturesAdapter

    private val diagnosticFeatures = listOf(
        DiagnosticFeature(
            id = "vehicle_selection",
            title = "Vehicle Selection",
            description = "Select your vehicle for enhanced diagnostics",
            requiresConnection = false
        ),
        DiagnosticFeature(
            id = "bluetooth_connection",
            title = "Bluetooth Connection",
            description = "Scan and connect to Bluetooth devices",
            requiresConnection = false
        ),
        DiagnosticFeature(
            id = "live_data",
            title = "Live Data",
            description = "View real-time vehicle parameters",
            requiresConnection = true
        ),
        DiagnosticFeature(
            id = "read_codes",
            title = "Read Codes",
            description = "Read diagnostic trouble codes",
            requiresConnection = true
        ),
        DiagnosticFeature(
            id = "clear_codes",
            title = "Clear Codes",
            description = "Clear diagnostic trouble codes",
            requiresConnection = true
        ),
        DiagnosticFeature(
            id = "vehicle_info",
            title = "Vehicle Information",
            description = "Display vehicle identification data",
            requiresConnection = true
        ),
        DiagnosticFeature(
            id = "remote_start",
            title = "Remote Start",
            description = "Remote engine start/stop functionality",
            requiresConnection = false
        ),
        DiagnosticFeature(
            id = "ecu_flashing",
            title = "ECU Flashing",
            description = "Update ECU firmware with security access",
            requiresConnection = true
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostics)

        // Setup toolbar as action bar for proper title/back handling
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Diagnostics"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }


        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewFeatures)
        featuresAdapter = DiagnosticFeaturesAdapter(diagnosticFeatures) { feature ->
            Log.d("DiagnosticsActivity", "Feature clicked: ${feature.id}")
            Toast.makeText(this, "Opening ${feature.title}...", Toast.LENGTH_SHORT).show()
            onFeatureSelected(feature)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@DiagnosticsActivity)
            adapter = featuresAdapter
        }
    }

    private fun onFeatureSelected(feature: DiagnosticFeature) {
        when (feature.id) {
            "vehicle_selection" -> {
                startActivity(Intent(this, VehicleSelectionActivity::class.java))
            }
            "bluetooth_connection" -> {
                startActivity(Intent(this, BluetoothConnectionActivity::class.java))
            }
            "live_data" -> {
                startActivity(Intent(this, LiveDataActivity::class.java))
            }
            "read_codes" -> {
                readDtcCodes()
            }
            "clear_codes" -> {
                clearDtcCodes()
            }
            "remote_start" -> {
                startActivity(Intent(this, RemoteStartActivity::class.java))
            }
            "ecu_flashing" -> {
                startActivity(Intent(this, EcuFlashingActivity::class.java))
            }
        }
    }

    private fun readDtcCodes() {
        Toast.makeText(this, "DTC reading functionality will be implemented with universal OBD adapter", Toast.LENGTH_SHORT).show()
    }

    private fun clearDtcCodes() {
        Toast.makeText(this, "DTC clearing functionality will be implemented with universal OBD adapter", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

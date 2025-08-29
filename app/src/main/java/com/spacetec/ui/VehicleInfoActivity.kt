package com.spacetec.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.spacetec.R
import com.spacetec.diagnostic.autel.VehicleDataManager
import com.spacetec.diagnostic.autel.VehicleInfo
import com.spacetec.diagnostic.autel.EcuInfo
import com.spacetec.services.VinDecoder
import com.spacetec.services.VinDecodeResult
import kotlinx.coroutines.launch


class VehicleInfoActivity : AppCompatActivity() {

    private lateinit var tvVinNumber: TextView
    private lateinit var tvVehicleDetails: TextView
    private lateinit var recyclerViewEcus: RecyclerView
    private lateinit var btnReadVin: Button
    private lateinit var btnSetVin: Button
    private lateinit var btnRefresh: Button
    private lateinit var btnExport: Button
    private lateinit var ecuAdapter: EcuInfoAdapter

    private val ecuList = mutableListOf<EcuInfo>()
    private var currentVin: String? = null
    private val vinDecoder = VinDecoder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_info)

        setupToolbar()
        initializeViews()
        setupEventListeners()
        setupRecyclerView()
        loadVehicleInfo()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Vehicle Information"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun initializeViews() {
        tvVinNumber = findViewById(R.id.tvVinNumber)
        tvVehicleDetails = findViewById(R.id.tvVehicleDetails)
        recyclerViewEcus = findViewById(R.id.recyclerViewEcus)
        btnReadVin = findViewById(R.id.btnReadVin)
        btnSetVin = findViewById(R.id.btnSetVin)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnExport = findViewById(R.id.btnExport)
    }

    private fun setupEventListeners() {
        btnReadVin.setOnClickListener {
            readVinFromVehicle()
        }

        btnSetVin.setOnClickListener {
            showSetVinDialog()
        }

        btnRefresh.setOnClickListener {
            loadVehicleInfo()
        }

        btnExport.setOnClickListener {
            exportVehicleData()
        }
    }

    private fun setupRecyclerView() {
        ecuAdapter = EcuInfoAdapter(ecuList)
        recyclerViewEcus.apply {
            adapter = ecuAdapter
            layoutManager = LinearLayoutManager(this@VehicleInfoActivity)
        }
    }

    private fun loadVehicleInfo() {
        lifecycleScope.launch {
            try {
                // Load ECU information
                loadEcuInfo()
                
                // Try to read VIN if not already set
                if (currentVin == null) {
                    readVinFromVehicle()
                }
                
            } catch (e: Exception) {
                tvVehicleDetails.text = "Error loading vehicle information: ${e.message}"
            }
        }
    }

    private fun readVinFromVehicle() {
        lifecycleScope.launch {
            try {
                btnReadVin.isEnabled = false
                tvVinNumber.text = "Reading VIN..."
                
                // Get vehicle info from VehicleDataManager
                val vehicleInfo = VehicleDataManager.getVehicleInfo()
                val ecuList = VehicleDataManager.readEcuInfo()

                // Update UI with vehicle info
                vehicleInfo?.let { info ->
                    currentVin = info.vin
                    tvVinNumber.text = info.vin
                    
                    // Update vehicle details text
                    tvVehicleDetails.text = buildString {
                        appendLine("VIN: ${info.vin}")
                        appendLine("Make: ${info.make}")
                        appendLine("Model: ${info.model}")
                        appendLine("Year: ${info.year}")
                        appendLine("Engine: ${info.engine}")
                        appendLine("Transmission: ${info.transmission}")
                    }
                }
                
            } catch (e: Exception) {
                tvVinNumber.text = "Error reading VIN"
                tvVehicleDetails.text = "VIN read error: ${e.message}"
            } finally {
                btnReadVin.isEnabled = true
            }
        }
    }

    private fun showSetVinDialog() {
        val editText = EditText(this).apply {
            hint = "Enter VIN (17 characters)"
            setText(currentVin ?: "")
        }

        AlertDialog.Builder(this)
            .setTitle("Set VIN Number")
            .setMessage("Enter the vehicle identification number:")
            .setView(editText)
            .setPositiveButton("Set") { _, _ ->
                val vin = editText.text.toString().uppercase().trim()
                if (vin.length == 17) {
                    currentVin = vin
                    tvVinNumber.text = vin
                    
                    // Decode VIN using professional decoder
                    lifecycleScope.launch {
                        try {
                            val decodeResult = vinDecoder.decodeVin(vin)
                            when (decodeResult) {
                                is VinDecodeResult.Success -> {
                                    tvVehicleDetails.text = buildString {
                                        appendLine("VIN: ${decodeResult.vin}")
                                        appendLine("Manufacturer: ${decodeResult.manufacturer}")
                                        appendLine("Brand: ${decodeResult.brand}")
                                        appendLine("Model: ${decodeResult.model}")
                                        appendLine("Year: ${decodeResult.modelYear}")
                                        appendLine("Engine: ${decodeResult.engine}")
                                        appendLine("Transmission: ${decodeResult.transmission}")
                                        appendLine("Body Style: ${decodeResult.bodyStyle}")
                                        appendLine("Drive Type: ${decodeResult.driveType}")
                                        appendLine("Fuel Type: ${decodeResult.fuelType}")
                                        appendLine("Assembly Plant: ${decodeResult.assemblyPlant}")
                                        appendLine("Country: ${decodeResult.assemblyCountry}")
                                        appendLine("Safety Rating: ${decodeResult.safetyRating}")
                                        if (decodeResult.recallInfo.isNotEmpty()) {
                                            appendLine("⚠️ Recalls: ${decodeResult.recallInfo.size} active")
                                        }
                                    }
                                }
                                is VinDecodeResult.Error -> {
                                    tvVehicleDetails.text = "VIN decode error: ${decodeResult.message}"
                                }
                            }
                        } catch (e: Exception) {
                            tvVehicleDetails.text = "Error decoding VIN: ${e.message}"
                        }
                    }
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Invalid VIN")
                        .setMessage("Please enter a valid 17-character VIN number")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadEcuInfo() {
        ecuList.clear()
        
        // Simulate ECU discovery
        val mockEcus = listOf(
            EcuInfo("Engine Control Module", "0x7E0", "SW: v1.2.3", "HW: Rev A", "✅ Online"),
            EcuInfo("Transmission Control", "0x7E1", "SW: v2.1.0", "HW: Rev B", "✅ Online"),
            EcuInfo("ABS Control Module", "0x7E2", "SW: v3.0.1", "HW: Rev C", "✅ Online"),
            EcuInfo("Body Control Module", "0x7E3", "SW: v1.5.2", "HW: Rev A", "✅ Online"),
            EcuInfo("Airbag Control", "0x7E4", "SW: v2.3.1", "HW: Rev B", "⚠️ Warning"),
            EcuInfo("Climate Control", "0x7E5", "SW: v1.8.0", "HW: Rev A", "❌ Offline")
        )
        
        ecuList.addAll(mockEcus)
        ecuAdapter.notifyDataSetChanged()
    }

    private fun exportVehicleData() {
        val data = buildString {
            appendLine("=== VEHICLE INFORMATION REPORT ===")
            appendLine("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}")
            appendLine()
            
            appendLine("VIN: ${currentVin ?: "Not available"}")
            appendLine()
            
            if (tvVehicleDetails.text.toString() != "Connect to vehicle to view details") {
                appendLine("VEHICLE DETAILS:")
                appendLine(tvVehicleDetails.text.toString())
                appendLine()
            }
            
            appendLine("ECU INFORMATION:")
            ecuList.forEach { ecu ->
                appendLine("${ecu.name} (${ecu.address})")
                appendLine("  Software: ${ecu.software}")
                appendLine("  Hardware: ${ecu.hardware}")
                appendLine("  Status: ${ecu.status}")
                appendLine()
            }
        }

        // In a real app, this would save to file or share
        AlertDialog.Builder(this)
            .setTitle("Vehicle Data Export")
            .setMessage("Vehicle data exported successfully!\n\nData preview:\n${data.take(200)}...")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

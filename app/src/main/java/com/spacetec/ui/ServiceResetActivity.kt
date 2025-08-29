package com.spacetec.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.spacetec.R
import com.spacetec.diagnostic.autel.VehicleDataManager
import kotlinx.coroutines.launch

class ServiceResetActivity : AppCompatActivity() {

    private lateinit var tvOilStatus: TextView
    private lateinit var tvBrakeStatus: TextView
    private lateinit var tvServiceStatus: TextView
    private lateinit var btnResetOil: Button
    private lateinit var btnResetBrake: Button
    private lateinit var btnResetService: Button
    private lateinit var btnResetBattery: Button
    private lateinit var btnResetSteering: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_reset)

        setupToolbar()
        initializeViews()
        loadServiceStatus()
        setupEventListeners()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Service Reset"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun initializeViews() {
        tvOilStatus = findViewById(R.id.tvOilStatus)
        tvBrakeStatus = findViewById(R.id.tvBrakeStatus)
        tvServiceStatus = findViewById(R.id.tvServiceStatus)
        btnResetOil = findViewById(R.id.btnResetOil)
        btnResetBrake = findViewById(R.id.btnResetBrake)
        btnResetService = findViewById(R.id.btnResetService)
        btnResetBattery = findViewById(R.id.btnResetBattery)
        btnResetSteering = findViewById(R.id.btnResetSteering)
    }

    private fun loadServiceStatus() {
        lifecycleScope.launch {
            try {
                // Simulate reading service intervals from vehicle
                val oilRemaining = (1000..5000).random()
                val brakeRemaining = (10000..20000).random()
                val serviceRemaining = (5000..15000).random()

                tvOilStatus.text = "Current: $oilRemaining km remaining"
                tvBrakeStatus.text = "Current: $brakeRemaining km remaining"
                tvServiceStatus.text = "Current: $serviceRemaining km remaining"
            } catch (e: Exception) {
                Toast.makeText(this@ServiceResetActivity, "Failed to load service status", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupEventListeners() {
        btnResetOil.setOnClickListener {
            showResetConfirmation("Oil Service") {
                performServiceReset("oil")
            }
        }

        btnResetBrake.setOnClickListener {
            showResetConfirmation("Brake Service") {
                performServiceReset("brake")
            }
        }

        btnResetService.setOnClickListener {
            showResetConfirmation("Service Interval") {
                performServiceReset("service")
            }
        }

        btnResetBattery.setOnClickListener {
            showResetConfirmation("Battery System") {
                performServiceReset("battery")
            }
        }

        btnResetSteering.setOnClickListener {
            showResetConfirmation("Steering Angle") {
                performServiceReset("steering")
            }
        }
    }

    private fun showResetConfirmation(serviceName: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Service Reset")
            .setMessage("Are you sure you want to reset the $serviceName?\n\nThis should only be done after completing the corresponding maintenance work.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Reset") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performServiceReset(serviceType: String) {
        lifecycleScope.launch {
            try {
                // Show progress
                val progressDialog = AlertDialog.Builder(this@ServiceResetActivity)
                    .setTitle("Resetting Service")
                    .setMessage("Please wait...")
                    .setCancelable(false)
                    .create()
                progressDialog.show()

                // Simulate service reset operation
                when (serviceType) {
                    "oil" -> {
                        val success = VehicleDataManager.resetOilService()
                        progressDialog.dismiss()
                        if (success) {
                            tvOilStatus.text = "Current: Service reset successfully"
                            Toast.makeText(this@ServiceResetActivity, "Oil service reset completed", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@ServiceResetActivity, "Oil service reset failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "brake" -> {
                        val success = VehicleDataManager.resetBrakeService()
                        progressDialog.dismiss()
                        if (success) {
                            tvBrakeStatus.text = "Current: Service reset successfully"
                            Toast.makeText(this@ServiceResetActivity, "Brake service reset completed", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@ServiceResetActivity, "Brake service reset failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "service" -> {
                        val success = VehicleDataManager.resetServiceInterval()
                        progressDialog.dismiss()
                        if (success) {
                            tvServiceStatus.text = "Current: Service reset successfully"
                            Toast.makeText(this@ServiceResetActivity, "Service interval reset completed", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@ServiceResetActivity, "Service interval reset failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "battery" -> {
                        val success = VehicleDataManager.resetBatterySystem()
                        progressDialog.dismiss()
                        if (success) {
                            Toast.makeText(this@ServiceResetActivity, "Battery system reset completed", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@ServiceResetActivity, "Battery system reset failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "steering" -> {
                        val success = VehicleDataManager.resetSteeringAngle()
                        progressDialog.dismiss()
                        if (success) {
                            Toast.makeText(this@ServiceResetActivity, "Steering angle reset completed", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@ServiceResetActivity, "Steering angle reset failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(this@ServiceResetActivity, "Service reset failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

package com.spacetec.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.spacetec.R
import com.spacetec.services.RemoteStartService
import kotlinx.coroutines.launch

class RemoteStartActivity : AppCompatActivity() {
    
    private lateinit var remoteStartService: RemoteStartService
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var btnCheckReadiness: Button
    private lateinit var btnStartEngine: Button
    private lateinit var btnStopEngine: Button
    private lateinit var btnStatus: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remote_start)
        
        supportActionBar?.title = "Remote Start"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Initialize views
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)
        btnCheckReadiness = findViewById(R.id.btnCheckReadiness)
        btnStartEngine = findViewById(R.id.btnStartEngine)
        btnStopEngine = findViewById(R.id.btnStopEngine)
        btnStatus = findViewById(R.id.btnStatus)
        
        // Initialize service
        remoteStartService = RemoteStartService(this)
        
        // Set up button listeners
        setupButtonListeners()
        
        // Update initial status
        updateStatus("Remote start system ready")
    }
    
    private fun setupButtonListeners() {
        btnCheckReadiness.setOnClickListener {
            checkReadiness()
        }
        
        btnStartEngine.setOnClickListener {
            showStartEngineDialog()
        }
        
        btnStopEngine.setOnClickListener {
            stopEngine()
        }
        
        btnStatus.setOnClickListener {
            showStatus()
        }
    }
    
    private fun checkReadiness() {
        showProgress(true)
        updateStatus("Checking vehicle readiness...")
        
        lifecycleScope.launch {
            try {
                val ready = remoteStartService.checkVehicleReadiness()
                updateStatus(if (ready) "✅ Vehicle ready for remote start" else "❌ Vehicle not ready")
            } catch (e: Exception) {
                updateStatus("Error checking readiness: ${e.message}")
            } finally {
                showProgress(false)
            }
        }
    }
    
    private fun showStartEngineDialog() {
        AlertDialog.Builder(this)
            .setTitle("Remote Engine Start")
            .setMessage("Are you sure you want to start the engine remotely?\n\nThis should only be used in emergency situations.")
            .setPositiveButton("Start") { _, _ ->
                startEngine()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun startEngine() {
        showProgress(true)
        updateStatus("Starting engine remotely...")
        
        lifecycleScope.launch {
            try {
                val result = remoteStartService.startEngineRemotely(
                    vin = "1G1JC5444R7252367", // Sample VIN
                    techCode = "123456",
                    verify = true
                )
                
                if (result.success) {
                    updateStatus("✅ ${result.message}")
                } else {
                    updateStatus("❌ ${result.message}")
                }
                
                if (result.warnings.isNotEmpty()) {
                    showWarningsDialog(result.warnings)
                }
            } catch (e: Exception) {
                updateStatus("Start error: ${e.message}")
            } finally {
                showProgress(false)
            }
        }
    }
    
    private fun stopEngine() {
        showProgress(true)
        updateStatus("Stopping engine...")
        
        lifecycleScope.launch {
            try {
                val result = remoteStartService.stopEngineRemotely()
                updateStatus(if (result.success) "✅ ${result.message}" else "❌ ${result.message}")
            } catch (e: Exception) {
                updateStatus("Stop error: ${e.message}")
            } finally {
                showProgress(false)
            }
        }
    }
    
    private fun showStatus() {
        val status = remoteStartService.getEngineStatus()
        val message = if (status["running"] as Boolean) {
            "🔥 Engine Status: RUNNING\n" +
                "RPM: ${status["rpm"]}\n" +
                "Runtime: ${status["runTimeMinutes"]} min\n" +
                "Remaining: ${status["remainingMinutes"]} min"
        } else {
            "⭕ Engine Status: STOPPED"
        }

        AlertDialog.Builder(this)
            .setTitle("Engine Status")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    private fun showWarningsDialog(warnings: List<String>) {
        AlertDialog.Builder(this)
            .setTitle("Warnings")
            .setMessage(warnings.joinToString("\n"))
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnCheckReadiness.isEnabled = !show
        btnStartEngine.isEnabled = !show
        btnStopEngine.isEnabled = !show
        btnStatus.isEnabled = !show
    }
    
    private fun updateStatus(message: String) {
        runOnUiThread {
            tvStatus.text = message
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

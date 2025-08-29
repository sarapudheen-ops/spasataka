package com.spacetec.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.spacetec.R
import com.spacetec.diagnostic.autel.VehicleDataManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ScanResult(
    val ecuName: String,
    val status: String,
    val dtcCount: Int,
    val responseTime: String
)

class QuickScanActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var cardProgress: View
    private lateinit var cardResults: View
    private lateinit var recyclerViewResults: RecyclerView
    private lateinit var btnStartScan: Button
    private lateinit var btnStopScan: Button
    private lateinit var resultsAdapter: QuickScanResultsAdapter

    private var isScanning = false
    private val scanResults = mutableListOf<ScanResult>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_scan)

        setupToolbar()
        initializeViews()
        setupEventListeners()
        setupRecyclerView()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Quick Scan"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun initializeViews() {
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)
        cardProgress = findViewById(R.id.cardProgress)
        cardResults = findViewById(R.id.cardResults)
        recyclerViewResults = findViewById(R.id.recyclerViewResults)
        btnStartScan = findViewById(R.id.btnStartScan)
        btnStopScan = findViewById(R.id.btnStopScan)
    }

    private fun setupEventListeners() {
        btnStartScan.setOnClickListener {
            startQuickScan()
        }

        btnStopScan.setOnClickListener {
            stopQuickScan()
        }
    }

    private fun setupRecyclerView() {
        resultsAdapter = QuickScanResultsAdapter(scanResults)
        recyclerViewResults.apply {
            adapter = resultsAdapter
            layoutManager = LinearLayoutManager(this@QuickScanActivity)
        }
    }

    private fun startQuickScan() {
        if (isScanning) return

        isScanning = true
        scanResults.clear()
        updateScanUI(true)
        updateStatus("Starting quick diagnostic scan...")

        lifecycleScope.launch {
            try {
                val ecuList = listOf(
                    "Engine Control Module" to "0x7E0",
                    "Transmission Control" to "0x7E1", 
                    "ABS Control Module" to "0x7E2",
                    "Body Control Module" to "0x7E3",
                    "Airbag Control Module" to "0x7E4",
                    "Climate Control" to "0x7E5"
                )

                ecuList.forEachIndexed { index, (ecuName, ecuId) ->
                    if (!isScanning) return@launch

                    val progress = ((index + 1) * 100) / ecuList.size
                    updateProgress(progress, "Scanning $ecuName...")

                    // Simulate ECU scan
                    delay(1500)

                    // Generate scan result
                    val dtcCount = (0..3).random()
                    val responseTime = "${(10..150).random()}ms"
                    val status = when {
                        dtcCount == 0 -> "✅ OK"
                        dtcCount <= 1 -> "⚠️ Warning"
                        else -> "❌ Error"
                    }

                    val result = ScanResult(ecuName, status, dtcCount, responseTime)
                    scanResults.add(result)
                    
                    runOnUiThread {
                        resultsAdapter.notifyItemInserted(scanResults.size - 1)
                        cardResults.visibility = View.VISIBLE
                    }
                }

                if (isScanning) {
                    updateProgress(100, "Quick scan completed")
                    updateStatus("✅ Quick scan completed - Found ${scanResults.sumOf { it.dtcCount }} total DTCs")
                }

            } catch (e: Exception) {
                updateStatus("❌ Scan failed: ${e.message}")
            } finally {
                isScanning = false
                updateScanUI(false)
            }
        }
    }

    private fun stopQuickScan() {
        isScanning = false
        updateStatus("Scan stopped by user")
        updateScanUI(false)
    }

    private fun updateScanUI(scanning: Boolean) {
        cardProgress.visibility = if (scanning) View.VISIBLE else View.GONE
        btnStartScan.visibility = if (scanning) View.GONE else View.VISIBLE
        btnStopScan.visibility = if (scanning) View.VISIBLE else View.GONE
    }

    private fun updateProgress(progress: Int, message: String) {
        runOnUiThread {
            progressBar.progress = progress
            tvProgress.text = "$progress%"
            updateStatus(message)
        }
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            tvStatus.text = message
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

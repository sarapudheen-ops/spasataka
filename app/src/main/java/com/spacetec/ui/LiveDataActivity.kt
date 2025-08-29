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
import com.spacetec.diagnostic.obd.PidCalculator
import com.spacetec.diagnostic.vehicle.VehicleLibraryManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class LiveDataItem(
    val pid: String,
    val name: String,
    val value: String,
    val unit: String,
    val timestamp: Long = System.currentTimeMillis()
)

class LiveDataActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var liveDataAdapter: LiveDataAdapter
    private lateinit var btnStartStop: Button
    private lateinit var btnRecord: Button
    private lateinit var btnSnapshot: Button
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar

    private var isStreaming = false
    private var isRecording = false
    private val liveDataItems = mutableListOf<LiveDataItem>()
    private lateinit var vehicleLibraryManager: VehicleLibraryManager

    private val standardPIDs = listOf(
        "0C" to "Engine RPM",
        "0D" to "Vehicle Speed",
        "05" to "Coolant Temperature",
        "0F" to "Intake Air Temperature",
        "11" to "Throttle Position",
        "04" to "Engine Load",
        "2F" to "Fuel Level",
        "42" to "Control Module Voltage",
        "43" to "Absolute Load Value"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_data)

        supportActionBar?.title = "Live Data"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        vehicleLibraryManager = VehicleLibraryManager(this)
        setupUI()
    }

    private fun setupUI() {
        recyclerView = findViewById(R.id.recyclerViewLiveData)
        btnStartStop = findViewById(R.id.buttonStartStop)
        btnRecord = findViewById(R.id.buttonRecord)
        btnSnapshot = findViewById(R.id.buttonSnapshot)
        tvStatus = findViewById(R.id.textStatus)
        progressBar = findViewById(R.id.progressBar)

        // Setup RecyclerView
        liveDataAdapter = LiveDataAdapter()
        recyclerView.apply {
            adapter = liveDataAdapter
            layoutManager = LinearLayoutManager(this@LiveDataActivity)
        }

        // Setup control buttons
        btnStartStop.setOnClickListener {
            toggleLiveDataStream()
        }

        btnRecord.setOnClickListener {
            toggleRecording()
        }

        btnSnapshot.setOnClickListener {
            takeSnapshot()
        }

        tvStatus.text = "Ready to stream live data"
    }

    private fun toggleLiveDataStream() {
        if (isStreaming) {
            stopLiveDataStream()
        } else {
            startLiveDataStream()
        }
    }

    private fun startLiveDataStream() {
        isStreaming = true
        btnStartStop.text = "Stop"
        tvStatus.text = "Streaming live data..."
        progressBar.visibility = View.VISIBLE
        simulateDataStream()
    }

    private fun simulateDataStream() {
        lifecycleScope.launch {
            while (isStreaming) {
                standardPIDs.forEach { (pid, name) ->
                    // Generate realistic raw data bytes for each PID
                    val rawData = when (pid) {
                        "0C" -> { // Engine RPM: ((A*256)+B)/4
                            val rpm = (800..6000).random()
                            val value = rpm * 4
                            byteArrayOf((value / 256).toByte(), (value % 256).toByte())
                        }
                        "0D" -> { // Vehicle Speed: A
                            val speed = (0..120).random()
                            byteArrayOf(speed.toByte())
                        }
                        "05" -> { // Coolant Temperature: A-40
                            val temp = (70..95).random()
                            byteArrayOf((temp + 40).toByte())
                        }
                        "0F" -> { // Intake Air Temperature: A-40
                            val temp = (20..60).random()
                            byteArrayOf((temp + 40).toByte())
                        }
                        "11" -> { // Throttle Position: (A*100)/255
                            val throttle = (0..100).random()
                            byteArrayOf(((throttle * 255) / 100).toByte())
                        }
                        "04" -> { // Engine Load: (A*100)/255
                            val load = (10..85).random()
                            byteArrayOf(((load * 255) / 100).toByte())
                        }
                        "2F" -> { // Fuel Level: (A*100)/255
                            val fuel = (20..100).random()
                            byteArrayOf(((fuel * 255) / 100).toByte())
                        }
                        "42" -> { // Control Module Voltage: ((A*256)+B)/1000
                            val voltage = (120..145).random() // 12.0V to 14.5V in tenths
                            val value = voltage * 100 // Convert to millivolts for formula
                            byteArrayOf((value / 256).toByte(), (value % 256).toByte())
                        }
                        "43" -> { // Absolute Load Value: ((A*256)+B)*100/255
                            val load = (10..85).random()
                            val value = (load * 255) / 100
                            byteArrayOf((value / 256).toByte(), (value % 256).toByte())
                        }
                        else -> byteArrayOf(0)
                    }
                    
                    // Use PidCalculator to get formatted value with space theme
                    val calculatedValue = PidCalculator.calculate(pid, rawData)
                    val spaceThemedName = PidCalculator.getSpaceThemedName(name)
                    
                    val item = LiveDataItem(pid, spaceThemedName, calculatedValue, "")
                    liveDataItems.add(0, item)
                    
                    // Keep only last 50 items
                    if (liveDataItems.size > 50) {
                        liveDataItems.removeAt(liveDataItems.size - 1)
                    }
                    
                    runOnUiThread {
                        liveDataAdapter.notifyItemInserted(0)
                        recyclerView.scrollToPosition(0)
                    }
                }
            }
        }
    }

    private fun stopLiveDataStream() {
        isStreaming = false
        btnStartStop.text = "Start"
        tvStatus.text = "Stopped"
        progressBar.visibility = View.GONE
    }

    private fun toggleRecording() {
        isRecording = !isRecording
        if (isRecording) {
            btnRecord.text = "Stop Recording"
            tvStatus.text = "Recording data..."
        } else {
            btnRecord.text = "Record"
            tvStatus.text = "Recording stopped"
        }
    }

    private fun takeSnapshot() {
        if (liveDataItems.isNotEmpty()) {
            tvStatus.text = "Snapshot saved: ${liveDataItems.size} PIDs"
        } else {
            tvStatus.text = "No data to snapshot"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isStreaming = false
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

package com.spacetec.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.spacetec.R
import com.spacetec.diagnostic.autel.VehicleDataManager
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class DataLoggerActivity : AppCompatActivity() {

    private lateinit var tvLoggerStatus: TextView
    private lateinit var tvLoggerDuration: TextView
    private lateinit var tvDataPoints: TextView
    private lateinit var btnStartLogging: Button
    private lateinit var btnStopLogging: Button
    private lateinit var spinnerSampleRate: Spinner
    private lateinit var recyclerPidSelection: RecyclerView
    private lateinit var recyclerCurrentData: RecyclerView
    private lateinit var btnExportCsv: Button
    private lateinit var btnExportJson: Button

    private var isLogging = false
    private var startTime = 0L
    private var dataPoints = 0
    private val handler = Handler(Looper.getMainLooper())
    private var loggingRunnable: Runnable? = null
    private val loggedData = mutableListOf<LogDataPoint>()
    private val selectedPids = mutableSetOf<String>()

    private lateinit var pidSelectionAdapter: PidSelectionAdapter
    private lateinit var currentDataAdapter: CurrentDataAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_logger)

        setupToolbar()
        initializeViews()
        setupSpinners()
        setupRecyclerViews()
        setupEventListeners()
        loadAvailablePids()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Data Logger"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun initializeViews() {
        tvLoggerStatus = findViewById(R.id.tvLoggerStatus)
        tvLoggerDuration = findViewById(R.id.tvLoggerDuration)
        tvDataPoints = findViewById(R.id.tvDataPoints)
        btnStartLogging = findViewById(R.id.btnStartLogging)
        btnStopLogging = findViewById(R.id.btnStopLogging)
        spinnerSampleRate = findViewById(R.id.spinnerSampleRate)
        recyclerPidSelection = findViewById(R.id.recyclerPidSelection)
        recyclerCurrentData = findViewById(R.id.recyclerCurrentData)
        btnExportCsv = findViewById(R.id.btnExportCsv)
        btnExportJson = findViewById(R.id.btnExportJson)
    }

    private fun setupSpinners() {
        val sampleRates = arrayOf("100ms", "250ms", "500ms", "1000ms", "2000ms")
        spinnerSampleRate.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sampleRates)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerSampleRate.setSelection(2) // Default to 500ms
    }

    private fun setupRecyclerViews() {
        pidSelectionAdapter = PidSelectionAdapter { pid, isSelected ->
            if (isSelected) {
                selectedPids.add(pid.id)
            } else {
                selectedPids.remove(pid.id)
            }
        }
        recyclerPidSelection.layoutManager = LinearLayoutManager(this)
        recyclerPidSelection.adapter = pidSelectionAdapter

        currentDataAdapter = CurrentDataAdapter()
        recyclerCurrentData.layoutManager = LinearLayoutManager(this)
        recyclerCurrentData.adapter = currentDataAdapter
    }

    private fun loadAvailablePids() {
        val availablePids = listOf(
            PidInfo("0C", "Engine RPM", "rpm"),
            PidInfo("0D", "Vehicle Speed", "km/h"),
            PidInfo("05", "Engine Coolant Temperature", "°C"),
            PidInfo("0A", "Fuel System Pressure", "kPa"),
            PidInfo("0B", "Intake Manifold Pressure", "kPa"),
            PidInfo("04", "Calculated Engine Load", "%"),
            PidInfo("06", "Short Term Fuel Trim", "%"),
            PidInfo("07", "Long Term Fuel Trim", "%"),
            PidInfo("0F", "Intake Air Temperature", "°C"),
            PidInfo("10", "MAF Air Flow Rate", "g/s"),
            PidInfo("11", "Throttle Position", "%"),
            PidInfo("1F", "Run Time Since Start", "seconds")
        )
        pidSelectionAdapter.updatePids(availablePids)
    }

    private fun setupEventListeners() {
        btnStartLogging.setOnClickListener {
            startLogging()
        }

        btnStopLogging.setOnClickListener {
            stopLogging()
        }

        btnExportCsv.setOnClickListener {
            exportToCsv()
        }

        btnExportJson.setOnClickListener {
            exportToJson()
        }
    }

    private fun startLogging() {
        if (selectedPids.isEmpty()) {
            Toast.makeText(this, "Please select at least one parameter to log", Toast.LENGTH_SHORT).show()
            return
        }

        isLogging = true
        startTime = System.currentTimeMillis()
        dataPoints = 0
        loggedData.clear()

        tvLoggerStatus.text = "🔴 Recording"
        btnStartLogging.isEnabled = false
        btnStopLogging.isEnabled = true

        val sampleRateMs = when (spinnerSampleRate.selectedItemPosition) {
            0 -> 100L
            1 -> 250L
            2 -> 500L
            3 -> 1000L
            4 -> 2000L
            else -> 500L
        }

        loggingRunnable = object : Runnable {
            override fun run() {
                if (isLogging) {
                    logDataPoint()
                    updateDuration()
                    handler.postDelayed(this, sampleRateMs)
                }
            }
        }
        handler.post(loggingRunnable!!)

        Toast.makeText(this, "Data logging started", Toast.LENGTH_SHORT).show()
    }

    private fun stopLogging() {
        isLogging = false
        loggingRunnable?.let { handler.removeCallbacks(it) }

        tvLoggerStatus.text = "⏹️ Stopped"
        btnStartLogging.isEnabled = true
        btnStopLogging.isEnabled = false

        Toast.makeText(this, "Data logging stopped. ${loggedData.size} data points captured", Toast.LENGTH_LONG).show()
    }

    private fun logDataPoint() {
        lifecycleScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val dataPoint = LogDataPoint(timestamp, mutableMapOf())
                val currentValues = mutableListOf<CurrentDataItem>()

                for (pidId in selectedPids) {
                    val value = VehicleDataManager.readPidValue(pidId)
                    dataPoint.values[pidId] = value
                    
                    val pidInfo = pidSelectionAdapter.getPidInfo(pidId)
                    if (pidInfo != null) {
                        currentValues.add(CurrentDataItem(pidInfo.name, value, pidInfo.unit))
                    }
                }

                loggedData.add(dataPoint)
                dataPoints++

                runOnUiThread {
                    tvDataPoints.text = dataPoints.toString()
                    currentDataAdapter.updateData(currentValues)
                }

            } catch (e: Exception) {
                // Continue logging even if one data point fails
            }
        }
    }

    private fun updateDuration() {
        val duration = System.currentTimeMillis() - startTime
        val hours = duration / 3600000
        val minutes = (duration % 3600000) / 60000
        val seconds = (duration % 60000) / 1000
        
        tvLoggerDuration.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun exportToCsv() {
        if (loggedData.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fileName = "spacetec_log_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val file = File(getExternalFilesDir(null), fileName)
            val writer = FileWriter(file)

            // Write header
            writer.append("Timestamp")
            for (pidId in selectedPids) {
                val pidInfo = pidSelectionAdapter.getPidInfo(pidId)
                writer.append(",${pidInfo?.name ?: pidId} (${pidInfo?.unit ?: ""})")
            }
            writer.append("\n")

            // Write data
            for (dataPoint in loggedData) {
                writer.append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(dataPoint.timestamp)))
                for (pidId in selectedPids) {
                    writer.append(",${dataPoint.values[pidId] ?: ""}")
                }
                writer.append("\n")
            }

            writer.close()
            Toast.makeText(this, "Data exported to $fileName", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportToJson() {
        if (loggedData.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fileName = "spacetec_log_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
            val file = File(getExternalFilesDir(null), fileName)
            val writer = FileWriter(file)

            writer.append("{\n")
            writer.append("  \"export_info\": {\n")
            writer.append("    \"timestamp\": \"${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())}\",\n")
            writer.append("    \"data_points\": ${loggedData.size},\n")
            writer.append("    \"duration_ms\": ${loggedData.lastOrNull()?.timestamp?.minus(loggedData.firstOrNull()?.timestamp ?: 0) ?: 0}\n")
            writer.append("  },\n")
            writer.append("  \"data\": [\n")

            loggedData.forEachIndexed { index, dataPoint ->
                writer.append("    {\n")
                writer.append("      \"timestamp\": ${dataPoint.timestamp},\n")
                writer.append("      \"values\": {\n")
                
                dataPoint.values.entries.forEachIndexed { valueIndex, entry ->
                    writer.append("        \"${entry.key}\": \"${entry.value}\"")
                    if (valueIndex < dataPoint.values.size - 1) writer.append(",")
                    writer.append("\n")
                }
                
                writer.append("      }\n")
                writer.append("    }")
                if (index < loggedData.size - 1) writer.append(",")
                writer.append("\n")
            }

            writer.append("  ]\n")
            writer.append("}")
            writer.close()

            Toast.makeText(this, "Data exported to $fileName", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isLogging) {
            stopLogging()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

data class LogDataPoint(
    val timestamp: Long,
    val values: MutableMap<String, String>
)

data class PidInfo(
    val id: String,
    val name: String,
    val unit: String
)

data class CurrentDataItem(
    val name: String,
    val value: String,
    val unit: String
)

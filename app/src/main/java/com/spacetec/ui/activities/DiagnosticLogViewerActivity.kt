package com.spacetec.ui.activities

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity for viewing diagnostic logs and session data
 * This is a placeholder implementation for the diagnostic log viewer
 */
class DiagnosticLogViewerActivity : AppCompatActivity() {
    
    private lateinit var recyclerLogs: RecyclerView
    private lateinit var btnClearLogs: Button
    private lateinit var btnExportLogs: Button
    private lateinit var spinnerLogLevel: Spinner
    
    private var logEntries = mutableListOf<LogEntry>()
    private lateinit var logAdapter: LogAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create main layout programmatically
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // Add toolbar
        val toolbar = Toolbar(this).apply {
            title = "Diagnostic Logs"
            setNavigationIcon(android.R.drawable.ic_menu_revert)
        }
        mainLayout.addView(toolbar)
        
        // Add controls layout
        val controlsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
        }
        
        // Log level spinner
        val logLevelLabel = TextView(this).apply {
            text = "Level: "
            setPadding(0, 0, 8, 0)
        }
        controlsLayout.addView(logLevelLabel)
        
        spinnerLogLevel = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@DiagnosticLogViewerActivity,
                android.R.layout.simple_spinner_item,
                arrayOf("ALL", "DEBUG", "INFO", "WARN", "ERROR")
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }
        controlsLayout.addView(spinnerLogLevel)
        
        // Buttons
        btnClearLogs = Button(this).apply {
            text = "Clear"
            setPadding(8, 0, 8, 0)
        }
        controlsLayout.addView(btnClearLogs)
        
        btnExportLogs = Button(this).apply {
            text = "Export"
        }
        controlsLayout.addView(btnExportLogs)
        
        mainLayout.addView(controlsLayout)
        
        // Add RecyclerView for logs
        recyclerLogs = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
            setPadding(16, 0, 16, 16)
        }
        mainLayout.addView(recyclerLogs)
        
        setContentView(mainLayout)
        
        setupActionBar(toolbar)
        setupLogViewer()
        loadSampleLogs()
        setupEventListeners()
    }
    
    private fun setupActionBar(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }
    
    private fun setupLogViewer() {
        logAdapter = LogAdapter(logEntries)
        recyclerLogs.layoutManager = LinearLayoutManager(this)
        recyclerLogs.adapter = logAdapter
    }
    
    private fun loadSampleLogs() {
        // Generate some sample log entries for demonstration
        val currentTime = System.currentTimeMillis()
        val sampleLogs = listOf(
            LogEntry(currentTime - 5000, LogLevel.INFO, "OBD", "Connection established to ELM327"),
            LogEntry(currentTime - 4000, LogLevel.DEBUG, "OBD", "Sending command: ATZ"),
            LogEntry(currentTime - 3500, LogLevel.DEBUG, "OBD", "Response: ELM327 v1.5"),
            LogEntry(currentTime - 3000, LogLevel.INFO, "OBD", "Protocol auto-detection started"),
            LogEntry(currentTime - 2500, LogLevel.DEBUG, "OBD", "Sending command: 0100"),
            LogEntry(currentTime - 2000, LogLevel.DEBUG, "OBD", "Response: 41 00 BE 3E A8 13"),
            LogEntry(currentTime - 1500, LogLevel.INFO, "DTC", "Reading diagnostic trouble codes"),
            LogEntry(currentTime - 1000, LogLevel.WARN, "DTC", "No DTCs found"),
            LogEntry(currentTime - 500, LogLevel.INFO, "LIVE", "Started live data monitoring"),
            LogEntry(currentTime, LogLevel.DEBUG, "LIVE", "RPM: 800, Speed: 0 km/h")
        )
        
        logEntries.addAll(sampleLogs)
        logAdapter.notifyDataSetChanged()
    }
    
    private fun setupEventListeners() {
        btnClearLogs.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear Logs")
                .setMessage("Are you sure you want to clear all diagnostic logs?")
                .setPositiveButton("Clear") { _, _ ->
                    logEntries.clear()
                    logAdapter.notifyDataSetChanged()
                    Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        btnExportLogs.setOnClickListener {
            exportLogs()
        }
        
        spinnerLogLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                filterLogsByLevel(position)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun filterLogsByLevel(levelIndex: Int) {
        // For this demo, we'll just show a toast
        val levelName = spinnerLogLevel.selectedItem.toString()
        Toast.makeText(this, "Filtering by: $levelName", Toast.LENGTH_SHORT).show()
        // In a real implementation, you'd filter the logEntries and update the adapter
    }
    
    private fun exportLogs() {
        // For this demo, we'll just show the export format
        val logText = logEntries.joinToString("\n") { entry ->
            "${entry.getFormattedTime()} [${entry.level}] ${entry.tag}: ${entry.message}"
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Export Logs")
            .setMessage("Log export preview:\n\n${logText.take(500)}${if (logText.length > 500) "..." else ""}\n\nIn the full version, logs would be saved to Downloads folder.")
            .setPositiveButton("OK", null)
            .setNeutralButton("Copy to Clipboard") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Diagnostic Logs", logText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}

data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String
) {
    fun getFormattedTime(): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

class LogAdapter(private val logs: List<LogEntry>) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {
    
    class LogViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val textTime: TextView = itemView.findViewById(android.R.id.text1)
        val textMessage: TextView = itemView.findViewById(android.R.id.text2)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): LogViewHolder {
        val view = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
            
            val timeText = TextView(parent.context).apply {
                id = android.R.id.text1
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            addView(timeText)
            
            val messageText = TextView(parent.context).apply {
                id = android.R.id.text2
                textSize = 14f
                setPadding(0, 4, 0, 0)
            }
            addView(messageText)
        }
        
        return LogViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        holder.textTime.text = "${log.getFormattedTime()} [${log.level}] ${log.tag}"
        holder.textMessage.text = log.message
        
        // Color code by log level
        val color = when (log.level) {
            LogLevel.DEBUG -> android.graphics.Color.GRAY
            LogLevel.INFO -> android.graphics.Color.BLACK
            LogLevel.WARN -> android.graphics.Color.rgb(255, 140, 0) // Orange
            LogLevel.ERROR -> android.graphics.Color.RED
        }
        holder.textTime.setTextColor(color)
        holder.textMessage.setTextColor(color)
    }
    
    override fun getItemCount() = logs.size
}

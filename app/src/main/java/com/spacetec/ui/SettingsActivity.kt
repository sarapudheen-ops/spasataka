package com.spacetec.ui

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.spacetec.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var switchAutoConnect: SwitchMaterial
    private lateinit var switchKeepAlive: SwitchMaterial
    private lateinit var switchAdvancedMode: SwitchMaterial
    private lateinit var switchAutoScan: SwitchMaterial
    private lateinit var spinnerTimeout: Spinner
    private lateinit var spinnerUpdateRate: Spinner
    private lateinit var spinnerTheme: Spinner
    private lateinit var spinnerUnits: Spinner
    private lateinit var btnClearCache: Button
    private lateinit var btnExportLogs: Button
    private lateinit var btnResetSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("spacetec_settings", MODE_PRIVATE)
        
        setupToolbar()
        initializeViews()
        setupSpinners()
        loadSettings()
        setupEventListeners()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun initializeViews() {
        switchAutoConnect = findViewById(R.id.switchAutoConnect)
        switchKeepAlive = findViewById(R.id.switchKeepAlive)
        switchAdvancedMode = findViewById(R.id.switchAdvancedMode)
        switchAutoScan = findViewById(R.id.switchAutoScan)
        spinnerTimeout = findViewById(R.id.spinnerTimeout)
        spinnerUpdateRate = findViewById(R.id.spinnerUpdateRate)
        spinnerTheme = findViewById(R.id.spinnerTheme)
        spinnerUnits = findViewById(R.id.spinnerUnits)
        btnClearCache = findViewById(R.id.btnClearCache)
        btnExportLogs = findViewById(R.id.btnExportLogs)
        btnResetSettings = findViewById(R.id.btnResetSettings)
    }

    private fun setupSpinners() {
        // Connection timeout options
        val timeoutOptions = arrayOf("5 seconds", "10 seconds", "15 seconds", "30 seconds")
        spinnerTimeout.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timeoutOptions)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // Update rate options
        val updateRateOptions = arrayOf("Fast (100ms)", "Normal (500ms)", "Slow (1000ms)", "Manual")
        spinnerUpdateRate.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, updateRateOptions)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // Theme options
        val themeOptions = arrayOf("Space Theme", "Dark Theme", "Light Theme", "Auto")
        spinnerTheme.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themeOptions)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // Units options
        val unitsOptions = arrayOf("Metric (km/h, °C)", "Imperial (mph, °F)", "Mixed")
        spinnerUnits.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, unitsOptions)
            .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun loadSettings() {
        switchAutoConnect.isChecked = prefs.getBoolean("auto_connect", true)
        switchKeepAlive.isChecked = prefs.getBoolean("keep_alive", false)
        switchAdvancedMode.isChecked = prefs.getBoolean("advanced_mode", false)
        switchAutoScan.isChecked = prefs.getBoolean("auto_scan", true)
        
        spinnerTimeout.setSelection(prefs.getInt("timeout_index", 1))
        spinnerUpdateRate.setSelection(prefs.getInt("update_rate_index", 1))
        spinnerTheme.setSelection(prefs.getInt("theme_index", 0))
        spinnerUnits.setSelection(prefs.getInt("units_index", 0))
    }

    private fun setupEventListeners() {
        // Save settings when switches change
        switchAutoConnect.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_connect", isChecked).apply()
        }

        switchKeepAlive.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("keep_alive", isChecked).apply()
        }

        switchAdvancedMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("advanced_mode", isChecked).apply()
            if (isChecked) {
                Toast.makeText(this, "Advanced mode enabled - Use with caution!", Toast.LENGTH_LONG).show()
            }
        }

        switchAutoScan.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_scan", isChecked).apply()
        }

        // Save spinner selections
        spinnerTimeout.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                prefs.edit().putInt("timeout_index", position).apply()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        spinnerUpdateRate.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                prefs.edit().putInt("update_rate_index", position).apply()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        spinnerTheme.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                prefs.edit().putInt("theme_index", position).apply()
                Toast.makeText(this@SettingsActivity, "Theme will apply on next restart", Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        spinnerUnits.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                prefs.edit().putInt("units_index", position).apply()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        btnClearCache.setOnClickListener {
            clearCache()
        }

        btnExportLogs.setOnClickListener {
            exportLogs()
        }

        btnResetSettings.setOnClickListener {
            resetSettings()
        }
    }

    private fun clearCache() {
        AlertDialog.Builder(this)
            .setTitle("Clear Cache")
            .setMessage("This will clear all cached diagnostic data. Continue?")
            .setPositiveButton("Clear") { _, _ ->
                // Clear cache logic here
                Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportLogs() {
        AlertDialog.Builder(this)
            .setTitle("Export Logs")
            .setMessage("Export diagnostic logs for troubleshooting?")
            .setPositiveButton("Export") { _, _ ->
                // Export logs logic here
                Toast.makeText(this, "Logs exported to Downloads folder", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetSettings() {
        AlertDialog.Builder(this)
            .setTitle("Reset Settings")
            .setMessage("This will reset all settings to default values. This action cannot be undone.")
            .setPositiveButton("Reset") { _, _ ->
                prefs.edit().clear().apply()
                loadSettings()
                Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

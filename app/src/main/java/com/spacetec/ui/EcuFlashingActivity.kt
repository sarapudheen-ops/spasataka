package com.spacetec.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.spacetec.R
import com.spacetec.vehicle.EcuProgrammingEngine
import com.spacetec.obd.RealObdManager
import com.spacetec.vehicle.ProgrammingType
import java.io.File
import com.spacetec.diagnostic.autel.VehicleDataManager
import kotlinx.coroutines.launch
import java.io.InputStream

class EcuFlashingActivity : AppCompatActivity() {

    private lateinit var spinnerEcu: Spinner
    private lateinit var tvEcuInfo: TextView
    private lateinit var btnSelectFirmware: Button
    private lateinit var tvFirmwareInfo: TextView
    private lateinit var btnSecurityAccess: Button
    private lateinit var btnStartFlashing: Button
    private lateinit var btnAbort: Button
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var cardProgress: View

    private var selectedEcuId: String? = null
    private var firmwareData: ByteArray? = null
    private var firmwareFile: File? = null
    private var isFlashing = false
    private var hasSecurityAccess = false
    private var programmingEngine: EcuProgrammingEngine? = null

    companion object {
        private const val FIRMWARE_FILE_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ecu_flashing)

        // Initialize programming engine (in a real app, this would be injected)
        // For now, we'll create it directly
        val obdManager = RealObdManager(applicationContext)
        programmingEngine = EcuProgrammingEngine(obdManager)

        setupToolbar()
        initializeViews()
        setupEventListeners()
        loadAvailableEcus()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "ECU Flashing"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun initializeViews() {
        spinnerEcu = findViewById(R.id.spinnerEcu)
        tvEcuInfo = findViewById(R.id.tvEcuInfo)
        btnSelectFirmware = findViewById(R.id.btnSelectFirmware)
        tvFirmwareInfo = findViewById(R.id.tvFirmwareInfo)
        btnSecurityAccess = findViewById(R.id.btnSecurityAccess)
        btnStartFlashing = findViewById(R.id.btnStartFlashing)
        btnAbort = findViewById(R.id.btnAbort)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)
        cardProgress = findViewById(R.id.cardProgress)
    }

    private fun setupEventListeners() {
        spinnerEcu.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val ecuList = getEcuList()
                    selectedEcuId = ecuList[position - 1].first
                    tvEcuInfo.text = ecuList[position - 1].second
                    updateFlashingButtonState()
                } else {
                    selectedEcuId = null
                    tvEcuInfo.text = "No ECU selected"
                    updateFlashingButtonState()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnSelectFirmware.setOnClickListener {
            selectFirmwareFile()
        }

        btnSecurityAccess.setOnClickListener {
            performSecurityAccess()
        }

        btnStartFlashing.setOnClickListener {
            startFlashing()
        }

        btnAbort.setOnClickListener {
            abortFlashing()
        }
    }

    private fun loadAvailableEcus() {
        val ecuList = getEcuList()
        val ecuNames = listOf("Select ECU") + ecuList.map { it.third }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ecuNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEcu.adapter = adapter
    }

    private fun getEcuList(): List<Triple<String, String, String>> {
        return listOf(
            Triple("0x7E0", "Engine Control Module (ECM)\nProtocol: CAN-TP\nSoftware: v1.2.3", "Engine Control Module"),
            Triple("0x7E1", "Transmission Control Module (TCM)\nProtocol: CAN-TP\nSoftware: v2.1.0", "Transmission Control"),
            Triple("0x7E2", "ABS Control Module\nProtocol: CAN-TP\nSoftware: v3.0.1", "ABS Control Module"),
            Triple("0x7E3", "Body Control Module (BCM)\nProtocol: CAN-TP\nSoftware: v1.5.2", "Body Control Module")
        )
    }

    private fun selectFirmwareFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream", "*/*"))
        }
        startActivityForResult(Intent.createChooser(intent, "Select Firmware File"), FIRMWARE_FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == FIRMWARE_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                loadFirmwareFile(uri)
            }
        }
    }

    private fun loadFirmwareFile(uri: Uri) {
        lifecycleScope.launch {
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                firmwareData = inputStream?.readBytes()
                inputStream?.close()

                // Save firmware data to a temporary file
                val fileName = getFileName(uri)
                val tempFile = File.createTempFile("firmware_", ".bin", cacheDir)
                tempFile.writeBytes(firmwareData ?: byteArrayOf())
                firmwareFile = tempFile

                val fileSize = firmwareData?.size ?: 0
                
                tvFirmwareInfo.text = "File: $fileName\nSize: ${fileSize / 1024} KB"
                updateFlashingButtonState()
                updateStatus("Firmware file loaded: $fileName")
                
            } catch (e: Exception) {
                updateStatus("Error loading firmware file: ${e.message}")
                firmwareData = null
                firmwareFile = null
                tvFirmwareInfo.text = "Error loading firmware file"
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        return uri.lastPathSegment ?: "Unknown file"
    }

    private fun performSecurityAccess() {
        if (selectedEcuId == null) {
            updateStatus("Please select an ECU first")
            return
        }

        val engine = programmingEngine ?: return
        val ecuId = selectedEcuId ?: return
        
        lifecycleScope.launch {
            try {
                updateStatus("Performing security access...")
                btnSecurityAccess.isEnabled = false
                
                // Perform real security access using the programming engine
                val success = engine.performSecurityAccess(ecuId)
                
                if (success) {
                    hasSecurityAccess = true
                    updateStatus("Security access granted ✓")
                    btnSecurityAccess.text = "Security Access ✓"
                } else {
                    hasSecurityAccess = false
                    updateStatus("Security access failed")
                }
                
                updateFlashingButtonState()
                
            } catch (e: Exception) {
                updateStatus("Security access error: ${e.message}")
                hasSecurityAccess = false
            } finally {
                btnSecurityAccess.isEnabled = true
            }
        }
    }

    private fun startFlashing() {
        val ecuId = selectedEcuId
        val firmware = firmwareData
        val firmwareFile = firmwareFile
        
        if (ecuId == null || firmware == null || firmwareFile == null) {
            updateStatus("Please select ECU and firmware file")
            return
        }

        if (!hasSecurityAccess) {
            updateStatus("Security access required before flashing")
            return
        }

        val engine = programmingEngine ?: return
        
        lifecycleScope.launch {
            try {
                isFlashing = true
                updateFlashingUI(true)
                updateStatus("Starting ECU flash programming...")
                
                // Subscribe to progress updates
                val progressJob = launch {
                    engine.programmingProgress.collect { progress ->
                        progress?.let {
                            runOnUiThread { 
                                updateProgress((it.progress * 100).toInt(), it.stepDescription) 
                            }
                        }
                    }
                }
                
                // Start programming
                val result = engine.programEcu(
                    vehicleMake = "TOYOTA", // In a real app, this would come from vehicle info
                    vehicleModel = "CAMRY",
                    vehicleYear = 2020,
                    ecuId = ecuId,
                    firmwareFile = firmwareFile,
                    programmingType = ProgrammingType.FLASH_PROGRAMMING
                )
                
                progressJob.cancel()
                
                when (result) {
                    is com.spacetec.vehicle.ProgrammingResult.Success -> {
                        updateProgress(100, "ECU flashing completed successfully!")
                        updateStatus("")
                    }
                    is com.spacetec.vehicle.ProgrammingResult.Error -> {
                        updateStatus("ECU flashing failed: ${result.message}")
                    }
                }
                
            } catch (e: Exception) {
                updateStatus("ECU flashing error: ${e.message}")
            } finally {
                isFlashing = false
                updateFlashingUI(false)
            }
        }
    }

    private fun abortFlashing() {
        val engine = programmingEngine ?: return
        
        lifecycleScope.launch {
            try {
                // In a real implementation, we would abort the programming engine
                // For now, we'll just update the UI but call the engine's abort method
                engine.abortProgramming()
                updateStatus("ECU flashing aborted")
                isFlashing = false
                updateFlashingUI(false)
            } catch (e: Exception) {
                updateStatus("Error aborting: ${e.message}")
            }
        }
    }

    private fun updateFlashingButtonState() {
        val canFlash = selectedEcuId != null && firmwareData != null && hasSecurityAccess && !isFlashing
        btnStartFlashing.isEnabled = canFlash
    }

    private fun updateFlashingUI(flashing: Boolean) {
        cardProgress.visibility = if (flashing) View.VISIBLE else View.GONE
        btnAbort.visibility = if (flashing) View.VISIBLE else View.GONE
        btnStartFlashing.isEnabled = !flashing
        btnSecurityAccess.isEnabled = !flashing
        btnSelectFirmware.isEnabled = !flashing
        spinnerEcu.isEnabled = !flashing
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

package com.spacetec.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.spacetec.R
import com.spacetec.diagnostic.vehicle.VehicleBrand
import com.spacetec.diagnostic.vehicle.VehicleEcu
import com.spacetec.diagnostic.vehicle.VehicleLibrary
import com.spacetec.diagnostic.vehicle.VehicleModel
import com.spacetec.diagnostic.viewmodel.DiagnosticViewModel

class VehicleSelectionActivity : AppCompatActivity() {

    private lateinit var diagnosticViewModel: DiagnosticViewModel
    private lateinit var spinnerBrands: Spinner
    private lateinit var spinnerModels: Spinner
    private lateinit var spinnerEcus: Spinner
    private lateinit var btnLoadStandardPids: Button
    private lateinit var btnContinue: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_selection)

        supportActionBar?.title = "Vehicle Selection"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupViewModel()
        setupUI()
        observeViewModel()
        
        // Load brands on startup
        diagnosticViewModel.loadBrands()
    }

    private fun setupViewModel() {
        val vehicleLibrary = VehicleLibrary(this)
        diagnosticViewModel = ViewModelProvider(
            this,
            DiagnosticViewModelFactory(vehicleLibrary)
        )[DiagnosticViewModel::class.java]
    }

    private fun setupUI() {
        spinnerBrands = findViewById(R.id.spinnerBrands)
        spinnerModels = findViewById(R.id.spinnerModels)
        spinnerEcus = findViewById(R.id.spinnerEcus)
        btnLoadStandardPids = findViewById(R.id.btnLoadStandardPids)
        btnContinue = findViewById(R.id.btnContinue)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
        
        // Setup toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        btnLoadStandardPids.setOnClickListener {
            diagnosticViewModel.loadStandardPids()
            navigateToLiveData()
        }

        btnContinue.setOnClickListener {
            navigateToLiveData()
        }
    }

    private fun observeViewModel() {
        diagnosticViewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        diagnosticViewModel.error.observe(this) { error ->
            tvError.text = error ?: ""
            val cardError = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardError)
            cardError.visibility = if (error != null) View.VISIBLE else View.GONE
        }

        diagnosticViewModel.brands.observe(this) { brands ->
            setupBrandsSpinner(brands)
        }

        diagnosticViewModel.models.observe(this) { models ->
            setupModelsSpinner(models)
        }

        diagnosticViewModel.ecus.observe(this) { ecus ->
            setupEcusSpinner(ecus)
        }

        diagnosticViewModel.selectedEcu.observe(this) { ecu ->
            btnContinue.isEnabled = ecu != null
        }
    }

    private fun setupBrandsSpinner(brands: List<VehicleBrand>) {
        val brandNames = if (brands.isEmpty()) listOf("No brands available") 
            else listOf("Select Brand") + brands.map { it.name }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, brandNames)
        
        val autoCompleteTextView = spinnerBrands as? com.google.android.material.textfield.MaterialAutoCompleteTextView
        if (autoCompleteTextView != null) {
            autoCompleteTextView.setAdapter(adapter)
            autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
                if (position > 0 && brands.isNotEmpty()) {
                    val selectedBrand = brands.getOrNull(position - 1)
                    selectedBrand?.let {
                        autoCompleteTextView.setText(it.name, false)
                        diagnosticViewModel.selectBrand(it)
                    }
                }
            }
        }
    }

    private fun setupModelsSpinner(models: List<VehicleModel>) {
        val modelNames = if (models.isEmpty()) listOf("No models available") 
            else listOf("Select Model") + models.map { "${it.name} (${it.yearRange})" }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, modelNames)
        
        val autoCompleteTextView = spinnerModels as? com.google.android.material.textfield.MaterialAutoCompleteTextView
        if (autoCompleteTextView != null) {
            autoCompleteTextView.setAdapter(adapter)
            autoCompleteTextView.isEnabled = models.isNotEmpty()
            autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
                if (position > 0 && models.isNotEmpty()) {
                    val selectedModel = models.getOrNull(position - 1) ?: return@setOnItemClickListener
                    autoCompleteTextView.setText("${selectedModel.name} (${selectedModel.yearRange})", false)
                    diagnosticViewModel.selectModel(selectedModel)
                }
            }
        }
    }

    private fun setupEcusSpinner(ecus: List<VehicleEcu>) {
        val ecuNames = if (ecus.isEmpty()) listOf("No ECUs available") 
            else listOf("Select ECU") + ecus.map { "${it.name} (${it.protocol})" }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, ecuNames)
        
        val autoCompleteTextView = spinnerEcus as? com.google.android.material.textfield.MaterialAutoCompleteTextView
        if (autoCompleteTextView != null) {
            autoCompleteTextView.setAdapter(adapter)
            autoCompleteTextView.isEnabled = ecus.isNotEmpty()
            autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
                if (position > 0 && ecus.isNotEmpty()) {
                    val selectedEcu = ecus.getOrNull(position - 1) ?: return@setOnItemClickListener
                    autoCompleteTextView.setText("${selectedEcu.name} (${selectedEcu.protocol})", false)
                    diagnosticViewModel.selectEcu(selectedEcu)
                }
            }
        }
    }

    private fun navigateToLiveData() {
        // Navigate to the modern dashboard instead of old LiveDataActivity
        val intent = Intent(this, com.spacetec.ui.MainActivity::class.java)
        intent.putExtra("screen", "dashboard")
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

class DiagnosticViewModelFactory(
    private val vehicleLibrary: VehicleLibrary
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiagnosticViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiagnosticViewModel(vehicleLibrary) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

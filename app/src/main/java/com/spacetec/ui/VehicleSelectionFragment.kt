package com.spacetec.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.spacetec.R
import kotlinx.coroutines.launch

class VehicleSelectionFragment : Fragment() {
    
    private val viewModel: VehicleSelectionViewModel by viewModels {
        VehicleSelectionViewModelFactory(requireContext())
    }
    
    private lateinit var makeAutoComplete: AutoCompleteTextView
    private lateinit var modelAutoComplete: AutoCompleteTextView
    private lateinit var selectedVehicleText: TextView
    private lateinit var clearButton: Button
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return createVehicleSelectionView()
    }
    
    private fun createVehicleSelectionView(): View {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Title
        val title = TextView(context).apply {
            text = "Select Vehicle"
            textSize = 20f
            setPadding(0, 0, 0, 24)
        }
        layout.addView(title)
        
        // Make selection
        val makeLabel = TextView(context).apply {
            text = "Vehicle Make:"
            setPadding(0, 0, 0, 8)
        }
        layout.addView(makeLabel)
        
        makeAutoComplete = AutoCompleteTextView(context).apply {
            hint = "Select or type vehicle make..."
            setPadding(16, 16, 16, 16)
            setOnItemClickListener { _, _, position, _ ->
                val selectedMake = (adapter as ArrayAdapter<String>).getItem(position)
                selectedMake?.let { viewModel.selectMake(it) }
            }
        }
        layout.addView(makeAutoComplete)
        
        // Model selection
        val modelLabel = TextView(context).apply {
            text = "Vehicle Model:"
            setPadding(0, 16, 0, 8)
        }
        layout.addView(modelLabel)
        
        modelAutoComplete = AutoCompleteTextView(context).apply {
            hint = "Select vehicle model..."
            setPadding(16, 16, 16, 16)
            isEnabled = false
            setOnItemClickListener { _, _, position, _ ->
                val selectedModel = (adapter as ArrayAdapter<String>).getItem(position)
                selectedModel?.let { viewModel.selectModel(it) }
            }
        }
        layout.addView(modelAutoComplete)
        
        // Selected vehicle display
        selectedVehicleText = TextView(context).apply {
            text = "No vehicle selected"
            setPadding(0, 24, 0, 16)
            textSize = 16f
        }
        layout.addView(selectedVehicleText)
        
        // Clear button
        clearButton = Button(context).apply {
            text = "Clear Selection"
            setOnClickListener { viewModel.clearSelection() }
        }
        layout.addView(clearButton)
        
        return layout
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.vehicleMakes.collect { makes ->
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, makes)
                makeAutoComplete.setAdapter(adapter)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.vehicleModels.collect { models ->
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, models)
                modelAutoComplete.setAdapter(adapter)
                modelAutoComplete.isEnabled = models.isNotEmpty()
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedMake.collect { make ->
                if (make != null) {
                    makeAutoComplete.setText(make, false)
                } else {
                    makeAutoComplete.text.clear()
                }
                updateSelectedVehicleDisplay()
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedModel.collect { model ->
                if (model != null) {
                    modelAutoComplete.setText(model, false)
                } else {
                    modelAutoComplete.text.clear()
                }
                updateSelectedVehicleDisplay()
            }
        }
    }
    
    private fun updateSelectedVehicleDisplay() {
        val make = viewModel.selectedMake.value
        val model = viewModel.selectedModel.value
        
        selectedVehicleText.text = when {
            make != null && model != null -> "Selected: $make $model"
            make != null -> "Selected: $make (choose model)"
            else -> "No vehicle selected"
        }
    }
}

package com.spacetec.ui.components

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.spacetec.repository.BrandsRepository

/**
 * Helper class for setting up brand selection spinner with the new brands data
 */
class BrandSelectionSpinner(
    private val context: Context,
    private val spinner: Spinner,
    private val brandsRepository: BrandsRepository = BrandsRepository()
) {
    
    private var onBrandSelectedListener: ((String) -> Unit)? = null
    
    /**
     * Initialize the spinner with all available brands
     */
    fun setupSpinner() {
        val brands = listOf("Select Brand") + brandsRepository.getBrandsSync()
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, brands)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val selectedBrand = brands[position]
                    onBrandSelectedListener?.invoke(selectedBrand)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    /**
     * Set listener for brand selection
     */
    fun setOnBrandSelectedListener(listener: (String) -> Unit) {
        onBrandSelectedListener = listener
    }
    
    /**
     * Get currently selected brand
     */
    fun getSelectedBrand(): String? {
        val position = spinner.selectedItemPosition
        return if (position > 0) {
            val brands = brandsRepository.getBrandsSync()
            brands[position - 1]
        } else null
    }
    
    /**
     * Set selected brand programmatically
     */
    fun setSelectedBrand(brand: String) {
        val brands = brandsRepository.getBrandsSync()
        val position = brands.indexOf(brand) + 1 // +1 for "Select Brand" item
        if (position > 0) {
            spinner.setSelection(position)
        }
    }
}

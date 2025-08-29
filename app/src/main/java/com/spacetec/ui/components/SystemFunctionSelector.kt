package com.spacetec.ui.components

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.spacetec.repository.DiagnosticFunctionsRepository

/**
 * Component for selecting ECU systems and their diagnostic functions
 */
class SystemFunctionSelector(
    private val context: Context,
    private val containerView: ViewGroup,
    private val repository: DiagnosticFunctionsRepository = DiagnosticFunctionsRepository()
) {
    
    private var systemSpinner: Spinner? = null
    private var functionsRecyclerView: RecyclerView? = null
    private var functionsAdapter: FunctionsAdapter? = null
    
    private var onFunctionSelectedListener: ((system: String, function: String) -> Unit)? = null
    
    /**
     * Initialize the system and function selectors
     */
    fun setupSelectors() {
        createSystemSpinner()
        createFunctionsRecyclerView()
    }
    
    private fun createSystemSpinner() {
        systemSpinner = Spinner(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        val systems = listOf("Select ECU System") + repository.getAllSystemFunctionsSync().keys.toList()
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, systems)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        systemSpinner?.adapter = adapter
        
        systemSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val selectedSystem = systems[position]
                    loadFunctionsForSystem(selectedSystem)
                } else {
                    clearFunctions()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        containerView.addView(systemSpinner)
    }
    
    private fun createFunctionsRecyclerView() {
        functionsRecyclerView = RecyclerView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutManager = LinearLayoutManager(context)
        }
        
        functionsAdapter = FunctionsAdapter { system, function ->
            onFunctionSelectedListener?.invoke(system, function)
        }
        
        functionsRecyclerView?.adapter = functionsAdapter
        containerView.addView(functionsRecyclerView)
    }
    
    private fun loadFunctionsForSystem(system: String) {
        val functions = repository.getFunctionsForSystemSync(system)
        functionsAdapter?.updateFunctions(system, functions)
    }
    
    private fun clearFunctions() {
        functionsAdapter?.clearFunctions()
    }
    
    /**
     * Set listener for function selection
     */
    fun setOnFunctionSelectedListener(listener: (system: String, function: String) -> Unit) {
        onFunctionSelectedListener = listener
    }
    
    /**
     * Get currently selected system
     */
    fun getSelectedSystem(): String? {
        val position = systemSpinner?.selectedItemPosition ?: 0
        return if (position > 0) {
            val systems = repository.getAllSystemFunctionsSync().keys.toList()
            systems[position - 1]
        } else null
    }
}

/**
 * RecyclerView adapter for displaying diagnostic functions
 */
class FunctionsAdapter(
    private val onFunctionClick: (system: String, function: String) -> Unit
) : RecyclerView.Adapter<FunctionsAdapter.FunctionViewHolder>() {
    
    private var currentSystem: String = ""
    private var functions: List<String> = emptyList()
    
    fun updateFunctions(system: String, newFunctions: List<String>) {
        currentSystem = system
        functions = newFunctions
        notifyDataSetChanged()
    }
    
    fun clearFunctions() {
        currentSystem = ""
        functions = emptyList()
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FunctionViewHolder {
        val button = Button(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }
        return FunctionViewHolder(button)
    }
    
    override fun onBindViewHolder(holder: FunctionViewHolder, position: Int) {
        val function = functions[position]
        holder.button.text = function
        holder.button.setOnClickListener {
            onFunctionClick(currentSystem, function)
        }
    }
    
    override fun getItemCount(): Int = functions.size
    
    class FunctionViewHolder(val button: Button) : RecyclerView.ViewHolder(button)
}

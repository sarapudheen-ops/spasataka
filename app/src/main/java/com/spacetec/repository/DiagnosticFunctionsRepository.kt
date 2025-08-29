package com.spacetec.repository

import com.spacetec.data.EcuFunctions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository for managing diagnostic functions for different ECU systems
 */
class DiagnosticFunctionsRepository {
    
    /**
     * Get all ECU systems as a Flow
     */
    fun getAllSystems(): Flow<List<String>> = flow {
        emit(EcuFunctions.getAllSystems())
    }
    
    /**
     * Get functions for a specific system as a Flow
     */
    fun getFunctionsForSystem(system: String): Flow<List<String>> = flow {
        emit(EcuFunctions.getFunctionsForSystem(system))
    }
    
    /**
     * Get all system functions synchronously
     */
    fun getAllSystemFunctionsSync(): Map<String, List<String>> {
        return EcuFunctions.getAllSystemFunctions()
    }
    
    /**
     * Get functions for a system synchronously
     */
    fun getFunctionsForSystemSync(system: String): List<String> {
        return EcuFunctions.getFunctionsForSystem(system)
    }
    
    /**
     * Search functions across all systems
     */
    fun searchFunctions(query: String): Flow<Map<String, List<String>>> = flow {
        emit(EcuFunctions.searchFunctions(query))
    }
    
    /**
     * Validate if a function is available for a system
     */
    fun validateFunction(system: String, function: String): Boolean {
        return EcuFunctions.isFunctionAvailable(system, function)
    }
    
    /**
     * Get common functions across systems
     */
    fun getCommonFunctions(): Flow<List<String>> = flow {
        emit(EcuFunctions.getCommonFunctions())
    }
    
    /**
     * Get systems that support a specific function
     */
    fun getSystemsForFunction(function: String): List<String> {
        return EcuFunctions.getAllSystemFunctions()
            .filter { (_, functions) -> functions.contains(function) }
            .keys.toList()
    }
    
    /**
     * Get function categories (grouped by type)
     */
    fun getFunctionCategories(): Map<String, List<String>> {
        val categories = mutableMapOf<String, MutableList<String>>()
        
        EcuFunctions.getAllSystemFunctions().values.flatten().distinct().forEach { function ->
            val category = when {
                function.contains("DTC", ignoreCase = true) -> "Diagnostic Trouble Codes"
                function.contains("Live Data", ignoreCase = true) -> "Live Data"
                function.contains("Actuation", ignoreCase = true) -> "Actuation Tests"
                function.contains("Adaptation", ignoreCase = true) -> "Adaptations"
                function.contains("Coding", ignoreCase = true) -> "Coding"
                function.contains("Programming", ignoreCase = true) || function.contains("Flash", ignoreCase = true) -> "Programming"
                function.contains("Reset", ignoreCase = true) -> "Reset Functions"
                function.contains("Learning", ignoreCase = true) -> "Learning Functions"
                else -> "Other Functions"
            }
            categories.getOrPut(category) { mutableListOf() }.add(function)
        }
        
        return categories
    }
}

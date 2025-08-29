package com.spacetec.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class VehicleRepository(private val context: Context) {
    
    private var vehicleDatabase: Map<String, List<String>>? = null
    
    suspend fun loadVehicleData(): Map<String, List<String>> {
        return withContext(Dispatchers.IO) {
            if (vehicleDatabase == null) {
                try {
                    val json = context.assets.open("vehicles.json").bufferedReader().use { it.readText() }
                    val type = object : TypeToken<Map<String, List<String>>>() {}.type
                    vehicleDatabase = Gson().fromJson(json, type)
                } catch (e: IOException) {
                    // Fallback to empty map if file not found
                    vehicleDatabase = emptyMap()
                }
            }
            vehicleDatabase ?: emptyMap()
        }
    }
    
    suspend fun getVehicleMakes(): List<String> {
        val data = loadVehicleData()
        return data.keys.sorted()
    }
    
    suspend fun getModelsForMake(make: String): List<String> {
        val data = loadVehicleData()
        return data[make] ?: emptyList()
    }
    
    suspend fun searchVehicles(query: String): Map<String, List<String>> {
        val data = loadVehicleData()
        val filteredData = mutableMapOf<String, List<String>>()
        
        data.forEach { (make, models) ->
            val filteredModels = models.filter { model ->
                make.contains(query, ignoreCase = true) || 
                model.contains(query, ignoreCase = true)
            }
            if (filteredModels.isNotEmpty() || make.contains(query, ignoreCase = true)) {
                filteredData[make] = if (make.contains(query, ignoreCase = true)) models else filteredModels
            }
        }
        
        return filteredData
    }
}

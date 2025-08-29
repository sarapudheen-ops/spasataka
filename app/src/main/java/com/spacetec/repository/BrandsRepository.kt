package com.spacetec.repository

import com.spacetec.data.VehicleBrands
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository for managing vehicle brands data
 */
class BrandsRepository {
    
    /**
     * Get all available vehicle brands as a Flow
     */
    fun getAllBrands(): Flow<List<String>> = flow {
        emit(VehicleBrands.getAllBrands())
    }
    
    /**
     * Search brands by query
     */
    fun searchBrands(query: String): Flow<List<String>> = flow {
        emit(VehicleBrands.searchBrands(query))
    }
    
    /**
     * Get brands synchronously
     */
    fun getBrandsSync(): List<String> {
        return VehicleBrands.getAllBrands()
    }
    
    /**
     * Validate if a brand is supported
     */
    fun validateBrand(brand: String): Boolean {
        return VehicleBrands.isBrandSupported(brand)
    }
    
    /**
     * Get brands grouped by first letter for better UI organization
     */
    fun getBrandsGrouped(): Map<Char, List<String>> {
        return VehicleBrands.getAllBrands().groupBy { it.first().uppercaseChar() }
    }
}

package com.spacetec.vehicle

import android.util.LruCache
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.Log

/**
 * Cached vehicle library for improved performance
 */
class CachedVehicleLibrary(
    private val database: VehicleDatabase
) {
    private val brandCache = LruCache<String, List<Brand>>(50)
    private val modelCache = LruCache<Long, List<Model>>(100)
    private val vehicleProfileCache = LruCache<String, VehicleProfile>(200)
    private val pidSupportCache = LruCache<String, List<String>>(100)
    
    private val cacheMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "CachedVehicleLibrary"
        private const val CACHE_EXPIRY_MS = 30 * 60 * 1000L // 30 minutes
    }
    
    // Cache metadata for expiry tracking
    private val cacheTimestamps = mutableMapOf<String, Long>()
    
    /**
     * Get brands with intelligent caching
     */
    suspend fun getBrandsWithCache(): List<Brand> {
        return cacheMutex.withLock {
            val cacheKey = "brands"
            val cached = brandCache.get(cacheKey)
            val timestamp = cacheTimestamps[cacheKey] ?: 0
            
            if (cached != null && !isCacheExpired(timestamp)) {
                Log.d(TAG, "Returning cached brands (${cached.size} items)")
                cached
            } else {
                Log.d(TAG, "Loading brands from database")
                val brands = database.getBrands()
                brandCache.put(cacheKey, brands)
                cacheTimestamps[cacheKey] = System.currentTimeMillis()
                brands
            }
        }
    }
    
    /**
     * Get models for a brand with caching
     */
    suspend fun getModelsWithCache(brandId: Long): List<Model> {
        return cacheMutex.withLock {
            val cached = modelCache.get(brandId)
            val timestamp = cacheTimestamps["models_$brandId"] ?: 0
            
            if (cached != null && !isCacheExpired(timestamp)) {
                Log.d(TAG, "Returning cached models for brand $brandId (${cached.size} items)")
                cached
            } else {
                Log.d(TAG, "Loading models for brand $brandId from database")
                val models = database.getModelsByBrand(brandId)
                modelCache.put(brandId, models)
                cacheTimestamps["models_$brandId"] = System.currentTimeMillis()
                models
            }
        }
    }
    
    /**
     * Get vehicle profile with caching
     */
    suspend fun getVehicleProfileWithCache(vin: String): VehicleProfile? {
        return cacheMutex.withLock {
            val cached = vehicleProfileCache.get(vin)
            val timestamp = cacheTimestamps["profile_$vin"] ?: 0
            
            if (cached != null && !isCacheExpired(timestamp)) {
                Log.d(TAG, "Returning cached vehicle profile for VIN $vin")
                cached
            } else {
                Log.d(TAG, "Loading vehicle profile for VIN $vin from database")
                val profile = database.getVehicleProfile(vin)
                profile?.let {
                    vehicleProfileCache.put(vin, it)
                    cacheTimestamps["profile_$vin"] = System.currentTimeMillis()
                }
                profile
            }
        }
    }
    
    /**
     * Get supported PIDs for a vehicle with caching
     */
    suspend fun getSupportedPidsWithCache(vehicleId: String): List<String> {
        return cacheMutex.withLock {
            val cached = pidSupportCache.get(vehicleId)
            val timestamp = cacheTimestamps["pids_$vehicleId"] ?: 0
            
            if (cached != null && !isCacheExpired(timestamp)) {
                Log.d(TAG, "Returning cached PIDs for vehicle $vehicleId (${cached.size} items)")
                cached
            } else {
                Log.d(TAG, "Loading supported PIDs for vehicle $vehicleId from database")
                val pids = database.getSupportedPids(vehicleId)
                pidSupportCache.put(vehicleId, pids)
                cacheTimestamps["pids_$vehicleId"] = System.currentTimeMillis()
                pids
            }
        }
    }
    
    /**
     * Search vehicles with intelligent caching and filtering
     */
    suspend fun searchVehiclesWithCache(
        query: String,
        brandId: Long? = null,
        year: Int? = null
    ): List<VehicleSearchResult> {
        return withContext(Dispatchers.IO) {
            val cacheKey = "search_${query}_${brandId}_$year"
            
            // For search results, use shorter cache time (5 minutes)
            val searchCache = LruCache<String, List<VehicleSearchResult>>(50)
            val cached = searchCache.get(cacheKey)
            val timestamp = cacheTimestamps[cacheKey] ?: 0
            
            if (cached != null && !isCacheExpired(timestamp, 5 * 60 * 1000L)) {
                Log.d(TAG, "Returning cached search results for '$query' (${cached.size} items)")
                cached
            } else {
                Log.d(TAG, "Performing database search for '$query'")
                val results = database.searchVehicles(query, brandId, year)
                searchCache.put(cacheKey, results)
                cacheTimestamps[cacheKey] = System.currentTimeMillis()
                results
            }
        }
    }
    
    /**
     * Preload common data into cache
     */
    suspend fun preloadCache() {
        scope.launch {
            try {
                Log.i(TAG, "Preloading cache with common data")
                
                // Preload popular brands
                getBrandsWithCache()
                
                // Preload models for top 5 brands
                val brands = getBrandsWithCache()
                brands.take(5).forEach { brand ->
                    getModelsWithCache(brand.id)
                }
                
                Log.i(TAG, "Cache preloading completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error preloading cache: ${e.message}")
            }
        }
    }
    
    /**
     * Clear expired cache entries
     */
    suspend fun clearExpiredCache() {
        cacheMutex.withLock {
            val currentTime = System.currentTimeMillis()
            val expiredKeys = cacheTimestamps.filter { (_, timestamp) ->
                currentTime - timestamp > CACHE_EXPIRY_MS
            }.keys
            
            expiredKeys.forEach { key ->
                when {
                    key == "brands" -> brandCache.evictAll()
                    key.startsWith("models_") -> {
                        val brandId = key.substringAfter("models_").toLongOrNull()
                        brandId?.let { modelCache.remove(it) }
                    }
                    key.startsWith("profile_") -> {
                        val vin = key.substringAfter("profile_")
                        vehicleProfileCache.remove(vin)
                    }
                    key.startsWith("pids_") -> {
                        val vehicleId = key.substringAfter("pids_")
                        pidSupportCache.remove(vehicleId)
                    }
                }
                cacheTimestamps.remove(key)
            }
            
            if (expiredKeys.isNotEmpty()) {
                Log.i(TAG, "Cleared ${expiredKeys.size} expired cache entries")
            }
        }
    }
    
    /**
     * Clear all cache
     */
    suspend fun clearAllCache() {
        cacheMutex.withLock {
            brandCache.evictAll()
            modelCache.evictAll()
            vehicleProfileCache.evictAll()
            pidSupportCache.evictAll()
            cacheTimestamps.clear()
            Log.i(TAG, "All cache cleared")
        }
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            brandCacheSize = brandCache.size(),
            modelCacheSize = modelCache.size(),
            profileCacheSize = vehicleProfileCache.size(),
            pidCacheSize = pidSupportCache.size(),
            totalEntries = cacheTimestamps.size
        )
    }
    
    /**
     * Start periodic cache cleanup
     */
    fun startPeriodicCleanup() {
        scope.launch {
            while (isActive) {
                try {
                    delay(15 * 60 * 1000L) // Every 15 minutes
                    clearExpiredCache()
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic cleanup: ${e.message}")
                }
            }
        }
    }
    
    private fun isCacheExpired(timestamp: Long, expiryMs: Long = CACHE_EXPIRY_MS): Boolean {
        return System.currentTimeMillis() - timestamp > expiryMs
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        runBlocking { clearAllCache() }
        Log.i(TAG, "CachedVehicleLibrary cleaned up")
    }
}

/**
 * Vehicle database interface
 */
interface VehicleDatabase {
    suspend fun getBrands(): List<Brand>
    suspend fun getModelsByBrand(brandId: Long): List<Model>
    suspend fun getVehicleProfile(vin: String): VehicleProfile?
    suspend fun getSupportedPids(vehicleId: String): List<String>
    suspend fun searchVehicles(query: String, brandId: Long?, year: Int?): List<VehicleSearchResult>
}

/**
 * Data classes
 */
data class Brand(
    val id: Long,
    val name: String,
    val country: String,
    val logoUrl: String? = null
)

data class Model(
    val id: Long,
    val brandId: Long,
    val name: String,
    val yearStart: Int,
    val yearEnd: Int?,
    val engineTypes: List<String>
)

data class VehicleProfile(
    val id: String,
    val vin: String,
    val brandId: Long,
    val modelId: Long,
    val year: Int,
    val engine: String,
    val transmission: String,
    val supportedPids: List<String>,
    val obdProtocol: String
)

data class VehicleSearchResult(
    val id: String,
    val brandName: String,
    val modelName: String,
    val year: Int,
    val engine: String,
    val matchScore: Float
)

data class CacheStats(
    val brandCacheSize: Int,
    val modelCacheSize: Int,
    val profileCacheSize: Int,
    val pidCacheSize: Int,
    val totalEntries: Int
)

package com.spacetec.diagnostic.vehicle

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

data class Brand(val id: Long, val name: String)
data class Model(val id: Long, val brandId: Long, val name: String, val yearFrom: Int?, val yearTo: Int?)
data class Ecu(val id: Long, val modelId: Long, val name: String, val protocol: String?)
data class Pid(val id: Long, val ecuId: Long, val pid: String, val name: String, val unit: String?, val formula: String?)

// Legacy data classes for backward compatibility
data class VehicleBrand(val id: Int, val name: String) {
    constructor(brand: Brand) : this(brand.id.toInt(), brand.name)
}
data class VehicleModel(val id: Int, val brandId: Int, val name: String, val yearRange: String) {
    constructor(model: Model) : this(
        model.id.toInt(), 
        model.brandId.toInt(), 
        model.name, 
        when {
            model.yearFrom != null && model.yearTo != null -> "${model.yearFrom}-${model.yearTo}"
            model.yearFrom != null -> "${model.yearFrom}+"
            else -> "Unknown"
        }
    )
}
data class VehicleEcu(val id: Int, val modelId: Int, val name: String, val protocol: String) {
    constructor(ecu: Ecu) : this(ecu.id.toInt(), ecu.modelId.toInt(), ecu.name, ecu.protocol ?: "Unknown")
}
data class VehiclePid(val pid: String, val name: String, val unit: String, val formula: String) {
    constructor(pidData: Pid) : this(pidData.pid, pidData.name, pidData.unit ?: "", pidData.formula ?: "")
}

class VehicleLibrary(private val context: Context) {

    private val log = "VehicleLibrary"
    private var db: SQLiteDatabase? = null
    private var tables: Set<String> = emptySet()

    // Column heuristics (to support unknown schemas)
    private data class Cols(
        val id: String, val name: String,
        val brandId: String? = null, val modelId: String? = null, val ecuId: String? = null,
        val yearFrom: String? = null, val yearTo: String? = null,
        val proto: String? = null, val pid: String? = null, val unit: String? = null, val formula: String? = null
    )

    private fun tryMatch(cols: List<String>, wanted: List<String>): String? =
        wanted.firstOrNull { w -> cols.any { it.equals(w, ignoreCase = true) } }

    private fun pick(colnames: List<String>, choices: List<String>) =
        choices.firstOrNull { c -> colnames.any { it.equals(c, ignoreCase = true) } }

    private fun detectCols(table: String): Cols {
        val cur = rawQuery("PRAGMA table_info($table)") ?: return Cols("id", "name")
        val names = mutableListOf<String>()
        cur.use { while (it.moveToNext()) names.add(it.getString(1)) }

        fun oneOf(vararg v: String) = pick(names, v.toList())
        return Cols(
            id = oneOf("id","_id","pk","brand_id","model_id","ecu_id") ?: "id",
            name = oneOf("name","label","title","display_name") ?: "name",
            brandId = oneOf("brand_id","brand","make_id"),
            modelId = oneOf("model_id","vehicle_model_id","veh_model_id"),
            ecuId = oneOf("ecu_id","ctrl_id","module_id"),
            yearFrom = oneOf("year_from","year_start","from_year","start_year"),
            yearTo = oneOf("year_to","year_end","to_year","end_year"),
            proto = oneOf("protocol","proto","transport"),
            pid = oneOf("pid","service_pid","pid_code","code"),
            unit = oneOf("unit","units","uom"),
            formula = oneOf("formula","expr","calc","expression")
        )
    }

    private suspend fun ensureDb(): Boolean = withContext(Dispatchers.IO) {
        if (db != null) return@withContext true

        // 1) find candidate DB in assets
        val candidates = context.assets.list("")?.filter { it.endsWith(".db", true) || it.endsWith(".sqlite", true) }.orEmpty()
        val dbAsset = candidates.firstOrNull()
        if (dbAsset != null) {
            val out = File(context.noBackupFilesDir, dbAsset)
            context.assets.open(dbAsset).use { it.copyTo(out.outputStream()) }
            db = SQLiteDatabase.openDatabase(out.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            tables = readTables()
            Log.i(log, "Opened assets DB=$dbAsset tables=$tables")
            return@withContext true
        }

        // 2) JSON fallback (assets/vehicle_library.json)
        if (context.assets.list("")?.contains("vehicle_library.json") == true) {
            Log.i(log, "JSON library present; using JSON-only mode")
            return@withContext true
        }

        Log.w(log, "No DB/JSON vehicle library found in assets.")
        false
    }

    private fun readTables(): Set<String> {
        val set = mutableSetOf<String>()
        rawQuery("SELECT name FROM sqlite_master WHERE type='table'")?.use { c ->
            while (c.moveToNext()) set.add(c.getString(0))
        }
        return set
    }

    private fun rawQuery(sql: String, args: Array<String>? = null): Cursor? =
        try { db?.rawQuery(sql, args) } catch (t: Throwable) { Log.w(log, "Query fail: $sql", t); null }

    // Heuristically pick table names
    private fun tableLike(vararg choices: String) =
        choices.firstOrNull { alt ->
            tables.any { it.equals(alt, ignoreCase = true) || it.lowercase(Locale.US).contains(alt.lowercase(Locale.US)) }
        }

    suspend fun listBrands(): List<Brand> = withContext(Dispatchers.IO) {
        if (!ensureDb()) return@withContext fromJsonBrands()

        val t = tableLike("brands","brand","make","makes") ?: return@withContext fromJsonBrands()
        val cols = detectCols(t)

        val sql = "SELECT ${cols.id}, ${cols.name} FROM $t"
        val out = mutableListOf<Brand>()
        rawQuery(sql)?.use { c ->
            while (c.moveToNext()) out.add(Brand(c.getLong(0), c.getString(1)))
        }
        out
    }

    suspend fun listModels(brandId: Long): List<Model> = withContext(Dispatchers.IO) {
        if (!ensureDb()) return@withContext fromJsonModels(brandId)

        val t = tableLike("models","vehicle_models","vehicles") ?: return@withContext emptyList()
        val cols = detectCols(t)

        // brand_id might be null in some libs → filter later
        val sql = "SELECT ${cols.id}, ${cols.name}, ${cols.brandId ?: "NULL"}, ${cols.yearFrom ?: "NULL"}, ${cols.yearTo ?: "NULL"} FROM $t"
        val out = mutableListOf<Model>()
        rawQuery(sql)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val name = c.getString(1)
                val bId = try { c.getLong(2) } catch (_: Throwable) { -1 }
                val yFrom = c.getString(3)?.toIntOrNull()
                val yTo = c.getString(4)?.toIntOrNull()
                if (bId == brandId || bId == -1L) {
                    out.add(Model(id, brandId, name, yFrom, yTo))
                }
            }
        }
        out
    }

    suspend fun listEcus(modelId: Long): List<Ecu> = withContext(Dispatchers.IO) {
        if (!ensureDb()) return@withContext emptyList()

        val t = tableLike("ecus","modules","controllers") ?: return@withContext emptyList()
        val cols = detectCols(t)

        val sql = "SELECT ${cols.id}, ${cols.name}, ${cols.modelId ?: "NULL"}, ${cols.proto ?: "NULL"} FROM $t"
        val out = mutableListOf<Ecu>()
        rawQuery(sql)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val name = c.getString(1)
                val mId = try { c.getLong(2) } catch (_: Throwable) { -1 }
                val proto = c.getString(3)
                if (mId == modelId || mId == -1L) out.add(Ecu(id, modelId, name, proto))
            }
        }
        out
    }

    suspend fun listPids(ecuId: Long): List<Pid> = withContext(Dispatchers.IO) {
        if (!ensureDb()) return@withContext emptyList()

        val t = tableLike("pids","live_data","parameters") ?: return@withContext emptyList()
        val cols = detectCols(t)

        val sql = "SELECT ${cols.id}, ${cols.pid ?: cols.name}, ${cols.name}, ${cols.unit ?: "NULL"}, ${cols.formula ?: "NULL"}, ${cols.ecuId ?: "NULL"} FROM $t"
        val out = mutableListOf<Pid>()
        rawQuery(sql)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val pidCode = c.getString(1)
                val name = c.getString(2)
                val unit = c.getString(3)
                val formula = c.getString(4)
                val eId = try { c.getLong(5) } catch (_: Throwable) { -1 }
                if (eId == ecuId || eId == -1L) out.add(Pid(id, eId, pidCode, name, unit, formula))
            }
        }
        out
    }

    // ---------- JSON fallback (individual brand files) ----------
    private fun fromJsonBrands(): List<Brand> {
        return try {
            val jsonBrands = mutableListOf<Brand>()
            
            // Load from JSON files - comprehensive vehicle library
            val brandFiles = context.assets.list("")?.filter { 
                it.endsWith(".json") && it != "vehicles.json" 
            }?.sorted() ?: emptyList()
            
            Log.i(log, "Loading ${brandFiles.size} brand files: ${brandFiles.joinToString()}")
            
            brandFiles.forEachIndexed { index, fileName ->
                try {
                    val txt = context.assets.open(fileName).use { it.readBytes().decodeToString() }
                    val root = JSONObject(txt)
                    val brandName = root.optString("brand", fileName.removeSuffix(".json").replace("-", " "))
                    jsonBrands.add(Brand((index + 1).toLong(), brandName))
                    Log.d(log, "Loaded brand: $brandName from $fileName")
                } catch (e: Exception) {
                    Log.w(log, "Failed to load brand from $fileName: ${e.message}")
                }
            }
            
            // Add brands from ExtendedVehicleDatabase if available (via reflection to avoid hard dependency)
            val extendedBrands: List<Brand> = try {
                val cls = Class.forName("com.spacetec.vehicle.ExtendedVehicleDatabase")
                val method = cls.getMethod("getAllVehicleProfiles")
                val profiles = method.invoke(null) as? List<*>
                val makes = profiles?.mapNotNull { p ->
                    try {
                        val make = p?.javaClass?.getMethod("getMake")?.invoke(p) as? String
                        make
                    } catch (_: Throwable) { null }
                }?.distinct().orEmpty()
                makes.filter { extBrand -> jsonBrands.none { it.name.equals(extBrand, ignoreCase = true) } }
                    .mapIndexed { index, brandName ->
                        Brand((jsonBrands.size + index + 1).toLong(), brandName)
                    }
            } catch (e: Exception) {
                Log.w(log, "ExtendedVehicleDatabase not available: ${e.message}")
                emptyList()
            }

            val combined = mutableListOf<Brand>().apply { addAll(jsonBrands); addAll(extendedBrands) }
            val allBrands = combined.distinctBy { it.name.lowercase(Locale.US) }
            Log.i(log, "Total brands loaded: ${allBrands.size}")
            allBrands
        } catch (e: Throwable) { 
            Log.e(log, "Failed to load brands: ${e.message}")
            emptyList() 
        }
    }

    private fun fromJsonModels(brandId: Long): List<Model> {
        return try {
            val jsonModels = mutableListOf<Model>()
            val extendedModels = mutableListOf<Model>()
            
            // Get brand name first
            val allBrands = fromJsonBrands()
            val brand = allBrands.find { it.id == brandId }
            val brandName = brand?.name ?: return emptyList()
            
            // Load from JSON files - find the correct brand file
            val brandFiles = context.assets.list("")?.filter { 
                it.endsWith(".json") && it != "vehicles.json" 
            }?.sorted() ?: emptyList()
            
            // Find the brand file by matching brand name
            val brandFile = brandFiles.find { fileName ->
                val fileBaseName = fileName.removeSuffix(".json").replace("-", " ")
                fileBaseName.equals(brandName, ignoreCase = true)
            }
            
            if (brandFile != null) {
                try {
                    val txt = context.assets.open(brandFile).use { it.readBytes().decodeToString() }
                    val root = JSONObject(txt)
                    val modelsArray = root.optJSONArray("models")
                    
                    if (modelsArray != null) {
                        for (i in 0 until modelsArray.length()) {
                            val modelObj = modelsArray.getJSONObject(i)
                            val modelName = modelObj.getString("model")
                            val yearsArray = modelObj.optJSONArray("years")
                            
                            var yearFrom: Int? = null
                            var yearTo: Int? = null
                            
                            if (yearsArray != null && yearsArray.length() > 0) {
                                yearFrom = yearsArray.getInt(0)
                                yearTo = yearsArray.getInt(yearsArray.length() - 1)
                            }
                            
                            jsonModels.add(Model((i + 1).toLong(), brandId, modelName, yearFrom, yearTo))
                        }
                    }
                    Log.d(log, "Loaded ${jsonModels.size} models for $brandName from $brandFile")
                } catch (e: Exception) {
                    Log.w(log, "Failed to load JSON models for $brandFile: ${e.message}")
                }
            } else {
                Log.w(log, "No JSON file found for brand: $brandName")
            }
            
            // Load from ExtendedVehicleDatabase if available (via reflection)
            try {
                val cls = Class.forName("com.spacetec.vehicle.ExtendedVehicleDatabase")
                val method = cls.getMethod("getAllVehicleProfiles")
                val profiles = method.invoke(null) as? List<*>
                val matching = profiles?.filter { p ->
                    try {
                        val make = p?.javaClass?.getMethod("getMake")?.invoke(p) as? String
                        make?.equals(brandName, ignoreCase = true) == true
                    } catch (_: Throwable) { false }
                }.orEmpty()

                matching.forEachIndexed { index, profile ->
                    try {
                        val modelName = profile?.javaClass?.getMethod("getModel")?.invoke(profile) as? String ?: return@forEachIndexed
                        val yr = profile.javaClass.getMethod("getYearRange").invoke(profile) as? String ?: ""
                        val parts = yr.split("-")
                        val yFrom = parts.getOrNull(0)?.toIntOrNull()
                        val yTo = parts.getOrNull(1)?.toIntOrNull() ?: yFrom

                        if (jsonModels.none { it.name.equals(modelName, ignoreCase = true) }) {
                            extendedModels.add(
                                Model(
                                    id = (jsonModels.size + index + 1).toLong(),
                                    brandId = brandId,
                                    name = modelName,
                                    yearFrom = yFrom,
                                    yearTo = yTo
                                )
                            )
                        }
                    } catch (_: Throwable) { /* ignore bad profile */ }
                }
            } catch (e: Exception) {
                Log.w(log, "ExtendedVehicleDatabase not available for $brandName: ${e.message}")
            }
            
            val allModels = mutableListOf<Model>().apply { addAll(jsonModels); addAll(extendedModels) }
                .distinctBy { it.name.lowercase(Locale.US) }
            Log.d(log, "Total models loaded for $brandName: ${allModels.size}")
            allModels
        } catch (e: Exception) {
            Log.e(log, "Failed to load models for brandId $brandId: ${e.message}")
            emptyList()
        }
    }

    // ---------- Legacy compatibility methods ----------
    suspend fun getBrands(dbName: String): List<VehicleBrand> = 
        listBrands().map { VehicleBrand(it) }

    suspend fun getModels(dbName: String, brandId: Int): List<VehicleModel> = 
        listModels(brandId.toLong()).map { VehicleModel(it) }

    suspend fun getEcus(dbName: String, modelId: Int): List<VehicleEcu> = 
        listEcus(modelId.toLong()).map { VehicleEcu(it) }

    suspend fun getPids(dbName: String, ecuId: Int): List<VehiclePid> = 
        listPids(ecuId.toLong()).map { VehiclePid(it) }

    // Fallback method for when database is not available
    suspend fun getStandardPids(): List<VehiclePid> = withContext(Dispatchers.IO) {
        listOf(
            VehiclePid("0C", "Engine RPM", "RPM", "((A*256)+B)/4"),
            VehiclePid("0D", "Vehicle Speed", "km/h", "A"),
            VehiclePid("05", "Coolant Temperature", "°C", "A-40"),
            VehiclePid("2F", "Fuel Level", "%", "(A*100)/255"),
            VehiclePid("0F", "Intake Air Temperature", "°C", "A-40"),
            VehiclePid("11", "Throttle Position", "%", "(A*100)/255"),
            VehiclePid("04", "Engine Load", "%", "(A*100)/255")
        )
    }

}

// Type alias for backward compatibility
typealias VehicleLibraryManager = VehicleLibrary

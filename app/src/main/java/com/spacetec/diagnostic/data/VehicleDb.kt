package com.spacetec.diagnostic.data

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.TypeConverters
import androidx.room.TypeConverter
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Database(
    entities = [
        VehicleInfo::class,
        PidDefinition::class,
        DtcDefinition::class,
        VehicleProfile::class,
        DtcTable::class,
        PidProfile::class,
        WmiTable::class,
        ServiceMap::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VehicleDb : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao

    companion object {
        @Volatile
        private var INSTANCE: VehicleDb? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new tables
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `dtc` (
                        `code` TEXT NOT NULL,
                        `description_en` TEXT NOT NULL,
                        `description_xx` TEXT,
                        PRIMARY KEY(`code`)
                    )
                """)
                
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pid_profile` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `make` TEXT NOT NULL,
                        `model` TEXT NOT NULL,
                        `year_from` INTEGER NOT NULL,
                        `year_to` INTEGER NOT NULL,
                        `pid` INTEGER NOT NULL,
                        `label` TEXT NOT NULL,
                        `unit` TEXT,
                        `formula` TEXT
                    )
                """)
                
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `wmi` (
                        `wmi` TEXT NOT NULL,
                        `make` TEXT NOT NULL,
                        PRIMARY KEY(`wmi`)
                    )
                """)
                
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `service_map` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `make` TEXT NOT NULL,
                        `model` TEXT NOT NULL,
                        `year_from` INTEGER NOT NULL,
                        `year_to` INTEGER NOT NULL,
                        `uds_did` INTEGER NOT NULL,
                        `label` TEXT NOT NULL
                    )
                """)
            }
        }

        fun getDatabase(context: Context): VehicleDb {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VehicleDb::class.java,
                    "vehicle_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(list: List<String>?): String? = list?.let { gson.toJson(it) }

    @TypeConverter
    fun toStringList(data: String?): List<String> = data?.let {
        val type = object : TypeToken<List<String>>() {}.type
        gson.fromJson(it, type)
    } ?: emptyList()

    @TypeConverter
    fun fromIntList(list: List<Int>?): String? = list?.let { gson.toJson(it) }

    @TypeConverter
    fun toIntList(data: String?): List<Int> = data?.let {
        val type = object : TypeToken<List<Int>>() {}.type
        gson.fromJson(it, type)
    } ?: emptyList()
}

@Entity
data class VehicleInfo(
    @PrimaryKey val vin: String,
    val make: String,
    val model: String,
    val year: Int,
    val supportedProtocols: List<String>
)

@Entity
data class PidDefinition(
    @PrimaryKey val pid: Int,
    val name: String,
    val description: String,
    val formula: String?,
    val units: String?
)

@Entity
data class DtcDefinition(
    @PrimaryKey val code: String,
    val description: String,
    val severity: String
)

@Entity(tableName = "dtc")
data class DtcTable(
    @PrimaryKey val code: String,
    val description_en: String,
    val description_xx: String?
)

@Entity(tableName = "pid_profile")
data class PidProfile(
    val make: String,
    val model: String,
    val year_from: Int,
    val year_to: Int,
    val pid: Int,
    val label: String,
    val unit: String?,
    val formula: String?
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}

@Entity(tableName = "wmi")
data class WmiTable(
    @PrimaryKey val wmi: String,
    val make: String
)

@Entity(tableName = "service_map")
data class ServiceMap(
    val make: String,
    val model: String,
    val year_from: Int,
    val year_to: Int,
    val uds_did: Int,
    val label: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}

@Entity
data class VehicleProfile(
    @PrimaryKey val vin: String,
    val lastConnected: Long,
    val favoritePids: List<Int>
)

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicleinfo WHERE vin = :vin")
    suspend fun getVehicle(vin: String): VehicleInfo?

    @Query("SELECT * FROM piddefinition WHERE pid IN (:pids)")
    suspend fun getPidDefinitions(pids: List<Int>): List<PidDefinition>

    @Query("SELECT * FROM dtcdefinition WHERE code IN (:codes)")
    suspend fun getDtcDefinitions(codes: List<String>): List<DtcDefinition>

    @Upsert
    suspend fun saveVehicleProfile(profile: VehicleProfile)

    @Query("SELECT * FROM vehicleprofile WHERE vin = :vin")
    suspend fun getVehicleProfile(vin: String): VehicleProfile?

    // New table operations
    @Query("SELECT * FROM dtc WHERE code = :code")
    suspend fun getDtc(code: String): DtcTable?

    @Query("SELECT * FROM dtc WHERE code IN (:codes)")
    suspend fun getDtcs(codes: List<String>): List<DtcTable>

    @Upsert
    suspend fun insertDtc(dtc: DtcTable)

    @Query("SELECT * FROM pid_profile WHERE make = :make AND model = :model AND :year BETWEEN year_from AND year_to")
    suspend fun getPidProfilesForVehicle(make: String, model: String, year: Int): List<PidProfile>

    @Query("SELECT * FROM pid_profile WHERE pid = :pid")
    suspend fun getPidProfilesByPid(pid: Int): List<PidProfile>

    @Upsert
    suspend fun insertPidProfile(pidProfile: PidProfile)

    @Query("SELECT * FROM wmi WHERE wmi = :wmi")
    suspend fun getWmi(wmi: String): WmiTable?

    @Query("SELECT * FROM wmi WHERE make = :make")
    suspend fun getWmiByMake(make: String): List<WmiTable>

    @Upsert
    suspend fun insertWmi(wmi: WmiTable)

    @Query("SELECT * FROM service_map WHERE make = :make AND model = :model AND :year BETWEEN year_from AND year_to")
    suspend fun getServiceMapForVehicle(make: String, model: String, year: Int): List<ServiceMap>

    @Query("SELECT * FROM service_map WHERE uds_did = :udsDid")
    suspend fun getServiceMapByDid(udsDid: Int): List<ServiceMap>

    @Upsert
    suspend fun insertServiceMap(serviceMap: ServiceMap)
}

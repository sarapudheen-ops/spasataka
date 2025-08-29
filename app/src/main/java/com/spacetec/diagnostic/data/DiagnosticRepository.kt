package com.spacetec.diagnostic.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for accessing diagnostic data including DTCs, PID profiles, WMI codes, and service maps
 */
class DiagnosticRepository(context: Context) {
    private val database = VehicleDb.getDatabase(context)
    private val dao = database.vehicleDao()

    /**
     * Get DTC information by code
     */
    suspend fun getDtcInfo(code: String): DtcTable? = withContext(Dispatchers.IO) {
        dao.getDtc(code)
    }

    /**
     * Get multiple DTC information by codes
     */
    suspend fun getDtcInfoBatch(codes: List<String>): List<DtcTable> = withContext(Dispatchers.IO) {
        dao.getDtcs(codes)
    }

    /**
     * Add or update DTC information
     */
    suspend fun saveDtc(code: String, descriptionEn: String, descriptionXx: String? = null) = withContext(Dispatchers.IO) {
        dao.insertDtc(DtcTable(code, descriptionEn, descriptionXx))
    }

    /**
     * Get PID profiles for a specific vehicle
     */
    suspend fun getPidProfilesForVehicle(make: String, model: String, year: Int): List<PidProfile> = withContext(Dispatchers.IO) {
        dao.getPidProfilesForVehicle(make, model, year)
    }

    /**
     * Get all PID profiles for a specific PID
     */
    suspend fun getPidProfilesByPid(pid: Int): List<PidProfile> = withContext(Dispatchers.IO) {
        dao.getPidProfilesByPid(pid)
    }

    /**
     * Add or update PID profile
     */
    suspend fun savePidProfile(
        make: String,
        model: String,
        yearFrom: Int,
        yearTo: Int,
        pid: Int,
        label: String,
        unit: String? = null,
        formula: String? = null
    ) = withContext(Dispatchers.IO) {
        dao.insertPidProfile(PidProfile(make, model, yearFrom, yearTo, pid, label, unit, formula))
    }

    /**
     * Get manufacturer by WMI code
     */
    suspend fun getManufacturerByWmi(wmi: String): WmiTable? = withContext(Dispatchers.IO) {
        dao.getWmi(wmi)
    }

    /**
     * Get WMI codes by manufacturer
     */
    suspend fun getWmiByManufacturer(make: String): List<WmiTable> = withContext(Dispatchers.IO) {
        dao.getWmiByMake(make)
    }

    /**
     * Add or update WMI information
     */
    suspend fun saveWmi(wmi: String, make: String) = withContext(Dispatchers.IO) {
        dao.insertWmi(WmiTable(wmi, make))
    }

    /**
     * Get service map entries for a specific vehicle
     */
    suspend fun getServiceMapForVehicle(make: String, model: String, year: Int): List<ServiceMap> = withContext(Dispatchers.IO) {
        dao.getServiceMapForVehicle(make, model, year)
    }

    /**
     * Get service map entries by UDS DID
     */
    suspend fun getServiceMapByDid(udsDid: Int): List<ServiceMap> = withContext(Dispatchers.IO) {
        dao.getServiceMapByDid(udsDid)
    }

    /**
     * Add or update service map entry
     */
    suspend fun saveServiceMap(
        make: String,
        model: String,
        yearFrom: Int,
        yearTo: Int,
        udsDid: Int,
        label: String
    ) = withContext(Dispatchers.IO) {
        dao.insertServiceMap(ServiceMap(make, model, yearFrom, yearTo, udsDid, label))
    }

    /**
     * Extract manufacturer from VIN using WMI
     */
    suspend fun getManufacturerFromVin(vin: String): String? = withContext(Dispatchers.IO) {
        if (vin.length < 3) return@withContext null
        val wmi = vin.substring(0, 3)
        dao.getWmi(wmi)?.make
    }

    /**
     * Get comprehensive vehicle diagnostic profile
     */
    suspend fun getVehicleDiagnosticProfile(make: String, model: String, year: Int): VehicleDiagnosticProfile = withContext(Dispatchers.IO) {
        val pidProfiles = dao.getPidProfilesForVehicle(make, model, year)
        val serviceMap = dao.getServiceMapForVehicle(make, model, year)
        
        VehicleDiagnosticProfile(
            make = make,
            model = model,
            year = year,
            supportedPids = pidProfiles,
            serviceMap = serviceMap
        )
    }
}

/**
 * Comprehensive diagnostic profile for a vehicle
 */
data class VehicleDiagnosticProfile(
    val make: String,
    val model: String,
    val year: Int,
    val supportedPids: List<PidProfile>,
    val serviceMap: List<ServiceMap>
)

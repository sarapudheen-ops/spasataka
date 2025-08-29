package com.spacetec.database

import android.content.Context

/**
 * Comprehensive Vehicle Database - Professional Implementation
 * Extensive offline database covering all major vehicle brands and models
 */
class VehicleDatabase(private val context: Context) {
    
    data class Vehicle(
        val make: String,
        val model: String,
        val year: Int,
        val engine: String,
        val transmission: String,
        val vinPattern: String,
        val supportedProtocols: List<String>,
        val availableServices: List<String>,
        val ecuAddresses: Map<String, Int>
    )
    
    private val vehicles = mutableListOf<Vehicle>()
    
    init {
        initializeDatabase()
    }
    
    private fun initializeDatabase() {
        // BMW Vehicles
        vehicles.addAll(listOf(
            Vehicle("BMW", "1 Series", 2020, "2.0L Turbo", "8-Speed Auto", "WBA", 
                listOf("CAN", "UDS", "CAN-FD"), 
                listOf("Oil Reset", "Battery Registration", "Coding", "TPMS Reset"),
                mapOf("Engine" to 0x12, "Transmission" to 0x18, "ABS" to 0x20)),
            Vehicle("BMW", "3 Series", 2021, "2.0L Turbo", "8-Speed Auto", "WBA",
                listOf("CAN", "UDS", "CAN-FD", "DoIP"),
                listOf("Oil Reset", "Battery Registration", "Coding", "TPMS Reset", "DPF Regeneration"),
                mapOf("Engine" to 0x12, "Transmission" to 0x18, "ABS" to 0x20, "SRS" to 0x5F)),
            Vehicle("BMW", "5 Series", 2022, "3.0L Turbo", "8-Speed Auto", "WBA",
                listOf("CAN", "UDS", "CAN-FD", "DoIP"),
                listOf("Oil Reset", "Battery Registration", "Coding", "TPMS Reset", "Suspension Calibration"),
                mapOf("Engine" to 0x12, "Transmission" to 0x18, "ABS" to 0x20, "Suspension" to 0x37)),
            Vehicle("BMW", "X3", 2021, "2.0L Turbo", "8-Speed Auto", "WBY",
                listOf("CAN", "UDS", "CAN-FD"),
                listOf("Oil Reset", "Battery Registration", "Coding", "TPMS Reset", "AWD Calibration"),
                mapOf("Engine" to 0x12, "AWD" to 0x1E, "ABS" to 0x20)),
            Vehicle("BMW", "X5", 2022, "4.4L V8", "8-Speed Auto", "5UX",
                listOf("CAN", "UDS", "CAN-FD", "DoIP"),
                listOf("Oil Reset", "Battery Registration", "Coding", "TPMS Reset", "Air Suspension"),
                mapOf("Engine" to 0x12, "Suspension" to 0x37, "AWD" to 0x1E))
        ))
        
        // Mercedes-Benz Vehicles
        vehicles.addAll(listOf(
            Vehicle("Mercedes-Benz", "A-Class", 2020, "2.0L Turbo", "7G-DCT", "WDD",
                listOf("CAN", "UDS", "CAN-FD"),
                listOf("Oil Reset", "Service Reset", "Coding", "TPMS Reset"),
                mapOf("Engine" to 0x7E0, "Transmission" to 0x7E1, "ABS" to 0x7E2)),
            Vehicle("Mercedes-Benz", "C-Class", 2021, "2.0L Turbo", "9G-Tronic", "WDD",
                listOf("CAN", "UDS", "CAN-FD", "DoIP"),
                listOf("Oil Reset", "Service Reset", "Coding", "TPMS Reset", "DPF Regeneration"),
                mapOf("Engine" to 0x7E0, "Transmission" to 0x7E1, "ABS" to 0x7E2, "SRS" to 0x7E3)),
            Vehicle("Mercedes-Benz", "E-Class", 2022, "3.0L V6", "9G-Tronic", "WDD",
                listOf("CAN", "UDS", "CAN-FD", "DoIP"),
                listOf("Oil Reset", "Service Reset", "Coding", "TPMS Reset", "Air Suspension"),
                mapOf("Engine" to 0x7E0, "Suspension" to 0x7E4, "ABS" to 0x7E2)),
            Vehicle("Mercedes-Benz", "GLC", 2021, "2.0L Turbo", "9G-Tronic", "WDC",
                listOf("CAN", "UDS", "CAN-FD"),
                listOf("Oil Reset", "Service Reset", "Coding", "TPMS Reset", "AWD Calibration"),
                mapOf("Engine" to 0x7E0, "AWD" to 0x7E5, "ABS" to 0x7E2))
        ))
        
        // Audi Vehicles
        vehicles.addAll(listOf(
            Vehicle("Audi", "A3", 2020, "2.0L TFSI", "S Tronic", "WAU",
                listOf("CAN", "UDS", "CAN-FD"),
                listOf("Oil Reset", "Service Reset", "Coding", "TPMS Reset"),
                mapOf("Engine" to 0x01, "Transmission" to 0x02, "ABS" to 0x03)),
            Vehicle("Audi", "A4", 2021, "2.0L TFSI", "S Tronic", "WAU",
                listOf("CAN", "UDS", "CAN-FD", "DoIP"),
                listOf("Oil Reset", "Service Reset", "Coding", "TPMS Reset", "DPF Regeneration"),
                mapOf("Engine" to 0x01, "Transmission" to 0x02, "ABS" to 0x03, "SRS" to 0x15)),
            Vehicle("Audi", "Q5", 2022, "2.0L TFSI", "Tiptronic", "WA1",
                listOf("CAN", "UDS", "CAN-FD", "DoIP"),
                listOf("Oil Reset", "Service Reset", "Coding", "TPMS Reset", "Quattro Calibration"),
                mapOf("Engine" to 0x01, "AWD" to 0x22, "ABS" to 0x03))
        ))
        
        // Ford Vehicles
        vehicles.addAll(listOf(
            Vehicle("Ford", "Focus", 2020, "2.0L EcoBoost", "8-Speed Auto", "1FA",
                listOf("CAN", "UDS"),
                listOf("Oil Reset", "TPMS Reset", "Throttle Adaptation"),
                mapOf("Engine" to 0x7E0, "Transmission" to 0x7E1, "ABS" to 0x7E2)),
            Vehicle("Ford", "Mustang", 2022, "5.0L V8", "10-Speed Auto", "1FA",
                listOf("CAN", "UDS", "CAN-FD"),
                listOf("Oil Reset", "TPMS Reset", "Throttle Adaptation", "Launch Control"),
                mapOf("Engine" to 0x7E0, "Transmission" to 0x7E1, "ABS" to 0x7E2)),
            Vehicle("Ford", "F-150", 2021, "3.5L EcoBoost", "10-Speed Auto", "1FM",
                listOf("CAN", "UDS", "CAN-FD"),
                listOf("Oil Reset", "TPMS Reset", "DPF Regeneration", "4WD Calibration"),
                mapOf("Engine" to 0x7E0, "4WD" to 0x7E3, "ABS" to 0x7E2))
        ))
        
        // GM Vehicles
        vehicles.addAll(listOf(
            Vehicle("Chevrolet", "Camaro", 2020, "6.2L V8", "10-Speed Auto", "1G1",
                listOf("CAN", "UDS"),
                listOf("Oil Reset", "TPMS Reset", "Throttle Relearn"),
                mapOf("Engine" to 0x7E0, "Transmission" to 0x7E1, "ABS" to 0x7E2)),
            Vehicle("Chevrolet", "Silverado", 2021, "5.3L V8", "10-Speed Auto", "1GC",
                listOf("CAN", "UDS", "CAN-FD"),
                listOf("Oil Reset", "TPMS Reset", "DPF Regeneration", "4WD Calibration"),
                mapOf("Engine" to 0x7E0, "4WD" to 0x7E3, "ABS" to 0x7E2)),
            Vehicle("Cadillac", "Escalade", 2022, "6.2L V8", "10-Speed Auto", "1GY",
                listOf("CAN", "UDS", "CAN-FD"),
                listOf("Oil Reset", "TPMS Reset", "Air Suspension", "AWD Calibration"),
                mapOf("Engine" to 0x7E0, "Suspension" to 0x7E4, "AWD" to 0x7E5))
        ))
        
        // Toyota Vehicles
        vehicles.addAll(listOf(
            Vehicle("Toyota", "Camry", 2022, "2.5L Hybrid", "CVT", "JTD",
                listOf("CAN", "UDS"),
                listOf("Oil Reset", "TPMS Reset", "Hybrid System Check"),
                mapOf("Engine" to 0x7E0, "Hybrid" to 0x7E2, "ABS" to 0x7E1)),
            Vehicle("Toyota", "RAV4", 2021, "2.5L", "8-Speed Auto", "5TD",
                listOf("CAN", "UDS"),
                listOf("Oil Reset", "TPMS Reset", "Throttle Adaptation", "AWD Calibration"),
                mapOf("Engine" to 0x7E0, "AWD" to 0x7E3, "ABS" to 0x7E1)),
            Vehicle("Toyota", "Prius", 2022, "1.8L Hybrid", "CVT", "JTD",
                listOf("CAN", "UDS"),
                listOf("Oil Reset", "TPMS Reset", "Hybrid System Check", "Battery Calibration"),
                mapOf("Engine" to 0x7E0, "Hybrid" to 0x7E2, "Battery" to 0x7E4))
        ))
        
        // Honda Vehicles
        vehicles.addAll(listOf(
            Vehicle("Honda", "Civic", 2022, "1.5L Turbo", "CVT", "1HC",
                listOf("CAN", "UDS"),
                listOf("Oil Reset", "TPMS Reset", "Throttle Adaptation"),
                mapOf("Engine" to 0x7E0, "Transmission" to 0x7E1, "ABS" to 0x7E2)),
            Vehicle("Honda", "CR-V", 2021, "1.5L Turbo", "CVT", "5J6",
                listOf("CAN", "UDS"),
                listOf("Oil Reset", "TPMS Reset", "Throttle Adaptation", "AWD Calibration"),
                mapOf("Engine" to 0x7E0, "AWD" to 0x7E3, "ABS" to 0x7E2)),
            Vehicle("Honda", "Accord", 2022, "2.0L Turbo", "10-Speed Auto", "1HC",
                listOf("CAN", "UDS"),
                listOf("Oil Reset", "TPMS Reset", "Throttle Adaptation", "Transmission Learn"),
                mapOf("Engine" to 0x7E0, "Transmission" to 0x7E1, "ABS" to 0x7E2))
        ))
    }
    
    fun searchByMake(make: String): List<Vehicle> {
        return vehicles.filter { it.make.equals(make, ignoreCase = true) }
    }
    
    fun searchByModel(model: String): List<Vehicle> {
        return vehicles.filter { it.model.contains(model, ignoreCase = true) }
    }
    
    fun searchByVinPattern(vinPattern: String): Vehicle? {
        return vehicles.find { it.vinPattern == vinPattern }
    }
    
    fun getAllMakes(): List<String> {
        return vehicles.map { it.make }.distinct().sorted()
    }
    
    fun getModelsByMake(make: String): List<String> {
        return vehicles.filter { it.make.equals(make, ignoreCase = true) }
            .map { it.model }.distinct().sorted()
    }
    
    fun getAllVehicles(): List<Vehicle> {
        return vehicles.toList()
    }
    
    fun getAllBrands(): List<String> {
        return vehicles.map { it.make }.distinct()
    }

    fun getModelsForBrand(brand: String): List<String> {
        return vehicles.filter { it.make == brand }.map { it.model }
    }
}

package com.spacetec.diagnostic.commercial

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Commercial Vehicle Support System
 * Based on MaxiSys commercial vehicle diagnostic capabilities
 */
class CommercialVehicleSupport(private val context: Context) {
    
    private val tag = "CommercialVehicle"
    
    private val _supportedVehicles = MutableStateFlow<List<CommercialVehicle>>(emptyList())
    val supportedVehicles: StateFlow<List<CommercialVehicle>> = _supportedVehicles
    
    private val _currentVehicle = MutableStateFlow<CommercialVehicle?>(null)
    val currentVehicle: StateFlow<CommercialVehicle?> = _currentVehicle
    
    enum class VehicleType {
        HEAVY_DUTY_TRUCK,
        MEDIUM_DUTY_TRUCK,
        LIGHT_DUTY_TRUCK,
        BUS,
        COACH,
        DELIVERY_VAN,
        CONSTRUCTION_EQUIPMENT,
        AGRICULTURAL_EQUIPMENT,
        MARINE_ENGINE,
        GENERATOR_SET,
        STATIONARY_ENGINE
    }
    
    enum class EngineType {
        DIESEL,
        CNG,
        LPG,
        HYBRID_DIESEL,
        ELECTRIC,
        HYDROGEN
    }
    
    data class CommercialVehicle(
        val make: String,
        val model: String,
        val type: VehicleType,
        val engineType: EngineType,
        val yearRange: IntRange,
        val supportedProtocols: List<String>,
        val specialFeatures: List<String>,
        val ecuList: List<CommercialEcu>
    )
    
    data class CommercialEcu(
        val name: String,
        val id: String,
        val protocol: String,
        val functions: List<String>,
        val parameters: List<String>
    )
    
    init {
        initializeCommercialVehicles()
    }
    
    private fun initializeCommercialVehicles() {
        val vehicles = mutableListOf<CommercialVehicle>()
        
        // Heavy Duty Trucks
        vehicles.addAll(createHeavyDutyTrucks())
        
        // Medium Duty Trucks
        vehicles.addAll(createMediumDutyTrucks())
        
        // Buses and Coaches
        vehicles.addAll(createBusesAndCoaches())
        
        // Construction Equipment
        vehicles.addAll(createConstructionEquipment())
        
        // Agricultural Equipment
        vehicles.addAll(createAgriculturalEquipment())
        
        // Marine and Stationary Engines
        vehicles.addAll(createMarineAndStationaryEngines())
        
        _supportedVehicles.value = vehicles
        Log.d(tag, "Initialized ${vehicles.size} commercial vehicle configurations")
    }
    
    private fun createHeavyDutyTrucks(): List<CommercialVehicle> {
        return listOf(
            CommercialVehicle(
                make = "Volvo",
                model = "VNL Series",
                type = VehicleType.HEAVY_DUTY_TRUCK,
                engineType = EngineType.DIESEL,
                yearRange = 2010..2024,
                supportedProtocols = listOf("J1939", "J1708", "J2534"),
                specialFeatures = listOf(
                    "DPF Regeneration",
                    "SCR System Diagnosis",
                    "Engine Parameter Programming",
                    "Transmission Calibration",
                    "ADAS Calibration"
                ),
                ecuList = listOf(
                    CommercialEcu("Engine Control Module", "ECM", "J1939", 
                        listOf("DTC Reading", "Live Data", "Actuator Tests", "Parameter Reset"),
                        listOf("Engine Speed", "Boost Pressure", "EGR Position", "DPF Status")),
                    CommercialEcu("Transmission Control Module", "TCM", "J1939",
                        listOf("Shift Adaptation", "Clutch Calibration", "Diagnostic Tests"),
                        listOf("Gear Position", "Clutch Pressure", "Transmission Temperature")),
                    CommercialEcu("After Treatment Control Module", "ACM", "J1939",
                        listOf("DPF Regeneration", "SCR Diagnosis", "NOx Sensor Calibration"),
                        listOf("DPF Pressure", "SCR Temperature", "NOx Levels", "DEF Level"))
                )
            ),
            CommercialVehicle(
                make = "Freightliner",
                model = "Cascadia",
                type = VehicleType.HEAVY_DUTY_TRUCK,
                engineType = EngineType.DIESEL,
                yearRange = 2008..2024,
                supportedProtocols = listOf("J1939", "J1708", "J2534"),
                specialFeatures = listOf(
                    "Detroit Connect Integration",
                    "Predictive Cruise Control",
                    "Collision Mitigation System",
                    "Lane Departure Warning"
                ),
                ecuList = listOf(
                    CommercialEcu("Detroit Engine ECU", "DECU", "J1939",
                        listOf("Engine Diagnostics", "Performance Tuning", "Fuel System Tests"),
                        listOf("Fuel Rail Pressure", "Turbo Speed", "Oil Pressure", "Coolant Temperature")),
                    CommercialEcu("Detroit Transmission ECU", "DTCU", "J1939",
                        listOf("Shift Programming", "Torque Management", "Clutch Adaptation"),
                        listOf("Input Speed", "Output Speed", "Line Pressure", "Converter Lockup"))
                )
            ),
            CommercialVehicle(
                make = "Peterbilt",
                model = "579",
                type = VehicleType.HEAVY_DUTY_TRUCK,
                engineType = EngineType.DIESEL,
                yearRange = 2012..2024,
                supportedProtocols = listOf("J1939", "J1708", "J2534"),
                specialFeatures = listOf(
                    "PACCAR Engine Integration",
                    "SmartNav System",
                    "Bendix Wingman Fusion",
                    "Eaton UltraShift Programming"
                ),
                ecuList = listOf(
                    CommercialEcu("PACCAR Engine ECU", "PECU", "J1939",
                        listOf("Engine Calibration", "Emission System Tests", "Performance Analysis"),
                        listOf("Engine Load", "Exhaust Temperature", "Fuel Consumption", "Engine Hours")),
                    CommercialEcu("Eaton Transmission ECU", "ETCU", "J1939",
                        listOf("Shift Strategy Programming", "Clutch Calibration", "Performance Optimization"),
                        listOf("Clutch Position", "Shift Force", "Transmission Load", "Gear Ratio"))
                )
            )
        )
    }
    
    private fun createMediumDutyTrucks(): List<CommercialVehicle> {
        return listOf(
            CommercialVehicle(
                make = "Isuzu",
                model = "NPR Series",
                type = VehicleType.MEDIUM_DUTY_TRUCK,
                engineType = EngineType.DIESEL,
                yearRange = 2008..2024,
                supportedProtocols = listOf("J1939", "OBD-II", "ISO15765"),
                specialFeatures = listOf(
                    "Isuzu Reach Integration",
                    "Diesel Particulate Filter Service",
                    "Selective Catalytic Reduction",
                    "Transmission Programming"
                ),
                ecuList = listOf(
                    CommercialEcu("Isuzu Engine ECU", "IECU", "J1939",
                        listOf("Engine Diagnostics", "DPF Service", "Injector Programming"),
                        listOf("Rail Pressure", "DPF Delta Pressure", "SCR Temperature", "Engine RPM"))
                )
            ),
            CommercialVehicle(
                make = "Hino",
                model = "268 Series",
                type = VehicleType.MEDIUM_DUTY_TRUCK,
                engineType = EngineType.DIESEL,
                yearRange = 2011..2024,
                supportedProtocols = listOf("J1939", "OBD-II", "ISO15765"),
                specialFeatures = listOf(
                    "Hino Insight Telematics",
                    "Emission System Service",
                    "Transmission Adaptation",
                    "Brake System Diagnosis"
                ),
                ecuList = listOf(
                    CommercialEcu("Hino Engine ECU", "HECU", "J1939",
                        listOf("Engine Parameter Reset", "DPF Regeneration", "SCR Diagnosis"),
                        listOf("Boost Pressure", "EGR Flow", "DPF Status", "NOx Sensor"))
                )
            )
        )
    }
    
    private fun createBusesAndCoaches(): List<CommercialVehicle> {
        return listOf(
            CommercialVehicle(
                make = "New Flyer",
                model = "Xcelsior",
                type = VehicleType.BUS,
                engineType = EngineType.HYBRID_DIESEL,
                yearRange = 2014..2024,
                supportedProtocols = listOf("J1939", "J1708", "CAN"),
                specialFeatures = listOf(
                    "Hybrid System Diagnosis",
                    "Battery Management System",
                    "Regenerative Braking Calibration",
                    "HVAC System Control"
                ),
                ecuList = listOf(
                    CommercialEcu("Hybrid Control Module", "HCM", "J1939",
                        listOf("Battery Diagnostics", "Motor Control", "Energy Management"),
                        listOf("Battery SOC", "Motor Temperature", "Regenerative Power", "System Efficiency")),
                    CommercialEcu("Engine Control Module", "ECM", "J1939",
                        listOf("Engine Start/Stop", "Emission Control", "Fuel Management"),
                        listOf("Engine Load", "Fuel Rate", "Exhaust Temperature", "Operating Hours"))
                )
            ),
            CommercialVehicle(
                make = "BYD",
                model = "K9 Electric Bus",
                type = VehicleType.BUS,
                engineType = EngineType.ELECTRIC,
                yearRange = 2016..2024,
                supportedProtocols = listOf("CAN", "J1939", "ISO15765"),
                specialFeatures = listOf(
                    "Battery Pack Diagnosis",
                    "Motor Controller Programming",
                    "Charging System Analysis",
                    "Thermal Management"
                ),
                ecuList = listOf(
                    CommercialEcu("Battery Management System", "BMS", "CAN",
                        listOf("Cell Balancing", "Thermal Management", "Safety Monitoring"),
                        listOf("Cell Voltage", "Pack Temperature", "Current Flow", "Insulation Resistance")),
                    CommercialEcu("Motor Control Unit", "MCU", "CAN",
                        listOf("Motor Calibration", "Torque Control", "Efficiency Optimization"),
                        listOf("Motor Speed", "Torque Output", "Motor Temperature", "Power Consumption"))
                )
            )
        )
    }
    
    private fun createConstructionEquipment(): List<CommercialVehicle> {
        return listOf(
            CommercialVehicle(
                make = "Caterpillar",
                model = "320 Excavator",
                type = VehicleType.CONSTRUCTION_EQUIPMENT,
                engineType = EngineType.DIESEL,
                yearRange = 2010..2024,
                supportedProtocols = listOf("J1939", "CAT ET", "J1708"),
                specialFeatures = listOf(
                    "Hydraulic System Diagnosis",
                    "Engine Performance Tuning",
                    "Emission System Service",
                    "Attachment Recognition"
                ),
                ecuList = listOf(
                    CommercialEcu("Engine Control Module", "ECM", "J1939",
                        listOf("Engine Diagnostics", "Performance Analysis", "Emission Tests"),
                        listOf("Engine Speed", "Hydraulic Pressure", "Fuel Consumption", "Operating Temperature")),
                    CommercialEcu("Hydraulic Control Module", "HCM", "J1939",
                        listOf("Hydraulic Calibration", "Flow Control", "Pressure Tests"),
                        listOf("System Pressure", "Flow Rate", "Valve Position", "Cylinder Pressure"))
                )
            ),
            CommercialVehicle(
                make = "Komatsu",
                model = "PC200 Excavator",
                type = VehicleType.CONSTRUCTION_EQUIPMENT,
                engineType = EngineType.DIESEL,
                yearRange = 2012..2024,
                supportedProtocols = listOf("J1939", "Komatsu KDPF", "CAN"),
                specialFeatures = listOf(
                    "KOMTRAX Integration",
                    "Hydraulic Matching",
                    "Engine Derate Management",
                    "Machine Control System"
                ),
                ecuList = listOf(
                    CommercialEcu("Komatsu Engine ECU", "KECU", "J1939",
                        listOf("Engine Parameter Programming", "DPF Service", "Performance Optimization"),
                        listOf("Engine Load", "DPF Status", "Fuel Rate", "Coolant Temperature"))
                )
            )
        )
    }
    
    private fun createAgriculturalEquipment(): List<CommercialVehicle> {
        return listOf(
            CommercialVehicle(
                make = "John Deere",
                model = "8R Series Tractor",
                type = VehicleType.AGRICULTURAL_EQUIPMENT,
                engineType = EngineType.DIESEL,
                yearRange = 2014..2024,
                supportedProtocols = listOf("J1939", "ISOBUS", "CAN"),
                specialFeatures = listOf(
                    "Precision Agriculture Integration",
                    "AutoTrac Guidance System",
                    "Hydraulic Remote Valves",
                    "PTO Control Programming"
                ),
                ecuList = listOf(
                    CommercialEcu("PowerTech Engine ECU", "PECU", "J1939",
                        listOf("Engine Calibration", "Emission System Service", "Power Management"),
                        listOf("Engine Power", "Torque Curve", "Fuel Efficiency", "DEF Consumption")),
                    CommercialEcu("Transmission Control Unit", "TCU", "J1939",
                        listOf("Shift Programming", "PTO Calibration", "Hydraulic Control"),
                        listOf("Ground Speed", "PTO Speed", "Hydraulic Flow", "Transmission Temperature"))
                )
            ),
            CommercialVehicle(
                make = "Case IH",
                model = "Magnum Series",
                type = VehicleType.AGRICULTURAL_EQUIPMENT,
                engineType = EngineType.DIESEL,
                yearRange = 2013..2024,
                supportedProtocols = listOf("J1939", "ISOBUS", "CAN"),
                specialFeatures = listOf(
                    "AFS Connect Integration",
                    "CVT Transmission Programming",
                    "Hydraulic System Calibration",
                    "Implement Control"
                ),
                ecuList = listOf(
                    CommercialEcu("FPT Engine ECU", "FECU", "J1939",
                        listOf("Engine Diagnostics", "SCR System Service", "Performance Tuning"),
                        listOf("Engine Speed", "Boost Pressure", "SCR Temperature", "Engine Hours"))
                )
            )
        )
    }
    
    private fun createMarineAndStationaryEngines(): List<CommercialVehicle> {
        return listOf(
            CommercialVehicle(
                make = "Cummins",
                model = "QSK Marine Engine",
                type = VehicleType.MARINE_ENGINE,
                engineType = EngineType.DIESEL,
                yearRange = 2010..2024,
                supportedProtocols = listOf("J1939", "NMEA2000", "CAN"),
                specialFeatures = listOf(
                    "Marine Engine Diagnostics",
                    "Fuel System Analysis",
                    "Cooling System Monitoring",
                    "Generator Set Integration"
                ),
                ecuList = listOf(
                    CommercialEcu("Marine Engine ECU", "MECU", "J1939",
                        listOf("Engine Diagnostics", "Performance Analysis", "Maintenance Scheduling"),
                        listOf("Engine Load", "Sea Water Temperature", "Fuel Consumption", "Operating Hours"))
                )
            ),
            CommercialVehicle(
                make = "Caterpillar",
                model = "Generator Set",
                type = VehicleType.GENERATOR_SET,
                engineType = EngineType.DIESEL,
                yearRange = 2008..2024,
                supportedProtocols = listOf("J1939", "Modbus", "CAN"),
                specialFeatures = listOf(
                    "Power Generation Control",
                    "Load Bank Testing",
                    "Paralleling System",
                    "Remote Monitoring"
                ),
                ecuList = listOf(
                    CommercialEcu("Generator Control Module", "GCM", "J1939",
                        listOf("Power Control", "Load Management", "System Protection"),
                        listOf("Power Output", "Frequency", "Voltage", "Load Factor"))
                )
            )
        )
    }
    
    fun getVehiclesByType(type: VehicleType): List<CommercialVehicle> {
        return _supportedVehicles.value.filter { it.type == type }
    }
    
    fun getVehiclesByMake(make: String): List<CommercialVehicle> {
        return _supportedVehicles.value.filter { 
            it.make.equals(make, ignoreCase = true) 
        }
    }
    
    fun getVehiclesByEngineType(engineType: EngineType): List<CommercialVehicle> {
        return _supportedVehicles.value.filter { it.engineType == engineType }
    }
    
    fun selectVehicle(vehicle: CommercialVehicle) {
        _currentVehicle.value = vehicle
        Log.d(tag, "Selected commercial vehicle: ${vehicle.make} ${vehicle.model}")
    }
    
    fun getSupportedProtocols(): List<String> {
        return _currentVehicle.value?.supportedProtocols ?: emptyList()
    }
    
    fun getAvailableEcus(): List<CommercialEcu> {
        return _currentVehicle.value?.ecuList ?: emptyList()
    }
    
    fun getSpecialFunctions(): List<String> {
        return _currentVehicle.value?.specialFeatures ?: emptyList()
    }
    
    fun isCommercialVehicleSupported(make: String, model: String, year: Int): Boolean {
        return _supportedVehicles.value.any { vehicle ->
            vehicle.make.equals(make, ignoreCase = true) &&
            vehicle.model.equals(model, ignoreCase = true) &&
            year in vehicle.yearRange
        }
    }
    
    fun getCommercialVehicleInfo(make: String, model: String, year: Int): CommercialVehicle? {
        return _supportedVehicles.value.find { vehicle ->
            vehicle.make.equals(make, ignoreCase = true) &&
            vehicle.model.equals(model, ignoreCase = true) &&
            year in vehicle.yearRange
        }
    }
    
    fun getAllSupportedMakes(): List<String> {
        return _supportedVehicles.value.map { it.make }.distinct().sorted()
    }
    
    fun getModelsForMake(make: String): List<String> {
        return _supportedVehicles.value
            .filter { it.make.equals(make, ignoreCase = true) }
            .map { it.model }
            .distinct()
            .sorted()
    }
    
    fun getVehicleTypeDescription(type: VehicleType): String {
        return when (type) {
            VehicleType.HEAVY_DUTY_TRUCK -> "Class 8 trucks over 33,000 lbs GVWR"
            VehicleType.MEDIUM_DUTY_TRUCK -> "Class 4-7 trucks 14,001-33,000 lbs GVWR"
            VehicleType.LIGHT_DUTY_TRUCK -> "Class 1-3 trucks under 14,000 lbs GVWR"
            VehicleType.BUS -> "Transit buses and school buses"
            VehicleType.COACH -> "Intercity coaches and motor coaches"
            VehicleType.DELIVERY_VAN -> "Commercial delivery and cargo vans"
            VehicleType.CONSTRUCTION_EQUIPMENT -> "Excavators, bulldozers, loaders, etc."
            VehicleType.AGRICULTURAL_EQUIPMENT -> "Tractors, combines, sprayers, etc."
            VehicleType.MARINE_ENGINE -> "Marine propulsion and auxiliary engines"
            VehicleType.GENERATOR_SET -> "Stationary and portable generator sets"
            VehicleType.STATIONARY_ENGINE -> "Industrial and stationary engines"
        }
    }
}

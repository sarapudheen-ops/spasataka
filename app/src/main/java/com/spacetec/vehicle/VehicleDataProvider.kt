package com.spacetec.vehicle

import com.spacetec.vehicle.model.*

/**
 * Provides sample vehicle data for development and testing
 */
object VehicleDataProvider {
    
    /**
     * Get a sample VehicleData instance with realistic values
     */
    fun getSampleVehicleData(): VehicleData {
        return VehicleData(
            speed = 85,
            rpm = 2450,
            coolantTemp = 92,
            fuelLevel = 65,
            throttlePosition = 23,
            engineLoad = 45,
            intakeAirTemp = 32,
            mafRate = 4.8f,
            ambientAirTemp = 25,
            barometricPressure = 101.3f,
            controlModuleVoltage = 13.8f,
            timingAdvance = 12.5f,
            runTime = 857,
            distanceWithMIL = 0,
            fuelRailPressure = 350f,
            fuelType = "Gasoline",
            odometer = 45230,
            vin = "WBA8E5C58J1234567",
            dtcCount = 0,
            isEngineRunning = true,
            vehicleInfo = getSampleVehicleInfo(),
            dtcList = getSampleDtcList(),
            supportedPIDs = getSupportedPIDs(),
            ecuInfo = getSampleEcuInfo()
        )
    }
    
    private fun getSampleVehicleInfo(): VehicleInfo {
        return VehicleInfo(
            make = "BMW",
            model = "330i",
            year = 2022,
            engineCode = "B48B20",
            transmissionType = "8-Speed Automatic",
            fuelType = "Gasoline",
            vin = "WBA8E5C58J1234567",
            ecuInfo = getSampleEcuInfo()
        )
    }
    
    private fun getSampleDtcList(): List<DiagnosticTroubleCode> {
        return listOf(
            DiagnosticTroubleCode(
                code = "P0172",
                description = "System Too Rich (Bank 1)",
                status = DtcStatus.PENDING,
                severity = DtcSeverity.MEDIUM,
                timestamp = System.currentTimeMillis() - 86400000 // 1 day ago
            )
        )
    }
    
    private fun getSupportedPIDs(): List<String> {
        return listOf(
            "0100", "0101", "0102", "0103", "0104", "0105", "0106", "0107", "0108", "0109",
            "010A", "010B", "010C", "010D", "010E", "010F", "0110", "0111", "0112", "0113",
            "0114", "0115", "0116", "0117", "0118", "0119", "011A", "011B", "011C", "011D",
            "011E", "011F", "0120", "0121", "0122", "0123", "0124", "0125", "0126", "0127",
            "0128", "0129", "012A", "012B", "012C", "012D", "012E", "012F", "0130", "0131",
            "0132", "0133", "0134", "0135", "0136", "0137", "0138", "0139", "013A", "013B",
            "013C", "013D", "013E", "013F", "0140", "0141", "0142", "0143", "0144", "0145",
            "0146", "0147", "0148", "0149", "014A", "014B", "014C", "014D", "014E", "014F"
        )
    }
    
    private fun getSampleEcuInfo(): List<EcuInfo> {
        return listOf(
            EcuInfo(
                name = "DME (Engine Control)",
                protocol = "ISO15765-4 (CAN)",
                address = "0x7E0",
                description = "Digital Motor Electronics"
            ),
            EcuInfo(
                name = "EGS (Transmission)",
                protocol = "ISO15765-4 (CAN)",
                address = "0x7E1",
                description = "Electronic Transmission Control"
            ),
            EcuInfo(
                name = "DSC (Stability Control)",
                protocol = "ISO15765-4 (CAN)",
                address = "0x7D0",
                description = "Dynamic Stability Control"
            )
        )
    }
    
    /**
     * Get a list of sample vehicles for the vehicle selection screen
     */
    fun getSampleVehicles(): List<VehicleInfo> {
        return listOf(
            VehicleInfo(
                make = "BMW",
                model = "330i",
                year = 2022,
                engineCode = "B48B20",
                transmissionType = "8-Speed Automatic",
                fuelType = "Gasoline",
                vin = "WBA8E5C58J1234567"
            ),
            VehicleInfo(
                make = "Toyota",
                model = "Camry",
                year = 2021,
                engineCode = "A25A-FKS",
                transmissionType = "8-Speed Automatic",
                fuelType = "Gasoline",
                vin = "4T1B11HK2MU123456"
            ),
            VehicleInfo(
                make = "Honda",
                model = "Civic",
                year = 2023,
                engineCode = "L15B7",
                transmissionType = "CVT",
                fuelType = "Gasoline",
                vin = "2HGFG4A58PH123456"
            )
        )
    }

    /**
     * Get a list of sample vehicle brands with models
     */
    fun getSampleVehicleBrands(): List<com.spacetec.vehicle.model.VehicleBrand> {
        return listOf(
            VehicleBrand(
                id = "bmw",
                name = "BMW",
                country = "Germany",
                yearFounded = 1916,
                vehicleTypes = listOf(VehicleType.PASSENGER, VehicleType.LUXURY, VehicleType.PERFORMANCE),
                isPremium = true,
                supportedProtocols = listOf("OBD2", "BMW-ENET", "BMW-IBUS"),
                models = listOf(
                    createBmwModel("330i", 2019..2023, "B48B20", 255, 400),
                    createBmwModel("M340i", 2019..2023, "B58B30", 382, 500),
                    createBmwModel("M3", 2021..2023, "S58B30", 473, 650)
                )
            ),
            VehicleBrand(
                id = "toyota",
                name = "Toyota",
                country = "Japan",
                yearFounded = 1937,
                vehicleTypes = listOf(VehicleType.PASSENGER, VehicleType.HYBRID),
                isPremium = false,
                supportedProtocols = listOf("OBD2", "Toyota"),
                models = listOf(
                    createToyotaModel("Camry", 2018..2023, "A25A-FKS", 203, 250),
                    createToyotaModel("RAV4", 2019..2023, "A25A-FKS", 203, 250),
                    createToyotaModel("Prius", 2016..2023, "2ZR-FXE", 121, 142, isHybrid = true)
                )
            )
        )
    }
    
    private fun createBmwModel(
        name: String,
        years: IntRange,
        engineCode: String,
        powerHp: Int,
        torqueNm: Int
    ): VehicleModel {
        return VehicleModel(
            id = "bmw_${name.lowercase()}",
            brandId = "bmw",
            name = name,
            generation = "G20",
            productionYears = years,
            bodyType = when (name) {
                "M3" -> BodyType.SEDAN
                else -> BodyType.SEDAN
            },
            engineOptions = listOf(
                EngineOption(
                    code = engineCode,
                    name = "${engineCode} Turbo",
                    displacement = when (engineCode[1]) {
                        '4' -> 2.0f
                        '5' -> 3.0f
                        else -> 2.0f
                    },
                    powerHp = powerHp,
                    torqueNm = torqueNm,
                    fuelType = FuelType.GASOLINE,
                    isHybrid = false
                )
            ),
            transmissionTypes = listOf(TransmissionType.AUTOMATIC, TransmissionType.MANUAL),
            driveType = when (name) {
                "M3" -> DriveType.RWD
                else -> DriveType.RWD
            },
            fuelType = FuelType.GASOLINE,
            imageUrl = "https://example.com/bmw_${name.lowercase()}.jpg",
            description = "BMW $name with ${engineCode} engine",
            features = listOf(
                "iDrive 7.0",
                "Adaptive Suspension",
                "Heated Seats",
                "Apple CarPlay",
                "Android Auto"
            )
        )
    }
    
    private fun createToyotaModel(
        name: String,
        years: IntRange,
        engineCode: String,
        powerHp: Int,
        torqueNm: Int,
        isHybrid: Boolean = false
    ): VehicleModel {
        return VehicleModel(
            id = "toyota_${name.lowercase()}",
            brandId = "toyota",
            name = name,
            generation = when (years.first) {
                in 2016..2018 -> "XV70"
                else -> "XV80"
            },
            productionYears = years,
            bodyType = when (name) {
                "Camry" -> BodyType.SEDAN
                "RAV4" -> BodyType.SUV
                "Prius" -> BodyType.HATCHBACK
                else -> BodyType.SEDAN
            },
            engineOptions = listOf(
                EngineOption(
                    code = engineCode,
                    name = engineCode,
                    displacement = when (engineCode[0]) {
                        '2' -> 2.0f
                        '3' -> 2.5f
                        else -> 2.0f
                    },
                    powerHp = powerHp,
                    torqueNm = torqueNm,
                    fuelType = if (isHybrid) FuelType.HYBRID else FuelType.GASOLINE,
                    isHybrid = isHybrid
                )
            ),
            transmissionTypes = listOf(TransmissionType.AUTOMATIC, TransmissionType.CVT),
            driveType = DriveType.FWD,
            fuelType = if (isHybrid) FuelType.HYBRID else FuelType.GASOLINE,
            imageUrl = "https://example.com/toyota_${name.lowercase()}.jpg",
            description = "Toyota $name ${if (isHybrid) "Hybrid" else ""}",
            features = listOf(
                "Toyota Safety Sense",
                "Bluetooth",
                "Backup Camera",
                if (isHybrid) "Regenerative Braking" else "",
                if (isHybrid) "EV Mode" else ""
            ).filter { it.isNotBlank() }
        )
    }
}

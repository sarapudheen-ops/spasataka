package com.spacetec.vehicle.model

/**
 * Represents a specific vehicle model from a manufacturer
 */
data class VehicleModel(
    val id: String,
    val brandId: String,
    val name: String,
    val generation: String,
    val productionYears: IntRange,
    val bodyType: BodyType,
    val engineOptions: List<EngineOption>,
    val transmissionTypes: List<TransmissionType>,
    val driveType: DriveType,
    val fuelType: FuelType,
    val imageUrl: String? = null,
    val description: String = "",
    val features: List<String> = emptyList(),
    val ecuInfo: List<EcuInfo> = emptyList()
)

enum class BodyType {
    SEDAN, COUPE, HATCHBACK, SUV, CROSSOVER, 
    PICKUP, VAN, MINIVAN, WAGON, CONVERTIBLE,
    ROADSTER, SUPERCAR, HYPERCAR, TRUCK, BUS
}

data class EngineOption(
    val code: String,
    val name: String,
    val displacement: Float, // in liters
    val powerHp: Int,
    val torqueNm: Int,
    val fuelType: FuelType,
    val isHybrid: Boolean = false,
    val isElectric: Boolean = false
)

enum class TransmissionType {
    MANUAL, AUTOMATIC, SEMI_AUTOMATIC, CVT, DCT, SEQUENTIAL
}

enum class DriveType {
    FWD, RWD, AWD, FOUR_BY_FOUR
}

enum class FuelType {
    GASOLINE, DIESEL, ELECTRIC, HYBRID, PLUGIN_HYBRID, LPG, CNG, HYDROGEN
}

data class EcuInfo(
    val name: String,
    val protocol: String,
    val address: String,
    val description: String = ""
)

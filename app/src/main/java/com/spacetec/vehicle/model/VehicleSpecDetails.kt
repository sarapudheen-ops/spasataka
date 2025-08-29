package com.spacetec.vehicle.model

/**
 * Detailed vehicle specifications
 */
data class VehicleSpecDetails(
    val engineSpecs: EngineSpecs? = null,
    val transmissionSpecs: TransmissionSpecs? = null,
    val performanceSpecs: PerformanceSpecs? = null,
    val dimensionSpecs: DimensionSpecs? = null,
    val capacitySpecs: CapacitySpecs? = null,
    val weightSpecs: WeightSpecs? = null,
    val electricalSpecs: ElectricalSpecs? = null
)

data class EngineSpecs(
    val type: String,
    val displacement: String,
    val power: String,
    val torque: String,
    val fuelSystem: String = "Unknown"
)

data class TransmissionSpecs(
    val type: String,
    val gears: Int,
    val driveType: String
)

data class PerformanceSpecs(
    val acceleration: String = "Unknown",
    val topSpeed: String = "Unknown",
    val fuelEconomy: String = "Unknown"
)

data class DimensionSpecs(
    val length: String = "Unknown",
    val width: String = "Unknown",
    val height: String = "Unknown",
    val wheelbase: String = "Unknown"
)

data class CapacitySpecs(
    val seatingCapacity: Int = 0,
    val cargoCapacity: String = "Unknown",
    val fuelTankCapacity: String = "Unknown"
)

data class WeightSpecs(
    val curbWeight: String = "Unknown",
    val grossWeight: String = "Unknown"
)

data class ElectricalSpecs(
    val batteryVoltage: String = "12V",
    val alternatorOutput: String = "Unknown",
    val starterPower: String = "Unknown"
)

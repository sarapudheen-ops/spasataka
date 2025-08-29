package com.spacetec.vehicle.library.models

// VehicleBrand is defined in com.spacetec.vehicle.model package

// VehicleModel is defined in com.spacetec.vehicle.model package

/**
 * Search criteria for finding vehicles
 */
data class VehicleSearchCriteria(
    val brand: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val fuelType: List<String> = emptyList(),
    val market: List<String> = emptyList(),
    val bodyType: List<String> = emptyList()
)

/**
 * DTC (Diagnostic Trouble Code) information
 */
data class DtcInfo(
    val code: String,
    val description: String,
    val severity: String = "Medium",
    val possibleCauses: List<String> = emptyList(),
    val symptoms: List<String> = emptyList(),
    val solutions: List<String> = emptyList(),
    val relatedDtcs: List<String> = emptyList(),
    val affectedSystems: List<String> = emptyList()
)

/**
 * Service or maintenance procedure for a vehicle
 */
data class ServiceProcedure(
    val id: String,
    val name: String,
    val description: String,
    val intervalKm: Int? = null,
    val intervalMonths: Int? = null,
    val difficulty: Int = 3, // 1-5, 5 being most difficult
    val estimatedTimeMinutes: Int = 60,
    val toolsRequired: List<String> = emptyList(),
    val steps: List<ServiceStep> = emptyList(),
    val safetyWarnings: List<String> = emptyList()
)

/**
 * Individual step in a service procedure
 */
data class ServiceStep(
    val number: Int,
    val instruction: String,
    val imageUrl: String? = null,
    val torqueSpec: String? = null,
    val notes: String = ""
)

/**
 * Vehicle maintenance record
 */
data class MaintenanceRecord(
    val id: String,
    val vehicleId: String,
    val date: Long,
    val odometerKm: Int,
    val servicePerformed: String,
    val serviceCenter: String = "",
    val cost: Double? = null,
    val receiptImageUrl: String? = null,
    val notes: String = "",
    val nextServiceKm: Int? = null,
    val nextServiceDate: Long? = null
)

/**
 * Vehicle recall information
 */
data class RecallInfo(
    val recallId: String,
    val nhtsaId: String? = null,
    val dateReported: Long,
    val component: String,
    val description: String,
    val consequence: String,
    val remedy: String,
    val affectedVehicles: List<String> = emptyList(),
    val status: String = "Open"
)

/**
 * Vehicle specification details
 */
data class VehicleSpecDetails(
    val engine: EngineSpecs,
    val transmission: TransmissionSpecs,
    val performance: PerformanceSpecs,
    val dimensions: DimensionSpecs,
    val capacities: CapacitySpecs,
    val weights: WeightSpecs,
    val electrical: ElectricalSpecs
)

data class EngineSpecs(
    val code: String,
    val type: String,
    val displacementCc: Int,
    val cylinderLayout: String,
    val valvesPerCylinder: Int,
    val fuelSystem: String,
    val fuelType: String,
    val powerKw: Int? = null,
    val powerHp: Int? = null,
    val torqueNm: Int? = null,
    val compressionRatio: String? = null
)

data class TransmissionSpecs(
    val type: String,
    val gears: Int,
    val driveType: String,
    val finalDriveRatio: String? = null
)

data class PerformanceSpecs(
    val topSpeedKph: Int? = null,
    val acceleration0To100Kph: Double? = null,
    val fuelConsumptionUrban: Double? = null,
    val fuelConsumptionExtraUrban: Double? = null,
    val fuelConsumptionCombined: Double? = null,
    val co2Emissions: Int? = null,
    val emissionStandard: String? = null
)

data class DimensionSpecs(
    val lengthMm: Int,
    val widthMm: Int,
    val heightMm: Int,
    val wheelbaseMm: Int,
    val frontTrackMm: Int? = null,
    val rearTrackMm: Int? = null,
    val groundClearanceMm: Int? = null,
    val cargoVolumeL: Int? = null
)

data class CapacitySpecs(
    val fuelTankL: Double,
    val engineOilL: Double,
    val coolingSystemL: Double? = null,
    val transmissionOilL: Double? = null,
    val brakeFluidMl: Int? = null
)

data class WeightSpecs(
    val curbWeightKg: Int,
    val grossWeightKg: Int? = null,
    val maxTowWeightKg: Int? = null,
    val maxRoofLoadKg: Int? = null
)

data class ElectricalSpecs(
    val batteryAh: Int? = null,
    val batteryVoltage: Int = 12,
    val alternatorOutputA: Int? = null,
    val fuseBoxLocations: List<String> = emptyList()
)

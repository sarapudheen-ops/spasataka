package com.spacetec.vehicle.model

/**
 * Detailed technical specifications for a vehicle
 */
data class VehicleSpecification(
    val dimensions: Dimensions,
    val weight: Weight,
    val performance: Performance,
    val fuelEconomy: FuelEconomy,
    val safety: SafetyFeatures,
    val emissions: EmissionsData,
    val serviceIntervals: ServiceIntervals
)

data class Dimensions(
    val length: Float, // mm
    val width: Float,  // mm
    val height: Float, // mm
    val wheelbase: Float, // mm
    val groundClearance: Float // mm
)

data class Weight(
    val curbWeight: Int, // kg
    val grossWeight: Int, // kg
    val maxPayload: Int // kg
)

data class Performance(
    val topSpeed: Int, // km/h
    val acceleration0to100: Float, // seconds
    val brakingDistance: Int // meters from 100-0 km/h
)

data class FuelEconomy(
    val urban: Float,    // l/100km or kWh/100km
    val extraUrban: Float,
    val combined: Float,
    val co2Emissions: Int // g/km
)

data class SafetyFeatures(
    val airbags: Int,
    val hasAbs: Boolean,
    val hasEsp: Boolean,
    val hasLaneAssist: Boolean,
    val hasAdaptiveCruise: Boolean,
    val ncapRating: Int? // 1-5 stars or null if not tested
)

data class EmissionsData(
    val euroStandard: Int,
    val nox: Float, // g/km
    val particulates: Float, // g/km
    val co: Float, // g/km
    val hc: Float // g/km
)

data class ServiceIntervals(
    val oilChangeKm: Int,
    val oilChangeMonths: Int,
    val inspectionKm: Int,
    val inspectionMonths: Int,
    val timingBeltKm: Int? = null,
    val timingBeltMonths: Int? = null
)

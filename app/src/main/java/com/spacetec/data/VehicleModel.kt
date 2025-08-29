package com.spacetec.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val make: String,
    val model: String,
    val year: Int? = null,
    val engineType: String? = null,
    val fuelType: String? = null,
    val transmission: String? = null,
    val vin: String? = null
)

data class VehicleMake(
    val name: String,
    val models: List<String>
)

data class VehicleDatabase(
    val makes: Map<String, List<String>>
)

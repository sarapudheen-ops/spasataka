package com.spacetec.diagnostic.obd

import kotlin.math.roundToInt

object PidCalculator {

    fun calculate(pid: String, rawData: ByteArray): String {
        return when (pid.uppercase()) {
            "0C" -> { // Engine RPM
                val value = ((rawData[0].toInt() and 0xFF) * 256 + (rawData[1].toInt() and 0xFF)) / 4
                "$value RPM (Thruster Power)"
            }
            "0D" -> { // Vehicle Speed
                val value = rawData[0].toInt() and 0xFF
                "$value km/h (Warp Speed)"
            }
            "05" -> { // Coolant Temp
                val value = (rawData[0].toInt() and 0xFF) - 40
                "$value °C (Engine Core Temp)"
            }
            "2F" -> { // Fuel Level
                val value = ((rawData[0].toInt() and 0xFF) * 100) / 255
                "$value% (Oxygen Levels)"
            }
            "0F" -> { // Intake Air Temperature
                val value = (rawData[0].toInt() and 0xFF) - 40
                "$value °C (Intake Air Temp)"
            }
            "11" -> { // Throttle Position
                val value = ((rawData[0].toInt() and 0xFF) * 100) / 255
                "$value% (Throttle Position)"
            }
            "04" -> { // Engine Load
                val value = ((rawData[0].toInt() and 0xFF) * 100) / 255
                "$value% (Engine Load)"
            }
            "42" -> { // Control Module Voltage
                val value = ((rawData[0].toInt() and 0xFF) * 256 + (rawData[1].toInt() and 0xFF)) / 1000.0
                "${String.format("%.2f", value)}V (System Voltage)"
            }
            "43" -> { // Absolute Load Value
                val value = ((rawData[0].toInt() and 0xFF) * 256 + (rawData[1].toInt() and 0xFF)) * 100 / 255
                "$value% (Absolute Load)"
            }
            "44" -> { // Fuel/Air Commanded Equivalence Ratio
                val value = ((rawData[0].toInt() and 0xFF) * 256 + (rawData[1].toInt() and 0xFF)) / 32768.0
                "${String.format("%.3f", value)} (Air/Fuel Ratio)"
            }
            "45" -> { // Relative Throttle Position
                val value = ((rawData[0].toInt() and 0xFF) * 100) / 255
                "$value% (Relative Throttle)"
            }
            "46" -> { // Ambient Air Temperature
                val value = (rawData[0].toInt() and 0xFF) - 40
                "$value °C (Ambient Air Temp)"
            }
            "47" -> { // Absolute Throttle Position B
                val value = ((rawData[0].toInt() and 0xFF) * 100) / 255
                "$value% (Abs Throttle B)"
            }
            "48" -> { // Absolute Throttle Position C
                val value = ((rawData[0].toInt() and 0xFF) * 100) / 255
                "$value% (Abs Throttle C)"
            }
            "49" -> { // Accelerator Pedal Position D
                val value = ((rawData[0].toInt() and 0xFF) * 100) / 255
                "$value% (Accelerator Pos D)"
            }
            "4A" -> { // Accelerator Pedal Position E
                val value = ((rawData[0].toInt() and 0xFF) * 100) / 255
                "$value% (Accelerator Pos E)"
            }
            "4B" -> { // Accelerator Pedal Position F
                val value = ((rawData[0].toInt() and 0xFF) * 100) / 255
                "$value% (Accelerator Pos F)"
            }
            "4C" -> { // Commanded Throttle Actuator
                val value = ((rawData[0].toInt() and 0xFF) * 100) / 255
                "$value% (Commanded Throttle)"
            }
            else -> rawData.joinToString(" ") { "%02X".format(it) }
        }
    }

    fun calculateWithFormula(formula: String, rawData: ByteArray): Double {
        return try {
            val A = if (rawData.isNotEmpty()) rawData[0].toInt() and 0xFF else 0
            val B = if (rawData.size > 1) rawData[1].toInt() and 0xFF else 0
            val C = if (rawData.size > 2) rawData[2].toInt() and 0xFF else 0
            val D = if (rawData.size > 3) rawData[3].toInt() and 0xFF else 0

            // Simple formula parser for common OBD formulas
            when {
                formula.contains("((A*256)+B)/4") -> ((A * 256) + B) / 4.0
                formula.contains("A-40") -> (A - 40).toDouble()
                formula.contains("(A*100)/255") -> (A * 100) / 255.0
                formula.contains("((A*256)+B)/1000") -> ((A * 256) + B) / 1000.0
                formula.contains("A") -> A.toDouble()
                else -> 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }

    fun getSpaceThemedName(pidName: String): String {
        return when {
            pidName.contains("RPM", ignoreCase = true) -> "Thruster Power"
            pidName.contains("Speed", ignoreCase = true) -> "Warp Speed"
            pidName.contains("Coolant", ignoreCase = true) -> "Engine Core Temperature"
            pidName.contains("Fuel", ignoreCase = true) -> "Oxygen Levels"
            pidName.contains("Temperature", ignoreCase = true) -> "Thermal Reading"
            pidName.contains("Throttle", ignoreCase = true) -> "Thrust Control"
            pidName.contains("Load", ignoreCase = true) -> "Engine Load"
            pidName.contains("Voltage", ignoreCase = true) -> "Power Systems"
            else -> pidName
        }
    }
}

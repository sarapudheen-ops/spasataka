package com.spacetec.diagnostic.data

import kotlin.math.*

/**
 * Comprehensive OBD-II PID Database
 * Contains all standard PIDs with accurate formulas, units, and ranges
 */
object PidDatabase {
    
    data class Pid(
        val id: String,
        val name: String,
        val description: String,
        val formula: (ByteArray) -> Double,
        val unit: String,
        val minValue: Double,
        val maxValue: Double,
        val decimalPlaces: Int = 2,
        val isPrimary: Boolean = false // Primary PIDs shown on main dashboard
    )
    
    val mode01Pids = mapOf(
        "00" to Pid(
            "00", "Supported PIDs [01-20]", "Bit encoded PIDs supported",
            { data -> data.toUInt32() },
            "", 0.0, 4294967295.0, 0
        ),
        
        "01" to Pid(
            "01", "Monitor status", "MIL status and DTC count",
            { data -> data[0].toDouble() },
            "", 0.0, 255.0, 0
        ),
        
        "03" to Pid(
            "03", "Fuel system status", "Fuel system control status",
            { data -> 
                val bank1 = data[0].toInt()
                val bank2 = if (data.size > 1) data[1].toInt() else 0
                // Return bank1 status, bank2 available via separate method
                bank1.toDouble()
            },
            "", 0.0, 255.0, 0
        ),
        
        "04" to Pid(
            "04", "Calculated engine load", "Indicates engine load percentage",
            { data -> data[0].toDouble() * 100.0 / 255.0 },
            "%", 0.0, 100.0, 1, isPrimary = true
        ),
        
        "05" to Pid(
            "05", "Engine coolant temperature", "Engine coolant temperature",
            { data -> data[0].toDouble() - 40.0 },
            "°C", -40.0, 215.0, 0, isPrimary = true
        ),
        
        "06" to Pid(
            "06", "Short term fuel trim (Bank 1)", "Short term fuel correction",
            { data -> (data[0].toDouble() - 128.0) * 100.0 / 128.0 },
            "%", -100.0, 99.2, 1
        ),
        
        "07" to Pid(
            "07", "Long term fuel trim (Bank 1)", "Long term fuel correction",
            { data -> (data[0].toDouble() - 128.0) * 100.0 / 128.0 },
            "%", -100.0, 99.2, 1
        ),
        
        "08" to Pid(
            "08", "Short term fuel trim (Bank 2)", "Short term fuel correction bank 2",
            { data -> (data[0].toDouble() - 128.0) * 100.0 / 128.0 },
            "%", -100.0, 99.2, 1
        ),
        
        "09" to Pid(
            "09", "Long term fuel trim (Bank 2)", "Long term fuel correction bank 2",
            { data -> (data[0].toDouble() - 128.0) * 100.0 / 128.0 },
            "%", -100.0, 99.2, 1
        ),
        
        "0A" to Pid(
            "0A", "Fuel pressure", "Fuel rail pressure (gauge)",
            { data -> data[0].toDouble() * 3.0 },
            "kPa", 0.0, 765.0, 0
        ),
        
        "0B" to Pid(
            "0B", "Intake manifold pressure", "Intake manifold absolute pressure",
            { data -> data[0].toDouble() },
            "kPa", 0.0, 255.0, 0
        ),
        
        "0C" to Pid(
            "0C", "Engine speed", "Engine RPM",
            { data -> ((data[0].toInt() * 256) + data[1].toInt()).toDouble() / 4.0 },
            "rpm", 0.0, 16383.75, 0, isPrimary = true
        ),
        
        "0D" to Pid(
            "0D", "Vehicle speed", "Vehicle speed sensor",
            { data -> data[0].toDouble() },
            "km/h", 0.0, 255.0, 0, isPrimary = true
        ),
        
        "0E" to Pid(
            "0E", "Timing advance", "Ignition timing advance",
            { data -> data[0].toDouble() / 2.0 - 64.0 },
            "°", -64.0, 63.5, 1
        ),
        
        "0F" to Pid(
            "0F", "Intake air temperature", "Intake air temperature",
            { data -> data[0].toDouble() - 40.0 },
            "°C", -40.0, 215.0, 0
        ),
        
        "10" to Pid(
            "10", "Mass air flow rate", "MAF sensor reading",
            { data -> ((data[0].toInt() * 256) + data[1].toInt()).toDouble() / 100.0 },
            "g/s", 0.0, 655.35, 2
        ),
        
        "11" to Pid(
            "11", "Throttle position", "Absolute throttle position",
            { data -> data[0].toDouble() * 100.0 / 255.0 },
            "%", 0.0, 100.0, 1, isPrimary = true
        ),
        
        "12" to Pid(
            "12", "Commanded secondary air status", "Secondary air system status",
            { data -> data[0].toDouble() },
            "", 0.0, 255.0, 0
        ),
        
        "13" to Pid(
            "13", "Oxygen sensors present", "Bit encoded O2 sensors present",
            { data -> data[0].toDouble() },
            "", 0.0, 255.0, 0
        ),
        
        "14" to Pid(
            "14", "O2 Sensor 1", "Bank 1, Sensor 1: Voltage & fuel trim",
            { data -> 
                val voltage = data[0].toDouble() / 200.0
                voltage // Short term fuel trim in data[1] if needed
            },
            "V", 0.0, 1.275, 3
        ),
        
        "15" to Pid(
            "15", "O2 Sensor 2", "Bank 1, Sensor 2: Voltage & fuel trim",
            { data -> data[0].toDouble() / 200.0 },
            "V", 0.0, 1.275, 3
        ),
        
        "16" to Pid(
            "16", "O2 Sensor 3", "Bank 1, Sensor 3: Voltage & fuel trim",
            { data -> data[0].toDouble() / 200.0 },
            "V", 0.0, 1.275, 3
        ),
        
        "17" to Pid(
            "17", "O2 Sensor 4", "Bank 1, Sensor 4: Voltage & fuel trim",
            { data -> data[0].toDouble() / 200.0 },
            "V", 0.0, 1.275, 3
        ),
        
        "18" to Pid(
            "18", "O2 Sensor 5", "Bank 2, Sensor 1: Voltage & fuel trim",
            { data -> data[0].toDouble() / 200.0 },
            "V", 0.0, 1.275, 3
        ),
        
        "19" to Pid(
            "19", "O2 Sensor 6", "Bank 2, Sensor 2: Voltage & fuel trim",
            { data -> data[0].toDouble() / 200.0 },
            "V", 0.0, 1.275, 3
        ),
        
        "1A" to Pid(
            "1A", "O2 Sensor 7", "Bank 2, Sensor 3: Voltage & fuel trim",
            { data -> data[0].toDouble() / 200.0 },
            "V", 0.0, 1.275, 3
        ),
        
        "1B" to Pid(
            "1B", "O2 Sensor 8", "Bank 2, Sensor 4: Voltage & fuel trim",
            { data -> data[0].toDouble() / 200.0 },
            "V", 0.0, 1.275, 3
        ),
        
        "1C" to Pid(
            "1C", "OBD standards", "OBD standards this vehicle conforms to",
            { data -> data[0].toDouble() },
            "", 0.0, 255.0, 0
        ),
        
        "1D" to Pid(
            "1D", "Oxygen sensors present", "Oxygen sensors present in 4 banks",
            { data -> data[0].toDouble() },
            "", 0.0, 255.0, 0
        ),
        
        "1E" to Pid(
            "1E", "Auxiliary input status", "Power take off status",
            { data -> data[0].toDouble() },
            "", 0.0, 255.0, 0
        ),
        
        "1F" to Pid(
            "1F", "Run time since engine start", "Time since engine start",
            { data -> ((data[0].toInt() * 256) + data[1].toInt()).toDouble() },
            "s", 0.0, 65535.0, 0
        ),
        
        "21" to Pid(
            "21", "Distance with MIL on", "Distance traveled with MIL on",
            { data -> ((data[0].toInt() * 256) + data[1].toInt()).toDouble() },
            "km", 0.0, 65535.0, 0
        ),
        
        "22" to Pid(
            "22", "Fuel rail pressure", "Fuel rail pressure (relative to manifold)",
            { data -> ((data[0].toInt() * 256) + data[1].toInt()).toDouble() * 0.079 },
            "kPa", 0.0, 5177.265, 1
        ),
        
        "23" to Pid(
            "23", "Fuel rail gauge pressure", "Fuel rail gauge pressure (diesel, gas direct inject)",
            { data -> ((data[0].toInt() * 256) + data[1].toInt()).toDouble() * 10.0 },
            "kPa", 0.0, 655350.0, 0
        ),
        
        "24" to Pid(
            "24", "Lambda Sensor 1", "Bank 1, Sensor 1: Lambda & voltage",
            { data -> 
                val lambda = ((data[0].toInt() * 256) + data[1].toInt()).toDouble() / 32768.0
                lambda
            },
            "λ", 0.0, 1.999, 3
        ),
        
        "2C" to Pid(
            "2C", "Commanded EGR", "Commanded EGR percentage",
            { data -> data[0].toDouble() * 100.0 / 255.0 },
            "%", 0.0, 100.0, 1
        ),
        
        "2D" to Pid(
            "2D", "EGR Error", "EGR system error",
            { data -> (data[0].toDouble() - 128.0) * 100.0 / 128.0 },
            "%", -100.0, 99.2, 1
        ),
        
        "2E" to Pid(
            "2E", "Commanded evap purge", "Commanded evaporative purge",
            { data -> data[0].toDouble() * 100.0 / 255.0 },
            "%", 0.0, 100.0, 1
        ),
        
        "2F" to Pid(
            "2F", "Fuel tank level", "Fuel tank level input",
            { data -> data[0].toDouble() * 100.0 / 255.0 },
            "%", 0.0, 100.0, 1, isPrimary = true
        ),
        
        "30" to Pid(
            "30", "Warm-ups since DTCs cleared", "Number of warm-ups since codes cleared",
            { data -> data[0].toDouble() },
            "", 0.0, 255.0, 0
        ),
        
        "31" to Pid(
            "31", "Distance since DTCs cleared", "Distance traveled since codes cleared",
            { data -> ((data[0].toInt() * 256) + data[1].toInt()).toDouble() },
            "km", 0.0, 65535.0, 0
        ),
        
        "32" to Pid(
            "32", "Evap system pressure", "Evaporative system vapor pressure",
            { data -> 
                val raw = (data[0].toInt() * 256) + data[1].toInt()
                (raw - 32768).toDouble() / 4.0
            },
            "Pa", -8192.0, 8191.75, 2
        ),
        
        "33" to Pid(
            "33", "Absolute barometric pressure", "Absolute barometric pressure",
            { data -> data[0].toDouble() },
            "kPa", 0.0, 255.0, 0
        ),
        
        "34" to Pid(
            "34", "Lambda Sensor 1 Current", "Bank 1, Sensor 1: Lambda & current",
            { data -> 
                val lambda = ((data[0].toInt() * 256) + data[1].toInt()).toDouble() / 32768.0
                lambda
            },
            "λ", 0.0, 1.999, 3
        ),
        
        "3C" to Pid(
            "3C", "Catalyst temperature B1S1", "Catalyst temperature: Bank 1, Sensor 1",
            { data -> ((data[0].toInt() * 256) + data[1].toInt()).toDouble() / 10.0 - 40.0 },
            "°C", -40.0, 6513.5, 1
        ),
        
        "42" to Pid(
            "42", "Control module voltage", "ECU voltage",
            { data -> ((data[0].toInt() * 256) + data[1].toInt()).toDouble() / 1000.0 },
            "V", 0.0, 65.535, 3, isPrimary = true
        ),
        
        "43" to Pid(
            "43", "Absolute load value", "Absolute engine load value",
            { data -> ((data[0].toInt() * 256) + data[1].toInt()).toDouble() * 100.0 / 255.0 },
            "%", 0.0, 25700.0, 1
        ),
        
        "44" to Pid(
            "44", "Lambda commanded", "Commanded lambda",
            { data -> ((data[0].toInt() * 256) + data[1].toInt()).toDouble() / 32768.0 },
            "λ", 0.0, 1.999, 3
        ),
        
        "45" to Pid(
            "45", "Relative throttle position", "Relative throttle position",
            { data -> data[0].toDouble() * 100.0 / 255.0 },
            "%", 0.0, 100.0, 1
        ),
        
        "46" to Pid(
            "46", "Ambient air temperature", "Ambient air temperature",
            { data -> data[0].toDouble() - 40.0 },
            "°C", -40.0, 215.0, 0
        ),
        
        "47" to Pid(
            "47", "Absolute throttle position B", "Absolute throttle position B",
            { data -> data[0].toDouble() * 100.0 / 255.0 },
            "%", 0.0, 100.0, 1
        ),
        
        "49" to Pid(
            "49", "Accelerator pedal position D", "Accelerator pedal position D",
            { data -> data[0].toDouble() * 100.0 / 255.0 },
            "%", 0.0, 100.0, 1
        ),
        
        "4A" to Pid(
            "4A", "Accelerator pedal position E", "Accelerator pedal position E",
            { data -> data[0].toDouble() * 100.0 / 255.0 },
            "%", 0.0, 100.0, 1
        ),
        
        "4B" to Pid(
            "4B", "Accelerator pedal position F", "Accelerator pedal position F",
            { data -> data[0].toDouble() * 100.0 / 255.0 },
            "%", 0.0, 100.0, 1
        ),
        
        "4C" to Pid(
            "4C", "Commanded throttle actuator", "Commanded throttle actuator control",
            { data -> data[0].toDouble() * 100.0 / 255.0 },
            "%", 0.0, 100.0, 1
        ),
        
        "4D" to Pid(
            "4D", "Time run with MIL on", "Time run with MIL on",
            { data -> ((data[0].toInt() * 256) + data[1].toInt()).toDouble() },
            "min", 0.0, 65535.0, 0
        ),
        
        "4E" to Pid(
            "4E", "Time since DTCs cleared", "Time since trouble codes cleared",
            { data -> ((data[0].toInt() * 256) + data[1].toInt()).toDouble() },
            "min", 0.0, 65535.0, 0
        ),
        
        "5C" to Pid(
            "5C", "Engine oil temperature", "Engine oil temperature",
            { data -> data[0].toDouble() - 40.0 },
            "°C", -40.0, 210.0, 0, isPrimary = true
        ),
        
        "5E" to Pid(
            "5E", "Engine fuel rate", "Engine fuel consumption rate",
            { data -> ((data[0].toInt() * 256) + data[1].toInt()).toDouble() / 20.0 },
            "L/h", 0.0, 3212.75, 2
        )
    )
    
    // Helper extension functions
    private fun ByteArray.toUInt32(): Double {
        return ((this[0].toInt() and 0xFF) shl 24 or
                (this[1].toInt() and 0xFF) shl 16 or
                (this[2].toInt() and 0xFF) shl 8 or
                (this[3].toInt() and 0xFF)).toDouble()
    }
    
    /**
     * Get human-readable fuel system status
     */
    fun getFuelSystemStatus(code: Int): String {
        return when (code) {
            1 -> "Open loop - insufficient engine temperature"
            2 -> "Closed loop, using O2 sensor(s)"
            4 -> "Open loop - engine load or fuel cut"
            8 -> "Open loop - system failure"
            16 -> "Closed loop, using at least one O2 sensor but fault detected"
            else -> "Unknown status ($code)"
        }
    }
    
    /**
     * Get OBD standard description
     */
    fun getObdStandard(code: Int): String {
        return when (code) {
            1 -> "OBD-II as defined by CARB"
            2 -> "OBD as defined by EPA"
            3 -> "OBD and OBD-II"
            4 -> "OBD-I"
            5 -> "Not OBD compliant"
            6 -> "EOBD (Europe)"
            7 -> "EOBD and OBD-II"
            8 -> "EOBD and OBD"
            9 -> "EOBD, OBD and OBD-II"
            10 -> "JOBD (Japan)"
            11 -> "JOBD and OBD-II"
            12 -> "JOBD and EOBD"
            13 -> "JOBD, EOBD, and OBD-II"
            14 -> "Reserved"
            15 -> "Reserved"
            16 -> "Reserved"
            17 -> "Engine Manufacturer Diagnostics (EMD)"
            18 -> "Engine Manufacturer Diagnostics Enhanced (EMD+)"
            19 -> "Heavy Duty On-Board Diagnostics (Child/Partial) (HD OBD-C)"
            20 -> "Heavy Duty On-Board Diagnostics (HD OBD)"
            21 -> "World Wide Harmonized OBD (WWH OBD)"
            22 -> "Reserved"
            23 -> "Heavy Duty Euro OBD Stage I without NOx control (HD EOBD-I)"
            24 -> "Heavy Duty Euro OBD Stage I with NOx control (HD EOBD-I N)"
            25 -> "Heavy Duty Euro OBD Stage II without NOx control (HD EOBD-II)"
            26 -> "Heavy Duty Euro OBD Stage II with NOx control (HD EOBD-II N)"
            27 -> "Reserved"
            28 -> "Brazil OBD Phase 1 (OBDBr-1)"
            29 -> "Brazil OBD Phase 2 (OBDBr-2)"
            30 -> "Korean OBD (KOBD)"
            31 -> "India OBD I (IOBD I)"
            32 -> "India OBD II (IOBD II)"
            33 -> "Heavy Duty Euro OBD Stage VI (HD EOBD-IV)"
            else -> "Unknown/Reserved ($code)"
        }
    }
}

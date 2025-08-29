package com.spacetec.data

/**
 * Data class containing diagnostic functions for different ECU systems
 */
data class EcuFunctions(
    val system: String,
    val functions: List<String>
) {
    companion object {
        /**
         * Get all available ECU systems and their functions
         */
        fun getAllSystemFunctions(): Map<String, List<String>> = mapOf(
            "Engine" to listOf(
                "Read DTCs",
                "Clear DTCs",
                "Live Data",
                "Actuation Tests",
                "Adaptations",
                "ECU Flash/Programming"
            ),
            "Transmission" to listOf(
                "Read DTCs",
                "Clear DTCs",
                "Live Data",
                "Adaptations"
            ),
            "ABS" to listOf(
                "Read DTCs",
                "Clear DTCs",
                "Actuation Tests",
                "Bleeding Procedures"
            ),
            "Airbag" to listOf(
                "Read DTCs",
                "Clear DTCs",
                "Crash Data Reset"
            ),
            "BCM" to listOf(
                "Coding",
                "Adaptations",
                "Key Learning"
            ),
            "Instrument Cluster" to listOf(
                "Odometer Sync",
                "Service Reset",
                "Coding"
            ),
            "HVAC" to listOf(
                "Read DTCs",
                "Clear DTCs",
                "Actuation Tests"
            ),
            "Immobilizer" to listOf(
                "Key Programming",
                "Immobilizer Reset",
                "Adaptations"
            )
        )
        
        /**
         * Get functions for a specific ECU system
         */
        fun getFunctionsForSystem(system: String): List<String> {
            return getAllSystemFunctions()[system] ?: emptyList()
        }
        
        /**
         * Get all available ECU systems
         */
        fun getAllSystems(): List<String> {
            return getAllSystemFunctions().keys.toList()
        }
        
        /**
         * Check if a function is available for a specific system
         */
        fun isFunctionAvailable(system: String, function: String): Boolean {
            return getAllSystemFunctions()[system]?.contains(function) ?: false
        }
        
        /**
         * Search functions across all systems
         */
        fun searchFunctions(query: String): Map<String, List<String>> {
            return getAllSystemFunctions().mapValues { (_, functions) ->
                functions.filter { it.contains(query, ignoreCase = true) }
            }.filterValues { it.isNotEmpty() }
        }
        
        /**
         * Get common functions across multiple systems
         */
        fun getCommonFunctions(): List<String> {
            val allFunctions = getAllSystemFunctions().values.flatten()
            return allFunctions.groupingBy { it }.eachCount()
                .filter { it.value > 1 }
                .keys.toList()
        }
    }
}

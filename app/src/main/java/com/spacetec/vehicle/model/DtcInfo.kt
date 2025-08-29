package com.spacetec.vehicle.model

/**
 * Diagnostic Trouble Code information
 */
data class DtcInfo(
    val code: String,
    val description: String,
    val severity: String = "Unknown",
    val system: String = "Generic",
    val possibleCauses: List<String> = emptyList(),
    val solutions: List<String> = emptyList()
)

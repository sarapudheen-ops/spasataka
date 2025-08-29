package com.spacetec.features

/**
 * Real-world repair guidance system
 */
object RepairGuidance {
    data class RepairTip(
        val code: String,
        val description: String,
        val commonCauses: List<String>,
        val repairSteps: List<String>,
        val severity: Int
    )

    /**
     * Get repair guidance for diagnostic trouble code
     */
    fun getGuidanceForDtc(dtc: String): RepairTip? {
        return when (dtc.uppercase()) {
            "P0171" -> RepairTip(
                code = "P0171",
                description = "System Too Lean (Bank 1)",
                commonCauses = listOf(
                    "Vacuum leak",
                    "Faulty MAF sensor",
                    "Clogged fuel injector",
                    "Weak fuel pump"
                ),
                repairSteps = listOf(
                    "Check for vacuum leaks",
                    "Inspect MAF sensor readings",
                    "Test fuel pressure",
                    "Check injector pulse width"
                ),
                severity = 3
            )
            "P0420" -> RepairTip(
                code = "P0420",
                description = "Catalyst System Efficiency Below Threshold",
                commonCauses = listOf(
                    "Failed catalytic converter",
                    "Oxygen sensor malfunction",
                    "Exhaust leak"
                ),
                repairSteps = listOf(
                    "Check pre-cat and post-cat O2 sensor readings",
                    "Inspect for exhaust leaks",
                    "Test catalytic converter efficiency"
                ),
                severity = 2
            )
            else -> null
        }
    }
}

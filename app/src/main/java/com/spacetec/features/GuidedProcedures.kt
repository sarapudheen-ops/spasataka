package com.spacetec.features

/**
 * Guided diagnostic procedures with step-by-step instructions
 */
class GuidedProcedures {
    data class ProcedureStep(
        val number: Int,
        val instruction: String,
        val imageResId: Int? = null,
        val verification: (() -> Boolean)? = null
    )

    /**
     * Get procedure steps for specified service
     */
    fun getProcedure(procedureType: String): List<ProcedureStep> {
        return when (procedureType.lowercase()) {
            "oil change" -> listOf(
                ProcedureStep(1, "Drain old engine oil", null),
                ProcedureStep(2, "Replace oil filter", null),
                ProcedureStep(3, "Add new engine oil", null),
                ProcedureStep(4, "Reset oil service indicator", null)
            )
            "battery replacement" -> listOf(
                ProcedureStep(1, "Disconnect negative terminal", null),
                ProcedureStep(2, "Disconnect positive terminal", null),
                ProcedureStep(3, "Remove old battery", null),
                ProcedureStep(4, "Install new battery", null),
                ProcedureStep(5, "Register new battery", null)
            )
            else -> emptyList()
        }
    }
}

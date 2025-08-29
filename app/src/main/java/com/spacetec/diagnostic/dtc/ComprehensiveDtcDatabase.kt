package com.spacetec.diagnostic.dtc

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Comprehensive DTC Database - Professional diagnostic trouble code management
 * Includes detailed descriptions, causes, and repair procedures
 */
class ComprehensiveDtcDatabase {
    private val _dtcDatabase = MutableStateFlow<Map<String, DtcInfo>>(emptyMap())
    val dtcDatabase: StateFlow<Map<String, DtcInfo>> = _dtcDatabase.asStateFlow()
    
    companion object {
        private const val TAG = "ComprehensiveDtcDatabase"
    }
    
    init {
        loadDtcDatabase()
    }
    
    /**
     * Get DTC information by code
     */
    fun getDtcInfo(code: String): DtcInfo? {
        val normalizedCode = code.uppercase().replace(" ", "")
        return _dtcDatabase.value[normalizedCode]
    }
    
    /**
     * Search DTCs by description or symptoms
     */
    fun searchDtcs(query: String): List<DtcInfo> {
        val searchQuery = query.lowercase()
        return _dtcDatabase.value.values.filter { dtc ->
            dtc.description.lowercase().contains(searchQuery) ||
            dtc.symptoms.any { it.lowercase().contains(searchQuery) } ||
            dtc.possibleCauses.any { it.lowercase().contains(searchQuery) }
        }
    }
    
    /**
     * Get DTCs by system
     */
    fun getDtcsBySystem(system: DtcSystem): List<DtcInfo> {
        return _dtcDatabase.value.values.filter { it.system == system }
    }
    
    /**
     * Get DTCs by severity
     */
    fun getDtcsBySeverity(severity: DtcSeverity): List<DtcInfo> {
        return _dtcDatabase.value.values.filter { it.severity == severity }
    }
    
    private fun loadDtcDatabase() {
        val dtcs = mutableMapOf<String, DtcInfo>()
        
        // Load engine DTCs
        dtcs.putAll(getEngineDtcs())
        
        // Load transmission DTCs
        dtcs.putAll(getTransmissionDtcs())
        
        // Load ABS/brake DTCs
        dtcs.putAll(getAbsDtcs())
        
        // Load airbag DTCs
        dtcs.putAll(getAirbagDtcs())
        
        // Load body control DTCs
        dtcs.putAll(getBodyControlDtcs())
        
        // Load manufacturer-specific DTCs
        dtcs.putAll(getManufacturerSpecificDtcs())
        
        _dtcDatabase.value = dtcs
        Log.i(TAG, "Loaded ${dtcs.size} DTCs into database")
    }
    
    private fun getEngineDtcs(): Map<String, DtcInfo> = mapOf(
        "P0100" to DtcInfo(
            code = "P0100",
            description = "Mass Air Flow Circuit Malfunction",
            system = DtcSystem.ENGINE,
            severity = DtcSeverity.MEDIUM,
            symptoms = listOf(
                "Check engine light on",
                "Poor fuel economy",
                "Rough idle",
                "Engine hesitation",
                "Black smoke from exhaust"
            ),
            possibleCauses = listOf(
                "Faulty MAF sensor",
                "Dirty MAF sensor",
                "Damaged air intake boot",
                "Clogged air filter",
                "Vacuum leak",
                "Faulty wiring to MAF sensor"
            ),
            repairProcedures = listOf(
                "Clean MAF sensor with MAF cleaner",
                "Check air filter condition",
                "Inspect air intake boot for cracks",
                "Test MAF sensor voltage",
                "Check for vacuum leaks",
                "Replace MAF sensor if faulty"
            ),
            estimatedRepairCost = "$150-$400",
            repairDifficulty = RepairDifficulty.EASY
        ),
        
        "P0171" to DtcInfo(
            code = "P0171",
            description = "System Too Lean (Bank 1)",
            system = DtcSystem.ENGINE,
            severity = DtcSeverity.MEDIUM,
            symptoms = listOf(
                "Check engine light on",
                "Poor fuel economy",
                "Engine runs rough",
                "Engine lacks power",
                "Engine may stall"
            ),
            possibleCauses = listOf(
                "Vacuum leak",
                "Faulty MAF sensor",
                "Clogged fuel injectors",
                "Weak fuel pump",
                "Faulty oxygen sensor",
                "Exhaust leak before O2 sensor"
            ),
            repairProcedures = listOf(
                "Check for vacuum leaks with smoke test",
                "Test fuel pressure",
                "Clean fuel injectors",
                "Test MAF sensor",
                "Check oxygen sensor operation",
                "Inspect exhaust system for leaks"
            ),
            estimatedRepairCost = "$200-$800",
            repairDifficulty = RepairDifficulty.MEDIUM
        ),
        
        "P0300" to DtcInfo(
            code = "P0300",
            description = "Random/Multiple Cylinder Misfire Detected",
            system = DtcSystem.ENGINE,
            severity = DtcSeverity.HIGH,
            symptoms = listOf(
                "Check engine light flashing",
                "Engine runs rough",
                "Loss of power",
                "Engine vibration",
                "Poor fuel economy"
            ),
            possibleCauses = listOf(
                "Faulty spark plugs",
                "Bad ignition coils",
                "Clogged fuel injectors",
                "Low compression",
                "Vacuum leak",
                "Faulty fuel pump"
            ),
            repairProcedures = listOf(
                "Replace spark plugs",
                "Test ignition coils",
                "Check fuel injector operation",
                "Perform compression test",
                "Check for vacuum leaks",
                "Test fuel pressure"
            ),
            estimatedRepairCost = "$300-$1200",
            repairDifficulty = RepairDifficulty.MEDIUM
        ),
        
        "P0420" to DtcInfo(
            code = "P0420",
            description = "Catalyst System Efficiency Below Threshold (Bank 1)",
            system = DtcSystem.ENGINE,
            severity = DtcSeverity.MEDIUM,
            symptoms = listOf(
                "Check engine light on",
                "Reduced fuel economy",
                "Failed emissions test",
                "Sulfur smell from exhaust"
            ),
            possibleCauses = listOf(
                "Faulty catalytic converter",
                "Faulty oxygen sensors",
                "Engine running rich or lean",
                "Exhaust leak",
                "Faulty fuel injectors"
            ),
            repairProcedures = listOf(
                "Test oxygen sensor operation",
                "Check catalytic converter efficiency",
                "Inspect exhaust system for leaks",
                "Test fuel trim values",
                "Replace catalytic converter if needed",
                "Replace oxygen sensors if faulty"
            ),
            estimatedRepairCost = "$500-$2000",
            repairDifficulty = RepairDifficulty.MEDIUM
        )
    )
    
    private fun getTransmissionDtcs(): Map<String, DtcInfo> = mapOf(
        "P0700" to DtcInfo(
            code = "P0700",
            description = "Transmission Control System Malfunction",
            system = DtcSystem.TRANSMISSION,
            severity = DtcSeverity.HIGH,
            symptoms = listOf(
                "Check engine light on",
                "Transmission shifts hard",
                "Transmission slipping",
                "No shifting",
                "Stuck in one gear"
            ),
            possibleCauses = listOf(
                "Internal transmission fault",
                "Faulty transmission control module",
                "Low transmission fluid",
                "Faulty shift solenoids",
                "Wiring issues"
            ),
            repairProcedures = listOf(
                "Check transmission fluid level",
                "Scan for additional transmission codes",
                "Test shift solenoids",
                "Check TCM operation",
                "Inspect wiring harness"
            ),
            estimatedRepairCost = "$500-$3000",
            repairDifficulty = RepairDifficulty.HARD
        ),
        
        "P0750" to DtcInfo(
            code = "P0750",
            description = "Shift Solenoid 'A' Malfunction",
            system = DtcSystem.TRANSMISSION,
            severity = DtcSeverity.MEDIUM,
            symptoms = listOf(
                "Harsh shifting",
                "No 1st or 2nd gear",
                "Transmission stuck in gear",
                "Check engine light on"
            ),
            possibleCauses = listOf(
                "Faulty shift solenoid A",
                "Dirty transmission fluid",
                "Internal transmission damage",
                "Wiring problems",
                "Faulty TCM"
            ),
            repairProcedures = listOf(
                "Replace shift solenoid A",
                "Change transmission fluid",
                "Test solenoid resistance",
                "Check wiring to solenoid",
                "Test TCM operation"
            ),
            estimatedRepairCost = "$300-$800",
            repairDifficulty = RepairDifficulty.MEDIUM
        )
    )
    
    private fun getAbsDtcs(): Map<String, DtcInfo> = mapOf(
        "C0035" to DtcInfo(
            code = "C0035",
            description = "Left Front Wheel Speed Sensor Circuit",
            system = DtcSystem.ABS,
            severity = DtcSeverity.MEDIUM,
            symptoms = listOf(
                "ABS light on",
                "ABS not functioning",
                "Traction control disabled",
                "Stability control disabled"
            ),
            possibleCauses = listOf(
                "Faulty wheel speed sensor",
                "Damaged sensor wiring",
                "Dirty or damaged tone ring",
                "Corroded connections",
                "Faulty ABS module"
            ),
            repairProcedures = listOf(
                "Test wheel speed sensor resistance",
                "Check sensor wiring for damage",
                "Inspect tone ring condition",
                "Clean sensor and mounting area",
                "Replace sensor if faulty"
            ),
            estimatedRepairCost = "$200-$500",
            repairDifficulty = RepairDifficulty.EASY
        )
    )
    
    private fun getAirbagDtcs(): Map<String, DtcInfo> = mapOf(
        "B0100" to DtcInfo(
            code = "B0100",
            description = "Driver Airbag Circuit Resistance High",
            system = DtcSystem.AIRBAG,
            severity = DtcSeverity.HIGH,
            symptoms = listOf(
                "Airbag light on",
                "Airbag system disabled",
                "SRS warning message"
            ),
            possibleCauses = listOf(
                "Faulty airbag module",
                "Open circuit in airbag wiring",
                "Faulty clock spring",
                "Corroded connections",
                "Faulty SRS module"
            ),
            repairProcedures = listOf(
                "Test airbag circuit resistance",
                "Check clock spring continuity",
                "Inspect airbag connections",
                "Test SRS module",
                "Replace faulty components"
            ),
            estimatedRepairCost = "$500-$1500",
            repairDifficulty = RepairDifficulty.HARD
        )
    )
    
    private fun getBodyControlDtcs(): Map<String, DtcInfo> = mapOf(
        "U0100" to DtcInfo(
            code = "U0100",
            description = "Lost Communication with ECM/PCM",
            system = DtcSystem.COMMUNICATION,
            severity = DtcSeverity.HIGH,
            symptoms = listOf(
                "Multiple warning lights",
                "Engine may not start",
                "Various systems not working",
                "Communication errors"
            ),
            possibleCauses = listOf(
                "Faulty ECM/PCM",
                "CAN bus wiring issues",
                "Blown fuses",
                "Poor connections",
                "Software corruption"
            ),
            repairProcedures = listOf(
                "Check ECM/PCM power and ground",
                "Test CAN bus continuity",
                "Check related fuses",
                "Inspect wiring harness",
                "Reprogram or replace ECM/PCM"
            ),
            estimatedRepairCost = "$800-$2500",
            repairDifficulty = RepairDifficulty.HARD
        )
    )
    
    private fun getManufacturerSpecificDtcs(): Map<String, DtcInfo> = mapOf(
        // VAG specific
        "01044" to DtcInfo(
            code = "01044",
            description = "Control Module Faulty",
            system = DtcSystem.ENGINE,
            severity = DtcSeverity.HIGH,
            symptoms = listOf(
                "Engine management light",
                "Reduced power",
                "Engine may not start"
            ),
            possibleCauses = listOf(
                "Faulty ECU",
                "Software corruption",
                "Power supply issues"
            ),
            repairProcedures = listOf(
                "Check ECU power supply",
                "Attempt ECU reset",
                "Replace ECU if necessary"
            ),
            estimatedRepairCost = "$1000-$3000",
            repairDifficulty = RepairDifficulty.HARD
        ),
        
        // BMW specific
        "2F87" to DtcInfo(
            code = "2F87",
            description = "DME: Valvetronic Motor, Mechanical Malfunction",
            system = DtcSystem.ENGINE,
            severity = DtcSeverity.HIGH,
            symptoms = listOf(
                "Reduced power",
                "Rough idle",
                "Engine warning light"
            ),
            possibleCauses = listOf(
                "Faulty Valvetronic motor",
                "Mechanical binding",
                "Worn components"
            ),
            repairProcedures = listOf(
                "Test Valvetronic motor",
                "Check mechanical operation",
                "Replace motor if faulty"
            ),
            estimatedRepairCost = "$1500-$3000",
            repairDifficulty = RepairDifficulty.HARD
        ),
        
        // Mercedes specific
        "P1000" to DtcInfo(
            code = "P1000",
            description = "System Readiness Test Not Complete",
            system = DtcSystem.ENGINE,
            severity = DtcSeverity.LOW,
            symptoms = listOf(
                "Check engine light",
                "Failed emissions test"
            ),
            possibleCauses = listOf(
                "Recent battery disconnect",
                "Recent ECU reset",
                "Incomplete drive cycle"
            ),
            repairProcedures = listOf(
                "Complete drive cycle",
                "Allow system monitors to run",
                "Drive vehicle normally"
            ),
            estimatedRepairCost = "$0-$100",
            repairDifficulty = RepairDifficulty.EASY
        )
    )
}

data class DtcInfo(
    val code: String,
    val description: String,
    val system: DtcSystem,
    val severity: DtcSeverity,
    val symptoms: List<String>,
    val possibleCauses: List<String>,
    val repairProcedures: List<String>,
    val estimatedRepairCost: String,
    val repairDifficulty: RepairDifficulty,
    val technicalServiceBulletins: List<String> = emptyList(),
    val recallInformation: List<String> = emptyList()
)

enum class DtcSystem {
    ENGINE,
    TRANSMISSION,
    ABS,
    AIRBAG,
    BODY_CONTROL,
    HVAC,
    COMMUNICATION,
    HYBRID,
    ELECTRIC
}

enum class DtcSeverity {
    LOW,     // Information only
    MEDIUM,  // May affect performance
    HIGH,    // Immediate attention required
    CRITICAL // Vehicle unsafe to drive
}

enum class RepairDifficulty {
    EASY,    // DIY friendly
    MEDIUM,  // Some experience required
    HARD     // Professional repair recommended
}

/**
 * DTC Analysis Engine - Provides intelligent diagnostics
 */
class DtcAnalysisEngine(private val database: ComprehensiveDtcDatabase) {
    
    /**
     * Analyze multiple DTCs and provide comprehensive diagnosis
     */
    fun analyzeDtcs(dtcCodes: List<String>): DiagnosisReport {
        val dtcInfos = dtcCodes.mapNotNull { database.getDtcInfo(it) }
        
        if (dtcInfos.isEmpty()) {
            return DiagnosisReport(
                summary = "No valid DTCs found",
                primaryIssues = emptyList(),
                recommendedActions = listOf("Verify DTC codes and re-scan"),
                estimatedCost = "$0",
                urgency = DtcSeverity.LOW
            )
        }
        
        // Group DTCs by system
        val systemGroups = dtcInfos.groupBy { it.system }
        
        // Determine primary issues
        val primaryIssues = identifyPrimaryIssues(dtcInfos)
        
        // Generate recommendations
        val recommendations = generateRecommendations(dtcInfos, systemGroups)
        
        // Calculate estimated costs
        val estimatedCost = calculateEstimatedCost(dtcInfos)
        
        // Determine overall urgency
        val urgency = dtcInfos.maxByOrNull { it.severity.ordinal }?.severity ?: DtcSeverity.LOW
        
        return DiagnosisReport(
            summary = generateSummary(dtcInfos, systemGroups),
            primaryIssues = primaryIssues,
            recommendedActions = recommendations,
            estimatedCost = estimatedCost,
            urgency = urgency,
            affectedSystems = systemGroups.keys.toList(),
            dtcDetails = dtcInfos
        )
    }
    
    private fun identifyPrimaryIssues(dtcs: List<DtcInfo>): List<String> {
        // Logic to identify root causes vs. secondary effects
        val issues = mutableListOf<String>()
        
        // Check for common patterns
        val engineDtcs = dtcs.filter { it.system == DtcSystem.ENGINE }
        val commDtcs = dtcs.filter { it.system == DtcSystem.COMMUNICATION }
        
        if (commDtcs.isNotEmpty()) {
            issues.add("Communication network issues may be causing multiple system faults")
        }
        
        if (engineDtcs.size > 3) {
            issues.add("Multiple engine faults suggest possible ECM or wiring issues")
        }
        
        return issues.ifEmpty { listOf("Individual component faults detected") }
    }
    
    private fun generateRecommendations(dtcs: List<DtcInfo>, systemGroups: Map<DtcSystem, List<DtcInfo>>): List<String> {
        val recommendations = mutableListOf<String>()
        
        // High priority items first
        val criticalDtcs = dtcs.filter { it.severity == DtcSeverity.HIGH || it.severity == DtcSeverity.CRITICAL }
        if (criticalDtcs.isNotEmpty()) {
            recommendations.add("Address critical faults immediately: ${criticalDtcs.joinToString(", ") { it.code }}")
        }
        
        // System-specific recommendations
        systemGroups.forEach { (system, systemDtcs) ->
            when (system) {
                DtcSystem.ENGINE -> {
                    if (systemDtcs.size > 1) {
                        recommendations.add("Perform comprehensive engine diagnosis")
                    }
                }
                DtcSystem.COMMUNICATION -> {
                    recommendations.add("Check CAN bus network and module communications")
                }
                else -> {
                    recommendations.add("Diagnose ${system.name.lowercase()} system")
                }
            }
        }
        
        return recommendations
    }
    
    private fun calculateEstimatedCost(dtcs: List<DtcInfo>): String {
        // Simple cost estimation logic
        val costs = dtcs.mapNotNull { dtc ->
            val costRange = dtc.estimatedRepairCost.replace("$", "").split("-")
            if (costRange.size == 2) {
                costRange[1].toIntOrNull()
            } else {
                costRange[0].toIntOrNull()
            }
        }
        
        if (costs.isEmpty()) return "Unknown"
        
        val totalCost = costs.sum()
        return "$$totalCost - ${(totalCost * 1.5).toInt()}"
    }
    
    private fun generateSummary(dtcs: List<DtcInfo>, systemGroups: Map<DtcSystem, List<DtcInfo>>): String {
        val systemNames = systemGroups.keys.joinToString(", ") { it.name.lowercase() }
        val highPriorityCount = dtcs.count { it.severity == DtcSeverity.HIGH || it.severity == DtcSeverity.CRITICAL }
        
        return buildString {
            append("Found ${dtcs.size} diagnostic trouble code(s) affecting $systemNames system(s). ")
            if (highPriorityCount > 0) {
                append("$highPriorityCount high-priority fault(s) require immediate attention.")
            } else {
                append("All faults are medium to low priority.")
            }
        }
    }
}

data class DiagnosisReport(
    val summary: String,
    val primaryIssues: List<String>,
    val recommendedActions: List<String>,
    val estimatedCost: String,
    val urgency: DtcSeverity,
    val affectedSystems: List<DtcSystem> = emptyList(),
    val dtcDetails: List<DtcInfo> = emptyList()
)

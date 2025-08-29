package com.spacetec.diagnostic.health

import com.spacetec.diagnostic.obd.ObdClient

data class EcuReport(val ecu: String, val dtcs: List<String>)
data class HealthReport(val vin: String?, val reports: List<EcuReport>)

class HealthScanner(private val client: ObdClient) {
    suspend fun run(ecus: List<String>): HealthReport {
        val vin = client.readVin()
        val reps = ecus.map { ecu ->
            val dtcs = client.readDtcs() // For true multi-ECU, set headers per ECU via ISO-TP/ELM header
            EcuReport(ecu, dtcs)
        }
        return HealthReport(vin, reps)
    }
}

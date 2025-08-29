package com.spacetec.diagnostic.flash

import com.spacetec.diagnostic.uds.UdsClient

data class FlashPlan(
    val brand: String, val ecuName: String,
    val session: Int = 0x02, val secLevel: Int = 0x03,
    val startAddr: Int, val blockSize: Int = 0x400
)

class EcuProgrammer(private val uds: UdsClient, private val seedKey: SeedKeyPlugin) {
    suspend fun flash(plan: FlashPlan, fw: ByteArray): Boolean {
        if (!uds.diagnosticSessionControl(plan.session)) return false
        if (!uds.securityAccess(plan.secLevel) { seedKey.computeKey(it) }) return false
        uds.requestDownload(plan.startAddr, fw.size) ?: return false
        var off = 0
        var seq = 1
        while (off < fw.size) {
            val end = (off + plan.blockSize).coerceAtMost(fw.size)
            uds.transferData(seq, fw.copyOfRange(off, end)) ?: return false
            off = end
            seq++
        }
        if (!uds.requestTransferExit()) return false
        uds.ecuReset(0x01)
        return true
    }
}

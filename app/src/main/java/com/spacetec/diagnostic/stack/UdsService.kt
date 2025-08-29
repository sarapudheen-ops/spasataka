package com.spacetec.diagnostic.stack

import com.spacetec.diagnostic.transport.Transport

class UdsService(private val t: Transport) {
    suspend fun diagnosticSessionControl(mode: Int): Boolean =
        sendUds(0x10, byteArrayOf(mode.toByte())) != null

    suspend fun securityAccess(level: Int, seedToKey: (ByteArray)->ByteArray): Boolean {
        val seed = sendUds(0x27, byteArrayOf((level*2-1).toByte())) ?: return false
        val key = seedToKey(seed)
        return sendUds(0x27, byteArrayOf((level*2).toByte()) + key) != null
    }

    suspend fun requestDownload(addr: Int, size: Int, fmt: Byte = 0x00): ByteArray? {
        val payload = byteArrayOf(fmt) + addr.toByteArray(4) + size.toByteArray(4)
        return sendUds(0x34, payload)
    }

    suspend fun transferData(seq: Int, chunk: ByteArray): ByteArray? =
        sendUds(0x36, byteArrayOf(seq.toByte()) + chunk)

    suspend fun requestTransferExit(): Boolean = sendUds(0x37, byteArrayOf()) != null
    suspend fun ecuReset(sub: Int = 0x01): Boolean = sendUds(0x11, byteArrayOf(sub.toByte())) != null

    private suspend fun sendUds(sid: Int, data: ByteArray): ByteArray? {
        // You'll need an ISO-TP layer (Tx: multi-frame; Rx: reassembly) for CAN 11/29bit IDs
        // If using Autel/J2534, you can ask native to do ISO-TP; for ELM, you can use AT CRA/SH/STCF
        // Here just placeholder:
        return null
    }

    private fun Int.toByteArray(len: Int) =
        ByteArray(len) { i -> ((this shr (8*(len-1-i))) and 0xFF).toByte() }
}

package com.spacetec.diagnostic.uds

import com.spacetec.diagnostic.isotp.IsoTp
import com.spacetec.diagnostic.core.toBytesBE

class UdsClient(private val isotp: IsoTp, private val reqId: Int, private val respId: Int) {
    suspend fun diagnosticSessionControl(type: Int) = send(0x10, byteArrayOf(type.toByte()))?.firstOrNull() == (0x50).toByte()
    
    suspend fun securityAccess(level: Int, seedToKey: (ByteArray) -> ByteArray): Boolean {
        val seed = send(0x27, byteArrayOf((level * 2 - 1).toByte())) ?: return false
        val key = seedToKey(seed)
        return send(0x27, byteArrayOf((level * 2).toByte()) + key)?.firstOrNull() == (0x67).toByte()
    }
    
    suspend fun requestDownload(addr: Int, size: Int, fmt: Byte = 0x00) = 
        send(0x34, byteArrayOf(fmt) + addr.toBytesBE(4) + size.toBytesBE(4))
    
    suspend fun transferData(seq: Int, chunk: ByteArray) = 
        send(0x36, byteArrayOf(seq.toByte()) + chunk)
    
    suspend fun requestTransferExit() = send(0x37, byteArrayOf()) != null
    
    suspend fun ecuReset(sub: Int) = send(0x11, byteArrayOf(sub.toByte())) != null

    private suspend fun send(sid: Int, data: ByteArray): ByteArray? {
        isotp.send(reqId, byteArrayOf(sid.toByte()) + data)
        return isotp.receive() // parse NRC/positive here
    }
}

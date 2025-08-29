package com.spacetec.diagnostic.transport

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * ISO-TP (ISO 14229-2) transport layer implementation
 * Handles segmentation and reassembly of UDS messages over CAN
 */
class IsoTpTransport(
    private val underlyingTransport: Transport,
    private val settings: IsoTpSettings
) : Transport {
    
    override val name: String = "ISO-TP over ${underlyingTransport.name}"
    
    private val maxDataLength = 4095 // ISO-TP max data length
    private var sequenceNumber = 0
    
    override suspend fun open(): Boolean = underlyingTransport.open()
    
    override suspend fun close() = underlyingTransport.close()
    
    override suspend fun write(bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        if (bytes.size <= 7) {
            // Single Frame (SF)
            sendSingleFrame(bytes)
        } else {
            // Multi Frame (FF + CF)
            sendMultiFrame(bytes)
        }
    }
    
    private suspend fun sendSingleFrame(data: ByteArray): Boolean {
        val frame = ByteArray(8)
        frame[0] = (0x00 or data.size).toByte() // SF PCI + DL
        data.copyInto(frame, 1)
        
        val canFrame = createCanFrame(settings.reqId, frame)
        return underlyingTransport.write(canFrame)
    }
    
    private suspend fun sendMultiFrame(data: ByteArray): Boolean {
        // First Frame
        val firstFrame = ByteArray(8)
        firstFrame[0] = (0x10 or ((data.size shr 8) and 0x0F)).toByte() // FF PCI + DL high
        firstFrame[1] = (data.size and 0xFF).toByte() // DL low
        data.copyInto(firstFrame, 2, 0, 6)
        
        if (!underlyingTransport.write(createCanFrame(settings.reqId, firstFrame))) {
            return false
        }
        
        // Wait for Flow Control
        val fcReceived = waitForFlowControl()
        if (!fcReceived) {
            Log.e("IsoTpTransport", "Flow Control frame not received")
            return false
        }
        
        // Consecutive Frames
        var offset = 6
        sequenceNumber = 1
        
        while (offset < data.size) {
            val cfFrame = ByteArray(8)
            cfFrame[0] = (0x20 or (sequenceNumber and 0x0F)).toByte() // CF PCI + SN
            
            val copyLength = minOf(7, data.size - offset)
            data.copyInto(cfFrame, 1, offset, offset + copyLength)
            
            if (!underlyingTransport.write(createCanFrame(settings.reqId, cfFrame))) {
                return false
            }
            
            offset += copyLength
            sequenceNumber = (sequenceNumber + 1) and 0x0F
            
            if (settings.stMin > 0) {
                delay(settings.stMin.toLong())
            }
        }
        
        return true
    }
    
    override fun read(): Flow<ByteArray> = flow {
        underlyingTransport.read().collect { canData ->
            val isoTpMessage = processCanFrame(canData)
            if (isoTpMessage != null) {
                emit(isoTpMessage)
            }
        }
    }.flowOn(Dispatchers.IO)
    
    private var receivedFrames = mutableMapOf<Int, ByteArray>()
    private var expectedLength = 0
    private var receivedLength = 0
    
    private fun processCanFrame(canData: ByteArray): ByteArray? {
        if (canData.size < 8) return null
        
        val canId = extractCanId(canData)
        if (canId != settings.respId) return null
        
        val data = canData.sliceArray(4..11) // Extract 8 data bytes
        val pci = (data[0].toInt() and 0xF0) shr 4
        
        return when (pci) {
            0x0 -> { // Single Frame
                val length = data[0].toInt() and 0x0F
                data.sliceArray(1..length)
            }
            0x1 -> { // First Frame
                expectedLength = ((data[0].toInt() and 0x0F) shl 8) or (data[1].toInt() and 0xFF)
                receivedLength = 6
                receivedFrames.clear()
                receivedFrames[0] = data.sliceArray(2..7)
                
                // Send Flow Control frame
                kotlinx.coroutines.runBlocking { sendFlowControl() }
                null
            }
            0x2 -> { // Consecutive Frame
                val sn = data[0].toInt() and 0x0F
                val frameData = data.sliceArray(1..7)
                receivedFrames[sn] = frameData
                receivedLength += frameData.size
                
                if (receivedLength >= expectedLength) {
                    assembleMessage()
                } else {
                    null
                }
            }
            else -> null
        }
    }
    
    private suspend fun sendFlowControl() {
        val fcFrame = ByteArray(8)
        fcFrame[0] = 0x30.toByte() // Continue to Send
        fcFrame[1] = settings.blockSize.toByte()
        fcFrame[2] = settings.stMin.toByte()
        
        underlyingTransport.write(createCanFrame(settings.reqId, fcFrame))
    }
    
    private fun assembleMessage(): ByteArray {
        val result = ByteArray(expectedLength)
        var offset = 0
        
        for (i in 0 until receivedFrames.size) {
            val frameData = receivedFrames[i] ?: continue
            val copyLength = minOf(frameData.size, expectedLength - offset)
            frameData.copyInto(result, offset, 0, copyLength)
            offset += copyLength
        }
        
        return result.sliceArray(0 until expectedLength)
    }
    
    private fun createCanFrame(canId: Int, data: ByteArray): ByteArray {
        val frame = ByteArray(12) // CAN frame: 4 bytes ID + 8 bytes data
        frame[0] = (canId shr 24).toByte()
        frame[1] = (canId shr 16).toByte()
        frame[2] = (canId shr 8).toByte()
        frame[3] = canId.toByte()
        data.copyInto(frame, 4)
        return frame
    }
    
    private fun extractCanId(canFrame: ByteArray): Int {
        return ((canFrame[0].toInt() and 0xFF) shl 24) or
               ((canFrame[1].toInt() and 0xFF) shl 16) or
               ((canFrame[2].toInt() and 0xFF) shl 8) or
               (canFrame[3].toInt() and 0xFF)
    }
    
    /**
     * Wait for Flow Control frame reception with timeout
     * @return true if FC frame received, false on timeout
     */
    private suspend fun waitForFlowControl(): Boolean {
        var retryCount = 0
        val maxRetries = 10 // Wait up to 1 second (100ms * 10)
        
        while (retryCount < maxRetries) {
            try {
                // Listen for incoming frames for a short duration
                val canData = withTimeoutOrNull(100) {
                    var receivedData: ByteArray? = null
                    underlyingTransport.read().collect { data ->
                        val canId = extractCanId(data)
                        if (canId == settings.respId) {
                            val frameData = data.sliceArray(4..11)
                            val pci = (frameData[0].toInt() and 0xF0) shr 4
                            if (pci == 0x3) { // Flow Control frame
                                receivedData = frameData
                                return@collect
                            }
                        }
                    }
                    receivedData
                }
                
                if (canData != null) {
                    val flowStatus = canData[0].toInt() and 0x0F
                    when (flowStatus) {
                        0x0 -> { // ContinueToSend (CTS)
                            Log.d("IsoTpTransport", "Flow Control: Continue to Send received")
                            return true
                        }
                        0x1 -> { // Wait (WT)
                            Log.d("IsoTpTransport", "Flow Control: Wait received")
                            delay(settings.stMin.toLong())
                            // Continue waiting
                        }
                        0x2 -> { // Overflow (OVFLW)
                            Log.e("IsoTpTransport", "Flow Control: Overflow received")
                            return false
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("IsoTpTransport", "Error waiting for Flow Control: ${e.message}")
            }
            
            retryCount++
            delay(100) // Wait 100ms before next attempt
        }
        
        Log.e("IsoTpTransport", "Timeout waiting for Flow Control frame")
        return false
    }
}

/**
 * ISO-TP configuration settings
 */
data class IsoTpSettings(
    val reqId: Int,           // Request CAN ID
    val respId: Int,          // Response CAN ID  
    val blockSize: Int = 0,   // Block size for flow control (0 = no limit)
    val stMin: Int = 0        // Separation time minimum (milliseconds)
)

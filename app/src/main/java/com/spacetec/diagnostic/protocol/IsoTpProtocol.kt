package com.spacetec.diagnostic.protocol

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * Professional ISO-TP (ISO 15765-2) Implementation
 * Handles single-frame, multi-frame with flow control
 * Used by BMW, Mercedes, VAG Group, and all modern vehicles
 */
class IsoTpProtocol(
    private val transport: (ByteArray) -> Unit,
    private val defaultTimeout: Long = 100L
) {
    private val sessions = ConcurrentHashMap<Int, IsoTpSession>()
    private val incomingFrames = Channel<CanFrame>(Channel.UNLIMITED)
    
    // Flow control parameters
    data class FlowControlParams(
        val blockSize: Int = 0,      // 0 = no limit
        val separationTime: Int = 0  // 0 = as fast as possible
    )
    
    init {
        // Start frame processor
        GlobalScope.launch(Dispatchers.IO) {
            processIncomingFrames()
        }
    }
    
    /**
     * Send ISO-TP message (handles segmentation automatically)
     */
    suspend fun sendMessage(
        targetId: Int,
        data: ByteArray,
        functionalAddressing: Boolean = false
    ): Result<ByteArray> = coroutineScope {
        val session = getOrCreateSession(targetId)
        
        return@coroutineScope when {
            data.size <= 7 -> sendSingleFrame(targetId, data, session)
            data.size <= 4095 -> sendMultiFrame(targetId, data, session, functionalAddressing)
            else -> Result.failure(IllegalArgumentException("Message too large: ${data.size} bytes"))
        }
    }
    
    /**
     * Single Frame transmission (SF)
     */
    private suspend fun sendSingleFrame(
        targetId: Int,
        data: ByteArray,
        session: IsoTpSession
    ): Result<ByteArray> {
        val frame = ByteArray(8) { 0x00 }
        frame[0] = (0x00 or data.size).toByte() // SF with length
        System.arraycopy(data, 0, frame, 1, data.size)
        
        // Send and wait for response
        transport(createCanFrame(targetId, frame))
        
        return withTimeoutOrNull(session.timeout) {
            session.responseChannel.receive()
        }?.let { Result.success(it) } 
            ?: Result.failure(TimeoutException("No response within ${session.timeout}ms"))
    }
    
    /**
     * Multi-frame transmission with proper flow control
     */
    private suspend fun sendMultiFrame(
        targetId: Int,
        data: ByteArray,
        session: IsoTpSession,
        functionalAddressing: Boolean
    ): Result<ByteArray> = coroutineScope {
        val totalLength = data.size
        var offset = 0
        
        // First Frame (FF)
        val firstFrame = ByteArray(8)
        firstFrame[0] = (0x10 or ((totalLength shr 8) and 0x0F)).toByte()
        firstFrame[1] = (totalLength and 0xFF).toByte()
        val firstDataBytes = min(6, data.size)
        System.arraycopy(data, 0, firstFrame, 2, firstDataBytes)
        offset += firstDataBytes
        
        // Send first frame
        transport(createCanFrame(targetId, firstFrame))
        
        // Wait for flow control
        val flowControl = withTimeoutOrNull(session.timeout) {
            session.flowControlChannel.receive()
        } ?: return@coroutineScope Result.failure(
            TimeoutException("No flow control received")
        )
        
        // Parse flow control
        if (flowControl[0].toInt() and 0xF0 != 0x30) {
            return@coroutineScope Result.failure(
                ProtocolException("Invalid flow control: ${flowControl[0].toHex()}")
            )
        }
        
        val fc = FlowControlParams(
            blockSize = flowControl[1].toInt() and 0xFF,
            separationTime = flowControl[2].toInt() and 0xFF
        )
        
        // Send consecutive frames
        var sequenceNumber = 1
        var blockCounter = 0
        
        while (offset < totalLength) {
            val consecutiveFrame = ByteArray(8) { 0x00 }
            consecutiveFrame[0] = (0x20 or (sequenceNumber and 0x0F)).toByte()
            
            val dataLength = min(7, totalLength - offset)
            System.arraycopy(data, offset, consecutiveFrame, 1, dataLength)
            
            transport(createCanFrame(targetId, consecutiveFrame))
            
            offset += dataLength
            sequenceNumber = (sequenceNumber + 1) and 0x0F
            blockCounter++
            
            // Handle flow control
            if (fc.blockSize > 0 && blockCounter >= fc.blockSize) {
                // Wait for next flow control
                val nextFc = withTimeoutOrNull(session.timeout) {
                    session.flowControlChannel.receive()
                } ?: return@coroutineScope Result.failure(
                    TimeoutException("No flow control for next block")
                )
                
                if (nextFc[0].toInt() and 0xF0 == 0x31) {
                    // Wait
                    delay(100)
                    continue
                }
                
                blockCounter = 0
            }
            
            // Separation time
            when {
                fc.separationTime == 0 -> { /* As fast as possible */ }
                fc.separationTime <= 127 -> delay(fc.separationTime.toLong())
                fc.separationTime in 0xF1..0xF9 -> delay((fc.separationTime - 0xF0) * 100L)
            }
        }
        
        // Wait for response
        return@coroutineScope withTimeoutOrNull(session.timeout * 2) {
            session.responseChannel.receive()
        }?.let { Result.success(it) }
            ?: Result.failure(TimeoutException("No response to multi-frame message"))
    }
    
    /**
     * Process incoming CAN frames
     */
    private suspend fun processIncomingFrames() {
        for (frame in incomingFrames) {
            val data = frame.data
            if (data.isEmpty()) continue
            
            val pci = data[0].toInt() and 0xF0
            val session = sessions[frame.id] ?: continue
            
            when (pci) {
                0x00 -> handleSingleFrame(frame, session)
                0x10 -> handleFirstFrame(frame, session)
                0x20 -> handleConsecutiveFrame(frame, session)
                0x30 -> handleFlowControl(frame, session)
            }
        }
    }
    
    /**
     * Handle incoming single frame
     */
    private suspend fun handleSingleFrame(frame: CanFrame, session: IsoTpSession) {
        val length = frame.data[0].toInt() and 0x0F
        if (length == 0 || length > 7) return
        
        val responseData = frame.data.sliceArray(1 until (1 + length))
        session.responseChannel.send(responseData)
    }
    
    /**
     * Handle first frame of multi-frame message
     */
    private suspend fun handleFirstFrame(frame: CanFrame, session: IsoTpSession) {
        val length = ((frame.data[0].toInt() and 0x0F) shl 8) or (frame.data[1].toInt() and 0xFF)
        
        session.apply {
            expectedLength = length
            receivedData.clear()
            receivedData.addAll(frame.data.sliceArray(2..7).toList())
            expectedSequence = 1
        }
        
        // Send flow control
        val flowControl = ByteArray(8) { 0x00 }
        flowControl[0] = 0x30 // Clear to send
        flowControl[1] = 0x00 // Block size (0 = unlimited)
        flowControl[2] = 0x00 // Separation time (0 = fast as possible)
        
        transport(createCanFrame(frame.id + 8, flowControl)) // Response ID is typically request ID + 8
    }
    
    /**
     * Handle consecutive frame
     */
    private suspend fun handleConsecutiveFrame(frame: CanFrame, session: IsoTpSession) {
        val sequence = frame.data[0].toInt() and 0x0F
        
        if (sequence != session.expectedSequence) {
            // Sequence error - abort reception
            session.receivedData.clear()
            return
        }
        
        session.expectedSequence = (session.expectedSequence + 1) and 0x0F
        
        val remainingBytes = session.expectedLength - session.receivedData.size
        val dataLength = min(7, remainingBytes)
        
        session.receivedData.addAll(frame.data.sliceArray(1 until (1 + dataLength)).toList())
        
        // Check if complete
        if (session.receivedData.size >= session.expectedLength) {
            val completeData = session.receivedData.take(session.expectedLength).toByteArray()
            session.responseChannel.send(completeData)
            session.receivedData.clear()
        }
    }
    
    /**
     * Handle flow control frame
     */
    private suspend fun handleFlowControl(frame: CanFrame, session: IsoTpSession) {
        session.flowControlChannel.send(frame.data)
    }
    
    /**
     * Feed incoming CAN frame
     */
    suspend fun receiveFrame(id: Int, data: ByteArray) {
        incomingFrames.send(CanFrame(id, data))
    }
    
    private fun getOrCreateSession(id: Int): IsoTpSession {
        return sessions.getOrPut(id) {
            IsoTpSession(
                id = id,
                timeout = defaultTimeout,
                responseChannel = Channel(Channel.CONFLATED),
                flowControlChannel = Channel(Channel.CONFLATED)
            )
        }
    }
    
    private fun createCanFrame(id: Int, data: ByteArray): ByteArray {
        // This would interface with your CAN driver
        // Format depends on your hardware (ELM327, SocketCAN, etc.)
        return data
    }
    
    /**
     * Session state for each ECU communication
     */
    private data class IsoTpSession(
        val id: Int,
        val timeout: Long,
        val responseChannel: Channel<ByteArray>,
        val flowControlChannel: Channel<ByteArray>,
        var expectedLength: Int = 0,
        var expectedSequence: Int = 0,
        val receivedData: MutableList<Byte> = mutableListOf()
    )
    
    data class CanFrame(
        val id: Int,
        val data: ByteArray
    )
    
    class TimeoutException(message: String) : Exception(message)
    class ProtocolException(message: String) : Exception(message)
}

// Extension functions
private fun Byte.toHex(): String = "%02X".format(this)

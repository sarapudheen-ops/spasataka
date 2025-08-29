package com.spacetec.diagnostic.transport

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class J2534Transport : Transport {
    init {
        try {
            System.loadLibrary("j2534")
        } catch (_: Throwable) {
        }
    }

    override val name = "J2534"
    private val flow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 128)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private external fun nativeOpen(): Int
    private external fun nativeClose(): Int
    private external fun nativeWrite(data: ByteArray): Int
    private external fun nativePoll(): ByteArray? // non-blocking in native

    override suspend fun open() = nativeOpen() == 0 && scope.launch {
        while (isActive) {
            nativePoll()?.let { flow.emit(it) } ?: delay(5) // Increased delay to prevent excessive CPU usage
        }
    }.let { true }

    override suspend fun close() {
        scope.cancel()
        nativeClose()
    }

    override suspend fun write(bytes: ByteArray) = nativeWrite(bytes) == 0
    override fun read() = flow.asSharedFlow()
}

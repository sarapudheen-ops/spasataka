package com.spacetec.diagnostic.transport

import kotlinx.coroutines.flow.Flow

interface Transport {
    val name: String
    suspend fun open(): Boolean
    suspend fun close()
    suspend fun write(bytes: ByteArray): Boolean
    fun read(): Flow<ByteArray>
}

enum class Bus { CAN, ISO15765, KLINE }

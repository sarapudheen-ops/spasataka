package com.spacetec.diagnostic.transport

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class Elm327Transport(private val mac: String) : Transport {
    override val name = "ELM327"
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var s: BluetoothSocket? = null
    private var `in`: InputStream? = null
    private var out: OutputStream? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val bus = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)

    override suspend fun open(): Boolean = withContext(Dispatchers.IO) {
        val dev = BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(mac) ?: return@withContext false
        s = dev.createRfcommSocketToServiceRecord(uuid)
        s!!.connect()
        `in` = s!!.inputStream
        out = s!!.outputStream
        scope.launch {
            val buf = ByteArray(4096)
            while (isActive) {
                val n = `in`?.read(buf) ?: -1
                if (n <= 0) break
                bus.tryEmit(buf.copyOf(n))
            }
        }
        listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH1", "ATSP0").forEach { send("$it\r") }
        true
    }

    private fun send(txt: String) {
        out?.write(txt.toByteArray())
        out?.flush()
    }

    override suspend fun close() {
        scope.cancel()
        try {
            `in`?.close()
            out?.close()
            s?.close()
        } catch (_: Throwable) {
        }
    }

    override suspend fun write(bytes: ByteArray) = try {
        out?.write(bytes)
        out?.flush()
        true
    } catch (_: Throwable) {
        false
    }

    override fun read() = bus.asSharedFlow()
}

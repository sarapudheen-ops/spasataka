package com.spacetec.diagnostic.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothTransport(private val device: BluetoothDevice) : Transport {
    
    override val name: String = "Bluetooth SPP (${device.name ?: "Unknown"})"
    
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    
    override suspend fun open(): Boolean = withContext(Dispatchers.IO) {
        try {
            socket = device.createRfcommSocketToServiceRecord(sppUuid)
            socket?.connect()
            inputStream = socket?.inputStream
            outputStream = socket?.outputStream
            Log.i("BluetoothTransport", "Connected to ${device.name}")
            true
        } catch (e: IOException) {
            Log.e("BluetoothTransport", "Connection failed", e)
            close()
            false
        }
    }
    
    override suspend fun close(): Unit = withContext(Dispatchers.IO) {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            Log.w("BluetoothTransport", "Error closing connection", e)
        } finally {
            inputStream = null
            outputStream = null
            socket = null
        }
    }
    
    override suspend fun write(bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            outputStream?.write(bytes)
            outputStream?.flush()
            true
        } catch (e: IOException) {
            Log.e("BluetoothTransport", "Write failed", e)
            false
        }
    }
    
    override fun read(): Flow<ByteArray> = flow {
        val buffer = ByteArray(1024)
        while (socket?.isConnected == true) {
            try {
                // Check if the coroutine has been cancelled
                if (!isActive) break
                
                // Use a small timeout to allow for cancellation
                val bytesRead = inputStream?.read(buffer) ?: -1
                if (bytesRead > 0) {
                    emit(buffer.copyOf(bytesRead))
                }
            } catch (e: IOException) {
                Log.e("BluetoothTransport", "Read failed", e)
                break
            }
        }
    }.flowOn(Dispatchers.IO)
}

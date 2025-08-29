package com.spacetec.diagnostic.transport

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class UsbTransport(
    private val device: UsbDevice,
    private val usbManager: UsbManager
) : Transport {
    
    override val name: String = "USB (${device.deviceName})"
    
    private var connection: UsbDeviceConnection? = null
    
    override suspend fun open(): Boolean = withContext(Dispatchers.IO) {
        try {
            connection = usbManager.openDevice(device)
            if (connection != null) {
                // Configure USB interface and endpoints
                val intf = device.getInterface(0)
                connection?.claimInterface(intf, true)
                Log.i("UsbTransport", "Connected to USB device ${device.deviceName}")
                true
            } else {
                Log.e("UsbTransport", "Failed to open USB device")
                false
            }
        } catch (e: Exception) {
            Log.e("UsbTransport", "USB connection failed", e)
            false
        }
    }
    
    override suspend fun close(): Unit = withContext(Dispatchers.IO) {
        try {
            connection?.close()
        } catch (e: Exception) {
            Log.w("UsbTransport", "Error closing USB connection", e)
        } finally {
            connection = null
        }
    }
    
    override suspend fun write(bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = device.getInterface(0).getEndpoint(1) // OUT endpoint
            val result = connection?.bulkTransfer(endpoint, bytes, bytes.size, 1000)
            result != null && result >= 0
        } catch (e: Exception) {
            Log.e("UsbTransport", "USB write failed", e)
            false
        }
    }
    
    override fun read(): Flow<ByteArray> = flow {
        val buffer = ByteArray(1024)
        val endpoint = device.getInterface(0).getEndpoint(0) // IN endpoint
        
        while (connection != null) {
            try {
                val bytesRead = connection?.bulkTransfer(endpoint, buffer, buffer.size, 100)
                if (bytesRead != null && bytesRead > 0) {
                    emit(buffer.copyOf(bytesRead))
                }
            } catch (e: Exception) {
                Log.e("UsbTransport", "USB read failed", e)
                break
            }
        }
    }.flowOn(Dispatchers.IO)
}

package com.spacetec.ui.protocol

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.spacetec.diagnostic.protocol.AutelProtocolHandler

class ProtocolConnectionViewModel(application: Application) : AndroidViewModel(application) {
    
    private val protocolHandler = AutelProtocolHandler()
    
    val connectionStatus = protocolHandler.connectionStatus
    val protocolVersion = protocolHandler.protocolVersion
    
    private val _bluetoothDevices = MutableStateFlow<List<String>>(emptyList())
    val bluetoothDevices: StateFlow<List<String>> = _bluetoothDevices.asStateFlow()
    
    private val _connectionInfo = MutableStateFlow<Map<String, String>>(emptyMap())
    val connectionInfo: StateFlow<Map<String, String>> = _connectionInfo.asStateFlow()
    
    fun connectTcp(host: String, port: Int) {
        viewModelScope.launch {
            protocolHandler.connectTcp(host, port)
            updateConnectionInfo()
        }
    }
    
    fun connectBluetooth(deviceAddress: String) {
        viewModelScope.launch {
            protocolHandler.connectBluetooth(deviceAddress)
            updateConnectionInfo()
        }
    }
    
    fun scanBluetoothDevices() {
        viewModelScope.launch {
            val devices = protocolHandler.getAvailableBluetoothDevices()
            _bluetoothDevices.value = devices
        }
    }
    
    fun sendDiagnosticRequest(ecuId: String, service: String, data: String = "") {
        viewModelScope.launch {
            protocolHandler.sendDiagnosticRequest(ecuId, service, data)
        }
    }
    
    fun requestLiveData(pids: List<String>) {
        viewModelScope.launch {
            protocolHandler.requestLiveData(pids)
        }
    }
    
    fun readDtcCodes() {
        viewModelScope.launch {
            protocolHandler.readDtcCodes()
        }
    }
    
    fun clearDtcCodes() {
        viewModelScope.launch {
            protocolHandler.clearDtcCodes()
        }
    }
    
    fun executeSpecialFunction(functionId: String, parameters: Map<String, String>) {
        viewModelScope.launch {
            protocolHandler.executeSpecialFunction(functionId, parameters)
        }
    }
    
    fun disconnect() {
        protocolHandler.disconnect()
        updateConnectionInfo()
    }
    
    private fun updateConnectionInfo() {
        _connectionInfo.value = protocolHandler.getConnectionInfo()
    }
}

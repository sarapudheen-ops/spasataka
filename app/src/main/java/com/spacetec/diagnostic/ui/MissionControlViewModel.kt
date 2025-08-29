package com.spacetec.diagnostic.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetec.diagnostic.obd.ObdClient
import com.spacetec.diagnostic.transport.Transport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class LiveGauge(val label: String, val value: String)
data class MissionState(
    val status: String = "Idle",
    val gauges: List<LiveGauge> = emptyList(),
    val connected: Boolean = false
)

class MissionControlViewModel(private val transport: Transport) : ViewModel() {
    val state = MutableStateFlow(MissionState())

    fun connect() {
        viewModelScope.launch {
            state.value = state.value.copy(status = "Launching…")
            val ok = transport.open()
            state.value = state.value.copy(connected = ok, status = if (ok) "Docked" else "Signal lost")
        }
    }

    fun pollBasic(obd: ObdClient) {
        viewModelScope.launch {
            val rpm = obd.readPid("0C") ?: ""
            val spd = obd.readPid("0D") ?: ""
            state.value = state.value.copy(
                gauges = listOf(
                    LiveGauge("Thruster Power", rpm),
                    LiveGauge("Warp Speed", spd)
                )
            )
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            transport.close()
            state.value = state.value.copy(connected = false, status = "Re-entry complete")
        }
    }
}

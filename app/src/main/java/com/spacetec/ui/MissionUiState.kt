package com.spacetec.ui

data class MissionUiState(
    val isScanning: Boolean = false,
    val devices: List<String> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.Idle,
    val statusMessage: String = "🛰️ Ready for launch"
)

enum class ConnectionState {
    Idle,
    Scanning,
    Connecting,
    Connected,
    Disconnected,
    Error
}

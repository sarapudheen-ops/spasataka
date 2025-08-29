package com.spacetec.bluetooth

/**
 * OBD response wrapper for handling command responses
 */
sealed class ObdResponse {
    data class Success(val data: String) : ObdResponse()
    data class Error(val message: String) : ObdResponse()
}

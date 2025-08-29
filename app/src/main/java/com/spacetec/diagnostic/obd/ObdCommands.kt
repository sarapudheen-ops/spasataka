package com.spacetec.diagnostic.obd

object Obd {
    fun pid01(hex: String) = "01$hex\r".encodeToByteArray()
    val MODE_03 = "03\r".encodeToByteArray()
    val MODE_04 = "04\r".encodeToByteArray()
    val MODE_09_VIN = "0902\r".encodeToByteArray()
}

package com.spacetec.diagnostic.logging

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class SessionLogger(private val dir: File) {
    private val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
    private var file: File? = null
    
    fun start(name: String = "session") {
        file = File(dir, "${name}_${sdf.format(Date())}.csv")
        file!!.writeText("time,channel,hex\n")
    }
    
    fun log(channel: String, bytes: ByteArray) {
        val t = System.currentTimeMillis()
        file?.appendText("$t,$channel,${bytes.joinToString("") { "%02X".format(it) }}\n")
    }
    
    fun stop() {
        file = null
    }
}

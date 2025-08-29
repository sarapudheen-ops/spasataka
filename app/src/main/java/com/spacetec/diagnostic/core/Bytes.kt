package com.spacetec.diagnostic.core

fun Int.toBytesBE(len: Int) = ByteArray(len) { i -> ((this shr (8 * (len - 1 - i))) and 0xFF).toByte() }

fun ByteArray.hex() = joinToString(" ") { "%02X".format(it) }

fun String.hexToBytes(): ByteArray = replace("\\s".toRegex(), "")
    .chunked(2).map { it.toInt(16).toByte() }.toByteArray()

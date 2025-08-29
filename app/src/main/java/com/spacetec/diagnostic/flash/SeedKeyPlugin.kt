package com.spacetec.diagnostic.flash

interface SeedKeyPlugin {
    val brand: String
    val level: Int
    fun computeKey(seed: ByteArray): ByteArray
}

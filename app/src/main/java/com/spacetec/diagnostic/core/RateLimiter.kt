package com.spacetec.diagnostic.core

class RateLimiter(private val minIntervalMs: Long) {
    private var last = 0L
    
    @Synchronized 
    fun gate() {
        val now = System.currentTimeMillis()
        val d = now - last
        if (d < minIntervalMs) Thread.sleep(minIntervalMs - d)
        last = System.currentTimeMillis()
    }
}

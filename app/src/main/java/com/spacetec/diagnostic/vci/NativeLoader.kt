package com.spacetec.diagnostic.vci

object NativeLoader {
    private val libraries = listOf(
        "autel_bluetooth",
        "Comm",
        "j2534",
        "MaxiDas",
        "passthru",
        "spacetec_core",
        "spacetec_comm_complete",
        "spacetec_adas",
        "spacetec_battery",
        "spacetec_comm",
        "spacetec_diagnostic",
        "spacetec_tpms"
        // Only 64-bit libraries available in arm64-v8a
    )

    fun loadAll() {
        libraries.forEach {
            try {
                System.loadLibrary(it)
                println("✅ Loaded native lib: $it")
            } catch (e: UnsatisfiedLinkError) {
                println("❌ Failed to load $it: ${e.message}")
            }
        }
    }
}

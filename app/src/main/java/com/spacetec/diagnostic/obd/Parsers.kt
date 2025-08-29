package com.spacetec.diagnostic.obd

object DtcParser {
    fun fromMode03(text: String): List<String> {
        // Extract hex bytes; map to P0xxx/C0xxx etc. (simplified)
        val hex = text.replace("[^0-9A-F]".toRegex(), "")
        if (hex.length < 4) return emptyList()
        // Simple slice; replace with robust frame parser later
        val out = mutableListOf<String>()
        var i = 0
        while (i + 4 <= hex.length) {
            val w = hex.substring(i, i + 4)
            out.add(wordToDtc(w))
            i += 4
        }
        return out.filter { it != "P0000" }
    }

    private fun wordToDtc(w: String): String {
        val b1 = w.substring(0, 2).toInt(16)
        val b2 = w.substring(2, 4).toInt(16)
        val t = when ((b1 and 0xC0) shr 6) {
            0 -> 'P'
            1 -> 'C'
            2 -> 'B'
            else -> 'U'
        }
        val d1 = (b1 shr 4) and 0x3
        val d2 = b1 and 0xF
        val d3 = (b2 shr 4) and 0xF
        val d4 = b2 and 0xF
        return "$t$d1$d2$d3$d4"
    }
}

data class Readiness(
    val milOn: Boolean,
    val misfire: Boolean, val fuel: Boolean, val comp: Boolean,
    val cat: Boolean, val evap: Boolean, val secAir: Boolean, val o2: Boolean, val o2Htr: Boolean,
    val egr: Boolean
)

object ReadinessParser {
    fun parse(resp: String): Readiness {
        // Expect "41 01 XX YY ..." → use XX bits
        val bytes = resp.split(Regex("\\s+")).mapNotNull { it.toIntOrNull(16) }
        val xx = bytes.getOrNull(2) ?: 0
        val mil = (xx and 0x80) != 0
        return Readiness(mil, false, false, false, false, false, false, false, false, false)
    }
}

object Mode09Parser {
    fun parseVin(resp: String): String? {
        // Extract ASCII hex from 49 02 frames; simplify here
        val hex = resp.replace("[^0-9A-F]".toRegex(), "")
        return hex.chunked(2).map { it.toInt(16).toByte() }
            .toByteArray().toString(Charsets.US_ASCII).trim().ifEmpty { null }
    }
}

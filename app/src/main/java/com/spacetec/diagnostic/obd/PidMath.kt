package com.spacetec.diagnostic.obd

object PidMath {
    fun eval(formula: String?, bytes: ByteArray): Double? {
        if (formula == null) return null
        // Minimal tokens: A,B,C,D are byte values 0..255
        val m = mapOf(
            'A' to (bytes.getOrNull(0)?.toInt()?.and(0xFF) ?: 0),
            'B' to (bytes.getOrNull(1)?.toInt()?.and(0xFF) ?: 0),
            'C' to (bytes.getOrNull(2)?.toInt()?.and(0xFF) ?: 0),
            'D' to (bytes.getOrNull(3)?.toInt()?.and(0xFF) ?: 0),
        )
        // Very small parser; you can swap with exp4j/kotlin-expression later
        return try {
            val expr = formula
                .replace("A", m['A'].toString())
                .replace("B", m['B'].toString())
                .replace("C", m['C'].toString())
                .replace("D", m['D'].toString())
            evalSimple(expr)
        } catch (_: Throwable) { null }
    }

    private fun evalSimple(expr: String): Double {
        // Simple expression evaluator for basic math operations
        return try {
            // Handle basic operations: +, -, *, /, parentheses
            val cleanExpr = expr.replace(" ", "")
            when {
                cleanExpr.contains("+") -> {
                    val parts = cleanExpr.split("+")
                    parts.map { evalSimple(it) }.sum()
                }
                cleanExpr.contains("-") && !cleanExpr.startsWith("-") -> {
                    val parts = cleanExpr.split("-")
                    var result = evalSimple(parts[0])
                    for (i in 1 until parts.size) {
                        result -= evalSimple(parts[i])
                    }
                    result
                }
                cleanExpr.contains("*") -> {
                    val parts = cleanExpr.split("*")
                    parts.map { evalSimple(it) }.reduce { acc, value -> acc * value }
                }
                cleanExpr.contains("/") -> {
                    val parts = cleanExpr.split("/")
                    var result = evalSimple(parts[0])
                    for (i in 1 until parts.size) {
                        result /= evalSimple(parts[i])
                    }
                    result
                }
                cleanExpr.startsWith("(") && cleanExpr.endsWith(")") -> {
                    evalSimple(cleanExpr.substring(1, cleanExpr.length - 1))
                }
                else -> cleanExpr.toDouble()
            }
        } catch (e: Exception) {
            0.0
        }
    }
}

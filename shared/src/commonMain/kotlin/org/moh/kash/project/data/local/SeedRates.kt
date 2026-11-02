package org.moh.kash.project.data.local

/**
 * Bundled fallback exchange rates for zero-crash offline cold starts.
 * Based on Central Bank of Jordan official peg and standard market anchor rates.
 */
object SeedRates {
    const val JOD_USD_PEG = 0.7090

    val BASE_USD: Map<String, Double> = mapOf(
        "USD" to 1.0000,
        "EUR" to 0.9200,
        "ILS" to 3.6500,
        "JOD" to 0.7090
    )

    /**
     * Derives seed rates for any supported base currency (USD, EUR, ILS, JOD).
     */
    fun getRatesForBase(baseCurrency: String): Map<String, Double> {
        val base = baseCurrency.trim().uppercase()
        val usdToBase = when (base) {
            "USD" -> 1.0
            "JOD" -> JOD_USD_PEG
            else -> BASE_USD[base] ?: 1.0
        }

        val result = mutableMapOf<String, Double>()
        for (target in listOf("USD", "EUR", "ILS", "JOD")) {
            if (target == base) {
                result[target] = 1.0000
            } else {
                val usdToTarget = when (target) {
                    "USD" -> 1.0
                    "JOD" -> JOD_USD_PEG
                    else -> BASE_USD[target] ?: 1.0
                }
                result[target] = usdToTarget / usdToBase
            }
        }
        return result
    }
}

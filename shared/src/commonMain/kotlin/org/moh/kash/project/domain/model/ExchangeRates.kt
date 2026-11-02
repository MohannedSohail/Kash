package org.moh.kash.project.domain.model

/**
 * Domain entity representing exchange rates relative to a given base currency.
 * Pure Kotlin — zero serialization or framework dependencies.
 */
data class ExchangeRates(
    val baseCurrency: String,
    val rates: Map<String, Double>,
    val timestamp: Long,
    val isOffline: Boolean = false
) {
    fun getRate(targetCode: String): Double? {
        val normalized = targetCode.trim().uppercase()
        if (normalized == baseCurrency.uppercase()) return 1.0
        return rates[normalized]
    }
}

package org.moh.kash.project.domain.repository

import kotlinx.coroutines.flow.Flow
import org.moh.kash.project.domain.model.ExchangeRates

/**
 * Domain repository contract for currency exchange rates and user preferences.
 * Adheres strictly to unidirectional data flow and clean hexagonal layering.
 */
interface CurrencyRepository {
    /**
     * Emits a reactive stream of exchange rates for the given base currency.
     * Implements cache-aside: returns cached or seed rates immediately, then reconciles over network.
     */
    fun getRates(baseCurrency: String, forceRefresh: Boolean = false): Flow<Result<ExchangeRates>>

    /**
     * Alias conforming to async stream naming convention.
     */
    fun getRatesStream(baseCurrency: String, forceRefresh: Boolean = false): Flow<Result<ExchangeRates>> =
        getRates(baseCurrency, forceRefresh)

    /**
     * Persists the user's preferred default base currency code.
     */
    suspend fun saveBaseCurrency(currencyCode: String)

    /**
     * Observes the user's persisted base currency preference.
     */
    fun getBaseCurrency(): Flow<String>
}

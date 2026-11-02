package org.moh.kash.project.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.moh.kash.project.domain.model.ExchangeRates
import org.moh.kash.project.domain.repository.CurrencyRepository

/**
 * Use case retrieving real-time or cached exchange rates for a given base currency.
 */
class GetExchangeRatesUseCase(
    private val repository: CurrencyRepository
) {
    operator fun invoke(baseCurrency: String, forceRefresh: Boolean = false): Flow<Result<ExchangeRates>> {
        return repository.getRates(baseCurrency, forceRefresh)
    }
}

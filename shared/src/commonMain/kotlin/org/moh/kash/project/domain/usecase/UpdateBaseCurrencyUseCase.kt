package org.moh.kash.project.domain.usecase

import org.moh.kash.project.domain.repository.CurrencyRepository

/**
 * Use case persisting user's default base currency preference.
 */
class UpdateBaseCurrencyUseCase(
    private val repository: CurrencyRepository
) {
    suspend operator fun invoke(currencyCode: String) {
        repository.saveBaseCurrency(currencyCode)
    }
}

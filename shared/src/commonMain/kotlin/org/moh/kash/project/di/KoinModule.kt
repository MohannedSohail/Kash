package org.moh.kash.project.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.moh.kash.project.data.local.CurrencyDataStore
import org.moh.kash.project.data.remote.FreeCurrencyApiClient
import org.moh.kash.project.data.repository.CurrencyRepositoryImpl
import org.moh.kash.project.domain.repository.CurrencyRepository
import org.moh.kash.project.domain.usecase.ConvertCurrencyUseCase
import org.moh.kash.project.domain.usecase.GetExchangeRatesUseCase
import org.moh.kash.project.domain.usecase.UpdateBaseCurrencyUseCase
import org.moh.kash.project.presentation.exchange.ExchangeViewModel

/**
 * Cross-platform In-Memory DataStore implementation for Preferences.
 * Ensures instant, thread-safe, non-blocking state access across all KMP targets.
 */
class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val _flow = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = _flow

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val next = transform(_flow.value)
        _flow.value = next
        return next
    }
}

val appModule: Module = module {
    single<DataStore<Preferences>> { InMemoryPreferencesDataStore() }
    single { CurrencyDataStore(dataStore = get()) }
    single { FreeCurrencyApiClient.createDefaultHttpClient() }
    single { FreeCurrencyApiClient(httpClient = get()) }
    single<CurrencyRepository> { CurrencyRepositoryImpl(apiClient = get(), dataStore = get()) }
    factory { ConvertCurrencyUseCase() }
    factory { GetExchangeRatesUseCase(repository = get()) }
    factory { UpdateBaseCurrencyUseCase(repository = get()) }
    viewModel {
        ExchangeViewModel(
            getExchangeRatesUseCase = get(),
            convertCurrencyUseCase = get(),
            updateBaseCurrencyUseCase = get(),
            currencyRepository = get()
        )
    }
}

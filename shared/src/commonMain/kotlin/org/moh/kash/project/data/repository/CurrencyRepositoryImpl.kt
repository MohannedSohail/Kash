package org.moh.kash.project.data.repository

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.moh.kash.project.core.error.AppError
import org.moh.kash.project.currentTimeMillis
import org.moh.kash.project.data.local.CurrencyDataStore
import org.moh.kash.project.data.local.SeedRates
import org.moh.kash.project.data.remote.FreeCurrencyApiClient
import org.moh.kash.project.domain.model.ExchangeRates
import org.moh.kash.project.domain.repository.CurrencyRepository

/**
 * Production implementation of CurrencyRepository with offline-first cache-aside strategy.
 * Derives JOD cross-rates mathematically from the Central Bank of Jordan USD peg (0.7090).
 */
class CurrencyRepositoryImpl(
    private val apiClient: FreeCurrencyApiClient,
    private val dataStore: CurrencyDataStore,
    private val timeProvider: () -> Long = { currentTimeMillis() }
) : CurrencyRepository {

    companion object {
        const val CACHE_TTL_MILLIS = 3600_000L // 1 hour
    }

    override fun getRates(baseCurrency: String, forceRefresh: Boolean): Flow<Result<ExchangeRates>> = flow {
        val base = baseCurrency.trim().uppercase()
        val now = timeProvider()

        // 1. Check local DataStore cache
        val cached = dataStore.getCachedRates(base)
        var cachedEmitted = false

        if (cached != null) {
            val cacheAge = now - cached.timestamp
            val isFresh = cacheAge in 0..CACHE_TTL_MILLIS

            if (isFresh && !forceRefresh) {
                // Fresh cache: emit and finish
                emit(Result.success(ExchangeRates(base, cached.rates, cached.timestamp, isOffline = false)))
                return@flow
            } else {
                // Stale cache: emit immediately as offline/cached so UI has content while reconciling
                emit(Result.success(ExchangeRates(base, cached.rates, cached.timestamp, isOffline = true)))
                cachedEmitted = true
            }
        } else {
            // Cold start without cache: emit bundled seed rates with isOffline = true
            val seed = SeedRates.getRatesForBase(base)
            emit(Result.success(ExchangeRates(base, seed, 0L, isOffline = true)))
            cachedEmitted = true
        }

        // 2. Fetch fresh rates over network
        try {
            val dto = apiClient.getLatestRates(base)
            val derivedRates = synthesizeRates(base, dto.data)
            val fetchTimestamp = timeProvider()

            // Persist to DataStore
            dataStore.saveRates(base, derivedRates, fetchTimestamp)

            // Emit fresh rates
            emit(Result.success(ExchangeRates(base, derivedRates, fetchTimestamp, isOffline = false)))
        } catch (e: Exception) {
            val appError = mapToAppError(e)
            // Propagate error wrapped in Result so UI can show Popup/Snackbar while retaining content
            emit(Result.failure(appError))
        }
    }

    override suspend fun saveBaseCurrency(currencyCode: String) {
        dataStore.saveBaseCurrency(currencyCode)
    }

    override fun getBaseCurrency(): Flow<String> {
        return dataStore.getBaseCurrency()
    }

    /**
     * Synthesizes JOD exchange rates via the Central Bank of Jordan fixed USD peg (0.7090).
     */
    fun synthesizeRates(baseCurrency: String, rawData: Map<String, Double>): Map<String, Double> {
        val base = baseCurrency.trim().uppercase()
        val peg = SeedRates.JOD_USD_PEG // 0.7090

        if (base == "JOD") {
            // Remote was queried with base_currency=USD, returning R(USD -> T)
            val result = mutableMapOf<String, Double>()
            result["JOD"] = 1.0000
            result["USD"] = 1.0 / peg // Exact division: 1.4104372355...
            for ((currency, usdRate) in rawData) {
                if (currency != "JOD" && currency != "USD") {
                    result[currency] = usdRate / peg
                }
            }
            return result
        } else {
            val result = rawData.toMutableMap()
            result[base] = 1.0000
            val rateToUsd = if (base == "USD") 1.0 else (result["USD"] ?: 1.0)
            result["JOD"] = rateToUsd * peg
            return result
        }
    }

    private fun mapToAppError(e: Throwable): AppError {
        return when (e) {
            is ClientRequestException -> {
                when (e.response.status.value) {
                    401 -> AppError.AuthenticationError(cause = e)
                    429 -> AppError.QuotaExceededError(cause = e)
                    else -> AppError.NetworkError("HTTP ${e.response.status.value}: ${e.message}", e.response.status.value, e)
                }
            }
            is ServerResponseException -> {
                AppError.NetworkError("Server outage (HTTP ${e.response.status.value})", e.response.status.value, e)
            }
            else -> {
                AppError.NetworkError(e.message ?: "Network connection error", cause = e)
            }
        }
    }
}

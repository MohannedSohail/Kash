package org.moh.kash.project.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.moh.kash.project.data.remote.model.LatestRatesDto
import org.moh.kash.project.data.remote.model.StatusDto

/**
 * Remote client for FreeCurrencyAPI (https://freecurrencyapi.com/).
 * Enforces client-side query sanitization: requests only native supported currencies (USD, EUR, ILS),
 * strictly omitting JOD to prevent HTTP 422 Unprocessable Entity errors.
 */
class FreeCurrencyApiClient(
    val httpClient: HttpClient,
    val apiKey: String = DEFAULT_API_KEY,
    val baseUrl: String = DEFAULT_BASE_URL
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://api.freecurrencyapi.com/v1"
        // Configurable fallback key for development / testing
        const val DEFAULT_API_KEY = "fca_live_kash_dev_key"
        const val SUPPORTED_REMOTE_CURRENCIES = "USD,EUR,ILS"

        fun createDefaultHttpClient(): HttpClient {
            return HttpClient {
                expectSuccess = true
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        prettyPrint = false
                    })
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 15000
                    connectTimeoutMillis = 15000
                    socketTimeoutMillis = 15000
                }
            }
        }
    }

    /**
     * Fetches latest exchange rates from /v1/latest.
     * When [baseCurrency] is "JOD", requests base_currency="USD" to avoid remote 422 error.
     */
    internal suspend fun getLatestRates(baseCurrency: String = "USD"): LatestRatesDto {
        val effectiveBase = if (baseCurrency.equals("JOD", ignoreCase = true)) "USD" else baseCurrency.uppercase()

        return httpClient.get("$baseUrl/latest") {
            if (apiKey.isNotBlank()) {
                header("apikey", apiKey)
                parameter("apikey", apiKey)
            }
            parameter("base_currency", effectiveBase)
            // Strictly query USD,EUR,ILS to prevent 422 error on JOD
            parameter("currencies", SUPPORTED_REMOTE_CURRENCIES)
        }.body()
    }

    /**
     * Checks account status and remaining monthly quota from /v1/status.
     * Zero-cost call that does not consume monthly quota.
     */
    internal suspend fun getStatus(): StatusDto {
        return httpClient.get("$baseUrl/status") {
            if (apiKey.isNotBlank()) {
                header("apikey", apiKey)
                parameter("apikey", apiKey)
            }
        }.body()
    }
}

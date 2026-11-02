package org.moh.kash.project.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class CachedRatesPayload(
    val baseCurrency: String,
    val rates: Map<String, Double>,
    val timestamp: Long
)

data class CachedRates(
    val baseCurrency: String,
    val rates: Map<String, Double>,
    val timestamp: Long
)

/**
 * Preferences DataStore wrapper for caching exchange rates and user preferences.
 * Structured rates payload rides as a JSON string key in Preferences DataStore.
 */
class CurrencyDataStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) {
    companion object {
        val KEY_RATES_PREFIX = "rates_payload_"
        val KEY_DEFAULT_RATES_TIMESTAMP = longPreferencesKey("default_rates_timestamp")
        val KEY_BASE_CURRENCY = stringPreferencesKey("user_base_currency")
    }

    /**
     * Persists latest rates for [baseCurrency] with timestamp.
     */
    suspend fun saveRates(baseCurrency: String, rates: Map<String, Double>, timestamp: Long) {
        val payload = CachedRatesPayload(
            baseCurrency = baseCurrency.uppercase(),
            rates = rates,
            timestamp = timestamp
        )
        val jsonString = json.encodeToString(payload)
        val key = stringPreferencesKey(KEY_RATES_PREFIX + baseCurrency.uppercase())

        dataStore.edit { preferences ->
            preferences[key] = jsonString
            preferences[KEY_DEFAULT_RATES_TIMESTAMP] = timestamp
        }
    }

    /**
     * Retrieves cached rates for [baseCurrency] if present.
     */
    suspend fun getCachedRates(baseCurrency: String): CachedRates? {
        val key = stringPreferencesKey(KEY_RATES_PREFIX + baseCurrency.uppercase())
        val preferences = dataStore.data.catch { emit(emptyPreferences()) }.firstOrNull() ?: return null
        val jsonString = preferences[key] ?: return null

        return try {
            val payload = json.decodeFromString<CachedRatesPayload>(jsonString)
            CachedRates(
                baseCurrency = payload.baseCurrency,
                rates = payload.rates,
                timestamp = payload.timestamp
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Persists the user's preferred default base currency.
     */
    suspend fun saveBaseCurrency(currencyCode: String) {
        dataStore.edit { preferences ->
            preferences[KEY_BASE_CURRENCY] = currencyCode.uppercase()
        }
    }

    /**
     * Returns a reactive flow of the selected base currency (defaulting to "USD").
     */
    fun getBaseCurrency(): Flow<String> {
        return dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { preferences ->
                preferences[KEY_BASE_CURRENCY] ?: "USD"
            }
    }
}

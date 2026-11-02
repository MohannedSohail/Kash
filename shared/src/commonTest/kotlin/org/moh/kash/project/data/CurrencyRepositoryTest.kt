package org.moh.kash.project.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.moh.kash.project.core.error.AppError
import org.moh.kash.project.data.local.CurrencyDataStore
import org.moh.kash.project.data.local.SeedRates
import org.moh.kash.project.data.remote.FreeCurrencyApiClient
import org.moh.kash.project.data.repository.CurrencyRepositoryImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CurrencyRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Test Fake InMemory DataStore
    private class TestInMemoryDataStore : DataStore<Preferences> {
        private val _flow = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = _flow
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val next = transform(_flow.value)
            _flow.value = next
            return next
        }
    }

    private fun createTestClient(engine: MockEngine): FreeCurrencyApiClient {
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(json)
            }
        }
        return FreeCurrencyApiClient(client, apiKey = "test_key", baseUrl = "https://api.freecurrencyapi.com/v1")
    }

    @Test
    fun testHttp200ReturnsRatesAndSynthesizesJodPeg() = runTest {
        var interceptedUrl = ""
        val engine = MockEngine { request ->
            interceptedUrl = request.url.toString()
            respond(
                content = """{"data":{"EUR":0.92,"ILS":3.65,"USD":1.0}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val apiClient = createTestClient(engine)
        val dataStore = CurrencyDataStore(TestInMemoryDataStore(), json)
        val repo = CurrencyRepositoryImpl(apiClient, dataStore, timeProvider = { 1000L })

        val emissions = repo.getRates("USD", forceRefresh = true).toList()
        assertTrue(emissions.isNotEmpty())

        val lastResult = emissions.last()
        assertTrue(lastResult.isSuccess)
        val rates = lastResult.getOrThrow()

        // Verify request omitted JOD and queried USD,EUR,ILS
        assertTrue(interceptedUrl.contains("currencies=USD%2CEUR%2CILS") || interceptedUrl.contains("currencies=USD,EUR,ILS"))
        assertFalse(interceptedUrl.contains("JOD"))

        // Verify JOD peg synthesized
        assertEquals(1.0, rates.rates["USD"])
        assertEquals(0.92, rates.rates["EUR"])
        assertEquals(3.65, rates.rates["ILS"])
        assertEquals(0.7090, rates.rates["JOD"])
        assertFalse(rates.isOffline)
    }

    @Test
    fun testJodBaseQueriesUsdAndDerivesTargetCrossRates() = runTest {
        var interceptedBase = ""
        val engine = MockEngine { request ->
            interceptedBase = request.url.parameters["base_currency"] ?: ""
            respond(
                content = """{"data":{"EUR":0.92,"ILS":3.65,"USD":1.0}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val apiClient = createTestClient(engine)
        val dataStore = CurrencyDataStore(TestInMemoryDataStore(), json)
        val repo = CurrencyRepositoryImpl(apiClient, dataStore, timeProvider = { 2000L })

        val emissions = repo.getRates("JOD", forceRefresh = true).toList()
        val rates = emissions.last().getOrThrow()

        // Remote was queried with base_currency=USD to prevent 422
        assertEquals("USD", interceptedBase)

        // JOD-relative cross rates
        assertEquals(1.0, rates.rates["JOD"])
        assertEquals(1.0 / 0.7090, rates.rates["USD"]!!, 0.0001)
        assertEquals(0.92 / 0.7090, rates.rates["EUR"]!!, 0.0001)
        assertEquals(3.65 / 0.7090, rates.rates["ILS"]!!, 0.0001)
    }

    @Test
    fun testOfflineColdLaunchWithEmptyCacheFallsBackToSeedRates() = runTest {
        val engine = MockEngine {
            throw RuntimeException("Network connection failed (Airplane mode)")
        }

        val apiClient = createTestClient(engine)
        val dataStore = CurrencyDataStore(TestInMemoryDataStore(), json)
        val repo = CurrencyRepositoryImpl(apiClient, dataStore, timeProvider = { 3000L })

        val emissions = repo.getRates("USD", forceRefresh = false).toList()
        assertTrue(emissions.isNotEmpty())

        // Cold start emits SeedRates first
        val firstEmission = emissions.first()
        assertTrue(firstEmission.isSuccess)
        val seed = firstEmission.getOrThrow()
        assertTrue(seed.isOffline)
        assertEquals(1.0, seed.rates["USD"])
        assertEquals(0.92, seed.rates["EUR"])
        assertEquals(3.65, seed.rates["ILS"])
        assertEquals(0.7090, seed.rates["JOD"])

        // Second emission is the network failure Result
        val lastEmission = emissions.last()
        assertTrue(lastEmission.isFailure)
        assertTrue(lastEmission.exceptionOrNull() is AppError.NetworkError)
    }

    @Test
    fun testFreshCacheDoesNotTriggerNetworkCall() = runTest {
        var networkCalled = false
        val engine = MockEngine {
            networkCalled = true
            respond(
                content = """{"data":{"EUR":0.90,"ILS":3.60,"USD":1.0}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val apiClient = createTestClient(engine)
        val testDataStore = TestInMemoryDataStore()
        val dataStore = CurrencyDataStore(testDataStore, json)
        val currentTime = 10000L

        // Pre-save fresh rates (timestamp 9500L, age 500ms < 1hr)
        dataStore.saveRates("USD", mapOf("EUR" to 0.91, "ILS" to 3.62, "USD" to 1.0, "JOD" to 0.709), 9500L)

        val repo = CurrencyRepositoryImpl(apiClient, dataStore, timeProvider = { currentTime })
        val emissions = repo.getRates("USD", forceRefresh = false).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions.first().isSuccess)
        val rates = emissions.first().getOrThrow()
        assertFalse(rates.isOffline)
        assertEquals(0.91, rates.rates["EUR"])
        assertFalse(networkCalled, "Network should not be called when cache is fresh")
    }

    @Test
    fun testHttp401UnauthorizedPropagatesError() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"message":"Invalid API key"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val apiClient = createTestClient(engine)
        val dataStore = CurrencyDataStore(TestInMemoryDataStore(), json)
        val repo = CurrencyRepositoryImpl(apiClient, dataStore, timeProvider = { 4000L })

        val emissions = repo.getRates("USD", forceRefresh = true).toList()
        assertEquals(2, emissions.size)
        assertTrue(emissions[0].isSuccess, "Emits initial fallback/cached rates first")
        assertTrue(emissions[1].isFailure, "Emits failure on 401")
        assertTrue(emissions[1].exceptionOrNull() is AppError.AuthenticationError)
    }

    @Test
    fun testHttp429QuotaExceededPropagatesError() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"message":"Quota exceeded"}""",
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val apiClient = createTestClient(engine)
        val dataStore = CurrencyDataStore(TestInMemoryDataStore(), json)
        val repo = CurrencyRepositoryImpl(apiClient, dataStore, timeProvider = { 5000L })

        val emissions = repo.getRates("USD", forceRefresh = true).toList()
        assertEquals(2, emissions.size)
        assertTrue(emissions[0].isSuccess)
        assertTrue(emissions[1].isFailure)
        assertTrue(emissions[1].exceptionOrNull() is AppError.QuotaExceededError)
    }

    @Test
    fun testBaseCurrencyPersistence() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.OK) }
        val apiClient = createTestClient(engine)
        val dataStore = CurrencyDataStore(TestInMemoryDataStore(), json)
        val repo = CurrencyRepositoryImpl(apiClient, dataStore)

        assertEquals("USD", repo.getBaseCurrency().first())

        repo.saveBaseCurrency("EUR")
        assertEquals("EUR", repo.getBaseCurrency().first())

        repo.saveBaseCurrency("JOD")
        assertEquals("JOD", repo.getBaseCurrency().first())
    }
}

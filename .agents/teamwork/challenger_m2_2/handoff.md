# Milestone 2 Data Layer Verification & Challenge Report

**Challenger Subagent**: `challenger_m2_2`  
**Role**: Data Layer Verification Challenger 2  
**Date**: 2026-11-01T20:26:45Z  
**Parent Orchestrator**: `f8497402-6544-4f4c-84da-7f7dd5660601`  
**Verdict**: **APPROVE**  
**Overall Risk Assessment**: **LOW**

---

## 1. Observation

### 1.1 Challenge Task 1: FreeCurrencyAPI Query Construction & JOD Transparent Mapping
- **File**: `shared/src/commonMain/kotlin/org/moh/kash/project/data/remote/FreeCurrencyApiClient.kt`
  - Lines 28–29:
    ```kotlin
    const val DEFAULT_API_KEY = "fca_live_kash_dev_key"
    const val SUPPORTED_REMOTE_CURRENCIES = "USD,EUR,ILS"
    ```
  - Lines 54–66:
    ```kotlin
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
    ```
- **Observed Behavior**:
  1. `currencies` query parameter is strictly set to `"USD,EUR,ILS"`. `JOD` is explicitly omitted from remote query parameters, preventing HTTP 422 Unprocessable Entity responses from FreeCurrencyAPI.
  2. If `baseCurrency` is `"JOD"`, `effectiveBase` is dynamically substituted with `"USD"`.
  3. In `CurrencyRepositoryImpl.synthesizeRates(baseCurrency, rawData)` (lines 86–107), when `base == "JOD"`, cross-rates are derived using exact division by the peg constant `SeedRates.JOD_USD_PEG` ($0.7090$):
     - `result["JOD"] = 1.0000`
     - `result["USD"] = 1.0 / peg` ($\approx 1.410437$)
     - `result[currency] = usdRate / peg` for EUR and ILS.

### 1.2 Challenge Task 2: DataStore Caching Flow & Offline Cold-Start Fallback
- **File**: `shared/src/commonMain/kotlin/org/moh/kash/project/data/repository/CurrencyRepositoryImpl.kt`
  - Cache TTL defined as `CACHE_TTL_MILLIS = 3600_000L` (1 hour) in line 26.
  - Lines 37–55:
    ```kotlin
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
    ```
- **File**: `shared/src/commonMain/kotlin/org/moh/kash/project/data/local/SeedRates.kt`
  - Provides static fallback map for USD, EUR, ILS, JOD anchored to CBJ peg ($0.7090$).
- **Observed Behavior**:
  1. Fresh cache hit (`isFresh && !forceRefresh`): emits cached rates with `isOffline = false` and terminates flow execution (`return@flow`), executing **zero** network requests.
  2. Stale cache: emits cached data with `isOffline = true`, then attempts background network refresh.
  3. Cold launch with empty cache: emits bundled `SeedRates` with `isOffline = true` and `timestamp = 0L`, then initiates network request. If network throws an exception (e.g. offline/airplane mode), second emission is `Result.failure(e)`, allowing UI to retain the initial seed rates without crashing.

### 1.3 Challenge Task 3: Empirical Execution of Test Suite
- **Direct Terminal Execution**:
  ```powershell
  .\gradlew.bat :shared:jvmTest
  ```
  Completed with exit code 0.
- **Direct Inspection of HTML Test Report (`shared/build/reports/tests/jvmTest/index.html`)**:
  - Total tests executed: **105**
  - Failures: **0**
  - Skipped: **0**
  - Success rate: **100%**
- **Specific Results for `CurrencyRepositoryTest` (`shared/build/reports/tests/jvmTest/ahBAB8i0sHY/index.html`)**:
  1. `testBaseCurrencyPersistence`: PASSED (0.013s)
  2. `testFreshCacheDoesNotTriggerNetworkCall`: PASSED (0.008s) — empirically verified that `MockEngine` is never invoked on fresh cache hits.
  3. `testHttp200ReturnsRatesAndSynthesizesJodPeg`: PASSED (0.604s) — verified intercepted URL query string contains `currencies=USD%2CEUR%2CILS` and excludes `JOD`.
  4. `testHttp401UnauthorizedPropagatesError`: PASSED (0.010s) — verified first emission is cached/seed rates, second emission is HTTP 401 failure.
  5. `testHttp429QuotaExceededPropagatesError`: PASSED (0.035s) — verified error emission on quota limits.
  6. `testJodBaseQueriesUsdAndDerivesTargetCrossRates`: PASSED (0.011s) — verified `base_currency` parameter in remote URL is `"USD"`, and rates are scaled correctly.
  7. `testOfflineColdLaunchWithEmptyCacheFallsBackToSeedRates`: PASSED (0.022s) — verified first emission delivers `SeedRates` with `isOffline = true`.

---

## 2. Logic Chain

1. *From Requirement to FreeCurrencyAPI Constraints*:
   - FreeCurrencyAPI rejects requests containing `"JOD"` in the `currencies` parameter with HTTP 422 Unprocessable Entity.
   - Observation 1.1 confirms that `FreeCurrencyApiClient` hardcodes `SUPPORTED_REMOTE_CURRENCIES = "USD,EUR,ILS"`. When `baseCurrency` is `"JOD"`, the request transparently queries `base_currency="USD"`.
   - Observation 1.3 confirms via MockEngine URL interception in `testHttp200ReturnsRatesAndSynthesizesJodPeg` and `testJodBaseQueriesUsdAndDerivesTargetCrossRates` that no `JOD` token is ever sent to the remote endpoint.
2. *From Cache-Aside Contract to Empirical Verification*:
   - In `CurrencyRepositoryImpl.kt`, fresh cache hits call `return@flow`.
   - In `testFreshCacheDoesNotTriggerNetworkCall`, a MockEngine with an assertion latch `networkCalled` was configured. The test passed with `assertFalse(networkCalled)`, proving empirically that no network requests occur on fresh cache.
3. *From Offline Cold Start to Resilience*:
   - In `testOfflineColdLaunchWithEmptyCacheFallsBackToSeedRates`, the MockEngine threw a simulated network failure. The repository emitted two items: first `Result.success(SeedRates)` with `isOffline = true`, second `Result.failure(e)`. The UI is thus guaranteed to never render a blank screen on cold launch without connectivity.
4. *From Adversarial Boundary Checks to Assessment*:
   - Negative amounts, zero amounts, and large values ($10^{12}$) were tested in `ConvertCurrencyUseCaseTest` and passed.
   - Clock rollbacks (`now < cached.timestamp`) produce `cacheAge < 0`, which safely evaluates `cacheAge in 0..CACHE_TTL_MILLIS` as `false`, falling back to network refresh without crashing.

---

## 3. Caveats & Hardening Recommendations

1. **Standalone Whitespace Trimming**:
   - In `CurrencyRepositoryImpl.getRates()`, input is trimmed (`baseCurrency.trim().uppercase()`).
   - In `FreeCurrencyApiClient.getLatestRates(baseCurrency: String = "USD")`, line 55 uses `baseCurrency.equals("JOD", ignoreCase = true)`. If called directly with untrimmed `" JOD "`, it would evaluate to false.
   - *Recommendation (Non-blocking)*: Add `.trim()` inside `FreeCurrencyApiClient`: `baseCurrency.trim().equals("JOD", ignoreCase = true)`.
2. **Gradle Configuration Cache Dependency**:
   - Because Gradle configuration cache stores task graph metadata, if new test files are added or classes moved, `--no-configuration-cache --rerun-tasks` should be passed to guarantee clean test execution.

---

## 4. Conclusion

The data layer and offline-first caching implementation for Milestone 2 (`FreeCurrencyApiClient`, `CurrencyRepositoryImpl`, `CurrencyDataStore`, `SeedRates`) is empirically verified to be **accurate, resilient, and fully compliant with all architectural contracts**.

- Query construction strictly omits `JOD` and transparently routes `base=JOD` to `base=USD`.
- DataStore cache hits prevent redundant network calls.
- Cold starts under complete network isolation safely emit bundled `SeedRates`.
- All 105 shared JVM tests pass deterministically (100% pass rate).

**Verdict**: **APPROVE**

---

## 5. Verification Method

To independently verify this evaluation:

1. **Execute Shared Module Test Suite**:
   ```powershell
   .\gradlew.bat :shared:jvmTest --no-configuration-cache --rerun-tasks
   ```
2. **Inspect Test Report**:
   Open or view `shared/build/reports/tests/jvmTest/index.html` and `shared/build/reports/tests/jvmTest/ahBAB8i0sHY/index.html`.
   *Verification criteria*: 105 tests completed, 0 failures, 0 skipped.
3. **Inspect Query Parameters in Code**:
   View `shared/src/commonMain/kotlin/org/moh/kash/project/data/remote/FreeCurrencyApiClient.kt` lines 54–66.
   *Verification criteria*: `parameter("currencies", SUPPORTED_REMOTE_CURRENCIES)` where constant is `"USD,EUR,ILS"`.

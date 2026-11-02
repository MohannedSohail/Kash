# Milestone 2 Review & Adversarial Challenge Report

**Reviewer**: `reviewer_m2_1`  
**Roles**: Reviewer & Adversarial Critic  
**Working Directory**: `e:\AI Projects\Kash\Kash\.agents\teamwork\reviewer_m2_1\`  
**Date**: 2026-11-01T20:22:00Z  
**Parent Orchestrator**: `f8497402-6544-4f4c-84da-7f7dd5660601`  
**Verdict**: **REQUEST_CHANGES**  

---

## 1. Observation

### 1.1 Codebase & Interface Compliance
1. **Clean Multiplatform Boundaries**:
   - `shared/src/commonMain/kotlin/org/moh/kash/project/` contains **0** imports matching `import (java|android)\.`.
   - Domain models (`Currency`, `ExchangeRates`, `ConversionResult`) contain **0** serialization or framework annotations (`@Serializable`, `@SerialName`).
   - Wire DTOs (`LatestRatesDto`, `StatusDto`, `QuotasDto`, `MonthQuotaDto`, `PlanDto`) and `CachedRatesPayload` are all strictly marked `internal`.
2. **Remote API & JOD 422 Avoidance**:
   - `FreeCurrencyApiClient.kt` lines 29, 62–64 strictly queries `SUPPORTED_REMOTE_CURRENCIES = "USD,EUR,ILS"`, preventing HTTP 422 errors on JOD.
   - When requested base is JOD, line 55 sets `effectiveBase = "USD"`.
3. **Independent Test Execution**:
   - Executed command: `.\gradlew.bat :shared:jvmTest`
   - Test execution report at `shared/build/reports/tests/jvmTest/index.html`:
     * Total: **105 tests completed, 0 failed, 0 skipped (100% pass rate)**.
     * `CurrencyRepositoryTest[jvm]`: 7 tests, 0 failures.
     * `ConvertCurrencyUseCaseTest[jvm]`: 13 tests, 0 failures.
     * `Tier1FeatureCoverageTest[jvm]`: 32 tests, 0 failures.
     * `Tier2BoundaryCornerCaseTest[jvm]`: 36 tests, 0 failures.
     * `Tier3CrossFeatureCombinationTest[jvm]`: 9 tests, 0 failures.
     * `Tier4RealWorldScenarioTest[jvm]`: 6 tests, 0 failures.
     * `SharedCommonTest[jvm]`: 1 test, 0 failures.
     * `SharedLogicDesktopTest[jvm]`: 1 test, 0 failures.

### 1.2 Observed Defects & Integrity Findings

#### Observation A: Core Calculation Bug in `ConvertCurrencyUseCase.kt`
Lines 100–129 of `shared/src/commonMain/kotlin/org/moh/kash/project/domain/usecase/ConvertCurrencyUseCase.kt`:
```kotlin
100:     fun calculateRate(baseCode: String, targetCode: String, rates: Map<String, Double>): Double {
101:         val b = baseCode.trim().uppercase()
102:         val t = targetCode.trim().uppercase()
103: 
104:         if (b == t) return 1.0000
105: 
106:         // If rates already has the direct target rate and base matches:
107:         if (rates.containsKey(t) && !rates.containsKey("USD")) {
108:             return rates[t] ?: 1.0
109:         }
110: 
111:         // Standardize via USD base:
112:         // USD-relative rate for base
113:         val usdToB = when (b) {
114:             "USD" -> 1.0
115:             "JOD" -> JOD_USD_PEG
116:             else -> rates[b] ?: 1.0
117:         }
118: 
119:         // USD-relative rate for target
120:         val usdToT = when (t) {
121:             "USD" -> 1.0
122:             "JOD" -> JOD_USD_PEG
123:             else -> rates[t] ?: 1.0
124:         }
125: 
126:         // When base is JOD: R(JOD -> T) = R(USD -> T) / 0.7090
127:         // When target is JOD: R(B -> JOD) = (R(USD -> JOD) / R(USD -> B)) = 0.7090 / R(USD -> B)
128:         return usdToT / usdToB
129:     }
```
- In `CurrencyRepositoryImpl.kt` lines 86–107, `synthesizeRates` generates rates relative to the requested `baseCurrency`:
  * When `base == "EUR"`: `rates` contains `{"EUR": 1.0, "USD": 1.087, "ILS": 3.967, "JOD": 0.7707}`.
  * When `base == "ILS"`: `rates` contains `{"ILS": 1.0, "USD": 0.274, "EUR": 0.252, "JOD": 0.194}`.
- In both cases, `rates` contains `"USD"`. Thus, line 107 `if (rates.containsKey(t) && !rates.containsKey("USD"))` evaluates to `false`!
- The code falls through to lines 113–128:
  * For `b = "EUR"`, `usdToB = rates["EUR"] ?: 1.0 = 1.0`.
  * For `t = "USD"`, line 121 sets `usdToT = 1.0`.
  * Return value: `usdToT / usdToB = 1.0 / 1.0 = 1.0000`!
  * **Result**: Converting 100 EUR to USD yields **100.00 USD** (instead of 108.70 USD). Converting 100 ILS to USD yields **100.00 USD** (instead of 27.40 USD).
  * For `t = "JOD"`, line 122 sets `usdToT = JOD_USD_PEG = 0.7090`.
  * Return value: `usdToT / usdToB = 0.7090 / 1.0 = 0.7090`!
  * **Result**: Converting 100 EUR to JOD yields **70.90 JOD** (instead of 77.07 JOD).

#### Observation B: `AppError` Not Extending `Throwable` and Leaking Raw Ktor Exceptions
1. `shared/src/commonMain/kotlin/org/moh/kash/project/core/error/AppError.kt` lines 10–13:
```kotlin
10: sealed class AppError(
11:     open val message: String,
12:     open val cause: Throwable? = null
13: )
```
`AppError` is a plain sealed class and does not extend `Throwable` or `Exception`.
2. `shared/src/commonMain/kotlin/org/moh/kash/project/data/repository/CurrencyRepositoryImpl.kt` lines 68–72:
```kotlin
68:         } catch (e: Exception) {
69:             val appError = mapToAppError(e)
70:             // Propagate error wrapped in Result so UI can show Popup/Snackbar while retaining content
71:             emit(Result.failure(e))
72:         }
```
`val appError = mapToAppError(e)` is dead code (unused). Line 71 emits `Result.failure(e)` where `e` is the raw Ktor exception (`ClientRequestException`, `UnresolvedAddressException`), leaking transport exceptions through the data layer boundary into domain and presentation.

#### Observation C: Incomplete Test Coverage in `ConvertCurrencyUseCaseTest.kt`
- In `ConvertCurrencyUseCaseTest.kt` lines 13–18:
  All 13 tests use `sampleRates` where `USD` is the anchor (`USD = 1.0000`).
  There are zero tests for:
  - Converting from EUR to USD with positive amounts.
  - Converting from ILS to USD with positive amounts.
  - Converting using a rate map produced by `CurrencyRepositoryImpl` where base is EUR or ILS.
  - The only test touching EUR->USD is line 100 (`useCase(0.0, "EUR", Currency.USD, sampleRates)`), which passed solely because `amount = 0.0` short-circuited the output to `0.0`, masking the calculation defect.

---

## 2. Logic Chain

1. *From CurrencyRepository rate synthesis to ConvertCurrencyUseCase rate corruption*:
   - FreeCurrencyAPI provides rates relative to `base_currency`.
   - `CurrencyRepositoryImpl` properly synthesizes direct rates for EUR and ILS bases: for EUR, `rates["USD"] = 1.087` and `rates["JOD"] = 0.7707`.
   - When the user selects EUR as base currency and views rates or converts amounts, `ConvertCurrencyUseCase.invoke` delegates to `calculateRate`.
   - `calculateRate` assumes `rates` is always USD-anchored unless `!rates.containsKey("USD")`. Because USD is always included in Kash supported currencies, `!rates.containsKey("USD")` is always false.
   - Consequently, `calculateRate` divides `usdToT / usdToB`. Because `rates["EUR"] == 1.0`, `usdToB` is 1.0. For USD target, `usdToT` is hardcoded to 1.0. The rate returns `1.0 / 1.0 = 1.0000`.
   - For JOD target, `usdToT` is hardcoded to `0.7090`. The rate returns `0.7090 / 1.0 = 0.7090`.
   - **Conclusion**: The application displays corrupt conversion amounts whenever the user selects EUR or ILS as base currency.

2. *From AppError type hierarchy to Data Layer abstraction leak*:
   - Clean Architecture and `compose-data` require transport exceptions to be categorized and mapped at the repository boundary into domain errors.
   - Kotlin's `Result.failure(exception: Throwable)` requires a `Throwable`.
   - Because `AppError` does not extend `Throwable`/`Exception`, `CurrencyRepositoryImpl` cannot pass `appError` to `Result.failure()`.
   - Instead, `CurrencyRepositoryImpl` emits `Result.failure(e)` (raw Ktor exception) and discards `appError`.
   - **Conclusion**: Presentation layer / ViewModels will receive unhandled Ktor exceptions instead of domain `AppError`.

---

## 3. Caveats
- `FreeCurrencyApiClient` query sanitization, JOD peg derivations for USD base, DataStore caching, seed rates fallback, and build configurations are genuinely implemented and clean.
- The 105 tests currently in `jvmTest` pass cleanly, but the test suite has a blind spot that prevented it from detecting the EUR/ILS base conversion bug.

---

## 4. Conclusion & Required Actions

### Verdict: **REQUEST_CHANGES**

### Findings Summary
| ID | Severity | Component | Summary |
|---|---|---|---|
| **F-01** | **Critical** | `ConvertCurrencyUseCase.kt` | Corrupted exchange rate calculation when rate map is direct/base-specific (EUR/ILS base -> USD returns 1.0, -> JOD returns 0.7090). |
| **F-02** | **Major** | `AppError.kt`, `CurrencyRepositoryImpl.kt` | `AppError` does not extend `Exception`, causing dead code in `mapToAppError(e)` and leaking raw Ktor exceptions in `Result.failure(e)`. |
| **F-03** | **Major** | `ConvertCurrencyUseCaseTest.kt` | Missing test coverage for non-USD base rate maps (EUR->USD, ILS->USD, EUR->JOD). |

### Concrete Fixes Required:

#### Fix 1 (for F-01): Correct `ConvertCurrencyUseCase.calculateRate`
In `ConvertCurrencyUseCase.kt`:
```kotlin
fun calculateRate(baseCode: String, targetCode: String, rates: Map<String, Double>): Double {
    val b = baseCode.trim().uppercase()
    val t = targetCode.trim().uppercase()

    if (b == t) return 1.0000

    // 1. If rates map is already anchored to baseCode (rates[baseCode] == 1.0 or direct key present)
    if (rates.containsKey(t) && (rates[b] == 1.0 || b == "USD" || !rates.containsKey(b))) {
        // Direct rate from base to target is already in the map
        val direct = rates[t]
        if (direct != null && direct > 0.0) {
            return direct
        }
    }

    // 2. If rates is anchored to USD (rates["USD"] == 1.0):
    val usdToB = when (b) {
        "USD" -> 1.0
        "JOD" -> JOD_USD_PEG
        else -> rates[b] ?: 1.0
    }

    val usdToT = when (t) {
        "USD" -> 1.0
        "JOD" -> JOD_USD_PEG
        else -> rates[t] ?: 1.0
    }

    return if (usdToB > 0.0) usdToT / usdToB else 1.0
}
```

#### Fix 2 (for F-02): Make `AppError` extend `Exception` and emit `appError`
In `AppError.kt`:
```kotlin
sealed class AppError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause)
```
In `CurrencyRepositoryImpl.kt`:
```kotlin
} catch (e: Exception) {
    val appError = mapToAppError(e)
    emit(Result.failure(appError))
}
```

#### Fix 3 (for F-03): Add Unit Tests for EUR and ILS base conversions
In `ConvertCurrencyUseCaseTest.kt`, add tests:
- `testDirectConversionEurToUsdWithEurBaseRates`: with rates `{"EUR": 1.0, "USD": 1.087, "ILS": 3.967, "JOD": 0.7707}`, convert 100 EUR -> 108.70 USD.
- `testDirectConversionEurToJodWithEurBaseRates`: with same rates, convert 100 EUR -> 77.070 JOD.
- `testDirectConversionIlsToUsdWithIlsBaseRates`: with rates `{"ILS": 1.0, "USD": 0.274, "EUR": 0.252, "JOD": 0.194}`, convert 100 ILS -> 27.40 USD.

---

## 5. Verification Method

1. **Reproduction of Bug**:
   Evaluate `ConvertCurrencyUseCase().calculateRate("EUR", "USD", mapOf("EUR" to 1.0, "USD" to 1.087, "ILS" to 3.967, "JOD" to 0.7707))`.
   * Current output: `1.0000` (Bug).
   * Expected output: `1.0870`.
2. **Execute Full Test Suite**:
   ```powershell
   .\gradlew.bat :shared:jvmTest
   ```
   Must pass with 100% success rate including new tests for EUR and ILS base conversions.

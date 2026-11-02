# spec.md — Kash Functional & Technical System Specification

> **Project**: Kash (Compose Multiplatform Currency Exchange Application)  
> **Target Platforms**: Android (API 24+), Desktop JVM, iOS  
> **API Provider**: FreeCurrencyAPI (`https://api.freecurrencyapi.com/v1/`)  
> **Status**: Approved Technical Contract  

---

## 1. System Overview & Architecture

Kash is structured as an offline-first, clean-architecture Compose Multiplatform application adopting strict Model-View-Intent (MVI) unidirectional data flow.

```
+-------------------------------------------------------------------------+
|                           PRESENTATION LAYER                            |
|  - ExchangeScreen (Stateless Compose UI)                                |
|  - ExchangeViewModel (BaseViewModel<UiAction, UiState, UiEffect>)       |
|  - UI Components: BaseCurrencySelector, QuickAmountInput, RateCardList  |
+------------------------------------+------------------------------------+
                                     |
                                     v
+-------------------------------------------------------------------------+
|                              DOMAIN LAYER                               |
|  - Models: Currency, ExchangeRates, ConversionResult                    |
|  - Use Cases: ConvertCurrencyUseCase, GetExchangeRatesUseCase          |
|  - Math: Direct quotes, cross-rates, JOD Central Bank USD peg derivation|
+------------------------------------+------------------------------------+
                                     |
                                     v
+-------------------------------------------------------------------------+
|                               DATA LAYER                                |
|  - CurrencyRepositoryImpl (Cache-aside coordinator)                     |
|  - Remote: FreeCurrencyApiClient (Ktor 3.1.1, /v1/latest, /v1/status)    |
|  - Local: CurrencyPreferencesDataStore (TTL cache, base currency key)   |
|  - Fallback: SeedRates (Bundled static rates for zero-crash cold starts)|
+-------------------------------------------------------------------------+
```

---

## 2. External API Specification: FreeCurrencyAPI

### 2.1 Protocol & Authentication
- **Base URL**: `https://api.freecurrencyapi.com/v1/`
- **Security**: HTTPS TLS 1.2 / 1.3 enforced.
- **Authentication**: HTTP Header `apikey: <API_KEY>` or query parameter `?apikey=<API_KEY>`.
- **Default/Mock Mode**: When no user API key is provided, the client operates using bundled seed rates or a pre-configured key, ensuring development builds function out-of-the-box.

### 2.2 Endpoints

#### Endpoint A: Latest Exchange Rates (`GET /v1/latest`)
Retrieves exchange rates for supported currencies relative to a base currency.
- **Request Parameters**:
  | Parameter | Type | Required | Default | Description |
  |---|---|---|---|---|
  | `apikey` | String | Yes | Configured | FreeCurrencyAPI key |
  | `base_currency` | String | No | `USD` | ISO 4217 base currency code |
  | `currencies` | String | No | (all supported) | Comma-delimited list of target ISO codes (e.g. `USD,EUR,ILS`) |

- **Success Response (HTTP 200)**:
  ```json
  {
    "data": {
      "EUR": 0.91845,
      "ILS": 3.65420,
      "USD": 1.00000
    }
  }
  ```

- **HTTP Status Codes & Error Handling**:
  | HTTP Code | Error Condition | System Action | Error Tier |
  |---|---|---|---|
  | `200 OK` | Successful query | Parse rates, update DataStore cache with timestamp, emit `Success`. | N/A |
  | `401 Unauthorized` | Invalid / missing API key | Log warning, fall back to DataStore cache or seed rates, emit error effect. | Inline / Popup |
  | `422 Unprocessable Entity` | Unsupported currency requested | Guarded client-side: never send unsupported currencies (e.g. `JOD`) in request. | Prevented by design |
  | `429 Too Many Requests` | Monthly/rate quota reached | Catch exception, serve cached rates with warning banner: *"Rate limit reached. Showing cached data."* | Popup |
  | `5xx Server Error` | FreeCurrencyAPI outage | Catch exception, serve cached rates or seed fallback. | Popup / Inline |

#### Endpoint B: Account Quota & Status (`GET /v1/status`)
Zero-cost verification endpoint (does not consume monthly request quota).
- **Request Parameters**: `apikey` (Required)
- **Success Response (HTTP 200)**:
  ```json
  {
    "account_id": 12345678,
    "quotas": {
      "month": {
        "total": 1000,
        "used": 42,
        "remaining": 958
      }
    },
    "plan": {
      "name": "free",
      "status": "active"
    }
  }
  ```

---

## 3. Top Currencies Specification & JOD Peg Derivation

### 3.1 Currency Metadata Table
| ISO Code | Common Display | Name | Symbol | Native Symbol | Decimals | Flag Emoji | Desktop Safe Badge |
|---|---|---|---|---|---|---|---|
| **USD** | USD | US Dollar | `$` | `$` | 2 | 🇺🇸 | `$` (Circle Emerald) |
| **EUR** | EUR | Euro | `€` | `€` | 2 | 🇪🇺 | `€` (Circle Blue) |
| **ILS** | NIS / ILS | Israeli New Shekel | `₪` | `₪` | 2 | 🇮🇱 | `₪` (Circle Cyan) |
| **JOD** | JOD | Jordanian Dinar | `JD` | `د.أ.` | 3 | 🇯🇴 | `JD` (Circle Gold) |

### 3.2 The JOD Peg Derivation Architecture
- **Problem**: FreeCurrencyAPI free tier natively supports 33 global fiat currencies (including USD, EUR, and ILS). However, **JOD is not in FreeCurrencyAPI's free tier**, and passing `currencies=JOD` or `base_currency=JOD` triggers an unhandled `HTTP 422 Unprocessable Entity`.
- **Legal Peg Fact**: The Central Bank of Jordan officially pegs the Jordanian Dinar to the US Dollar at a fixed legal rate:
  $$1 \text{ USD} = 0.7090 \text{ JOD} \quad \Longleftrightarrow \quad 1 \text{ JOD} = \frac{1}{0.7090} \approx 1.4104372355... \text{ USD}$$
- **Mathematical Derivation Rules**:
  1. **Query Sanitization**: When querying FreeCurrencyAPI, Kash requests **only** native supported currencies:
     $$\text{Request: } \texttt{currencies=USD,EUR,ILS}$$
  2. **Scenario 1: Base Currency is USD, EUR, or ILS**:
     - Remote API returns rate relative to base $B$: $R(B \to \text{USD})$.
     - Kash derives the JOD rate via the fixed USD peg:
       $$R(B \to \text{JOD}) = R(B \to \text{USD}) \times 0.7090$$
     - *Example (Base = EUR, USD = 1.088)*:
       $$R(\text{EUR} \to \text{JOD}) = 1.088 \times 0.7090 = 0.771392$$
  3. **Scenario 2: Base Currency is JOD**:
     - Remote API does not accept `base_currency=JOD`.
     - Kash transparently queries FreeCurrencyAPI with `base_currency=USD`.
     - Kash computes cross-rates for all targets $T \in \{\text{USD}, \text{EUR}, \text{ILS}\}$ using exact mathematical division by the official peg $0.7090$ (preventing truncation drift on large financial transaction amounts up to $10^{12}$):
       $$R(\text{JOD} \to \text{USD}) = \frac{1.0}{0.7090} \approx 1.4104372355...$$
       $$R(\text{JOD} \to T) = \frac{R(\text{USD} \to T)}{0.7090} \quad \left(\text{or } \frac{1.0}{0.7090} \times R(\text{USD} \to T)\right)$$
       $$R(\text{JOD} \to \text{JOD}) = 1.0000$$
     - *Implementation Precision Mandate*: Implementations must perform exact floating/decimal division (`/ 0.7090` or `rate / 0.7090`) rather than multiplying by a truncated floating constant (`1.4104372`), ensuring zero drift across all calculation scales.
  4. **Outcome**: The user experiences 100% full support for JOD as both source and target currency with zero 422 errors, zero truncation drift, and zero added latency.

### 3.3 Desktop Cross-Platform Flag Glyph Handling
- **Android / iOS**: Modern mobile OS font stacks render standard Unicode flags (`\uD83C\uDDFA\uD83C\uDDF8` for 🇺🇸).
- **Desktop JVM (Windows)**: Windows typography font stacks lack color flag emojis and render two-letter regional indicator letters (`US`, `EU`, `IL`, `JO`).
- **Specification**: Kash UI implements a circular badge component (`FlagBadge`) that displays a styled circular emblem containing the official currency symbol (`$`, `€`, `₪`, `JD`) with the flag emoji as an optional accent, guaranteeing aesthetic beauty across all operating systems.

---

## 4. Domain Conversion Calculations & Formatting

### 4.1 Formulas & Calculation Rules
1. **Direct Currency Conversion**:
   $$\text{Converted Amount} = \text{Input Amount} \times R(\text{Base} \to \text{Target})$$
2. **Cross-Rate Conversion (between two non-base currencies $C_1, C_2$)**:
   $$R(C_1 \to C_2) = \frac{R(\text{Base} \to C_2)}{R(\text{Base} \to C_1)}$$
3. **Rounding Mode**: `RoundingMode.HALF_UP` (standard financial rounding).
4. **Precision Rules**:
   - **Exchange Rates**: Displayed formatted to **4 decimal places** (e.g. `3.6542`, `0.7090`).
   - **Converted Amounts (USD, EUR, ILS)**: Formatted to **2 decimal places** with comma thousands separators (e.g. `1,250.75`).
   - **Converted Amounts (JOD)**: Formatted to **3 decimal places** with comma thousands separators (e.g. `886.782`).
5. **Precision & Representation Distinction**:
   - **Internal Numeric Value**: Domain calculations maintain full unrounded `Double` precision (e.g. converting 100 JOD to USD produces `amount / 0.7090` $\approx 141.04372355...$).
   - **UI Formatted Output**: Presentation formatting strictly follows target currency decimal rules (USD, EUR, ILS format to 2 decimals `$141.04`; JOD formats to 3 decimals `141.044`).

### 4.2 Boundary & Input Validation Matrix
| Input Condition | Raw Input | Sanitized / Handled Value | Converted Amount | UI Feedback |
|---|---|---|---|---|
| **Standard Valid** | `"100"` or `"100.50"` | `100.0` / `100.5` | $\text{Amount} \times \text{Rate}$ | Normal display |
| **Zero Amount** | `"0"` or `"0.00"` | `0.0` | `0.00` (or `0.000` for JOD) | Valid state, no error banner |
| **Empty Input** | `""` | `0.0` | `0.00` (placeholder) | Clean display |
| **Negative Amount** | `"-50"` | Coerced to `0.0` or rejected | `0.00` | Inline field error: *"Amount must be positive"* |
| **Non-Numeric Characters**| `"12a.5"` or `"$50"` | Filtered to `"12.5"` / `"50"` | Correct computed value | Invalid characters stripped on input |
| **Multiple Decimals** | `"12.34.56"` | Filtered to `"12.3456"` | Single decimal preserved | Second decimal point ignored |
| **Extreme Large Amount** | `"> 10^12"` | Capped at 12 digits | Formatted with commas | Prevents floating point overflow |
| **Identical Currency** | Base == Target | Rate = `1.0000` | Exactly equal to input | Immediate local calculation |

---

## 5. Offline Caching, DataStore & Seed Rates

### 5.1 Cache-Aside Repository Flow
1. **Cache Read & Cold Launch Fallback**:
   - On ViewModel initialization, `CurrencyRepository` immediately reads cached rates from `Preferences DataStore`.
   - If cached rates exist: emits `ExchangeRates` with `isOffline = true` or `false` based on TTL (1 hour default).
   - If cache is empty (first fresh launch without network): loads bundled `SeedRates` immediately marked as fallback/offline (`isOffline = true`), displaying an offline warning banner so the UI renders without delay.
   - If both cache and seed rate loading fail completely (e.g. fatal store corruption), or if a manual retry fails to obtain any rates, the presentation layer enters the Inline Error Card state.
2. **Background Network Refresh**:
   - In parallel, repository triggers an asynchronous network fetch to FreeCurrencyAPI.
   - On success: serializes new rates to DataStore, updates cache timestamp, and emits fresh `ExchangeRates(isOffline = false)`.
   - On network failure (timeout, no connection, 429 quota): retains existing cached rates (or seed rates) in memory, emits `UiEffect.ShowSnackbar("Using cached rates: network unavailable")`.

### 5.2 Bundled Seed Rates (Fallback Snapshot)
```kotlin
object SeedRates {
    const val JOD_USD_PEG = 0.7090
    val BASE_USD = mapOf(
        "USD" to 1.0000,
        "EUR" to 0.9200,
        "ILS" to 3.6500,
        "JOD" to 0.7090
    )
}
```

---

## 6. MVI Presentation Contract & Error Tiers

### 6.1 MVI Contract Definition
```kotlin
@Immutable
data class ExchangeUiState(
    val baseCurrency: String = "USD",
    val inputAmount: String = "100.00",
    val rates: List<RateCardItem> = emptyList(),
    val isColdLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isCurrencyPickerOpen: Boolean = false,
    val amountError: String? = null,
    val error: AppError? = null,
    val lastUpdated: String? = null
)

@Immutable
data class RateCardItem(
    val currencyCode: String,
    val currencyName: String,
    val symbol: String,
    val unitRate: Double,
    val formattedRate: String,
    val convertedAmount: Double,
    val formattedConvertedAmount: String,
    val trendPercent: Double? = null
)

sealed interface ExchangeUiAction {
    data class SelectBaseCurrency(val currencyCode: String) : ExchangeUiAction
    data class ChangeAmount(val amount: String) : ExchangeUiAction
    data class SelectQuickAmount(val amount: Double) : ExchangeUiAction
    data class SetCurrencyPickerOpen(val isOpen: Boolean) : ExchangeUiAction
    data object ClearAmount : ExchangeUiAction
    data object RefreshRates : ExchangeUiAction
    data object RetryColdLoad : ExchangeUiAction
}

sealed interface ExchangeUiEffect {
    data class ShowSnackbar(val message: String) : ExchangeUiEffect
}
```

### 6.2 Error Tier Matrix
1. **Inline Tier (Fatal Cold Start / Corruption Failure)**:
   - Condition: Rate loading fails completely with neither cached rates nor bundled seed rates available, or on a failed manual cold-load retry when no rates can be rendered.
   - Behavior: Render error card with error icon, user-friendly explanation, and an actionable `Button("Retry")`. Note: On normal offline cold launches, bundled `SeedRates` are loaded with an offline warning banner rather than blocking the user with this full-card error.
2. **Popup Tier (Background Refresh Failure)**:
   - Condition: Pull-to-refresh or scheduled background refresh fails while valid rates are already visible on screen.
   - Behavior: Never clear or hide visible rates; emit `UiEffect.ShowSnackbar("Refresh failed. Displaying cached rates.")`.
3. **Field Tier (Input Error)**:
   - Condition: User enters negative numbers or malformed text into the amount field.
   - Behavior: Populate `ExchangeUiState.amountError` (e.g. *"Amount must be positive"*); display red error text below the input field while keeping the rest of the UI stable and functional. Do NOT set `ExchangeUiState.error`.
4. **Silent Tier (Background Polling)**:
   - Condition: Background status check or silent rate check fails.
   - Behavior: Log diagnostic warning; zero UI interruption.

---

## 7. Acceptance Criteria & Test Scenarios

- [ ] **AC-1 (Base Currency Selection)**: User can select USD, EUR, NIS, or JOD as base currency. All target rate cards recalculate instantly.
- [ ] **AC-2 (JOD Conversion Accuracy)**: Converting 100 USD to JOD yields strictly `70.900 JOD` (unrounded `70.9`). Converting 100 JOD to USD yields raw unrounded converted amount `amount / 0.7090` ($\approx 141.04372...$, within $141.044 \pm 0.001$), while UI formatted display adheres to target currency decimal precision (USD = 2 decimals `$141.04`, JOD = 3 decimals `141.044`).
- [ ] **AC-3 (Zero & Negative Handling)**: Inputting `"0"` displays `0.00` across all cards without errors. Negative input is rejected or coerced to 0 with an inline validation indicator.
- [ ] **AC-4 (Offline Resilience)**: App launched in Airplane mode renders bundled seed rates or cached DataStore rates with an offline badge. Zero app crashes.
- [ ] **AC-5 (API Quota Protection)**: FreeCurrencyAPI is queried with `currencies=USD,EUR,ILS` (omitting JOD) to avoid 422 errors, and queries are cached in DataStore with TTL.
- [ ] **AC-6 (Cross-Platform Compilation)**: Compiles and runs cleanly on Android (`:androidApp:assembleDebug`) and Desktop JVM (`:desktopApp:run`).

# Comprehensive Specification Analysis: Kash Currency Exchange App

**Project**: Kash — Compose Multiplatform Currency Exchange Application  
**Target Platforms**: Android, Desktop (JVM), iOS (KMP/CMP)  
**Author**: Spec Miner Subagent (`spec_miner_survey_1`)  
**Timestamp**: 2026-11-01T19:15:00Z  

---

## 1. FreeCurrencyAPI Specification

### 1.1 Overview & Authentication
- **Base URL**: `https://api.freecurrencyapi.com`
- **API Version**: `v1`
- **Protocol**: HTTPS (TLS 1.2 / 1.3 required)
- **Authentication**:
  - HTTP Header (Preferred): `apikey: <YOUR_API_KEY>`
  - Query Parameter: `?apikey=<YOUR_API_KEY>`
- **Security Rule**: API keys must never be committed to source code or logged. Use secure configuration (e.g., `local.properties`, BuildKonfig, or environment variables).

### 1.2 Endpoints Specification

#### A. Latest Exchange Rates (`GET /v1/latest`)
Returns the latest daily foreign exchange rates relative to a base currency.
- **Path**: `/v1/latest`
- **HTTP Method**: `GET`
- **Parameters**:
  | Parameter | Type | Required | Default | Description |
  |---|---|---|---|---|
  | `apikey` | String | Yes | - | API key |
  | `base_currency` | String | No | `USD` | 3-letter ISO 4217 code. Must be one of 33 supported currencies. |
  | `currencies` | String | No | (all 33) | Comma-separated list of target ISO codes (e.g. `USD,EUR,ILS`). |
- **Success Response (HTTP 200)**:
  ```json
  {
    "data": {
      "EUR": 0.91845,
      "ILS": 3.6542,
      "USD": 1.0
    }
  }
  ```
- **Error Responses**:
  - `401 Unauthorized`: Invalid or missing API key.
  - `422 Unprocessable Entity`: Invalid base currency or unsupported currency in list.
  - `429 Too Many Requests`: Rate limit or monthly quota exceeded.
  - `500 / 503 Server Error`: Upstream service outage.

#### B. API Status & Quota (`GET /v1/status`)
Returns account usage, remaining quota, and plan status.
- **Path**: `/v1/status`
- **HTTP Method**: `GET`
- **Quota Impact**: Zero-cost (does NOT count towards monthly request quota).
- **Parameters**: `apikey` (Required)
- **Success Response (HTTP 200)**:
  ```json
  {
    "account_id": 12345678,
    "quotas": {
      "month": {
        "total": 1000,
        "used": 42,
        "remaining": 958
      },
      "grace": {
        "total": 0,
        "used": 0,
        "remaining": 0
      }
    },
    "plan": {
      "name": "free",
      "interval": "month",
      "free": true,
      "status": "active"
    }
  }
  ```

#### C. Currencies Metadata (`GET /v1/currencies`)
Returns list of supported currencies with display names, symbols, and formatting info.
- **Path**: `/v1/currencies`
- **HTTP Method**: `GET`
- **Parameters**:
  - `apikey` (Required)
  - `currencies` (Optional comma-separated filter)
- **Success Response (HTTP 200)**:
  ```json
  {
    "data": {
      "USD": {
        "symbol": "$",
        "name": "US Dollar",
        "symbol_native": "$",
        "decimal_digits": 2,
        "rounding": 0,
        "code": "USD",
        "name_plural": "US dollars",
        "type": "fiat"
      },
      "EUR": {
        "symbol": "€",
        "name": "Euro",
        "symbol_native": "€",
        "decimal_digits": 2,
        "rounding": 0,
        "code": "EUR",
        "name_plural": "Euros",
        "type": "fiat"
      },
      "ILS": {
        "symbol": "₪",
        "name": "Israeli New Sheqel",
        "symbol_native": "₪",
        "decimal_digits": 2,
        "rounding": 0,
        "code": "ILS",
        "name_plural": "Israeli new sheqels",
        "type": "fiat"
      }
    }
  }
  ```

### 1.3 Rate Limits & Quotas
- **Monthly Limit**: 1,000 requests / month on the Free Tier.
- **Rate Limit Window**: 10 requests / minute.
- **Quota Consumption**: Only HTTP 200 responses consume quota. Calls to `/v1/status` and 4xx/5xx responses do not consume quota.

### 1.4 Error Schemas
- **401 Unauthorized**:
  ```json
  {
    "message": "Unauthorized",
    "error": {
      "code": "invalid_api_key",
      "message": "Unauthorized"
    }
  }
  ```
- **422 Validation Error**:
  ```json
  {
    "message": "Validation error",
    "errors": {
      "base_currency": ["The selected base currency is invalid."]
    }
  }
  ```
- **429 Rate / Quota Limit**:
  ```json
  {
    "message": "You have hit your rate limit or your monthly limit.",
    "error": {
      "code": "rate_limit_exceeded",
      "message": "You have hit your rate limit or your monthly limit."
    },
    "quota": {
      "limit": 1000,
      "used": 1000,
      "remaining": 0,
      "resets_at": 1775001600
    }
  }
  ```

---

## 2. Top Currencies Handling & Metadata

### 2.1 Currency Specifications
| ISO Code | Common Label | Display Name | Symbol | Native Symbol | Decimals | Flag (Unicode) | Alpha-2 | FreeCurrencyAPI Support |
|---|---|---|---|---|---|---|---|---|
| **USD** | USD | US Dollar | `$` | `$` | 2 | 🇺🇸 (`\uD83C\uDDFA\uD83C\uDDF8`) | `US` | **Native** (33 list) |
| **EUR** | EUR | Euro | `€` | `€` | 2 | 🇪🇺 (`\uD83C\uDDEA\uD83C\uDDFA`) | `EU` | **Native** (33 list) |
| **ILS** | NIS / Shekel | Israeli New Shekel | `₪` | `₪` | 2 | 🇮🇱 (`\uD83C\uDDEE\uD83C\uDDF1`) | `IL` | **Native** (33 list) |
| **JOD** | JD / Dinar | Jordanian Dinar | `JD` | `د.أ.` | 3 | 🇯🇴 (`\uD83C\uDDEF\uD83C\uDDF4`) | `JO` | **Not on free tier (422)** |

### 2.2 Crucial Architectural Finding: JOD Integration via Official USD Peg
- **The Problem**: FreeCurrencyAPI free tier supports only 33 currencies. Requesting `currencies=USD,EUR,ILS,JOD` or `base_currency=JOD` triggers an `HTTP 422 Unprocessable Entity` response.
- **The Economic Fact**: The Jordanian Dinar (JOD) is officially pegged to the US Dollar by the Central Bank of Jordan at a fixed exchange rate:
  $$1 \text{ USD} = 0.7090 \text{ JOD} \quad \iff \quad 1 \text{ JOD} = \frac{1}{0.7090} \approx 1.4104372 \text{ USD}$$
- **The Solution (Synthesis / Peg Strategy)**:
  1. The API repository queries FreeCurrencyAPI for native supported currencies only: `currencies=USD,EUR,ILS`.
  2. When the user selects `USD`, `EUR`, or `ILS` as base currency:
     $$R(\text{Base} \to \text{JOD}) = R(\text{Base} \to \text{USD}) \times 0.7090$$
  3. When the user selects `JOD` as the base currency:
     - The repository queries FreeCurrencyAPI with `base_currency=USD`.
     - Cross-rates are derived mathematically:
       $$R(\text{JOD} \to \text{USD}) = 1.4104372$$
       $$R(\text{JOD} \to \text{Target}) = 1.4104372 \times R(\text{USD} \to \text{Target})$$
  4. This provides 100% accurate, continuous real-time rates for JOD across all base currencies without triggering 422 errors or requiring paid API tiers.

### 2.3 Cross-Platform Flag Rendering
- **Android**: Supports standard Unicode country flags natively in text composables.
- **Desktop (JVM on Windows)**: Windows font stack does not render flag emoji glyphs, falling back to two-letter regional indicator symbols ("US", "EU", "IL", "JO").
- **UI Spec**: To ensure flawless rendering across both Android and Desktop, Kash should use circular badge components displaying either the currency symbol/code or vector graphics, with Unicode emoji as an enhancement.

---

## 3. Domain Conversion Calculations

### 3.1 Mathematical Formulations
1. **Direct Conversion**:
   $$\text{Converted Amount}(T) = \text{Amount}(B) \times R(B \to T)$$
2. **Cross-Rate Calculation (between two non-base currencies $C_1$ and $C_2$)**:
   $$R(C_1 \to C_2) = \frac{R(B \to C_2)}{R(B \to C_1)}$$
   $$\text{Converted Amount}(C_2) = \text{Amount}(C_1) \times \frac{R(B \to C_2)}{R(B \to C_1)}$$
3. **Rate Inversion**:
   $$R(T \to B) = \frac{1}{R(B \to T)}$$

### 3.2 Precision and Formatting Rules
- **Exchange Rates**: Stored as high-precision `Double`; displayed formatted to **4 decimal places** (e.g. `3.6542`, `0.7090`).
- **Converted Currency Amounts**:
  - Standard Currencies (USD, EUR, ILS): Formatted to **2 decimal places** with thousands separators (`1,234.56`).
  - Three-Decimal Currencies (JOD): Formatted to **3 decimal places** with thousands separators (`1,234.567`).
  - Rounding Mode: `RoundingMode.HALF_UP` (standard financial rounding).

### 3.3 Boundary & Input Edge Cases
| Input Case | Expected Input | Behavior / Calculation | State & Error Handling |
|---|---|---|---|
| Zero Amount | `"0"` or `"0.00"` | Converted amount is strictly `0.00` (or `0.000` for JOD). | Valid state. No error message. |
| Empty Input | `""` | Converted amounts display `0.00` or `-`. | Valid initial/cleared state. |
| Invalid Characters | `"12a.5"` or `"12..5"` | Sanitized / rejected by input filter. | Prevent input or show inline field error. |
| Negative Amount | `"-50"` | Invalid for exchange operations. | Coerce to `0` or trigger input validation error. |
| Very Large Amount | `"> 10^12"` | Guard against floating point overflow / NaN. | Limit input length (e.g., max 12 digits). |
| Self-Conversion | Base == Target | $R(B \to B) = 1.0000$, Converted = Amount. | Instant calculation without network call. |

### 3.4 Offline Fallback & Cache Behavior
- **Offline First Strategy**:
  1. On successful network fetch: Store latest rates and UTC timestamp in local storage (`Preferences DataStore`).
  2. On network failure (no connection, timeout, 5xx, or 429 quota):
     - If local cache is present: Serve cached rates, emit non-blocking banner/toast: *"Showing cached rates from [Formatted Date/Time]"*.
     - If local cache is absent (first cold launch offline): Use bundled seed rates:
       - USD: `1.0000`
       - EUR: `0.9200`
       - ILS: `3.6500`
       - JOD: `0.7090`
       and display an inline warning indicating fallback rates.
     - If a fatal error occurs: Inline error state with a dedicated "Retry" button.

---

## 4. AI SDLC & SPEC Kit Specifications

### 4.1 Core AI SDLC Artifacts
The SPEC Kit standardizes the progression from human intent to verified code:
1. **`intent.md`**: Captures business goals, user personas, high-level features, and value proposition. Authored collaboratively with the Product Manager.
2. **`spec.md`**: Formal specification and technical requirements contract. Details functional rules, API schemas, UI/UX requirements, error states, and acceptance criteria.
3. **`plan.md`**: Architectural execution plan. Outlines file layout, dependency additions, technical trade-offs, and step-by-step milestones.
4. **`AGENTS.md`**: Context engineering configuration defining repository rules, build/test commands, module conventions, and sub-agent swarm roles.

### 4.2 Specialized Sub-Agent Swarm Roles
1. **Coding / Architecture Agent**:
   - Implements KMP/CMP shared modules (`data/`, `domain/`, `presentation/`).
   - Ensures strict compliance with MVI architecture (`BaseViewModel`, `launchGuarded`), clean DTO/domain separation, and Ktor client best practices.
2. **UI/UX Design Agent**:
   - Designs Material 3 design tokens, typography, and cohesive color palette.
   - Builds responsive layouts, animations (fade, scale, rate transitions), custom app logo, presentation deck, marketing kit, and Google Play Store kit.
3. **Testing & QA Agent**:
   - Creates comprehensive unit tests for domain calculation math, cross-rate formulas, boundary conditions, and DTO mappers.
   - Implements repository mock tests (using Ktor `MockEngine`) and Compose UI tests.
4. **Security Agent**:
   - Audits API key handling (ensuring keys are externalized and not exposed in VCS).
   - Validates HTTPS enforcement, input sanitization against malformed input, and safe error masking.
5. **Performance Agent**:
   - Optimizes Compose recomposition (immutable parameters, `derivedStateOf`, list keys).
   - Enforces rate caching and debounced input handling to conserve network requests.
6. **Memory Agent**:
   - Validates coroutine lifecycle scoping (`viewModelScope`), avoids memory leaks across configuration changes, and ensures bounded in-memory cache size.
7. **Multi-Platform Agent**:
   - Assures portability across Android, Desktop JVM, and iOS. Verifies zero reliance on `java.*` or `android.*` in shared commonMain code.

### 4.3 End-to-End Workflow: Antigravity to Android Studio
```
+-----------------------------------------------------------------------------------+
|                           AI SDLC Workflow (A to Z)                                |
+-----------------------------------------------------------------------------------+
|  [Phase A] Intent Definition (intent.md)                                          |
|    - Define problem statement, target currencies (USD, EUR, NIS, JOD), user goals |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|  [Phase B] Specification & Context Engineering (spec.md, AGENTS.md)                |
|    - FreeCurrencyAPI endpoints, JOD peg math, MVI contract, swarm definitions    |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|  [Phase C] Architecture & Planning (plan.md)                                      |
|    - Layering, version catalog additions (Ktor, DataStore), execution tasks       |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|  [Phase D] TDD Domain & Data Layer Implementation                                  |
|    - Domain models, calculation math, unit tests, Ktor client, DataStore cache   |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|  [Phase E] UI/UX Presentation & Store Assets                                      |
|    - Material 3 theme, Currency Cards, animations, Logo, Play Store kit           |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|  [Phase F] Multi-Platform Testing & Studio Verification                            |
|    - Android Studio debug assemble, Desktop JVM execution, automated test runs   |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|  [Phase G] Security, Performance & Release Review                                 |
|    - Security audit, memory profiling, final handoff to user                      |
+-----------------------------------------------------------------------------------+
```

---

## 5. Compose Kit Architecture Alignment

### 5.1 Layering & Package Structure (`:shared`)
Following `compose-architecture` and `compose-data` skill conventions:
```
shared/src/commonMain/kotlin/org/moh/kash/project/
├── core/
│   ├── error/
│   │   ├── AppError.kt
│   │   └── NetworkException.kt
│   └── presentation/
│       ├── BaseViewModel.kt
│       └── Contract.kt
├── data/
│   ├── local/
│   │   └── RatesDataStore.kt
│   ├── remote/
│   │   ├── api/
│   │   │   ├── FreeCurrencyApiService.kt
│   │   │   └── KtorClientFactory.kt
│   │   ├── mapper/
│   │   │   └── ExchangeRateMapper.kt
│   │   └── model/
│   │       ├── CurrenciesDto.kt
│   │       ├── LatestRatesDto.kt
│   │       └── StatusDto.kt
│   └── repository/
│       └── ExchangeRateRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   ├── ConversionResult.kt
│   │   ├── Currency.kt
│   │   └── ExchangeRates.kt
│   └── repository/
│       └── ExchangeRateRepository.kt
└── presentation/
    ├── components/
    │   ├── AmountInputField.kt
    │   ├── CurrencyCard.kt
    │   ├── CurrencySelectorSheet.kt
    │   └── FlagBadge.kt
    ├── exchangerate/
    │   ├── ExchangeRateContract.kt
    │   ├── ExchangeRateScreen.kt
    │   └── ExchangeRateViewModel.kt
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

### 5.2 Non-Negotiable Kit Rules Checked
1. **DTOs internal**: All wire DTOs (`LatestRatesDto`, `StatusDto`, `CurrenciesDto`) are declared `internal` in `data/remote/model/` and mapped to domain models.
2. **Ktor client**: `expectSuccess = true` configured. Non-2xx mapped to typed `NetworkException`.
3. **MVI BaseViewModel**: Single entry point `onAction()`. Sealed `UiState`, `UiAction`, `UiEffect`.
4. **launchGuarded**: All coroutines use `launchGuarded(onError = ...)` with explicit error tiers.
5. **No Android/Java imports in commonMain**: Pure multiplatform Kotlin libraries used.
6. **DataStore**: Preferences DataStore used for caching rates JSON and base currency preference.

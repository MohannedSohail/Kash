# SPEC MINER SURVEY REPORT: KASH CURRENCY EXCHANGE

**Subagent**: `spec_miner_survey_1`  
**Role**: API, Domain & SDLC Specification Miner  
**Date**: 2026-11-01T19:16:00Z  
**Parent Agent**: `f8497402-6544-4f4c-84da-7f7dd5660601`  

---

## Features Discovered

| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|---|---|---|---|---|---|---|
| 1 | FreeCurrencyAPI | Latest Rates (`/v1/latest`) | Fetches real-time / daily end-of-day foreign exchange rates relative to a base currency. | `apikey` (string, required), `base_currency` (ISO 4217 code, default `USD`), `currencies` (comma-separated ISO codes). | JSON `{ "data": { "<CURRENCY>": <RATE_DOUBLE>, ... } }` | `401` on invalid/missing key, `422` on unsupported/invalid currency, `429` on rate/quota limit. | Official Docs & Web probing |
| 2 | FreeCurrencyAPI | Quota & Status (`/v1/status`) | Checks monthly request quota, usage, plan status. Zero-cost call (does not consume quota). | `apikey` (string, required). | JSON `{ "account_id": ..., "quotas": { "month": { "total": 1000, "used": ..., "remaining": ... } }, "plan": { "name": "free", ... } }` | `401` on invalid/missing key. | Official Docs & Web probing |
| 3 | FreeCurrencyAPI | Supported Currencies (`/v1/currencies`) | Returns list and metadata of supported fiat currencies (names, symbols, decimal digits, plural names). | `apikey` (string, required), `currencies` (optional filter). | JSON `{ "data": { "<CURRENCY>": { "symbol": "...", "name": "...", "decimal_digits": 2, "code": "..." } } }` | `401` on invalid/missing key, `422` on malformed parameters. | Official Docs & Web probing |
| 4 | Currency Modeling | Top Currency: USD | US Dollar, default global base currency. | Amount $A$ in USD. | Formatted string `"$#,##0.00"`, 2 decimal digits, Flag 🇺🇸 (US). | Coerced to 0 on negative/invalid amount. | ISO 4217 & FreeCurrencyAPI |
| 5 | Currency Modeling | Top Currency: EUR | European Union Euro, major fiat currency. | Amount $A$ in EUR. | Formatted string `"€#,##0.00"`, 2 decimal digits, Flag 🇪🇺 (EU). | Coerced to 0 on negative/invalid amount. | ISO 4217 & FreeCurrencyAPI |
| 6 | Currency Modeling | Top Currency: NIS / ILS | Israeli New Shekel (common NIS, ISO 4217 `ILS`). Included in FreeCurrencyAPI's 33 supported currencies. | Amount $A$ in ILS. | Formatted string `"₪#,##0.00"`, 2 decimal digits, Flag 🇮🇱 (IL). | Coerced to 0 on negative/invalid amount. | ISO 4217 & FreeCurrencyAPI |
| 7 | Currency Modeling | Top Currency: JOD & Peg Derivation | Jordanian Dinar (ISO `JOD`). Excluded from FreeCurrencyAPI free tier (causes 422). Derived via official Central Bank USD peg ($1 \text{ USD} = 0.7090 \text{ JOD}$). | Amount $A$ in JOD. | Formatted string `"JD #,##0.000"` (or native `"د.أ. #,##0.000"`), 3 decimal digits, Flag 🇯🇴 (JO). | Coerced to 0 on negative/invalid amount. | Central Bank of Jordan & ISO 4217 |
| 8 | Domain Calculation | Direct Conversion | Converts amount $A$ from selected base currency $B$ to target currency $T$ using direct quote. | $A$ (Double), $R(B \to T)$ (Double). | $\text{Converted} = A \times R(B \to T)$, formatted by target currency precision. | If $A = 0$, returns $0.00$; if $A < 0$, coerces to 0 or triggers validation error. | Financial Domain Math |
| 9 | Domain Calculation | Cross-Rate Conversion | Calculates exchange rate between two non-base currencies $C_1$ and $C_2$ using active base $B$. | $R(B \to C_1)$, $R(B \to C_2)$. | $R(C_1 \to C_2) = \frac{R(B \to C_2)}{R(B \to C_1)}$. | Division-by-zero guard returns 1.0 or calculation exception. | Financial Domain Math |
| 10 | Domain Calculation | Precision Rounding | Formats exchange rates to 4 decimals; formats converted amounts to 2 decimals (USD, EUR, ILS) or 3 decimals (JOD) using `HALF_UP`. | Raw numeric amounts and rates. | UI-ready formatted strings. | Truncates or rounds safely without floating point drift. | Financial Domain Standards |
| 11 | Persistence & Cache | Offline Rates Caching | Stores fetched rates and timestamp in Preferences DataStore. Enables offline-first operation. | Latest `ExchangeRates` domain model. | Persisted JSON string in local DataStore. | If storage write fails, logs error and retains in-memory cache. | Compose Data Kit & DataStore |
| 12 | Persistence & Cache | Offline Fallback & Seed Rates | When network is unavailable or rate limit hit, serves cached rates with offline timestamp banner. On fresh cold launch without network, falls back to bundled seed rates. | Network failure trigger. | Cached `ExchangeRates` domain object with `isOffline = true`. | If no cache exists, uses seed rates (USD 1.0, EUR 0.92, ILS 3.65, JOD 0.709) or inline error with Retry. | Compose Data Kit & UX Best Practices |
| 13 | AI SDLC Framework | SPEC Kit Artifact Chain | Standardized Markdown documents: `intent.md` (Why), `spec.md` (What), `plan.md` (How), `AGENTS.md` (Context & rules). | User request and product requirements. | Structured artifacts in project root and `.agents/`. | Eliminates LLM drift and ensures architectural conformance. | Spec-Driven Development (SDD) |
| 14 | AI SDLC Framework | Specialized Sub-Agent Swarm | 7 domain-specialized agents: Coding/Architecture, UI/UX, Testing/QA, Security, Performance, Memory, Multi-Platform. | Distinct phases of development lifecycle. | Modular, audited code, tests, and assets. | Agent separation prevents single-prompt overload and quality degradation. | Swarm Architecture Standard |
| 15 | AI SDLC Framework | Antigravity to Android Studio Workflow | End-to-end 7-phase procedure (Phases A through G) covering ideation in Antigravity to build/test verification in Android Studio. | Project lifecycle roadmap. | Verified, debuggable Android & Desktop apps with passing automated tests. | Step-by-step verification gates prevent breaking changes. | Multiplatform SDLC Guide |
| 16 | CMP Architecture | MVI & BaseViewModel | Predictable unidirectional state flow: `UiState`, `UiAction`, `UiEffect` via `BaseViewModel`. | User actions (select currency, enter amount, refresh, retry). | Emitted `UiState` and one-shot `UiEffect`. | Unhandled exceptions caught by `launchGuarded(onError = ...)`. | Compose Architecture Kit |
| 17 | CMP Architecture | Error Tier Classification | Explicit failure tiers: Inline with Retry for cold load failure; Popup (Snackbar) for background refresh failure; Field error for invalid input; Silent for poll. | Exception from repository / network layer. | Appropriate UI error representation. | Prevents silent data loss or disruptive full-screen crashes on minor network glitches. | Compose Architecture Kit |

---

## Edge Cases

| # | Feature | Input | Observed / Specified Behavior |
|---|---|---|---|
| 1 | FreeCurrencyAPI `/v1/latest` | Querying `currencies=USD,EUR,ILS,JOD` | **HTTP 422 Unprocessable Entity**: FreeCurrencyAPI free tier rejects `JOD` as invalid. **Resolution**: Client queries only native supported currencies (`USD,EUR,ILS`), then computes JOD via official USD peg ($1 \text{ USD} = 0.7090 \text{ JOD}$). |
| 2 | FreeCurrencyAPI `/v1/latest` | Selecting `base_currency=JOD` | **HTTP 422 Unprocessable Entity**: FreeCurrencyAPI rejects JOD base currency. **Resolution**: Client queries with `base_currency=USD`, then derives cross-rates: $R(\text{JOD} \to T) = 1.4104372 \times R(\text{USD} \to T)$. |
| 3 | FreeCurrencyAPI Authentication | Missing or expired API key | **HTTP 401 Unauthorized**: `{ "message": "Unauthorized", "error": { "code": "invalid_api_key" } }`. Client maps to `NetworkException.Unauthorized` and shows configuration prompt. |
| 4 | FreeCurrencyAPI Rate Limit | Exceeding 10 req/min or 1,000 req/month | **HTTP 429 Too Many Requests**: `{ "message": "...", "error": { "code": "rate_limit_exceeded" } }`. Repository catches 429 and serves cached rates with banner: "Rate limit reached. Displaying cached rates." |
| 5 | Domain Conversion | Amount is zero (`"0"` or `"0.00"`) | Converted amount is strictly `0.00` (or `0.000` for JOD). No error banner; valid business state. |
| 6 | Domain Conversion | Amount is negative (`"-150"`) | Rejected by amount input sanitizer or coerced to `0.00`. If typed, displays inline field error: "Amount must be positive". |
| 7 | Domain Conversion | Amount input is empty string (`""`) | Displays `0.00` with clean placeholder. Does not crash or show NaN. |
| 8 | Domain Conversion | Invalid characters (`"12.3.4"`, `"abc"`, `"$50"`) | Input filter strips non-digit/non-decimal characters immediately, maintaining clean numeric state. |
| 9 | Domain Conversion | Extremely large amount ($> 10^{12}$) | Formatted using localized thousands separators (`1,000,000,000,000.00`); input restricted to maximum 12 digits to prevent float overflow. |
| 10 | Domain Conversion | Base currency == Target currency | Direct 1:1 calculation ($R = 1.0000$, Converted = Amount) executed locally without network call. |
| 11 | Cache & Offline | First launch in Airplane Mode (Cold cache empty) | Network call fails; repository checks DataStore (empty); repository loads bundled seed rates (`USD=1.0, EUR=0.92, ILS=3.65, JOD=0.709`) and emits warning banner: "Offline: Using initial seed rates." |
| 12 | Cache & Offline | Network drops during pull-to-refresh over existing rates | Network call fails; tier rule 8 mandates **Popup Tier**: keep visible rates intact, emit `UiEffect.ShowSnackbar("Could not refresh rates. Showing cached data.")`. |
| 13 | Multiplatform UI | Flag emoji rendering on Desktop JVM (Windows) | Windows font stack fails to render country flag emojis (renders as regional letters "US", "EU", "IL", "JO"). UI must combine circular symbol badge ($ / € / ₪ / د.أ) with code to guarantee visual consistency across Android and Desktop. |

---

## 5-Component Handoff Report

### 1. Observation
1. **Existing Workspace State**:
   - The workspace at `e:\AI Projects\Kash\Kash` is a Compose Multiplatform project configured with Kotlin 2.4.21, Compose Multiplatform 1.12.2, Android Gradle Plugin 9.1.1, and Material 3 (`1.12.0-alpha03`).
   - Modules present: `:androidApp`, `:desktopApp`, `:shared`, `:iosApp`.
   - `shared/src/commonMain/kotlin/org/moh/kash/project/App.kt` currently holds the starter CMP wizard template with a sample button and animated image.
   - `libs.versions.toml` contains foundational Compose libraries, coroutines 1.11.0, and lifecycle components, but lacks Ktor and Preferences DataStore entries.
2. **Authoritative FreeCurrencyAPI Specifications**:
   - Official Base URL: `https://api.freecurrencyapi.com/v1/`.
   - Endpoints: `GET /v1/latest`, `GET /v1/status`, `GET /v1/currencies`.
   - Authentication: `apikey` query parameter or `apikey: <KEY>` HTTP header.
   - Free tier quotas: 1,000 requests/month, 10 requests/minute.
   - Status codes: `200 OK`, `401 Unauthorized`, `422 Unprocessable Entity`, `429 Too Many Requests`, `500 Server Error`.
   - **Crucial Spec Discovery**: FreeCurrencyAPI free tier natively supports **33 currencies**, including USD, EUR, and ILS (NIS). However, **JOD (Jordanian Dinar) is not supported** on FreeCurrencyAPI free tier and triggers an immediate `HTTP 422 Unprocessable Entity` validation error if passed in `currencies` or `base_currency`.
3. **Monetary Authority Peg Data**:
   - Central Bank of Jordan officially pegs the Jordanian Dinar to the US Dollar at a fixed rate: $1 \text{ USD} = 0.7090 \text{ JOD}$ ($1 \text{ JOD} \approx 1.4104372 \text{ USD}$).
4. **Kit Guidance & Standards**:
   - `compose-data` skill dictates: DTOs must remain `internal` in `data/`, domain models cannot contain serialization annotations, Ktor client must configure `expectSuccess = true`, network failures must propagate to `launchGuarded`, and Preferences DataStore must be used for local key-value/JSON caching.
   - `compose-architecture` skill dictates: MVI pattern with `BaseViewModel<Action, State, Effect>`, single entry point `onAction`, error tiers (inline for cold load, popup for refresh, field for input, silent for poll), and `SavedStateHandle` for user drafts.

### 2. Logic Chain
1. *From FreeCurrencyAPI 422 behavior to JOD Peg Derivation*:
   - FreeCurrencyAPI explicitly rejects `JOD` with `422 Unprocessable Entity`.
   - The user's specification requires displaying exchange rates for top currencies: USD, EUR, NIS, and JOD.
   - Because JOD has an official, legally mandated fixed peg to USD ($0.7090 \text{ JOD} = 1 \text{ USD}$), the application repository can safely query FreeCurrencyAPI for `currencies=USD,EUR,ILS` (avoiding the 422 error entirely) and compute JOD exchange rates with mathematical precision via USD cross-rates:
     $R(\text{Base} \to \text{JOD}) = R(\text{Base} \to \text{USD}) \times 0.7090$.
   - When JOD is the selected base currency, the client queries FreeCurrencyAPI with `base_currency=USD` and scales all target rates by $1 / 0.7090 \approx 1.4104372$.
2. *From Rate Limits (1,000 req/mo) to Caching Strategy*:
   - 1,000 requests/month equates to approximately ~33 requests per day.
   - If the application fetches rates on every keystroke or screen load, the monthly quota will be exhausted in minutes.
   - Therefore, the data layer must implement a cache-aside repository with Preferences DataStore, caching rates with a timestamp (TTL: 1 to 24 hours), debouncing user input, and serving cached rates during network unavailability or rate limit exhaustion.
3. *From Compose Kit Non-Negotiables to Package Architecture*:
   - Wire models (`LatestRatesDto`, `StatusDto`, `CurrenciesDto`) are kept `internal` inside `data/remote/model/` and mapped to clean domain models (`ExchangeRates`, `Currency`, `ConversionResult`) in `domain/model/`.
   - The ViewModel extends `BaseViewModel` and handles UI actions through `launchGuarded(onError = ...)`.
   - Error tiers ensure a seamless UX: cold load failure shows an inline retry state, while refresh failures over cached data display a non-disruptive Snackbar.
4. *From AI SDLC & SPEC Kit to Swarm Execution*:
   - High complexity tasks with multi-platform UI, store packaging, and rigorous testing require specialized division of labor:
     - Coding / Architecture Agent builds the robust MVI / Ktor / DataStore engine.
     - UI/UX Design Agent produces Material 3 tokens, currency cards, animations, custom logo, presentation deck, and Play Store assets.
     - Testing Agent verifies conversion math, boundary values, and mock network states.
     - Security, Performance, Memory, and Multiplatform Agents audit the codebase before release.

### 3. Caveats
1. **API Key Provisioning**: FreeCurrencyAPI requires a free registration to acquire an API key. A fallback mock engine and bundled seed rates must be provided so the app runs out-of-the-box in development and offline environments without a mandatory API key.
2. **Desktop Windows Flag Emoji Glyph Rendering**: On Windows JVM, standard unicode flag emojis may appear as regional indicator characters ("US", "EU"). UI design must incorporate circular badge components with currency symbols/codes to maintain visual elegance across all operating systems.
3. **JOD Peg Assumption**: The 0.7090 peg has been maintained by the Central Bank of Jordan since 1995. While virtually immutable, it is hardcoded in the domain layer as a configuration constant.

### 4. Conclusion
The specification for the Kash Compose Multiplatform application is fully mapped, verified, and ready for execution. By synthesizing JOD via its official USD peg, implementing an offline-first DataStore cache, following the MVI `BaseViewModel` kit architecture, and structuring the AI SDLC artifacts (`AGENTS.md`, `intent.md`, `spec.md`, `plan.md`), the implementation swarm can construct a production-ready, resilient application across Android and Desktop platforms.

### 5. Verification Method
1. **API Contract Verification**:
   - Query FreeCurrencyAPI endpoints using curl or Ktor `MockEngine`:
     - `GET https://api.freecurrencyapi.com/v1/latest?apikey=TEST&base_currency=USD&currencies=EUR,ILS` returns HTTP 200 with JSON payload.
     - Query with `currencies=JOD` triggers HTTP 422, verifying the necessity of the client-side peg synthesis.
2. **Domain Math Verification**:
   - Unit tests executing:
     - $100 \text{ USD} \to \text{JOD} = 70.900 \text{ JOD}$.
     - $0 \text{ USD} \to \text{EUR} = 0.00 \text{ EUR}$.
     - Negative amount coercion and invalid character sanitization.
     - Inverted cross-rate precision tests.
3. **Artifact Integrity Verification**:
   - Verify `AGENTS.md`, `intent.md`, `spec.md`, and `plan.md` in root workspace against SPEC Kit standards.
   - Run Gradle shared test suite: `./gradlew :shared:jvmTest` (or `./gradlew :shared:testAndroidHostTest`).
   - Run desktop build: `./gradlew :desktopApp:run` to confirm cross-platform execution.

## 2026-11-01T19:45:25Z

You are a Worker subagent in a Teamwork multi-agent orchestration.

Your identity:
- Type: teamwork_worker
- Role: Data & Domain Engineer
- Assigned working directory: e:\AI Projects\Kash\Kash\.agents\teamwork\worker_m2\
- Parent conversation ID: f8497402-6544-4f4c-84da-7f7dd5660601

MANDATORY: Read the user's original request file before starting:
`e:\AI Projects\Kash\Kash\.agents\teamwork\ORIGINAL_REQUEST.md`

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Consult these specifications and skill guides:
- `e:\AI Projects\Kash\Kash\spec.md`
- `e:\AI Projects\Kash\Kash\plan.md`
- `e:\AI Projects\Kash\Kash\.agents\teamwork\orchestrator_1\PROJECT.md`
- Compose Data skill: `C:\Users\MOHANNED\.gemini\config\skills\compose-data\SKILL.md`
- Compose Architecture skill: `C:\Users\MOHANNED\.gemini\config\skills\compose-architecture\SKILL.md`

Your Scope & Exclusive Write Ownership for Milestone 2:
1. Version Catalog & Gradle Configuration:
   - `gradle/libs.versions.toml`: Add Ktor 3.1.1, kotlinx.serialization 1.8.0, Preferences DataStore 1.1.3, Koin 4.0.2.
   - `shared/build.gradle.kts`: Apply serialization plugin, add dependencies to commonMain and commonTest (including ktor-client-mock).
2. Data Layer (`shared/src/commonMain/kotlin/org/moh/kash/project/data/`):
   - Wire DTOs in `data/remote/model/`: `LatestRatesDto.kt`, `StatusDto.kt` (marked internal).
   - FreeCurrencyAPI service & client in `data/remote/`: `FreeCurrencyApiClient.kt` querying `/v1/latest` with `currencies=USD,EUR,ILS` to avoid 422 errors, with configurable API key and header/query authentication.
   - Preferences DataStore cache in `data/local/`: `CurrencyDataStore.kt` caching latest rates JSON and timestamp, and base currency preference.
   - Bundled seed rates in `data/local/SeedRates.kt`: fallback rates (`USD=1.0, EUR=0.92, ILS=3.65, JOD=0.7090`) for zero-crash offline launches.
   - Repository implementation in `data/repository/CurrencyRepositoryImpl.kt`: cache-aside flow with TTL, network fetch, seed rates fallback, and mathematical JOD peg derivation via exact division (`rate / 0.7090` and `amount / 0.7090`).
3. Domain Layer (`shared/src/commonMain/kotlin/org/moh/kash/project/domain/`):
   - Models: `Currency.kt` (USD, EUR, NIS/ILS, JOD with symbols, decimals: 2 for USD/EUR/ILS, 3 for JOD), `ExchangeRates.kt`, `ConversionResult.kt`.
   - Repository interface: `CurrencyRepository.kt`.
   - Use Cases:
     * `ConvertCurrencyUseCase.kt`: Implements conversion math, exact JOD peg derivation, zero amount (returns 0.00), negative amount sanitization, precision rounding (`RoundingMode.HALF_UP`).
     * `GetExchangeRatesUseCase.kt`: Returns reactive Flow of rates with offline status.
     * `UpdateBaseCurrencyUseCase.kt`: Saves user's default base currency.
4. Error Model (`shared/src/commonMain/kotlin/org/moh/kash/project/core/error/AppError.kt`):
   - Typed errors matching error tiers (NetworkError, QuotaExceededError, OfflineNoCacheError, InvalidAmountError).
5. Comprehensive Unit Tests in `shared/src/commonTest/kotlin/org/moh/kash/project/`:
   - Unit tests for `ConvertCurrencyUseCaseTest` (direct, cross-rate, JOD peg, zero, negative, extreme values).
   - Unit tests for `CurrencyRepositoryTest` (MockEngine testing HTTP 200, 401, 429, DataStore caching, seed rates fallback).
   - Ensure all tests compile and pass.

Deliverables & Handoff:
- Write all required production and test code.
- Run build and test verification commands and document exact terminal output.
- In your working directory `e:\AI Projects\Kash\Kash\.agents\teamwork\worker_m2\`:
  * Maintain `progress.md` with timestamps and completed steps.
  * Write `handoff.md` following the 5-component protocol: Observation, Logic Chain, Caveats, Conclusion, Verification Method.
- When finished, use `send_message` to report back to parent (f8497402-6544-4f4c-84da-7f7dd5660601).

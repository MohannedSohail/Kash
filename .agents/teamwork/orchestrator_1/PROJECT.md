# Project: Kash (Compose Multiplatform Currency Exchange Application)

## Architecture
- **Paradigm**: Compose Multiplatform (CMP / KMP) targeting Android, Desktop (JVM), and iOS.
- **Presentation**: Strict MVI (`UiState`, `UiAction`, `UiEffect`) via `BaseViewModel`.
- **Domain**: Pure Kotlin multiplatform business models (`Currency`, `ExchangeRates`, `ConversionResult`), use cases (`ConvertCurrencyUseCase`, `GetExchangeRatesUseCase`), and precision math.
- **Data**: Ktor 3.1.1 HTTP client with ContentNegotiation (JSON), internal wire DTOs, offline cache-aside repository with Preferences DataStore and bundled seed rates.
- **UI/UX**: Material 3 design system with custom emerald/mint/slate/amber tokens, tabular typography, Compose Canvas logo, smooth animations (`AnimatedContent`, `graphicsLayer` pulse, `animateItem`), and visual/store marketing assets.
- **AI SDLC**: Full SPEC Kit workflow (`AGENTS.md`, `intent.md`, `spec.md`, `plan.md`) defining the 7 specialized sub-agent swarm roles and Antigravity-to-Android Studio procedures.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | AI SDLC Swarm Roles | Define 7 specialized sub-agents (coding, UI/UX, testing, security, performance, memory, multi-platform) in AGENTS.md | M1 | ORIGINAL_REQUEST §R1 |
| 2 | SPEC Kit Artifact Chain | Create intent.md, spec.md, plan.md in workspace root following SPEC Kit standards | M1 | ORIGINAL_REQUEST §R1 |
| 3 | Antigravity to Studio Guide | Comprehensive A-to-Z workflow guide covering ideation, development, testing, and debugging | M1 | ORIGINAL_REQUEST §R1 |
| 4 | FreeCurrencyAPI Client | Ktor HTTP client configured for FreeCurrencyAPI endpoints (/v1/latest, /v1/status) | M2 | ORIGINAL_REQUEST §R2 |
| 5 | Top Currencies Mapping | Map USD, EUR, NIS (ILS), and JOD with symbols, decimals, and flag metadata | M2 | ORIGINAL_REQUEST §R2 |
| 6 | JOD Peg Cross-Rate Derivation | Derive JOD rates via official Central Bank of Jordan USD peg (1 USD = 0.7090 JOD) avoiding 422 errors | M2 | Survey / Spec Miner |
| 7 | Offline Cache & Seed Rates | Cache rates in Preferences DataStore with TTL; bundle fallback seed rates for zero-crash cold starts | M2 | Survey / Spec Miner |
| 8 | Conversion Math & Validation | Support valid amounts, zero amount (returns 0.00), negative/invalid amount sanitization, precision rounding | M2 | ORIGINAL_REQUEST §R2 |
| 9 | UI/UX Design System Tokens | Cohesive light/dark color tokens, custom status/trend colors, tabular typography scale | M3 | ORIGINAL_REQUEST §R3 |
| 10 | Compose UI Components | BaseCurrencySelector, QuickAmountInput, ExchangeRateCard, ExchangeRateList, LivePulseIndicator, error banner | M3 | ORIGINAL_REQUEST §R3 |
| 11 | Motion & Animations | AnimatedContent numeric counters, graphicsLayer pulse on refresh, animateItem on list reorder | M3 | ORIGINAL_REQUEST §R3 |
| 12 | App Logo & Visual Assets | Custom vector SVG logo, Compose Canvas KashLogo, Android adaptive icon drawables | M3 | ORIGINAL_REQUEST §R3 |
| 13 | Marketing Kit & Presentation Deck | 7-slide presentation deck, taglines, value propositions, social media campaign copy | M3 | ORIGINAL_REQUEST §R3 |
| 14 | Google Play Store Listing Kit | App title, short description, full description, 6 screenshot specs, feature graphic concept | M3 | ORIGINAL_REQUEST §R3 |
| 15 | App Wiring & Navigation | Connect App.kt, theme wrapper, Koin/service locator, base currency configuration | M4 | ORIGINAL_REQUEST §R2 |
| 16 | Android Manifest Permissions | Add INTERNET and ACCESS_NETWORK_STATE to androidApp/src/main/AndroidManifest.xml | M4 | Survey / Explorer 1 |
| 17 | Multiplatform Build Verification | Ensure project builds and packages across androidApp, desktopApp, and shared modules | M4 | ORIGINAL_REQUEST §R2 |
| 18 | Automated Unit Tests | Unit tests for conversion math, cross-rates, JOD peg, zero/negative handling, repository caching | M5 | ORIGINAL_REQUEST §R4 |
| 19 | Automated UI Tests | UI tests verifying base currency selection and exchange rate list rendering | M5 | ORIGINAL_REQUEST §R4 |
| 20 | Multi-Tier E2E Testing Suite | Requirements-driven test suite (Tiers 1-4) published via TEST_READY.md | E2E Track | ORIGINAL_REQUEST §R4 |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | AI SDLC & SPEC Kit Swarm Framework | AGENTS.md, intent.md, spec.md, plan.md, docs/AI_SDLC_A_TO_Z_GUIDE.md | none | DONE (Verified & Clean) |
| M2 | Domain Logic & FreeCurrencyAPI Data Layer | Ktor client, DTOs, DataStore caching, seed rates, conversion math, JOD peg derivation, domain models & repository | M1 | DONE (Verified & 109 tests pass) |
| M3 | UI/UX Design System, Components & Store Kits | Theme tokens, MVI contracts & ViewModel, Compose components, animations, logo SVG/Compose Canvas, marketing kit, Play Store kit | M2 | PLANNED |
| M4 | App Integration & Multiplatform Packaging | App.kt wiring, Android manifest permissions, Koin DI, Android and Desktop build setup | M3 | PLANNED |
| M5 | Automated Testing Suite | Unit tests (math, repo, ViewModel) and UI component tests (currency selection, rate list) | M4 | PLANNED |
| E2E | Opaque-Box E2E Testing Track | Requirements-driven test runner & test cases across Tiers 1-4, publishing TEST_READY.md | M1 | DONE (83 tests ready) |
| Final | Final Acceptance & Adversarial Hardening | Pass 100% E2E tests, Tier 5 adversarial coverage audit, forensic integrity verification | M5, E2E | PLANNED |

## Code Layout
```
shared/src/commonMain/kotlin/org/moh/kash/project/
├── core/
│   ├── designsystem/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   ├── Typography.kt
│   │   ├── Shape.kt
│   │   └── KashLogo.kt
│   └── network/
│       └── HttpClientFactory.kt
├── data/
│   ├── local/
│   │   ├── CurrencyDataStore.kt
│   │   └── SeedRates.kt
│   ├── remote/
│   │   ├── FreeCurrencyApi.kt
│   │   ├── FreeCurrencyApiClient.kt
│   │   └── model/
│   │       ├── LatestRatesDto.kt
│   │       └── StatusDto.kt
│   └── repository/
│       └── CurrencyRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   ├── Currency.kt
│   │   ├── ExchangeRates.kt
│   │   └── ConversionResult.kt
│   ├── repository/
│   │   └── CurrencyRepository.kt
│   └── usecase/
│       ├── ConvertCurrencyUseCase.kt
│       ├── GetExchangeRatesUseCase.kt
│       └── UpdateBaseCurrencyUseCase.kt
└── presentation/
    ├── exchange/
    │   ├── ExchangeContract.kt
    │   ├── ExchangeViewModel.kt
    │   ├── ExchangeScreen.kt
    │   └── components/
    │       ├── BaseCurrencySelector.kt
    │       ├── QuickAmountInput.kt
    │       ├── ExchangeRateCard.kt
    │       ├── ExchangeRateList.kt
    │       └── LivePulseIndicator.kt
    └── App.kt

docs/
├── AI_SDLC_A_TO_Z_GUIDE.md
├── marketing_kit.md
├── play_store_listing.md
└── presentation_deck.md

workspace root/
├── AGENTS.md
├── intent.md
├── spec.md
└── plan.md
```

## Interface Contracts
### `domain` ↔ `data`
```kotlin
interface CurrencyRepository {
    fun getRates(baseCurrency: String, forceRefresh: Boolean = false): Flow<Result<ExchangeRates>>
    suspend fun saveBaseCurrency(currencyCode: String)
    fun getBaseCurrency(): Flow<String>
}
```

### `presentation` ↔ `domain`
```kotlin
class ConvertCurrencyUseCase {
    operator fun invoke(amount: Double, baseRate: Double, targetRate: Double, targetCurrency: Currency): ConversionResult
}

class GetExchangeRatesUseCase(private val repository: CurrencyRepository) {
    operator fun invoke(baseCurrency: String, forceRefresh: Boolean = false): Flow<Result<ExchangeRates>>
}
```

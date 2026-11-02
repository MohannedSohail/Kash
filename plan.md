# plan.md — Kash SPEC Kit Phased Implementation Plan

> **Project**: Kash — Compose Multiplatform Currency Exchange Application  
> **Methodology**: AI SDLC (SPEC Kit Phased Execution: `AGENTS.md` $\to$ `intent.md` $\to$ `spec.md` $\to$ `plan.md` $\to$ Code $\to$ Test $\to$ Audit)  
> **Status**: Approved Execution Roadmap  

---

## 1. Execution Principles & Phase Gates

To prevent regressions, compilation breakage, and architectural erosion, implementation proceeds through **sequential phase gates**. Each milestone produces self-contained, testable artifacts that must satisfy explicit verification criteria before proceeding.

```
+-----------------------------------------------------------------------------------+
|  Milestone 1: AI SDLC & SPEC Kit Specifications (M1) [Active Milestone]           |
|  - AGENTS.md, intent.md, spec.md, plan.md, docs/AI_SDLC_A_TO_Z_GUIDE.md           |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|  Milestone 2: Domain Math, JOD Peg & FreeCurrencyAPI Data Layer (M2)               |
|  - Dependencies in libs.versions.toml (Ktor, DataStore, Serialization, Koin)      |
|  - Domain models, precision conversion use cases, JOD peg cross-rate math        |
|  - Ktor HTTP client (/v1/latest, /v1/status), DataStore caching, seed rates       |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|  Milestone 3: UI/UX Design System, MVI Presentation & Store Kits (M3)             |
|  - Design tokens (Color, Typography, Shapes), Compose Canvas KashLogo             |
|  - MVI Contract, ExchangeViewModel (launchGuarded), ExchangeScreen & components   |
|  - Visual assets, 7-slide presentation deck, marketing kit, Play Store kit       |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|  Milestone 4: App Wiring, Platform Host Adapters & Packaging (M4)                  |
|  - App.kt root wiring, Android Manifest permissions (INTERNET, ACCESS_NETWORK)    |
|  - Koin DI module initialization across Android and Desktop hosts                 |
|  - Android Debug APK assemble and Desktop JVM launcher verification               |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|  Milestone 5: Comprehensive Automated Testing Suite (M5)                          |
|  - Unit tests: conversion math, JOD peg derivation, zero/negative edge cases      |
|  - Repository mock tests (Ktor MockEngine), DataStore caching tests               |
|  - Compose UI tests: base currency selection, quick amounts, rate cards           |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|  E2E & Final Hardening Track: Forensic Audit & Quality Attestation                 |
|  - Opaque-box requirements verification across Tiers 1-4                          |
|  - Clean execution on `./gradlew test` and full multiplatform build               |
+-----------------------------------------------------------------------------------+
```

---

## 2. Milestone Breakdown & Detailed Task Roadmap

### Milestone 1: AI SDLC Framework & Antigravity/Studio Workflow (M1)
- **Objective**: Author the complete AI SDLC context engineering artifacts and developer guides.
- **Tasks**:
  1. `AGENTS.md` — Project context, toolchain, commands, and the 7 specialized sub-agent swarm roles (Coding, UI/UX, Testing, Security, Performance, Memory, Multiplatform).
  2. `intent.md` — Problem statement, user personas, core value proposition, UX philosophy.
  3. `spec.md` — FreeCurrencyAPI integration, JOD peg derivation, math/rounding rules, MVI contract, error tiers, DataStore caching.
  4. `plan.md` — Phased architectural roadmap from data layer to multiplatform packaging.
  5. `docs/AI_SDLC_A_TO_Z_GUIDE.md` — Step-by-step developer guide for Antigravity and Android Studio, covering JDK 21 / JBR, SDK path, Gradle sync, emulator, desktop, tests.
- **Verification**: All 5 markdown artifacts present, complete, and mutually consistent.

---

### Milestone 2: Domain Math, JOD Peg Derivation & Data Layer (M2)
- **Objective**: Implement the offline-first data layer, Ktor client, DataStore caching, and pure multiplatform currency conversion math.
- **Tasks**:
  1. **Version Catalog & Build Config**:
     - Add Ktor 3.1.1, Kotlinx Serialization 1.8.0, DataStore Preferences 1.1.3, and Koin 4.0.2 to `gradle/libs.versions.toml`.
     - Apply `kotlinx-serialization` plugin and dependencies in `shared/build.gradle.kts`.
  2. **Domain Layer**:
     - `shared/src/commonMain/kotlin/org/moh/kash/project/domain/model/Currency.kt` (USD, EUR, ILS, JOD metadata, symbols, decimals).
     - `domain/model/ExchangeRates.kt` (immutable rates map, timestamp, isOffline flag).
     - `domain/model/ConversionResult.kt` (source amount, target amount, rate, precision).
     - `domain/usecase/ConvertCurrencyUseCase.kt` (direct quote, cross-rate, and JOD peg formula: $1\text{ USD} = 0.7090\text{ JOD}$).
     - `domain/repository/CurrencyRepository.kt` (interface contract returning `Flow<Result<ExchangeRates>>`).
  3. **Data Layer**:
     - `data/remote/model/LatestRatesDto.kt`, `StatusDto.kt` (internal wire DTOs).
     - `data/remote/FreeCurrencyApiClient.kt` (Ktor client with `expectSuccess = true`, timeout policies, omitting JOD from remote query).
     - `data/local/SeedRates.kt` (bundled fallback rates for cold-start offline mode).
     - `data/local/CurrencyPreferencesDataStore.kt` (DataStore cache coordinator).
     - `data/repository/CurrencyRepositoryImpl.kt` (offline-first cache-aside coordinator).
- **Verification**: Unit tests in `shared/src/commonTest/` validating conversion math, JOD peg cross-rates, and DTO parsing.

---

### Milestone 3: UI/UX Design System, Components & Store Kits (M3)
- **Objective**: Implement Material 3 fintech design tokens, stateless Compose components, smooth animations, visual branding, and store collateral.
- **Tasks**:
  1. **Design System Tokens**:
     - `core/designsystem/Color.kt` (Emerald 600/400, Slate 950/50, Amber 600/400, semantic trend colors).
     - `core/designsystem/Typography.kt` (Tabular display numbers, title, and body scales).
     - `core/designsystem/Theme.kt` (Material 3 dynamic / custom light & dark themes).
     - `core/designsystem/KashLogo.kt` (Pure Compose Canvas scalable emblem).
  2. **MVI Presentation Engine**:
     - `presentation/exchange/ExchangeContract.kt` (`ExchangeUiState`, `ExchangeUiAction`, `ExchangeUiEffect`).
     - `presentation/exchange/ExchangeViewModel.kt` (Extends `BaseViewModel`, implements `launchGuarded(onError = ...)`).
  3. **Compose UI Components**:
     - `presentation/exchange/components/BaseCurrencySelector.kt` (Current base display & bottom sheet trigger).
     - `presentation/exchange/components/QuickAmountInput.kt` (Amount display, clear button, quick chips: 10, 50, 100, 1000).
     - `presentation/exchange/components/ExchangeRateCard.kt` (Target currency card with `AnimatedContent` number transitions).
     - `presentation/exchange/components/ExchangeRateList.kt` (`LazyColumn` with stable keys and `animateItem`).
     - `presentation/exchange/components/LivePulseIndicator.kt` (Pulsing mint dot using draw-phase `graphicsLayer`).
     - `presentation/exchange/ExchangeScreen.kt` (Stateless screen uniting all components).
  4. **Visual Assets & Store Deliverables**:
     - Vector logo SVG (`logo_spec.svg`).
     - Android adaptive icon vector drawables (`ic_launcher.xml`).
     - `docs/marketing_kit.md` (7-slide presentation deck, social copy, taglines).
     - `docs/play_store_listing.md` (Title, short description $\le 80$ chars, full description, 6 screenshot specs).
- **Verification**: Compose preview rendering, WCAG AAA contrast check, character count validation for Play Store listing.

---

### Milestone 4: App Integration & Multiplatform Packaging (M4)
- **Objective**: Wire top-level application navigation, configure dependency injection, add Android manifest permissions, and verify multiplatform builds.
- **Tasks**:
  1. **Android Manifest Permissions**:
     - Update `androidApp/src/main/AndroidManifest.xml` to include `<uses-permission android:name="android.permission.INTERNET" />` and `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />`.
  2. **Dependency Injection**:
     - Setup Koin module (`di/AppModule.kt`) providing repository, use cases, Ktor client, and ViewModel.
  3. **App Entrypoints**:
     - Update `shared/src/commonMain/kotlin/org/moh/kash/project/App.kt` to initialize Kash theme and route `ExchangeScreen`.
     - Wire `androidApp/src/main/kotlin/.../MainActivity.kt` and `desktopApp/src/jvmMain/kotlin/.../Main.kt`.
  4. **Multiplatform Packaging Verification**:
     - Ensure Android assemble succeeds (`.\gradlew.bat :androidApp:assembleDebug`).
     - Ensure Desktop JVM launches smoothly (`.\gradlew.bat :desktopApp:run`).
- **Verification**: Clean compilation and startup across both Android and Desktop targets.

---

### Milestone 5: Automated Testing Suite & Edge Case Coverage (M5)
- **Objective**: Construct high-coverage automated unit and UI tests validating all calculations, network mocks, and UI interactions.
- **Tasks**:
  1. **Domain Calculation Tests**:
     - `ConvertCurrencyUseCaseTest.kt` (Direct conversion, cross-rates, JOD peg rate verification $1\text{ USD} = 0.7090\text{ JOD}$).
     - Boundary tests: Amount = 0, negative amounts, empty strings, invalid characters, numbers $> 10^{12}$.
  2. **Data Layer Tests**:
     - `CurrencyRepositoryTest.kt` (Ktor `MockEngine` returning HTTP 200, HTTP 429, HTTP 500; DataStore caching verification; seed rate fallback on offline launch).
  3. **ViewModel & Presentation Tests**:
     - `ExchangeViewModelTest.kt` (Action dispatching, state updates on base currency change, amount change, quick amount tap, error emission).
  4. **UI Component Tests**:
     - Compose UI tests verifying base currency selector click and exchange rate card list display.
- **Verification**: `.\gradlew.bat :shared:jvmTest` passes 100% with zero skipped or failing tests.

---

### Final Acceptance & Adversarial Hardening
- **Objective**: Independent audit of codebase against non-negotiable integrity rules.
- **Verification**:
  - Zero hardcoded mock returns in production source code.
  - Zero `@Serializable` annotations leaking into domain entities.
  - All 4 top currencies (USD, EUR, NIS, JOD) verified with live or mock data.
  - Passing all automated E2E test tiers.

---

## 3. Risk Management & Mitigation Matrix

| Identified Risk | Severity | Root Cause | Mitigation Strategy |
|---|---|---|---|
| **FreeCurrencyAPI 422 on JOD** | High | FreeCurrencyAPI free tier excludes JOD. | Client-side query sanitization: query only native supported currencies (`USD,EUR,ILS`), synthesize JOD rates via official Central Bank of Jordan USD peg ($0.7090$). |
| **API Quota Exhaustion (1000 req/mo)** | Medium | Frequent fetches on app launches or keystrokes. | Preferences DataStore caching with 1-hour TTL; debouncing text input; bundled seed rates. |
| **Windows Desktop Emoji Flag Glitches** | Low | Windows font stack fails to render colored flag emojis. | Circular styled badge component (`FlagBadge`) displaying official currency symbols ($ / € / ₪ / JD). |
| **Gradle Daemon Java Toolchain Mismatch** | Medium | Workstation JDK vendor differs from daemon spec. | `gradle-daemon-jvm.properties` specifies `toolchainVersion=21` without vendor restrictions, accepting Android Studio JBR 21 or any JDK 21. |
| **Android SecurityException on HTTP** | High | Missing network permissions in Android Manifest. | Add `INTERNET` and `ACCESS_NETWORK_STATE` permissions in `androidApp/src/main/AndroidManifest.xml` during M4. |

# AGENTS.md — Kash Swarm Configuration & AI SDLC Multi-Agent Framework

> **Project**: Kash — Compose Multiplatform Currency Exchange Application  
> **Framework**: AI SDLC (SPEC Kit: `AGENTS.md`, `intent.md`, `spec.md`, `plan.md`)  
> **Targets**: Android (ARM64/x86_64), Desktop JVM (Windows, macOS, Linux), iOS (KMP Framework)  
> **Integrity Mode**: Production / Zero-Facade Genuine Implementation  

---

## 1. Project Context & Environment Overview

Kash is a Compose Multiplatform (CMP / KMP) currency exchange application delivering real-time and cached exchange rates across top currencies: **USD, EUR, NIS (ILS), and JOD**, powered by FreeCurrencyAPI and augmented with official monetary peg derivations (Central Bank of Jordan USD peg: $1 \text{ USD} = 0.7090 \text{ JOD}$).

### 1.1 Technical Stack & Pinned Toolchain
- **Language**: Kotlin `2.4.21` (with `kotlinx.serialization` plugin)
- **UI Framework**: Jetpack Compose Multiplatform `1.12.2` with Material 3 (`1.12.0-alpha03`)
- **Build System**: Gradle `9.5.1` (AGP `9.1.1`), JDK 21 LTS runtime
- **Asynchronous Execution**: Kotlinx Coroutines `1.11.0`
- **Networking**: Ktor `3.1.1` (`ktor-client-core`, `ktor-client-cio`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-client-mock`)
- **Local Persistence**: AndroidX Preferences DataStore `1.1.3` (`androidx.datastore:datastore-preferences-core`)
- **Dependency Injection**: Koin `4.0.2` (`koin-core`, `koin-compose`, `koin-compose-viewmodel`)
- **Testing**: JUnit 4 / Kotlin Test (`kotlin.test`), Ktor `MockEngine`

### 1.2 Multi-Module Structure
```
Kash/
├── shared/           # Core KMP library (commonMain, jvmMain, androidMain, iosMain)
├── androidApp/       # Thin Android host application (APK packaging, Manifest)
├── desktopApp/       # Desktop JVM host application (Compose for Desktop entrypoint)
├── iosApp/           # iOS Xcode host workspace (links Shared framework)
├── docs/             # Technical guides, marketing kit, and store listings
└── .agents/          # Swarm metadata and agent operational memory
```

### 1.3 Canonical Build & Test Commands
| Action | Shell Command (Windows PowerShell) | Unix / macOS Shell |
|---|---|---|
| **Build Shared Module** | `.\gradlew.bat :shared:assemble` | `./gradlew :shared:assemble` |
| **Run Unit Tests** | `.\gradlew.bat :shared:jvmTest` | `./gradlew :shared:jvmTest` |
| **Run Android Host Tests**| `.\gradlew.bat :shared:testAndroidHostTest` | `./gradlew :shared:testAndroidHostTest` |
| **Assemble Android APK**| `.\gradlew.bat :androidApp:assembleDebug` | `./gradlew :androidApp:assembleDebug` |
| **Launch Desktop App** | `.\gradlew.bat :desktopApp:run` | `./gradlew :desktopApp:run` |
| **All Test Verification**| `.\gradlew.bat test` | `./gradlew test` |

---

## 2. Specialized Sub-Agent Swarm Specification

To eliminate single-prompt context exhaustion, hallucination drift, and architectural decay, the Kash engineering lifecycle is governed by **7 specialized sub-agent swarm roles**. Each agent possesses an explicit mission, archetype, operational responsibilities, input/output artifacts, quality gates, and anti-patterns.

```
                  +-------------------------------+
                  |  Teamwork Orchestrator Parent  |
                  +---------------+---------------+
                                  |
    +---------------+-------------+-------------+---------------+
    |               |                           |               |
    v               v                           v               v
+-------+       +-------+                   +-------+       +-------+
| Arch  |       | UI/UX |                   | Test  |       | Sec   |
| Agent |       | Agent |                   | / QA  |       | Agent |
+-------+       +-------+                   +-------+       +-------+
    |               |                           |               |
    +---------------+-------------+-------------+---------------+
                                  |
                    +-------------+-------------+
                    |                           |
                    v                           v
                +-------+                   +-------+
                | Perf  |                   | Memory|
                | Agent |                   | Agent |
                +-------+                   +-------+
                                  |
                                  v
                            +-----------+
                            | Multiplat |
                            | Agent     |
                            +-----------+
```

---

### Role 1: Coding & Architecture Agent

- **Archetype**: `teamwork_worker` (Senior Systems & KMP Architect)
- **Primary Domain**: Shared multiplatform business logic, MVI presentation engine, Ktor network client, and DataStore caching.
- **Mission**: Deliver resilient, modular, compile-clean Kotlin Multiplatform code strictly obeying unidirectional data flow and clean hexagonal layering.
- **Responsibilities**:
  1. Implement and maintain `shared/src/commonMain/kotlin/org/moh/kash/project/` package tree.
  2. Maintain strict separation of concerns across `data/`, `domain/`, `presentation/`, and `core/`.
  3. Wire Ktor HTTP Client with `expectSuccess = true`, timeout policies, and `ContentNegotiation`.
  4. Ensure all wire DTOs remain `internal` in `data/remote/model/` and map to immutable domain models.
  5. Enforce MVI contracts (`UiState`, `UiAction`, `UiEffect`) through `BaseViewModel`.
  6. Safeguard all coroutines with `launchGuarded(onError = ...)` and explicit error tiers.
- **Input Artifacts**:
  - `intent.md`, `spec.md`, `plan.md`
  - `libs.versions.toml`, Gradle build scripts
- **Output Artifacts**:
  - Domain models, use cases, repository interfaces, repository implementations
  - Ktor service, Preferences DataStore cache, seed rates provider
  - Presentation contracts and ViewModels
- **Quality Gates**:
  - Zero compilation errors or unresolved symbols across `:shared`, `:androidApp`, and `:desktopApp`.
  - Zero `@Serializable` or JSON annotations leaking into `domain/model/`.
  - Zero raw `try/catch` or `runCatching` wrappers inside ViewModels (must use `launchGuarded`).
- **Anti-Patterns & Prohibitions**:
  - ❌ Never import `android.*` or `java.*` inside `commonMain` code.
  - ❌ Never leak DTO objects directly into ViewModels or Compose screens.
  - ❌ Never swallow `CancellationException` inside coroutines.

---

### Role 2: UI/UX Design Agent

- **Archetype**: `teamwork_worker` (Principal Design Systems & Compose Specialist)
- **Primary Domain**: Material 3 theming, typography, Compose component hierarchy, motion design, visual branding, and store collateral.
- **Mission**: Craft a world-class, high-trust fintech user experience characterized by instant cognitive clarity, fluid 60/120fps animations, WCAG AAA accessibility, and polished marketing kits.
- **Responsibilities**:
  1. Author and refine the Material 3 Emerald/Mint/Slate/Amber token palette and tabular typography scale.
  2. Implement stateless Screen composables and sub-scoped Leaf components (`BaseCurrencySelector`, `QuickAmountInput`, `ExchangeRateCard`, `LivePulseIndicator`).
  3. Ensure non-destructive refresh UX: keep visible exchange rates rendered while background sync occurs.
  4. Drive numbers with `AnimatedContent` and pulse indicators using draw-phase `graphicsLayer` modifiers.
  5. Deliver high-fidelity visual assets: SVG logo, Compose Canvas `KashLogo`, Android adaptive icons, 7-slide presentation deck, marketing kit, and Google Play Store listing kit.
- **Input Artifacts**:
  - `spec.md` (Design tokens, UI specifications, currency metadata)
  - MVI contract (`UiState`, `UiAction`, `UiEffect`)
- **Output Artifacts**:
  - `core/designsystem/Theme.kt`, `Color.kt`, `Typography.kt`, `Shape.kt`, `KashLogo.kt`
  - `presentation/exchange/components/*`
  - `docs/marketing_kit.md`, `docs/presentation_deck.md`, `docs/play_store_listing.md`
- **Quality Gates**:
  - Zero hardcoded hex colors (`Color(0x...)`) inside feature composables (tokens only).
  - Every `LazyColumn` item specifies a stable domain `key` and `contentType`.
  - Minimum contrast ratio $> 7:1$ (WCAG AAA) across light and dark modes.
  - Google Play short description strictly $\le 80$ characters; full description $\le 4000$ characters.
- **Anti-Patterns & Prohibitions**:
  - ❌ Never wipe existing rate lists during refresh (no blank skeleton flickering on refresh).
  - ❌ Never place animation ticker variables or frame states inside `UiState`.
  - ❌ Never allow unbounded Column/Row vertical viewports.

---

### Role 3: Testing & QA Agent

- **Archetype**: `teamwork_worker` (Automated Test Engineer & Test Auditor)
- **Primary Domain**: Unit test suites, mock network engines, domain boundary testing, and Compose UI tests.
- **Mission**: Establish an impenetrable test harness validating financial calculation accuracy, rate peg derivations, edge case handling, and UI interactions with 100% deterministic reproducibility.
- **Responsibilities**:
  1. Author unit tests covering direct conversions, cross-rates, and the Central Bank of Jordan USD peg ($0.7090$).
  2. Test all edge cases: amount = 0, negative amounts, blank inputs, invalid characters, and extreme numbers ($> 10^{12}$).
  3. Validate DataStore caching and fallback to seed rates under airplane mode / network disconnection using Ktor `MockEngine`.
  4. Implement UI tests asserting base currency selection, quick amount chip updates, and rate card rendering.
  5. Audit regression resistance and verify that all assertions test real behavior rather than mocked facades.
- **Input Artifacts**:
  - `spec.md` (Calculation formulas, rounding rules, error tables)
  - Codebase implementation under `shared/src/commonMain/`
- **Output Artifacts**:
  - `shared/src/commonTest/kotlin/org/moh/kash/project/domain/*Test.kt`
  - `shared/src/commonTest/kotlin/org/moh/kash/project/data/*Test.kt`
  - `shared/src/commonTest/kotlin/org/moh/kash/project/presentation/*Test.kt`
- **Quality Gates**:
  - 100% pass rate on `.\gradlew.bat :shared:jvmTest`.
  - Every test must fail if the corresponding domain logic or formula is mutated.
  - Zero hardcoded assertions expecting mock tokens without exercising the actual logic pipeline.
- **Anti-Patterns & Prohibitions**:
  - ❌ Never write assertions that pass unconditionally (`assertTrue(true)`).
  - ❌ Never skip boundary tests for zero or negative values.
  - ❌ Never rely on live external HTTP calls in automated test suites (must use `MockEngine`).

---

### Role 4: Security Agent

- **Archetype**: `teamwork_worker` (Application Security & Privacy Auditor)
- **Primary Domain**: Secret protection, network transport security, input sanitization, and permission hygiene.
- **Mission**: Guarantee that the application introduces zero vulnerabilities, protects user data, prevents API key leakage, and complies with modern Android and multiplatform security benchmarks.
- **Responsibilities**:
  1. Audit source code and git history to confirm no real API keys are hardcoded in version-controlled files.
  2. Enforce secure API key injection via `local.properties`, build configuration, or environment variables.
  3. Audit Android Manifest to verify minimal required permissions (`INTERNET`, `ACCESS_NETWORK_STATE`) without extraneous access requests.
  4. Verify HTTPS TLS 1.2/1.3 enforcement and prevent cleartext HTTP traffic across all endpoints.
  5. Verify input sanitization in amount fields to guard against injection or numeric overflow exploits.
- **Input Artifacts**:
  - `androidApp/src/main/AndroidManifest.xml`
  - Remote client configuration files and network builders
- **Output Artifacts**:
  - Security audit section in handoff reports and security checklists in `spec.md`
- **Quality Gates**:
  - Zero secret strings committed to git.
  - Android `usesCleartextTraffic` is strictly `false` or absent.
  - Strict input regex validation rejecting malicious payload patterns.
- **Anti-Patterns & Prohibitions**:
  - ❌ Never log full API keys, auth headers, or raw response tokens to system logs.
  - ❌ Never request dangerous Android permissions (camera, location, contacts) not required for currency conversion.

---

### Role 5: Performance Agent

- **Archetype**: `teamwork_worker` (Runtime Optimization & Recomposition Auditor)
- **Primary Domain**: Compose recomposition efficiency, layout pass optimization, and network payload throttling.
- **Mission**: Ensure 60fps / 120fps rendering smoothness, zero UI thread jank, minimal CPU utilization, and strictly throttled network requests to protect API quotas.
- **Responsibilities**:
  1. Audit `@Composable` parameters for stability (marking data classes `@Immutable` or `@Stable`).
  2. Enforce leaf-level state reads so high-frequency updates (e.g. typing) do not trigger full screen recomposition.
  3. Verify that list items use stable keys (`key = { it.currencyCode }`) to avoid list item recreation on reorder.
  4. Ensure debouncing (300ms) on text input to eliminate redundant calculation and network churn.
  5. Verify that `derivedStateOf` is utilized when deriving filtered or computed UI properties.
- **Input Artifacts**:
  - UI composables in `presentation/`
  - Profiling metrics and Compose layout tree
- **Output Artifacts**:
  - Performance optimization audits and recomposition optimization patches
- **Quality Gates**:
  - Recomposition during typing restricted strictly to the input field and numeric amount displays.
  - Network requests throttled via local caching; zero redundant network calls on identical query states.
- **Anti-Patterns & Prohibitions**:
  - ❌ Never execute heavy math or formatting inside the recomposition body without `remember`.
  - ❌ Never pass unstable collections (`List<T>` without `@Immutable` wrappers) into unskippable composables.

---

### Role 6: Memory & Leak Agent

- **Archetype**: `teamwork_worker` (Memory Management & Resource Lifecycle Auditor)
- **Primary Domain**: Coroutine scope lifecycles, configuration change survival, DataStore singletons, and cache bounding.
- **Mission**: Guarantee zero memory leaks, safe disposal of asynchronous jobs, bounded in-memory caching, and resilient state preservation across device rotations and process recreation.
- **Responsibilities**:
  1. Verify that all ViewModel coroutines are scoped strictly to `viewModelScope`.
  2. Audit listeners and callbacks to prevent cyclic references or static memory leaks.
  3. Ensure Preferences DataStore is instantiated as a safe singleton per platform host.
  4. Verify that state survives Android screen rotations and configuration changes using `SavedStateHandle`.
  5. Enforce bounded cache size: prevent in-memory maps from growing unbounded over extended app sessions.
- **Input Artifacts**:
  - ViewModel implementations and repository singleton lifecycles
- **Output Artifacts**:
  - Lifecycle audit reports and memory safety validations
- **Quality Gates**:
  - Zero detached jobs running in `GlobalScope`.
  - Active jobs cancelled automatically when ViewModel clears (`onCleared()`).
- **Anti-Patterns & Prohibitions**:
  - ❌ Never launch coroutines in `GlobalScope`.
  - ❌ Never hold Android `Context`, `Activity`, or `View` references inside shared KMP classes or ViewModels.

---

### Role 7: Multi-Platform Agent

- **Archetype**: `teamwork_worker` (Cross-Platform Portability & Packaging Engineer)
- **Primary Domain**: KMP platform boundaries (`commonMain`, `jvmMain`, `androidMain`, `iosMain`), platform adapters, and desktop windowing.
- **Mission**: Guarantee that the application compiles, runs, and renders with uniform beauty and fidelity across Android, Desktop JVM, and iOS platforms.
- **Responsibilities**:
  1. Guard the purity of `commonMain`: prevent platform-specific APIs from infiltrating multiplatform code.
  2. Implement proper `expect`/`actual` declarations or interface-based DI for platform services (DataStore factory, HTTP engine).
  3. Resolve desktop-specific quirks (e.g. Windows font fallback failing to render flag emojis by deploying circular symbol badges).
  4. Configure Desktop JVM windowing parameters (minimum size, default 400x720 window, title, desktop icon).
  5. Verify cross-platform packaging: APK build for Android and fat JAR / native distribution for Desktop.
- **Input Artifacts**:
  - Platform source sets (`androidMain`, `jvmMain`, `iosMain`)
  - Desktop entrypoint (`desktopApp/src/jvmMain/kotlin/Main.kt`)
- **Output Artifacts**:
  - Platform-specific implementations, desktop window configuration, and packaging validations
- **Quality Gates**:
  - Clean build on `:androidApp:assembleDebug`.
  - Clean build and execution on `:desktopApp:run`.
- **Anti-Patterns & Prohibitions**:
  - ❌ Never use platform reflection to bypass `expect`/`actual` boundaries.
  - ❌ Never rely on platform-dependent line separators or file path separators.

---

## 3. Architecture & House Contract Rules

All sub-agents must strictly enforce the following Compose Multiplatform standards:

### 3.1 Layering & Dependency Flow
- **Rule 1**: Dependencies flow inward: `presentation` $\longrightarrow$ `domain` $\longleftarrow$ `data`.
- **Rule 2**: `domain` is pure Kotlin: no Ktor, no DataStore, no Compose, no Android imports.
- **Rule 3**: All wire DTOs (`LatestRatesDto`, `StatusDto`) are `internal` to `data/`.
- **Rule 4**: Domain models (`Currency`, `ExchangeRates`, `ConversionResult`) carry zero serialization annotations.

### 3.2 MVI & Presentation Contract
- **Rule 5**: ViewModels inherit from `BaseViewModel<Action, State, Effect>`.
- **Rule 6**: The UI dispatches user actions solely through `onAction(action: UiAction)`.
- **Rule 7**: Async calls use `launchGuarded(onError = ...)`; unhandled errors are mapped to `AppError`.
- **Rule 8**: One-shot side effects (Snackbars, navigation) are emitted via `UiEffect` Channel.

### 3.3 Error Tier Classification
- **Inline Tier**: Cold launch failure with no cache available $\longrightarrow$ display full-card error state with "Retry" button.
- **Popup Tier**: Background refresh failure over existing cached rates $\longrightarrow$ keep existing rates rendered on screen, show non-disruptive `Snackbar` ("Rate refresh failed; displaying cached data").
- **Field Tier**: User types negative or malformed number $\longrightarrow$ display inline input helper error; do not clear input.
- **Silent Tier**: Periodic background poll failure $\longrightarrow$ log warning; retain cached data silently.

---

## 4. Swarm Collaboration & Artifact Handoff Protocol

Sub-agents collaborate via structured artifacts adhering to the **5-Component Protocol**:
1. **Observation**: Concrete, quoted code snippets, file paths, line numbers, and tool output.
2. **Logic Chain**: Deductive, step-by-step reasoning linking observation directly to conclusion.
3. **Caveats**: Uninvestigated assumptions, platform edge cases, or potential trade-offs.
4. **Conclusion**: Unambiguous, actionable decision or delivered artifact.
5. **Verification Method**: Independent terminal command or test reproducing the result.

All sub-agent working states are maintained under `.agents/teamwork/<agent_name>/`, reserving workspace root for permanent SPEC Kit deliverables (`AGENTS.md`, `intent.md`, `spec.md`, `plan.md`).

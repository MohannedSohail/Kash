# Handoff Report — Specification Verification Challenger 2 (Milestone 1)

> **Agent**: Specification Verification Challenger 2 (`teamwork_challenger`)  
> **Milestone**: Milestone 1 (AI SDLC & SPEC Kit Specifications)  
> **Status**: **CHALLENGE_RAISED**  
> **Date**: 2026-11-01  

---

## 1. Observation

### Observation 1: Hardcoded Daemon Vendor Lockout Blocks CLI Gradle Builds
- **File**: `gradle/gradle-daemon-jvm.properties`
  ```properties
  toolchainVendor=AZUL
  toolchainVersion=21
  ```
- **File**: `docs/AI_SDLC_A_TO_Z_GUIDE.md` (Lines 39–43, Line 236):
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
  $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
  ```
  And troubleshooting table:
  `"Cannot find a Java installation matching {vendor=Azul Zulu, languageVersion=21} -> Run under Android Studio's bundled JBR, or point $env:JAVA_HOME to JDK 21, or add org.gradle.java.installations.auto-download=true in gradle.properties."`
- **Execution & Verbatim Tool Output**:
  Running `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :shared:jvmTest` produces:
  ```
  FAILURE: Build failed with an exception.
  * What went wrong:
  Cannot find a Java installation on your machine (Windows 11 10.0 amd64) matching: {languageVersion=21, vendor=Azul Zulu, implementation=vendor-specific, nativeImageCapable=false}. Toolchain auto-provisioning is not enabled.
  ```
  **Direct Finding**: Pointing `$env:JAVA_HOME` to Android Studio's JBR (`OpenJDK 21.0.10, JetBrains`) **does NOT fix** the build because Gradle's launcher rejects any JVM where vendor is not `AZUL`. Furthermore, auto-provisioning is disabled by default, and `org.gradle.java.installations.auto-download=true` is absent from `gradle.properties`.

### Observation 2: Version Discrepancy Between AGENTS.md and Gradle Wrapper
- **File**: `AGENTS.md` (Line 18):
  `Build System: Gradle 9.1.1 (AGP 9.1.1), JDK 21 LTS runtime`
- **File**: `gradle/wrapper/gradle-wrapper.properties` (Line 4):
  `distributionUrl=https\://services.gradle.org/distributions/gradle-9.5.1-bin.zip`
- **Execution**: `.\gradlew.bat --version` reports `Gradle 9.5.1`. AGP is `9.1.1`, but Gradle is `9.5.1`.

### Observation 3: AC-2 Decimal Precision Contradicts Currency Domain Rule
- **File**: `spec.md` Section 3.1 & Section 4.1:
  - Currency Metadata Table: `USD: Decimals = 2`
  - Converted Amounts (USD, EUR, ILS): *"Formatted to 2 decimal places with comma thousands separators (e.g. 1,250.75)"*
- **File**: `spec.md` Section 7 (Line 258):
  `- [ ] **AC-2 (JOD Conversion Accuracy)**: Converting 100 USD to JOD yields strictly 70.900 JOD. Converting 100 JOD to USD yields strictly 141.044 USD (± 0.001).`
- **Empirical Execution**: Python verification test:
  `100 * (1 / 0.7090) = 141.0437...` -> Rounded to 2 decimals (USD rule) is `141.04 USD`.
  AC-2 formats USD to 3 decimals (`141.044 USD`), directly violating the domain specification requiring 2 decimals for USD.

### Observation 4: Architectural Contradiction Between SeedRates Cold Start vs. Inline Tier Error Card
- **File**: `spec.md` Section 5.1 (Line 176):
  `"If cache is empty (first fresh launch without network): loads bundled SeedRates immediately so UI renders without delay."`
- **File**: `spec.md` Section 7 (AC-4, Line 260):
  `"App launched in Airplane mode renders bundled seed rates or cached DataStore rates with an offline badge. Zero app crashes."`
- **File**: `spec.md` Section 6.2 (Lines 240–242):
  `"1. Inline Tier (Cold Start Failure): Condition: Initial launch has no cached rates in DataStore and network call fails. Behavior: Render error card with icon, friendly explanation, and an actionable Button('Retry')."`
- **Direct Finding**: If SeedRates are loaded when cache is empty so UI renders without delay, an initial launch with no network renders SeedRates, meaning the Inline Tier error condition is unreachable. If the Inline Tier error card is rendered, AC-4 is violated and SeedRates are ignored.

### Observation 5: Incomplete Error Modeling in MVI Contract (`ExchangeUiState`)
- **File**: `spec.md` Section 6.1 (Lines 201–210):
  ```kotlin
  data class ExchangeUiState(
      val baseCurrency: String = "USD",
      val inputAmount: String = "100.00",
      val rates: List<RateCardItem> = emptyList(),
      val isColdLoading: Boolean = true,
      val isRefreshing: Boolean = false,
      val error: AppError? = null,
      val lastUpdated: String? = null
  )
  ```
- **File**: `spec.md` Section 6.2 (Field Tier, Line 248):
  `"Condition: User pastes negative numbers or malformed text. Behavior: Show red error text below the input field; keep the rest of the UI stable."`
- **Direct Finding**: `ExchangeUiState` has only `error: AppError?` (which drives whole-screen/inline errors). There is no field-level error property (e.g. `amountError: String?`). In the UI, setting `error` for an invalid amount input risks replacing the entire screen with an error card.

### Observation 6: MVI Anti-Pattern in Sheet Presentation & Unstable Collection
- **File**: `spec.md` Section 6.1 (Lines 233–236):
  `data object OpenCurrencySelectorSheet : ExchangeUiEffect`
- **Direct Finding**: Bottom sheet presentation is modeled as a one-shot side-effect channel instead of state. If a configuration change (rotation) occurs while the sheet is visible, the state is unrecoverable.
- **File**: `spec.md` Section 6.1 (Line 205): `val rates: List<RateCardItem> = emptyList()`. `List<T>` is considered unstable by default in Compose without `@Immutable` wrappers or `ImmutableList`, risking redundant recompositions in violation of `AGENTS.md` Role 5.

---

## 2. Logic Chain

1. **Premise 1 (Toolchain)**: `gradle/gradle-daemon-jvm.properties` enforces `toolchainVendor=AZUL 21`. Machines with standard Android Studio installations have JetBrains JBR 21 (`C:\Program Files\Android\Android Studio\jbr`), not Azul Zulu.
2. **Inference 1**: Running `./gradlew.bat` in CLI fails immediately because Gradle does not match JBR against `AZUL`. The guidance in `AI_SDLC_A_TO_Z_GUIDE.md` stating that setting `JAVA_HOME` to JBR fixes this is empirically false. Unless `toolchainVendor=AZUL` is removed or auto-download is enabled in `gradle.properties`, developer CLI onboarding fails.
3. **Premise 2 (Precision)**: USD currency formatting is explicitly defined as 2 decimal places throughout `spec.md` and `intent.md`.
4. **Inference 2**: AC-2 specifying `141.044 USD` (3 decimals) introduces an internal contradiction into automated test suites. A test asserting `141.044 USD` will fail against a domain usecase implementing the 2-decimal USD formatting rule (`141.04 USD`).
5. **Premise 3 (Cold Boot UX)**: Section 5.1 and AC-4 mandate that a cold launch without internet renders `SeedRates` with an offline badge. Section 6.2 mandates that a cold launch without cache renders an Inline Tier Error Card.
6. **Inference 3**: These two behaviors are mutually exclusive in the same state machine. Either the repository always emits `SeedRates` when cache is empty (rendering rates with an offline banner), or it emits an error (rendering the full error card). The specification must clarify the priority.
7. **Premise 4 (MVI & State Safety)**: Field validation errors (Field Tier) require distinct state representation from system errors (Inline Tier).
8. **Inference 4**: Without `val amountError: String? = null` in `ExchangeUiState`, developers will either overload `error: AppError?` (breaking the screen) or implement out-of-band state inside the composable, violating unidirectional data flow.

---

## 3. Caveats

- Android Studio's IDE Gradle integration may bypass the CLI daemon launcher if the user configures the IDE to run Gradle via JBR directly inside the IDE settings. However, all CLI builds (`.\gradlew.bat`) and CI runners will hit the Azul toolchain blocker.
- Offline behavior depends on whether `SeedRates` are bundled synchronously into the repository or dispatched asynchronously. If synchronous, an empty rate list is impossible on cold launch.

---

## 4. Conclusion & Challenge Report

**Verdict: CHALLENGE_RAISED**

The Milestone 1 artifacts are exceptionally thorough, high-quality, and robust, but require the following 6 targeted fixes before proceeding to Milestone 2 code generation:

### Challenge 1 (High Severity): CLI Gradle Daemon Toolchain Lockout
- **Assumption Challenged**: Setting `$env:JAVA_HOME` to Android Studio's JBR 21 resolves the Gradle daemon vendor mismatch.
- **Empirical Failure**: Gradle exits with code 1 (`Cannot find a Java installation matching: {vendor=Azul Zulu}`). JBR is rejected.
- **Blast Radius**: Any developer following `docs/AI_SDLC_A_TO_Z_GUIDE.md` cannot execute `.\gradlew.bat` CLI commands.
- **Mitigation**: In `gradle/gradle-daemon-jvm.properties`, remove the `toolchainVendor=AZUL` line (leaving only `toolchainVersion=21`), OR add `org.gradle.java.installations.auto-download=true` to `gradle.properties`.

### Challenge 2 (Medium Severity): AC-2 USD Decimal Precision Error
- **Assumption Challenged**: AC-2 correctly represents domain conversion formatting.
- **Empirical Failure**: AC-2 requires `141.044 USD` (3 decimals), but USD is defined as 2 decimals (`141.04 USD`).
- **Blast Radius**: Unit tests in Milestone 5 will fail assertion checks or require violating domain formatting rules.
- **Mitigation**: Update AC-2 in `spec.md` to: *"Converting 100 JOD to USD yields strictly `141.04 USD` (or raw value `141.044 ± 0.001` before formatting)"*.

### Challenge 3 (High Severity): SeedRates vs. Inline Error Card State Conflict
- **Assumption Challenged**: The system can simultaneously guarantee zero-crash offline cold starts via SeedRates and display a full-screen Inline Tier error card on cold start failure.
- **Empirical Failure**: If SeedRates are returned when cache is empty, the rate list is never empty, so the full error card never displays.
- **Blast Radius**: Inconsistent implementation between repository and presentation layer.
- **Mitigation**: Clarify in `spec.md` Section 5.1 and Section 6.2:
  - If network fails and DataStore is empty, `CurrencyRepository` serves `SeedRates` marked with `isOffline = true, isFallback = true`, and emits a banner/Snackbar informing the user.
  - Reserve the Inline Tier error card strictly for fatal corruption where neither DataStore nor SeedRates can be read.

### Challenge 4 (Medium Severity): Missing Field-Level Error in `ExchangeUiState`
- **Assumption Challenged**: `ExchangeUiState.error: AppError?` is sufficient for all error tiers.
- **Empirical Failure**: Setting `error: AppError?` on invalid input triggers full-screen error states.
- **Blast Radius**: Broken input UX when typing negative numbers or malformed text.
- **Mitigation**: Add `val amountError: String? = null` to `ExchangeUiState` in `spec.md` and `plan.md`.

### Challenge 5 (Medium Severity): Currency Sheet Presentation Modeled as Effect
- **Assumption Challenged**: Modal presentation should be driven by one-shot `UiEffect`.
- **Empirical Failure**: Sheet state is lost on Android activity rotation/recreation.
- **Blast Radius**: Glitchy UI during screen rotation.
- **Mitigation**: Add `val isCurrencyPickerOpen: Boolean = false` to `ExchangeUiState`, and add `OpenCurrencyPicker` / `DismissCurrencyPicker` to `ExchangeUiAction`.

### Challenge 6 (Low Severity): Version Discrepancy (Gradle 9.1.1 vs 9.5.1)
- **Assumption Challenged**: `AGENTS.md` accurately documents the build system version.
- **Empirical Failure**: `AGENTS.md` says Gradle 9.1.1; wrapper uses Gradle 9.5.1.
- **Blast Radius**: Documentation confusion.
- **Mitigation**: Update `AGENTS.md` line 18 to `Gradle 9.5.1 (AGP 9.1.1)`.

---

## 5. Verification Method

To independently verify these findings:

1. **Verify Toolchain Lockout**:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   .\gradlew.bat :shared:jvmTest
   ```
   *Expected Result*: Fails with `Cannot find a Java installation matching: {vendor=Azul Zulu}`.
2. **Verify AC-2 Precision Inconsistency**:
   ```powershell
   python -c "from decimal import Decimal, ROUND_HALF_UP; print((Decimal('100') / Decimal('0.7090')).quantize(Decimal('0.01'), rounding=ROUND_HALF_UP))"
   ```
   *Expected Result*: Outputs `141.04`, contradicting AC-2's `141.044`.
3. **Inspect Specification Inconsistencies**:
   - Compare `spec.md` line 176 and line 260 against lines 240–242.
   - Compare `spec.md` line 208 (`val error: AppError?`) against line 248 (Field Tier).

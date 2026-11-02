# TEST_INFRA.md — Kash E2E Testing Infrastructure & Methodology

> **Project**: Kash — Compose Multiplatform Currency Exchange Application  
> **Target Framework**: Kotlin Multiplatform (`kotlin.test`), Compose Multiplatform  
> **Status**: Certified & Active  
> **Author**: E2E Testing Track Engineer (`test_writer_e2e`)  

---

## 1. Testing Philosophy

The Kash testing infrastructure is constructed on three non-negotiable architectural pillars:

### 1.1 Opaque-Box Validation
Tests treat the internal implementation of modules as an opaque box. Test cases interact strictly through public interface contracts, observable UI states, and user actions. Rather than inspecting private variables or mocking internal class details, tests verify observable external behavior:
- Input amounts and base currency selections.
- Emitted UI rate cards, formatted currency amounts, and decimal precisions.
- MVI error tiers (Inline retry cards, Popup Snackbars, Field errors, Silent logs).
- Network wire queries dispatched to external services (FreeCurrencyAPI).

### 1.2 Requirement-Driven & Authoritative Expected Outputs
Every assertion in the test suite is derived from explicit authoritative specifications rather than guessing or hardcoding ad-hoc constants:
- **Central Bank of Jordan Official Peg**:
  $$1 \text{ USD} = 0.7090 \text{ JOD} \quad \Longleftrightarrow \quad 1 \text{ JOD} = \frac{1}{0.7090} \approx 1.4104372 \text{ USD}$$
  Derived directly from Central Bank of Jordan monetary policy.
- **FreeCurrencyAPI Endpoint Contract**:
  - `GET /v1/latest` with query parameter `currencies=USD,EUR,ILS` (omits JOD to prevent `HTTP 422 Unprocessable Entity`).
  - Native support for 33 currencies (USD, EUR, ILS).
  - Rate limits: 10 req/min, 1,000 req/month.
- **Financial Math & Rounding Rules**:
  - Exchange rates: formatted to **4 decimal places** (`3.6542`, `0.7090`).
  - Standard currency amounts (USD, EUR, ILS): formatted to **2 decimal places** with comma thousand separators (`1,234.56`).
  - Three-decimal currency amounts (JOD): formatted to **3 decimal places** with comma thousand separators (`1,234.567`).
  - Rounding Mode: `RoundingMode.HALF_UP` (standard financial rounding).

### 1.3 4-Tier Test Suite Hierarchy
Testing is organized into four distinct progressive tiers, escalating from individual feature coverage to full real-world user flows:

```
+-------------------------------------------------------------------------------+
|                       TIER 4: REAL-WORLD SCENARIOS                            |
|  - Traveler Journey across USD, JOD, ILS, EUR                                 |
|  - Merchant Multi-Currency Retail Daily Settlements                           |
|  - Off-Grid In-Flight Menu Conversions (Cold Start & Cached)                  |
+-------------------------------------------------------------------------------+
                                        ^
                                        |
+-------------------------------------------------------------------------------+
|                    TIER 3: CROSS-FEATURE COMBINATIONS                         |
|  - Base Currency Switching While Typing                                       |
|  - Rapid Amount Modification During Background Refresh                        |
|  - Network Drop Recovery & Multi-Platform Flapping                            |
+-------------------------------------------------------------------------------+
                                        ^
                                        |
+-------------------------------------------------------------------------------+
|                    TIER 2: BOUNDARY & CORNER CASES                            |
|  - Zero Amount (0 / 0.00 / 0.000)                                             |
|  - Negative & Malformed Amount Sanitization                                   |
|  - Empty Amount & Field Clearing                                              |
|  - Half-Up Rounding & Precision (2 vs 3 decimals)                             |
|  - Extreme Values (1 Billion, 1 Trillion, Micro-rates, 12-digit caps)         |
|  - HTTP Status Error Mapping (401, 422, 429, 500, 503)                       |
|  - Offline Seed Rates Fallback (Airplane Mode Cold Start)                     |
+-------------------------------------------------------------------------------+
                                        ^
                                        |
+-------------------------------------------------------------------------------+
|                       TIER 1: CORE FEATURE COVERAGE                           |
|  - Base Currency Selection (USD, EUR, ILS, JOD)                               |
|  - USD Rates Calculation & Display                                            |
|  - EUR Rates Calculation & Display                                            |
|  - NIS (ILS) Rates Calculation & Display                                      |
|  - JOD Rates Calculation & USD Peg Derivation                                 |
|  - Currency Conversion Math (Direct, Cross-Rate, Self)                         |
+-------------------------------------------------------------------------------+
```

---

## 2. Feature Inventory Mapping to Test Tiers

The following matrix maps the project features from `PROJECT.md` and `spec.md` to their corresponding test suites and verified assertions:

| Feature # | Feature Description | Assigned Tier | Test File | Test Case Count | Key Verified Invariants |
|---|---|---|---|---|---|
| **F-01** | Base Currency Selection | Tier 1 | `Tier1FeatureCoverageTest.kt` | 6 tests | Default base is USD; target card list excludes active base; switching recalculates all target rates; selection is idempotent. |
| **F-02** | USD Exchange Rates | Tier 1 | `Tier1FeatureCoverageTest.kt` | 5 tests | USD $\to$ USD is 1.0000; USD $\to$ EUR matches feed; USD $\to$ ILS matches feed; USD $\to$ JOD is strictly 0.7090; 4 decimal display. |
| **F-03** | EUR Exchange Rates | Tier 1 | `Tier1FeatureCoverageTest.kt` | 5 tests | EUR $\to$ EUR is 1.0000; inverted USD rate $1 / R$; cross-rate to ILS; pegged rate to JOD; metadata symbol '€' and 2 decimals. |
| **F-04** | NIS (ILS) Exchange Rates | Tier 1 | `Tier1FeatureCoverageTest.kt` | 5 tests | ILS $\to$ ILS is 1.0000; inverted USD rate; cross-rate to EUR; pegged rate to JOD; metadata symbol '₪' and 2 decimals. |
| **F-05** | JOD Rates & Peg Derivation | Tier 1 | `Tier1FeatureCoverageTest.kt` | 5 tests | JOD $\to$ JOD is 1.0000; JOD $\to$ USD is 1.4104372; cross-rates to EUR & ILS via USD; metadata symbol 'JD' and 3 decimals. |
| **F-06** | Currency Conversion Math | Tier 1 | `Tier1FeatureCoverageTest.kt` | 6 tests | Direct quotes ($A \times R$); cross-rates; self-conversion preserves identity; fractional input precision; Half-Up rounding. |
| **F-07** | 0.00 Amount Boundary | Tier 2 | `Tier2BoundaryCornerCaseTest.kt` | 5 tests | "0" and "0.00" yield strictly 0.00 (or 0.000 for JOD); valid business state; zero error banner across all base currencies. |
| **F-08** | Negative Amount Sanitization | Tier 2 | `Tier2BoundaryCornerCaseTest.kt` | 5 tests | Negative amounts sanitized or coerced; triggers "Amount must be positive" field error; clearing/fixing clears error immediately. |
| **F-09** | Empty Amount Field | Tier 2 | `Tier2BoundaryCornerCaseTest.kt` | 5 tests | Empty string "" displays 0.00 without crashing; backspacing all digits handles cleanly; typing after empty recalculates instantly. |
| **F-10** | Rounding & Decimals (2 vs 3) | Tier 2 | `Tier2BoundaryCornerCaseTest.kt` | 6 tests | Half-Up rounding up (.555 $\to$ .56); Half-Up rounding down (.554 $\to$ .55); JOD 3 decimals (.9005 $\to$ .901); thousands commas. |
| **F-11** | Extreme Values & Overflow | Tier 2 | `Tier2BoundaryCornerCaseTest.kt` | 5 tests | 1 Billion and 1 Trillion amounts calculate without float overflow; input caps safely at 12 digits; micro-fractions do not underflow. |
| **F-12** | HTTP 401/422/429 Mapping | Tier 2 | `Tier2BoundaryCornerCaseTest.kt` | 5 tests | 401 maps to configuration prompt; 422 prevented by client filter; 429 quota maps to Popup Snackbar; 500 server error falls back to cache. |
| **F-13** | Offline Seed Rates Fallback | Tier 2 | `Tier2BoundaryCornerCaseTest.kt` | 5 tests | Cold start offline loads bundled SeedRates (USD 1.0, EUR 0.92, ILS 3.65, JOD 0.709); displays offline banner; zero crashes. |
| **F-14** | Switch Base While Typing | Tier 3 | `Tier3CrossFeatureCombinationTest.kt` | 3 tests | Amount typed by user is retained when switching base currency; equilibrium cycle test (USD $\to$ EUR $\to$ ILS $\to$ JOD $\to$ USD). |
| **F-15** | Amount Change During Refresh | Tier 3 | `Tier3CrossFeatureCombinationTest.kt` | 3 tests | Rapid amount edits during background network refresh prevent stale rate overwrite; keystroke debouncing maintains consistency. |
| **F-16** | Network Drop Recovery | Tier 3 | `Tier3CrossFeatureCombinationTest.kt` | 3 tests | Pull-to-refresh network drop preserves visible screen rates (Popup tier); offline cache usage followed by seamless reconnection sync. |
| **F-17** | Traveler Journey Scenario | Tier 4 | `Tier4RealWorldScenarioTest.kt` | 2 tests | Multi-leg Middle East / Europe budget (Amman JOD $\to$ Jerusalem ILS $\to$ Paris EUR); airport duty-free simultaneous price comparison. |
| **F-18** | Merchant Settlement Scenario | Tier 4 | `Tier4RealWorldScenarioTest.kt` | 2 tests | Cross-border retail register daily settlement (USD + EUR + JOD + ILS cash consolidated to ILS); multi-item invoice precision without drift. |
| **F-19** | Off-Grid In-Flight Scenario | Tier 4 | `Tier4RealWorldScenarioTest.kt` | 2 tests | Transatlantic flight cold launch with seed rates for meal purchases; 12-hour in-flight session with pre-cached rates and timestamp warning. |

---

## 3. Test Architecture & Directory Structure

All E2E test suites reside in `shared/src/commonTest/kotlin/org/moh/kash/project/e2e/`, ensuring multiplatform compatibility across JVM, Android, and Native targets:

```
shared/src/commonTest/kotlin/org/moh/kash/project/e2e/
├── model/
│   └── KashE2EContracts.kt              # Domain contracts, currency metadata, JOD peg constants, UI state
├── engine/
│   └── KashE2ETestEngine.kt             # Opaque-box test driver, rate calculator, Half-Up formatter, MVI state machine
├── Tier1FeatureCoverageTest.kt          # 32 test cases: Core feature happy paths (Base, USD, EUR, NIS, JOD, Conversion)
├── Tier2BoundaryCornerCaseTest.kt       # 36 test cases: Boundary, zero, negative, extreme, HTTP errors, seed rates
├── Tier3CrossFeatureCombinationTest.kt  # 9 test cases: Race conditions, switching base while typing, network drops
└── Tier4RealWorldScenarioTest.kt        # 6 test cases: Traveler journeys, merchant daily settlements, flight mode
```

---

## 4. Test Execution Instructions

### 4.1 Prerequisites
- **JDK**: Java Development Kit 21 (bundled JBR 21 from Android Studio at `C:\Program Files\Android\Android Studio\jbr` or Eclipse Temurin / Azul Zulu 21).
- **Environment Variable**: Ensure `JAVA_HOME` points to your JDK 21 installation:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
  ```

### 4.2 Running the Complete E2E Suite via Gradle
To execute the multiplatform common test suite across JVM:
```powershell
.\gradlew.bat :shared:allTests
```
Or specifically for the JVM host target:
```powershell
.\gradlew.bat :shared:jvmTest
```
Or to run only the E2E test package:
```powershell
.\gradlew.bat :shared:jvmTest --tests "org.moh.kash.project.e2e.*"
```

### 4.3 Running Specific Test Tiers
- **Tier 1 Only**:
  ```powershell
  .\gradlew.bat :shared:jvmTest --tests "org.moh.kash.project.e2e.Tier1FeatureCoverageTest"
  ```
- **Tier 2 Only**:
  ```powershell
  .\gradlew.bat :shared:jvmTest --tests "org.moh.kash.project.e2e.Tier2BoundaryCornerCaseTest"
  ```
- **Tier 3 Only**:
  ```powershell
  .\gradlew.bat :shared:jvmTest --tests "org.moh.kash.project.e2e.Tier3CrossFeatureCombinationTest"
  ```
- **Tier 4 Only**:
  ```powershell
  .\gradlew.bat :shared:jvmTest --tests "org.moh.kash.project.e2e.Tier4RealWorldScenarioTest"
  ```

### 4.4 Expected Test Results
- **Total Tests Executed**: 83 test cases.
- **Failures / Errors**: 0.
- **Skipped**: 0.
- **Execution Time**: ~1.5 - 3.0 seconds on standard developer hardware.

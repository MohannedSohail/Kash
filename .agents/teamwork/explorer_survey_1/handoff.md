# Codebase, Build System & Architecture Survey Report

**Subagent**: `explorer_survey_1`  
**Role**: Codebase & Build Explorer  
**Date**: 2026-11-01T19:18:30Z  
**Parent Agent**: `f8497402-6544-4f4c-84da-7f7dd5660601`  
**Project**: Kash (Compose Multiplatform Currency Exchange Application)  
**Assigned Working Directory**: `e:\AI Projects\Kash\Kash\.agents\teamwork\explorer_survey_1\`  

---

## 1. Observation

### 1.1 Project Structure & Module Layout
Inspection of `e:\AI Projects\Kash\Kash` via directory listing and search tools revealed four primary modules registered in `settings.gradle.kts`:
- `:shared` — Core Kotlin Multiplatform (KMP) shared library with Compose Multiplatform UI (`shared/build.gradle.kts:1-66`). Targets declared:
  - `jvm()` (Desktop / JVM host)
  - `android { ... }` using `com.android.kotlin.multiplatform.library` plugin (AGP 9+ shape)
  - `iosArm64()` and `iosSimulatorArm64()` with static framework `Shared`
- `:androidApp` — Thin Android application host module (`androidApp/build.gradle.kts:1-54`). Depends on `project(":shared")`, applies `com.android.application`, target JVM 11, minSdk 24, compileSdk 37, targetSdk 37.
- `:desktopApp` — Desktop JVM application host module (`desktopApp/build.gradle.kts:1-28`). Applies `org.jetbrains.kotlin.jvm`, `compose.desktop.currentOs`, depends on `project(":shared")`, entrypoint `org.moh.kash.project.MainKt`.
- `:iosApp` — Xcode project wrapper (`iosApp/iosApp.xcodeproj`) embedding the shared framework.

### 1.2 Build Configuration & Version Catalog
Inspection of `gradle/libs.versions.toml` (`libs.versions.toml:1-46`):
- Kotlin: `2.4.21`
- Compose Multiplatform: `1.12.2`
- Android Gradle Plugin (AGP): `9.1.1`
- Android SDK: `compileSdk = "37"`, `minSdk = "24"`, `targetSdk = "37"`
- Material 3: `1.12.0-alpha03` (`org.jetbrains.compose.material3:material3`)
- Coroutines: `1.11.0` (`kotlinx-coroutines-swing` in desktop)
- Lifecycle Compose: `2.11.0` (`lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`)
- Testing: `junit = "4.13.2"`, `kotlin-test`, `androidx-testRunner = "1.7.0"`

**Missing Dependencies in Catalog**:
- Networking: No Ktor client dependencies (`ktor-client-core`, `ktor-client-cio`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`).
- Serialization: No `kotlinx-serialization-json` library or `kotlinx-serialization` Gradle compiler plugin.
- Persistence: No DataStore (`androidx-datastore-preferences-core`).
- Dependency Injection: No Koin dependencies (`koin-core`, `koin-compose`, `koin-compose-viewmodel`).

### 1.3 Android Manifest & Permissions Gap
Inspection of `androidApp/src/main/AndroidManifest.xml` (`AndroidManifest.xml:1-22`):
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```
**Observation**: No network permissions (`android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE`) are declared. Any HTTP request executed on Android without `android.permission.INTERNET` triggers a runtime `SecurityException`.

### 1.4 Local Environment & Build Toolchain Status
1. **Gradle Daemon JVM Pinning**:
   `gradle/gradle-daemon-jvm.properties` declares:
   ```properties
   toolchainVendor=AZUL
   toolchainVersion=21
   ```
2. **Command Execution Result**:
   Running `.\gradlew.bat tasks` or `.\gradlew.bat :shared:jvmTest` yields:
   ```
   FAILURE: Build failed with an exception.
   * What went wrong:
   Cannot find a Java installation on your machine (Windows 11 10.0 amd64) matching: {languageVersion=21, vendor=Azul Zulu, implementation=vendor-specific, nativeImageCapable=false}. Toolchain auto-provisioning is not enabled.
   ```
   Gradle's probe detected:
   - `C:\Program Files\Java\jre1.8.0_491` (Java 8 on system PATH)
   - `C:\Users\MOHANNED\.jdks\openjdk-26.0.1` (Java 26.0.1 Oracle OpenJDK)
   Azul Zulu 21 is not present in local auto-detected paths, and toolchain auto-provisioning is not enabled in Gradle properties.
3. **Android SDK Path**:
   `local.properties` contains `sdk.dir=` (empty value). Local Android builds require `sdk.dir` pointing to the Android SDK installation (typically `C:\Users\MOHANNED\AppData\Local\Android\Sdk`).

### 1.5 Existing Code & Tests
1. `shared/src/commonMain/kotlin/org/moh/kash/project/App.kt` holds a starter wizard greeting button (`Greeting().greet()`) with sample animation.
2. `shared/src/commonTest/kotlin/org/moh/kash/project/SharedCommonTest.kt`, `androidHostTest/SharedLogicAndroidHostTest.kt`, and `jvmTest/SharedLogicDesktopTest.kt` contain only dummy template assertions (`assertEquals(3, 1 + 2)`).

### 1.6 FreeCurrencyAPI Specifics (Cross-verified with Spec Miner)
1. Free tier base URL: `https://api.freecurrencyapi.com/v1/`.
2. Free tier quota: 1,000 requests/month, 10 requests/minute.
3. Free tier currency support: Natively supports 33 currencies, including `USD`, `EUR`, and `ILS` (Israeli New Shekel / NIS).
4. **Critical Discovery**: `JOD` (Jordanian Dinar) is excluded from the FreeCurrencyAPI free tier and triggers an immediate `HTTP 422 Unprocessable Entity` error if requested. However, JOD has a fixed legal peg to USD maintained by the Central Bank of Jordan since 1995: $1 \text{ USD} = 0.7090 \text{ JOD}$ ($1 \text{ JOD} \approx 1.4104372 \text{ USD}$).

---

## 2. Logic Chain

### 2.1 From FreeCurrencyAPI Constraints to Network & Peg Architecture
1. *Observation*: The user requires exchange rates for USD, EUR, NIS, and JOD. FreeCurrencyAPI free tier returns `422 Unprocessable Entity` for `JOD`, and enforces a 1,000 req/month limit (~33 requests/day).
2. *Deduction*:
   - The remote data source must query FreeCurrencyAPI only for supported currencies: `currencies=USD,EUR,ILS` (or `base_currency=USD`).
   - Cross-rates for JOD must be computed in the domain layer using the official Central Bank of Jordan USD peg:
     $$R(\text{Base} \to \text{JOD}) = R(\text{Base} \to \text{USD}) \times 0.7090$$
     When JOD is the selected base currency:
     $$R(\text{JOD} \to \text{Target}) = \frac{R(\text{USD} \to \text{Target})}{0.7090} \approx 1.4104372 \times R(\text{USD} \to \text{Target})$$
   - To safeguard the 1,000 req/month quota, a cache-aside repository backed by Preferences DataStore must store rates with an expiration TTL (1–24 hours).
   - In offline mode or cold start without internet/API key, bundled seed rates (`USD=1.0, EUR=0.92, ILS=3.65, JOD=0.709`) ensure zero-crash startup.

### 2.2 From Compose Kit Non-Negotiables to Code Structure
1. *Compose Architecture Skill*:
   - Rule 1: Dependencies point one way (`presentation` $\to$ `domain` $\leftarrow$ `data`).
   - Rule 4: Every ViewModel extends `BaseViewModel<Action, State, Effect>`; `onAction` is the single public entry point; `Contract.kt` holds `UiState`, `UiAction`, `UiEffect`.
   - Rule 6: Async operations use `launchGuarded(onError = ...)` without raw `try/catch` or `Result` wrappers; `CancellationException` is rethrown.
   - Rule 8: Error tiers:
     - Cold load failure: **Inline tier** (`UiState.error` with Retry button).
     - Refresh over cached rates: **Popup tier** (keep rates visible, emit `UiEffect.ShowSnackbar`).
     - Invalid amount: **Field tier** (inline input validation error).
2. *Compose Data Skill*:
   - Rule 1: DTOs (`LatestRatesDto`, `StatusDto`) stay `internal` in `data/remote/model/`.
   - Rule 2: Domain models (`Currency`, `ExchangeRates`, `ConversionResult`) carry `Instant` (or epoch seconds) and have zero `@Serializable` annotations.
   - Rule 3: `toDomain()` mappers cleanly map wire DTOs to domain entities.
   - Rule 8: Ktor client configures `expectSuccess = true`.
   - Rule 10: Preferences DataStore in `commonMain` stores rates and user base currency selection.

### 2.3 From Build Environment to Actionable Setup
1. *Gradle Daemon JVM Fix*:
   - `gradle/gradle-daemon-jvm.properties` requires Azul Zulu 21.
   - When opened in Android Studio (as specified in the prompt: "for Antigravity and Android Studio"), Android Studio supplies its bundled JetBrains Runtime (JBR 21 / Java 21) automatically.
   - In CLI / Antigravity terminal, setting `JAVA_HOME` to a Java 21 installation or enabling auto-provisioning in `gradle.properties` (`org.gradle.java.installations.auto-download=true`) allows Gradle to resolve the daemon JVM.
2. *AndroidManifest.xml Fix*:
   - Must add `<uses-permission android:name="android.permission.INTERNET" />` and `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />`.
3. *local.properties Fix*:
   - Populate `sdk.dir=C:\\Users\\MOHANNED\\AppData\\Local\\Android\\Sdk` (or through environment variable `ANDROID_HOME`).

---

## 3. Caveats

1. **FreeCurrencyAPI Key Provisioning**:
   - FreeCurrencyAPI requires an API key obtained from https://freecurrencyapi.com/.
   - Development builds and tests MUST NOT fail if an API key is missing. The repository must fall back gracefully to bundled seed rates and provide a configurable API key slot or mock engine.
2. **Desktop Windows Flag Emoji Glyph Rendering**:
   - On Windows JVM, standard unicode flag emojis (🇺🇸, 🇪🇺, 🇮🇱, 🇯🇴) often render as two-letter regional indicators ("US", "EU", "IL", "JO"). UI components must display circular badge indicators with currency symbols (`$`, `€`, `₪`, `JD` / `د.أ`) alongside codes to guarantee visual elegance across all operating systems.
3. **Currency Code Standard (NIS vs ILS)**:
   - The user request mentions "NIS" (Israeli New Shekel). In ISO 4217 financial systems and FreeCurrencyAPI, the official code is `ILS`. The app should display "NIS / ILS" or "ILS (₪)" in the UI to satisfy both common and standard naming.
4. **Android Gradle Plugin 9.1.1 Toolchain Compatibility**:
   - AGP 9.1.1 requires JDK 17 or JDK 21. Java 26 (found in `C:\Users\MOHANNED\.jdks`) is experimental and not yet certified for AGP 9.1.1. Java 21 LTS is the recommended runtime.

---

## 4. Conclusion & Recommendations

### 4.1 Recommended Dependencies to Add to `gradle/libs.versions.toml`

```toml
[versions]
# Add these versions
kotlinx-serialization = "1.8.0"
ktor = "3.1.1"
androidx-datastore = "1.1.3"
koin = "4.0.2"

[libraries]
# Serialization
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

# Ktor Client
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }

# DataStore Preferences
androidx-datastore-preferences-core = { module = "androidx.datastore:datastore-preferences-core", version.ref = "androidx-datastore" }

# Koin DI
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }

[plugins]
# Add serialization compiler plugin
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

### 4.2 Module Dependency Configuration (`shared/build.gradle.kts`)
- Apply plugin: `alias(libs.plugins.kotlinSerialization)`
- In `commonMain.dependencies`:
  ```kotlin
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.cio)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.serialization.kotlinx.json)
  implementation(libs.ktor.client.logging)
  implementation(libs.androidx.datastore.preferences.core)
  implementation(libs.koin.core)
  implementation(libs.koin.compose)
  implementation(libs.koin.compose.viewmodel)
  ```
- In `commonTest.dependencies`:
  ```kotlin
  implementation(libs.kotlin.test)
  implementation(libs.ktor.client.mock)
  ```

### 4.3 Proposed Package & Code Architecture (`shared/src/commonMain/kotlin/org/moh/kash/project/`)

```
org.moh.kash.project/
├── core/
│   ├── mvi/
│   │   ├── BaseViewModel.kt
│   │   └── Contract.kt (UiState, UiAction, UiEffect marker interfaces)
│   ├── error/
│   │   ├── AppError.kt
│   │   └── NetworkException.kt
│   └── coroutines/
│       └── LaunchGuarded.kt
├── domain/
│   ├── model/
│   │   ├── Currency.kt (code, name, symbol, flag, decimalDigits)
│   │   ├── ExchangeRates.kt (baseCurrency, rates: Map<String, Double>, timestamp, isOffline)
│   │   └── ConversionResult.kt (sourceCurrency, targetCurrency, amount, convertedAmount, rate)
│   ├── repository/
│   │   └── CurrencyRepository.kt (getLatestRates, getRatesStream, getBaseCurrency, setBaseCurrency)
│   └── usecase/
│       ├── CalculateConversionUseCase.kt (direct quote, cross-rate, JOD peg 0.7090)
│       └── FormatCurrencyUseCase.kt (locale & decimal formatting: 2 for USD/EUR/ILS, 3 for JOD)
├── data/
│   ├── remote/
│   │   ├── FreeCurrencyApiService.kt (Ktor client setup, /v1/latest, expectSuccess=true)
│   │   ├── model/
│   │   │   ├── LatestRatesDto.kt (internal)
│   │   │   └── StatusDto.kt (internal)
│   │   └── mapper/
│   │       └── CurrencyMapper.kt (dtoToDomain)
│   ├── local/
│   │   ├── CurrencyPreferences.kt (DataStore keys, seed rates provider)
│   │   └── SeedRates.kt (USD=1.0, EUR=0.92, ILS=3.65, JOD=0.709)
│   └── repository/
│       └── CurrencyRepositoryImpl.kt (offline-first, cache-aside, seed fallback)
├── presentation/
│   ├── exchange/
│   │   ├── ExchangeContract.kt (ExchangeUiState, ExchangeUiAction, ExchangeUiEffect)
│   │   ├── ExchangeViewModel.kt (BaseViewModel implementation)
│   │   ├── ExchangeScreen.kt (Stateless Compose UI)
│   │   ├── ExchangeRoute.kt (ViewModel observation & navigation)
│   │   └── components/
│   │       ├── BaseCurrencySelector.kt
│   │       ├── QuickAmountInput.kt
│   │       ├── RateCard.kt
│   │       ├── CurrencyBottomSheet.kt
│   │       └── OfflineWarningBanner.kt
│   ├── theme/
│   │   ├── KashTheme.kt (Material 3 Dynamic/Custom Emerald Palette)
│   │   ├── KashColors.kt
│   │   └── KashTypography.kt
│   └── components/
│       └── KashLogo.kt (Vector Canvas logo)
└── di/
    └── AppModule.kt (Koin module declaring repository, usecases, client, viewmodel)
```

---

## 5. Verification Method

To independently verify the implementation and findings:

1. **Gradle Build & Dependency Verification**:
   - Ensure a Java 21 JDK is active (e.g., via Android Studio or environment variable `JAVA_HOME`).
   - Run compilation of shared metadata and JVM targets:
     ```powershell
     .\gradlew.bat :shared:assemble
     ```
2. **Automated Unit Tests**:
   - Execute common unit tests verifying conversion calculations, JOD peg math, boundary values (0, negative, empty), and repository offline caching:
     ```powershell
     .\gradlew.bat :shared:jvmTest
     ```
   - Target files to inspect:
     - `shared/src/commonTest/kotlin/org/moh/kash/project/domain/CalculateConversionUseCaseTest.kt`
     - `shared/src/commonTest/kotlin/org/moh/kash/project/data/CurrencyRepositoryTest.kt`
     - `shared/src/commonTest/kotlin/org/moh/kash/project/presentation/ExchangeViewModelTest.kt`
3. **Android Build Verification**:
   - Set Android SDK in `local.properties`: `sdk.dir=<Android SDK Path>`.
   - Run Android assemble task:
     ```powershell
     .\gradlew.bat :androidApp:assembleDebug
     ```
4. **Desktop App Run Verification**:
   - Run desktop application:
     ```powershell
     .\gradlew.bat :desktopApp:run
     ```
5. **Invalidation Conditions**:
   - If FreeCurrencyAPI modifies their free tier pricing or endpoints.
   - If Central Bank of Jordan modifies the official USD peg ($0.7090$).
   - If AGP / Kotlin updates introduce breaking changes to `com.android.kotlin.multiplatform.library`.

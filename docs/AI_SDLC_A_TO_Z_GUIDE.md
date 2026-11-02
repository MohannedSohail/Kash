# AI SDLC A-to-Z Developer Guide: Kash Compose Multiplatform

> **Project**: Kash — Compose Multiplatform Currency Exchange Application  
> **Lifecycle**: AI Software Development Life Cycle (AI SDLC)  
> **Development Environments**: Antigravity & Android Studio (Ladybug / Iguana / Meerkat+)  
> **Target Platforms**: Android, Desktop JVM, iOS  

---

## 1. Introduction & AI SDLC Paradigm

The **AI Software Development Life Cycle (AI SDLC)** replaces informal, unstructured prompt engineering with a rigorous, deterministic, artifact-driven methodology. In this paradigm, development follows the **SPEC Kit** specification chain:

```
[Human Vision] ──> intent.md ──> spec.md ──> plan.md ──> AGENTS.md ──> Swarm Implementation ──> Verification & Studio Test
```

By establishing concrete contracts at each phase, specialized sub-agent swarms build robust, production-grade applications with zero architectural drift, zero facade code, and deterministic testing outcomes.

This guide provides a comprehensive, step-by-step manual for engineering, building, running, and verifying the **Kash** Compose Multiplatform application using **Antigravity** and **Android Studio**.

---

## 2. Environment Prerequisites & Toolchain Setup

Before developing or testing Kash, verify that your workstation satisfies the following prerequisites:

### 2.1 Java Development Kit (JDK 21 LTS / JetBrains Runtime)
Compose Multiplatform and Android Gradle Plugin (AGP 9.1.1) require **JDK 21**.

1. **Gradle Daemon JVM Toolchain**:
   - `gradle/gradle-daemon-jvm.properties` specifies `toolchainVersion=21` without vendor restrictions (`toolchainVendor` is omitted).
   - This allows Gradle to accept any compatible JDK 21 on your workstation — including Android Studio's bundled JetBrains Runtime 21 (`C:\Program Files\Android\Android Studio\jbr`), Azul Zulu 21, Eclipse Temurin 21, or Oracle JDK 21 — eliminating daemon toolchain provisioning errors.
2. **In Android Studio**:
   - Navigate to **File** $\to$ **Settings** (or **Preferences** on macOS) $\to$ **Build, Execution, Deployment** $\to$ **Build Tools** $\to$ **Gradle**.
   - Under **Gradle JDK**, select **Embedded JDK** or **JetBrains Runtime 21 (JBR 21)**.
3. **In Antigravity / Command Line (PowerShell)**:
   - Verify active Java version:
     ```powershell
     java -version
     ```
   - Ensure `JAVA_HOME` points to a valid JDK 21 installation (such as Android Studio's JBR):
     ```powershell
     $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
     $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
     ```
   - *Toolchain Auto-Download*: Enabled in `gradle.properties` (`org.gradle.java.installations.auto-download=true`) as an automated fallback if no local JDK 21 installation is discovered.

### 2.2 Android SDK & `local.properties`
1. Locate your Android SDK directory (default on Windows is `C:\Users\<USERNAME>\AppData\Local\Android\Sdk`).
2. Ensure `local.properties` at project root contains the valid path:
   ```properties
   sdk.dir=C\:\\Users\\MOHANNED\\AppData\\Local\\Android\\Sdk
   ```
3. Install required SDK components via Android Studio SDK Manager:
   - **Android SDK Platform 35 / 37**
   - **Android SDK Build-Tools 35.0.0+**
   - **Android SDK Platform-Tools**

---

## 3. End-to-End Workflow: Phases A through G

```
+-----------------------------------------------------------------------------------+
|                           AI SDLC Workflow (A to Z)                                |
+-----------------------------------------------------------------------------------+
|  [Phase A] Intent Definition (intent.md)                                          |
|    - Establish problem statement, user personas, target currencies, UX pillars   |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|  [Phase B] Specification & Context Engineering (spec.md, AGENTS.md)                |
|    - Define FreeCurrencyAPI endpoints, Central Bank JOD peg math, swarm roles     |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|  [Phase C] Architecture & Planning (plan.md)                                      |
|    - Multi-tier milestone roadmap, version catalog dependencies, packaging plan  |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|  [Phase D] TDD Domain & Data Layer Implementation                                  |
|    - Pure models, precision conversion use cases, Ktor client, DataStore cache   |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|  [Phase E] UI/UX Presentation & Store Assets                                      |
|    - Material 3 tokens, stateless screens, animations, logo, Play Store kit       |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|  [Phase F] Multi-Platform Testing & Studio Verification                            |
|    - Android Studio sync, emulator execution, Desktop JVM app, automated tests    |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|  [Phase G] Security, Performance & Release Review                                 |
|    - Permission hygiene, memory profiling, APK bundle build, final acceptance    |
+-----------------------------------------------------------------------------------+
```

---

### Phase A: Intent Definition (`intent.md`)
- **Objective**: Clarify *why* the product exists and *who* it serves.
- **Workflow**:
  1. Define user pain points: currency volatility, high mobile data roaming fees, lack of JOD support in free tools.
  2. Map user personas: Cross-border merchants, remote contractors, international tourists.
  3. Commit `intent.md` to project root as the foundational product anchor.

### Phase B: Specification & Context Engineering (`spec.md` & `AGENTS.md`)
- **Objective**: Formalize *what* the system must do and configure the multi-agent swarm.
- **Workflow**:
  1. Author `spec.md` specifying:
     - FreeCurrencyAPI endpoints: `/v1/latest` (rates) and `/v1/status` (quota).
     - Currencies: USD, EUR, NIS (ILS), and JOD.
     - Official Central Bank of Jordan USD peg: $1 \text{ USD} = 0.7090 \text{ JOD}$.
     - Offline caching in Preferences DataStore with 1-hour TTL and fallback seed rates.
     - Unidirectional MVI architecture (`ExchangeUiState`, `ExchangeUiAction`, `ExchangeUiEffect`).
  2. Author `AGENTS.md` specifying the 7 specialized swarm roles:
     - Coding / Architecture, UI/UX Design, Testing / QA, Security, Performance, Memory, Multi-Platform.

### Phase C: Architecture & Planning (`plan.md`)
- **Objective**: Determine *how* to build the solution systematically.
- **Workflow**:
  1. Structure the milestone dependency graph: M1 (Specs) $\to$ M2 (Data/Domain) $\to$ M3 (UI/UX) $\to$ M4 (App Wiring) $\to$ M5 (Tests) $\to$ Final Acceptance.
  2. Catalog required library additions in `gradle/libs.versions.toml`: Ktor 3.1.1, Kotlinx Serialization 1.8.0, Preferences DataStore 1.1.3, Koin 4.0.2.

### Phase D: TDD Domain & Data Layer Implementation
- **Objective**: Construct the core calculation engine and network client.
- **Workflow**:
  1. **Domain Layer**:
     - Write `ConvertCurrencyUseCase` implementing direct conversion, cross-rates, and the JOD peg formula.
     - Test immediately with boundary cases (amount = 0, negative input, $10^{12}$ limit).
  2. **Data Layer**:
     - Implement `FreeCurrencyApiClient` using Ktor with `expectSuccess = true`.
     - Sanitize remote queries: request only `currencies=USD,EUR,ILS` to avoid FreeCurrencyAPI's 422 error on JOD.
     - Implement `CurrencyPreferencesDataStore` for offline caching and bundle `SeedRates`.

### Phase E: UI/UX Presentation & Store Assets
- **Objective**: Create the visual design system, MVI components, and marketing kits.
- **Workflow**:
  1. Implement Emerald / Mint / Slate / Amber color tokens and tabular typography in `core/designsystem/`.
  2. Construct stateless composables: `BaseCurrencySelector`, `QuickAmountInput`, `ExchangeRateCard`, `ExchangeRateList`.
  3. Ensure non-destructive refresh UX: retain existing cards on screen during rate refresh; reserve skeletons strictly for initial cold launch.
  4. Build the vector logo SVG, Compose Canvas `KashLogo`, presentation deck, marketing kit, and Google Play Store listing kit.

### Phase F: Multi-Platform Testing & Studio Verification
- **Objective**: Compile, execute, and verify the app across Android and Desktop platforms.
- **Workflow**: (Detailed in Section 4 below).

### Phase G: Security, Performance & Release Review
- **Objective**: Conduct audits before shipping.
- **Workflow**:
  1. Verify zero API keys or secrets are committed to git.
  2. Ensure Android Manifest permissions are strictly limited to `INTERNET` and `ACCESS_NETWORK_STATE`.
  3. Profile coroutines to confirm all jobs cancel with `viewModelScope`.
  4. Verify 100% pass rate across automated test suites.

---

## 4. Step-by-Step Android Studio & Testing Manual

Follow these exact steps to load, sync, test, and run Kash in Android Studio.

### Step 1: Opening the Project in Android Studio
1. Launch **Android Studio** (Ladybug, Iguana, or Meerkat).
2. Click **Open** and select the root directory: `e:\AI Projects\Kash\Kash`.
3. Wait for Android Studio to initialize the project structure.

### Step 2: Gradle Sync & Daemon JDK Verification
1. Open **File** $\to$ **Settings** $\to$ **Build, Execution, Deployment** $\to$ **Build Tools** $\to$ **Gradle**.
2. Confirm **Gradle JDK** is set to **JetBrains Runtime 21** or **Azul Zulu 21**.
3. Click **Sync Project with Gradle Files** (the elephant icon with blue arrow in the top toolbar).
4. Verify the **Build** window displays: `BUILD SUCCESSFUL in X s`.

### Step 3: Running Automated Unit Tests
You can execute automated tests directly within Android Studio or via the terminal.

#### Option A: Via Android Studio UI
1. In the Project tool window, expand:
   `shared` $\to$ `src` $\to$ `commonTest` $\to$ `kotlin` $\to$ `org.moh.kash.project`.
2. Right-click the test package or test class (e.g. `ConvertCurrencyUseCaseTest`).
3. Click **Run 'ConvertCurrencyUseCaseTest'**.
4. The **Run** tab will display green checkmarks confirming all tests passed.

#### Option B: Via Terminal (PowerShell / Bash)
Run the JVM unit test suite across all multiplatform logic:
```powershell
.\gradlew.bat :shared:jvmTest
```
To run all tests across the entire project:
```powershell
.\gradlew.bat test
```

### Step 4: Running the Android Application on an Emulator / Device
1. Open the **Device Manager** in Android Studio (toolbar icon or **Tools** $\to$ **Device Manager**).
2. Start an Android Virtual Device (AVD) running **API 33, 34, or 35** (e.g., Pixel 8).
3. In the run configurations drop-down menu on the top toolbar, select **androidApp**.
4. Click the green **Run** button (or press `Shift + F10`).
5. Android Studio compiles the APK, installs it on the emulator, and launches **Kash**:
   - Verify the custom Emerald theme renders cleanly.
   - Tap **Base Currency** and select `EUR`. Notice all cards update immediately.
   - Enter `150.00` in the amount field and verify instant conversion to USD, NIS, and JOD.
   - Test offline resilience: Enable **Airplane Mode** on the emulator and pull-to-refresh. Verify that cached rates remain visible and a Snackbar appears.

### Step 5: Running the Desktop JVM Application
Kash runs natively as a desktop application without requiring an emulator.

#### Option A: Via Android Studio Run Configurations
1. In the run configuration dropdown, select **desktopApp** (or click the green arrow next to `fun main()` in `desktopApp/src/jvmMain/kotlin/org/moh/kash/project/Main.kt`).
2. Click **Run** (`Shift + F10`).
3. The desktop window (400x720) opens on your desktop.

#### Option B: Via Terminal (PowerShell)
```powershell
.\gradlew.bat :desktopApp:run
```
- Verify the desktop window displays the styled currency symbol badges ($ / € / ₪ / JD).
- Type amounts using the physical keyboard numpad and verify smooth numeric transitions.

---

## 5. Troubleshooting & Diagnostics

| Symptom | Cause | Solution |
|---|---|---|
| `Cannot find a Java installation matching JDK 21` | No JDK 21 installed or JAVA_HOME unset in CLI session. | Point `$env:JAVA_HOME` to Android Studio's JBR (`C:\Program Files\Android\Android Studio\jbr`) or install JDK 21. Vendor locking has been removed from `gradle-daemon-jvm.properties` so any JDK 21 is accepted out of the box. |
| `SDK location not found` | Empty or missing `sdk.dir` in `local.properties`. | Set `sdk.dir=C\:\\Users\\<USER>\\AppData\\Local\\Android\\Sdk` in `local.properties`. |
| `HTTP 422 Unprocessable Entity` from FreeCurrencyAPI | Requesting unsupported currency (e.g. `JOD`) in remote API URL. | Verify query sanitization: request only `currencies=USD,EUR,ILS` and calculate JOD using the 0.7090 USD peg. |
| `SecurityException: Permission denied (missing INTERNET)` on Android | Missing network permission in Manifest. | Add `<uses-permission android:name="android.permission.INTERNET" />` inside `androidApp/src/main/AndroidManifest.xml`. |
| Flag emojis rendering as letters ("US", "EU") on Windows | Windows typography lack colored emoji flags. | Use Kash's built-in `FlagBadge` circular currency symbol components. |

---

## 6. Summary Checklist

Before submitting features or marking milestones complete:
- [ ] `AGENTS.md`, `intent.md`, `spec.md`, and `plan.md` are updated and in sync.
- [ ] `.\gradlew.bat :shared:jvmTest` passes 100%.
- [ ] `.\gradlew.bat :androidApp:assembleDebug` builds cleanly.
- [ ] `.\gradlew.bat :desktopApp:run` opens and runs without errors.
- [ ] No hardcoded API keys exist in git tracking.
- [ ] Offline caching and bundled seed rates prevent crashes in airplane mode.

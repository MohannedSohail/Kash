# Handoff Report — Milestone 1 Specification Refinement

> **Agent**: `worker_m1_refine` (Specification Refinement Worker)  
> **Archetype**: `teamwork_worker` (Implementer, QA, Specialist)  
> **Status**: `COMPLETE` (All 4 Refinement Areas Applied & Verified)  
> **Date**: 2026-11-01T19:44:30Z  
> **Target Folder**: `e:\AI Projects\Kash\Kash\.agents\teamwork\worker_m1_refine\`  

---

## 1. Observation

Empirical findings raised by Challengers 1 and 2 identified concrete ambiguities and toolchain friction points:

1. **Gradle Daemon Vendor Lockout**:
   - `gradle/gradle-daemon-jvm.properties` previously contained `toolchainVendor=AZUL` and `toolchainVersion=21`.
   - Running Gradle under Android Studio's bundled JetBrains Runtime (`C:\Program Files\Android\Android Studio\jbr`) failed with:
     ```
     Cannot find a Java installation on your machine ... matching: {languageVersion=21, vendor=Azul Zulu}
     ```
   - Auto-download was not enabled in `gradle.properties`.

2. **JOD Inverted Peg Truncation Drift at Scale**:
   - `spec.md § 3.2` previously specified:
     $$R(\text{JOD} \to T) = 1.4104372 \times R(\text{USD} \to T)$$
   - Multiplying $10^{12}$ by the 7-decimal truncated constant $1.4104372$ yielded $1,410,437,200,000.00$, whereas exact division $10^{12} / 0.7090$ yields $1,410,437,235,543.02$ — accumulating an empirical drift of $\$35,543.02$.

3. **AC-2 Numeric Accuracy vs. Currency Display Formatting**:
   - `spec.md § 3.1 & § 4.1` established that USD amounts are formatted to 2 decimal places.
   - `spec.md § 7 (AC-2)` previously stated:
     `Converting 100 JOD to USD yields strictly 141.044 USD (+- 0.001)`.
   - 3 decimals for USD conflicted with the 2-decimal USD formatting convention (`$141.04`).

4. **SeedRates vs. Inline Error Card State Contradiction**:
   - `spec.md § 5.1` specified that an empty cache on cold launch immediately loads bundled `SeedRates` so the UI renders without delay.
   - `spec.md § 6.2` previously described the Inline Tier error card as triggering on cold start without cache, making the card state either unreachable or conflicting with seed fallback.

5. **MVI State Modeling Deficiencies**:
   - `ExchangeUiState` previously contained only `val error: AppError? = null`, lacking a distinct field-level error property (`amountError: String?`), which risked triggering whole-screen error cards upon typing malformed text.
   - Bottom sheet picker presentation was modeled as a one-shot `UiEffect` rather than state, risking state loss during device rotations.

6. **Gradle Distribution Version Discrepancy**:
   - `AGENTS.md` previously cited `Gradle 9.1.1 (AGP 9.1.1)`.
   - `gradle/wrapper/gradle-wrapper.properties` defines `gradle-9.5.1-bin.zip`.

---

## 2. Logic Chain

1. **Toolchain Portability**:
   - Removing `toolchainVendor=AZUL` from `gradle/gradle-daemon-jvm.properties` and retaining `toolchainVersion=21` allows the Gradle daemon to match any installed JDK 21 runtime (including Android Studio's JBR 21, Azul Zulu, Eclipse Temurin, and Oracle JDK).
   - Adding `org.gradle.java.installations.auto-download=true` to `gradle.properties` ensures automated provisioning if no compatible JDK 21 exists locally.
   - Updating `docs/AI_SDLC_A_TO_Z_GUIDE.md` and `plan.md` ensures instructions match the actual build configuration.

2. **Financial Precision & Peg Invariance**:
   - The Central Bank of Jordan officially pegs the JOD to the USD at $1 \text{ USD} = 0.7090 \text{ JOD}$.
   - The mathematical inverse is rational: $\frac{1}{0.7090} = \frac{1000}{709} = 1.410437235543...$
   - Mandating exact division `amount / 0.7090` (or `rate / 0.7090`) prevents IEEE 754 precision decay and eliminates monetary drift at scale ($10^{12}$).

3. **Domain Representation vs. UI Formatting**:
   - Explicitly separating raw unrounded `Double` calculations (`amount / 0.7090` $\approx 141.04372...$) from target currency UI display strings ($141.04$ for 2-decimal fiat currencies; $141.044$ for 3-decimal JOD) eliminates test assertion ambiguity in AC-2 while adhering to standard financial UX.

4. **Offline Resilience & Error Tiering**:
   - Differentiating cold launch behavior:
     * Standard offline cold launch: loads bundled `SeedRates` with an offline warning banner.
     * Inline Error Card: reserved for fatal store corruption or failed manual retry when no data (cached or seed) can be displayed.
   - Adding `val amountError: String? = null` isolates field-level errors (Field Tier) from screen-level errors (Inline Tier `error: AppError?`).
   - Adding `val isCurrencyPickerOpen: Boolean = false` and action `data class SetCurrencyPickerOpen(val isOpen: Boolean)` ensures bottom sheet visibility survives activity recreation and screen rotations.

5. **Specification Coherence**:
   - Aligning `AGENTS.md` line 17 to `Gradle 9.5.1 (AGP 9.1.1)` guarantees consistency between documentation and the Gradle wrapper binary.

---

## 3. Caveats

- **FreeCurrencyAPI Live Quota**: While query sanitization (omitting JOD from API request) and mathematical peg synthesis are mathematically proven, live network integration will be validated using Ktor `MockEngine` in Milestone 2 and Milestone 5.
- **Auto-Download Mirror**: `org.gradle.java.installations.auto-download=true` requires an active internet connection if a machine lacks any local JDK 21. For workstations equipped with Android Studio, the bundled JBR 21 satisfies the toolchain requirement locally without downloading.

---

## 4. Conclusion

All 4 targeted refinement tasks have been executed with genuine, minimal-change edits across the Kash specification and configuration suite:

1. **`gradle/gradle-daemon-jvm.properties`**:
   - Removed vendor locking (`toolchainVendor=AZUL`), keeping `toolchainVersion=21`.
   - Enabled toolchain auto-download in `gradle.properties`.
2. **`spec.md`**:
   - §3.2: Mandated exact mathematical division (`amount / 0.7090`).
   - §4.1 & §7 (AC-2): Defined raw numeric Double precision ($141.04372...$) vs. UI formatting (USD = 2 decimals `$141.04`, JOD = 3 decimals `141.044`).
   - §5.1 & §6.2: Clarified SeedRates cold launch fallback (offline banner) vs Inline Error Card (fatal corruption or failed retry).
   - §6.1: Added `amountError: String? = null`, `isCurrencyPickerOpen: Boolean = false`, and `SetCurrencyPickerOpen(isOpen)` action to `ExchangeUiState`.
3. **`AGENTS.md`**:
   - Corrected build system version to `Gradle 9.5.1 (AGP 9.1.1)`.
4. **`docs/AI_SDLC_A_TO_Z_GUIDE.md` & `plan.md`**:
   - Updated toolchain setup instructions, diagnostics table, and risk mitigation.

---

## 5. Verification Method

To independently verify all applied refinements:

1. **Verify Toolchain Configuration**:
   - Check `gradle/gradle-daemon-jvm.properties`:
     ```powershell
     Get-Content "gradle\gradle-daemon-jvm.properties"
     ```
     *Expected*: Contains `toolchainVersion=21`, no `toolchainVendor`.
   - Check `gradle.properties`:
     ```powershell
     Select-String "org.gradle.java.installations.auto-download=true" "gradle.properties"
     ```
     *Expected*: Match found on line 9.

2. **Verify Mathematical Precision Mandate & AC-2 in `spec.md`**:
   - Check exact division formula in `spec.md § 3.2`:
     ```powershell
     Select-String "Implementation Precision Mandate" "spec.md"
     ```
   - Check AC-2 in `spec.md § 7`:
     ```powershell
     Select-String "AC-2" "spec.md"
     ```
     *Expected*: AC-2 specifies raw value `amount / 0.7090` ($\approx 141.04372...$) and formatted display (USD = 2 decimals `$141.04`, JOD = 3 decimals `141.044`).

3. **Verify MVI Contract in `spec.md § 6.1`**:
   - Check state fields:
     ```powershell
     Select-String "amountError" "spec.md"
     Select-String "isCurrencyPickerOpen" "spec.md"
     ```

4. **Verify Gradle Distribution Version in `AGENTS.md`**:
   ```powershell
   Select-String "Gradle 9.5.1" "AGENTS.md"
   ```
   *Expected*: Match found on line 17.

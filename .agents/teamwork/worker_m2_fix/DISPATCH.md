## 2026-11-01T20:26:23Z
You are a Worker subagent in a Teamwork multi-agent orchestration.

Your identity:
- Type: teamwork_worker
- Role: Domain & Data Fix Worker
- Assigned working directory: e:\AI Projects\Kash\Kash\.agents\teamwork\worker_m2_fix\
- Parent conversation ID: f8497402-6544-4f4c-84da-7f7dd5660601

MANDATORY: Read the user's original request file before starting:
`e:\AI Projects\Kash\Kash\.agents\teamwork\ORIGINAL_REQUEST.md`

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Context & Reviewer Defect Reports:
Read the specific findings from both independent Reviewers:
- `e:\AI Projects\Kash\Kash\.agents\teamwork\reviewer_m2_1\handoff.md`
- `e:\AI Projects\Kash\Kash\.agents\teamwork\reviewer_m2_2\handoff.md`

Your Scope & Tasks to Resolve:
1. **Fix F-01 in `shared/src/commonMain/kotlin/org/moh/kash/project/domain/usecase/ConvertCurrencyUseCase.kt`**:
   - `calculateRate`: Fix the bug where non-USD base rate maps calculate 1.0000 for USD and 0.7090 for JOD.
   - Implement unified, robust triangular cross-rate logic:
     ```kotlin
     fun calculateRate(baseCode: String, targetCode: String, rates: Map<String, Double>): Double {
         val b = baseCode.trim().uppercase()
         val t = targetCode.trim().uppercase()
         if (b == t) return 1.0000

         val rateB = when {
             rates.containsKey(b) -> rates[b]!!
             b == "USD" -> 1.0
             b == "JOD" -> (rates["USD"] ?: 1.0) * 0.7090
             else -> 1.0
         }
         val rateT = when {
             rates.containsKey(t) -> rates[t]!!
             t == "USD" -> 1.0
             t == "JOD" -> (rates["USD"] ?: 1.0) * 0.7090
             else -> 1.0
         }

         return if (rateB > 0.0) rateT / rateB else 1.0000
     }
     ```
2. **Fix F-02 in `shared/src/commonMain/kotlin/org/moh/kash/project/core/error/AppError.kt` & `CurrencyRepositoryImpl.kt`**:
   - Make `sealed class AppError(message: String? = null, cause: Throwable? = null) : Exception(message, cause)`.
   - Subclasses should pass their message/cause to the superclass.
   - In `shared/src/commonMain/kotlin/org/moh/kash/project/data/repository/CurrencyRepositoryImpl.kt`:
     * Change lines where `emit(Result.failure(e))` is called to `emit(Result.failure(mapToAppError(e)))`, ensuring callers receive typed `AppError` instances rather than leaking raw Ktor transport exceptions.
3. **Fix F-03 in `shared/src/commonTest/kotlin/org/moh/kash/project/domain/ConvertCurrencyUseCaseTest.kt`**:
   - Add unit tests verifying conversions with non-USD base rate maps:
     * EUR-anchored map (EUR=1.0, USD=1.087, ILS=3.967, JOD=0.7707) converting to USD, ILS, JOD.
     * ILS-anchored map (ILS=1.0, USD=0.274, EUR=0.252, JOD=0.1943) converting to USD, EUR, JOD.
     * JOD-anchored map (JOD=1.0, USD=1.4104, EUR=1.2976, ILS=5.1481) converting to USD, EUR, ILS.
4. **Verification**:
   - Run `.\gradlew.bat :shared:jvmTest` and verify that all unit and E2E tests pass (100% pass rate).


## 2026-11-01T20:31:53Z
**Context**: Milestone 2 Fix Implementation
**Content**: Checking in on your status implementing F-01 (ConvertCurrencyUseCase), F-02 (AppError & CurrencyRepositoryImpl), and F-03 (unit tests).
**Action**: Please report your current progress and ETA for handoff.md.

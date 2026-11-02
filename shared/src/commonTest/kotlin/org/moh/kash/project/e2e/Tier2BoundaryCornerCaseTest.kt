package org.moh.kash.project.e2e

import org.moh.kash.project.e2e.engine.KashE2ETestEngine
import org.moh.kash.project.e2e.model.E2ECurrency
import org.moh.kash.project.e2e.model.E2EExchangeAction
import org.moh.kash.project.e2e.model.E2ESeedRates
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tier 2: Boundary & Corner Cases Test Suite.
 *
 * Covers all boundary, stress, sanitization, and error tier conditions specified in:
 * - ORIGINAL_REQUEST.md (§Acceptance Criteria - Currency conversion calculations)
 * - PROJECT.md (§2 Feature Inventory #8 & #20)
 * - spec.md (§4.2 Boundary & Input Validation Matrix, §5 Offline Caching, §6.2 Error Tier Matrix)
 *
 * Feature categories tested (>= 5 test cases per feature):
 * 1. 0.00 Amount (5 tests)
 * 2. Negative Amount (5 tests)
 * 3. Empty Amount (5 tests)
 * 4. Decimal Precision Rounding (2 vs 3 decimals) (6 tests)
 * 5. Extreme Values (5 tests)
 * 6. 401/422/429 HTTP Status Mapping (5 tests)
 * 7. Offline Seed Rates Fallback (5 tests)
 */
class Tier2BoundaryCornerCaseTest {

    private lateinit var engine: KashE2ETestEngine

    @BeforeTest
    fun setUp() {
        engine = KashE2ETestEngine(
            initialBaseCurrency = "USD",
            initialAmount = "100.00",
            initialSeedRates = E2ESeedRates.BASE_USD
        )
    }

    // =========================================================================
    // FEATURE 1: 0.00 Amount Handling (5 test cases)
    // =========================================================================

    @Test
    fun testZeroAmountStringProducesZeroDotZeroZeroForStandardCurrencies() {
        engine.dispatch(E2EExchangeAction.ChangeAmount("0"))
        val eurCard = engine.state.rateCards.first { it.currency.code == "EUR" }
        assertEquals(0.0, eurCard.convertedAmount)
        assertEquals("0.00", eurCard.formattedAmount)

        val ilsCard = engine.state.rateCards.first { it.currency.code == "ILS" }
        assertEquals(0.0, ilsCard.convertedAmount)
        assertEquals("0.00", ilsCard.formattedAmount)
    }

    @Test
    fun testZeroAmountStringProducesZeroDotZeroZeroZeroForJod() {
        engine.dispatch(E2EExchangeAction.ChangeAmount("0"))
        val jodCard = engine.state.rateCards.first { it.currency.code == "JOD" }
        assertEquals(0.0, jodCard.convertedAmount)
        assertEquals("0.000", jodCard.formattedAmount, "JOD zero must format with 3 decimals: 0.000")
    }

    @Test
    fun testZeroDotZeroZeroStringPreservesZeroResults() {
        engine.dispatch(E2EExchangeAction.ChangeAmount("0.00"))
        assertEquals("0.00", engine.state.inputAmount)
        for (card in engine.state.rateCards) {
            assertEquals(0.0, card.convertedAmount)
            if (card.currency.code == "JOD") {
                assertEquals("0.000", card.formattedAmount)
            } else {
                assertEquals("0.00", card.formattedAmount)
            }
        }
    }

    @Test
    fun testZeroAmountDoesNotTriggerErrorBanner() {
        // Authoritative Spec: spec.md §4.2: Zero amount is a valid business state, no error banner
        engine.dispatch(E2EExchangeAction.ChangeAmount("0"))
        assertNull(engine.state.errorBanner, "Zero amount must not show error banner")
        assertNull(engine.state.fieldError, "Zero amount must not show field error")
    }

    @Test
    fun testZeroAmountCalculationAcrossAllBaseCurrencies() {
        for (code in E2ECurrency.TOP_CURRENCY_CODES) {
            engine.dispatch(E2EExchangeAction.SelectBaseCurrency(code))
            engine.dispatch(E2EExchangeAction.ChangeAmount("0"))
            for (card in engine.state.rateCards) {
                assertEquals(0.0, card.convertedAmount)
            }
        }
    }

    // =========================================================================
    // FEATURE 2: Negative Amount Handling & Sanitization (5 test cases)
    // =========================================================================

    @Test
    fun testNegativeAmountCoercedAndEmitsFieldError() {
        // Authoritative Spec: spec.md §4.2: Input "-50" triggers inline field error
        engine.dispatch(E2EExchangeAction.ChangeAmount("-50"))
        assertEquals("50", engine.state.inputAmount, "Minus sign stripped from sanitized numeric input")
        assertEquals("Amount must be positive", engine.state.fieldError)
    }

    @Test
    fun testNegativeSmallFractionalAmountRejected() {
        engine.dispatch(E2EExchangeAction.ChangeAmount("-0.01"))
        assertEquals("Amount must be positive", engine.state.fieldError)
    }

    @Test
    fun testMultipleNegativeSignsSanitized() {
        engine.dispatch(E2EExchangeAction.ChangeAmount("--150"))
        assertEquals("150", engine.state.inputAmount)
        assertEquals("Amount must be positive", engine.state.fieldError)
    }

    @Test
    fun testNegativeAmountDoesNotProduceNegativeTargetBalances() {
        engine.dispatch(E2EExchangeAction.ChangeAmount("-200"))
        for (card in engine.state.rateCards) {
            assertTrue(card.convertedAmount >= 0.0, "Target converted balance must never be negative")
        }
    }

    @Test
    fun testCorrectingNegativeAmountClearsFieldError() {
        engine.dispatch(E2EExchangeAction.ChangeAmount("-100"))
        assertNotNull(engine.state.fieldError)

        engine.dispatch(E2EExchangeAction.ChangeAmount("100"))
        assertNull(engine.state.fieldError, "Correcting to positive amount must clear field error immediately")
    }

    // =========================================================================
    // FEATURE 3: Empty Amount Handling (5 test cases)
    // =========================================================================

    @Test
    fun testEmptyStringAmountYieldsZeroDisplay() {
        engine.dispatch(E2EExchangeAction.ChangeAmount(""))
        assertEquals("", engine.state.inputAmount)
        for (card in engine.state.rateCards) {
            assertEquals(0.0, card.convertedAmount)
            if (card.currency.code == "JOD") {
                assertEquals("0.000", card.formattedAmount)
            } else {
                assertEquals("0.00", card.formattedAmount)
            }
        }
    }

    @Test
    fun testClearingAmountFieldDoesNotCrash() {
        engine.dispatch(E2EExchangeAction.ClearAmount)
        assertEquals("", engine.state.inputAmount)
        assertNull(engine.state.fieldError)
        assertNull(engine.state.errorBanner)
    }

    @Test
    fun testWhitespaceOnlyAmountHandledAsEmpty() {
        engine.dispatch(E2EExchangeAction.ChangeAmount("   "))
        assertEquals("", engine.state.inputAmount)
        for (card in engine.state.rateCards) {
            assertEquals(0.0, card.convertedAmount)
        }
    }

    @Test
    fun testTypingAfterEmptyInputRecalculatesImmediately() {
        engine.dispatch(E2EExchangeAction.ClearAmount)
        engine.dispatch(E2EExchangeAction.ChangeAmount("75"))
        assertEquals("75", engine.state.inputAmount)
        val eurCard = engine.state.rateCards.first { it.currency.code == "EUR" }
        // 75 * 0.92 = 69.00
        assertEquals("69.00", eurCard.formattedAmount)
    }

    @Test
    fun testRapidClearAndRetypeSequence() {
        engine.dispatch(E2EExchangeAction.ChangeAmount("123"))
        engine.dispatch(E2EExchangeAction.ClearAmount)
        engine.dispatch(E2EExchangeAction.ChangeAmount("456"))
        assertEquals("456", engine.state.inputAmount)
        val ilsCard = engine.state.rateCards.first { it.currency.code == "ILS" }
        // 456 * 3.65 = 1664.40
        assertEquals("1,664.40", ilsCard.formattedAmount)
    }

    // =========================================================================
    // FEATURE 4: Decimal Precision Rounding (2 vs 3 decimals) (6 test cases)
    // =========================================================================

    @Test
    fun testHalfUpRoundingTwoDecimalsRoundUp() {
        // 100.555 -> 100.56
        val rounded = KashE2ETestEngine.roundHalfUp(100.555, 2)
        assertEquals(100.56, rounded)
        assertEquals("100.56", KashE2ETestEngine.formatNumber(100.555, 2))
    }

    @Test
    fun testHalfUpRoundingTwoDecimalsRoundDown() {
        // 100.554 -> 100.55
        val rounded = KashE2ETestEngine.roundHalfUp(100.554, 2)
        assertEquals(100.55, rounded)
        assertEquals("100.55", KashE2ETestEngine.formatNumber(100.554, 2))
    }

    @Test
    fun testHalfUpRoundingThreeDecimalsJodRoundUp() {
        // 70.9005 -> 70.901
        val rounded = KashE2ETestEngine.roundHalfUp(70.9005, 3)
        assertEquals(70.901, rounded)
        assertEquals("70.901", KashE2ETestEngine.formatNumber(70.9005, 3))
    }

    @Test
    fun testHalfUpRoundingThreeDecimalsJodRoundDown() {
        // 70.9004 -> 70.900
        val rounded = KashE2ETestEngine.roundHalfUp(70.9004, 3)
        assertEquals(70.900, rounded)
        assertEquals("70.900", KashE2ETestEngine.formatNumber(70.9004, 3))
    }

    @Test
    fun testThousandCommaSeparatorsStandardFormat() {
        assertEquals("1,234,567.89", KashE2ETestEngine.formatNumber(1234567.89, 2))
        assertEquals("12,345,678.90", KashE2ETestEngine.formatNumber(12345678.9, 2))
    }

    @Test
    fun testThousandCommaSeparatorsJodThreeDecimalFormat() {
        assertEquals("1,234,567.891", KashE2ETestEngine.formatNumber(1234567.8912, 3))
        assertEquals("500,000.000", KashE2ETestEngine.formatNumber(500000.0, 3))
    }

    // =========================================================================
    // FEATURE 5: Extreme Values & Edge Amounts (5 test cases)
    // =========================================================================

    @Test
    fun testOneBillionAmountCalculatesWithoutFloatingOverflow() {
        engine.dispatch(E2EExchangeAction.ChangeAmount("1000000000"))
        val eurCard = engine.state.rateCards.first { it.currency.code == "EUR" }
        // 1,000,000,000 * 0.92 = 920,000,000.00
        assertEquals("920,000,000.00", eurCard.formattedAmount)
    }

    @Test
    fun testOneTrillionAmountFormatsWithCommas() {
        val result = engine.convert(1000000000000.0, "USD", "ILS")
        // 10^12 * 3.65 = 3,650,000,000,000.00
        assertEquals("3,650,000,000,000.00", result.formattedTargetAmount)
    }

    @Test
    fun testInputExceedingTwelveDigitsIsSafelyCapped() {
        // Spec §4.2: Maximum 12 digits
        engine.dispatch(E2EExchangeAction.ChangeAmount("123456789012345"))
        assertEquals(12, engine.state.inputAmount.length, "Input must cap at 12 digits")
    }

    @Test
    fun testMicroFractionalAmountCalculatesWithoutUnderflow() {
        val result = engine.convert(0.0001, "USD", "EUR")
        // 0.0001 * 0.92 = 0.000092 -> rounds to 0.00
        assertEquals("0.00", result.formattedTargetAmount)
        assertEquals(0.000092, result.rawTargetAmount, 0.000001)
    }

    @Test
    fun testExtremeRateCalculationPrecision() {
        val rate = engine.calculateRate("ILS", "EUR")
        assertEquals("0.2521", KashE2ETestEngine.formatRate(rate))
    }

    // =========================================================================
    // FEATURE 6: HTTP 401/422/429 Status Mapping (5 test cases)
    // =========================================================================

    @Test
    fun testHttp401UnauthorizedMapsToConfigurationPrompt() {
        // Spec §2.2: 401 Unauthorized -> Invalid API Key
        engine.dispatch(E2EExchangeAction.SimulateNetworkError(401, "Invalid API key"))
        engine.dispatch(E2EExchangeAction.RefreshRates)

        assertNotNull(engine.state.errorBanner)
        assertTrue(engine.state.errorBanner!!.contains("Invalid API Key"))
    }

    @Test
    fun testHttp422UnprocessableEntityPreventedByQuerySanitization() {
        // Spec §3.2 & survey finding: JOD must NEVER be requested in currencies parameter
        engine.dispatch(E2EExchangeAction.RefreshRates)
        val lastQuery = engine.interceptedRemoteQueries.lastOrNull()
        assertNotNull(lastQuery)
        assertFalse(lastQuery.contains("currencies=USD,EUR,ILS,JOD"), "Query must never request JOD directly")
        assertTrue(lastQuery.contains("currencies=USD,EUR,ILS"), "Query must only request native 33 currencies")
    }

    @Test
    fun testHttp422ResponseMapsToValidationErrorIfReceived() {
        engine.dispatch(E2EExchangeAction.SimulateNetworkError(422, "Unsupported currency"))
        engine.dispatch(E2EExchangeAction.RefreshRates)

        assertNotNull(engine.state.errorBanner)
        assertTrue(engine.state.errorBanner!!.contains("Unprocessable Entity"))
    }

    @Test
    fun testHttp429QuotaExceededEmitsPopupSnackbarAndServesCachedRates() {
        // Spec §2.2 & §6.2: 429 Too Many Requests -> Popup Tier: Snack bar, visible rates preserved
        val initialCards = engine.state.rateCards
        engine.dispatch(E2EExchangeAction.SimulateNetworkError(429, "Rate limit reached"))
        engine.dispatch(E2EExchangeAction.RefreshRates)

        assertEquals(initialCards, engine.state.rateCards, "Visible rates must remain visible on screen")
        assertNotNull(engine.state.snackbarEffect)
        assertTrue(engine.state.snackbarEffect!!.contains("Rate limit reached"))
        assertTrue(engine.state.isOffline)
    }

    @Test
    fun testHttp500ServerOutageFallsBackToCachedRates() {
        val initialCards = engine.state.rateCards
        engine.dispatch(E2EExchangeAction.SimulateNetworkError(500, "Server Outage"))
        engine.dispatch(E2EExchangeAction.RefreshRates)

        assertEquals(initialCards, engine.state.rateCards)
        assertNotNull(engine.state.snackbarEffect)
        assertTrue(engine.state.snackbarEffect!!.contains("server unavailable"))
    }

    // =========================================================================
    // FEATURE 7: Offline Seed Rates Fallback (5 test cases)
    // =========================================================================

    @Test
    fun testColdStartWithoutNetworkLoadsBundledSeedRates() {
        // Spec §5.2: Bundled seed rates on fresh cold launch offline
        val offlineEngine = KashE2ETestEngine(initialBaseCurrency = "USD")
        offlineEngine.dispatch(E2EExchangeAction.DisconnectNetwork)
        offlineEngine.dispatch(E2EExchangeAction.RetryColdLoad)

        assertTrue(offlineEngine.state.isOffline)
        val eurCard = offlineEngine.state.rateCards.first { it.currency.code == "EUR" }
        assertEquals(0.9200, eurCard.unitRate)
        val ilsCard = offlineEngine.state.rateCards.first { it.currency.code == "ILS" }
        assertEquals(3.6500, ilsCard.unitRate)
        val jodCard = offlineEngine.state.rateCards.first { it.currency.code == "JOD" }
        assertEquals(0.7090, jodCard.unitRate)
    }

    @Test
    fun testColdStartOfflineMarksIsOfflineTrueWithBanner() {
        val offlineEngine = KashE2ETestEngine(initialBaseCurrency = "USD")
        offlineEngine.dispatch(E2EExchangeAction.DisconnectNetwork)
        offlineEngine.dispatch(E2EExchangeAction.RetryColdLoad)

        assertTrue(offlineEngine.state.isOffline)
        assertNotNull(offlineEngine.state.errorBanner)
        assertTrue(offlineEngine.state.errorBanner!!.contains("seed rates"))
    }

    @Test
    fun testSeedRatesPerformExactJodPegConversion() {
        val offlineEngine = KashE2ETestEngine(initialBaseCurrency = "USD", initialAmount = "100.00")
        offlineEngine.dispatch(E2EExchangeAction.DisconnectNetwork)
        offlineEngine.dispatch(E2EExchangeAction.RetryColdLoad)

        val jodCard = offlineEngine.state.rateCards.first { it.currency.code == "JOD" }
        assertEquals("70.900", jodCard.formattedAmount)
    }

    @Test
    fun testSeedRatesSupportSwitchingToEurBase() {
        val offlineEngine = KashE2ETestEngine(initialBaseCurrency = "USD", initialAmount = "100.00")
        offlineEngine.dispatch(E2EExchangeAction.DisconnectNetwork)
        offlineEngine.dispatch(E2EExchangeAction.RetryColdLoad)

        offlineEngine.dispatch(E2EExchangeAction.SelectBaseCurrency("EUR"))
        assertEquals("EUR", offlineEngine.state.baseCurrency)
        val usdCard = offlineEngine.state.rateCards.first { it.currency.code == "USD" }
        assertEquals("1.0870", usdCard.formattedRate)
    }

    @Test
    fun testOnlineRestorationReplacesSeedRatesWithFreshRates() {
        val offlineEngine = KashE2ETestEngine(initialBaseCurrency = "USD", initialAmount = "100.00")
        offlineEngine.dispatch(E2EExchangeAction.DisconnectNetwork)
        offlineEngine.dispatch(E2EExchangeAction.RetryColdLoad)
        assertTrue(offlineEngine.state.isOffline)

        // Restore network and update with fresh live rates
        offlineEngine.dispatch(E2EExchangeAction.RestoreNetwork)
        offlineEngine.updateLiveRates(mapOf(
            "USD" to 1.0,
            "EUR" to 0.9500,
            "ILS" to 3.8000
        ))
        offlineEngine.dispatch(E2EExchangeAction.RefreshRates)

        assertFalse(offlineEngine.state.isOffline)
        val eurCard = offlineEngine.state.rateCards.first { it.currency.code == "EUR" }
        assertEquals(0.9500, eurCard.unitRate)
        assertEquals("95.00", eurCard.formattedAmount)
    }
}

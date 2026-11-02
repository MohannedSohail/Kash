package org.moh.kash.project.e2e

import org.moh.kash.project.e2e.engine.KashE2ETestEngine
import org.moh.kash.project.e2e.model.E2ECurrency
import org.moh.kash.project.e2e.model.E2EExchangeAction
import org.moh.kash.project.e2e.model.E2EJodPeg
import org.moh.kash.project.e2e.model.E2ESeedRates
import kotlin.math.abs
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tier 3: Cross-Feature Combinations Test Suite.
 *
 * Verifies complex multi-feature interactions, race conditions, and state transitions:
 * 1. Base currency switching while typing (input preserved, target cards updated).
 * 2. Rapid amount modification during network refresh (prevents stale data overwrite).
 * 3. Network drop recovery (offline cache retention, seamless sync on reconnection).
 */
class Tier3CrossFeatureCombinationTest {

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
    // COMBINATION 1: Base Currency Switching While Typing
    // =========================================================================

    @Test
    fun testBaseCurrencySwitchingWhileTypingPreservesAmountAndRecalculatesTargetRates() {
        // User starts typing an amount "250"
        engine.dispatch(E2EExchangeAction.ChangeAmount("250"))
        assertEquals("250", engine.state.inputAmount)

        // User switches base currency to EUR while the amount is entered
        engine.dispatch(E2EExchangeAction.SelectBaseCurrency("EUR"))

        assertEquals("EUR", engine.state.baseCurrency)
        assertEquals("250", engine.state.inputAmount, "Amount 250 must be preserved after switching base")

        val usdCard = engine.state.rateCards.first { it.currency.code == "USD" }
        // 250 EUR * (1 / 0.92) ≈ 271.74 USD
        assertEquals("271.74", usdCard.formattedAmount)

        val jodCard = engine.state.rateCards.first { it.currency.code == "JOD" }
        // 250 EUR * ((1 / 0.92) * 0.7090) ≈ 192.663 JOD
        assertEquals("192.663", jodCard.formattedAmount)
    }

    @Test
    fun testDecimalAmountPreservedAcrossBaseCurrencyToggles() {
        engine.dispatch(E2EExchangeAction.ChangeAmount("149.99"))
        engine.dispatch(E2EExchangeAction.SelectBaseCurrency("ILS"))

        assertEquals("ILS", engine.state.baseCurrency)
        assertEquals("149.99", engine.state.inputAmount)

        // Switch to JOD base
        engine.dispatch(E2EExchangeAction.SelectBaseCurrency("JOD"))
        assertEquals("JOD", engine.state.baseCurrency)
        assertEquals("149.99", engine.state.inputAmount)

        val usdCard = engine.state.rateCards.first { it.currency.code == "USD" }
        // 149.99 JOD * (1 / 0.7090) ≈ 211.55 USD
        assertEquals("211.55", usdCard.formattedAmount)
    }

    @Test
    fun testFullCycleBaseSwitchEquilibriumIdentity() {
        // Converting A through USD -> EUR -> ILS -> JOD -> USD should preserve economic value
        val initialAmount = 1000.0
        val toEur = engine.calculateRate("USD", "EUR")
        val toIls = engine.calculateRate("EUR", "ILS")
        val toJod = engine.calculateRate("ILS", "JOD")
        val toUsd = engine.calculateRate("JOD", "USD")

        val finalAmount = initialAmount * toEur * toIls * toJod * toUsd
        // Tolerance for floating point calculation
        assertTrue(abs(finalAmount - initialAmount) < 1e-4, "Full currency cycle must preserve value within epsilon")
    }

    // =========================================================================
    // COMBINATION 2: Rapid Amount Modification During Refresh
    // =========================================================================

    @Test
    fun testRapidAmountModificationDuringBackgroundRefreshPreservesLatestUserAmount() {
        // User enters 100
        engine.dispatch(E2EExchangeAction.ChangeAmount("100"))

        // Fresh rates arrive from upstream in background
        engine.updateLiveRates(mapOf(
            "USD" to 1.0,
            "EUR" to 0.9400,
            "ILS" to 3.7000
        ))

        // Simultaneously, user rapidly changes amount to 500
        engine.dispatch(E2EExchangeAction.ChangeAmount("500"))

        // Background refresh completes
        engine.dispatch(E2EExchangeAction.RefreshRates)

        assertEquals("500", engine.state.inputAmount, "User amount 500 must not be overwritten by background refresh")
        val eurCard = engine.state.rateCards.first { it.currency.code == "EUR" }
        // 500 * 0.94 = 470.00
        assertEquals("470.00", eurCard.formattedAmount)
    }

    @Test
    fun testClearingAmountDuringRefreshYieldsZeroDisplayWithFreshRates() {
        engine.dispatch(E2EExchangeAction.ChangeAmount("300"))
        engine.updateLiveRates(mapOf("USD" to 1.0, "EUR" to 0.9500))
        engine.dispatch(E2EExchangeAction.ClearAmount)
        engine.dispatch(E2EExchangeAction.RefreshRates)

        assertEquals("", engine.state.inputAmount)
        val eurCard = engine.state.rateCards.first { it.currency.code == "EUR" }
        assertEquals(0.0, eurCard.convertedAmount)
        assertEquals("0.00", eurCard.formattedAmount)
        assertEquals(0.9500, eurCard.unitRate)
    }

    @Test
    fun testRapidKeystrokeInputSequenceDebouncingAndStateConsistency() {
        val keystrokes = listOf("1", "12", "125", "1250", "12500")
        for (step in keystrokes) {
            engine.dispatch(E2EExchangeAction.ChangeAmount(step))
            assertEquals(step, engine.state.inputAmount)
        }

        val jodCard = engine.state.rateCards.first { it.currency.code == "JOD" }
        // 12500 USD * 0.7090 = 8862.500 JOD
        assertEquals("8,862.500", jodCard.formattedAmount)
    }

    // =========================================================================
    // COMBINATION 3: Network Drop Recovery
    // =========================================================================

    @Test
    fun testNetworkDropDuringPullToRefreshPreservesVisibleRatesAndEmitsSnackbar() {
        // Setup initial visible rates
        val initialEurRate = engine.state.rateCards.first { it.currency.code == "EUR" }.unitRate
        val initialAmount = engine.state.rateCards.first { it.currency.code == "EUR" }.formattedAmount

        // Network drops mid-operation
        engine.dispatch(E2EExchangeAction.DisconnectNetwork)
        engine.dispatch(E2EExchangeAction.RefreshRates)

        // Tier rule 8 (Popup Tier): Keep visible rates intact, show snackbar
        val curEurCard = engine.state.rateCards.first { it.currency.code == "EUR" }
        assertEquals(initialEurRate, curEurCard.unitRate, "Visible unit rate must NOT be cleared")
        assertEquals(initialAmount, curEurCard.formattedAmount, "Visible converted amount must NOT be cleared")

        assertTrue(engine.state.isOffline)
        assertNotNull(engine.state.snackbarEffect)
        assertTrue(engine.state.snackbarEffect!!.contains("Network unavailable"))
    }

    @Test
    fun testOfflineCacheUsageFollowedByOnlineReconnectionAndSeamlessSync() {
        // 1. Operate offline with cached rates
        engine.dispatch(E2EExchangeAction.DisconnectNetwork)
        engine.dispatch(E2EExchangeAction.ChangeAmount("400"))
        val offlineJod = engine.state.rateCards.first { it.currency.code == "JOD" }.formattedAmount
        // 400 * 0.7090 = 283.600
        assertEquals("283.600", offlineJod)

        // 2. Reconnect network with new exchange rates
        engine.dispatch(E2EExchangeAction.RestoreNetwork)
        engine.updateLiveRates(mapOf(
            "USD" to 1.0,
            "EUR" to 0.9000,
            "ILS" to 3.5000
        ))
        engine.dispatch(E2EExchangeAction.RefreshRates)

        assertFalse(engine.state.isOffline)
        assertNull(engine.state.snackbarEffect)

        val eurCard = engine.state.rateCards.first { it.currency.code == "EUR" }
        // 400 * 0.90 = 360.00
        assertEquals("360.00", eurCard.formattedAmount)
    }

    @Test
    fun testNetworkFlappingStateTransitionsWithoutCrashingOrStateLoss() {
        // Simulate rapid online/offline flapping (e.g. subway / elevator)
        for (i in 1..5) {
            engine.dispatch(E2EExchangeAction.DisconnectNetwork)
            engine.dispatch(E2EExchangeAction.RefreshRates)
            assertTrue(engine.state.isOffline)

            engine.dispatch(E2EExchangeAction.RestoreNetwork)
            engine.dispatch(E2EExchangeAction.RefreshRates)
            assertFalse(engine.state.isOffline)
        }

        // Verify state is clean and operational after flapping
        assertEquals("100.00", engine.state.inputAmount)
        assertEquals(3, engine.state.rateCards.size)
    }
}

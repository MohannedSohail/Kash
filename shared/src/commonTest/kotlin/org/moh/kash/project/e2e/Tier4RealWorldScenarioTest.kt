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
import kotlin.test.assertTrue

/**
 * Tier 4: Real-World Application Scenarios Test Suite.
 *
 * Implements end-to-end, multi-step real-world personas and use cases:
 * 1. Traveler Journey: Multi-country itinerary exchanging between USD, JOD, NIS (ILS), and EUR.
 * 2. Merchant Daily Settlements: Multi-currency retail cash register daily settlement.
 * 3. Off-Grid Flight Mode: Cold start and in-flight operations with zero network connectivity.
 */
class Tier4RealWorldScenarioTest {

    private lateinit var engine: KashE2ETestEngine

    // Benchmark realistic market rates:
    // USD: 1.0, EUR: 0.9200, ILS: 3.6500, JOD: 0.7090 (CBJ Peg)
    private val marketRates = mapOf(
        "USD" to 1.0000,
        "EUR" to 0.9200,
        "ILS" to 3.6500,
        "JOD" to 0.7090
    )

    @BeforeTest
    fun setUp() {
        engine = KashE2ETestEngine(
            initialBaseCurrency = "USD",
            initialAmount = "100.00",
            initialSeedRates = marketRates
        )
    }

    // =========================================================================
    // SCENARIO 1: Traveler Journey Across Middle East & Europe
    // =========================================================================

    @Test
    fun testMultiLegMiddleEastAndEuropeanTravelerBudget() {
        // Leg 1: Traveler lands in Queen Alia Airport (Amman, Jordan) with $1,000 USD
        val leg1Result = engine.convert(1000.0, "USD", "JOD")
        assertEquals(709.000, leg1Result.rawTargetAmount)
        assertEquals("709.000", leg1Result.formattedTargetAmount)

        // Traveler spends 250 JOD for lodging and dining in Amman
        val remainingJod = 709.000 - 250.000 // = 459.000 JOD
        assertEquals(459.000, remainingJod)

        // Leg 2: Traveler crosses the Allenby/King Hussein Bridge to Jerusalem (ILS)
        // Switches base currency to JOD and enters 459.000 JOD
        engine.dispatch(E2EExchangeAction.SelectBaseCurrency("JOD"))
        engine.dispatch(E2EExchangeAction.ChangeAmount("459.000"))

        val ilsCard = engine.state.rateCards.first { it.currency.code == "ILS" }
        // R(JOD -> ILS) = (1 / 0.7090) * 3.65 ≈ 5.1480959
        // 459.000 * 5.1480959 ≈ 2,362.98 ILS
        val expectedIls = 459.000 * E2EJodPeg.JOD_TO_USD_RATE * 3.6500
        assertTrue(abs(ilsCard.convertedAmount - expectedIls) < 1e-3)
        assertEquals("2,362.98", ilsCard.formattedAmount)

        // Traveler spends 1,200.00 ILS in Jerusalem and Bethlehem
        val remainingIls = expectedIls - 1200.00 // ≈ 1,162.976 ILS

        // Leg 3: Traveler flies from Ben Gurion Airport to Paris (Charles de Gaulle)
        // Switches base currency to ILS and enters remaining amount
        val formattedRemainingIls = KashE2ETestEngine.formatNumber(remainingIls, 2)
        engine.dispatch(E2EExchangeAction.SelectBaseCurrency("ILS"))
        engine.dispatch(E2EExchangeAction.ChangeAmount(formattedRemainingIls))

        val eurCard = engine.state.rateCards.first { it.currency.code == "EUR" }
        // R(ILS -> EUR) = 0.9200 / 3.6500 ≈ 0.25205479
        // Converted EUR ≈ 1,162.98 * 0.25205479 ≈ 293.13 EUR
        assertEquals("293.13", eurCard.formattedAmount)
    }

    @Test
    fun testDutyFreePriceComparisonAcrossAllFourCurrencies() {
        // Traveler in airport duty-free evaluates a $299.99 USD smartwatch
        engine.dispatch(E2EExchangeAction.SelectBaseCurrency("USD"))
        engine.dispatch(E2EExchangeAction.ChangeAmount("299.99"))

        val eurCard = engine.state.rateCards.first { it.currency.code == "EUR" }
        val ilsCard = engine.state.rateCards.first { it.currency.code == "ILS" }
        val jodCard = engine.state.rateCards.first { it.currency.code == "JOD" }

        // 299.99 * 0.9200 = 275.9908 -> rounds to 275.99 EUR
        assertEquals("275.99", eurCard.formattedAmount)
        // 299.99 * 3.6500 = 1094.9635 -> rounds to 1,094.96 ILS
        assertEquals("1,094.96", ilsCard.formattedAmount)
        // 299.99 * 0.7090 = 212.69291 -> rounds to 212.693 JOD
        assertEquals("212.693", jodCard.formattedAmount)
    }

    // =========================================================================
    // SCENARIO 2: Merchant Daily Settlements
    // =========================================================================

    @Test
    fun testCrossBorderMerchantMultiCurrencySalesSettlementToIls() {
        // A merchant in Jerusalem accepts payments across all 4 currencies:
        // Cash Register End-of-Day totals:
        val usdCash = 1500.00
        val eurCash = 850.00
        val jodCash = 420.000
        val ilsCash = 3800.00

        // Merchant settles everything into local currency (ILS):
        val usdToIls = engine.convert(usdCash, "USD", "ILS")
        val eurToIls = engine.convert(eurCash, "EUR", "ILS")
        val jodToIls = engine.convert(jodCash, "JOD", "ILS")

        // 1,500 USD * 3.65 = 5,475.00 ILS
        assertEquals(5475.00, usdToIls.rawTargetAmount)
        assertEquals("5,475.00", usdToIls.formattedTargetAmount)

        // 850 EUR * (3.65 / 0.92) ≈ 3,372.2826 -> 3,372.28 ILS
        assertEquals("3,372.28", eurToIls.formattedTargetAmount)

        // 420 JOD * (1 / 0.7090) * 3.65 ≈ 2,162.2002 -> 2,162.20 ILS
        assertEquals("2,162.20", jodToIls.formattedTargetAmount)

        val totalIlsSettlement = usdToIls.rawTargetAmount +
                eurToIls.rawTargetAmount +
                jodToIls.rawTargetAmount +
                ilsCash

        val formattedTotal = KashE2ETestEngine.formatNumber(totalIlsSettlement, 2)
        // Total = 5475.00 + 3372.28 + 2162.20 + 3800.00 = 14,809.48 ILS
        assertEquals("14,809.48", formattedTotal)
    }

    @Test
    fun testMerchantInvoicingPrecisionWithoutCentDrift() {
        // Invoice with 3 items in USD converted to JOD:
        // Item 1: 45.50 USD
        // Item 2: 78.25 USD
        // Item 3: 125.75 USD
        // Total: 249.50 USD
        val item1 = engine.convert(45.50, "USD", "JOD")
        val item2 = engine.convert(78.25, "USD", "JOD")
        val item3 = engine.convert(125.75, "USD", "JOD")
        val bulkTotal = engine.convert(249.50, "USD", "JOD")

        // Sum of raw amounts
        val sumRaw = item1.rawTargetAmount + item2.rawTargetAmount + item3.rawTargetAmount
        assertTrue(abs(sumRaw - bulkTotal.rawTargetAmount) < 1e-6)
        assertEquals("176.896", bulkTotal.formattedTargetAmount)
    }

    // =========================================================================
    // SCENARIO 3: Off-Grid Flight Mode Conversions
    // =========================================================================

    @Test
    fun testTransatlanticFlightColdStartSeedRatesMenuConversion() {
        // Passenger opens Kash during transatlantic flight in Airplane Mode (Cold Launch)
        val flightEngine = KashE2ETestEngine(initialBaseCurrency = "USD", initialAmount = "25.00")
        flightEngine.dispatch(E2EExchangeAction.DisconnectNetwork)
        flightEngine.dispatch(E2EExchangeAction.RetryColdLoad)

        // Must display bundled seed rates with offline banner
        assertTrue(flightEngine.state.isOffline)
        assertNotNull(flightEngine.state.errorBanner)
        assertTrue(flightEngine.state.errorBanner!!.contains("seed rates"))

        // Passenger converts inflight meal price: $25.00 USD
        val eurCard = flightEngine.state.rateCards.first { it.currency.code == "EUR" }
        // 25 * 0.9200 = 23.00 EUR
        assertEquals("23.00", eurCard.formattedAmount)

        val jodCard = flightEngine.state.rateCards.first { it.currency.code == "JOD" }
        // 25 * 0.7090 = 17.725 JOD
        assertEquals("17.725", jodCard.formattedAmount)
    }

    @Test
    fun testLongHaulFlightWithTwelveHourOldCachedRates() {
        // Pre-cached rates before boarding
        val inFlightEngine = KashE2ETestEngine(initialBaseCurrency = "USD", initialAmount = "150.00")
        inFlightEngine.updateLiveRates(mapOf(
            "USD" to 1.0,
            "EUR" to 0.9150,
            "ILS" to 3.6200
        ))
        inFlightEngine.dispatch(E2EExchangeAction.RefreshRates)

        // Flight takes off, network disconnected for 12 hours
        inFlightEngine.dispatch(E2EExchangeAction.DisconnectNetwork)

        // User pulls to refresh during flight
        inFlightEngine.dispatch(E2EExchangeAction.RefreshRates)

        // Cached rates must be retained without clearing
        assertTrue(inFlightEngine.state.isOffline)
        val eurCard = inFlightEngine.state.rateCards.first { it.currency.code == "EUR" }
        assertEquals(0.9150, eurCard.unitRate)
        // 150 * 0.915 = 137.25 EUR
        assertEquals("137.25", eurCard.formattedAmount)
        assertNotNull(inFlightEngine.state.snackbarEffect)
    }
}

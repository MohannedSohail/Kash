package org.moh.kash.project.e2e

import org.moh.kash.project.e2e.engine.KashE2ETestEngine
import org.moh.kash.project.e2e.model.E2ECurrency
import org.moh.kash.project.e2e.model.E2EExchangeAction
import org.moh.kash.project.e2e.model.E2EJodPeg
import kotlin.math.abs
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tier 1: Core Feature Coverage Test Suite.
 *
 * Covers all primary happy-path requirements from ORIGINAL_REQUEST.md (§R2) and spec.md:
 * - Feature 1: Base Currency Selection (>= 5 test cases)
 * - Feature 2: USD Rates Calculation & Display (>= 5 test cases)
 * - Feature 3: EUR Rates Calculation & Display (>= 5 test cases)
 * - Feature 4: NIS (ILS) Rates Calculation & Display (>= 5 test cases)
 * - Feature 5: JOD Rates Calculation & Display (>= 5 test cases)
 * - Feature 6: Currency Conversion Math (>= 5 test cases)
 */
class Tier1FeatureCoverageTest {

    private lateinit var engine: KashE2ETestEngine

    // Authoritative reference rates relative to USD from FreeCurrencyAPI sample fixture:
    // USD: 1.0, EUR: 0.9200, ILS: 3.6500, JOD: 0.7090 (Pegged)
    private val standardSampleRates = mapOf(
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
            initialSeedRates = standardSampleRates
        )
    }

    // =========================================================================
    // FEATURE 1: Base Currency Selection (6 test cases)
    // =========================================================================

    @Test
    fun testDefaultBaseCurrencyIsUsdOnInitialLaunch() {
        // Authoritative Spec: spec.md §6.1 default base currency is "USD"
        assertEquals("USD", engine.state.baseCurrency)
        assertEquals("100.00", engine.state.inputAmount)
        assertEquals(3, engine.state.rateCards.size, "Must show cards for the other 3 currencies (EUR, ILS, JOD)")

        val displayedCodes = engine.state.rateCards.map { it.currency.code }.toSet()
        assertEquals(setOf("EUR", "ILS", "JOD"), displayedCodes)
    }

    @Test
    fun testSwitchBaseCurrencyToEurUpdatesBaseAndRecalculatesTargetRates() {
        engine.dispatch(E2EExchangeAction.SelectBaseCurrency("EUR"))

        assertEquals("EUR", engine.state.baseCurrency)
        val displayedCodes = engine.state.rateCards.map { it.currency.code }.toSet()
        assertEquals(setOf("USD", "ILS", "JOD"), displayedCodes, "EUR must not be in target card list")

        // R(EUR -> USD) = 1.0 / 0.9200 ≈ 1.0869565
        val usdCard = engine.state.rateCards.first { it.currency.code == "USD" }
        val expectedUsdRate = 1.0 / 0.9200
        assertTrue(abs(usdCard.unitRate - expectedUsdRate) < 1e-4)
        assertEquals("1.0870", usdCard.formattedRate)
        assertEquals("108.70", usdCard.formattedAmount)
    }

    @Test
    fun testSwitchBaseCurrencyToIlsUpdatesBaseAndRecalculatesTargetRates() {
        engine.dispatch(E2EExchangeAction.SelectBaseCurrency("ILS"))

        assertEquals("ILS", engine.state.baseCurrency)
        val displayedCodes = engine.state.rateCards.map { it.currency.code }.toSet()
        assertEquals(setOf("USD", "EUR", "JOD"), displayedCodes)

        // R(ILS -> USD) = 1.0 / 3.6500 ≈ 0.27397
        val usdCard = engine.state.rateCards.first { it.currency.code == "USD" }
        val expectedRate = 1.0 / 3.6500
        assertTrue(abs(usdCard.unitRate - expectedRate) < 1e-4)
        assertEquals("0.2740", usdCard.formattedRate)
    }

    @Test
    fun testSwitchBaseCurrencyToJodTriggersPegCrossRates() {
        engine.dispatch(E2EExchangeAction.SelectBaseCurrency("JOD"))

        assertEquals("JOD", engine.state.baseCurrency)
        val displayedCodes = engine.state.rateCards.map { it.currency.code }.toSet()
        assertEquals(setOf("USD", "EUR", "ILS"), displayedCodes)

        // R(JOD -> USD) = 1.0 / 0.7090 ≈ 1.4104372
        val usdCard = engine.state.rateCards.first { it.currency.code == "USD" }
        assertTrue(abs(usdCard.unitRate - E2EJodPeg.JOD_TO_USD_RATE) < 1e-4)
        assertEquals("1.4104", usdCard.formattedRate)
        assertEquals("141.04", usdCard.formattedAmount)
    }

    @Test
    fun testSelectingCurrentlyActiveBaseCurrencyIsIdempotent() {
        val initialCards = engine.state.rateCards
        engine.dispatch(E2EExchangeAction.SelectBaseCurrency("USD"))

        assertEquals("USD", engine.state.baseCurrency)
        assertEquals(initialCards, engine.state.rateCards, "Selecting same base should not alter state")
    }

    @Test
    fun testSwitchingAcrossAllCurrenciesMaintainsConsistency() {
        val currencySequence = listOf("EUR", "ILS", "JOD", "USD")
        for (code in currencySequence) {
            engine.dispatch(E2EExchangeAction.SelectBaseCurrency(code))
            assertEquals(code, engine.state.baseCurrency)
            assertEquals(3, engine.state.rateCards.size)
            assertTrue(engine.state.rateCards.none { it.currency.code == code })
        }
    }

    // =========================================================================
    // FEATURE 2: USD Rates Calculation & Display (5 test cases)
    // =========================================================================

    @Test
    fun testUsdToUsdSelfRateIsExactlyOne() {
        val rate = engine.calculateRate("USD", "USD")
        assertEquals(1.0000, rate)
        val result = engine.convert(100.0, "USD", "USD")
        assertEquals(100.0, result.rawTargetAmount)
        assertEquals("100.00", result.formattedTargetAmount)
    }

    @Test
    fun testUsdToEurRateMatchesApiFeed() {
        val rate = engine.calculateRate("USD", "EUR")
        assertEquals(0.9200, rate)
        val result = engine.convert(100.0, "USD", "EUR")
        assertEquals(92.00, result.rawTargetAmount)
        assertEquals("92.00", result.formattedTargetAmount)
        assertEquals("0.9200", result.formattedRate)
    }

    @Test
    fun testUsdToIlsRateMatchesApiFeed() {
        val rate = engine.calculateRate("USD", "ILS")
        assertEquals(3.6500, rate)
        val result = engine.convert(100.0, "USD", "ILS")
        assertEquals(365.00, result.rawTargetAmount)
        assertEquals("365.00", result.formattedTargetAmount)
        assertEquals("3.6500", result.formattedRate)
    }

    @Test
    fun testUsdToJodRateMatchesOfficialPegStrictly() {
        // Authoritative Spec: Central Bank of Jordan Peg: 1 USD = 0.7090 JOD
        val rate = engine.calculateRate("USD", "JOD")
        assertEquals(0.7090, rate)
        val result = engine.convert(100.0, "USD", "JOD")
        assertEquals(70.900, result.rawTargetAmount, 0.001)
        assertEquals("70.900", result.formattedTargetAmount, "JOD must format with 3 decimal places")
        assertEquals("0.7090", result.formattedRate)
    }

    @Test
    fun testUsdRateCardDisplayFormatsToFourDecimals() {
        engine.dispatch(E2EExchangeAction.SelectBaseCurrency("EUR"))
        val usdCard = engine.state.rateCards.first { it.currency.code == "USD" }
        assertEquals(4, usdCard.formattedRate.substringAfter(".").length)
        assertEquals("$", usdCard.currency.symbol)
        assertEquals("US Dollar", usdCard.currency.commonName)
    }

    // =========================================================================
    // FEATURE 3: EUR Rates Calculation & Display (5 test cases)
    // =========================================================================

    @Test
    fun testEurToEurSelfRateIsExactlyOne() {
        val rate = engine.calculateRate("EUR", "EUR")
        assertEquals(1.0000, rate)
    }

    @Test
    fun testEurToUsdInvertedRateCalculation() {
        // R(EUR -> USD) = 1.0 / 0.9200 = 1.08695652...
        val rate = engine.calculateRate("EUR", "USD")
        val expected = 1.0 / 0.9200
        assertTrue(abs(rate - expected) < 1e-6)
        val conv = engine.convert(50.0, "EUR", "USD")
        assertEquals("54.35", conv.formattedTargetAmount)
    }

    @Test
    fun testEurToIlsCrossRateCalculation() {
        // R(EUR -> ILS) = R(USD -> ILS) / R(USD -> EUR) = 3.6500 / 0.9200 ≈ 3.9673913
        val rate = engine.calculateRate("EUR", "ILS")
        val expected = 3.6500 / 0.9200
        assertTrue(abs(rate - expected) < 1e-5)
        val conv = engine.convert(100.0, "EUR", "ILS")
        assertEquals("396.74", conv.formattedTargetAmount)
    }

    @Test
    fun testEurToJodPeggedCrossRate() {
        // R(EUR -> JOD) = R(EUR -> USD) * 0.7090 = (1 / 0.9200) * 0.7090 ≈ 0.770652
        val rate = engine.calculateRate("EUR", "JOD")
        val expected = (1.0 / 0.9200) * 0.7090
        assertTrue(abs(rate - expected) < 1e-5)
        val conv = engine.convert(100.0, "EUR", "JOD")
        assertEquals("77.065", conv.formattedTargetAmount)
    }

    @Test
    fun testEurMetadataSymbolAndPrecision() {
        val eur = E2ECurrency.EUR
        assertEquals("EUR", eur.code)
        assertEquals("€", eur.symbol)
        assertEquals(2, eur.decimalDigits)
        assertEquals("Euro", eur.commonName)
    }

    // =========================================================================
    // FEATURE 4: NIS (ILS) Rates Calculation & Display (5 test cases)
    // =========================================================================

    @Test
    fun testIlsToIlsSelfRateIsExactlyOne() {
        val rate = engine.calculateRate("ILS", "ILS")
        assertEquals(1.0000, rate)
    }

    @Test
    fun testIlsToUsdInvertedRateCalculation() {
        // R(ILS -> USD) = 1.0 / 3.6500 ≈ 0.2739726
        val rate = engine.calculateRate("ILS", "USD")
        val expected = 1.0 / 3.6500
        assertTrue(abs(rate - expected) < 1e-6)
        val conv = engine.convert(1000.0, "ILS", "USD")
        assertEquals("273.97", conv.formattedTargetAmount)
    }

    @Test
    fun testIlsToEurCrossRateCalculation() {
        // R(ILS -> EUR) = R(USD -> EUR) / R(USD -> ILS) = 0.9200 / 3.6500 ≈ 0.25205479
        val rate = engine.calculateRate("ILS", "EUR")
        val expected = 0.9200 / 3.6500
        assertTrue(abs(rate - expected) < 1e-6)
        val conv = engine.convert(1000.0, "ILS", "EUR")
        assertEquals("252.05", conv.formattedTargetAmount)
    }

    @Test
    fun testIlsToJodPeggedCrossRate() {
        // R(ILS -> JOD) = R(ILS -> USD) * 0.7090 = (1 / 3.6500) * 0.7090 ≈ 0.19424657
        val rate = engine.calculateRate("ILS", "JOD")
        val expected = (1.0 / 3.6500) * 0.7090
        assertTrue(abs(rate - expected) < 1e-6)
        val conv = engine.convert(1000.0, "ILS", "JOD")
        assertEquals("194.247", conv.formattedTargetAmount)
    }

    @Test
    fun testIlsMetadataSymbolAndPrecision() {
        val ils = E2ECurrency.ILS
        assertEquals("ILS", ils.code)
        assertEquals("₪", ils.symbol)
        assertEquals(2, ils.decimalDigits)
        assertEquals("Israeli New Shekel", ils.commonName)
    }

    // =========================================================================
    // FEATURE 5: JOD Rates Calculation & Display (5 test cases)
    // =========================================================================

    @Test
    fun testJodToJodSelfRateIsExactlyOne() {
        val rate = engine.calculateRate("JOD", "JOD")
        assertEquals(1.0000, rate)
    }

    @Test
    fun testJodToUsdInvertedPegRateCalculation() {
        // R(JOD -> USD) = 1.0 / 0.7090 ≈ 1.4104372
        val rate = engine.calculateRate("JOD", "USD")
        assertEquals(E2EJodPeg.JOD_TO_USD_RATE, rate)
        val conv = engine.convert(100.0, "JOD", "USD")
        assertEquals("141.04", conv.formattedTargetAmount)
    }

    @Test
    fun testJodToEurPeggedCrossRateCalculation() {
        // R(JOD -> EUR) = (1.0 / 0.7090) * 0.9200 ≈ 1.2976022
        val rate = engine.calculateRate("JOD", "EUR")
        val expected = (1.0 / 0.7090) * 0.9200
        assertTrue(abs(rate - expected) < 1e-5)
        val conv = engine.convert(100.0, "JOD", "EUR")
        assertEquals("129.76", conv.formattedTargetAmount)
    }

    @Test
    fun testJodToIlsPeggedCrossRateCalculation() {
        // R(JOD -> ILS) = (1.0 / 0.7090) * 3.6500 ≈ 5.1480959
        val rate = engine.calculateRate("JOD", "ILS")
        val expected = (1.0 / 0.7090) * 3.6500
        assertTrue(abs(rate - expected) < 1e-5)
        val conv = engine.convert(100.0, "JOD", "ILS")
        assertEquals("514.81", conv.formattedTargetAmount)
    }

    @Test
    fun testJodMetadataSymbolAndThreeDecimalPrecision() {
        val jod = E2ECurrency.JOD
        assertEquals("JOD", jod.code)
        assertEquals("JD", jod.symbol)
        assertEquals("د.أ.", jod.nativeSymbol)
        assertEquals(3, jod.decimalDigits, "JOD must enforce 3 decimal digits")
        assertEquals("Jordanian Dinar", jod.commonName)
    }

    // =========================================================================
    // FEATURE 6: Currency Conversion Calculation (6 test cases)
    // =========================================================================

    @Test
    fun testStandardConversionUsdToEur() {
        val result = engine.convert(250.0, "USD", "EUR")
        assertEquals(250.0, result.sourceAmount)
        assertEquals(230.00, result.rawTargetAmount)
        assertEquals("230.00", result.formattedTargetAmount)
    }

    @Test
    fun testStandardConversionUsdToJodPeg() {
        // 500 USD * 0.7090 = 354.500 JOD
        val result = engine.convert(500.0, "USD", "JOD")
        assertEquals(354.500, result.rawTargetAmount)
        assertEquals("354.500", result.formattedTargetAmount)
    }

    @Test
    fun testStandardConversionJodToUsdInvertedPeg() {
        // 70.900 JOD / 0.7090 = 100.00 USD
        val result = engine.convert(70.900, "JOD", "USD")
        assertTrue(abs(result.rawTargetAmount - 100.00) < 1e-4)
        assertEquals("100.00", result.formattedTargetAmount)
    }

    @Test
    fun testStandardConversionEurToIlsCrossRate() {
        // 1000 EUR * (3.65 / 0.92) ≈ 3967.39 ILS
        val result = engine.convert(1000.0, "EUR", "ILS")
        assertEquals("3,967.39", result.formattedTargetAmount)
    }

    @Test
    fun testSelfConversionAmountIsExactlyPreserved() {
        val result = engine.convert(1234.56, "ILS", "ILS")
        assertEquals(1234.56, result.rawTargetAmount)
        assertEquals("1,234.56", result.formattedTargetAmount)
        assertEquals(1.0000, result.rate)
    }

    @Test
    fun testFractionalAmountConversionPrecision() {
        // 12.34 USD * 0.9200 = 11.3528 -> rounds to 11.35 EUR
        val result = engine.convert(12.34, "USD", "EUR")
        assertEquals("11.35", result.formattedTargetAmount)
    }
}

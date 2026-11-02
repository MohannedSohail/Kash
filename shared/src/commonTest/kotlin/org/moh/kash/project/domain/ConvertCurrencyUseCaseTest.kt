package org.moh.kash.project.domain

import org.moh.kash.project.domain.model.Currency
import org.moh.kash.project.domain.usecase.ConvertCurrencyUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConvertCurrencyUseCaseTest {

    private val useCase = ConvertCurrencyUseCase()

    private val sampleRates = mapOf(
        "USD" to 1.0000,
        "EUR" to 0.9200,
        "ILS" to 3.6500,
        "JOD" to 0.7090
    )

    @Test
    fun testDirectConversionUsdToEur() {
        val result = useCase(100.0, "USD", Currency.EUR, sampleRates)
        assertEquals(92.00, result.convertedAmount, 0.001)
        assertEquals("92.00", result.formattedAmount)
        assertEquals("0.9200", result.formattedRate)
    }

    @Test
    fun testDirectConversionUsdToIls() {
        val result = useCase(100.0, "USD", Currency.ILS, sampleRates)
        assertEquals(365.00, result.convertedAmount, 0.001)
        assertEquals("365.00", result.formattedAmount)
        assertEquals("3.6500", result.formattedRate)
    }

    @Test
    fun testOfficialJodPegFromUsd() {
        // Spec §3.2: 1 USD = 0.7090 JOD strictly
        val rate = useCase.calculateRate("USD", "JOD", sampleRates)
        assertEquals(0.7090, rate, 0.00001)

        val result = useCase(100.0, "USD", Currency.JOD, sampleRates)
        assertEquals(70.900, result.convertedAmount, 0.001)
        assertEquals("70.900", result.formattedAmount, "JOD amounts must format with 3 decimal digits")
        assertEquals("0.7090", result.formattedRate)
    }

    @Test
    fun testOfficialJodPegInvertedToUsdExactDivision() {
        // Spec §3.2: 1 JOD = 1 / 0.7090 ≈ 1.4104372355...
        val rate = useCase.calculateRate("JOD", "USD", sampleRates)
        val expectedRate = 1.0 / 0.7090
        assertEquals(expectedRate, rate, 0.00000001)

        val result = useCase(100.0, "JOD", Currency.USD, sampleRates)
        val expectedAmount = 100.0 / 0.7090
        assertEquals(expectedAmount, result.convertedAmount, 0.0001)
        assertEquals("141.04", result.formattedAmount, "USD amounts format with 2 decimals")
        assertEquals("1.4104", result.formattedRate)
    }

    @Test
    fun testCrossRateEurToIls() {
        // R(EUR -> ILS) = R(USD -> ILS) / R(USD -> EUR) = 3.65 / 0.92 ≈ 3.96739
        val rate = useCase.calculateRate("EUR", "ILS", sampleRates)
        val expectedRate = 3.65 / 0.92
        assertEquals(expectedRate, rate, 0.0001)

        val result = useCase(50.0, "EUR", Currency.ILS, sampleRates)
        assertEquals(50.0 * expectedRate, result.convertedAmount, 0.001)
        assertEquals("3.9674", result.formattedRate)
    }

    @Test
    fun testCrossRateJodToIlsExactDivision() {
        // R(JOD -> ILS) = R(USD -> ILS) / 0.7090 = 3.65 / 0.7090 ≈ 5.1480959
        val rate = useCase.calculateRate("JOD", "ILS", sampleRates)
        val expectedRate = 3.65 / 0.7090
        assertEquals(expectedRate, rate, 0.000001)

        val result = useCase(10.0, "JOD", Currency.ILS, sampleRates)
        assertEquals(10.0 * expectedRate, result.convertedAmount, 0.001)
        assertEquals("51.48", result.formattedAmount)
    }

    @Test
    fun testSelfConversionPreservesIdentity() {
        for (currency in Currency.ALL_TOP_CURRENCIES) {
            val rate = useCase.calculateRate(currency.code, currency.code, sampleRates)
            assertEquals(1.0000, rate, 0.00001)

            val result = useCase(123.45, currency.code, currency, sampleRates)
            assertEquals(123.45, result.convertedAmount, 0.001)
            assertEquals("1.0000", result.formattedRate)
        }
    }

    @Test
    fun testZeroAmountReturnsZeroFormatted() {
        val usdResult = useCase(0.0, "EUR", Currency.USD, sampleRates)
        assertEquals(0.0, usdResult.convertedAmount, 0.0001)
        assertEquals("0.00", usdResult.formattedAmount)

        val jodResult = useCase(0.0, "USD", Currency.JOD, sampleRates)
        assertEquals(0.0, jodResult.convertedAmount, 0.0001)
        assertEquals("0.000", jodResult.formattedAmount)
    }

    @Test
    fun testNegativeAmountSanitization() {
        val result = useCase(-50.0, "USD", Currency.EUR, sampleRates)
        assertEquals(0.0, result.convertedAmount, 0.0001)
        assertEquals("0.00", result.formattedAmount)
    }

    @Test
    fun testHalfUpRoundingTwoDecimals() {
        // 100.555 -> 100.56
        val roundedUp = useCase.roundHalfUp(100.555, 2)
        assertEquals(100.56, roundedUp, 0.0001)
        assertEquals("100.56", useCase.formatNumber(100.555, 2))

        // 100.554 -> 100.55
        val roundedDown = useCase.roundHalfUp(100.554, 2)
        assertEquals(100.55, roundedDown, 0.0001)
        assertEquals("100.55", useCase.formatNumber(100.554, 2))
    }

    @Test
    fun testHalfUpRoundingThreeDecimalsJod() {
        // 70.9005 -> 70.901
        val roundedUp = useCase.roundHalfUp(70.9005, 3)
        assertEquals(70.901, roundedUp, 0.0001)
        assertEquals("70.901", useCase.formatNumber(70.9005, 3))

        // 70.9004 -> 70.900
        val roundedDown = useCase.roundHalfUp(70.9004, 3)
        assertEquals(70.900, roundedDown, 0.0001)
        assertEquals("70.900", useCase.formatNumber(70.9004, 3))
    }

    @Test
    fun testCommaThousandSeparators() {
        assertEquals("1,234,567.89", useCase.formatNumber(1234567.89, 2))
        assertEquals("12,345,678.90", useCase.formatNumber(12345678.9, 2))
        assertEquals("1,234,567.891", useCase.formatNumber(1234567.8912, 3))
    }

    @Test
    fun testExtremeAmountsUpToTrillion() {
        // 10^12 * 3.65 = 3,650,000,000,000.00
        val result = useCase(1_000_000_000_000.0, "USD", Currency.ILS, sampleRates)
        assertEquals("3,650,000,000,000.00", result.formattedAmount)
    }

    @Test
    fun testEurAnchoredRatesConversions() {
        val eurRates = mapOf(
            "EUR" to 1.0,
            "USD" to 1.087,
            "ILS" to 3.967,
            "JOD" to 0.7707
        )

        // EUR -> USD
        val toUsd = useCase(100.0, "EUR", Currency.USD, eurRates)
        assertEquals(108.70, toUsd.convertedAmount, 0.001)
        assertEquals("108.70", toUsd.formattedAmount)
        assertEquals("1.0870", toUsd.formattedRate)

        // EUR -> ILS
        val toIls = useCase(100.0, "EUR", Currency.ILS, eurRates)
        assertEquals(396.70, toIls.convertedAmount, 0.001)
        assertEquals("396.70", toIls.formattedAmount)
        assertEquals("3.9670", toIls.formattedRate)

        // EUR -> JOD
        val toJod = useCase(100.0, "EUR", Currency.JOD, eurRates)
        assertEquals(77.070, toJod.convertedAmount, 0.001)
        assertEquals("77.070", toJod.formattedAmount)
        assertEquals("0.7707", toJod.formattedRate)
    }

    @Test
    fun testIlsAnchoredRatesConversions() {
        val ilsRates = mapOf(
            "ILS" to 1.0,
            "USD" to 0.274,
            "EUR" to 0.252,
            "JOD" to 0.1943
        )

        // ILS -> USD
        val toUsd = useCase(100.0, "ILS", Currency.USD, ilsRates)
        assertEquals(27.40, toUsd.convertedAmount, 0.001)
        assertEquals("27.40", toUsd.formattedAmount)
        assertEquals("0.2740", toUsd.formattedRate)

        // ILS -> EUR
        val toEur = useCase(100.0, "ILS", Currency.EUR, ilsRates)
        assertEquals(25.20, toEur.convertedAmount, 0.001)
        assertEquals("25.20", toEur.formattedAmount)
        assertEquals("0.2520", toEur.formattedRate)

        // ILS -> JOD
        val toJod = useCase(100.0, "ILS", Currency.JOD, ilsRates)
        assertEquals(19.430, toJod.convertedAmount, 0.001)
        assertEquals("19.430", toJod.formattedAmount)
        assertEquals("0.1943", toJod.formattedRate)
    }

    @Test
    fun testJodAnchoredRatesConversions() {
        val jodRates = mapOf(
            "JOD" to 1.0,
            "USD" to 1.4104,
            "EUR" to 1.2976,
            "ILS" to 5.1481
        )

        // JOD -> USD
        val toUsd = useCase(100.0, "JOD", Currency.USD, jodRates)
        assertEquals(141.04, toUsd.convertedAmount, 0.001)
        assertEquals("141.04", toUsd.formattedAmount)
        assertEquals("1.4104", toUsd.formattedRate)

        // JOD -> EUR
        val toEur = useCase(100.0, "JOD", Currency.EUR, jodRates)
        assertEquals(129.76, toEur.convertedAmount, 0.001)
        assertEquals("129.76", toEur.formattedAmount)
        assertEquals("1.2976", toEur.formattedRate)

        // JOD -> ILS
        val toIls = useCase(100.0, "JOD", Currency.ILS, jodRates)
        assertEquals(514.81, toIls.convertedAmount, 0.001)
        assertEquals("514.81", toIls.formattedAmount)
        assertEquals("5.1481", toIls.formattedRate)
    }

    @Test
    fun testEurAnchoredWithoutJodDerivesPegFromUsdRate() {
        val eurWithoutJod = mapOf(
            "EUR" to 1.0,
            "USD" to 1.087,
            "ILS" to 3.967
        )

        // Expected EUR -> JOD rate = 1.087 * 0.7090 = 0.770683
        val rate = useCase.calculateRate("EUR", "JOD", eurWithoutJod)
        assertEquals(1.087 * 0.7090, rate, 0.00001)

        val result = useCase(100.0, "EUR", Currency.JOD, eurWithoutJod)
        val expectedAmount = 100.0 * 1.087 * 0.7090
        assertEquals(expectedAmount, result.convertedAmount, 0.001)
        assertEquals("77.068", result.formattedAmount)
        assertEquals("0.7707", result.formattedRate)
    }
}

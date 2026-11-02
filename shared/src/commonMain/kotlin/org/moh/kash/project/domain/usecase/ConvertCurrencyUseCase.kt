package org.moh.kash.project.domain.usecase

import org.moh.kash.project.domain.model.ConversionResult
import org.moh.kash.project.domain.model.Currency
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round

/**
 * Use case executing financial currency conversions, cross-rates, and official JOD peg derivations.
 * Pure Kotlin Multiplatform — zero java.* or android.* imports.
 */
class ConvertCurrencyUseCase {

    companion object {
        const val JOD_USD_PEG = 0.7090
    }

    /**
     * Converts an [amount] from [baseCurrencyCode] to [targetCurrency] given a [rates] map.
     * Note: [rates] is expected to contain rates relative to USD (USD = 1.0) or relative to [baseCurrencyCode].
     */
    operator fun invoke(
        amount: Double,
        baseCurrencyCode: String,
        targetCurrency: Currency,
        rates: Map<String, Double>
    ): ConversionResult {
        val sanitizedAmount = if (amount <= 0.0 || amount.isNaN() || amount.isInfinite()) 0.0 else amount
        val rate = calculateRate(baseCurrencyCode, targetCurrency.code, rates)
        val rawConverted = sanitizedAmount * rate
        val formattedAmount = formatNumber(rawConverted, targetCurrency.decimalDigits)
        val formattedRate = formatRate(rate)

        return ConversionResult(
            sourceAmount = sanitizedAmount,
            baseCurrency = baseCurrencyCode.uppercase(),
            targetCurrency = targetCurrency,
            rate = rate,
            convertedAmount = rawConverted,
            formattedAmount = formattedAmount,
            formattedRate = formattedRate
        )
    }

    /**
     * Overload accepting target currency code as String.
     */
    operator fun invoke(
        amount: Double,
        baseCurrencyCode: String,
        targetCurrencyCode: String,
        rates: Map<String, Double>
    ): ConversionResult {
        val target = Currency.fromCode(targetCurrencyCode)
            ?: Currency(
                code = targetCurrencyCode.uppercase(),
                name = targetCurrencyCode.uppercase(),
                symbol = targetCurrencyCode.uppercase(),
                nativeSymbol = targetCurrencyCode.uppercase(),
                decimalDigits = 2,
                flagEmoji = "",
                desktopBadge = targetCurrencyCode.take(2).uppercase()
            )
        return invoke(amount, baseCurrencyCode, target, rates)
    }

    /**
     * Direct quote conversion when unit rates are already provided.
     */
    operator fun invoke(
        amount: Double,
        baseRate: Double,
        targetRate: Double,
        targetCurrency: Currency
    ): ConversionResult {
        val sanitizedAmount = if (amount <= 0.0 || amount.isNaN() || amount.isInfinite()) 0.0 else amount
        val effectiveRate = if (baseRate > 0.0) targetRate / baseRate else 1.0
        val rawConverted = sanitizedAmount * effectiveRate
        val formattedAmount = formatNumber(rawConverted, targetCurrency.decimalDigits)
        val formattedRate = formatRate(effectiveRate)

        return ConversionResult(
            sourceAmount = sanitizedAmount,
            baseCurrency = "",
            targetCurrency = targetCurrency,
            rate = effectiveRate,
            convertedAmount = rawConverted,
            formattedAmount = formattedAmount,
            formattedRate = formattedRate
        )
    }

    /**
     * Calculates the exchange rate between any two currencies.
     * Incorporates the Central Bank of Jordan USD peg:
     * 1 USD = 0.7090 JOD (exact mathematical division by 0.7090).
     */
    fun calculateRate(baseCode: String, targetCode: String, rates: Map<String, Double>): Double {
        val b = baseCode.trim().uppercase()
        val t = targetCode.trim().uppercase()

        if (b == t) return 1.0000

        val rateB = when {
            rates.containsKey(b) -> rates[b]!!
            b == "USD" -> 1.0
            b == "JOD" -> (rates["USD"] ?: 1.0) * JOD_USD_PEG
            else -> 1.0
        }
        val rateT = when {
            rates.containsKey(t) -> rates[t]!!
            t == "USD" -> 1.0
            t == "JOD" -> (rates["USD"] ?: 1.0) * JOD_USD_PEG
            else -> 1.0
        }

        return if (rateB > 0.0) rateT / rateB else 1.0000
    }

    /**
     * Half-Up financial rounding to [decimals] places.
     */
    fun roundHalfUp(value: Double, decimals: Int): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        val factor = 10.0.pow(decimals)
        val sign = if (value < 0) -1.0 else 1.0
        return sign * (round((abs(value) + 1e-10) * factor) / factor)
    }

    /**
     * Formats number to fixed decimal places with comma thousand separators.
     */
    fun formatNumber(value: Double, decimals: Int): String {
        if (value.isNaN() || value.isInfinite()) {
            return if (decimals == 3) "0.000" else "0.00"
        }

        val rounded = roundHalfUp(abs(value), decimals)
        val integerPart = floor(rounded).toLong()
        val fractionalFactor = 10.0.pow(decimals)
        val fractionalPart = round((rounded - integerPart) * fractionalFactor).toLong()

        val intWithCommas = formatWithCommas(integerPart)
        val fracString = fractionalPart.toString().padStart(decimals, '0')
        val sign = if (value < 0 && (integerPart > 0 || fractionalPart > 0)) "-" else ""

        return "$sign$intWithCommas.$fracString"
    }

    /**
     * Formats exchange rate to 4 decimal places.
     */
    fun formatRate(rate: Double): String {
        return formatNumber(rate, 4)
    }

    private fun formatWithCommas(n: Long): String {
        val s = n.toString()
        val len = s.length
        if (len <= 3) return s

        val sb = StringBuilder()
        var count = 0
        for (i in len - 1 downTo 0) {
            sb.append(s[i])
            count++
            if (count % 3 == 0 && i > 0) {
                sb.append(',')
            }
        }
        return sb.reverse().toString()
    }
}

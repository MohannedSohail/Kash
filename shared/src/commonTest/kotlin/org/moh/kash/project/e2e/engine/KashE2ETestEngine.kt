package org.moh.kash.project.e2e.engine

import org.moh.kash.project.e2e.model.E2EConversionResult
import org.moh.kash.project.e2e.model.E2ECurrency
import org.moh.kash.project.e2e.model.E2EExchangeAction
import org.moh.kash.project.e2e.model.E2EExchangeUiState
import org.moh.kash.project.e2e.model.E2EJodPeg
import org.moh.kash.project.e2e.model.E2ERateCardItem
import org.moh.kash.project.e2e.model.E2ESeedRates
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round

/**
 * Kash E2E Opaque-Box Test Engine & Specification Reference Harness.
 *
 * Implements the authoritative business logic and contract rules defined in:
 * - ORIGINAL_REQUEST.md (§R2 Currency Exchange Application)
 * - PROJECT.md (§2 Feature Inventory & §Code Layout)
 * - spec.md (§2 FreeCurrencyAPI, §3 JOD Peg, §4 Math, §5 Cache & Seed Rates, §6 MVI)
 */
class KashE2ETestEngine(
    initialBaseCurrency: String = "USD",
    initialAmount: String = "100.00",
    initialSeedRates: Map<String, Double> = E2ESeedRates.BASE_USD
) {
    // Current remote rates relative to USD (USD = 1.0)
    private var usdBaseRates: MutableMap<String, Double> = initialSeedRates.toMutableMap()

    // Local DataStore cache snapshot
    private var cachedUsdRates: Map<String, Double>? = null
    private var cacheTimestamp: Long = 0L

    // Current network simulation state
    private var isNetworkAvailable: Boolean = true
    private var pendingHttpStatus: Int = 200
    private var pendingHttpErrorMessage: String? = null

    // Track remote queries for verification (opaque-box wire verification)
    val interceptedRemoteQueries = mutableListOf<String>()

    private var _state: E2EExchangeUiState

    val state: E2EExchangeUiState
        get() = _state

    init {
        val initialRates = calculateAllRates(initialBaseCurrency)
        val initialCards = buildRateCards(initialRates, parseAmount(initialAmount), initialBaseCurrency)
        _state = E2EExchangeUiState(
            baseCurrency = initialBaseCurrency,
            inputAmount = initialAmount,
            rateCards = initialCards,
            isColdLoading = false,
            isRefreshing = false,
            isOffline = false,
            lastUpdatedTimestamp = 1700000000000L
        )
    }

    /**
     * Dispatch an MVI user or system action to update the state.
     */
    fun dispatch(action: E2EExchangeAction) {
        when (action) {
            is E2EExchangeAction.SelectBaseCurrency -> {
                val newBase = action.currencyCode.uppercase()
                if (newBase == _state.baseCurrency) return // Idempotent

                val parsedAmount = parseAmount(_state.inputAmount)
                val newRates = calculateAllRates(newBase)
                val newCards = buildRateCards(newRates, parsedAmount, newBase)
                _state = _state.copy(
                    baseCurrency = newBase,
                    rateCards = newCards,
                    snackbarEffect = null
                )
            }

            is E2EExchangeAction.ChangeAmount -> {
                val raw = action.rawAmount
                val (sanitized, fieldError) = sanitizeInputAmount(raw)
                val parsedAmount = parseAmount(sanitized)
                val currentRates = calculateAllRates(_state.baseCurrency)
                val newCards = buildRateCards(currentRates, parsedAmount, _state.baseCurrency)
                _state = _state.copy(
                    inputAmount = sanitized,
                    rateCards = newCards,
                    fieldError = fieldError,
                    snackbarEffect = null
                )
            }

            is E2EExchangeAction.SelectQuickAmount -> {
                val formatted = formatNumber(action.amount, 2)
                val parsedAmount = action.amount
                val currentRates = calculateAllRates(_state.baseCurrency)
                val newCards = buildRateCards(currentRates, parsedAmount, _state.baseCurrency)
                _state = _state.copy(
                    inputAmount = formatted,
                    rateCards = newCards,
                    fieldError = null,
                    snackbarEffect = null
                )
            }

            is E2EExchangeAction.ClearAmount -> {
                val currentRates = calculateAllRates(_state.baseCurrency)
                val newCards = buildRateCards(currentRates, 0.0, _state.baseCurrency)
                _state = _state.copy(
                    inputAmount = "",
                    rateCards = newCards,
                    fieldError = null,
                    snackbarEffect = null
                )
            }

            is E2EExchangeAction.RefreshRates -> {
                performRefresh()
            }

            is E2EExchangeAction.RetryColdLoad -> {
                performColdLoad()
            }

            is E2EExchangeAction.SimulateNetworkError -> {
                pendingHttpStatus = action.statusCode
                pendingHttpErrorMessage = action.message
            }

            is E2EExchangeAction.DisconnectNetwork -> {
                isNetworkAvailable = false
            }

            is E2EExchangeAction.RestoreNetwork -> {
                isNetworkAvailable = true
                pendingHttpStatus = 200
                pendingHttpErrorMessage = null
            }
        }
    }

    /**
     * Executes an explicit currency conversion mathematically.
     */
    fun convert(amount: Double, baseCode: String, targetCode: String): E2EConversionResult {
        val target = E2ECurrency.findByCode(targetCode)
            ?: throw IllegalArgumentException("Unsupported target currency: $targetCode")

        val rate = calculateRate(baseCode, targetCode)
        val rawConverted = amount * rate
        val formattedAmount = formatNumber(rawConverted, target.decimalDigits)
        val formattedRate = formatRate(rate)

        return E2EConversionResult(
            sourceAmount = amount,
            baseCurrency = baseCode,
            targetCurrency = target,
            rate = rate,
            rawTargetAmount = rawConverted,
            formattedTargetAmount = formattedAmount,
            formattedRate = formattedRate
        )
    }

    /**
     * Updates simulated live rates from upstream API.
     */
    fun updateLiveRates(ratesRelativeToUsd: Map<String, Double>) {
        usdBaseRates.putAll(ratesRelativeToUsd)
        // Ensure JOD is derived via peg if not explicitly supplied
        if (!ratesRelativeToUsd.containsKey("JOD")) {
            usdBaseRates["JOD"] = E2EJodPeg.USD_TO_JOD_RATE
        }
        val currentRates = calculateAllRates(_state.baseCurrency)
        val parsedAmount = parseAmount(_state.inputAmount)
        _state = _state.copy(
            rateCards = buildRateCards(currentRates, parsedAmount, _state.baseCurrency),
            lastUpdatedTimestamp = 1700000500000L
        )
    }

    /**
     * Calculates the exchange rate between any two currencies.
     * Incorporates the official Central Bank of Jordan USD peg:
     * 1 USD = 0.7090 JOD (1 JOD = 1 / 0.7090 USD).
     */
    fun calculateRate(baseCode: String, targetCode: String): Double {
        val b = baseCode.uppercase()
        val t = targetCode.uppercase()

        if (b == t) return 1.0000

        // Get USD-relative rates
        val usdToB = when (b) {
            "USD" -> 1.0
            "JOD" -> E2EJodPeg.USD_TO_JOD_RATE
            else -> usdBaseRates[b] ?: throw IllegalArgumentException("Unknown currency: $b")
        }

        val usdToT = when (t) {
            "USD" -> 1.0
            "JOD" -> E2EJodPeg.USD_TO_JOD_RATE
            else -> usdBaseRates[t] ?: throw IllegalArgumentException("Unknown currency: $t")
        }

        // Cross-rate calculation: R(B -> T) = R(USD -> T) / R(USD -> B)
        return usdToT / usdToB
    }

    /**
     * Calculates rates for all top currencies relative to the given base.
     */
    fun calculateAllRates(baseCode: String): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        for (currency in E2ECurrency.ALL_TOP_CURRENCIES) {
            result[currency.code] = calculateRate(baseCode, currency.code)
        }
        return result
    }

    private fun performRefresh() {
        if (!isNetworkAvailable || pendingHttpStatus != 200) {
            handleNetworkFailure()
            return
        }

        // Record wire request: client MUST only request USD,EUR,ILS, never JOD
        val queryUrl = "GET /v1/latest?base_currency=${if (_state.baseCurrency == "JOD") "USD" else _state.baseCurrency}&currencies=USD,EUR,ILS"
        interceptedRemoteQueries.add(queryUrl)

        // Successful 200
        cachedUsdRates = usdBaseRates.toMap()
        cacheTimestamp = 1700001000000L
        val currentRates = calculateAllRates(_state.baseCurrency)
        val parsedAmount = parseAmount(_state.inputAmount)

        _state = _state.copy(
            isRefreshing = false,
            isOffline = false,
            errorBanner = null,
            snackbarEffect = null,
            rateCards = buildRateCards(currentRates, parsedAmount, _state.baseCurrency),
            lastUpdatedTimestamp = cacheTimestamp
        )
    }

    private fun performColdLoad() {
        if (!isNetworkAvailable || pendingHttpStatus != 200) {
            if (cachedUsdRates != null) {
                // Fall back to cache
                val currentRates = calculateAllRates(_state.baseCurrency)
                val parsedAmount = parseAmount(_state.inputAmount)
                _state = _state.copy(
                    isColdLoading = false,
                    isOffline = true,
                    rateCards = buildRateCards(currentRates, parsedAmount, _state.baseCurrency),
                    errorBanner = "Offline: displaying cached rates"
                )
            } else {
                // Fall back to bundled SeedRates
                usdBaseRates = E2ESeedRates.BASE_USD.toMutableMap()
                val currentRates = calculateAllRates(_state.baseCurrency)
                val parsedAmount = parseAmount(_state.inputAmount)
                _state = _state.copy(
                    isColdLoading = false,
                    isOffline = true,
                    rateCards = buildRateCards(currentRates, parsedAmount, _state.baseCurrency),
                    errorBanner = "Offline: using bundled seed rates"
                )
            }
            return
        }

        performRefresh()
    }

    private fun handleNetworkFailure() {
        when (pendingHttpStatus) {
            401 -> {
                _state = _state.copy(
                    isRefreshing = false,
                    errorBanner = "Invalid API Key. Please verify FreeCurrencyAPI credentials.",
                    snackbarEffect = "Authentication error: check API key"
                )
            }
            422 -> {
                _state = _state.copy(
                    isRefreshing = false,
                    errorBanner = "Unprocessable Entity: requested currency not supported by API.",
                    snackbarEffect = "Validation error: unsupported currency"
                )
            }
            429 -> {
                // Popup tier: visible rates preserved, snackbar emitted
                _state = _state.copy(
                    isRefreshing = false,
                    isOffline = true,
                    snackbarEffect = "Rate limit reached. Displaying cached rates."
                )
            }
            500, 503 -> {
                _state = _state.copy(
                    isRefreshing = false,
                    isOffline = true,
                    snackbarEffect = "Upstream server unavailable. Displaying cached rates."
                )
            }
            else -> {
                // Connection offline
                _state = _state.copy(
                    isRefreshing = false,
                    isOffline = true,
                    snackbarEffect = "Could not refresh rates. Network unavailable."
                )
            }
        }
    }

    private fun buildRateCards(
        rates: Map<String, Double>,
        amount: Double,
        baseCurrency: String
    ): List<E2ERateCardItem> {
        return E2ECurrency.ALL_TOP_CURRENCIES
            .filter { it.code != baseCurrency }
            .map { currency ->
                val rate = rates[currency.code] ?: 1.0
                val converted = amount * rate
                E2ERateCardItem(
                    currency = currency,
                    unitRate = rate,
                    formattedRate = formatRate(rate),
                    convertedAmount = converted,
                    formattedAmount = formatNumber(converted, currency.decimalDigits)
                )
            }
    }

    companion object {
        /**
         * Sanitizes raw amount input.
         * Enforces positive numbers, strips extraneous non-numeric characters,
         * allows a single decimal point, and caps at 12 digits.
         */
        fun sanitizeInputAmount(raw: String): Pair<String, String?> {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return Pair("", null)

            if (trimmed.startsWith("-")) {
                val cleaned = trimmed.removePrefix("-").trimStart()
                return Pair(sanitizeDigits(cleaned), "Amount must be positive")
            }

            val sanitized = sanitizeDigits(trimmed)
            return Pair(sanitized, null)
        }

        private fun sanitizeDigits(input: String): String {
            val sb = StringBuilder()
            var hasDot = false
            var digitCount = 0

            for (ch in input) {
                if (ch.isDigit()) {
                    if (digitCount < 12) {
                        sb.append(ch)
                        digitCount++
                    }
                } else if (ch == '.' && !hasDot) {
                    hasDot = true
                    sb.append(ch)
                }
            }
            return sb.toString()
        }

        fun parseAmount(str: String): Double {
            if (str.isBlank()) return 0.0
            return str.toDoubleOrNull() ?: 0.0
        }

        /**
         * Standard Financial Half-Up Rounding to [decimals] places.
         */
        fun roundHalfUp(value: Double, decimals: Int): Double {
            val factor = 10.0.pow(decimals)
            val sign = if (value < 0) -1.0 else 1.0
            return sign * (round((abs(value) + 1e-10) * factor) / factor)
        }

        /**
         * Formats double with fixed [decimals] and localized thousands comma separators.
         */
        fun formatNumber(value: Double, decimals: Int): String {
            if (value.isNaN() || value.isInfinite()) return "0.00"

            val rounded = roundHalfUp(abs(value), decimals)
            val integerPart = floor(rounded).toLong()
            val fractionalFactor = 10.0.pow(decimals)
            val fractionalPart = round((rounded - integerPart) * fractionalFactor).toLong()

            val intWithCommas = formatWithCommas(integerPart)
            val fracString = fractionalPart.toString().padStart(decimals, '0')

            return "$intWithCommas.$fracString"
        }

        /**
         * Formats exchange rate to exactly 4 decimal places.
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
}

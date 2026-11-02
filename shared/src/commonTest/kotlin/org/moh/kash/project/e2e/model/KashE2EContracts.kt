package org.moh.kash.project.e2e.model

/**
 * Authoritative E2E Test Contracts for the Kash Currency Exchange Application.
 * Derived directly from ORIGINAL_REQUEST.md, PROJECT.md, and spec.md.
 */

/**
 * Currency metadata specification.
 */
data class E2ECurrency(
    val code: String,
    val commonName: String,
    val symbol: String,
    val nativeSymbol: String,
    val decimalDigits: Int,
    val flagEmoji: String,
    val desktopBadge: String
) {
    companion object {
        val USD = E2ECurrency(
            code = "USD",
            commonName = "US Dollar",
            symbol = "$",
            nativeSymbol = "$",
            decimalDigits = 2,
            flagEmoji = "\uD83C\uDDFA\uD83C\uDDF8",
            desktopBadge = "$"
        )
        val EUR = E2ECurrency(
            code = "EUR",
            commonName = "Euro",
            symbol = "€",
            nativeSymbol = "€",
            decimalDigits = 2,
            flagEmoji = "\uD83C\uDDEA\uD83C\uDDFA",
            desktopBadge = "€"
        )
        val ILS = E2ECurrency(
            code = "ILS",
            commonName = "Israeli New Shekel",
            symbol = "₪",
            nativeSymbol = "₪",
            decimalDigits = 2,
            flagEmoji = "\uD83C\uDDEE\uD83C\uDDF1",
            desktopBadge = "₪"
        )
        val JOD = E2ECurrency(
            code = "JOD",
            commonName = "Jordanian Dinar",
            symbol = "JD",
            nativeSymbol = "د.أ.",
            decimalDigits = 3,
            flagEmoji = "\uD83C\uDDEF\uD83C\uDDF4",
            desktopBadge = "JD"
        )

        val ALL_TOP_CURRENCIES = listOf(USD, EUR, ILS, JOD)
        val TOP_CURRENCY_CODES = ALL_TOP_CURRENCIES.map { it.code }

        fun findByCode(code: String): E2ECurrency? {
            return ALL_TOP_CURRENCIES.find { it.code.equals(code, ignoreCase = true) }
        }
    }
}

/**
 * Official Central Bank of Jordan Peg Constant.
 * 1 USD = 0.7090 JOD (Fixed legal peg since 1995).
 * Inverted: 1 JOD = 1 / 0.7090 ≈ 1.4104372 USD.
 */
object E2EJodPeg {
    const val USD_TO_JOD_RATE = 0.7090
    val JOD_TO_USD_RATE: Double = 1.0 / USD_TO_JOD_RATE // ≈ 1.4104372355430184
}

/**
 * Bundled Seed Rates for Cold Start and Offline Mode.
 */
object E2ESeedRates {
    val BASE_USD = mapOf(
        "USD" to 1.0000,
        "EUR" to 0.9200,
        "ILS" to 3.6500,
        "JOD" to 0.7090
    )
}

/**
 * Conversion Calculation Result.
 */
data class E2EConversionResult(
    val sourceAmount: Double,
    val baseCurrency: String,
    val targetCurrency: E2ECurrency,
    val rate: Double,
    val rawTargetAmount: Double,
    val formattedTargetAmount: String,
    val formattedRate: String
)

/**
 * UI State Model for E2E Opaque-Box Verification.
 */
data class E2EExchangeUiState(
    val baseCurrency: String = "USD",
    val inputAmount: String = "100.00",
    val rateCards: List<E2ERateCardItem> = emptyList(),
    val isColdLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val errorBanner: String? = null,
    val fieldError: String? = null,
    val snackbarEffect: String? = null,
    val lastUpdatedTimestamp: Long = 0L
)

data class E2ERateCardItem(
    val currency: E2ECurrency,
    val unitRate: Double,
    val formattedRate: String,
    val convertedAmount: Double,
    val formattedAmount: String
)

/**
 * User and System Actions.
 */
sealed interface E2EExchangeAction {
    data class SelectBaseCurrency(val currencyCode: String) : E2EExchangeAction
    data class ChangeAmount(val rawAmount: String) : E2EExchangeAction
    data class SelectQuickAmount(val amount: Double) : E2EExchangeAction
    data object ClearAmount : E2EExchangeAction
    data object RefreshRates : E2EExchangeAction
    data object RetryColdLoad : E2EExchangeAction
    data class SimulateNetworkError(val statusCode: Int, val message: String) : E2EExchangeAction
    data object RestoreNetwork : E2EExchangeAction
    data object DisconnectNetwork : E2EExchangeAction
}

package org.moh.kash.project.domain.model

/**
 * Domain Currency model representing supported top currencies:
 * USD, EUR, ILS (NIS), and JOD.
 * Pure Kotlin — zero framework, serialization, or Compose dependencies.
 */
data class Currency(
    val code: String,
    val name: String,
    val symbol: String,
    val nativeSymbol: String,
    val decimalDigits: Int,
    val flagEmoji: String,
    val desktopBadge: String,
    val commonDisplay: String = code
) {
    companion object {
        val USD = Currency(
            code = "USD",
            name = "US Dollar",
            symbol = "$",
            nativeSymbol = "$",
            decimalDigits = 2,
            flagEmoji = "\uD83C\uDDFA\uD83C\uDDF8",
            desktopBadge = "$",
            commonDisplay = "USD"
        )

        val EUR = Currency(
            code = "EUR",
            name = "Euro",
            symbol = "€",
            nativeSymbol = "€",
            decimalDigits = 2,
            flagEmoji = "\uD83C\uDDEA\uD83C\uDDFA",
            desktopBadge = "€",
            commonDisplay = "EUR"
        )

        val ILS = Currency(
            code = "ILS",
            name = "Israeli New Shekel",
            symbol = "₪",
            nativeSymbol = "₪",
            decimalDigits = 2,
            flagEmoji = "\uD83C\uDDEE\uD83C\uDDF1",
            desktopBadge = "₪",
            commonDisplay = "NIS / ILS"
        )

        val JOD = Currency(
            code = "JOD",
            name = "Jordanian Dinar",
            symbol = "JD",
            nativeSymbol = "د.أ.",
            decimalDigits = 3,
            flagEmoji = "\uD83C\uDDEF\uD83C\uDDF4",
            desktopBadge = "JD",
            commonDisplay = "JOD"
        )

        val ALL_TOP_CURRENCIES: List<Currency> = listOf(USD, EUR, ILS, JOD)

        fun fromCode(code: String): Currency? {
            val normalized = code.trim().uppercase()
            return when (normalized) {
                "USD" -> USD
                "EUR" -> EUR
                "ILS", "NIS" -> ILS
                "JOD" -> JOD
                else -> null
            }
        }
    }
}

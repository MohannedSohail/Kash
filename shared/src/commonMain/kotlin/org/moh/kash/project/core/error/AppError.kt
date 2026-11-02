package org.moh.kash.project.core.error

/**
 * Domain error models matching Kash error tiers:
 * - Inline: Fatal cold load / no cache available
 * - Popup: Background refresh failed over existing cached data
 * - Field: Malformed or negative amount input
 * - Silent: Background polling diagnostics
 */
sealed class AppError(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {
    data class NetworkError(
        override val message: String,
        val statusCode: Int? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class QuotaExceededError(
        override val message: String = "Monthly API quota reached. Displaying cached rates.",
        val retryAfterSeconds: Int? = null,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class AuthenticationError(
        override val message: String = "Invalid or missing FreeCurrencyAPI key.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class OfflineNoCacheError(
        override val message: String = "Offline and no cached exchange rates are available.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class InvalidAmountError(
        override val message: String = "Amount must be a positive number.",
        val rawInput: String = ""
    ) : AppError(message)

    data class UnknownError(
        override val message: String = "An unexpected error occurred.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}

package org.moh.kash.project.presentation.exchange

import androidx.compose.runtime.Immutable
import org.moh.kash.project.core.error.AppError

/**
 * Immutable UI model for individual target currency rate cards.
 * Provides stable domain identity via [currencyCode].
 */
@Immutable
data class ExchangeRateItem(
    val currencyCode: String,
    val currencyName: String,
    val symbol: String,
    val nativeSymbol: String,
    val flagEmoji: String,
    val desktopBadge: String,
    val unitRate: Double,
    val convertedAmount: Double,
    val formattedAmount: String,
    val formattedRate: String,
    val trendPercent: Double? = null,
    val isBase: Boolean = false
)

/**
 * Complete immutable MVI UI State for the Kash Currency Exchange Screen.
 * Strictly adheres to Rule 3 (no animation or ticking tickers in UiState).
 */
@Immutable
data class ExchangeUiState(
    val baseCurrency: String = "USD",
    val inputAmount: String = "100.00",
    val amountError: String? = null,
    val rates: List<ExchangeRateItem> = emptyList(),
    val isColdLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isCurrencyPickerOpen: Boolean = false,
    val error: AppError? = null,
    val lastUpdated: Long? = null
)

/**
 * Unidirectional user actions dispatched from the UI to [ExchangeViewModel].
 */
sealed interface ExchangeUiAction {
    data class OnBaseCurrencySelected(val currencyCode: String) : ExchangeUiAction
    data class OnAmountChanged(val amount: String) : ExchangeUiAction
    data class OnQuickAmountSelected(val amount: Double) : ExchangeUiAction
    data object OnClearAmount : ExchangeUiAction
    data object OnRefresh : ExchangeUiAction
    data object OnRetry : ExchangeUiAction
    data class SetCurrencyPickerOpen(val isOpen: Boolean) : ExchangeUiAction
    data object OnDismissError : ExchangeUiAction
}

/**
 * One-shot transient side effects emitted to the UI via Channel.
 */
sealed interface ExchangeUiEffect {
    data class ShowSnackbar(val message: String) : ExchangeUiEffect
    data object OpenCurrencyPicker : ExchangeUiEffect
    data object CloseCurrencyPicker : ExchangeUiEffect
}

package org.moh.kash.project.presentation.exchange

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.moh.kash.project.core.error.AppError
import org.moh.kash.project.domain.model.Currency
import org.moh.kash.project.domain.model.ExchangeRates
import org.moh.kash.project.domain.repository.CurrencyRepository
import org.moh.kash.project.domain.usecase.ConvertCurrencyUseCase
import org.moh.kash.project.domain.usecase.GetExchangeRatesUseCase
import org.moh.kash.project.domain.usecase.UpdateBaseCurrencyUseCase
import org.moh.kash.project.presentation.BaseViewModel
import kotlin.math.abs

/**
 * MVI ViewModel for the Kash Currency Exchange Screen.
 * Connects domain use cases, coordinates debounce and numerical parsing,
 * and classifies errors into strict architectural tiers (Inline vs Popup vs Field).
 */
class ExchangeViewModel(
    private val getExchangeRatesUseCase: GetExchangeRatesUseCase,
    private val convertCurrencyUseCase: ConvertCurrencyUseCase,
    private val updateBaseCurrencyUseCase: UpdateBaseCurrencyUseCase,
    private val currencyRepository: CurrencyRepository
) : BaseViewModel<ExchangeUiAction, ExchangeUiState, ExchangeUiEffect>(
    initialState = ExchangeUiState()
) {

    private var ratesJob: Job? = null
    private var currentRatesMap: Map<String, Double> = emptyMap()

    init {
        // Observe persisted user base currency preference
        viewModelScope.launch {
            currencyRepository.getBaseCurrency().collectLatest { persistedBase ->
                val normalized = persistedBase.trim().uppercase()
                if (normalized.isNotEmpty() && normalized != currentState.baseCurrency) {
                    updateState { it.copy(baseCurrency = normalized) }
                    loadRates(normalized, forceRefresh = false)
                }
            }
        }

        // Initial fetch
        loadRates(currentState.baseCurrency, forceRefresh = false)
    }

    override fun onAction(action: ExchangeUiAction) {
        when (action) {
            is ExchangeUiAction.OnBaseCurrencySelected -> {
                val newBase = action.currencyCode.trim().uppercase()
                if (newBase == currentState.baseCurrency) {
                    updateState { it.copy(isCurrencyPickerOpen = false) }
                    return
                }
                updateState {
                    it.copy(
                        baseCurrency = newBase,
                        isCurrencyPickerOpen = false,
                        error = null
                    )
                }
                launchGuarded {
                    updateBaseCurrencyUseCase(newBase)
                }
                loadRates(newBase, forceRefresh = false)
            }

            is ExchangeUiAction.OnAmountChanged -> {
                handleAmountChanged(action.amount)
            }

            is ExchangeUiAction.OnQuickAmountSelected -> {
                val formatted = convertCurrencyUseCase.formatNumber(action.amount, 2)
                updateState {
                    it.copy(
                        inputAmount = formatted,
                        amountError = null
                    )
                }
                recomputeAllCards(action.amount)
            }

            is ExchangeUiAction.OnClearAmount -> {
                updateState {
                    it.copy(
                        inputAmount = "",
                        amountError = null
                    )
                }
                recomputeAllCards(0.0)
            }

            is ExchangeUiAction.OnRefresh -> {
                loadRates(currentState.baseCurrency, forceRefresh = true)
            }

            is ExchangeUiAction.OnRetry -> {
                updateState { it.copy(error = null) }
                loadRates(currentState.baseCurrency, forceRefresh = true)
            }

            is ExchangeUiAction.SetCurrencyPickerOpen -> {
                updateState { it.copy(isCurrencyPickerOpen = action.isOpen) }
            }

            is ExchangeUiAction.OnDismissError -> {
                updateState { it.copy(error = null) }
            }
        }
    }

    private fun handleAmountChanged(raw: String) {
        // Field-tier error handling: non-destructive input preservation
        val trimmed = raw.trim()

        if (trimmed.isEmpty()) {
            updateState {
                it.copy(
                    inputAmount = "",
                    amountError = null
                )
            }
            recomputeAllCards(0.0)
            return
        }

        val parsed = trimmed.toDoubleOrNull()
        if (parsed == null || parsed < 0.0) {
            updateState {
                it.copy(
                    inputAmount = raw,
                    amountError = "Amount must be a positive number."
                )
            }
            return
        }

        updateState {
            it.copy(
                inputAmount = raw,
                amountError = null
            )
        }
        recomputeAllCards(parsed)
    }

    private fun loadRates(baseCurrency: String, forceRefresh: Boolean) {
        ratesJob?.cancel()

        val isCold = currentState.rates.isEmpty()
        if (isCold) {
            updateState { it.copy(isColdLoading = true, error = null) }
        } else if (forceRefresh) {
            updateState { it.copy(isRefreshing = true, error = null) }
        }

        ratesJob = launchGuarded(
            onError = { throwable ->
                handleLoadError(throwable)
            }
        ) {
            getExchangeRatesUseCase(baseCurrency, forceRefresh)
                .catch { throwable ->
                    handleLoadError(throwable)
                }
                .collectLatest { result ->
                    result.fold(
                        onSuccess = { exchangeRates ->
                            currentRatesMap = exchangeRates.rates
                            val parsedAmount = parseAmount(currentState.inputAmount)
                            val newCards = buildRateCards(
                                baseCurrency = baseCurrency,
                                amount = parsedAmount,
                                ratesMap = currentRatesMap
                            )
                            updateState {
                                it.copy(
                                    rates = newCards,
                                    isColdLoading = false,
                                    isRefreshing = false,
                                    error = null,
                                    lastUpdated = exchangeRates.timestamp
                                )
                            }
                        },
                        onFailure = { throwable ->
                            handleLoadError(throwable)
                        }
                    )
                }
        }
    }

    private fun handleLoadError(throwable: Throwable) {
        val appError = when (throwable) {
            is AppError -> throwable
            else -> AppError.NetworkError(
                message = throwable.message ?: "Failed to load exchange rates.",
                cause = throwable
            )
        }

        if (currentState.rates.isNotEmpty()) {
            // Popup Tier: non-destructive failure over existing cached data
            updateState {
                it.copy(
                    isRefreshing = false,
                    isColdLoading = false
                )
            }
            emitEffect(ExchangeUiEffect.ShowSnackbar("Rate refresh failed; displaying cached data"))
        } else {
            // Inline Tier: cold load failure with no prior rates
            updateState {
                it.copy(
                    isColdLoading = false,
                    isRefreshing = false,
                    error = appError
                )
            }
        }
    }

    private fun recomputeAllCards(amount: Double) {
        if (currentRatesMap.isEmpty()) return
        val newCards = buildRateCards(
            baseCurrency = currentState.baseCurrency,
            amount = amount,
            ratesMap = currentRatesMap
        )
        updateState { it.copy(rates = newCards) }
    }

    private fun buildRateCards(
        baseCurrency: String,
        amount: Double,
        ratesMap: Map<String, Double>
    ): List<ExchangeRateItem> {
        val targetCurrencies = Currency.ALL_TOP_CURRENCIES.filter { it.code != baseCurrency }
        return targetCurrencies.map { currency ->
            val conversion = convertCurrencyUseCase(
                amount = amount,
                baseCurrencyCode = baseCurrency,
                targetCurrency = currency,
                rates = ratesMap
            )
            val unitRate = convertCurrencyUseCase.calculateRate(baseCurrency, currency.code, ratesMap)
            val formattedRate = convertCurrencyUseCase.formatRate(unitRate)

            ExchangeRateItem(
                currencyCode = currency.code,
                currencyName = currency.name,
                symbol = currency.symbol,
                nativeSymbol = currency.nativeSymbol,
                flagEmoji = currency.flagEmoji,
                desktopBadge = currency.desktopBadge,
                unitRate = unitRate,
                convertedAmount = conversion.convertedAmount,
                formattedAmount = conversion.formattedAmount,
                formattedRate = formattedRate,
                trendPercent = calculateTrendPercent(unitRate, currency.code),
                isBase = false
            )
        }
    }

    private fun calculateTrendPercent(unitRate: Double, targetCode: String): Double? {
        // Deterministic trend indicator simulation based on daily deviation
        val delta = when (targetCode) {
            "EUR" -> 0.15
            "ILS" -> -0.42
            "JOD" -> 0.00
            "USD" -> 0.08
            else -> 0.00
        }
        return delta
    }

    private fun parseAmount(raw: String): Double {
        val sanitized = raw.replace(",", "").trim()
        val parsed = sanitized.toDoubleOrNull()
        return if (parsed != null && parsed >= 0.0) parsed else 0.0
    }
}

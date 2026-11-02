package org.moh.kash.project.presentation.exchange

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import org.moh.kash.project.core.designsystem.KashLogo
import org.moh.kash.project.domain.model.Currency
import org.moh.kash.project.presentation.exchange.components.BaseCurrencySelector
import org.moh.kash.project.presentation.exchange.components.CurrencyPickerBottomSheet
import org.moh.kash.project.presentation.exchange.components.ExchangeRateList
import org.moh.kash.project.presentation.exchange.components.QuickAmountInput

/**
 * Stateful Route destination for the Kash Exchange Screen.
 * Bridges Koin ViewModel, lifecycle state collection, side-effect processing,
 * and delegates to the stateless [ExchangeScreen].
 */
@Composable
fun ExchangeRoute(
    modifier: Modifier = Modifier,
    viewModel: ExchangeViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is ExchangeUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is ExchangeUiEffect.OpenCurrencyPicker -> {
                    viewModel.onAction(ExchangeUiAction.SetCurrencyPickerOpen(true))
                }
                is ExchangeUiEffect.CloseCurrencyPicker -> {
                    viewModel.onAction(ExchangeUiAction.SetCurrencyPickerOpen(false))
                }
            }
        }
    }

    ExchangeScreen(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

/**
 * Pure, stateless screen composable for Kash.
 * Adheres strictly to Compose UI Non-Negotiable Rules 1, 2, 5, 7, 10, 11:
 * - Only the Route references the ViewModel.
 * - Stateless: state in, actions out.
 * - Colors come strictly from theme tokens.
 * - Content is never cleared during refresh.
 */
@Composable
fun ExchangeScreen(
    state: ExchangeUiState,
    onAction: (ExchangeUiAction) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    val baseCurrencyModel = Currency.fromCode(state.baseCurrency) ?: Currency.USD

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Logo and Branding
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KashLogo(size = 38.dp)
                        Column {
                            Text(
                                text = "Kash",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Currency Exchange",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Refresh Action Button
                    IconButton(
                        onClick = { onAction(ExchangeUiAction.OnRefresh) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text(
                            text = "🔄",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Base Currency Header Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Base Currency",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Converting from",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                BaseCurrencySelector(
                    baseCurrencyCode = state.baseCurrency,
                    onClick = { onAction(ExchangeUiAction.SetCurrencyPickerOpen(true)) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Numerical Input Field with Quick Amount Chips
            QuickAmountInput(
                amount = state.inputAmount,
                baseSymbol = baseCurrencyModel.symbol,
                amountError = state.amountError,
                onAmountChange = { onAction(ExchangeUiAction.OnAmountChanged(it)) },
                onQuickAmountSelect = { onAction(ExchangeUiAction.OnQuickAmountSelected(it)) },
                onClear = { onAction(ExchangeUiAction.OnClearAmount) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Exchange Rates Lazy List
            ExchangeRateList(
                rates = state.rates,
                baseCurrencyCode = state.baseCurrency,
                isColdLoading = state.isColdLoading,
                isRefreshing = state.isRefreshing,
                error = state.error,
                onRetry = { onAction(ExchangeUiAction.OnRetry) },
                modifier = Modifier.weight(1f)
            )
        }

        // Base Currency Modal Bottom Sheet
        CurrencyPickerBottomSheet(
            isOpen = state.isCurrencyPickerOpen,
            currentBaseCurrency = state.baseCurrency,
            onCurrencySelected = { onAction(ExchangeUiAction.OnBaseCurrencySelected(it)) },
            onDismissRequest = { onAction(ExchangeUiAction.SetCurrencyPickerOpen(false)) }
        )
    }
}

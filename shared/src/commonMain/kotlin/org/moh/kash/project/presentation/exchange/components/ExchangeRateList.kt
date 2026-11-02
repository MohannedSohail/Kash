package org.moh.kash.project.presentation.exchange.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.moh.kash.project.core.error.AppError
import org.moh.kash.project.presentation.exchange.ExchangeRateItem

/**
 * LazyColumn for exchange rate cards.
 * Adheres strictly to:
 * - Rule 7: Never wipes or hides visible content during refresh.
 * - Rule 8: Every lazy list item has a stable key from domain identity plus contentType.
 * - Error Tiers: Inline error on cold failure; Non-destructive indicator on background refresh.
 */
@Composable
fun ExchangeRateList(
    rates: List<ExchangeRateItem>,
    baseCurrencyCode: String,
    isColdLoading: Boolean,
    isRefreshing: Boolean,
    error: AppError?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 24.dp)
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header with Live Status & Refresh indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Live Exchange Rates",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            LivePulseIndicator(
                label = if (isRefreshing) "Refreshing..." else "Live"
            )
        }

        // Non-destructive refresh progress bar
        AnimatedVisibility(
            visible = isRefreshing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // State Machine Resolution
        when {
            // Cold Loading: display skeleton cards
            isColdLoading && rates.isEmpty() -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(3) {
                        ShimmerLoadingCard()
                    }
                }
            }

            // Fatal Cold Error with no rates
            error != null && rates.isEmpty() -> {
                ErrorBanner(
                    error = error,
                    onRetry = onRetry,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Main Rates List (kept rendered during refresh!)
            else -> {
                LazyColumn(
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = rates,
                        key = { it.currencyCode },
                        contentType = { "ExchangeRateCard" }
                    ) { rateItem ->
                        ExchangeRateCard(
                            item = rateItem,
                            baseCurrencyCode = baseCurrencyCode,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

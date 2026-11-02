package org.moh.kash.project.presentation.exchange.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.moh.kash.project.core.designsystem.CardShape
import org.moh.kash.project.core.designsystem.ChipShape
import org.moh.kash.project.core.designsystem.statusColors
import org.moh.kash.project.presentation.exchange.ExchangeRateItem

/**
 * High-fidelity currency exchange rate card.
 * Features:
 * - Target currency flag/symbol badge
 * - Currency code and full name
 * - Numerical converted amount with smooth AnimatedContent sliding counter
 * - Real-time unit rate (e.g. 1 USD = 0.9200 EUR)
 * - Semantic trend indicator pill (Emerald positive, Crimson negative, Slate neutral)
 */
@Composable
fun ExchangeRateCard(
    item: ExchangeRateItem,
    baseCurrencyCode: String,
    modifier: Modifier = Modifier
) {
    val statusColors = MaterialTheme.statusColors

    val trendColor = when {
        item.trendPercent != null && item.trendPercent > 0.0 -> statusColors.trendPositive
        item.trendPercent != null && item.trendPercent < 0.0 -> statusColors.trendNegative
        else -> statusColors.trendNeutral
    }

    val trendText = when {
        item.trendPercent != null && item.trendPercent > 0.0 -> "↑ +${item.trendPercent}%"
        item.trendPercent != null && item.trendPercent < 0.0 -> "↓ ${item.trendPercent}%"
        item.trendPercent != null -> "— 0.00%"
        else -> null
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Top Row: Flag/Code/Name on Left, Trend Pill on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Currency Flag or Badge
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (item.flagEmoji.isNotBlank()) {
                                Text(
                                    text = item.flagEmoji,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            } else {
                                Text(
                                    text = item.desktopBadge,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = item.currencyCode,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.currencyName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Trend Pill
                if (trendText != null) {
                    Surface(
                        shape = ChipShape,
                        color = trendColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = trendText,
                            style = MaterialTheme.typography.labelSmall,
                            color = trendColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Row: Unit Rate on Left, Converted Balance on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Exchange Rate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "1 $baseCurrencyCode = ${item.formattedRate} ${item.currencyCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Animated Converted Amount Display
                AnimatedContent(
                    targetState = item.formattedAmount,
                    transitionSpec = {
                        (slideInVertically { it / 3 } + fadeIn()).togetherWith(
                            slideOutVertically { -it / 3 } + fadeOut()
                        ).using(SizeTransform(clip = false))
                    },
                    label = "ConvertedAmountCounter"
                ) { targetAmountStr ->
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = item.symbol,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = targetAmountStr,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

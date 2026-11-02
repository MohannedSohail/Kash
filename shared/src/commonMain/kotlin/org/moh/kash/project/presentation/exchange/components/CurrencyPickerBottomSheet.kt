package org.moh.kash.project.presentation.exchange.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.moh.kash.project.core.designsystem.CardShape
import org.moh.kash.project.core.designsystem.SheetShape
import org.moh.kash.project.domain.model.Currency

/**
 * Modal bottom sheet presenting the supported top currencies (USD, EUR, ILS, JOD).
 * Features:
 * - Highlight for the actively selected base currency
 * - Flag / Desktop badge display
 * - Checkmark selection indicator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerBottomSheet(
    isOpen: Boolean,
    currentBaseCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = SheetShape,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Select Base Currency",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Text(
                text = "Choose the currency you want to convert from",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Currency.ALL_TOP_CURRENCIES.forEach { currency ->
                    val isSelected = currency.code.equals(currentBaseCurrency, ignoreCase = true)

                    val containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }

                    val border = if (isSelected) {
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CardShape)
                            .clickable(role = Role.RadioButton) {
                                onCurrencySelected(currency.code)
                            },
                        shape = CardShape,
                        color = containerColor,
                        border = border
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (currency.flagEmoji.isNotBlank()) {
                                            Text(
                                                text = currency.flagEmoji,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        } else {
                                            Text(
                                                text = currency.desktopBadge,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }

                                Column {
                                    Text(
                                        text = "${currency.code} — ${currency.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Symbol: ${currency.symbol} (${currency.nativeSymbol})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "✓",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

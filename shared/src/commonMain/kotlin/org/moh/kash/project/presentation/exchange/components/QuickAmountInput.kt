package org.moh.kash.project.presentation.exchange.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.moh.kash.project.core.designsystem.ChipShape
import org.moh.kash.project.core.designsystem.ContainerShape

/**
 * High-clarity numerical currency input container.
 * Features:
 * - Currency prefix symbol badge
 * - Clear button with animated visibility
 * - Quick amount selection chips (10, 50, 100, 1000)
 * - Field-tier error handling (shows inline error without clearing input)
 */
@Composable
fun QuickAmountInput(
    amount: String,
    baseSymbol: String,
    amountError: String?,
    onAmountChange: (String) -> Unit,
    onQuickAmountSelect: (Double) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val quickAmounts = listOf(10.0, 50.0, 100.0, 1000.0)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ContainerShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Amount to convert",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                leadingIcon = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(start = 8.dp).size(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = baseSymbol,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = amount.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button, onClick = onClear)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "✕",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                isError = amountError != null,
                placeholder = {
                    Text(
                        text = "0.00",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() }
                ),
                singleLine = true,
                shape = ChipShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            AnimatedVisibility(visible = amountError != null) {
                if (amountError != null) {
                    Text(
                        text = amountError,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Amount Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickAmounts.forEach { quickVal ->
                    val isSelected = amount.toDoubleOrNull() == quickVal
                    val chipColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                    val textColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(ChipShape)
                            .clickable(role = Role.Button) {
                                onQuickAmountSelect(quickVal)
                            },
                        shape = ChipShape,
                        color = chipColor,
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = quickVal.toInt().toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

package org.moh.kash.project.presentation.exchange.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.moh.kash.project.core.designsystem.CardShape
import org.moh.kash.project.domain.model.Currency

/**
 * Base currency selector button affordance.
 * Displays the active base currency flag, code, and chevron affordance.
 * Tapping triggers [onClick] to open the currency picker bottom sheet.
 */
@Composable
fun BaseCurrencySelector(
    baseCurrencyCode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = Currency.fromCode(baseCurrencyCode) ?: Currency.USD

    Surface(
        modifier = modifier
            .clip(CardShape)
            .clickable(role = Role.Button, onClick = onClick),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Flag or Badge container
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
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
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Column {
                Text(
                    text = currency.code,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = currency.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Chevron Icon
            Text(
                text = "▼",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

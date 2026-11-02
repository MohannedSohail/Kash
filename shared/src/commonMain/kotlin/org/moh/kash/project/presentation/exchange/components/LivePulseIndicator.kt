package org.moh.kash.project.presentation.exchange.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.moh.kash.project.core.designsystem.statusColors

/**
 * Draw-phase pulsing indicator.
 * Strictly adheres to Rule 2 & 3: clock-driven animation values run strictly in the draw phase
 * via graphicsLayer without invalidating enclosing composable scopes.
 */
@Composable
fun LivePulseIndicator(
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val pulseColor = MaterialTheme.statusColors.livePulse

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer { this.alpha = alpha }
                .background(color = pulseColor, shape = CircleShape)
        )
        if (label != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = pulseColor
            )
        }
    }
}

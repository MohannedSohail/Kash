package org.moh.kash.project.presentation.exchange.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.moh.kash.project.core.designsystem.CardShape
import org.moh.kash.project.core.designsystem.ChipShape

/**
 * Cold-load placeholder skeleton card.
 * Strictly adheres to Rule 7 (skeletons appear only during cold load when no prior data exists).
 * Shimmer alpha animation runs purely in the draw phase via graphicsLayer.
 */
@Composable
fun ShimmerLoadingCard(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShimmerAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha },
        shape = CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    )
                    Column {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(16.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, ChipShape)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(12.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, ChipShape)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(54.dp)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, ChipShape)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(16.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, ChipShape)
                )

                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, ChipShape)
                )
            }
        }
    }
}

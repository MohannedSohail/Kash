package org.moh.kash.project.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape scale for Kash:
 * - small: 8dp for quick amount chips and pills
 * - medium: 12dp for exchange rate cards
 * - large: 16dp for input containers
 * - extraLarge: 24dp for bottom sheets
 */
val KashShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

val ChipShape = RoundedCornerShape(8.dp)
val CardShape = RoundedCornerShape(12.dp)
val ContainerShape = RoundedCornerShape(16.dp)
val SheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

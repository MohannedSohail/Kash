package org.moh.kash.project.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Root theme wrapper for Kash.
 * Configures the Material 3 colorScheme, typography, shapes, and custom [KashStatusColors].
 */
@Composable
fun KashTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) KashDarkColorScheme else KashLightColorScheme
    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors

    CompositionLocalProvider(
        LocalStatusColors provides statusColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KashTypography,
            shapes = KashShapes,
            content = content
        )
    }
}

/**
 * Extension property providing access to semantic trend & pulse colors:
 * MaterialTheme.statusColors.trendPositive, etc.
 */
val MaterialTheme.statusColors: KashStatusColors
    @Composable
    @ReadOnlyComposable
    get() = LocalStatusColors.current

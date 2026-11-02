package org.moh.kash.project.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Status and trend semantic color extension for Kash fintech indicators.
 * Used for live pulses, upward/downward rate trends, and neutral movements.
 */
@Immutable
data class KashStatusColors(
    val trendPositive: Color,
    val trendNegative: Color,
    val trendNeutral: Color,
    val livePulse: Color
)

val DarkStatusColors = KashStatusColors(
    trendPositive = Color(0xFF10B981),
    trendNegative = Color(0xFFF43F5E),
    trendNeutral = Color(0xFF94A3B8),
    livePulse = Color(0xFF34D399)
)

val LightStatusColors = KashStatusColors(
    trendPositive = Color(0xFF059669),
    trendNegative = Color(0xFFE11D48),
    trendNeutral = Color(0xFF64748B),
    livePulse = Color(0xFF059669)
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

/**
 * Material 3 Light Color Scheme for Kash (Emerald / Mint / Slate / Amber)
 */
val KashLightColorScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFECFDF5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0D9488),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0FDFA),
    onSecondaryContainer = Color(0xFF134E4A),
    tertiary = Color(0xFFD97706),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFFBEB),
    onTertiaryContainer = Color(0xFF78350F),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFE11D48),
    onError = Color(0xFFFFFFFF)
)

/**
 * Material 3 Dark Color Scheme for Kash (Emerald 400 / Charcoal / Dark Slate / Amber)
 */
val KashDarkColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF022C22),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF0B0F17),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),
    error = Color(0xFFFB7185),
    onError = Color(0xFF4C0519)
)

package org.moh.kash.project.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pure Compose Multiplatform vector implementation of the "Kash" Logo emblem.
 * Renders without external image loading dependencies on Android, Desktop JVM, and iOS.
 *
 * @param size Target dimension for the square icon (defaults to 64.dp)
 * @param modifier Optional modifier applied to the Canvas container
 */
@Composable
fun KashLogo(
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Background Slate Rounded Container
        val bgBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF0F172A), Color(0xFF0B0F17), Color(0xFF020617)),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
        drawRoundRect(
            brush = bgBrush,
            size = Size(w, h),
            cornerRadius = CornerRadius(w * 0.22f, h * 0.22f)
        )

        // Subtle Border Stroke
        drawRoundRect(
            color = Color(0xFF1E293B),
            size = Size(w, h),
            cornerRadius = CornerRadius(w * 0.22f, h * 0.22f),
            style = Stroke(width = w * 0.015f)
        )

        // Subtle Ambient Ring
        drawCircle(
            color = Color(0x3310B981),
            radius = w * 0.38f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = w * 0.008f)
        )

        // Left Vertical Spine (Kash Pillar)
        val emeraldBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFF34D399), Color(0xFF10B981), Color(0xFF059669)),
            startY = h * 0.18f,
            endY = h * 0.82f
        )
        val spineLeft = w * 0.31f
        val spineTop = h * 0.18f
        val spineWidth = w * 0.10f
        val spineHeight = h * 0.64f
        drawRoundRect(
            brush = emeraldBrush,
            topLeft = Offset(spineLeft, spineTop),
            size = Size(spineWidth, spineHeight),
            cornerRadius = CornerRadius(spineWidth * 0.5f, spineWidth * 0.5f)
        )

        // Upper Branch (Diagonal Up Arrow)
        val upperPath = Path().apply {
            moveTo(w * 0.40f, h * 0.48f)
            lineTo(w * 0.62f, h * 0.28f)
            lineTo(w * 0.70f, h * 0.36f)
            lineTo(w * 0.48f, h * 0.56f)
            close()
        }
        drawPath(upperPath, brush = emeraldBrush)

        // Upper Arrowhead Tip
        val upperHead = Path().apply {
            moveTo(w * 0.60f, h * 0.26f)
            lineTo(w * 0.74f, h * 0.29f)
            lineTo(w * 0.68f, h * 0.42f)
            close()
        }
        drawPath(upperHead, color = Color(0xFF34D399))

        // Lower Branch (Diagonal Down Arrow / Counter Loop)
        val tealBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF2DD4BF), Color(0xFF0D9488), Color(0xFF042F2E)),
            start = Offset(w * 0.45f, h * 0.50f),
            end = Offset(w * 0.70f, h * 0.80f)
        )
        val lowerPath = Path().apply {
            moveTo(w * 0.45f, h * 0.52f)
            lineTo(w * 0.62f, h * 0.72f)
            lineTo(w * 0.54f, h * 0.79f)
            lineTo(w * 0.38f, h * 0.59f)
            close()
        }
        drawPath(lowerPath, brush = tealBrush)

        // Lower Arrowhead Tip
        val lowerHead = Path().apply {
            moveTo(w * 0.68f, h * 0.69f)
            lineTo(w * 0.71f, h * 0.83f)
            lineTo(w * 0.58f, h * 0.81f)
            close()
        }
        drawPath(lowerHead, color = Color(0xFF2DD4BF))

        // Amber Intersection Spark (The Currency Pulse Node)
        val amberBrush = Brush.radialGradient(
            colors = listOf(Color(0xFFFDE68A), Color(0xFFF59E0B), Color(0xFFD97706)),
            center = Offset(w * 0.43f, h * 0.50f),
            radius = w * 0.05f
        )
        drawCircle(
            brush = amberBrush,
            radius = w * 0.038f,
            center = Offset(w * 0.43f, h * 0.50f)
        )
        drawCircle(
            color = Color.White,
            radius = w * 0.015f,
            center = Offset(w * 0.43f, h * 0.50f)
        )
    }
}

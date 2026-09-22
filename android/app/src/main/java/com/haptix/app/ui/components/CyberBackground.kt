package com.haptix.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.haptix.app.ui.theme.CyberCyan
import com.haptix.app.ui.theme.CyberThemeMode
import com.haptix.app.ui.theme.CyberViolet
import com.haptix.app.ui.theme.LocalCyberThemeMode

/**
 * Atmospheric laboratory background layer rendering soft ambient radial glow
 * and subtle technical registration marks.
 */
@Composable
fun CyberBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val themeMode = LocalCyberThemeMode.current
    val isDark = themeMode == CyberThemeMode.DARK

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            if (isDark) {
                // Top-Left subtle cyan atmospheric glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CyberCyan.copy(alpha = 0.065f),
                            CyberCyan.copy(alpha = 0.02f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.15f, h * 0.05f),
                        radius = w * 0.9f
                    ),
                    center = Offset(w * 0.15f, h * 0.05f),
                    radius = w * 0.9f
                )

                // Bottom-Right very subtle violet atmospheric depth
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CyberViolet.copy(alpha = 0.045f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.85f, h * 0.85f),
                        radius = w * 0.75f
                    ),
                    center = Offset(w * 0.85f, h * 0.85f),
                    radius = w * 0.75f
                )
            } else {
                // Light mode: soft technical cool blue lighting
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF007AFF).copy(alpha = 0.035f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.2f, h * 0.08f),
                        radius = w * 0.85f
                    ),
                    center = Offset(w * 0.2f, h * 0.08f),
                    radius = w * 0.85f
                )
            }

            // Subtle top horizontal registration guideline
            val guideColor = if (isDark) {
                CyberCyan.copy(alpha = 0.04f)
            } else {
                Color(0xFF007AFF).copy(alpha = 0.05f)
            }
            drawLine(
                color = guideColor,
                start = Offset(24.dp.toPx(), 4.dp.toPx()),
                end = Offset(w - 24.dp.toPx(), 4.dp.toPx()),
                strokeWidth = 1f
            )
        }

        content()
    }
}

package com.haptix.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Clean Canvas play triangle icon.
 */
@Composable
fun CanvasPlayIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.5f)
            lineTo(w * 0.25f, h * 0.85f)
            close()
        }
        drawPath(path, color, style = Fill)
    }
}

/**
 * Clean Canvas pause double-bar icon.
 */
@Composable
fun CanvasPauseIcon(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val barWidth = w * 0.22f
        val gap = w * 0.18f
        val left1 = (w - (barWidth * 2 + gap)) / 2f
        val top = h * 0.2f
        val barHeight = h * 0.6f

        drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(left1, top),
            size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
        )
        drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(left1 + barWidth + gap, top),
            size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
        )
    }
}

/**
 * Clean Canvas volume icon with muted state support.
 */
@Composable
fun CanvasVolumeIcon(
    color: Color,
    isMuted: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Speaker cone
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.38f)
            lineTo(w * 0.35f, h * 0.38f)
            lineTo(w * 0.55f, h * 0.2f)
            lineTo(w * 0.55f, h * 0.8f)
            lineTo(w * 0.35f, h * 0.62f)
            lineTo(w * 0.15f, h * 0.62f)
            close()
        }
        drawPath(path, color, style = Fill)

        if (isMuted) {
            // Diagonal slash for muted state
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.35f),
                end = androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.65f),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.35f),
                end = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.65f),
                strokeWidth = 2.dp.toPx()
            )
        } else {
            // Sound wave arcs
            val arcPath = Path().apply {
                moveTo(w * 0.68f, h * 0.32f)
                quadraticTo(w * 0.82f, h * 0.5f, w * 0.68f, h * 0.68f)
            }
            drawPath(arcPath, color, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

/**
 * Clean Canvas fullscreen expand/compress icon.
 */
@Composable
fun CanvasFullscreenIcon(
    color: Color,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 2.dp.toPx()
        val cornerLen = w * 0.26f

        if (!isFullscreen) {
            // Expand corners
            // Top Left
            drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.15f), androidx.compose.ui.geometry.Offset(w * 0.15f + cornerLen, h * 0.15f), stroke)
            drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.15f), androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.15f + cornerLen), stroke)
            // Top Right
            drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.15f), androidx.compose.ui.geometry.Offset(w * 0.85f - cornerLen, h * 0.15f), stroke)
            drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.15f), androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.15f + cornerLen), stroke)
            // Bottom Left
            drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.85f), androidx.compose.ui.geometry.Offset(w * 0.15f + cornerLen, h * 0.85f), stroke)
            drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.85f), androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.85f - cornerLen), stroke)
            // Bottom Right
            drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.85f), androidx.compose.ui.geometry.Offset(w * 0.85f - cornerLen, h * 0.85f), stroke)
            drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.85f), androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.85f - cornerLen), stroke)
        } else {
            // Compress corners
            val innerOffset = w * 0.28f
            // Top Left
            drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.15f + innerOffset, h * 0.15f + innerOffset), androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.15f + innerOffset), stroke)
            drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.15f + innerOffset, h * 0.15f + innerOffset), androidx.compose.ui.geometry.Offset(w * 0.15f + innerOffset, h * 0.15f), stroke)
            // Bottom Right
            drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.85f - innerOffset, h * 0.85f - innerOffset), androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.85f - innerOffset), stroke)
            drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.85f - innerOffset, h * 0.85f - innerOffset), androidx.compose.ui.geometry.Offset(w * 0.85f - innerOffset, h * 0.85f), stroke)
        }
    }
}

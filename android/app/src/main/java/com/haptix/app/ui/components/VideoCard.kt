package com.haptix.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.data.model.VideoItem
import com.haptix.app.ui.theme.CyberCyan
import com.haptix.app.ui.theme.CyberViolet
import com.haptix.app.ui.theme.LocalSpacing
import com.haptix.app.ui.theme.TechnicalMicroLabel
import com.haptix.app.ui.theme.TechnicalValueLabel
import java.util.Locale

/**
 * Formats duration in milliseconds to MM:SS string representation.
 */
fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

/**
 * Premium experimental stimulus card with large abstract waveform banner,
 * floating duration metadata, and interactive progression CTA.
 */
@Composable
fun VideoCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    val spacing = LocalSpacing.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(120),
        label = "videoCardScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected || isPressed) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        },
        animationSpec = tween(200),
        label = "videoCardBorder"
    )

    // Formatted stimulus code (e.g. "video_01" -> "STIMULUS 01")
    val stimulusTag = if (video.id.startsWith("video_")) {
        "STIMULUS " + video.id.removePrefix("video_")
    } else {
        "STIMULUS // ${video.id.uppercase()}"
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, borderColor),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                this.role = Role.Button
                this.contentDescription = "Select stimulus ${video.title}, duration ${formatDuration(video.durationMs)}"
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Large Visual Thumbnail / Abstract Waveform Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF070A12)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Abstract Frequency / Waveform Graphic
                Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    val w = size.width
                    val h = size.height

                    // Grid-like background lines
                    val lineCount = 6
                    val stepY = h / lineCount
                    for (i in 1 until lineCount) {
                        drawLine(
                            color = CyberCyan.copy(alpha = 0.05f),
                            start = Offset(0f, i * stepY),
                            end = Offset(w, i * stepY),
                            strokeWidth = 1f
                        )
                    }

                    // Sine wave 1 (Cyan frequency stream)
                    val path1 = Path()
                    path1.moveTo(0f, h * 0.5f)
                    val waveLength = w / 3f
                    for (x in 0..w.toInt() step 6) {
                        val y = h * 0.5f + (kotlin.math.sin((x / waveLength) * 2 * Math.PI) * 28f).toFloat()
                        path1.lineTo(x.toFloat(), y)
                    }
                    drawPath(
                        path = path1,
                        color = CyberCyan.copy(alpha = 0.6f),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Sine wave 2 (Violet secondary harmonic)
                    val path2 = Path()
                    path2.moveTo(0f, h * 0.5f)
                    for (x in 0..w.toInt() step 6) {
                        val y = h * 0.5f + (kotlin.math.cos((x / (waveLength * 0.75f)) * 2 * Math.PI) * 20f).toFloat()
                        path2.lineTo(x.toFloat(), y)
                    }
                    drawPath(
                        path = path2,
                        color = CyberViolet.copy(alpha = 0.45f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Floating Center Play Affordance
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CanvasPlayIcon(
                        color = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Top-Left Floating Stimulus Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stimulusTag,
                        style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                        color = CyberCyan
                    )
                }

                // Bottom-Right Floating Duration Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = formatDuration(video.durationMs),
                        style = TechnicalValueLabel.copy(fontSize = 11.sp),
                        color = Color.White
                    )
                }
            }

            // Stimulus Metadata & Interactive Action Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (video.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Text(
                        text = video.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(spacing.sm))

                // Bottom Action CTA Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OPEN STIMULUS →",
                        style = TechnicalMicroLabel.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Small indicator dot
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    )
                }
            }
        }
    }
}

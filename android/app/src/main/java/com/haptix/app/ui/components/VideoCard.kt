package com.haptix.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import com.haptix.app.ui.theme.AppleSystemBlueDark
import com.haptix.app.ui.theme.EditorialMetadataLabel
import com.haptix.app.ui.theme.LocalSpacing
import java.util.Locale

/**
 * Formats duration in milliseconds to MM:SS string representation.
 * Preserved for behavioral assertions in UiLayerTest.
 */
fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

/**
 * Featured large editorial stimulus card resembling Apple Music featured media.
 * Dominant 16:9 thumbnail with rounded 22dp corners, translucent duration pill,
 * bold editorial title, and clean research description.
 */
@Composable
fun FeaturedStimulusCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(120),
        label = "featuredScale"
    )

    val stimulusNumber = if (video.id.startsWith("video_")) {
        video.id.removePrefix("video_")
    } else {
        video.id.takeLast(2)
    }

    Column(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                this.role = Role.Button
                this.contentDescription = "Featured stimulus: ${video.title}, duration ${formatDuration(video.durationMs)}"
            }
    ) {
        // Dominant 16:9 Rich Media Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF151518))
        ) {
            // Calm generative abstract artwork for stimulus
            StimulusArtworkCanvas(stimulusSeed = video.id.hashCode())

            // Gradient scrim for contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.55f)
                            )
                        )
                    )
            )

            // Centered subtle play affordance
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                CanvasPlayIcon(color = Color.White, size = 22.dp)
            }

            // Bottom-Right Translucent Duration Pill Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = formatDuration(video.durationMs),
                    style = EditorialMetadataLabel.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Small uppercase stimulus metadata
        Text(
            text = "STIMULUS $stimulusNumber",
            style = EditorialMetadataLabel.copy(
                letterSpacing = 1.0.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = video.title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        if (video.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = video.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Editorial Media Card for the Stimulus Library list.
 * Media-first layout inspired by modern Apple media browsing.
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

    val stimulusNumber = if (video.id.startsWith("video_")) {
        video.id.removePrefix("video_")
    } else {
        video.id.takeLast(2)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 16:9 Rounded Media Thumbnail
            Box(
                modifier = Modifier
                    .width(112.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF151518))
            ) {
                StimulusArtworkCanvas(stimulusSeed = video.id.hashCode())

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatDuration(video.durationMs),
                        style = EditorialMetadataLabel.copy(fontSize = 10.sp),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Metadata & Title
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "STIMULUS $stimulusNumber",
                    style = EditorialMetadataLabel.copy(
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                if (video.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = video.description,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Minimalist play affordance icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                CanvasPlayIcon(
                    color = MaterialTheme.colorScheme.onSurface,
                    size = 14.dp
                )
            }
        }
    }
}

/**
 * Restrained abstract scientific/media artwork for stimulus thumbnails.
 * Renders smooth organic frequency curves over dark subtle gradient.
 */
@Composable
private fun StimulusArtworkCanvas(
    stimulusSeed: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Subtle gradient backdrop
        val seedFactor = kotlin.math.abs(stimulusSeed % 3)
        val tint1 = when (seedFactor) {
            0 -> Color(0xFF14243A)
            1 -> Color(0xFF241838)
            else -> Color(0xFF142E28)
        }
        val tint2 = Color(0xFF0C0E14)

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(tint1, tint2)
            )
        )

        // Waveform stroke 1
        val path1 = Path()
        path1.moveTo(0f, h * 0.5f)
        val step = 8
        val freqFactor = if (seedFactor == 0) 2.5f else if (seedFactor == 1) 3.5f else 2.0f
        for (x in 0..w.toInt() step step) {
            val y = h * 0.5f + (kotlin.math.sin((x / w) * Math.PI * freqFactor) * (h * 0.28f)).toFloat()
            path1.lineTo(x.toFloat(), y)
        }
        drawPath(
            path = path1,
            color = AppleSystemBlueDark.copy(alpha = 0.5f),
            style = Stroke(width = 2.dp.toPx())
        )

        // Harmonic stroke 2
        val path2 = Path()
        path2.moveTo(0f, h * 0.5f)
        for (x in 0..w.toInt() step step) {
            val y = h * 0.5f + (kotlin.math.cos((x / w) * Math.PI * (freqFactor * 1.5f)) * (h * 0.18f)).toFloat()
            path2.lineTo(x.toFloat(), y)
        }
        drawPath(
            path = path2,
            color = Color.White.copy(alpha = 0.25f),
            style = Stroke(width = 1.2.dp.toPx())
        )
    }
}


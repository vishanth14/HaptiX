package com.haptix.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.haptix.app.R
import com.haptix.app.data.model.VideoItem
import com.haptix.app.domain.model.VideoSource
import com.haptix.app.ui.components.motion.lensing
import com.haptix.app.ui.theme.HaptiXBodyMedium
import com.haptix.app.ui.theme.HaptiXShapeTokens
import com.haptix.app.ui.theme.HaptiXThemeTokens
import com.haptix.app.ui.theme.LocalReducedMotion
import com.haptix.app.ui.theme.MotionTokens
import com.haptix.app.ui.theme.ObsidianBalthazarFontFamily
import com.haptix.app.ui.theme.TechnicalMicroLabel
import com.haptix.app.ui.theme.TechnicalProtocolLabel
import com.haptix.app.ui.theme.rememberUiHaptics
import java.util.Locale
import kotlin.math.abs

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
 * Resolves a clean, consistent research specimen tag avoiding character truncation.
 */
fun resolveSpecimenTag(videoId: String): String {
    return when {
        videoId.contains("f1", ignoreCase = true) -> "SPECIMEN 01"
        videoId.contains("koji", ignoreCase = true) -> "SPECIMEN 02"
        videoId == "video_01" -> "SPECIMEN 03"
        videoId == "video_02" -> "SPECIMEN 04"
        videoId == "video_03" -> "SPECIMEN 05"
        videoId.startsWith("remote_") -> "REMOTE SPECIMEN"
        videoId.startsWith("video_") -> {
            val num = videoId.removePrefix("video_").filter { it.isDigit() }
            if (num.isNotBlank()) "SPECIMEN ${num.padStart(2, '0')}" else "SPECIMEN"
        }
        else -> "SPECIMEN"
    }
}

/**
 * Resolves the drawable resource ID for a video thumbnail.
 */
fun resolveThumbnailDrawable(video: VideoItem): Int? {
    if (video.thumbnailResId != null) return video.thumbnailResId
    val uri = video.thumbnailResUri?.lowercase() ?: ""
    val id = video.id.lowercase()
    return when {
        id.contains("f1") || uri.contains("f1") -> R.drawable.f1_2025_thumbnail
        id.contains("koji") || uri.contains("koji") -> R.drawable.koji_thumbnail
        else -> null
    }
}

/**
 * Featured Stimulus Research Specimen Card.
 *
 * Media-first layout, subtle obsidian borders, precision overlays,
 * tactile frequency profile metadata, and spring bounce.
 */
@Composable
fun HaptiXFeaturedStimulusCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEvaluated: Boolean = false
) {
    val uiHaptics = rememberUiHaptics()
    val reducedMotion = LocalReducedMotion.current
    val colors = HaptiXThemeTokens.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetScale = if (isPressed && !reducedMotion) MotionTokens.PressedScale else MotionTokens.SettledScale
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (isPressed) MotionTokens.Instant else MotionTokens.Interactive,
        label = "featuredScale"
    )

    val specimenTag = resolveSpecimenTag(video.id)
    val hasHaptics = video.hapticConfigResName.isNotBlank() || video.hapticUrl.isNotBlank()

    val borderColor = if (isEvaluated) colors.signalGreen.copy(alpha = 0.5f) else colors.accentPrimary.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .scale(scale)
            .lensing(interactionSource)
            .fillMaxWidth()
            .clip(HaptiXShapeTokens.card)
            .background(colors.surface)
            .border(1.dp, borderColor, HaptiXShapeTokens.card)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = {
                    uiHaptics.tap()
                    onClick()
                }
            )
            .semantics {
                this.role = Role.Button
                this.contentDescription = "Featured stimulus ${video.title}, duration ${if (video.durationMs > 0L) formatDuration(video.durationMs) else "calibrated"}"
            }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Protocol Next Target Callout or Completed Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(HaptiXShapeTokens.micro)
                            .background(if (isEvaluated) colors.signalGreen else colors.accentPrimary)
                    )
                    Text(
                        text = if (isEvaluated) "EVALUATED SPECIMEN ✓" else "PRIMARY EVALUATION TARGET",
                        style = TechnicalMicroLabel.copy(fontWeight = FontWeight.Bold),
                        color = if (isEvaluated) colors.signalGreen else colors.accentPrimary,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = specimenTag,
                    style = TechnicalMicroLabel,
                    color = colors.textTertiary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 16:9 Dominant Media Surface with Precision Overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(HaptiXShapeTokens.media)
                    .background(colors.elevated)
                    .border(0.5.dp, colors.borderSubtle, HaptiXShapeTokens.media)
            ) {
                val thumbnailRes = resolveThumbnailDrawable(video)
                if (thumbnailRes != null) {
                    Image(
                        painter = painterResource(id = thumbnailRes),
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    SpecimenArtworkCanvas(seed = video.id.hashCode())

                    // Gradient Scrim for Contrast
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        colors.background.copy(alpha = 0.85f)
                                    ),
                                    startY = 80f
                                )
                            )
                    )
                }

                // Top-left: Haptic State Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    HaptiXTelemetryBadge(
                        text = if (hasHaptics) "HAPTICS SYNCHRONIZED" else "AUDIOVISUAL ONLY",
                        accent = hasHaptics
                    )
                }

                // Bottom-right: Duration pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    HaptiXTelemetryBadge(
                        text = if (video.durationMs > 0L) formatDuration(video.durationMs) else "CALIBRATED CLIP",
                        accent = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title and description
            Text(
                text = video.title,
                fontFamily = ObsidianBalthazarFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = (-0.2).sp,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (video.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.description,
                    style = HaptiXBodyMedium,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bento Status & Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(HaptiXShapeTokens.input)
                    .background(colors.controlSurface)
                    .border(0.5.dp, colors.borderSubtle, HaptiXShapeTokens.input)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (hasHaptics) colors.accentPrimary else colors.textTertiary)
                    )
                    Text(
                        text = if (hasHaptics) "HAPTICS SYNCHRONIZED" else "AUDIOVISUAL ONLY",
                        style = TechnicalMicroLabel.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (hasHaptics) colors.accentPrimary else colors.textSecondary,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(HaptiXShapeTokens.micro)
                        .background(if (isEvaluated) colors.surface else colors.focusSurface)
                        .border(
                            0.5.dp,
                            if (isEvaluated) colors.signalGreen.copy(alpha = 0.5f) else colors.accentPrimary.copy(alpha = 0.5f),
                            HaptiXShapeTokens.micro
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isEvaluated) "REVIEW →" else "OPEN →",
                        style = TechnicalMicroLabel.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isEvaluated) colors.signalGreen else colors.accentPrimary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Standard Stimulus Specimen Bento Card for the Stimulus Library list.
 */
@Composable
fun HaptiXStimulusSpecimenCard(
    video: VideoItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEvaluated: Boolean = false
) {
    val uiHaptics = rememberUiHaptics()
    val reducedMotion = LocalReducedMotion.current
    val colors = HaptiXThemeTokens.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetScale = if (isPressed && !reducedMotion) MotionTokens.PressedScale else MotionTokens.SettledScale
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (isPressed) MotionTokens.Instant else MotionTokens.Interactive,
        label = "specimenScale"
    )

    val specimenTag = resolveSpecimenTag(video.id)
    val hasHaptics = video.hapticConfigResName.isNotBlank() || video.hapticUrl.isNotBlank()

    val shape = HaptiXShapeTokens.card
    val borderColor = when {
        isSelected -> colors.accentPrimary
        isEvaluated -> colors.signalGreen.copy(alpha = 0.4f)
        else -> colors.borderSubtle
    }

    Box(
        modifier = modifier
            .scale(scale)
            .lensing(interactionSource, isSelected)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = {
                    uiHaptics.tap()
                    onClick()
                }
            )
            .semantics {
                this.role = Role.Button
                this.contentDescription = "$specimenTag: ${video.title}${if (isEvaluated) ", evaluated" else ""}"
            }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 16:9 Thumbnail with Duration Pill
            Box(
                modifier = Modifier
                    .width(116.dp)
                    .aspectRatio(16f / 9f)
                    .clip(HaptiXShapeTokens.input)
                    .background(colors.elevated)
                    .border(0.5.dp, colors.borderSubtle, HaptiXShapeTokens.input)
            ) {
                val thumbnailRes = resolveThumbnailDrawable(video)
                if (thumbnailRes != null) {
                    Image(
                        painter = painterResource(id = thumbnailRes),
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    SpecimenArtworkCanvas(seed = video.id.hashCode())
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = if (video.durationMs > 0L) formatDuration(video.durationMs) else "--:--",
                        style = TechnicalMicroLabel.copy(fontSize = 9.sp),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Metadata column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = specimenTag,
                        style = TechnicalProtocolLabel.copy(fontSize = 11.sp),
                        color = colors.accentPrimary,
                        maxLines = 1
                    )
                    if (isEvaluated) {
                        Text(
                            text = "• EVALUATED ✓",
                            style = TechnicalMicroLabel.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = colors.signalGreen,
                            maxLines = 1
                        )
                    } else if (isSelected) {
                        Text(
                            text = "• ACTIVE",
                            style = TechnicalMicroLabel.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = colors.accentPrimary,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = video.title,
                    fontFamily = ObsidianBalthazarFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = if (hasHaptics) "HAPTICS SYNCHRONIZED" else "AUDIOVISUAL ONLY",
                    style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                    color = if (hasHaptics) colors.accentPrimary.copy(alpha = 0.85f) else colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(HaptiXShapeTokens.micro)
                    .background(if (isEvaluated) colors.surface else colors.focusSurface)
                    .border(
                        0.5.dp,
                        if (isEvaluated) colors.signalGreen.copy(alpha = 0.5f) else colors.accentPrimary.copy(alpha = 0.5f),
                        HaptiXShapeTokens.micro
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isEvaluated) "REVIEW" else "OPEN →",
                    style = TechnicalMicroLabel.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = if (isEvaluated) colors.signalGreen else colors.accentPrimary,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Generative abstract visual specimen background.
 */
@Composable
private fun SpecimenArtworkCanvas(
    seed: Int,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val s = abs(seed)

        val baseColors = when (s % 3) {
            0 -> listOf(Color(0xFF071424), Color(0xFF0A2540), Color(0xFF040A12))
            1 -> listOf(Color(0xFF140D26), Color(0xFF28134D), Color(0xFF090514))
            else -> listOf(Color(0xFF081C1C), Color(0xFF0F3636), Color(0xFF040D0D))
        }

        drawRect(brush = Brush.linearGradient(baseColors))

        // Diagonal research line accents
        val lineCount = 4
        for (i in 0 until lineCount) {
            val offset = (w / lineCount) * i
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(offset, 0f),
                end = Offset(offset + 60f, h),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

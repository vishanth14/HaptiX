package com.haptix.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.ui.components.motion.bounceClick
import com.haptix.app.ui.theme.HaptiXShapeTokens
import com.haptix.app.ui.theme.HaptiXThemeTokens
import com.haptix.app.ui.theme.LocalReducedMotion
import com.haptix.app.ui.theme.MotionTokens
import com.haptix.app.ui.theme.ObsidianBalthazarFontFamily
import com.haptix.app.ui.theme.TechnicalMicroLabel
import com.haptix.app.ui.theme.TechnicalReadoutValue
import com.haptix.app.ui.theme.rememberUiHaptics

/**
 * Standardized Research Evaluation Questionnaire Bento Unit.
 *
 * Each question is a distinct evaluation unit with structured header,
 * question prompt, discrete 1-5 tactile rating selector with soft depth,
 * and calibrated anchor labels.
 */
@Composable
fun FeedbackQuestionCard(
    questionNumber: Int,
    questionText: String,
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    lowAnchorLabel: String = "1 — Strongly Disagree",
    highAnchorLabel: String = "5 — Strongly Agree"
) {
    val colors = HaptiXThemeTokens.colors
    val uiHaptics = rememberUiHaptics()
    val isAnswered = rating > 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HaptiXShapeTokens.card)
            .background(colors.surface)
            .border(
                width = 1.dp,
                color = if (isAnswered) colors.accentPrimary.copy(alpha = 0.35f) else colors.borderSubtle,
                shape = HaptiXShapeTokens.card
            )
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Protocol question marker & score readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dimensionName = when (questionNumber) {
                    1 -> "VISUAL QUALITY"
                    2 -> "IMMERSION"
                    3 -> "AUDIO-HAPTIC SYNCHRONY"
                    4 -> "TACTILE REALISM"
                    5 -> "OVERALL SATISFACTION"
                    else -> "DIMENSION 0$questionNumber"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isAnswered) colors.accentPrimary else colors.textTertiary)
                    )
                    Text(
                        text = "0$questionNumber — $dimensionName",
                        style = TechnicalMicroLabel,
                        color = if (isAnswered) colors.accentPrimary else colors.textSecondary
                    )
                }

                if (isAnswered) {
                    Text(
                        text = "SCORE: 0$rating",
                        style = TechnicalReadoutValue.copy(fontSize = 12.sp),
                        color = colors.accentPrimary
                    )
                } else {
                    Text(
                        text = "REQUIRED",
                        style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                        color = colors.textTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Question prompt
            Text(
                text = questionText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Discrete 1 — 2 — 3 — 4 — 5 Tactile Rating Selector
            TactileRatingSelector(
                currentRating = rating,
                onRatingSelected = { selected ->
                    uiHaptics.selection()
                    onRatingChanged(selected)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Low / High Anchor Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = lowAnchorLabel,
                    style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                    color = colors.textTertiary
                )
                Text(
                    text = highAnchorLabel,
                    style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                    color = colors.textTertiary
                )
            }
        }
    }
}

/**
 * 5-Segment discrete tactile rating selector with soft depth and high-contrast active state.
 */
@Composable
private fun TactileRatingSelector(
    currentRating: Int,
    onRatingSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HaptiXThemeTokens.colors
    val reducedMotion = LocalReducedMotion.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (1..5).forEach { score ->
            val isSelected = currentRating == score
            val targetScale = if (isSelected && !reducedMotion) 1.04f else 1.0f
            val scale by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = MotionTokens.Interactive,
                label = "ratingScale_$score"
            )

            val animatedBg by animateColorAsState(
                targetValue = if (isSelected) colors.accentPrimary else colors.controlSurface,
                animationSpec = MotionTokens.colorSpring(),
                label = "ratingBg_$score"
            )

            val animatedBorder by animateColorAsState(
                targetValue = if (isSelected) colors.accentPrimary else colors.borderSubtle,
                animationSpec = MotionTokens.colorSpring(),
                label = "ratingBorder_$score"
            )

            val animatedText by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF050608) else colors.textSecondary,
                animationSpec = MotionTokens.colorSpring(),
                label = "ratingText_$score"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .scale(scale)
                    .clip(HaptiXShapeTokens.input)
                    .background(animatedBg)
                    .border(1.dp, animatedBorder, HaptiXShapeTokens.input)
                    .bounceClick(
                        role = Role.RadioButton,
                        onClick = { onRatingSelected(score) }
                    )
                    .semantics {
                        this.role = Role.RadioButton
                        this.selected = isSelected
                        this.contentDescription = "Rating $score of 5"
                    }
            ) {
                Text(
                    text = "$score",
                    fontFamily = ObsidianBalthazarFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = animatedText
                )
            }
        }
    }
}

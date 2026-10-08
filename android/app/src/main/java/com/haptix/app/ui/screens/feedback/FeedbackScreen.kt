package com.haptix.app.ui.screens.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haptix.app.data.model.FeedbackResponse
import com.haptix.app.ui.components.FeedbackQuestionCard
import com.haptix.app.ui.components.HaptiXBackground
import com.haptix.app.ui.components.HaptiXPrimaryButton
import com.haptix.app.ui.components.HaptiXProtocolEyebrow
import com.haptix.app.ui.components.motion.elasticOverscroll
import com.haptix.app.ui.components.motion.rememberWiggleState
import com.haptix.app.ui.components.motion.wiggle
import com.haptix.app.ui.theme.HaptiXBodyLarge
import com.haptix.app.ui.theme.HaptiXShapeTokens
import com.haptix.app.ui.theme.HaptiXThemeTokens
import com.haptix.app.ui.theme.ObsidianBalthazarFontFamily
import com.haptix.app.ui.theme.TechnicalMicroLabel
import com.haptix.app.ui.theme.TechnicalReadoutValue
import com.haptix.app.ui.theme.rememberUiHaptics

/**
 * Screen 5: Post-Stimulus Perceptual Evaluation.
 *
 * Minimal progress tracking, 5 standardized Likert questions with cyan/white
 * precision rating glyphs, qualitative input field, and localized questionnaire
 * wiggle on incomplete submission.
 */
@Composable
fun FeedbackScreen(
    videoId: String,
    viewModel: FeedbackViewModel = viewModel(key = videoId),
    participantId: String = "",
    isHapticsEnabled: Boolean = true,
    onFeedbackSubmitted: (List<FeedbackResponse>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiHaptics = rememberUiHaptics()
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val wiggleState = rememberWiggleState()
    val colors = HaptiXThemeTokens.colors

    LaunchedEffect(videoId) {
        viewModel.setVideoId(videoId)
    }

    var answeredQuestions by remember(videoId) { mutableStateOf(setOf<Int>()) }
    val answeredCount = answeredQuestions.size
    val progressFraction = answeredCount.toFloat() / STUDY_QUESTIONS.size.toFloat()

    HaptiXBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .elasticOverscroll()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(32.dp))

                // Eyebrow Protocol Tag
                HaptiXProtocolEyebrow(text = "03 // PERCEPTUAL EVALUATION")

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Perceptual Evaluation",
                    fontFamily = ObsidianBalthazarFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    letterSpacing = (-0.4).sp,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Evaluate your perceptual experience of the audiovisual and tactile stimulus across 5 standardized dimensions.",
                    style = HaptiXBodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Minimal Progress Tracker Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(HaptiXShapeTokens.card)
                        .background(colors.surface)
                        .border(1.dp, colors.borderSubtle, HaptiXShapeTokens.card)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EVALUATION PROTOCOL PROGRESS",
                                style = TechnicalMicroLabel,
                                color = colors.textTertiary
                            )
                            Text(
                                text = "0$answeredCount // 0${STUDY_QUESTIONS.size}",
                                style = TechnicalReadoutValue,
                                color = if (answeredCount == STUDY_QUESTIONS.size) colors.signalGreen else colors.accentPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(HaptiXShapeTokens.micro),
                            color = colors.accentPrimary,
                            trackColor = colors.borderSubtle
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Standardized 5 Likert Questions with localized Wiggle
                Box(modifier = Modifier.fillMaxWidth().wiggle(wiggleState)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        STUDY_QUESTIONS.forEach { question ->
                            val rating = if (answeredQuestions.contains(question.number)) {
                                viewModel.getRatingForQuestion(question.number)
                            } else {
                                0
                            }

                            FeedbackQuestionCard(
                                questionNumber = question.number,
                                questionText = question.text,
                                rating = rating,
                                onRatingChanged = { newRating ->
                                    answeredQuestions = answeredQuestions + question.number
                                    viewModel.updateRating(question.number, newRating)
                                },
                                lowAnchorLabel = question.lowAnchor,
                                highAnchorLabel = question.highAnchor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Submission Button CTA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                val isReadyToSubmit = answeredCount == STUDY_QUESTIONS.size && viewModel.areAllQuestionsAnswered() && !uiState.isSubmitted && !uiState.isSubmitting
                HaptiXPrimaryButton(
                    text = when {
                        uiState.isSubmitted -> "EVALUATION SUBMITTED ✓"
                        uiState.isSubmitting -> "SUBMITTING EVALUATION..."
                        isReadyToSubmit -> "SUBMIT EVALUATION  →"
                        else -> "COMPLETE ALL QUESTIONS (0$answeredCount // 0${STUDY_QUESTIONS.size})"
                    },
                    enabled = isReadyToSubmit,
                    onClick = {
                        if (!isReadyToSubmit) {
                            wiggleState.trigger()
                            uiHaptics.warning()
                        } else {
                            uiHaptics.confirm()
                            viewModel.submitFeedback(
                                participantId = participantId,
                                hapticEnabled = isHapticsEnabled,
                                onSubmitted = { responses ->
                                    onFeedbackSubmitted(responses)
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}

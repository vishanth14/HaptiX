package com.haptix.app.ui.screens.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haptix.app.data.model.FeedbackResponse
import com.haptix.app.ui.components.CyberBackground
import com.haptix.app.ui.components.FeedbackQuestionCard
import com.haptix.app.ui.components.HaptiXPrimaryButton
import com.haptix.app.ui.components.HaptiXTextField
import com.haptix.app.ui.theme.CyberCyan
import com.haptix.app.ui.theme.LocalSpacing
import com.haptix.app.ui.theme.TechnicalMicroLabel

/**
 * Screen 5: Post-Playback Experimental Questionnaire.
 * Editorial layout with 22dp evaluation cards, progress bar indicator, and 5 standardized Likert items.
 */
@Composable
fun FeedbackScreen(
    videoId: String,
    viewModel: FeedbackViewModel = viewModel(),
    participantId: String = "",
    isHapticsEnabled: Boolean = true,
    onFeedbackSubmitted: (List<FeedbackResponse>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(videoId) {
        viewModel.setVideoId(videoId)
    }

    val answeredCount = STUDY_QUESTIONS.count { viewModel.getRatingForQuestion(it.number) > 0 }
    val progressFraction = answeredCount.toFloat() / STUDY_QUESTIONS.size.toFloat()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        CyberBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.lg)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(spacing.md))

                    // Editorial Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "04 // POST-PLAYBACK EVALUATION",
                            style = TechnicalMicroLabel.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "04 / 04",
                                style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.xs))

                    Text(
                        text = "Subjective Evaluation",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "Please evaluate the stimulus presentation across the 5 dimensions below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(spacing.md))

                    // Progress indicator row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PROGRESS: $answeredCount OF ${STUDY_QUESTIONS.size} QUESTIONS EVALUATED",
                            style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                            color = CyberCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.xxs))

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = CyberCyan,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(spacing.lg))

                    // The 5 Fixed Study Questions in 22dp Cards
                    STUDY_QUESTIONS.forEach { question ->
                        val rating = viewModel.getRatingForQuestion(question.number)

                        FeedbackQuestionCard(
                            questionNumber = question.number,
                            questionText = question.text,
                            rating = rating,
                            onRatingChanged = { newRating ->
                                viewModel.updateRating(question.number, newRating)
                            },
                            lowAnchorLabel = question.lowAnchor,
                            highAnchorLabel = question.highAnchor
                        )

                        Spacer(modifier = Modifier.height(spacing.md))
                    }

                    // Optional commentary
                    HaptiXTextField(
                        value = uiState.comments,
                        onValueChange = { viewModel.updateComments(it) },
                        label = "QUALITATIVE OBSERVATIONS",
                        placeholder = "Enter any observations regarding tactile sensations or timing...",
                        singleLine = false
                    )

                    Spacer(modifier = Modifier.height(spacing.xl))
                }

                // Submission Action
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.xl)
                ) {
                    HaptiXPrimaryButton(
                        text = "SUBMIT EVALUATION  →",
                        enabled = answeredCount == STUDY_QUESTIONS.size,
                        onClick = {
                            viewModel.submitFeedback(
                                participantId = participantId,
                                hapticEnabled = isHapticsEnabled,
                                onSubmitted = { responses ->
                                    onFeedbackSubmitted(responses)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

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
import com.haptix.app.ui.components.FeedbackQuestionCard
import com.haptix.app.ui.components.HaptiXBackground
import com.haptix.app.ui.components.HaptiXPrimaryButton
import com.haptix.app.ui.components.HaptiXTextField
import com.haptix.app.ui.theme.EditorialMetadataLabel
import com.haptix.app.ui.theme.LocalSpacing

/**
 * Screen 5: Post-Playback Experimental Questionnaire.
 * Apple-style review and rating interface with large 5-star controls,
 * clean typography, restrained separators, and spacious layout.
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
        HaptiXBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = spacing.screenHorizontal),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(spacing.md))

                    // Header
                    Text(
                        text = "03",
                        style = EditorialMetadataLabel.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Evaluate the Stimulus",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "How did the stimulus feel?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(spacing.md))

                    // Progress indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$answeredCount of ${STUDY_QUESTIONS.size} completed",
                            style = EditorialMetadataLabel.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(spacing.sectionGap))

                    // Standardized Likert Questions
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

                        Spacer(modifier = Modifier.height(spacing.sectionGap))
                    }

                    // Optional qualitative notes
                    HaptiXTextField(
                        value = uiState.comments,
                        onValueChange = { viewModel.updateComments(it) },
                        label = "QUALITATIVE OBSERVATIONS",
                        placeholder = "Enter any observations regarding tactile sensations or timing...",
                        singleLine = false
                    )

                    Spacer(modifier = Modifier.height(spacing.largeSectionGap))
                }

                // Submission CTA
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

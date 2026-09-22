package com.haptix.app.ui.screens.profile

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haptix.app.data.model.Participant
import com.haptix.app.ui.components.CyberBackground
import com.haptix.app.ui.components.HaptiXChoiceGroup
import com.haptix.app.ui.components.HaptiXDropdown
import com.haptix.app.ui.components.HaptiXPrimaryButton
import com.haptix.app.ui.components.HaptiXTextField
import com.haptix.app.ui.components.LikertScaleSelector
import com.haptix.app.ui.theme.LocalSpacing
import com.haptix.app.ui.theme.TechnicalMicroLabel

private val GENDER_OPTIONS = listOf("Female", "Male", "Non-binary", "Prefer not to say")
private val GENRE_OPTIONS = listOf(
    "Action",
    "Drama",
    "Sci-Fi",
    "Documentary",
    "Animation",
    "Thriller",
    "Comedy",
    "Other"
)

/**
 * Screen 2: Participant Profile Questionnaire.
 * Editorial layout with 20dp grouped sections, comfortable controls, and clear progress indication.
 */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onNavigateToVideoList: (Participant) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

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
                            text = "01 // PARTICIPANT INTAKE",
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
                                text = "01 / 04",
                                style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.xs))

                    Text(
                        text = "Participant Profile",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "Demographic baseline calibration for tactile correlation analysis.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(spacing.lg))

                    // Grouped Section 1: Demographics Card (20dp rounded)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.lg)
                        ) {
                            Text(
                                text = "[ DEMOGRAPHIC BASELINE ]",
                                style = TechnicalMicroLabel,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(spacing.md))

                            // Participant ID
                            HaptiXTextField(
                                value = uiState.participantId,
                                onValueChange = { viewModel.updateParticipantId(it) },
                                label = "Participant Identifier",
                                helperText = "Generated study code for response correlation"
                            )

                            Spacer(modifier = Modifier.height(spacing.lg))

                            // Gender Selection
                            HaptiXChoiceGroup(
                                options = GENDER_OPTIONS,
                                selectedOption = uiState.gender,
                                onOptionSelected = { viewModel.updateGender(it) },
                                label = "Gender Identification"
                            )

                            Spacer(modifier = Modifier.height(spacing.lg))

                            // Age Input
                            HaptiXTextField(
                                value = uiState.ageText,
                                onValueChange = { viewModel.updateAge(it) },
                                label = "Age in Years",
                                placeholder = "e.g. 25",
                                errorMessage = uiState.ageError,
                                helperText = if (uiState.ageError == null) "Must be between 18 and 100" else null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.md))

                    // Grouped Section 2: Media Preferences Card (20dp rounded)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.lg)
                        ) {
                            Text(
                                text = "[ MEDIA PREFERENCES ]",
                                style = TechnicalMicroLabel,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(spacing.md))

                            // Movie Interest Rating (1 to 5)
                            LikertScaleSelector(
                                rating = uiState.movieInterestRating,
                                onRatingSelected = { viewModel.updateMovieInterest(it) },
                                label = "General Film & Media Affinity",
                                minLabel = "01 (Low Affinity)",
                                maxLabel = "05 (High Affinity)"
                            )

                            Spacer(modifier = Modifier.height(spacing.lg))

                            // Favorite Genre Dropdown
                            HaptiXDropdown(
                                options = GENRE_OPTIONS,
                                selectedOption = uiState.favoriteGenre,
                                onOptionSelected = { viewModel.updateFavoriteGenre(it) },
                                label = "Primary Genre Preference",
                                placeholder = "Select preferred genre..."
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.xl))
                }

                // Progression Action
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.xl)
                ) {
                    HaptiXPrimaryButton(
                        text = "CONTINUE TO STIMULUS LIBRARY  →",
                        enabled = uiState.isValid,
                        onClick = {
                            viewModel.saveParticipant { participant ->
                                onNavigateToVideoList(participant)
                            }
                        }
                    )
                }
            }
        }
    }
}

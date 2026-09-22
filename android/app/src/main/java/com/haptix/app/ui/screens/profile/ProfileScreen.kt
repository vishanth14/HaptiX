package com.haptix.app.ui.screens.profile

import androidx.compose.foundation.background
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
import com.haptix.app.ui.components.HaptiXBackground
import com.haptix.app.ui.components.HaptiXChoiceGroup
import com.haptix.app.ui.components.HaptiXDropdown
import com.haptix.app.ui.components.HaptiXPrimaryButton
import com.haptix.app.ui.components.HaptiXTextField
import com.haptix.app.ui.components.StarRating
import com.haptix.app.ui.theme.EditorialMetadataLabel
import com.haptix.app.ui.theme.LocalSpacing

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
 * Clean editorial form inspired by native iOS settings and onboarding screens.
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
        HaptiXBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.screenHorizontal)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(28.dp))

                    // Step Eyebrow
                    Text(
                        text = "01",
                        style = EditorialMetadataLabel.copy(
                            fontSize = 13.sp,
                            letterSpacing = 1.0.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Participant Profile",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.4).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Tell us a little about you.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Grouped iOS-style Onboarding Form Container
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp)
                        ) {
                            // Field 1: Gender Selection
                            HaptiXChoiceGroup(
                                options = GENDER_OPTIONS,
                                selectedOption = uiState.gender,
                                onOptionSelected = { viewModel.updateGender(it) },
                                label = "Gender"
                            )

                            Spacer(modifier = Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            // Field 2: Age Input
                            HaptiXTextField(
                                value = uiState.ageText,
                                onValueChange = { viewModel.updateAge(it) },
                                label = "Age",
                                placeholder = "e.g. 25",
                                errorMessage = uiState.ageError,
                                helperText = if (uiState.ageError == null) "Must be between 18 and 100" else null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            Spacer(modifier = Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            // Field 3: Movie Interest Rating (5 Stars)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Movie Interest",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (uiState.movieInterestRating > 0) {
                                        Text(
                                            text = "${uiState.movieInterestRating} of 5",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                StarRating(
                                    rating = uiState.movieInterestRating,
                                    onRatingChanged = { viewModel.updateMovieInterest(it) },
                                    modifier = Modifier.fillMaxWidth(),
                                    starSize = 34.dp,
                                    touchTargetSize = 50.dp
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            // Field 4: Favorite Genre Dropdown
                            HaptiXDropdown(
                                options = GENRE_OPTIONS,
                                selectedOption = uiState.favoriteGenre,
                                onOptionSelected = { viewModel.updateFavoriteGenre(it) },
                                label = "Favorite Genre",
                                placeholder = "Select favorite genre..."
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))
                }

                // Progression Action: Apple-style CONTINUE filled button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    HaptiXPrimaryButton(
                        text = "CONTINUE  →",
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


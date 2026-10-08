package com.haptix.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haptix.app.data.model.Participant
import com.haptix.app.ui.components.HaptiXBackground
import com.haptix.app.ui.components.HaptiXChoiceGroup
import com.haptix.app.ui.components.HaptiXDropdown
import com.haptix.app.ui.components.HaptiXInput
import com.haptix.app.ui.components.HaptiXPrimaryButton
import com.haptix.app.ui.components.HaptiXProtocolEyebrow
import com.haptix.app.ui.components.HaptiXRating
import com.haptix.app.ui.components.motion.elasticOverscroll
import com.haptix.app.ui.components.motion.rememberWiggleState
import com.haptix.app.ui.components.motion.wiggle
import com.haptix.app.ui.theme.HaptiXBodyLarge
import com.haptix.app.ui.theme.HaptiXShapeTokens
import com.haptix.app.ui.theme.HaptiXThemeTokens
import com.haptix.app.ui.theme.ObsidianBalthazarFontFamily
import com.haptix.app.ui.theme.TechnicalMicroLabel
import com.haptix.app.ui.theme.rememberUiHaptics

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
 * Screen 2: Participant Intake Console.
 *
 * Open futuristic layout with hairline dividers (no monolithic gray card).
 * Dark demographic chips, refined numeric input, precision cyan rating glyphs,
 * and localized spring wiggle on validation error.
 */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onNavigateToVideoList: (Participant) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiHaptics = rememberUiHaptics()
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val wiggleState = rememberWiggleState()
    val colors = HaptiXThemeTokens.colors

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

                // Eyebrow Protocol Tag & Subject Identifier
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HaptiXProtocolEyebrow(text = "01 // PARTICIPANT SETUP")

                    Text(
                        text = "ID: ${uiState.participantId}",
                        style = TechnicalMicroLabel.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.accentPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Heading
                Text(
                    text = "Participant Profile",
                    fontFamily = ObsidianBalthazarFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    letterSpacing = (-0.4).sp,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Calibrate study baseline with your demographic and media background.",
                    style = HaptiXBodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Structured Section 1: Demographics Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(HaptiXShapeTokens.card)
                        .background(colors.surface)
                        .border(1.dp, colors.borderSubtle, HaptiXShapeTokens.card)
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "DEMOGRAPHIC BASELINE",
                            style = TechnicalMicroLabel,
                            color = colors.accentPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Gender Selection Chips
                        HaptiXChoiceGroup(
                            options = GENDER_OPTIONS,
                            selectedOption = uiState.gender,
                            onOptionSelected = { viewModel.updateGender(it) },
                            label = "GENDER IDENTITY"
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Age input with localized wiggle
                        Box(modifier = Modifier.fillMaxWidth().wiggle(wiggleState)) {
                            HaptiXInput(
                                value = uiState.ageText,
                                onValueChange = { viewModel.updateAge(it) },
                                label = "AGE (YEARS)",
                                placeholder = "e.g. 24",
                                errorMessage = uiState.ageError,
                                helperText = if (uiState.ageError == null) "Eligible range: 18 to 100 years" else null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Structured Section 2: Media Preference Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(HaptiXShapeTokens.card)
                        .background(colors.surface)
                        .border(1.dp, colors.borderSubtle, HaptiXShapeTokens.card)
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "MEDIA CONSUMPTION PROFILE",
                            style = TechnicalMicroLabel,
                            color = colors.accentPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Movie Interest Rating
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CINEMA & VIDEO INTEREST",
                                    style = TechnicalMicroLabel,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = "SCORE: 0${uiState.movieInterestRating} / 05",
                                    style = TechnicalMicroLabel.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colors.accentPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Discrete 1 to 5 tactile segmented selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                (1..5).forEach { score ->
                                    val isSelected = uiState.movieInterestRating == score
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(HaptiXShapeTokens.input)
                                            .background(if (isSelected) colors.accentPrimary else colors.controlSurface)
                                            .border(
                                                1.dp,
                                                if (isSelected) colors.accentPrimary else colors.borderSubtle,
                                                HaptiXShapeTokens.input
                                            )
                                            .clickable {
                                                uiHaptics.selection()
                                                viewModel.updateMovieInterest(score)
                                            }
                                    ) {
                                        Text(
                                            text = "$score",
                                            fontFamily = ObsidianBalthazarFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = if (isSelected) Color(0xFF050608) else colors.textSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "1 — Rare / Casual",
                                    style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                                    color = colors.textTertiary
                                )
                                Text(
                                    text = "5 — Avid Cinephile",
                                    style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                                    color = colors.textTertiary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Favorite Genre Dropdown
                        HaptiXDropdown(
                            options = GENRE_OPTIONS,
                            selectedOption = uiState.favoriteGenre,
                            onOptionSelected = { viewModel.updateFavoriteGenre(it) },
                            label = "PRIMARY GENRE PREFERENCE",
                            placeholder = "SELECT PRIMARY GENRE"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Bottom Continue CTA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
            ) {
                HaptiXPrimaryButton(
                    text = "CONTINUE TO STIMULI  →",
                    onClick = {
                        if (!uiState.isValid) {
                            wiggleState.trigger()
                            uiHaptics.warning()
                        } else {
                            uiHaptics.confirm()
                            viewModel.saveParticipant { participant ->
                                onNavigateToVideoList(participant)
                            }
                        }
                    }
                )
            }
        }
    }
}

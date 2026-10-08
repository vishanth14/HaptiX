package com.haptix.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * StarRating component for post-stimulus experimental questionnaires.
 * Directly delegates to [HaptiXRating].
 */
@Composable
fun StarRating(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: Dp = 26.dp,
    touchTargetSize: Dp = 48.dp,
    enabled: Boolean = true,
    activeColor: Color = Color.Unspecified,
    inactiveColor: Color = Color.Unspecified
) {
    HaptiXRating(
        rating = rating,
        onRatingChanged = onRatingChanged,
        modifier = modifier,
        maxScore = maxStars,
        glyphSize = starSize,
        touchTargetSize = touchTargetSize,
        enabled = enabled
    )
}

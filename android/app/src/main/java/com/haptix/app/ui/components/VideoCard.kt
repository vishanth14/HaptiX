package com.haptix.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.haptix.app.data.model.VideoItem

/**
 * Featured stimulus card delegating to [HaptiXFeaturedStimulusCard].
 */
@Composable
fun FeaturedStimulusCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HaptiXFeaturedStimulusCard(
        video = video,
        onClick = onClick,
        modifier = modifier
    )
}

/**
 * Standard stimulus card delegating to [HaptiXStimulusSpecimenCard].
 */
@Composable
fun VideoCard(
    video: VideoItem,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HaptiXStimulusSpecimenCard(
        video = video,
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier
    )
}

package com.haptix.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.haptix.app.haptics.HapticCapabilityLevel

/**
 * Haptic status and toggle component for video player.
 * Delegates directly to [HaptiXTactileInstrument].
 */
@Composable
fun HapticStatusToggle(
    isHapticsEnabled: Boolean,
    isHapticSupported: Boolean,
    capabilityLevel: HapticCapabilityLevel,
    isPlaying: Boolean,
    onToggleChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    currentFrequencyHz: Float? = null,
    currentAmplitude: Float? = null
) {
    HaptiXTactileInstrument(
        isHapticsEnabled = isHapticsEnabled,
        isHapticSupported = isHapticSupported,
        capabilityLevel = capabilityLevel,
        isPlaying = isPlaying,
        currentFrequencyHz = currentFrequencyHz,
        currentAmplitude = currentAmplitude,
        onToggleChanged = onToggleChanged,
        modifier = modifier
    )
}

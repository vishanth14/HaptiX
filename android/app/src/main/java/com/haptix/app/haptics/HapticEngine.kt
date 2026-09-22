package com.haptix.app.haptics

import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticPattern

/**
 * Contract defining high-level haptic actuation engine responsibilities.
 *
 * Provides lifecycle management and pattern coordination decoupled from specific
 * platform implementations (e.g. Android Vibrator vs. external actuators).
 */
interface HapticEngine {
    /**
     * Checks whether the host device hardware supports advanced vibration/amplitude control.
     */
    fun isHapticSupported(): Boolean

    /**
     * Prepares and loads a [HapticPattern] for playback.
     */
    fun loadPattern(pattern: HapticPattern)

    /**
     * Starts or resumes haptic playback of the loaded pattern.
     */
    fun start()

    /**
     * Immediately pauses/cancels active vibration and playback.
     */
    fun stop()

    /**
     * Releases system vibration resources and clears loaded patterns.
     */
    fun release()

    /**
     * Triggers a single discrete haptic event directly.
     */
    fun playEvent(event: HapticEvent)
}

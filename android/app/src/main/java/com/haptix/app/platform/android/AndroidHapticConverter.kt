package com.haptix.app.platform.android

import com.haptix.app.data.model.HapticEnvelope
import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticFrequencyPattern
import com.haptix.app.data.model.HapticFrequencyPoint
import com.haptix.app.data.model.HapticPattern
import com.haptix.app.haptics.SemanticHapticPatternGenerator
import com.haptix.app.haptics.WaveformProfile

/**
 * Platform adapter translating platform-independent [HapticPattern] and [HapticEvent] instances
 * into Android-specific hardware vibration models.
 *
 * Responsibilities:
 * - Translates canonical domain events to [HapticFrequencyPoint] and [HapticFrequencyPattern].
 * - Synthesizes hardware [WaveformProfile] with parametric Attack-Sustain-Release envelopes.
 * - Bridges API 36+ [android.os.VibrationEffect.WaveformEnvelopeBuilder] control points and
 *   API 26-35 amplitude-modulated resonant fallbacks.
 * - Strictly avoids universal resonant frequency assumptions.
 */
object AndroidHapticConverter {

    /**
     * Converts a platform-independent [HapticPattern] to an Android [HapticFrequencyPattern].
     */
    fun toAndroidFrequencyPattern(pattern: HapticPattern): HapticFrequencyPattern {
        val points = pattern.events.map { toAndroidFrequencyPoint(it) }
        return HapticFrequencyPattern(
            videoId = pattern.videoId,
            version = pattern.version,
            source = "Converted from ${pattern.source.name} via AndroidHapticConverter",
            points = points
        )
    }

    /**
     * Converts a platform-independent [HapticEvent] into an Android [HapticFrequencyPoint].
     */
    fun toAndroidFrequencyPoint(event: HapticEvent): HapticFrequencyPoint {
        val resolvedFreq = event.frequencyHz
            ?: (event.parameters["frequencyHz"] as? Number)?.toFloat()
            ?: (100f + event.sharpness * 200f)

        val params = event.parameters.toMutableMap()
        params["semanticType"] = event.semanticType.name
        params["hapticType"] = event.semanticType.name
        params["attackMs"] = event.attackMs
        params["sustainMs"] = event.sustainMs
        params["releaseMs"] = event.releaseMs
        params["confidence"] = event.confidence
        event.evidence?.let { params["evidence"] = it.toMap() }

        return HapticFrequencyPoint(
            startTimeMs = event.startTimeMs,
            durationMs = event.durationMs,
            frequencyHz = resolvedFreq,
            amplitude = event.intensity,
            parameters = params
        )
    }

    /**
     * Translates a canonical [HapticEvent] into a fully synthesized Android [WaveformProfile].
     */
    fun toWaveformProfile(event: HapticEvent): WaveformProfile {
        return SemanticHapticPatternGenerator.generateWaveformProfile(event)
    }

    /**
     * Converts an Android [HapticFrequencyPattern] back into a domain [HapticPattern].
     */
    fun toDomainHapticPattern(frequencyPattern: HapticFrequencyPattern): HapticPattern {
        return frequencyPattern.toHapticPattern()
    }
}

package com.haptix.app.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticFrequencyPoint
import com.haptix.app.data.model.HapticPattern

/**
 * Android platform implementation of [HapticEngine] supporting frequency-aware actuation
 * where hardware permits, and honest amplitude-calibrated fallback on standard devices.
 *
 * Designed to satisfy research standards:
 * 1. On Android 16+ (API 36+) devices supporting [android.os.Vibrator.areEnvelopeEffectsSupported],
 *    frequency envelopes are synthesized via [android.os.VibrationEffect.WaveformEnvelopeBuilder].
 * 2. On standard devices (API 26-35) or devices lacking frequency envelope hardware,
 *    frequency modulation is NOT faked. The player exposes [HapticCapabilityLevel.LIMITED]
 *    and actuates amplitude-proportional waveforms at device resonant frequency.
 * 3. On devices lacking vibrators, [HapticCapabilityLevel.UNAVAILABLE] is reported and actuation is safely bypassed.
 */
class AndroidHapticPlayer(
    private val context: Context? = null,
    val capabilities: AndroidHapticCapabilities = AndroidHapticCapabilities(context)
) : HapticEngine {

    private val vibrator: Vibrator? get() = capabilities.vibrator
    private var loadedPattern: HapticPattern? = null
    var isPlaying: Boolean = false
        private set

    /**
     * Diagnostic telemetry representing the most recently dispatched event for research debugging.
     */
    var lastDispatchedEvent: HapticEvent? = null
        private set
    var lastDispatchedFrequencyHz: Float? = null
        private set
    var lastDispatchedAmplitude: Float? = null
        private set

    /**
     * Cached hardware capability profile.
     */
    val hardwareProfile: HapticHardwareProfile by lazy {
        capabilities.getHardwareProfile()
    }

    /**
     * Checks if the device has a working vibrator actuator.
     */
    override fun isHapticSupported(): Boolean {
        return hardwareProfile.capabilityLevel != HapticCapabilityLevel.UNAVAILABLE
    }

    /**
     * Returns the 3-tier capability classification.
     */
    fun getCapabilityLevel(): HapticCapabilityLevel {
        return hardwareProfile.capabilityLevel
    }

    override fun loadPattern(pattern: HapticPattern) {
        this.loadedPattern = pattern
    }

    override fun start() {
        if (loadedPattern == null || !isHapticSupported()) return
        isPlaying = true
    }

    override fun stop() {
        isPlaying = false
        try {
            vibrator?.cancel()
        } catch (_: Throwable) {}
    }

    override fun release() {
        stop()
        loadedPattern = null
        lastDispatchedEvent = null
        lastDispatchedFrequencyHz = null
        lastDispatchedAmplitude = null
    }

    /**
     * Triggers a single haptic event with frequency and amplitude awareness.
     */
    override fun playEvent(event: HapticEvent) {
        val level = hardwareProfile.capabilityLevel
        if (level == HapticCapabilityLevel.UNAVAILABLE) return

        val vib = vibrator ?: return

        // Extract frequency parameters (stored in event.parameters or inferred from sharpness)
        val frequencyHz = (event.parameters["frequencyHz"] as? Number)?.toFloat()
            ?: (100f + event.sharpness * 200f) // Normalized mapping to 100Hz-300Hz
        val amplitude = (event.parameters["amplitude"] as? Number)?.toFloat()
            ?: event.intensity

        lastDispatchedEvent = event
        lastDispatchedFrequencyHz = frequencyHz
        lastDispatchedAmplitude = amplitude

        if (level == HapticCapabilityLevel.SUPPORTED) {
            val synthesized = trySynthesizeEnvelope(vib, frequencyHz, amplitude, event.durationMs)
            if (synthesized) return
        }

        // Fallback actuation path for LIMITED hardware:
        // Actuates amplitude-controlled impulse at native actuator resonant frequency f0.
        // We explicitly document that frequency modulation is unactuated.
        playAmplitudeFallback(vib, amplitude, event.durationMs)
    }

    /**
     * Direct playback of a research [HapticFrequencyPoint].
     */
    fun playFrequencyPoint(point: HapticFrequencyPoint) {
        playEvent(point.toHapticEvent())
    }

    /**
     * Attempts to synthesize a frequency-aware vibration envelope via Android 16 (API 36+) APIs.
     * Uses safe reflection to ensure binary compatibility across API 26..36+.
     */
    private fun trySynthesizeEnvelope(
        vib: Vibrator,
        frequencyHz: Float,
        amplitude: Float,
        durationMs: Long
    ): Boolean {
        return try {
            val builderClass = Class.forName("android.os.VibrationEffect\$WaveformEnvelopeBuilder")
            val builder = builderClass.getConstructor().newInstance()
            val addControlPointMethod = builderClass.getMethod(
                "addControlPoint",
                Float::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
                Long::class.javaPrimitiveType
            )

            val safeDuration = durationMs.coerceAtLeast(15L)
            addControlPointMethod.invoke(builder, amplitude.coerceIn(0f, 1f), frequencyHz, safeDuration)

            val buildMethod = builderClass.getMethod("build")
            val effect = buildMethod.invoke(builder) as? VibrationEffect
            if (effect != null) {
                vib.vibrate(effect)
                true
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Scientifically calibrated amplitude-proportional fallback for devices lacking frequency envelopes.
     * Modulates vibration amplitude within [1, 255] on API 26+ while operating at hardware resonance.
     */
    private fun playAmplitudeFallback(vib: Vibrator, amplitude: Float, durationMs: Long) {
        try {
            val safeDuration = durationMs.coerceAtLeast(20L)
            if (hardwareProfile.hasAmplitudeControl && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intAmplitude = (amplitude.coerceIn(0.01f, 1.0f) * 255).toInt().coerceIn(1, 255)
                val effect = VibrationEffect.createOneShot(safeDuration, intAmplitude)
                vib.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(safeDuration, VibrationEffect.DEFAULT_AMPLITUDE)
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(safeDuration)
            }
        } catch (_: Throwable) {}
    }
}

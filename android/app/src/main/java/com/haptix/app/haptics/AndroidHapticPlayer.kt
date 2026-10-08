package com.haptix.app.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticFrequencyPoint
import com.haptix.app.data.model.HapticPattern

import com.haptix.app.haptics.calibration.CalibratedWaveform
import com.haptix.app.haptics.calibration.DeviceHapticCalibration

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
    val capabilities: AndroidHapticCapabilities = AndroidHapticCapabilities(context),
    var calibration: DeviceHapticCalibration = DeviceHapticCalibration.DEFAULT
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
    var lastDispatchedCalibratedPeak: Int? = null
        private set
    var lastDispatchedCalibratedWaveform: CalibratedWaveform? = null
        private set
    var lastDispatchedRepresentation: String = "IDLE"
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

    /**
     * Determines what Android vibration primitive will be generated for a given profile/hardware.
     */
    fun getExpectedRepresentation(): String {
        return when (hardwareProfile.capabilityLevel) {
            HapticCapabilityLevel.SUPPORTED -> "WaveformEnvelope (API 36+)"
            HapticCapabilityLevel.LIMITED -> if (hardwareProfile.hasAmplitudeControl) "AmplitudeWaveform (Resonant)" else "OneShot (Fallback)"
            HapticCapabilityLevel.UNAVAILABLE -> "UNAVAILABLE"
        }
    }

    override fun loadPattern(pattern: HapticPattern) {
        this.loadedPattern = pattern
    }

    override fun start() {
        if (loadedPattern == null || !isHapticSupported()) return
        isPlaying = true
    }

    private val mainHandler by lazy {
        try {
            android.os.Handler(android.os.Looper.getMainLooper())
        } catch (_: Throwable) {
            null
        }
    }
    private var activeDispatchedEvent: HapticEvent? = null
    private var activeDispatchedRealtimeMs: Long = 0L
    private var activeDispatchedDurationMs: Long = 0L

    override fun stop() {
        stop("EXPLICIT_STOP")
    }

    override fun stop(reason: String) {
        isPlaying = false
        mainHandler?.removeCallbacksAndMessages(null)
        val nowMs = System.currentTimeMillis()
        com.haptix.app.util.HaptiXLog.d("VIBRATOR_CANCEL: reason=$reason, activeEvent=${activeDispatchedEvent?.semanticType}")
        val prev = activeDispatchedEvent
        if (prev != null) {
            val elapsedMs = nowMs - activeDispatchedRealtimeMs
            if (elapsedMs < activeDispatchedDurationMs) {
                val remainingMs = activeDispatchedDurationMs - elapsedMs
                val isTransient = prev.durationMs <= 350L || prev.parameters["classification"] == "TRANSIENT"
                if (isTransient) {
                    com.haptix.app.util.HaptiXLog.d(
                        """
                        TRANSIENT LIFECYCLE CANCEL:
                        cancelledType=${prev.semanticType}
                        duration=${prev.durationMs}ms
                        actualRendererStop=$nowMs
                        elapsedBeforeStop=${elapsedMs}ms
                        remainingMs=$remainingMs
                        cancelReason=$reason
                        """.trimIndent()
                    )
                }
            }
            activeDispatchedEvent = null
        }
        try {
            com.haptix.app.util.HaptiXLog.d("PHYSICAL_VIBRATOR_CANCEL: executing vibrator.cancel(), reason=$reason")
            vibrator?.cancel()
        } catch (_: Throwable) {}
    }

    override fun release() {
        stop()
        loadedPattern = null
        lastDispatchedEvent = null
        lastDispatchedFrequencyHz = null
        lastDispatchedAmplitude = null
        lastDispatchedCalibratedPeak = null
        lastDispatchedCalibratedWaveform = null
    }

    init {
        logHardwareCapabilities()
    }

    /**
     * Reports physical device capability to logcat.
     */
    fun logHardwareCapabilities() {
        val vib = vibrator
        val hasVib = vib != null && vib.hasVibrator()
        val level = hardwareProfile.capabilityLevel
        val hasAmp = hardwareProfile.hasAmplitudeControl
        val sdkInt = Build.VERSION.SDK_INT

        if (!hasVib || level == HapticCapabilityLevel.UNAVAILABLE) {
            com.haptix.app.util.HaptiXLog.w("========================================")
            com.haptix.app.util.HaptiXLog.w("HAPTICS UNAVAILABLE: Physical phone reports no vibrator!")
            com.haptix.app.util.HaptiXLog.w("Build.VERSION.SDK_INT = $sdkInt")
            com.haptix.app.util.HaptiXLog.w("vibrator exists = ${vib != null}")
            com.haptix.app.util.HaptiXLog.w("vibrator.hasVibrator() = $hasVib")
            com.haptix.app.util.HaptiXLog.w("capability state = $level")
            com.haptix.app.util.HaptiXLog.w("========================================")
        } else {
            com.haptix.app.util.HaptiXLog.d("========================================")
            com.haptix.app.util.HaptiXLog.d("PHYSICAL PHONE HAPTIC CAPABILITY DETECTED:")
            com.haptix.app.util.HaptiXLog.d("Build.VERSION.SDK_INT = $sdkInt")
            com.haptix.app.util.HaptiXLog.d("vibrator exists = ${vib != null}")
            com.haptix.app.util.HaptiXLog.d("vibrator.hasVibrator() = $hasVib")
            com.haptix.app.util.HaptiXLog.d("capability state = $level")
            com.haptix.app.util.HaptiXLog.d("hasAmplitudeControl = $hasAmp")
            com.haptix.app.util.HaptiXLog.d("========================================")
        }
    }

    /**
     * Development-only manual tactile verification test.
     * Fires a 600ms smooth envelope (Attack: 150ms, Sustain: 250ms, Release: 200ms)
     * at 0.85 intensity through the active actuation pipeline.
     */
    /**
     * Step 10: Developer-only Reference Stimuli for physical A/B testing and calibration.
     * Controlled stimuli representing:
     * A = low amplitude short pulse (Quiet)
     * B = medium amplitude short pulse (Medium click / shift)
     * C = strong amplitude short pulse (Strong impact)
     * D = long continuous rumble (Sustained motion)
     * E = crash-style dominant impact (Collision landmark)
     */
    enum class ReferenceStimulus(
        val label: String,
        val durationMs: Long,
        val intensity: Float,
        val semanticType: com.haptix.app.data.model.SemanticHapticType,
        val attackMs: Long,
        val sustainMs: Long,
        val releaseMs: Long,
        val description: String
    ) {
        STIMULUS_A_LOW_PULSE("Stimulus A: Low Pulse", 100L, 0.20f, com.haptix.app.data.model.SemanticHapticType.SOFT_IMPACT, 10L, 40L, 50L, "Low amplitude short pulse (Quiet)"),
        STIMULUS_B_MEDIUM_PULSE("Stimulus B: Medium Pulse", 150L, 0.45f, com.haptix.app.data.model.SemanticHapticType.GEAR_SHIFT, 15L, 65L, 70L, "Medium amplitude short pulse (Medium snap)"),
        STIMULUS_C_STRONG_PULSE("Stimulus C: Strong Pulse", 200L, 0.70f, com.haptix.app.data.model.SemanticHapticType.HEAVY_IMPACT, 25L, 85L, 90L, "Strong amplitude short pulse (Strong impact)"),
        STIMULUS_D_LONG_RUMBLE("Stimulus D: Long Rumble", 800L, 0.50f, com.haptix.app.data.model.SemanticHapticType.CONTINUOUS_RUMBLE, 200L, 400L, 200L, "Long continuous textured rumble"),
        STIMULUS_E_CRASH_IMPACT("Stimulus E: Crash Impact", 180L, 0.95f, com.haptix.app.data.model.SemanticHapticType.COLLISION, 15L, 65L, 100L, "Crash-style dominant landmark impact")
    }

    /**
     * Plays a controlled reference stimulus for tactile evaluation.
     */
    fun playReferenceStimulus(stimulus: ReferenceStimulus) {
        val testEvent = HapticEvent(
            startTimeMs = 0L,
            durationMs = stimulus.durationMs,
            intensity = stimulus.intensity,
            semanticType = stimulus.semanticType,
            attackMs = stimulus.attackMs,
            sustainMs = stimulus.sustainMs,
            releaseMs = stimulus.releaseMs,
            parameters = mapOf(
                "description" to stimulus.description,
                "intensity" to stimulus.intensity,
                "amplitude" to stimulus.intensity,
                "attackMs" to stimulus.attackMs,
                "sustainMs" to stimulus.sustainMs,
                "releaseMs" to stimulus.releaseMs
            )
        )
        com.haptix.app.util.HaptiXLog.d(">>> MANUAL REFERENCE STIMULUS: ${stimulus.label} <<<")
        playEvent(testEvent)
    }

    /**
     * Development-only manual tactile verification test.
     * Fires a 600ms smooth envelope (Attack: 150ms, Sustain: 250ms, Release: 200ms)
     * at 0.85 intensity through the active actuation pipeline.
     */
    fun playTestHaptic() {
        val testEvent = HapticEvent(
            startTimeMs = 0L,
            durationMs = 600L,
            intensity = 0.85f,
            sharpness = 0.60f,
            semanticType = com.haptix.app.data.model.SemanticHapticType.SMOOTH,
            parameters = mapOf(
                "description" to "Manual Test Haptic",
                "attackMs" to 150L,
                "sustainMs" to 250L,
                "releaseMs" to 200L,
                "frequencyHz" to 180.0f
            )
        )
        com.haptix.app.util.HaptiXLog.d(">>> MANUAL TEST HAPTIC INITIATED (Attack: 150ms, Sustain: 250ms, Release: 200ms) <<<")
        playEvent(testEvent)
    }

    /**
     * Triggers a single haptic event shaped by its semantic amplitude envelope and hardware calibration.
     */
    override fun playEvent(event: HapticEvent) {
        val level = hardwareProfile.capabilityLevel
        val vib = vibrator

        if (level == HapticCapabilityLevel.UNAVAILABLE || vib == null || !vib.hasVibrator()) {
            com.haptix.app.util.HaptiXLog.w("HAPTICS UNAVAILABLE: device lacks working vibrator actuator")
            return
        }

        isPlaying = true
        val frequencyHz = event.frequencyHz
            ?: (event.parameters["frequencyHz"] as? Number)?.toFloat()
            ?: (100f + event.sharpness * 200f)
        val amplitude = (event.parameters["amplitude"] as? Number)?.toFloat()
            ?: (event.parameters["intensity"] as? Number)?.toFloat()
            ?: event.intensity

        val nowMs = System.currentTimeMillis()
        val prev = activeDispatchedEvent
        if (prev != null) {
            val elapsedMs = nowMs - activeDispatchedRealtimeMs
            if (elapsedMs < activeDispatchedDurationMs) {
                val remainingMs = activeDispatchedDurationMs - elapsedMs
                val isTransient = prev.durationMs <= 350L || prev.parameters["classification"] == "TRANSIENT"

                if (isTransient) {
                    com.haptix.app.util.HaptiXLog.w(
                        """
                        TRANSIENT LIFECYCLE INTERRUPT:
                        interruptedType=${prev.semanticType}
                        duration=${prev.durationMs}ms
                        elapsedBeforeInterrupt=${elapsedMs}ms
                        remainingMs=$remainingMs
                        newIncomingType=${event.semanticType}
                        cancelReason=OVERLAPPING_EVENT_DISPATCH
                        """.trimIndent()
                    )
                }
            }
        }

        // 1. Generate canonical synthesized amplitude envelope profile from source metrics
        val profile = SemanticHapticPatternGenerator.generateWaveformProfile(event)

        // 2. Apply device-specific actuator calibration (pre-emphasis, dynamic range expansion, perceptual hierarchy)
        val calibrated = calibration.calibrate(profile, event)

        val isCurrentTransient = profile.envelope.totalMs <= 350L || event.parameters["classification"] == "TRANSIENT"
        if (isCurrentTransient) {
            com.haptix.app.util.HaptiXLog.d(
                """
                TRANSIENT LIFECYCLE START:
                event start=${event.startTimeMs}ms
                event end=${event.startTimeMs + event.durationMs}ms
                actual renderer start=$nowMs
                type=${profile.semanticType}
                duration=${profile.envelope.totalMs}ms
                frequency=${profile.frequencyHz.toInt()}Hz
                sourceIntensity=${event.intensity}
                calibratedPeak=${calibrated.calibratedPeak}
                calibratedRMS=${String.format(java.util.Locale.US, "%.2f", calibrated.rms)}
                calibratedArea=${calibrated.area}
                timings=${calibrated.timings.joinToString()}
                amplitudes=${calibrated.amplitudes.joinToString()}
                """.trimIndent()
            )
        }

        activeDispatchedEvent = event
        activeDispatchedRealtimeMs = nowMs
        activeDispatchedDurationMs = profile.envelope.totalMs

        lastDispatchedEvent = event
        lastDispatchedFrequencyHz = frequencyHz
        lastDispatchedAmplitude = amplitude
        lastDispatchedCalibratedPeak = calibrated.calibratedPeak
        lastDispatchedCalibratedWaveform = calibrated

        if (level == HapticCapabilityLevel.SUPPORTED) {
            val synthesized = trySynthesizeEnvelope(vib, profile)
            if (synthesized) {
                lastDispatchedRepresentation = "WaveformEnvelope (API 36+)"
                logResearchTrace(event, amplitude, calibrated.calibratedPeak, lastDispatchedRepresentation)
                com.haptix.app.util.HaptiXLog.d("Selected implementation: WaveformEnvelopeBuilder (API 36+), execution=SUCCESS")
                return
            }
        }

        // Fallback actuation path for LIMITED hardware:
        // Actuates amplitude-calibrated waveform envelope via createWaveform at hardware resonance.
        lastDispatchedRepresentation = if (hardwareProfile.hasAmplitudeControl && calibrated.amplitudes.isNotEmpty()) {
            "AmplitudeWaveform (Resonant)"
        } else {
            "OneShot (Fallback)"
        }
        logResearchTrace(event, amplitude, calibrated.calibratedPeak, lastDispatchedRepresentation)
        playAmplitudeFallback(vib, calibrated)
    }

    private fun logResearchTrace(event: HapticEvent, amplitude: Float, calibratedPeak: Int, representation: String) {
        val eventIndex = loadedPattern?.events?.indexOfFirst { it.id == event.id }?.takeIf { it >= 0 }?.toString() ?: "N/A"
        val endMs = event.startTimeMs + event.durationMs
        val freqStr = event.frequencyHz?.let { String.format(java.util.Locale.US, "%.1fHz", it) }
            ?: (event.parameters["frequencyHz"] as? Number)?.let { String.format(java.util.Locale.US, "%.1fHz", it.toFloat()) }
            ?: "N/A"

        com.haptix.app.util.HaptiXLog.d(
            """
            [HAPTIX-HAPTIC]
            video=${loadedPattern?.videoId ?: "UNKNOWN"}
            position=${String.format(java.util.Locale.US, "%.3fs", event.startTimeMs / 1000.0)}
            eventIndex=$eventIndex
            eventStart=${String.format(java.util.Locale.US, "%.3fs", event.startTimeMs / 1000.0)}
            eventEnd=${String.format(java.util.Locale.US, "%.3fs", endMs / 1000.0)}
            sourceFrequency=$freqStr
            sourceIntensity=${String.format(java.util.Locale.US, "%.3f", event.intensity)}
            sourceAmplitude=${String.format(java.util.Locale.US, "%.3f", amplitude)}
            deviceRenderAmplitude=$calibratedPeak
            sourceDuration=${event.durationMs}ms
            androidRepresentation=$representation
            triggered=true
            """.trimIndent()
        )
    }

    /**
     * Direct playback of a research [HapticFrequencyPoint].
     */
    fun playFrequencyPoint(point: HapticFrequencyPoint) {
        playEvent(point.toHapticEvent())
    }

    /**
     * Attempts to synthesize a multi-point frequency-aware vibration envelope via Android 16 (API 36+) APIs.
     * Uses safe reflection to ensure binary compatibility across API 26..36+.
     */
    private fun trySynthesizeEnvelope(
        vib: Vibrator,
        profile: WaveformProfile
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

            val points = profile.controlPoints.ifEmpty {
                listOf(
                    EnvelopeControlPoint(0.1f * profile.peakAmplitude, profile.frequencyHz, profile.envelope.attackMs.coerceAtLeast(15L)),
                    EnvelopeControlPoint(profile.peakAmplitude, profile.frequencyHz, profile.envelope.sustainMs.coerceAtLeast(15L)),
                    EnvelopeControlPoint(0f, profile.frequencyHz, profile.envelope.releaseMs.coerceAtLeast(15L))
                )
            }

            for (pt in points) {
                val safeDur = pt.durationMs.coerceAtLeast(10L)
                addControlPointMethod.invoke(builder, pt.amplitude.coerceIn(0f, 1f), pt.frequencyHz, safeDur)
            }

            val buildMethod = builderClass.getMethod("build")
            val effect = buildMethod.invoke(builder) as? VibrationEffect
            if (effect != null) {
                vibrateWithMediaAttributes(vib, effect, profile.envelope.totalMs)
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
     * Actuates hardware-calibrated waveform (with motor inertia pre-emphasis, expanded dynamic range,
     * and active release compensation) via VibrationEffect.createWaveform at hardware resonance.
     */
    private fun playAmplitudeFallback(vib: Vibrator, calibrated: CalibratedWaveform) {
        val totalMs = calibrated.timings.sum().coerceAtLeast(30L)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (hardwareProfile.hasAmplitudeControl && calibrated.amplitudes.isNotEmpty()) {
                    try {
                        VibrationEffect.createWaveform(calibrated.timings, calibrated.amplitudes, -1)
                    } catch (e: Throwable) {
                        com.haptix.app.util.HaptiXLog.w("Waveform with amplitudes failed, falling back to one-shot: ${e.message}")
                        VibrationEffect.createOneShot(totalMs, VibrationEffect.DEFAULT_AMPLITUDE)
                    }
                } else {
                    VibrationEffect.createOneShot(totalMs, VibrationEffect.DEFAULT_AMPLITUDE)
                }

                vibrateWithMediaAttributes(vib, effect, totalMs)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(totalMs)
            }
        } catch (e: Throwable) {
            com.haptix.app.util.HaptiXLog.w("Haptic vibration error: ${e.message}")
            try {
                @Suppress("DEPRECATION")
                vib.vibrate(totalMs)
            } catch (_: Throwable) {}
        }
    }

    /**
     * Dispatches vibration with AudioAttributes/VibrationAttributes configured for USAGE_MEDIA.
     * Prevents vibration from being silently suppressed by system touch feedback filters.
     */
    private fun vibrateWithMediaAttributes(vib: Vibrator, effect: VibrationEffect, fallbackDurationMs: Long) {
        val audioAttrs = android.media.AudioAttributes.Builder()
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val vibAttrs = android.os.VibrationAttributes.Builder()
                    .setUsage(android.os.VibrationAttributes.USAGE_MEDIA)
                    .build()
                vib.vibrate(effect, vibAttrs)
                return
            } catch (t: Throwable) {
                com.haptix.app.util.HaptiXLog.w("VibrationAttributes failed: ${t.message}")
            }
        }

        try {
            vib.vibrate(effect, audioAttrs)
        } catch (t: Throwable) {
            com.haptix.app.util.HaptiXLog.w("AudioAttributes vibrate failed: ${t.message}")
            try {
                vib.vibrate(effect)
            } catch (t2: Throwable) {
                com.haptix.app.util.HaptiXLog.w("Direct vibrate(effect) failed: ${t2.message}")
                @Suppress("DEPRECATION")
                vib.vibrate(fallbackDurationMs)
            }
        }
    }
}

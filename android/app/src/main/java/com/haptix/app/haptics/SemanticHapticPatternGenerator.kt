package com.haptix.app.haptics

import com.haptix.app.data.model.HapticEnvelope
import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.SemanticHapticType

/**
 * Control point descriptor for Android 16 (API 36+) [android.os.VibrationEffect.WaveformEnvelopeBuilder].
 */
data class EnvelopeControlPoint(
    val amplitude: Float,
    val frequencyHz: Float,
    val durationMs: Long
)

/**
 * Executable waveform profile synthesizing parametric amplitude envelopes
 * into Android-compatible timing and amplitude arrays.
 */
data class WaveformProfile(
    val semanticType: SemanticHapticType,
    val envelope: HapticEnvelope,
    val peakAmplitude: Float,
    val frequencyHz: Float,
    val timings: LongArray,
    val amplitudes: IntArray,
    val controlPoints: List<EnvelopeControlPoint> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WaveformProfile) return false
        if (semanticType != other.semanticType) return false
        if (envelope != other.envelope) return false
        if (peakAmplitude != other.peakAmplitude) return false
        if (frequencyHz != other.frequencyHz) return false
        if (!timings.contentEquals(other.timings)) return false
        if (!amplitudes.contentEquals(other.amplitudes)) return false
        return controlPoints == other.controlPoints
    }

    override fun hashCode(): Int {
        var result = semanticType.hashCode()
        result = 31 * result + envelope.hashCode()
        result = 31 * result + peakAmplitude.hashCode()
        result = 31 * result + frequencyHz.hashCode()
        result = 31 * result + timings.contentHashCode()
        result = 31 * result + amplitudes.contentHashCode()
        result = 31 * result + controlPoints.hashCode()
        return result
    }
}

/**
 * Centralized generator translating abstract [HapticEvent] instances into smooth,
 * natural-feeling tactile envelopes (Attack -> Sustain -> Release).
 *
 * Design Rule:
 * Eliminates sudden vibration spikes (0 -> max -> 0). Every pattern is shaped through
 * a controlled ramp-up (fade-in), an intentional body (sustain), and a natural decay (fade-out).
 */
object SemanticHapticPatternGenerator {

    private const val DEFAULT_STEP_MS = 25L

    /**
     * Resolves the [SemanticHapticType] associated with a given [HapticEvent].
     */
    fun resolveSemanticType(event: HapticEvent): SemanticHapticType {
        if (event.semanticType != SemanticHapticType.SMOOTH) {
            return event.semanticType
        }

        val explicitType = (event.parameters["semanticType"] as? String)
            ?: (event.parameters["hapticType"] as? String)
            ?: (event.parameters["type"] as? String)
        if (explicitType != null) {
            return SemanticHapticType.fromString(explicitType)
        }

        // Infer from description if available
        val desc = event.parameters["description"] as? String
        if (desc != null) {
            return SemanticHapticType.fromString(desc)
        }

        return SemanticHapticType.SMOOTH
    }

    /**
     * Resolves or synthesizes an appropriate [HapticEnvelope] for the given event and semantic type.
     */
    fun resolveEnvelope(event: HapticEvent, semanticType: SemanticHapticType): HapticEnvelope {
        // 1. Check for explicit envelope in parameters or event.envelope
        val env = event.envelope
        if (env.attackMs > 0L || env.sustainMs > 0L || env.releaseMs > 0L) {
            env.validateForDuration(event.durationMs)
            return env
        }

        val explicitAttack = (event.parameters["attackMs"] as? Number)?.toLong()
        val explicitSustain = (event.parameters["sustainMs"] as? Number)?.toLong()
        val explicitRelease = (event.parameters["releaseMs"] as? Number)?.toLong()

        if (explicitAttack != null && explicitSustain != null && explicitRelease != null) {
            val explicitEnv = HapticEnvelope(explicitAttack, explicitSustain, explicitRelease)
            explicitEnv.validateForDuration(event.durationMs)
            return explicitEnv
        }

        // 2. Synthesize canonical envelope for the semantic type based on total duration
        return createDefaultEnvelope(semanticType, event.durationMs)
    }

    /**
     * Mathematical curve interpolation functions for envelope attack, sustain, and release phases.
     */
    fun applyCurve(curveName: String?, t: Float): Float {
        val clampedT = t.coerceIn(0f, 1f)
        val name = curveName?.trim()?.uppercase() ?: "EASE_IN_OUT"
        return when {
            name.contains("LINEAR") -> clampedT
            name.contains("SMOOTHSTEP") -> clampedT * clampedT * (3f - 2f * clampedT)
            name.contains("EASE_IN_OUT") -> 0.5f * (1f - kotlin.math.cos(kotlin.math.PI * clampedT).toFloat())
            name.contains("EASE_IN") -> clampedT * clampedT
            name.contains("EASE_OUT") -> 1f - (1f - clampedT) * (1f - clampedT)
            name.contains("EXPONENTIAL") -> if (clampedT <= 0f) 0f else java.lang.Math.pow(2.0, 10.0 * (clampedT.toDouble() - 1.0)).toFloat()
            name.contains("LOGARITHMIC") -> (kotlin.math.ln(1.0 + 9.0 * clampedT.toDouble()) / kotlin.math.ln(10.0)).toFloat()
            else -> 0.5f * (1f - kotlin.math.cos(kotlin.math.PI * clampedT).toFloat())
        }
    }

    /**
     * Creates a canonical default envelope tailored to the specific semantic sensation.
     */
    fun createDefaultEnvelope(semanticType: SemanticHapticType, durationMs: Long): HapticEnvelope {
        val safeDur = durationMs.coerceAtLeast(30L)
        return when (semanticType) {
            SemanticHapticType.SMOOTH, SemanticHapticType.CONTINUOUS_RUMBLE -> {
                val attack = (safeDur * 0.25f).toLong().coerceAtLeast(10L)
                val release = (safeDur * 0.25f).toLong().coerceAtLeast(10L)
                val sustain = (safeDur - attack - release).coerceAtLeast(0L)
                HapticEnvelope(attack, sustain, release, "SMOOTHSTEP", "SMOOTHSTEP")
            }
            SemanticHapticType.SMOOTH_BUILD, SemanticHapticType.TENSION_BUILD, SemanticHapticType.TENSION -> {
                val attack = (safeDur * 0.60f).toLong().coerceAtLeast(20L)
                val sustain = (safeDur * 0.25f).toLong().coerceAtLeast(10L)
                val release = (safeDur - attack - sustain).coerceAtLeast(10L)
                HapticEnvelope(attack, sustain, release, "SMOOTHSTEP", "EASE_OUT")
            }
            SemanticHapticType.SMOOTH_RELEASE, SemanticHapticType.CRASH_AFTERSHOCK -> {
                val attack = (safeDur * 0.10f).toLong().coerceAtLeast(10L)
                val sustain = (safeDur * 0.30f).toLong().coerceAtLeast(10L)
                val release = (safeDur - attack - sustain).coerceAtLeast(10L)
                HapticEnvelope(attack, sustain, release, "EASE_OUT", "EASE_OUT")
            }
            SemanticHapticType.ACCELERATION_RISE -> {
                val attack = (safeDur * 0.35f).toLong().coerceAtLeast(20L)
                val sustain = (safeDur * 0.45f).toLong().coerceAtLeast(10L)
                val release = (safeDur - attack - sustain).coerceAtLeast(15L)
                HapticEnvelope(attack, sustain, release, "EASE_IN", "EASE_OUT")
            }
            SemanticHapticType.BRAKING_PRESSURE -> {
                val attack = (safeDur * 0.30f).toLong().coerceAtLeast(20L)
                val sustain = (safeDur * 0.45f).toLong().coerceAtLeast(10L)
                val release = (safeDur - attack - sustain).coerceAtLeast(15L)
                HapticEnvelope(attack, sustain, release, "EASE_IN_OUT", "EASE_IN_OUT")
            }
            SemanticHapticType.ENGINE_RUMBLE, SemanticHapticType.RUMBLE -> {
                val attack = (safeDur * 0.15f).toLong().coerceAtLeast(15L)
                val release = (safeDur * 0.20f).toLong().coerceAtLeast(15L)
                val sustain = (safeDur - attack - release).coerceAtLeast(10L)
                HapticEnvelope(attack, sustain, release, "SMOOTHSTEP", "SMOOTHSTEP")
            }
            SemanticHapticType.LOW_FREQUENCY_DRONE -> {
                val attack = (safeDur * 0.25f).toLong().coerceAtLeast(20L)
                val release = (safeDur * 0.25f).toLong().coerceAtLeast(20L)
                val sustain = (safeDur - attack - release).coerceAtLeast(10L)
                HapticEnvelope(attack, sustain, release, "SMOOTHSTEP", "SMOOTHSTEP")
            }
            SemanticHapticType.ROAD_TEXTURE -> {
                val attack = (safeDur * 0.20f).toLong().coerceAtLeast(10L)
                val release = (safeDur * 0.20f).toLong().coerceAtLeast(10L)
                val sustain = (safeDur - attack - release).coerceAtLeast(10L)
                HapticEnvelope(attack, sustain, release, "SMOOTHSTEP", "SMOOTHSTEP")
            }
            SemanticHapticType.CURB_RUMBLE -> {
                val attack = (safeDur * 0.15f).toLong().coerceAtLeast(15L)
                val release = (safeDur * 0.15f).toLong().coerceAtLeast(15L)
                val sustain = (safeDur - attack - release).coerceAtLeast(10L)
                HapticEnvelope(attack, sustain, release, "SMOOTHSTEP", "SMOOTHSTEP")
            }
            SemanticHapticType.FAST_MOTION, SemanticHapticType.SWEEP -> {
                val attack = (safeDur * 0.30f).toLong().coerceAtLeast(15L)
                val sustain = (safeDur * 0.40f).toLong().coerceAtLeast(10L)
                val release = (safeDur - attack - sustain).coerceAtLeast(15L)
                HapticEnvelope(attack, sustain, release, "SMOOTHSTEP", "SMOOTHSTEP")
            }
            SemanticHapticType.RUNNING_RHYTHM -> {
                val attack = (safeDur * 0.20f).toLong().coerceAtLeast(10L)
                val sustain = (safeDur * 0.50f).toLong().coerceAtLeast(10L)
                val release = (safeDur - attack - sustain).coerceAtLeast(10L)
                HapticEnvelope(attack, sustain, release, "EASE_IN_OUT", "EASE_IN_OUT")
            }
            SemanticHapticType.MECHANICAL_TEXTURE -> {
                val attack = (safeDur * 0.20f).toLong().coerceAtLeast(10L)
                val sustain = (safeDur * 0.60f).toLong().coerceAtLeast(10L)
                val release = (safeDur - attack - sustain).coerceAtLeast(10L)
                HapticEnvelope(attack, sustain, release, "LINEAR", "LINEAR")
            }
            SemanticHapticType.GEAR_SHIFT, SemanticHapticType.MECHANICAL_CLICK -> {
                val attack = 15L.coerceAtMost(safeDur / 4)
                val sustain = 65L.coerceAtMost(safeDur / 2)
                val release = (safeDur - attack - sustain).coerceAtLeast(20L)
                HapticEnvelope(attack, sustain, release, "LINEAR", "EASE_OUT")
            }
            SemanticHapticType.HEAVY_IMPACT, SemanticHapticType.COLLISION -> {
                val attack = 25L
                val sustain = (safeDur * 0.48f).toLong().coerceIn(50L, 140L)
                val release = (safeDur - attack - sustain).coerceAtLeast(30L)
                HapticEnvelope(attack, sustain, release, "LINEAR", "EASE_OUT")
            }
            SemanticHapticType.SOFT_IMPACT, SemanticHapticType.LIGHT_IMPACT, SemanticHapticType.LANDING -> {
                val attack = 20L
                val sustain = (safeDur * 0.40f).toLong().coerceIn(40L, 100L)
                val release = (safeDur - attack - sustain).coerceAtLeast(25L)
                HapticEnvelope(attack, sustain, release, "LINEAR", "EASE_OUT")
            }
            SemanticHapticType.EMOTIONAL_SWELL, SemanticHapticType.CINEMATIC_ACCENT -> {
                val attack = (safeDur * 0.40f).toLong().coerceAtLeast(30L)
                val release = (safeDur * 0.35f).toLong().coerceAtLeast(30L)
                val sustain = (safeDur - attack - release).coerceAtLeast(10L)
                HapticEnvelope(attack, sustain, release, "EASE_IN_OUT", "EASE_IN_OUT")
            }
            SemanticHapticType.ENERGY -> {
                val attack = (safeDur * 0.35f).toLong().coerceAtLeast(15L)
                val sustain = (safeDur * 0.35f).toLong().coerceAtLeast(15L)
                val release = (safeDur - attack - sustain).coerceAtLeast(15L)
                HapticEnvelope(attack, sustain, release, "EASE_IN_OUT", "EASE_IN_OUT")
            }
            SemanticHapticType.METALLIC_CLASH -> {
                val attack = (safeDur * 0.15f).toLong().coerceIn(15L, 80L)
                val sustain = (safeDur * 0.25f).toLong().coerceIn(20L, 120L)
                val release = (safeDur - attack - sustain).coerceAtLeast(15L)
                HapticEnvelope(attack, sustain, release, "LINEAR", "EASE_OUT")
            }
            SemanticHapticType.DOUBLE_PULSE, SemanticHapticType.RAPID_PULSES -> {
                val attack = (safeDur * 0.20f).toLong().coerceAtLeast(10L)
                val sustain = (safeDur * 0.60f).toLong().coerceAtLeast(10L)
                val release = (safeDur - attack - sustain).coerceAtLeast(10L)
                HapticEnvelope(attack, sustain, release, "SMOOTHSTEP", "SMOOTHSTEP")
            }
            SemanticHapticType.JUMP, SemanticHapticType.FALL, SemanticHapticType.LAUNCH -> {
                val attack = (safeDur * 0.35f).toLong().coerceAtLeast(15L)
                val sustain = (safeDur * 0.30f).toLong().coerceAtLeast(10L)
                val release = (safeDur - attack - sustain).coerceAtLeast(15L)
                HapticEnvelope(attack, sustain, release, "EASE_IN", "EASE_OUT")
            }
            SemanticHapticType.TRANSITION -> {
                val attack = (safeDur * 0.35f).toLong().coerceAtLeast(15L)
                val release = (safeDur * 0.35f).toLong().coerceAtLeast(15L)
                val sustain = (safeDur - attack - release).coerceAtLeast(10L)
                HapticEnvelope(attack, sustain, release, "SMOOTHSTEP", "SMOOTHSTEP")
            }
            SemanticHapticType.QUIET -> {
                HapticEnvelope(0L, 0L, 0L, "LINEAR", "LINEAR")
            }
        }
    }

    /**
     * Determines whether a semantic type represents a continuous vibration sensation.
     */
    private fun isContinuousSemanticType(type: SemanticHapticType): Boolean {
        return when (type) {
            SemanticHapticType.CONTINUOUS_RUMBLE,
            SemanticHapticType.ENGINE_RUMBLE,
            SemanticHapticType.RUMBLE,
            SemanticHapticType.LOW_FREQUENCY_DRONE,
            SemanticHapticType.ACCELERATION_RISE,
            SemanticHapticType.BRAKING_PRESSURE,
            SemanticHapticType.ROAD_TEXTURE,
            SemanticHapticType.CURB_RUMBLE,
            SemanticHapticType.CRASH_AFTERSHOCK,
            SemanticHapticType.FAST_MOTION,
            SemanticHapticType.RUNNING_RHYTHM,
            SemanticHapticType.MECHANICAL_TEXTURE,
            SemanticHapticType.TENSION,
            SemanticHapticType.TENSION_BUILD,
            SemanticHapticType.EMOTIONAL_SWELL,
            SemanticHapticType.CINEMATIC_ACCENT,
            SemanticHapticType.SWEEP,
            SemanticHapticType.ENERGY,
            SemanticHapticType.TRANSITION,
            SemanticHapticType.SMOOTH,
            SemanticHapticType.SMOOTH_BUILD,
            SemanticHapticType.SMOOTH_RELEASE -> true
            else -> false
        }
    }

    /**
     * Transforms a [HapticEvent] into a fully synthesized, hardware-ready [WaveformProfile].
     */
    fun generateWaveformProfile(event: HapticEvent): WaveformProfile {
        val semanticType = resolveSemanticType(event)
        val envelope = resolveEnvelope(event, semanticType)

        // Calibrate target peak amplitude while faithfully preserving source research dynamic range
        val rawAmp = (event.parameters["amplitude"] as? Number)?.toFloat()
            ?: (event.parameters["intensity"] as? Number)?.toFloat()
            ?: event.intensity

        val peakAmplitude = when (semanticType) {
            SemanticHapticType.SOFT_IMPACT -> rawAmp.coerceIn(0.10f, 0.55f)
            SemanticHapticType.EMOTIONAL_SWELL -> rawAmp.coerceIn(0.10f, 0.50f)
            SemanticHapticType.TENSION -> rawAmp.coerceIn(0.10f, 0.65f)
            SemanticHapticType.HEAVY_IMPACT -> rawAmp.coerceIn(0.10f, 1.0f)
            SemanticHapticType.COLLISION -> rawAmp.coerceIn(0.10f, 1.0f)
            SemanticHapticType.GEAR_SHIFT -> rawAmp.coerceIn(0.10f, 1.0f)
            SemanticHapticType.LANDING -> rawAmp.coerceIn(0.10f, 1.0f)
            SemanticHapticType.JUMP -> rawAmp.coerceIn(0.10f, 0.90f)
            SemanticHapticType.LAUNCH -> rawAmp.coerceIn(0.10f, 0.95f)
            else -> rawAmp.coerceIn(0.01f, 1.0f)
        }

        val frequencyHz = event.frequencyHz
            ?: (event.parameters["frequencyHz"] as? Number)?.toFloat()
            ?: (100f + event.sharpness * 200f)

        val classification = event.parameters["classification"] as? String
        val isExplicitTransient = classification == "TRANSIENT"
        val isShortTransientDuration = event.durationMs in 1L..350L
        val isTransientSemantic = semanticType in setOf(
            SemanticHapticType.COLLISION,
            SemanticHapticType.HEAVY_IMPACT,
            SemanticHapticType.GEAR_SHIFT,
            SemanticHapticType.LANDING,
            SemanticHapticType.MECHANICAL_CLICK,
            SemanticHapticType.LIGHT_IMPACT,
            SemanticHapticType.SOFT_IMPACT,
            SemanticHapticType.METALLIC_CLASH
        )
        val isTransient = (isExplicitTransient && isShortTransientDuration) ||
            (isShortTransientDuration && (isTransientSemantic || !isContinuousSemanticType(semanticType)))

        // Respect explicit source envelope if provided
        val hasExplicitEnvelope = (event.envelope.attackMs > 0L || event.envelope.sustainMs > 0L || event.envelope.releaseMs > 0L)

        return when {
            semanticType == SemanticHapticType.DOUBLE_PULSE -> {
                generateDoublePulseWaveform(event.durationMs, peakAmplitude, frequencyHz, envelope)
            }
            semanticType == SemanticHapticType.RAPID_PULSES -> {
                generateRapidPulsesWaveform(event.durationMs, peakAmplitude, frequencyHz, envelope)
            }
            semanticType == SemanticHapticType.JUMP -> {
                generateJumpWaveform(event.durationMs, peakAmplitude, frequencyHz, envelope)
            }
            hasExplicitEnvelope -> {
                // Faithfully render explicit Attack-Sustain-Release envelope from research dataset
                generateStandardEnvelopeWaveform(semanticType, envelope, event.durationMs, peakAmplitude, frequencyHz)
            }
            isTransient -> {
                generateSolidTransientWaveform(semanticType, envelope, event.durationMs, peakAmplitude, frequencyHz)
            }
            else -> {
                generateStandardEnvelopeWaveform(semanticType, envelope, event.durationMs, peakAmplitude, frequencyHz)
            }
        }
    }

    /**
     * Synthesizes a solid, multi-stage physical transient waveform.
     * Prevents micro-slicing and premature dead-zone dropoff on physical phone actuators.
     * Macro-stages:
     * ATTACK -> SOLID PEAK HOLD -> CONTROLLED RELEASE (Stage 1 -> Stage 2)
     */
    private fun generateSolidTransientWaveform(
        semanticType: SemanticHapticType,
        envelope: HapticEnvelope,
        durationMs: Long,
        peakAmp: Float,
        frequencyHz: Float
    ): WaveformProfile {
        val timingList = mutableListOf<Long>()
        val ampList = mutableListOf<Int>()
        val controlPoints = mutableListOf<EnvelopeControlPoint>()

        val peakInt = (peakAmp * 255f).toInt().coerceIn(1, 255)
        val safeDur = durationMs.coerceAtLeast(30L)

        when (semanticType) {
            SemanticHapticType.COLLISION -> {
                val attackDur = (safeDur * 0.15f).toLong().coerceIn(15L, 40L)
                val holdDur = (safeDur * 0.45f).toLong().coerceAtLeast(20L)
                val rel1Dur = (safeDur * 0.25f).toLong().coerceAtLeast(15L)
                val rel2Dur = (safeDur - attackDur - holdDur - rel1Dur).coerceAtLeast(15L)

                val attackAmp = (peakAmp * 240f).toInt().coerceIn(1, 255)
                val holdAmp = peakInt
                val rel1Amp = (peakAmp * 180f).toInt().coerceIn(1, 255)
                val rel2Amp = (peakAmp * 120f).toInt().coerceIn(1, 255)

                timingList.addAll(listOf(attackDur, holdDur, rel1Dur, rel2Dur))
                ampList.addAll(listOf(attackAmp, holdAmp, rel1Amp, rel2Amp))

                controlPoints.addAll(listOf(
                    EnvelopeControlPoint(0.85f * peakAmp, frequencyHz, attackDur),
                    EnvelopeControlPoint(1.0f * peakAmp, frequencyHz, holdDur),
                    EnvelopeControlPoint(0.70f * peakAmp, frequencyHz, rel1Dur),
                    EnvelopeControlPoint(0.40f * peakAmp, frequencyHz, rel2Dur),
                    EnvelopeControlPoint(0.0f, frequencyHz, 10L)
                ))
            }

            SemanticHapticType.HEAVY_IMPACT -> {
                val attackDur = (safeDur * 0.15f).toLong().coerceIn(15L, 30L)
                val shockDur = (safeDur * 0.35f).toLong().coerceAtLeast(20L)
                val resonanceDur = (safeDur * 0.35f).toLong().coerceAtLeast(20L)
                val decayDur = (safeDur - attackDur - shockDur - resonanceDur).coerceAtLeast(20L)

                val attackAmp = (peakAmp * 160f).toInt().coerceIn(1, 255)
                val shockAmp = peakInt
                val resonanceAmp = (peakAmp * 175f).toInt().coerceIn(1, 255)
                val decayAmp = (peakAmp * 110f).toInt().coerceIn(1, 255)

                timingList.addAll(listOf(attackDur, shockDur, resonanceDur, decayDur))
                ampList.addAll(listOf(attackAmp, shockAmp, resonanceAmp, decayAmp))

                controlPoints.addAll(listOf(
                    EnvelopeControlPoint(0.65f * peakAmp, frequencyHz, attackDur),
                    EnvelopeControlPoint(1.0f * peakAmp, frequencyHz, shockDur),
                    EnvelopeControlPoint(0.65f * peakAmp, frequencyHz * 0.95f, resonanceDur),
                    EnvelopeControlPoint(0.35f * peakAmp, frequencyHz * 0.90f, decayDur),
                    EnvelopeControlPoint(0.0f, frequencyHz * 0.85f, 10L)
                ))
            }

            SemanticHapticType.GEAR_SHIFT, SemanticHapticType.MECHANICAL_CLICK -> {
                val attackDur = (safeDur * 0.15f).toLong().coerceIn(10L, 25L)
                val peakDur = (safeDur * 0.45f).toLong().coerceAtLeast(20L)
                val rel1Dur = (safeDur * 0.25f).toLong().coerceAtLeast(15L)
                val rel2Dur = (safeDur - attackDur - peakDur - rel1Dur).coerceAtLeast(15L)

                val attackAmp = (peakAmp * 220f).toInt().coerceIn(1, 255)
                val snapAmp = peakInt
                val rel1Amp = (peakAmp * 160f).toInt().coerceIn(1, 255)
                val rel2Amp = (peakAmp * 100f).toInt().coerceIn(1, 255)

                timingList.addAll(listOf(attackDur, peakDur, rel1Dur, rel2Dur))
                ampList.addAll(listOf(attackAmp, snapAmp, rel1Amp, rel2Amp))

                controlPoints.addAll(listOf(
                    EnvelopeControlPoint(0.85f * peakAmp, frequencyHz, attackDur),
                    EnvelopeControlPoint(1.0f * peakAmp, frequencyHz, peakDur),
                    EnvelopeControlPoint(0.60f * peakAmp, frequencyHz, rel1Dur),
                    EnvelopeControlPoint(0.30f * peakAmp, frequencyHz, rel2Dur),
                    EnvelopeControlPoint(0.0f, frequencyHz, 10L)
                ))
            }

            SemanticHapticType.LANDING -> {
                val attackDur = (safeDur * 0.15f).toLong().coerceIn(15L, 30L)
                val impactDur = (safeDur * 0.35f).toLong().coerceAtLeast(20L)
                val recoveryDur = (safeDur * 0.35f).toLong().coerceAtLeast(20L)
                val decayDur = (safeDur - attackDur - impactDur - recoveryDur).coerceAtLeast(15L)

                val impactAmp = peakInt
                val recoveryAmp = (peakAmp * 165f).toInt().coerceIn(1, 255)
                val decayAmp = (peakAmp * 105f).toInt().coerceIn(1, 255)

                timingList.addAll(listOf(attackDur, impactDur, recoveryDur, decayDur))
                ampList.addAll(listOf((peakAmp * 240f).toInt().coerceIn(1, 255), impactAmp, recoveryAmp, decayAmp))

                controlPoints.addAll(listOf(
                    EnvelopeControlPoint(0.85f * peakAmp, frequencyHz, attackDur),
                    EnvelopeControlPoint(1.0f * peakAmp, frequencyHz, impactDur),
                    EnvelopeControlPoint(0.60f * peakAmp, frequencyHz, recoveryDur),
                    EnvelopeControlPoint(0.30f * peakAmp, frequencyHz, decayDur),
                    EnvelopeControlPoint(0.0f, frequencyHz, 10L)
                ))
            }

            else -> {
                // General & Reference Transients (e.g. 100ms, 150ms, 200ms, 250ms)
                val safeDur = durationMs.coerceAtLeast(60L)
                val attackDur = (safeDur * 0.15f).toLong().coerceIn(15L, 30L)
                val holdDur = (safeDur * 0.50f).toLong().coerceIn(35L, 140L)
                val rel1Dur = (safeDur * 0.20f).toLong().coerceIn(20L, 60L)
                val rel2Dur = (safeDur - attackDur - holdDur - rel1Dur).coerceAtLeast(15L)

                val attackAmp = (peakAmp * 230f).toInt().coerceIn(1, 255)
                val holdAmp = peakInt
                val rel1Amp = (peakAmp * 180f).toInt().coerceIn(1, 255)
                val rel2Amp = (peakAmp * 120f).toInt().coerceIn(1, 255)

                timingList.addAll(listOf(attackDur, holdDur, rel1Dur, rel2Dur))
                ampList.addAll(listOf(attackAmp, holdAmp, rel1Amp, rel2Amp))

                controlPoints.addAll(listOf(
                    EnvelopeControlPoint(0.80f * peakAmp, frequencyHz, attackDur),
                    EnvelopeControlPoint(1.0f * peakAmp, frequencyHz, holdDur),
                    EnvelopeControlPoint(0.65f * peakAmp, frequencyHz, rel1Dur),
                    EnvelopeControlPoint(0.35f * peakAmp, frequencyHz, rel2Dur),
                    EnvelopeControlPoint(0.0f, frequencyHz, 10L)
                ))
            }
        }

        return WaveformProfile(
            semanticType = semanticType,
            envelope = envelope,
            peakAmplitude = peakAmp,
            frequencyHz = frequencyHz,
            timings = timingList.toLongArray(),
            amplitudes = ampList.toIntArray(),
            controlPoints = controlPoints
        )
    }

    /**
     * Synthesizes standard 3-phase envelope waveform (Fade In -> Sustain -> Fade Out).
     */
    private fun generateStandardEnvelopeWaveform(
        semanticType: SemanticHapticType,
        envelope: HapticEnvelope,
        durationMs: Long,
        peakAmp: Float,
        frequencyHz: Float
    ): WaveformProfile {
        val safeDur = durationMs.coerceAtLeast(30L)
        val attackMs = envelope.attackMs
        val sustainMs = envelope.sustainMs
        val releaseMs = envelope.releaseMs

        val stepMs = DEFAULT_STEP_MS.coerceAtMost(safeDur / 4).coerceAtLeast(10L)

        val timingList = mutableListOf<Long>()
        val ampList = mutableListOf<Int>()

        // 1. Attack Phase (Fade In: ramp from near-zero to peakAmp using envelope.curveIn)
        if (attackMs > 0) {
            val steps = (attackMs / stepMs).toInt().coerceAtLeast(1)
            val actualStepMs = attackMs / steps
            var remainder = attackMs % steps

            for (i in 1..steps) {
                val dur = actualStepMs + if (remainder-- > 0) 1 else 0
                val ratio = i.toFloat() / steps
                val curvedRatio = applyCurve(envelope.curveIn, ratio)
                val amp = (peakAmp * curvedRatio * 255f).toInt().coerceIn(1, 255)
                timingList.add(dur)
                ampList.add(amp)
            }
        }

        // 2. Sustain Phase (Sustained Body: steady or textured modulation at peakAmp)
        if (sustainMs > 0) {
            val steps = (sustainMs / stepMs).toInt().coerceAtLeast(1)
            val actualStepMs = sustainMs / steps
            var remainder = sustainMs % steps
            val peakInt = (peakAmp * 255f).toInt().coerceIn(1, 255)

            for (i in 1..steps) {
                val dur = actualStepMs + if (remainder-- > 0) 1 else 0
                val stepRatio = i.toFloat() / steps
                val modulatedAmp = when (semanticType) {
                    SemanticHapticType.CURB_RUMBLE -> {
                        // High-frequency tactile vibration modulation
                        val pulse = if (i % 2 == 0) 0.12f else -0.12f
                        ((peakAmp + pulse).coerceIn(0.1f, 1f) * 255f).toInt().coerceIn(1, 255)
                    }
                    SemanticHapticType.ENGINE_RUMBLE, SemanticHapticType.RUMBLE, SemanticHapticType.CONTINUOUS_RUMBLE -> {
                        val modulation = (kotlin.math.sin(i * 0.8) * 0.08).toFloat()
                        ((peakAmp + modulation).coerceIn(0.1f, 1f) * 255f).toInt().coerceIn(1, 255)
                    }
                    SemanticHapticType.ACCELERATION_RISE -> {
                        // Progressive ramp towards maximum through sustain
                        val rise = 0.85f + 0.15f * stepRatio
                        ((peakAmp * rise).coerceIn(0.1f, 1f) * 255f).toInt().coerceIn(1, 255)
                    }
                    SemanticHapticType.ROAD_TEXTURE -> {
                        val jitter = (((i * 13) % 7 - 3) * 0.02f)
                        ((peakAmp + jitter).coerceIn(0.05f, 1f) * 255f).toInt().coerceIn(1, 255)
                    }
                    SemanticHapticType.TENSION, SemanticHapticType.TENSION_BUILD -> {
                        val modulation = (kotlin.math.sin(i * 0.5) * 0.06).toFloat()
                        ((peakAmp + modulation).coerceIn(0.1f, 1f) * 255f).toInt().coerceIn(1, 255)
                    }
                    else -> peakInt
                }
                timingList.add(dur)
                ampList.add(modulatedAmp)
            }
        }

        // 3. Release Phase (Fade Out: ramp down from peakAmp to 0 using envelope.curveOut)
        if (releaseMs > 0) {
            val steps = (releaseMs / stepMs).toInt().coerceAtLeast(1)
            val actualStepMs = releaseMs / steps
            var remainder = releaseMs % steps

            for (i in 1..steps) {
                val dur = actualStepMs + if (remainder-- > 0) 1 else 0
                val ratio = 1.0f - (i.toFloat() / steps)
                val curvedRatio = applyCurve(envelope.curveOut, ratio)
                val amp = (peakAmp * curvedRatio * 255f).toInt().coerceIn(0, 255)
                timingList.add(dur)
                ampList.add(amp)
            }
        }

        // Handle any leftover duration up to durationMs
        val totalAllocated = timingList.sum()
        if (totalAllocated < safeDur) {
            timingList.add(safeDur - totalAllocated)
            ampList.add(0)
        }

        // API 36+ control points (Attack, Sustain, Release points)
        val controlPoints = listOf(
            EnvelopeControlPoint(0.05f * peakAmp, frequencyHz, 10L.coerceAtMost(attackMs.coerceAtLeast(10L))),
            EnvelopeControlPoint(peakAmp * 0.5f, frequencyHz, attackMs / 2),
            EnvelopeControlPoint(peakAmp, frequencyHz, attackMs + sustainMs / 2),
            EnvelopeControlPoint(peakAmp * 0.6f, frequencyHz, attackMs + sustainMs),
            EnvelopeControlPoint(0f, frequencyHz, attackMs + sustainMs + releaseMs)
        )

        return WaveformProfile(
            semanticType = semanticType,
            envelope = envelope,
            peakAmplitude = peakAmp,
            frequencyHz = frequencyHz,
            timings = timingList.toLongArray(),
            amplitudes = ampList.toIntArray(),
            controlPoints = controlPoints
        )
    }

    /**
     * Synthesizes DOUBLE_PULSE: two distinct pulses, each with independent fade-in -> peak -> fade-out,
     * separated by a brief gap.
     */
    private fun generateDoublePulseWaveform(
        durationMs: Long,
        peakAmp: Float,
        frequencyHz: Float,
        envelope: HapticEnvelope
    ): WaveformProfile {
        val safeDur = durationMs.coerceAtLeast(80L)
        val pulse1Dur = (safeDur * 0.45f).toLong()
        val gapDur = (safeDur * 0.10f).toLong().coerceAtLeast(10L)
        val pulse2Dur = safeDur - pulse1Dur - gapDur

        val timingList = mutableListOf<Long>()
        val ampList = mutableListOf<Int>()

        // Pulse 1: Fade In -> Peak -> Fade Out
        val p1Attack = pulse1Dur / 3
        val p1Release = pulse1Dur / 3
        val p1Sustain = pulse1Dur - p1Attack - p1Release
        appendRamp(timingList, ampList, p1Attack, 0f, peakAmp)
        if (p1Sustain > 0) {
            timingList.add(p1Sustain)
            ampList.add((peakAmp * 255f).toInt().coerceIn(1, 255))
        }
        appendRamp(timingList, ampList, p1Release, peakAmp, 0f)

        // Gap: silence
        timingList.add(gapDur)
        ampList.add(0)

        // Pulse 2: slightly softer secondary peak (70% of primary)
        val p2Peak = peakAmp * 0.70f
        val p2Attack = pulse2Dur / 3
        val p2Release = pulse2Dur / 3
        val p2Sustain = pulse2Dur - p2Attack - p2Release
        appendRamp(timingList, ampList, p2Attack, 0f, p2Peak)
        if (p2Sustain > 0) {
            timingList.add(p2Sustain)
            ampList.add((p2Peak * 255f).toInt().coerceIn(1, 255))
        }
        appendRamp(timingList, ampList, p2Release, p2Peak, 0f)

        val controlPoints = listOf(
            EnvelopeControlPoint(peakAmp, frequencyHz, pulse1Dur),
            EnvelopeControlPoint(0f, frequencyHz, gapDur),
            EnvelopeControlPoint(p2Peak, frequencyHz, pulse2Dur)
        )

        return WaveformProfile(
            semanticType = SemanticHapticType.DOUBLE_PULSE,
            envelope = envelope,
            peakAmplitude = peakAmp,
            frequencyHz = frequencyHz,
            timings = timingList.toLongArray(),
            amplitudes = ampList.toIntArray(),
            controlPoints = controlPoints
        )
    }

    /**
     * Synthesizes RAPID_PULSES: sequence of 3 micro-pulses, each with miniature attack -> sustain -> release.
     */
    private fun generateRapidPulsesWaveform(
        durationMs: Long,
        peakAmp: Float,
        frequencyHz: Float,
        envelope: HapticEnvelope
    ): WaveformProfile {
        val safeDur = durationMs.coerceAtLeast(100L)
        val pulseCount = 3
        val unitDur = safeDur / pulseCount

        val timingList = mutableListOf<Long>()
        val ampList = mutableListOf<Int>()

        for (i in 0 until pulseCount) {
            val pDur = if (i == pulseCount - 1) safeDur - (unitDur * (pulseCount - 1)) else unitDur
            val active = (pDur * 0.75f).toLong()
            val gap = pDur - active
            val attack = active / 3
            val release = active / 3
            val sustain = active - attack - release

            appendRamp(timingList, ampList, attack, 0f, peakAmp)
            if (sustain > 0) {
                timingList.add(sustain)
                ampList.add((peakAmp * 255f).toInt().coerceIn(1, 255))
            }
            appendRamp(timingList, ampList, release, peakAmp, 0f)
            if (gap > 0) {
                timingList.add(gap)
                ampList.add(0)
            }
        }

        return WaveformProfile(
            semanticType = SemanticHapticType.RAPID_PULSES,
            envelope = envelope,
            peakAmplitude = peakAmp,
            frequencyHz = frequencyHz,
            timings = timingList.toLongArray(),
            amplitudes = ampList.toIntArray(),
            controlPoints = listOf(EnvelopeControlPoint(peakAmp, frequencyHz, safeDur))
        )
    }

    /**
     * Synthesizes JUMP: 4-phase kinematic trajectory
     * Phase 1: Anticipation (low amplitude preparation ramp)
     * Phase 2: Launch (smooth surge to target peak)
     * Phase 3: Airborne / Apex (controlled sustained glide)
     * Phase 4: Landing (touchdown impact + natural decay to 0)
     */
    private fun generateJumpWaveform(
        durationMs: Long,
        peakAmp: Float,
        frequencyHz: Float,
        envelope: HapticEnvelope
    ): WaveformProfile {
        val safeDur = durationMs.coerceAtLeast(100L)
        val timingList = mutableListOf<Long>()
        val ampList = mutableListOf<Int>()

        // 4-Phase Kinematic Breakdown:
        val p1Dur = (safeDur * 0.20f).toLong().coerceAtLeast(15L) // Anticipation
        val p2Dur = (safeDur * 0.25f).toLong().coerceAtLeast(15L) // Launch
        val p3Dur = (safeDur * 0.25f).toLong().coerceAtLeast(15L) // Airborne Apex
        val p4Dur = (safeDur - p1Dur - p2Dur - p3Dur).coerceAtLeast(20L) // Landing & Decay

        // Phase 1 - Anticipation: ramp from 0 to 25% peakAmp
        val anticipationAmp = (peakAmp * 0.25f).coerceIn(0.05f, 0.35f)
        appendRamp(timingList, ampList, p1Dur, 0f, anticipationAmp)

        // Phase 2 - Launch: ramp from anticipationAmp up to peakAmp
        appendRamp(timingList, ampList, p2Dur, anticipationAmp, peakAmp)

        // Phase 3 - Apex: smooth airborne float down to ~45% peakAmp
        val apexAmp = (peakAmp * 0.45f).coerceIn(0.10f, 0.50f)
        appendRamp(timingList, ampList, p3Dur, peakAmp, apexAmp)

        // Phase 4 - Landing: controlled touchdown impact (~80% peakAmp), then smooth release to 0
        val landingImpactDur = (p4Dur * 0.30f).toLong().coerceAtLeast(10L)
        val landingDecayDur = p4Dur - landingImpactDur
        val landingPeak = (peakAmp * 0.80f).coerceIn(0.15f, 0.90f)

        appendRamp(timingList, ampList, landingImpactDur, apexAmp, landingPeak)
        appendRamp(timingList, ampList, landingDecayDur, landingPeak, 0f)

        // Pad any remainder
        val totalAllocated = timingList.sum()
        if (totalAllocated < safeDur) {
            timingList.add(safeDur - totalAllocated)
            ampList.add(0)
        }

        val controlPoints = listOf(
            EnvelopeControlPoint(anticipationAmp, frequencyHz, p1Dur),
            EnvelopeControlPoint(peakAmp, frequencyHz, p2Dur),
            EnvelopeControlPoint(apexAmp, frequencyHz, p3Dur),
            EnvelopeControlPoint(landingPeak, frequencyHz, landingImpactDur),
            EnvelopeControlPoint(0f, frequencyHz, landingDecayDur)
        )

        return WaveformProfile(
            semanticType = SemanticHapticType.JUMP,
            envelope = envelope,
            peakAmplitude = peakAmp,
            frequencyHz = frequencyHz,
            timings = timingList.toLongArray(),
            amplitudes = ampList.toIntArray(),
            controlPoints = controlPoints
        )
    }

    private fun appendRamp(
        timingList: MutableList<Long>,
        ampList: MutableList<Int>,
        durationMs: Long,
        startAmp: Float,
        endAmp: Float
    ) {
        if (durationMs <= 0) return
        val stepMs = 15L
        val steps = (durationMs / stepMs).toInt().coerceAtLeast(1)
        val actualStep = durationMs / steps
        var rem = durationMs % steps

        for (s in 1..steps) {
            val d = actualStep + if (rem-- > 0) 1 else 0
            val ratio = s.toFloat() / steps
            val current = startAmp + (endAmp - startAmp) * ratio
            timingList.add(d)
            ampList.add((current * 255f).toInt().coerceIn(0, 255))
        }
    }
}

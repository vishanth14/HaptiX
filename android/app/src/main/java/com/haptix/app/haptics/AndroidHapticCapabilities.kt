package com.haptix.app.haptics

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Three-tier hardware capability state for experimental research haptics.
 */
enum class HapticCapabilityLevel {
    /**
     * Hardware & OS fully support frequency-aware vibration envelopes (Android 16+ / API 36+).
     */
    SUPPORTED,

    /**
     * Vibrator hardware exists with amplitude/waveform control, but frequency modulation/envelopes
     * are unsupported by the device actuator or Android platform version.
     */
    LIMITED,

    /**
     * No vibrator motor detected on the device or vibration permission/service unavailable.
     */
    UNAVAILABLE
}

/**
 * Detailed snapshot of the physical and software vibration actuation stack.
 */
data class HapticHardwareProfile(
    val hasVibrator: Boolean,
    val hasAmplitudeControl: Boolean,
    val areEnvelopeEffectsSupported: Boolean,
    val resonantFrequencyHz: Float?,
    val minFrequencyHz: Float? = null,
    val maxFrequencyHz: Float? = null,
    val capabilityLevel: HapticCapabilityLevel,
    val statusLabel: String,
    val statusDescription: String
)

/**
 * Android vibration capability detector adhering to Android API levels,
 * AOSP specifications, and Android 16 (API 36) frequency envelope contracts.
 *
 * Uses safe reflection and version checks to probe advanced frequency and envelope APIs
 * without causing [ClassNotFoundException] or [NoSuchMethodException] on older Android versions.
 */
class AndroidHapticCapabilities(
    private val context: Context? = null,
    customVibrator: Vibrator? = null
) {
    val vibrator: Vibrator? = customVibrator ?: resolveSystemVibrator(context)

    /**
     * Evaluates and returns the complete [HapticHardwareProfile] for the current device.
     */
    fun getHardwareProfile(): HapticHardwareProfile {
        val vib = vibrator
        if (vib == null || !vib.hasVibrator()) {
            return HapticHardwareProfile(
                hasVibrator = false,
                hasAmplitudeControl = false,
                areEnvelopeEffectsSupported = false,
                resonantFrequencyHz = null,
                capabilityLevel = HapticCapabilityLevel.UNAVAILABLE,
                statusLabel = "HAPTICS UNAVAILABLE",
                statusDescription = "No physical vibrator actuator detected on this device."
            )
        }

        val hasAmplitude = vib.hasAmplitudeControl()
        val envelopeSupported = checkEnvelopeEffectsSupported(vib)
        val resonantFreq = probeResonantFrequency(vib)
        val (minFreq, maxFreq) = probeFrequencyRange(vib)

        val level = when {
            envelopeSupported -> HapticCapabilityLevel.SUPPORTED
            hasAmplitude -> HapticCapabilityLevel.LIMITED
            else -> HapticCapabilityLevel.LIMITED
        }

        val (label, desc) = when (level) {
            HapticCapabilityLevel.SUPPORTED -> Pair(
                "HAPTICS READY",
                "Frequency-aware envelope synthesis active (${resonantFreq?.let { "f₀ = ${it}Hz" } ?: "API 36+"})."
            )
            HapticCapabilityLevel.LIMITED -> Pair(
                "HAPTICS LIMITED",
                "Actuator supports amplitude control; frequency-aware envelopes unsupported on this device/OS."
            )
            HapticCapabilityLevel.UNAVAILABLE -> Pair(
                "HAPTICS UNAVAILABLE",
                "Vibrator hardware not available."
            )
        }

        return HapticHardwareProfile(
            hasVibrator = true,
            hasAmplitudeControl = hasAmplitude,
            areEnvelopeEffectsSupported = envelopeSupported,
            resonantFrequencyHz = resonantFreq,
            minFrequencyHz = minFreq,
            maxFrequencyHz = maxFreq,
            capabilityLevel = level,
            statusLabel = label,
            statusDescription = desc
        )
    }

    /**
     * Checks if frequency-aware envelope effects are supported (Android 16 / API 36+).
     */
    private fun checkEnvelopeEffectsSupported(vib: Vibrator): Boolean {
        return try {
            val method = vib.javaClass.getMethod("areEnvelopeEffectsSupported")
            (method.invoke(vib) as? Boolean) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Probes the hardware resonant frequency in Hertz if exposed by the HAL/framework.
     */
    private fun probeResonantFrequency(vib: Vibrator): Float? {
        return try {
            val method = vib.javaClass.getMethod("getResonantFrequency")
            val result = method.invoke(vib)
            if (result is Number) {
                val f = result.toFloat()
                if (!f.isNaN() && !f.isInfinite() && f > 0f) f else null
            } else null
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Probes supported frequency profile range (minFrequencyHz, maxFrequencyHz).
     */
    private fun probeFrequencyRange(vib: Vibrator): Pair<Float?, Float?> {
        return try {
            val method = vib.javaClass.getMethod("getFrequencyProfile")
            val profile = method.invoke(vib) ?: return Pair(null, null)
            // Profile may expose min/max frequency methods or frequency array
            var minF: Float? = null
            var maxF: Float? = null
            try {
                val minMethod = profile.javaClass.getMethod("getMinFrequency")
                val r = (minMethod.invoke(profile) as? Number)?.toFloat()
                if (r != null && !r.isNaN() && r > 0f) minF = r
            } catch (_: Throwable) {}

            try {
                val maxMethod = profile.javaClass.getMethod("getMaxFrequency")
                val r = (maxMethod.invoke(profile) as? Number)?.toFloat()
                if (r != null && !r.isNaN() && r > 0f) maxF = r
            } catch (_: Throwable) {}

            Pair(minF, maxF)
        } catch (_: Throwable) {
            Pair(null, null)
        }
    }

    companion object {
        fun resolveSystemVibrator(context: Context?): Vibrator? {
            if (context == null) return null
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }
    }
}

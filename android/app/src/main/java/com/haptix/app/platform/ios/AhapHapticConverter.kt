package com.haptix.app.platform.ios

import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticEventType
import com.haptix.app.data.model.HapticPattern
import org.json.JSONArray
import org.json.JSONObject

/**
 * Platform adapter translating the platform-independent [HapticPattern] into Apple Haptic Audio Pattern (AHAP).
 *
 * Design Constraints:
 * - Independent of Apple iOS SDK binaries (generates standard compliant AHAP JSON).
 * - Android does NOT consume AHAP files; this converter prepares the identical domain pattern
 *   for the future iOS CoreHaptics engine.
 * - Standard AHAP specification: Version 1.0, Pattern array, Event objects with EventParameters.
 */
object AhapHapticConverter {

    /**
     * Converts a platform-independent [HapticPattern] into an AHAP JSON string.
     */
    fun toAhapJson(pattern: HapticPattern): String {
        val root = JSONObject()
        root.put("Version", 1.0)

        val metadata = JSONObject()
        metadata.put("Project", "HaptiX")
        metadata.put("VideoId", pattern.videoId)
        metadata.put("Source", pattern.source.name)
        metadata.put("Created", "HaptiX Platform-Independent Architecture")
        root.put("Metadata", metadata)

        val patternArray = JSONArray()

        for (event in pattern.events) {
            val eventObj = JSONObject()
            val timeSeconds = event.startTimeMs / 1000.0
            val durSeconds = event.durationMs / 1000.0

            val eventType = if (event.durationMs > 50L || event.type == HapticEventType.CONTINUOUS) {
                "HapticContinuous"
            } else {
                "HapticTransient"
            }

            val eventData = JSONObject()
            eventData.put("Time", timeSeconds)
            eventData.put("EventType", eventType)
            if (eventType == "HapticContinuous") {
                eventData.put("EventDuration", durSeconds)
            }

            val eventParameters = JSONArray()

            // Intensity parameter (0.0 to 1.0)
            val intensityParam = JSONObject()
            intensityParam.put("ParameterID", "HapticIntensity")
            intensityParam.put("ParameterValue", event.intensity.toDouble())
            eventParameters.put(intensityParam)

            // Sharpness parameter (0.0 to 1.0)
            val sharpnessParam = JSONObject()
            sharpnessParam.put("ParameterID", "HapticSharpness")
            sharpnessParam.put("ParameterValue", event.sharpness.toDouble())
            eventParameters.put(sharpnessParam)

            eventData.put("EventParameters", eventParameters)
            eventObj.put("Event", eventData)
            patternArray.put(eventObj)

            // Synthesize Attack and Release parameter curves for continuous envelopes if present
            if (eventType == "HapticContinuous" && (event.attackMs > 0L || event.releaseMs > 0L)) {
                val curveObj = createParameterCurve(event, timeSeconds, durSeconds)
                if (curveObj != null) {
                    patternArray.put(curveObj)
                }
            }
        }

        root.put("Pattern", patternArray)
        return root.toString(2)
    }

    private fun createParameterCurve(
        event: HapticEvent,
        timeSeconds: Double,
        durSeconds: Double
    ): JSONObject? {
        val curveRoot = JSONObject()
        val curve = JSONObject()
        curve.put("ParameterID", "HapticIntensityControl")
        curve.put("StartTime", timeSeconds)

        val controlPoints = JSONArray()

        val attackSec = event.attackMs / 1000.0
        val sustainSec = event.sustainMs / 1000.0
        val releaseSec = event.releaseMs / 1000.0

        // Point 0: Start of attack (intensity = 0.0)
        val p0 = JSONObject()
        p0.put("TimeOffset", 0.0)
        p0.put("ParameterValue", 0.0)
        controlPoints.put(p0)

        // Point 1: End of attack / peak body (intensity = 1.0 normalized)
        val p1 = JSONObject()
        p1.put("TimeOffset", attackSec.coerceAtMost(durSeconds))
        p1.put("ParameterValue", 1.0)
        controlPoints.put(p1)

        // Point 2: End of sustain / start of release
        val p2Time = (attackSec + sustainSec).coerceAtMost(durSeconds)
        val p2 = JSONObject()
        p2.put("TimeOffset", p2Time)
        p2.put("ParameterValue", 1.0)
        controlPoints.put(p2)

        // Point 3: End of release (decay to 0.0)
        val p3 = JSONObject()
        p3.put("TimeOffset", durSeconds)
        p3.put("ParameterValue", 0.0)
        controlPoints.put(p3)

        curve.put("ParameterCurveControlPoints", controlPoints)
        curveRoot.put("ParameterCurve", curve)
        return curveRoot
    }
}

package com.haptix.app.data.parser

import com.haptix.app.data.model.HapticFrequencyPattern
import com.haptix.app.data.model.HapticFrequencyPoint
import com.haptix.app.data.model.HapticPattern
import org.json.JSONException
import org.json.JSONObject
import java.io.InputStream

/**
 * Parser for HaptiX frequency-calibrated haptic stimuli datasets.
 *
 * Validates strict physical constraints:
 * - Frequency: > 0 Hz, finite, non-NaN
 * - Timing: startTimeMs >= 0, durationMs >= 0
 * - Amplitude: 0.0f .. 1.0f, finite, non-NaN
 * - Enforces chronological sorting of timeline events.
 */
object FrequencyPatternParser {

    /**
     * Parses a JSON string into a validated [HapticFrequencyPattern].
     *
     * @param jsonString Raw JSON content.
     * @return [Result.success] with validated [HapticFrequencyPattern], or [Result.failure] with error.
     */
    fun parseJson(jsonString: String): Result<HapticFrequencyPattern> {
        if (jsonString.isBlank()) {
            return Result.failure(IllegalArgumentException("Haptic frequency JSON string is empty or blank"))
        }

        return try {
            val root = JSONObject(jsonString)

            val videoId = root.optString("videoId", "unspecified_video")
            val source = root.optString("source", "AOSP Haptics & Vibrotactile Research")
            val sourceUrl = root.optString("sourceUrl", "https://source.android.com/devices/sensors/haptics")
            val description = root.optString("description", "")
            val frequencyUnit = root.optString("frequencyUnit", "Hz")
            val timingUnit = root.optString("timingUnit", "ms")
            val version = root.optString("version", "1.0")

            val pointsArray = root.optJSONArray("points")
            val pointsList = mutableListOf<HapticFrequencyPoint>()

            if (pointsArray != null) {
                for (i in 0 until pointsArray.length()) {
                    val pointObj = pointsArray.optJSONObject(i)
                        ?: return Result.failure(IllegalArgumentException("Point at index $i is not a valid JSON object"))

                    // Validate startTimeMs
                    if (!pointObj.has("startTimeMs")) {
                        return Result.failure(IllegalArgumentException("Point at index $i missing 'startTimeMs'"))
                    }
                    val startTimeMs = pointObj.getLong("startTimeMs")
                    if (startTimeMs < 0L) {
                        return Result.failure(IllegalArgumentException("Invalid startTimeMs at index $i: $startTimeMs (must be >= 0)"))
                    }

                    // Validate durationMs
                    val durationMs = if (pointObj.has("durationMs")) pointObj.getLong("durationMs") else 0L
                    if (durationMs < 0L) {
                        return Result.failure(IllegalArgumentException("Invalid durationMs at index $i: $durationMs (must be >= 0)"))
                    }

                    // Validate frequencyHz
                    if (!pointObj.has("frequencyHz")) {
                        return Result.failure(IllegalArgumentException("Point at index $i missing 'frequencyHz'"))
                    }
                    val rawFreq = pointObj.getDouble("frequencyHz")
                    val frequencyHz = rawFreq.toFloat()
                    if (frequencyHz.isNaN() || frequencyHz.isInfinite() || frequencyHz <= 0f) {
                        return Result.failure(IllegalArgumentException("Invalid frequencyHz at index $i: $rawFreq (must be > 0 and finite)"))
                    }

                    // Validate amplitude
                    val rawAmp = if (pointObj.has("amplitude")) pointObj.getDouble("amplitude") else 1.0
                    val amplitude = rawAmp.toFloat()
                    if (amplitude.isNaN() || amplitude.isInfinite() || amplitude < 0.0f || amplitude > 1.0f) {
                        return Result.failure(IllegalArgumentException("Invalid amplitude at index $i: $rawAmp (must be between 0.0 and 1.0)"))
                    }

                    // Extract extensible metadata
                    val params = mutableMapOf<String, Any>()
                    if (pointObj.has("description")) {
                        params["description"] = pointObj.getString("description")
                    }
                    if (pointObj.has("provenance")) {
                        params["provenance"] = pointObj.getString("provenance")
                    }

                    pointsList.add(
                        HapticFrequencyPoint(
                            startTimeMs = startTimeMs,
                            durationMs = durationMs,
                            frequencyHz = frequencyHz,
                            amplitude = amplitude,
                            parameters = params
                        )
                    )
                }
            }

            // Guarantee chronological ordering
            val sortedPoints = pointsList.sortedBy { it.startTimeMs }

            val pattern = HapticFrequencyPattern(
                videoId = videoId,
                source = source,
                sourceUrl = sourceUrl,
                description = description,
                frequencyUnit = frequencyUnit,
                timingUnit = timingUnit,
                version = version,
                points = sortedPoints
            )

            Result.success(pattern)
        } catch (e: JSONException) {
            Result.failure(IllegalArgumentException("Malformed JSON syntax in haptic frequency configuration: ${e.message}", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses an [InputStream] into a validated [HapticFrequencyPattern].
     */
    fun parseInputStream(inputStream: InputStream): Result<HapticFrequencyPattern> {
        return try {
            val content = inputStream.bufferedReader().use { it.readText() }
            parseJson(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses a JSON string directly to the domain [HapticPattern].
     */
    fun parseToHapticPattern(jsonString: String): Result<HapticPattern> {
        return parseJson(jsonString).map { it.toHapticPattern() }
    }
}

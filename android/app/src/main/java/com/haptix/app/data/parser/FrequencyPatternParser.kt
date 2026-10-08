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

                    // Validate amplitude (supports 'amplitude' or 'intensity')
                    val rawAmp = when {
                        pointObj.has("amplitude") -> pointObj.getDouble("amplitude")
                        pointObj.has("intensity") -> pointObj.getDouble("intensity")
                        else -> 1.0
                    }
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
                    if (pointObj.has("hapticType")) {
                        params["hapticType"] = pointObj.getString("hapticType")
                    }
                    if (pointObj.has("texture")) {
                        val texObj = pointObj.getJSONObject("texture")
                        val texMap = mutableMapOf<String, Any>()
                        if (texObj.has("type")) texMap["type"] = texObj.getString("type")
                        if (texObj.has("modulationDepth")) texMap["modulationDepth"] = texObj.getDouble("modulationDepth")
                        if (texObj.has("modulationRateHz")) texMap["modulationRateHz"] = texObj.getDouble("modulationRateHz")
                        params["texture"] = texMap
                    }
                    val envObj = if (pointObj.has("envelope")) pointObj.getJSONObject("envelope")
                                 else if (pointObj.has("pattern")) pointObj.getJSONObject("pattern")
                                 else null
                    if (envObj != null) {
                        val attackMs = if (envObj.has("attackMs")) envObj.getLong("attackMs") else 0L
                        val sustainMs = if (envObj.has("sustainMs")) envObj.getLong("sustainMs") else 0L
                        val releaseMs = if (envObj.has("releaseMs")) envObj.getLong("releaseMs") else 0L
                        val curveIn = envObj.optString("curveIn", "EASE_IN_OUT")
                        val curveOut = envObj.optString("curveOut", "EASE_IN_OUT")

                        if (attackMs < 0L || sustainMs < 0L || releaseMs < 0L) {
                            return Result.failure(IllegalArgumentException("Point at index $i has negative envelope parameters: attack=$attackMs, sustain=$sustainMs, release=$releaseMs"))
                        }
                        if (attackMs + sustainMs + releaseMs > durationMs) {
                            return Result.failure(IllegalArgumentException("Point at index $i envelope ($attackMs + $sustainMs + $releaseMs = ${attackMs + sustainMs + releaseMs}ms) exceeds duration ($durationMs ms)"))
                        }
                        params["attackMs"] = attackMs
                        params["sustainMs"] = sustainMs
                        params["releaseMs"] = releaseMs
                        params["curveIn"] = curveIn
                        params["curveOut"] = curveOut
                        params["envelope"] = mapOf("attackMs" to attackMs, "sustainMs" to sustainMs, "releaseMs" to releaseMs, "curveIn" to curveIn, "curveOut" to curveOut)
                        params["pattern"] = mapOf("attackMs" to attackMs, "sustainMs" to sustainMs, "releaseMs" to releaseMs)
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
     *
     * If the JSON contains a native "events" collection (platform-independent schema 2.0),
     * it is parsed with full fidelity (supporting null frequencyHz, envelopes, and evidence).
     * If only "points" is present, it falls back to the frequency points parser.
     */
    fun parseToHapticPattern(jsonString: String): Result<HapticPattern> {
        if (jsonString.isBlank()) {
            return Result.failure(IllegalArgumentException("Haptic pattern JSON string is empty or blank"))
        }

        return try {
            val root = JSONObject(jsonString)
            if (root.has("events")) {
                val videoId = root.optString("videoId", "unspecified_video")
                val version = root.optString("version", "1.0")
                val sourceStr = root.optString("source", "CURATED")
                val source = when (sourceStr.uppercase()) {
                    "CURATED" -> com.haptix.app.domain.model.HapticSource.CURATED
                    "GENERATED" -> com.haptix.app.domain.model.HapticSource.GENERATED
                    else -> com.haptix.app.domain.model.HapticSource.LEGACY_MANUAL
                }

                val eventsArr = root.getJSONArray("events")
                val eventList = mutableListOf<com.haptix.app.data.model.HapticEvent>()

                for (i in 0 until eventsArr.length()) {
                    val evObj = eventsArr.getJSONObject(i)
                    val id = evObj.optString("id", java.util.UUID.randomUUID().toString())
                    val startTimeMs = evObj.getLong("startTimeMs")
                    val durationMs = evObj.optLong("durationMs", 0L)
                    val rawAmp = when {
                        evObj.has("intensity") -> evObj.getDouble("intensity")
                        evObj.has("amplitude") -> evObj.getDouble("amplitude")
                        else -> 1.0
                    }
                    val intensity = rawAmp.toFloat().coerceIn(0.0f, 1.0f)
                    val freqHz = if (evObj.has("frequencyHz") && !evObj.isNull("frequencyHz")) {
                        evObj.getDouble("frequencyHz").toFloat()
                    } else if (evObj.has("frequency") && !evObj.isNull("frequency")) {
                        evObj.getDouble("frequency").toFloat()
                    } else null
                    val defaultSharpness = if (freqHz != null) ((freqHz - 100f) / 200f).coerceIn(0f, 1f) else 0.5f
                    val sharpness = (evObj.optDouble("sharpness", defaultSharpness.toDouble())).toFloat().coerceIn(0.0f, 1.0f)
                    val semStr = evObj.optString("semanticType", "SMOOTH")
                    val semanticType = com.haptix.app.data.model.SemanticHapticType.fromString(semStr)
                    val envObj = if (evObj.has("envelope")) evObj.optJSONObject("envelope") else null
                    val attackMs = if (envObj != null && envObj.has("attackMs")) envObj.optLong("attackMs", 0L) else evObj.optLong("attackMs", 0L)
                    val sustainMs = if (envObj != null && envObj.has("sustainMs")) envObj.optLong("sustainMs", 0L) else evObj.optLong("sustainMs", 0L)
                    val releaseMs = if (envObj != null && envObj.has("releaseMs")) envObj.optLong("releaseMs", 0L) else evObj.optLong("releaseMs", 0L)
                    val confidence = (evObj.optDouble("confidence", 1.0)).toFloat().coerceIn(0.0f, 1.0f)

                    val evidence = if (evObj.has("evidence")) {
                        val evJson = evObj.getJSONObject("evidence")
                        val evMap = mutableMapOf<String, Any?>()
                        val keys = evJson.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            evMap[k] = evJson.get(k)
                        }
                        com.haptix.app.domain.model.HapticEvidence.fromMap(evMap)
                    } else null

                    val modArr = evObj.optJSONArray("sourceModalities")
                    val modalities = mutableListOf<String>()
                    if (modArr != null) {
                        for (m in 0 until modArr.length()) {
                            modalities.add(modArr.getString(m))
                        }
                    }

                    val params = mutableMapOf<String, Any>()
                    params["intensity"] = intensity
                    params["amplitude"] = intensity
                    if (freqHz != null) {
                        params["frequencyHz"] = freqHz
                    }
                    params["attackMs"] = attackMs
                    params["sustainMs"] = sustainMs
                    params["releaseMs"] = releaseMs

                    if (envObj != null) {
                        val envMap = mutableMapOf<String, Any>()
                        val eKeys = envObj.keys()
                        while (eKeys.hasNext()) {
                            val ek = eKeys.next()
                            envMap[ek] = envObj.get(ek)
                        }
                        params["envelope"] = envMap
                    }
                    if (evObj.has("texture")) {
                        val texObj = evObj.getJSONObject("texture")
                        val texMap = mutableMapOf<String, Any>()
                        val tKeys = texObj.keys()
                        while (tKeys.hasNext()) {
                            val tk = tKeys.next()
                            texMap[tk] = texObj.get(tk)
                        }
                        params["texture"] = texMap
                    }
                    if (evObj.has("classification")) {
                        params["classification"] = evObj.getString("classification")
                    }
                    if (evObj.has("layerRole")) {
                        params["layerRole"] = evObj.getString("layerRole")
                    }
                    if (evObj.has("parameters")) {
                        val pObj = evObj.getJSONObject("parameters")
                        val pKeys = pObj.keys()
                        while (pKeys.hasNext()) {
                            val pk = pKeys.next()
                            params[pk] = pObj.get(pk)
                        }
                    }

                    val peakTimeMs = if (evObj.has("peakTimeMs") && !evObj.isNull("peakTimeMs")) {
                        evObj.getLong("peakTimeMs")
                    } else {
                        (params["peakTimeMs"] as? Number)?.toLong()
                    }
                    if (peakTimeMs != null) {
                        params["peakTimeMs"] = peakTimeMs
                    }

                    eventList.add(
                        com.haptix.app.data.model.HapticEvent(
                            id = id,
                            type = if (durationMs > 80L) com.haptix.app.data.model.HapticEventType.CONTINUOUS else com.haptix.app.data.model.HapticEventType.TRANSIENT,
                            startTimeMs = startTimeMs,
                            durationMs = durationMs,
                            intensity = intensity,
                            sharpness = sharpness,
                            semanticType = semanticType,
                            attackMs = attackMs,
                            sustainMs = sustainMs,
                            releaseMs = releaseMs,
                            frequencyHz = freqHz,
                            confidence = confidence,
                            evidence = evidence,
                            sourceModalities = modalities,
                            parameters = params,
                            peakTimeMs = peakTimeMs
                        )
                    )
                }

                val vidDur = if (root.has("videoDurationMs")) root.optLong("videoDurationMs") else null
                Result.success(
                    HapticPattern(
                        videoId = videoId,
                        version = version,
                        source = source,
                        events = eventList.sortedBy { it.startTimeMs },
                        videoDurationMs = vidDur
                    )
                )
            } else {
                parseJson(jsonString).map { it.toHapticPattern() }
            }
        } catch (e: Exception) {
            parseJson(jsonString).map { it.toHapticPattern() }
        }
    }
}

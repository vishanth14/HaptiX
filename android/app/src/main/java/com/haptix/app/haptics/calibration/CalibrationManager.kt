package com.haptix.app.haptics.calibration

import android.content.Context
import android.os.Build
import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.SemanticHapticType
import com.haptix.app.haptics.AndroidHapticPlayer
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CalibrationTestResult(
    val testId: String,
    val frequencyHz: Float,
    val intensity: Float,
    val durationMs: Long,
    var perceivedRating: Int = 0,
    var isSmooth: Boolean = true
)

class CalibrationManager(
    private val context: Context? = null,
    val hapticPlayer: AndroidHapticPlayer = if (context != null) AndroidHapticPlayer(context) else AndroidHapticPlayer(),
    storageDir: File? = context?.filesDir
) {
    private val calibrationFile = if (storageDir != null) File(storageDir, "haptix_device_calibration.json") else null
    val stimuli = CalibrationStimulus.ALL_STIMULI
    val results = mutableMapOf<String, CalibrationTestResult>()
    var currentIndex: Int = 0

    val currentStimulus: CalibrationStimulus
        get() = stimuli[currentIndex]

    private var activeProfile: DeviceHapticProfile = loadSavedProfile() ?: DeviceHapticProfile.getConservativeDefault()

    init {
        // Initialize results map
        for (s in stimuli) {
            results[s.testId] = CalibrationTestResult(
                testId = s.testId,
                frequencyHz = s.frequencyHz,
                intensity = s.intensity,
                durationMs = s.durationMs
            )
        }
    }

    fun next() {
        if (currentIndex < stimuli.size - 1) currentIndex++
    }

    fun previous() {
        if (currentIndex > 0) currentIndex--
    }

    fun playCurrent() {
        playStimulus(currentStimulus)
    }

    fun getActiveProfile(): DeviceHapticProfile = activeProfile

    fun playStimulus(stimulus: CalibrationStimulus) {
        val event = HapticEvent(
            startTimeMs = 0L,
            durationMs = stimulus.durationMs,
            intensity = stimulus.intensity,
            semanticType = if (stimulus.isContinuous) SemanticHapticType.CONTINUOUS_RUMBLE else SemanticHapticType.LIGHT_IMPACT,
            parameters = mapOf(
                "hapticType" to if (stimulus.isContinuous) "CONTINUOUS_RUMBLE" else "LIGHT_IMPACT",
                "frequencyHz" to stimulus.frequencyHz,
                "intensity" to stimulus.intensity,
                "amplitude" to stimulus.intensity
            )
        )
        hapticPlayer.playEvent(event)
    }

    fun stop() {
        hapticPlayer.stop()
    }

    fun recordRating(testId: String, rating: Int, isSmooth: Boolean) {
        val r = results[testId]
        if (r != null) {
            r.perceivedRating = rating
            r.isSmooth = isSmooth
        }
        computeAndSaveProfile()
    }

    fun computeProfile(
        deviceModel: String = Build.MODEL ?: "Android Device",
        androidApi: Int = Build.VERSION.SDK_INT
    ): DeviceHapticProfile {
        var floor = 0.35f
        val sortedByIntensity = results.values.sortedBy { it.intensity }
        for (r in sortedByIntensity) {
            if (r.perceivedRating >= 3) {
                floor = r.intensity
                break
            }
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        return DeviceHapticProfile(
            deviceModel = deviceModel,
            androidApi = androidApi,
            hasVibrator = hapticPlayer.isHapticSupported(),
            hasAmplitudeControl = hapticPlayer.hardwareProfile.hasAmplitudeControl,
            preferredFrequencyHz = 180.0f,
            minPerceivableIntensity = floor,
            comfortableContinuousIntensity = (floor + 0.25f).coerceAtMost(0.70f),
            strongEventIntensity = 0.85f,
            maxPracticalIntensity = 0.95f,
            preferredTransientDurationMs = 160,
            preferredContinuousDurationMs = 2500,
            calibrationTimestamp = timestamp,
            isCalibrated = true
        )
    }

    private fun computeAndSaveProfile() {
        val profile = computeProfile()
        activeProfile = profile

        // Persist to file if storage available
        if (calibrationFile != null) {
            try {
                val root = JSONObject()
                root.put("profile", JSONObject(profile.toJson()))
                val testsArr = JSONArray()
                for (res in results.values) {
                    val tObj = JSONObject()
                    tObj.put("testId", res.testId)
                    tObj.put("frequencyHz", res.frequencyHz)
                    tObj.put("intensity", res.intensity)
                    tObj.put("durationMs", res.durationMs)
                    tObj.put("perceivedRating", res.perceivedRating)
                    tObj.put("smooth", res.isSmooth)
                    testsArr.put(tObj)
                }
                root.put("tests", testsArr)
                calibrationFile.writeText(root.toString(2))
            } catch (_: Exception) {}
        }
    }

    private fun loadSavedProfile(): DeviceHapticProfile? {
        if (calibrationFile == null || !calibrationFile.exists()) return null
        return try {
            val root = JSONObject(calibrationFile.readText())
            val profObj = root.optJSONObject("profile") ?: return null
            DeviceHapticProfile.fromJson(profObj.toString())
        } catch (_: Exception) {
            null
        }
    }
}

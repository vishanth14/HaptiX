package com.haptix.app.haptics.calibration

import org.json.JSONObject

/**
 * Encapsulates device-specific physical actuator calibration parameters.
 * Accounts for minimum perceivable intensity floor, actuator response, and dynamic contrast.
 */
data class DeviceHapticProfile(
    val deviceModel: String,
    val androidApi: Int,
    val hasVibrator: Boolean,
    val hasAmplitudeControl: Boolean,
    val preferredFrequencyHz: Float = 180.0f,
    val minPerceivableIntensity: Float = 0.35f,
    val comfortableContinuousIntensity: Float = 0.60f,
    val strongEventIntensity: Float = 0.82f,
    val maxPracticalIntensity: Float = 0.95f,
    val preferredTransientDurationMs: Int = 160,
    val preferredContinuousDurationMs: Int = 2500,
    val calibrationTimestamp: String = "DEFAULT",
    val isCalibrated: Boolean = false
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("device_model", deviceModel)
        obj.put("android_api", androidApi)
        obj.put("has_vibrator", hasVibrator)
        obj.put("has_amplitude_control", hasAmplitudeControl)
        obj.put("preferred_frequency_hz", preferredFrequencyHz.toDouble())
        obj.put("min_perceivable_intensity", minPerceivableIntensity.toDouble())
        obj.put("comfortable_continuous_intensity", comfortableContinuousIntensity.toDouble())
        obj.put("strong_event_intensity", strongEventIntensity.toDouble())
        obj.put("max_practical_intensity", maxPracticalIntensity.toDouble())
        obj.put("preferred_transient_duration_ms", preferredTransientDurationMs)
        obj.put("preferred_continuous_duration_ms", preferredContinuousDurationMs)
        obj.put("calibration_timestamp", calibrationTimestamp)
        obj.put("is_calibrated", isCalibrated)
        return obj.toString(2)
    }

    val isDefault: Boolean
        get() = !isCalibrated

    companion object {
        fun createDefault(
            deviceModel: String = "Reference Android Device",
            androidApi: Int = 34
        ): DeviceHapticProfile = DeviceHapticProfile(
            deviceModel = deviceModel,
            androidApi = androidApi,
            hasVibrator = true,
            hasAmplitudeControl = true,
            preferredFrequencyHz = 180.0f,
            minPerceivableIntensity = 0.35f,
            comfortableContinuousIntensity = 0.60f,
            strongEventIntensity = 0.82f,
            maxPracticalIntensity = 0.95f,
            preferredTransientDurationMs = 160,
            preferredContinuousDurationMs = 2500,
            calibrationTimestamp = "DEFAULT_BASELINE",
            isCalibrated = false
        )

        fun getConservativeDefault(): DeviceHapticProfile = createDefault()

        fun getStrongDefault(
            deviceModel: String = "Strong Calibrated Android Profile",
            androidApi: Int = 34
        ): DeviceHapticProfile = DeviceHapticProfile(
            deviceModel = deviceModel,
            androidApi = androidApi,
            hasVibrator = true,
            hasAmplitudeControl = true,
            preferredFrequencyHz = 180.0f,
            minPerceivableIntensity = 0.50f,
            comfortableContinuousIntensity = 0.72f,
            strongEventIntensity = 0.88f,
            maxPracticalIntensity = 1.00f,
            preferredTransientDurationMs = 160,
            preferredContinuousDurationMs = 2500,
            calibrationTimestamp = "V4_1_STRONG_DEFAULT",
            isCalibrated = true
        )

        fun fromJson(jsonString: String): DeviceHapticProfile {
            val obj = JSONObject(jsonString)
            return DeviceHapticProfile(
                deviceModel = obj.optString("device_model", "Generic Android Device"),
                androidApi = obj.optInt("android_api", 34),
                hasVibrator = obj.optBoolean("has_vibrator", true),
                hasAmplitudeControl = obj.optBoolean("has_amplitude_control", true),
                preferredFrequencyHz = obj.optDouble("preferred_frequency_hz", 180.0).toFloat(),
                minPerceivableIntensity = obj.optDouble("min_perceivable_intensity", 0.35).toFloat(),
                comfortableContinuousIntensity = obj.optDouble("comfortable_continuous_intensity", 0.60).toFloat(),
                strongEventIntensity = obj.optDouble("strong_event_intensity", 0.82).toFloat(),
                maxPracticalIntensity = obj.optDouble("max_practical_intensity", 0.95).toFloat(),
                preferredTransientDurationMs = obj.optInt("preferred_transient_duration_ms", 160),
                preferredContinuousDurationMs = obj.optInt("preferred_continuous_duration_ms", 2500),
                calibrationTimestamp = obj.optString("calibration_timestamp", "UNSET"),
                isCalibrated = obj.optBoolean("is_calibrated", false)
            )
        }
    }
}

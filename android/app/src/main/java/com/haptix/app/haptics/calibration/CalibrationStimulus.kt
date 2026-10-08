package com.haptix.app.haptics.calibration

/**
 * Controlled calibration stimulus representing a specific physical actuator test.
 */
data class CalibrationStimulus(
    val testId: String,
    val name: String,
    val durationMs: Long,
    val intensity: Float,
    val frequencyHz: Float,
    val isContinuous: Boolean,
    val description: String
) {
    companion object {
        val ALL_STIMULI: List<CalibrationStimulus> = listOf(
            CalibrationStimulus("T01", "100ms Transient (0.30)", 100L, 0.30f, 180.0f, false, "Sub-threshold / perceptual floor assessment"),
            CalibrationStimulus("T02", "100ms Transient (0.45)", 100L, 0.45f, 180.0f, false, "Low-amplitude tactile boundary test"),
            CalibrationStimulus("T03", "100ms Transient (0.60)", 100L, 0.60f, 200.0f, false, "Moderate transient mechanical pulse"),
            CalibrationStimulus("T04", "100ms Transient (0.75)", 100L, 0.75f, 210.0f, false, "Strong transient click / paddle accent"),
            CalibrationStimulus("T05", "100ms Transient (0.90)", 100L, 0.90f, 220.0f, false, "High-amplitude impact transient"),
            CalibrationStimulus("T06", "200ms Transient (0.60)", 200L, 0.60f, 190.0f, false, "Extended transient contact pulse"),
            CalibrationStimulus("T07", "400ms Continuous (0.40)", 400L, 0.40f, 150.0f, true, "Light continuous texture envelope"),
            CalibrationStimulus("T08", "400ms Continuous (0.60)", 400L, 0.60f, 170.0f, true, "Moderate continuous surface vibration"),
            CalibrationStimulus("T09", "400ms Continuous (0.80)", 400L, 0.80f, 180.0f, true, "Strong continuous kinetic texture"),
            CalibrationStimulus("T10", "1000ms Continuous (0.50)", 1000L, 0.50f, 140.0f, true, "Sustained moderate propulsion rumble"),
            CalibrationStimulus("T11", "1000ms Continuous (0.70)", 1000L, 0.70f, 160.0f, true, "Sustained high-energy motion texture"),
            CalibrationStimulus("T12", "1000ms Continuous (0.90)", 1000L, 0.90f, 180.0f, true, "Sustained peak acceleration surge")
        )
    }
}

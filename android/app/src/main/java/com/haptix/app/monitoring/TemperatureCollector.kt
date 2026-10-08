package com.haptix.app.monitoring

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.io.File

/**
 * Strategy interface for measuring device thermal temperature.
 */
interface TemperatureCollector {
    /**
     * Samples the current device temperature.
     *
     * @return Temperature in degrees Celsius, or null if sensor reading is unavailable.
     */
    fun sampleTemperature(): Float?
}

/**
 * Android implementation of [TemperatureCollector].
 *
 * Measurement Details & Source Hierarchy:
 * 1. Battery Hardware Thermal Sensor ([BatteryManager.EXTRA_TEMPERATURE]):
 *    Uses Android's sticky system broadcast [Intent.ACTION_BATTERY_CHANGED] via `context.registerReceiver(null, ...)`.
 *    Accessible to standard unprivileged applications without special permissions.
 *    The returned value represents internal device battery/chassis temperature in tenths of a degree Celsius
 *    (e.g., 348 represents 34.8°C), and is normalized to degrees Celsius by dividing by 10.0f.
 * 2. Linux Thermal Zone Fallback (/sys/class/thermal/thermal_zone0/temp):
 *    If the battery intent is unavailable (e.g. running on non-standard hardware), checks the thermal zone
 *    node if accessible, normalized from millidegrees Celsius.
 * 3. Unavailable:
 *    If neither hardware reading can be obtained (e.g. headless environment or missing sensor), returns null.
 *    Values are never fabricated.
 */
class AndroidTemperatureCollector(
    private val context: Context? = null,
    private val batteryIntentProvider: () -> Intent? = {
        try {
            context?.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Throwable) {
            null
        }
    },
    private val thermalFileReader: (String) -> String? = { path ->
        try {
            val file = File(path)
            if (file.exists() && file.canRead()) file.readText().trim() else null
        } catch (_: Throwable) {
            null
        }
    }
) : TemperatureCollector {

    override fun sampleTemperature(): Float? {
        // Source 1: Android BatteryManager sticky broadcast
        val intent = batteryIntentProvider()
        if (intent != null) {
            val rawTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            if (rawTemp != Int.MIN_VALUE && rawTemp > 0) {
                // Convert tenths of degree Celsius to Celsius
                val celsius = rawTemp / 10.0f
                if (celsius in -20.0f..100.0f) {
                    return celsius
                }
            }
        }

        // Source 2: Linux thermal zone kernel sysfs node (millidegrees Celsius)
        val thermalContent = thermalFileReader("/sys/class/thermal/thermal_zone0/temp")
        if (thermalContent != null) {
            val rawVal = thermalContent.toDoubleOrNull()
            if (rawVal != null && rawVal > 0) {
                val celsius = if (rawVal > 1000.0) (rawVal / 1000.0).toFloat() else rawVal.toFloat()
                if (celsius in -20.0f..100.0f) {
                    return celsius
                }
            }
        }

        return null
    }
}

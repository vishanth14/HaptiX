package com.haptix.app.monitoring

import java.io.File

/**
 * Strategy interface for measuring GPU utilization.
 */
interface GpuCollector {
    /**
     * Samples the GPU utilization percentage.
     *
     * @return GPU busy percentage (0.0f - 100.0f), or null if the hardware/driver does not expose
     *         accessible GPU utilization to unprivileged applications.
     */
    fun sampleGpuUsage(): Float?
}

/**
 * Conservative Android implementation of [GpuCollector].
 *
 * Technical Methodology & Android Constraints:
 * 1. Standard Android OS (AOSP) does NOT provide a public unprivileged SDK API for querying GPU utilization.
 * 2. On certain chipsets with vendor kernel sysfs interfaces accessible to applications, known nodes are checked:
 *    - Qualcomm Adreno: /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage or /sys/class/kgsl/kgsl-3d0/gpubusy
 *    - ARM Mali: /sys/class/misc/mali0/device/utilization or /sys/devices/platform/mali.0/utilization
 *    - Generic kernel: /sys/kernel/gpu/gpu_busy
 * 3. SELinux & Non-Root Enforcement: On standard modern Android devices, SELinux policy blocks third-party apps
 *    from reading GPU driver sysfs entries. When these entries are unreadable or inaccessible, this collector
 *    STRICTLY returns null.
 * 4. Honesty Contract: GPU utilization is NEVER fabricated, estimated from CPU, or defaulted to 0%.
 */
class AndroidGpuCollector(
    private val candidatePaths: List<String> = listOf(
        "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
        "/sys/class/kgsl/kgsl-3d0/gpubusy",
        "/sys/class/misc/mali0/device/utilization",
        "/sys/devices/platform/mali.0/utilization",
        "/sys/kernel/gpu/gpu_busy"
    ),
    private val fileReader: (String) -> String? = { path ->
        try {
            val file = File(path)
            if (file.exists() && file.canRead()) file.readText().trim() else null
        } catch (_: Throwable) {
            null
        }
    }
) : GpuCollector {

    override fun sampleGpuUsage(): Float? {
        for (path in candidatePaths) {
            val content = fileReader(path) ?: continue
            val parsed = parseGpuContent(path, content)
            if (parsed != null) {
                return parsed.coerceIn(0.0f, 100.0f)
            }
        }
        return null
    }

    private fun parseGpuContent(path: String, content: String): Float? {
        return try {
            // Format 1: Direct numeric percentage (e.g., "45" or "45.2%")
            val clean = content.removeSuffix("%").trim()
            if (clean.matches(Regex("^[0-9]+(\\.[0-9]+)?$"))) {
                return clean.toFloat()
            }

            // Format 2: kgsl gpubusy: "busy_cycles total_cycles"
            if (path.contains("gpubusy")) {
                val parts = clean.split("\\s+".toRegex())
                if (parts.size >= 2) {
                    val busy = parts[0].toDoubleOrNull() ?: return null
                    val total = parts[1].toDoubleOrNull() ?: return null
                    if (total > 0) {
                        return ((busy / total) * 100.0).toFloat()
                    }
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }
}

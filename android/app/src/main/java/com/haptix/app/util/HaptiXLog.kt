package com.haptix.app.util

/**
 * Diagnostic logger for HaptiX video and haptic synchronization.
 *
 * Formats log entries with the standard `[HaptiX]` prefix and delegates to [android.util.Log.d]
 * on Android runtimes, while falling back safely to standard output during unit tests where
 * the Android framework classes are not mocked.
 */
object HaptiXLog {
    const val TAG = "HaptiX"
    var isEnabled: Boolean = true

    /**
     * Emits a debug log entry if logging is enabled.
     */
    fun d(message: String) {
        if (!isEnabled) return
        try {
            android.util.Log.d(TAG, message)
        } catch (_: Throwable) {
            // JVM / unit test environment fallback
            println("[$TAG] $message")
        }
    }

    /**
     * Emits an info log entry if logging is enabled.
     */
    fun i(message: String) {
        if (!isEnabled) return
        try {
            android.util.Log.i(TAG, message)
        } catch (_: Throwable) {
            println("[$TAG] $message")
        }
    }

    /**
     * Emits a warning log entry if logging is enabled.
     */
    fun w(message: String) {
        if (!isEnabled) return
        try {
            android.util.Log.w(TAG, message)
        } catch (_: Throwable) {
            System.err.println("[$TAG] WARN: $message")
        }
    }

    /**
     * Emits an error log entry.
     */
    fun e(message: String, throwable: Throwable? = null) {
        if (!isEnabled) return
        try {
            android.util.Log.e(TAG, message, throwable)
        } catch (_: Throwable) {
            System.err.println("[$TAG] ERROR: $message ${throwable?.message ?: ""}")
            throwable?.printStackTrace()
        }
    }
}

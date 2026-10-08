package com.haptix.app.ui.screens.video

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.haptix.app.util.HaptiXLog

/**
 * Centralized system UI and window controller managing immersive sticky fullscreen
 * for video playback across Android versions (API 26–35+).
 *
 * Responsibilities:
 * - Hides status bar and navigation bars when entering landscape / fullscreen mode.
 * - Restores standard system bars upon returning to portrait or navigating away.
 * - Uses [WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE] so edge swipes
 *   temporarily reveal bars without jarring video interruptions.
 * - Supports idempotent re-application after configuration and orientation changes.
 */
object PlayerSystemUiHelper {

    /**
     * Toggles system bar visibility based on the desired fullscreen state.
     *
     * @param activity The host [Activity] hosting the window.
     * @param isFullscreen Whether the video player is in fullscreen / landscape mode.
     */
    fun setFullscreen(activity: Activity?, isFullscreen: Boolean) {
        if (activity == null) return
        val window = activity.window ?: return

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (isFullscreen) {
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            HaptiXLog.d("System UI: Immersive sticky fullscreen activated")
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            HaptiXLog.d("System UI: System bars restored")
        }
    }

    /**
     * Re-applies immersive sticky fullscreen if currently in fullscreen mode,
     * necessary when Android automatically restores bars after window focus or orientation changes.
     */
    fun reapplyIfFullscreen(activity: Activity?, isFullscreen: Boolean) {
        if (isFullscreen && activity != null) {
            setFullscreen(activity, true)
        }
    }
}

package com.haptix.app.ui.theme

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * CompositionLocal providing user or system preference for reduced motion.
 * When true, animations such as Breathe, Wiggle, and Bounce are suppressed or minimized.
 */
val LocalReducedMotion = compositionLocalOf { false }

/**
 * Native Android UI tactile interaction feedback helper.
 *
 * ARCHITECTURAL NOTICE:
 * This is strictly for standard OS-level touch affordance (button taps, selection toggles,
 * rating star clicks). It operates through Android's [View.performHapticFeedback] and is
 * COMPLETELY DECOUPLED from the HaptiX experimental audiovisual haptic playback engine
 * (com.haptix.app.haptics.AndroidHapticPlayer).
 */
object InteractionHaptics {

    /**
     * Performs subtle tap haptic feedback on button interaction.
     */
    fun performTap(view: View) {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        } catch (_: Exception) {}
    }

    /**
     * Performs subtle selection click on toggles, checkboxes, and tabs.
     */
    fun performSelection(view: View) {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } catch (_: Exception) {}
    }

    /**
     * Performs confirmation haptic feedback on successful form completion or finish.
     */
    fun performConfirmation(view: View) {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } catch (_: Exception) {
            try {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            } catch (_: Exception) {}
        }
    }

    /**
     * Performs rejection/warning haptic feedback on validation error.
     */
    fun performWarning(view: View) {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } catch (_: Exception) {
            try {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            } catch (_: Exception) {}
        }
    }
}

/**
 * Convenient Composable helper returning a rememberable lambda for UI haptic actions.
 */
@Composable
fun rememberUiHaptics(): UiHapticController {
    val view = LocalView.current
    return UiHapticController(view)
}

class UiHapticController(private val view: View) {
    fun tap() = InteractionHaptics.performTap(view)
    fun selection() = InteractionHaptics.performSelection(view)
    fun confirm() = InteractionHaptics.performConfirmation(view)
    fun warning() = InteractionHaptics.performWarning(view)
}

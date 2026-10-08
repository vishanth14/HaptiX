package com.haptix.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Centralized motion tokens implementing Apple-style spring physics.
 * Replaces linear/abrupt transitions with mass, tension, and damping.
 */
object MotionTokens {
    // Target scale values for interaction physics
    const val PressedScale: Float = 0.96f
    const val OvershootScale: Float = 1.015f
    const val SettledScale: Float = 1.0f
    const val LensingScale: Float = 1.02f

    /**
     * Instant: For immediate micro-responses (e.g. initial press contact).
     * High stiffness, critically damped.
     */
    val Instant: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    /**
     * Fast: For rapid toggles and quick feedback.
     */
    val Fast: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * Interactive: Natural Apple bounce response for taps, buttons, and cards.
     * Produces a gentle 0.96 -> 1.015 -> 1.0 tactile spring settlement.
     */
    val Interactive: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /**
     * Standard: For content sliding, disclosure expansion, and state changes.
     */
    val Standard: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    /**
     * Emphasized: For screen entries, hero surfaces, and prominent modal presentations.
     */
    val Emphasized: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessVeryLow
    )

    /**
     * Color transition spring specification.
     */
    fun <T> colorSpring(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

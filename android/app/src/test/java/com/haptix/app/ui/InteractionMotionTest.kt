package com.haptix.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.ui.unit.dp
import com.haptix.app.ui.components.motion.WiggleState
import com.haptix.app.ui.theme.MaterialLevel
import com.haptix.app.ui.theme.MaterialTokens
import com.haptix.app.ui.theme.MotionTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Apple-inspired Motion, Physics, and Material Tokens.
 */
class InteractionMotionTest {

    @Test
    fun motionTokens_targetScales_matchDesignSpecification() {
        assertEquals(0.96f, MotionTokens.PressedScale, 0.001f)
        assertEquals(1.015f, MotionTokens.OvershootScale, 0.001f)
        assertEquals(1.0f, MotionTokens.SettledScale, 0.001f)
        assertEquals(1.02f, MotionTokens.LensingScale, 0.001f)
    }

    @Test
    fun motionTokens_springPhysics_haveCorrectDampingAndStiffness() {
        // Interactive spring must have medium bounciness for natural tactile settlement
        assertEquals(Spring.DampingRatioMediumBouncy, MotionTokens.Interactive.dampingRatio, 0.001f)
        assertEquals(Spring.StiffnessMediumLow, MotionTokens.Interactive.stiffness, 0.001f)

        // Instant must be critically damped without overshoot on initial touch contact
        assertEquals(Spring.DampingRatioNoBouncy, MotionTokens.Instant.dampingRatio, 0.001f)
        assertEquals(Spring.StiffnessHigh, MotionTokens.Instant.stiffness, 0.001f)

        // Fast spring for toggles
        assertEquals(Spring.DampingRatioLowBouncy, MotionTokens.Fast.dampingRatio, 0.001f)
        assertEquals(Spring.StiffnessMedium, MotionTokens.Fast.stiffness, 0.001f)
    }

    @Test
    fun materialTokens_dimensions_matchAppleGuidelines() {
        assertEquals(0.5.dp, MaterialTokens.HairlineBorderWidth)
        assertEquals(12.dp, MaterialTokens.SmallShapeRadius)
        assertEquals(18.dp, MaterialTokens.MediumShapeRadius)
        assertEquals(22.dp, MaterialTokens.LargeShapeRadius)
        assertEquals(28.dp, MaterialTokens.PillShapeRadius)
    }

    @Test
    fun materialLevels_containsAllExpectedTiers() {
        val levels = MaterialLevel.values()
        assertTrue(levels.contains(MaterialLevel.UltraThin))
        assertTrue(levels.contains(MaterialLevel.Thin))
        assertTrue(levels.contains(MaterialLevel.Regular))
        assertTrue(levels.contains(MaterialLevel.Thick))
        assertTrue(levels.contains(MaterialLevel.Chrome))
    }

    @Test
    fun wiggleState_incrementsTriggerCountOnDemand() {
        val state = WiggleState()
        assertEquals(0L, state.triggerCount)
        state.trigger()
        assertEquals(1L, state.triggerCount)
        state.trigger()
        assertEquals(2L, state.triggerCount)
    }
}

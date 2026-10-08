package com.haptix.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Geometric shape tokens calibrated for the HaptiX research instrument.
 * Replaces generic oversized rounded corners with disciplined, precision geometry.
 */
object HaptiXShapeTokens {
    val micro = RoundedCornerShape(8.dp)       // Badges, micro controls (6-8dp)
    val input = RoundedCornerShape(12.dp)      // Text fields, intake boxes (10-12dp)
    val card = RoundedCornerShape(14.dp)       // Specimen cards, telemetry panels (14-16dp)
    val media = RoundedCornerShape(16.dp)      // Media viewports, video surface (16-20dp)
    val hero = RoundedCornerShape(20.dp)       // Hero canvas, dominant visual (20dp max)
    val button = RoundedCornerShape(12.dp)     // Precision research actions (10-14dp, NOT giant pills)
    val circle = CircleShape                   // Circular controls (play, status indicator, theme toggle)
}

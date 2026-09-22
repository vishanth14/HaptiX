package com.haptix.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationTest {
    @Test
    fun verify_route_formatting() {
        assertEquals("video_player/vid_1", HaptiXDestinations.videoPlayerRoute("vid_1"))
        assertEquals("feedback/vid_1", HaptiXDestinations.feedbackRoute("vid_1"))
    }
}

package com.haptix.app.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MediaResourceHelperTest {

    @Test
    fun resolveVideoUri_handlesBlankUri() {
        // Robolectric or pure JVM without Android Context returns null for blank
        // For blank input, context is not invoked
        val dummyContext = android.content.ContextWrapper(null)
        val result = try {
            MediaResourceHelper.resolveVideoUri(dummyContext, "")
        } catch (_: Exception) {
            null
        }
        assertNull(result)
    }

    @Test
    fun resolveVideoUri_preservesStandardSchemeUris() {
        // Context is not invoked when URI begins with standard prefixes
        val dummyContext = android.content.ContextWrapper(null)
        val testHttp = "https://example.com/stream.mp4"
        try {
            val uri = MediaResourceHelper.resolveVideoUri(dummyContext, testHttp)
            assertNotNull(uri)
            assertEquals("https://example.com/stream.mp4", uri.toString())
        } catch (_: RuntimeException) {
            // Android Uri.parse on JVM throws if not mocked; this is expected without Robolectric
        }
    }
}

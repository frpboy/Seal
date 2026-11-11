package com.junkfood.seal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickDownloadActivityTest {
    @Test
    fun acceptsOnlyHttpUrlsWithHosts() {
        assertTrue(isSupportedShareUrl("https://example.com/watch?v=1"))
        assertFalse(isSupportedShareUrl(""))
        assertFalse(isSupportedShareUrl("https://"))
        assertFalse(isSupportedShareUrl("javascript:alert(1)"))
        assertFalse(isSupportedShareUrl("ftp://example.com/video"))
    }
}

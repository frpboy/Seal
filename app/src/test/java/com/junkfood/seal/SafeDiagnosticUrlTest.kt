package com.junkfood.seal

import com.junkfood.seal.util.SafeDiagnosticUrl
import org.junit.Assert.*
import org.junit.Test

class SafeDiagnosticUrlTest {
    @Test fun `media identifiers and playlist capabilities are withheld`() {
        for (url in listOf("https://youtube.com/watch?v=TJBXF0p4cxw&list=PL_private_123&token=SECRET", "https://youtu.be/TJBXF0p4cxw?si=SECRET", "https://m.youtube.com/shorts/TJBXF0p4cxw", "https://youtube.com/live/TJBXF0p4cxw")) {
            val result = SafeDiagnosticUrl.describe(url)
            for (secret in listOf("TJBXF0p4cxw", "PL_private_123", "SECRET")) assertFalse(result.contains(secret))
            assertTrue(result.contains("withheld"))
        }
    }
    @Test fun `unknown bearer paths credentials queries and fragments are withheld`() {
        val result = SafeDiagnosticUrl.describe("https://user:SECRET@example.org/private/SECRET?token=SECRET#SECRET")
        assertTrue(result.startsWith("https://example.org/"))
        assertFalse(result.contains("SECRET")); assertFalse(result.contains("user:"))
        assertTrue(result.contains("origin may itself be sensitive: inspect before sharing"))
    }
    @Test fun `invalid and unsupported URLs are withheld`() {
        for (url in listOf("https://example.org/\nSECRET", "file:///private/SECRET", "intent://SECRET", "javascript:SECRET", "https:SECRET", "https://example.org\\SECRET")) {
            val result = SafeDiagnosticUrl.describe(url)
            assertTrue(result.contains("withheld")); assertFalse(result.contains("SECRET"))
        }
        assertEquals("URL unavailable.", SafeDiagnosticUrl.describe(null))
    }
}

package com.junkfood.seal

import com.junkfood.seal.util.SafeDiagnosticUrl
import org.junit.Assert.*
import org.junit.Test

class SafeDiagnosticUrlTest {
    @Test fun `public YouTube identifiers survive without private parameters`() {
        for (url in listOf(
            "https://youtube.com/watch?v=TJBXF0p4cxw&token=SECRET#private",
            "https://youtu.be/TJBXF0p4cxw?si=SECRET",
            "https://m.youtube.com/shorts/TJBXF0p4cxw?signature=SECRET",
            "https://youtube.com/live/TJBXF0p4cxw?x=SECRET",
        )) {
            val result = SafeDiagnosticUrl.describe(url)
            assertTrue(result.contains("watch?v=TJBXF0p4cxw"))
            assertFalse(result.contains("SECRET"))
            assertFalse(result.contains("private"))
        }
    }
    @Test fun `unknown hosts and shortlinks retain origin not bearer paths`() {
        for (url in listOf("https://example.org/private/SECRET?token=SECRET", "https://bit.ly/SECRET", "https://youtube.com.evil.test/SECRET", "https://youtube.com/redirect?q=SECRET")) {
            val result = SafeDiagnosticUrl.describe(url)
            assertTrue(result.contains("https://"))
            assertTrue(result.contains("withheld"))
            assertFalse(result.contains("SECRET"))
        }
    }
    @Test fun `credentials are removed even on recognized public URLs`() {
        val result = SafeDiagnosticUrl.describe("https://user:SECRET@youtube.com/watch?v=TJBXF0p4cxw")
        assertTrue(result.contains("TJBXF0p4cxw"))
        assertFalse(result.contains("user:"))
        assertFalse(result.contains("SECRET"))
    }
    @Test fun `controls nonHTTP deeplinks malformed and opaque URLs are withheld`() {
        for (url in listOf("https://example.org/\nSECRET", "file:///private/SECRET", "intent://SECRET", "javascript:SECRET", "https:SECRET", "https://user%40evil.test@/SECRET", "https://example.org\\SECRET")) {
            val result = SafeDiagnosticUrl.describe(url)
            assertTrue(result.contains("withheld"))
            assertFalse(result.contains("SECRET"))
        }
    }
    @Test fun `safe reproduction options survive but secret options do not`() {
        val result = SafeDiagnosticUrl.describe("https://youtube.com/watch?v=TJBXF0p4cxw&t=1h2m3s&start=12&end=98&index=2&list=PL_public_123&sig=SECRET&token=SECRET#SECRET")
        for (option in listOf("t=1h2m3s", "start=12", "end=98", "index=2", "list=PL_public_123"))
            assertTrue(result.contains(option))
        assertFalse(result.contains("SECRET"))
        assertTrue(result.contains("normalized"))
        assertTrue(result.contains("redacted"))
    }
    @Test fun `ambiguous duplicate query IDs and encoded paths do not leak`() {
        for (url in listOf("https://youtube.com/watch?v=TJBXF0p4cxw&v=SECRET", "https://youtu.be/%53ECRET", "https://youtube.com/watch?v=SECRET", "https://youtube.com/shorts/private/SECRET"))
            assertFalse(SafeDiagnosticUrl.describe(url).contains("SECRET"))
        assertEquals("URL unavailable.", SafeDiagnosticUrl.describe(null))
        val unknown = SafeDiagnosticUrl.describe("https://private-token.example.org/private")
        assertTrue(unknown.contains("origin may itself be sensitive: inspect before sharing"))
    }
}

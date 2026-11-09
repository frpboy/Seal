package com.junkfood.seal

import com.junkfood.seal.util.SafeErrorReport
import org.junit.Assert.*
import org.junit.Test

class SafeErrorReportTest {
    @Test fun `null message identifies exception instead of literal null`() {
        val error = IllegalStateException()
        val report = SafeErrorReport.describe(error)
        assertTrue(report.contains("IllegalStateException: No error message was provided."))
        assertFalse(report.contains("null"))
        assertNull(error.message)
    }
    @Test fun `nested cause survives as bounded safe description without modifying throwable`() {
        val cause = IllegalArgumentException("ERROR: Sign in to confirm you’re not a bot. https://private.test/path cookie=SECRET")
        val error = IllegalStateException(null, cause)
        val report = SafeErrorReport.describe(error)
        assertTrue(report.contains("Caused by: IllegalArgumentException: The server requested sign-in verification."))
        assertSame(cause, error.cause)
        assertFalse(report.contains("SECRET"))
        assertFalse(report.contains("private.test"))
    }
    @Test fun `unknown arbitrary output secrets and file paths are never copied`() {
        val report = SafeErrorReport.copyReport("App version: test\n", Exception("Authorization: Bearer ABC\nCookie: secret\n/home/user/private.mp4 https://site/video?token=KEY"))
        for (secret in listOf("ABC", "secret", "/home", "private.mp4", "https://", "KEY"))
            assertFalse(secret, report.contains(secret))
        assertTrue(report.contains("URL unavailable."))
        assertTrue(report.contains("Error details were withheld for privacy."))
    }
    @Test fun `cycle and long cause chains terminate`() {
        val a = Exception("a")
        val b = Exception("b")
        a.initCause(b); b.initCause(a)
        assertTrue(SafeErrorReport.describe(a).contains("Additional causes omitted."))
        var error: Throwable = Exception("bottom")
        repeat(12) { error = Exception(null, error) }
        assertEquals(5, SafeErrorReport.describe(error).lines().count { it.startsWith("Caused by:") })
    }
    @Test fun `known categories retain actionable generic detail without raw process text`() {
        val inputs = mapOf(
            "No supported JavaScript runtime could be found: /private/bin" to "JavaScript runtime",
            "Permission denied /private/a" to "Permission was denied",
            "No space left on device /private/b" to "insufficient storage",
            "HTTP Error 416: https://private" to "HTTP 416",
            "HTTP Error 403: token=X" to "HTTP 403",
            "operation timed out token=X" to "timed out",
            "Unable to resolve host private.test" to "could not be resolved",
        )
        for ((message, expected) in inputs) {
            val report = SafeErrorReport.describe(Exception(message))
            assertTrue(report.contains(expected))
            assertFalse(report.contains("/private"))
            assertFalse(report.contains("token="))
        }
    }
}

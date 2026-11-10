package com.frpboy.seali

import com.frpboy.seali.ui.page.downloadv2.queueTaskDisplayTitle
import com.frpboy.seali.util.SafeErrorReport
import org.junit.Assert.*
import org.junit.Test

class QueueErrorPresentationTest {
    @Test fun actualTitleWins() {
        assertEquals("Example title",queueTaskDisplayTitle("https://example.com/private?token=x","Example title"))
    }
    @Test fun fallbackOnlyHostNeverUserInfoPathQueryFragment() {
        val url="https://secret:password@EXAMPLE.com:443/private?token=abc#session"
        assertEquals("example.com",queueTaskDisplayTitle(url,url))
    }
    @Test fun invalidFallbacksAreWithheld() {
        for (url in listOf("file:///storage/private", "javascript:alert(1)","https://example.com/\nsecret", "https://example.com\\secret", "https:///token"))
            assertNull(queueTaskDisplayTitle(url,url))
    }
    @Test fun unknownErrorNeverShowsProcessOutput() {
        val message="SECRET cookie=abc https://secret.test/token password=hidden"
        val reason=SafeErrorReport.reason(IllegalStateException(message))
        assertEquals("Error details were withheld for privacy.", reason)
        assertFalse(reason.contains("SECRET"))
    }
    @Test fun nestedSafeReasonAndCycleAreBounded() {
        assertEquals("The server returned HTTP 403.",SafeErrorReport.reason(RuntimeException("wrapper",RuntimeException("HTTP Error 403: secret"))))
        val first=RuntimeException("private1");val second=RuntimeException("private2")
        first.initCause(second);second.initCause(first)
        assertEquals("Error details were withheld for privacy.",SafeErrorReport.reason(first))
    }
    @Test fun allKnownCategoriesRemainSafe() {
        val messages=listOf("Permission denied password=secret","No space left on device /private/file","timed out token=abc","Unable to resolve host secret.host","No supported JavaScript runtime private", "Sign in to confirm you're not a bot secret", "HTTP Error 416 secret")
        for (message in messages) {
            val result=SafeErrorReport.reason(RuntimeException(message))
            assertFalse(result.contains("secret"));assertFalse(result.contains("private"));assertFalse(result.contains("password"))
        }
    }
}

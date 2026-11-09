package com.frpboy.seali.ui.page.downloadv2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueProgressTelemetryTest {
    @Test fun keepsOnlyMeasuredFraction() {
        assertEquals(0f, measuredQueueProgress(0f))
        assertEquals(0.58f, measuredQueueProgress(0.58f))
        assertEquals(1f, measuredQueueProgress(1f))
        listOf(-1f, 1.1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach {
            assertNull(measuredQueueProgress(it))
        }
    }
    @Test fun extractsKnownDownloadSpeedAndEta() {
        assertEquals(QueueProgressTelemetry("1.25MiB/s", "00:15"),
            queueProgressTelemetry("[download] 12.0% of 10MiB at 1.25MiB/s ETA 00:15"))
        assertEquals(QueueProgressTelemetry("512 B/s", "1:00:15"),
            queueProgressTelemetry("[download] 5% at 512 B/s ETA 1:00:15"))
    }
    @Test fun unknownFieldsAreAbsentNotMadeUp() {
        assertEquals(QueueProgressTelemetry(null, null), queueProgressTelemetry("[download] 2% at Unknown B/s ETA Unknown"))
        assertEquals(QueueProgressTelemetry(null, null), queueProgressTelemetry("[download] 100% of 1MiB in 00:01"))
    }
    @Test fun rejectsUnrelatedAndSensitiveOutput() {
        listOf("https://private.test/key?token=abc", "WARNING at 2MiB/s ETA 00:01",
            "[download] at https://private.test/secret ETA token", "[download] at 2MiB/s\nETA 00:01",
            "[download]" + "x".repeat(1024)).forEach {
            assertEquals(QueueProgressTelemetry(null, null), queueProgressTelemetry(it))
        }
    }
    @Test fun boundedRecognizedFieldsDoNotEchoSurroundingContent() {
        assertEquals(QueueProgressTelemetry("2MiB/s", "00:01"),
            queueProgressTelemetry("[download] token=private at 2MiB/s ETA 00:01 cookie=secret"))
    }
    @Test fun rejectsMalformedNumbersAndEta() {
        assertEquals(QueueProgressTelemetry(null, null), queueProgressTelemetry("[download] at 1234567MiB/s ETA 99:99"))
        assertEquals(QueueProgressTelemetry(null, null), queueProgressTelemetry("[download] at 1.9999MiB/s ETA 1234:00:00"))
    }
}

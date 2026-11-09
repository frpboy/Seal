package com.frpboy.seali.ui.page.downloadv2

internal data class QueueProgressTelemetry(val speed: String?, val eta: String?)

/** Present only measured progress. Never turn unknown/non-finite input into zero or completion. */
internal fun measuredQueueProgress(progress: Float): Float? =
    progress.takeIf { it.isFinite() && it in 0f..1f }

/** Recognize bounded download speed/ETA fields; never display arbitrary command or diagnostic output. */
internal fun queueProgressTelemetry(line: String): QueueProgressTelemetry {
    if (line.length > 1024 || line.any { it.isISOControl() } || !line.startsWith("[download]")) {
        return QueueProgressTelemetry(null, null)
    }
    val speed = Regex("(?:^|\\s)at ([0-9]{1,6}(?:\\.[0-9]{1,3})? ?(?:[KMGT]i?B|B)/s)(?=\\s|$)")
        .find(line)?.groupValues?.get(1)
    val eta = Regex("(?:^|\\s)ETA ((?:[0-9]{1,3}:)?[0-5][0-9]:[0-5][0-9])(?=\\s|$)")
        .find(line)?.groupValues?.get(1)
    return QueueProgressTelemetry(speed, eta)
}

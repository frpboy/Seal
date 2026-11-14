package com.frpboy.seali.ui.page.downloadv2.configure

internal data class SelectedStreamSummary(
    val exactBytes: Double?,
    val approximateBytes: Double?,
    val container: String?,
    val videoCodec: String?,
    val audioCodec: String?,
)

/** Only a complete finite positive input-stream total. Never an output-file size promise. */
internal fun selectedStreamBytes(streams: List<SelectedStreamSummary>, transformed: Boolean): Long? {
    if (transformed || streams.isEmpty()) return null
    var total = 0.0
    for (stream in streams) {
        val size = stream.exactBytes?.takeIf { it.isFinite() && it > 0 }
            ?: stream.approximateBytes?.takeIf { it.isFinite() && it > 0 }
            ?: return null
        total += size
        if (!total.isFinite() || total >= Long.MAX_VALUE.toDouble()) return null
    }
    return total.toLong().takeIf { it > 0 }
}

/** Small literal codec/container tokens, never a format URL, free-form note or guessed metadata. */
internal fun selectedStreamBadges(streams: List<SelectedStreamSummary>): List<String> =
    streams.flatMap { listOfNotNull(it.container, it.videoCodec, it.audioCodec) }
        .filter { it != "none" && it.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,39}")) }
        .distinct().take(8)

package com.frpboy.seali.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class NightlyChannelSelection(
    val channel: Int = YT_DLP_STABLE,
    val nightlyAllowed: Boolean = false,
    val dontWarnAgain: Boolean = false,
)

/** An inherited Nightly value/default is not an explicit warning acceptance. */
object NightlyChannelPolicy {
    fun effectiveChannel(savedChannel: Int, nightlyAllowed: Boolean): Int =
        if (savedChannel == YT_DLP_NIGHTLY && nightlyAllowed) YT_DLP_NIGHTLY else YT_DLP_STABLE

    fun needsWarning(dontWarnAfterAllow: Boolean): Boolean = !dontWarnAfterAllow

    fun normalize(value: NightlyChannelSelection): NightlyChannelSelection = value.copy(
        channel = effectiveChannel(value.channel, value.nightlyAllowed),
        dontWarnAgain = value.nightlyAllowed && value.dontWarnAgain,
    )

    fun decode(raw: String?): NightlyChannelSelection = runCatching {
        raw?.let { normalize(Json.decodeFromString<NightlyChannelSelection>(it)) }
            ?: NightlyChannelSelection()
    }.getOrElse { NightlyChannelSelection() }

    fun encode(value: NightlyChannelSelection): String {
        require(value.channel != YT_DLP_NIGHTLY || value.nightlyAllowed)
        require(!value.dontWarnAgain || value.nightlyAllowed)
        return Json.encodeToString(normalize(value))
    }
}

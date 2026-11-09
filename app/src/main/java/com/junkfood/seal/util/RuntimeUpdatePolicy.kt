package com.junkfood.seal.util

/** Pure download admission policy. Old auto-update settings are not F-Droid consent. */
object RuntimeUpdatePolicy {
    fun mayDownload(
        isPlay: Boolean,
        isFDroid: Boolean,
        automaticEnabled: Boolean,
        automaticConsent: Boolean,
        manualConsent: Boolean,
    ): Boolean =
        !isPlay && (!isFDroid || manualConsent || (automaticEnabled && automaticConsent))
}

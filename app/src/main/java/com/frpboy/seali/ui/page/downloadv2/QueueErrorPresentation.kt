package com.frpboy.seali.ui.page.downloadv2

import java.net.URI
import java.util.Locale

/** Local display label only: no URL is opened, fetched or rewritten by this helper. */
internal fun queueTaskDisplayTitle(url: String, title: String): String? {
    if (title.isNotBlank() && title != url) return title
    if (url.length > 8192 || url.any { it.isISOControl() } || '\\' in url) return null
    val uri=runCatching { URI(url) }.getOrNull() ?: return null
    if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("http","https") || uri.isOpaque) return null
    return uri.host?.lowercase(Locale.ROOT)?.takeIf {
        it.matches(Regex("[a-z0-9.-]{1,253}")) && !it.startsWith('.') && !it.endsWith('.')
    }
}

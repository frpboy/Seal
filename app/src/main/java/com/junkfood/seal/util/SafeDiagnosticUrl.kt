package com.junkfood.seal.util

import java.net.URI
import java.util.Locale

/** Retains public media identifiers, never arbitrary bearer paths or signed query parameters. */
object SafeDiagnosticUrl {
    fun describe(raw: String?): String {
        if (raw.isNullOrBlank()) return "URL unavailable."
        if (raw.length > 8192 || raw.any { it.isISOControl() } || '\\' in raw)
            return "URL withheld: unsafe or unsupported URL."
        val uri = runCatching { URI(raw) }.getOrNull()
            ?: return "URL withheld: invalid URL."
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        val host = uri.host?.lowercase(Locale.ROOT)
        if (scheme !in setOf("https", "http") || host.isNullOrBlank() || uri.isOpaque || uri.port !in -1..65535 ||
            !host.matches(Regex("[a-z0-9.-]+")) || host.startsWith('.') || host.endsWith('.'))
            return "URL withheld: unsafe or unsupported URL."
        val origin = "$scheme://$host" + if (uri.port in 1..65535) ":${uri.port}" else ""
        val videoId = Regex("[A-Za-z0-9_-]{11}")
        val youtubeHosts = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com")
        val id = when {
            host == "youtu.be" -> uri.rawPath?.removePrefix("/")
            host in youtubeHosts && uri.rawPath == "/watch" -> {
                val values = uri.rawQuery.orEmpty().split('&').filter { it.startsWith("v=") }
                if (values.size == 1) values.single().removePrefix("v=") else null
            }
            host in youtubeHosts && (uri.rawPath.orEmpty().startsWith("/shorts/") ||
                uri.rawPath.orEmpty().startsWith("/live/")) ->
                uri.rawPath.split('/').takeIf { it.size == 3 }?.last()
            else -> null
        }
        if (id != null && videoId.matches(id)) {
            val options = uri.rawQuery.orEmpty().split('&').mapNotNull { entry ->
                val pair = entry.split('=', limit = 2)
                if (pair.size != 2) return@mapNotNull null
                val (key, value) = pair
                val allowed = when (key) {
                    "t" -> value.matches(Regex("(?:[0-9]{1,8}|(?:[0-9]{1,4}h)?(?:[0-9]{1,4}m)?(?:[0-9]{1,4}s)?)")) && value.isNotEmpty()
                    "start", "end", "index" -> value.matches(Regex("[0-9]{1,8}"))
                    "list" -> value.matches(Regex("[A-Za-z0-9_-]{2,150}"))
                    else -> false
                }
                if (allowed) "$key=$value" else null
            }
            val suffix = if (options.isEmpty()) "" else "&" + options.joinToString("&")
            return "https://www.youtube.com/watch?v=$id$suffix [normalized public URL; other details redacted]"
        }
        return "$origin/ [path, query and fragment withheld; origin may itself be sensitive: inspect before sharing; use existing Copy link locally for original]"
    }
}

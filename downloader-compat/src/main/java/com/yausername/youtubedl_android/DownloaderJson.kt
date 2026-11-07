package com.yausername.youtubedl_android

import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** JSON boundary for the local API24 compatibility investigation. No reflection. */
object DownloaderJson {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private fun checked(text: String): String {
        require(text.length <= 2 * 1024 * 1024) { "Metadata exceeds 2 MiB" }
        var depth = 0
        var quoted = false
        var escaped = false
        for (c in text) {
            if (quoted) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') quoted = false
            } else when (c) {
                '"' -> quoted = true
                '{', '[' -> { depth++; require(depth <= 64) { "Metadata nesting exceeds 64" } }
                '}', ']' -> depth--
            }
        }
        return text
    }
    fun videoInfo(text: String): VideoInfo = json.decodeFromString(checked(text))
    fun release(text: String): Release = json.decodeFromString(checked(text))
    fun release(reader: java.io.Reader): Release {
        val text = StringBuilder()
        val buffer = CharArray(8192)
        while (true) {
            val count = reader.read(buffer)
            if (count < 0) break
            require(text.length + count <= 2 * 1024 * 1024) { "Metadata exceeds 2 MiB" }
            text.append(buffer, 0, count)
        }
        return release(text.toString())
    }
    @Serializable data class Release(val tag_name: String, val name: String, val assets: List<Asset>) {
        fun downloadUrl(): String = assets.firstOrNull { it.name == "yt-dlp" }?.browser_download_url
            ?.takeIf { it.isNotBlank() } ?: throw YoutubeDLException("unable to get download url")
    }
    @Serializable data class Asset(val name: String, val browser_download_url: String)
}

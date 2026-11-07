package com.yausername.youtubedl_android

import org.junit.Test
import org.junit.Assert.*

class DownloaderJsonTest {
    @Test fun metadataAliasesUnknownNullAndNumericValues() {
        val item = DownloaderJson.videoInfo("""{"id":"fixture","title":null,"view_count":42,"format_id":"fmt","formats":[{"format_id":"nested","filesize":1234}],"unknown":{"safe":true}}""")
        assertNull(item.title)
        assertEquals("fixture", item.id)
        assertEquals("42", item.viewCount)
        assertEquals("fmt", item.formatId)
        assertEquals("nested", item.formats!![0].formatId)
        assertEquals(1234L, item.formats!![0].fileSize)
    }
    @Test fun updaterFindsExactAssetAndIgnoresExtraFields() {
        val item = DownloaderJson.release("""{"tag_name":"v1","name":"Release","unknown":1,"assets":[{"name":"yt-dlp.exe","browser_download_url":"wrong"},{"name":"yt-dlp","browser_download_url":"https://example.com/yt-dlp"}]}""".reader())
        assertEquals("v1", item.tag_name)
        assertEquals("https://example.com/yt-dlp", item.downloadUrl())
    }
    @Test fun missingAssetIsRejected() {
        try { DownloaderJson.release("""{"tag_name":"v1","name":"Release","assets":[]}""").downloadUrl(); fail("must reject") }
        catch (expected: YoutubeDLException) {}
    }
    @Test fun missingOrWrongFieldsAreRejected() {
        for (text in listOf("{}", """{"tag_name":[],"name":"Release","assets":[]}""", "{")) {
            try { DownloaderJson.release(text); fail("must reject") }
            catch (expected: kotlinx.serialization.SerializationException) {}
        }
    }
    @Test fun oversizedAndNestedMetadataAreRejected() {
        for (text in listOf(" ".repeat(2 * 1024 * 1024 + 1), "[".repeat(65) + "]".repeat(65))) {
            try { DownloaderJson.release(text.reader()); fail("must reject") }
            catch (expected: IllegalArgumentException) {}
        }
    }
    @Test fun bracesInsideEscapedStringsAreNotDepth() {
        val title = "braces: \"" + "[".repeat(100)
        val text = kotlinx.serialization.json.Json.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), kotlinx.serialization.json.buildJsonObject { put("title",kotlinx.serialization.json.JsonPrimitive(title)) })
        assertEquals(title, DownloaderJson.videoInfo(text).title)
    }
}

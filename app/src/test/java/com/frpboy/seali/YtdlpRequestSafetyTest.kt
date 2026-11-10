package com.frpboy.seali

import com.frpboy.seali.util.enforceNativeManifestDownloads
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.junit.Assert.*
import org.junit.Test

class YtdlpRequestSafetyTest {
    @Test fun aria2StillWorksForDirectFilesButFenceIsLast() {
        val request = YoutubeDLRequest("https://example.invalid/media")
            .addOption("--downloader", "libaria2c.so")
            .enforceNativeManifestDownloads()
        assertEquals(listOf("--downloader", "libaria2c.so", "--downloader", "dash,m3u8:native",
            "https://example.invalid/media"), request.buildCommand())
    }
    @Test fun customConfigIsFencedEvenWithoutAria2Preference() {
        val request = YoutubeDLRequest("https://example.invalid/media")
            .addOption("--config-locations", "/fixture/config")
            .enforceNativeManifestDownloads()
        assertEquals(listOf("--downloader", "dash,m3u8:native"), request.buildCommand().takeLast(3).take(2))
    }
    @Test fun priorExplicitProtocolAndCustomTokensCannotOverrideFinalFence() {
        val request = YoutubeDLRequest("https://example.invalid/media")
            .addOption("--downloader", "dash,m3u8:libaria2c.so")
            .addCommands(listOf("--downloader", "dash,m3u8:aria2c"))
            .enforceNativeManifestDownloads()
        assertEquals(listOf("--downloader", "dash,m3u8:native"), request.buildCommand().takeLast(3).take(2))
    }
    @Test fun chainingPreservesSameRequestAndUrls() {
        val request = YoutubeDLRequest(listOf("https://example.invalid/1", "https://example.invalid/2"))
        assertSame(request, request.enforceNativeManifestDownloads())
        assertEquals(listOf("https://example.invalid/1", "https://example.invalid/2"),request.buildCommand().takeLast(2))
    }
    @Test fun nativeFenceIsLastDownloaderOverrideAndPreservesOtherOptions() {
        val urls = listOf("https://example.invalid/1", "https://example.invalid/2")
        val request = YoutubeDLRequest(urls)
            .addOption("--external-downloader", "libaria2c.so")
            .addOption("--downloader", "aria2c")
            .addOption("--no-mtime")
            .enforceNativeManifestDownloads()

        val command = request.buildCommand()
        val lastDownloader = command.indexOfLast { it == "--downloader" }
        assertTrue(lastDownloader >= 0)
        assertEquals("dash,m3u8:native", command[lastDownloader + 1])
        assertTrue(command.indexOf("--external-downloader") < lastDownloader)
        assertTrue(command.indexOf("--no-mtime") < lastDownloader)
        assertEquals(1, command.count { it == "dash,m3u8:native" })
        assertEquals(urls, command.takeLast(urls.size))
    }

    @Test fun repeatedFenceRemainsSafeAndDoesNotDropOptions() {
        val request = YoutubeDLRequest("https://example.invalid/media").addOption("--no-mtime")
        request.enforceNativeManifestDownloads().enforceNativeManifestDownloads()
        assertTrue(request.buildCommand().contains("--no-mtime"))
        assertEquals(2,request.buildCommand().count { it == "dash,m3u8:native" })
    }
}

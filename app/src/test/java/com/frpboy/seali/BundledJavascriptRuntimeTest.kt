package com.frpboy.seali

import com.frpboy.seali.util.enforceBundledJavascriptRuntime
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BundledJavascriptRuntimeTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun finalArgumentsOverrideEarlierRuntimeAndRemoteComponents() {
        val runtime = tmp.newFile("libqjs.so")
        assertTrue(runtime.setExecutable(true, true))
        val request = YoutubeDLRequest("https://example.invalid/media")
            .addOption("--js-runtimes", "node:/untrusted")
            .addCommands(listOf("--remote-components", "ejs:github", "--extractor-args", "youtube-ejs:dev=true;repo=untrusted/repo;script_version=1"))
        assertSame(request, request.enforceBundledJavascriptRuntime(tmp.root.absolutePath))
        assertEquals(
            listOf("--no-js-runtimes", "--js-runtimes", "quickjs:${runtime.absolutePath}", "--no-remote-components", "--extractor-args", "youtube-ejs:dev=false"),
            request.buildCommand().dropLast(1).takeLast(6),
        )
        assertEquals("https://example.invalid/media", request.buildCommand().last())
    }

    @Test fun absentRuntimeFailsClearlyWithoutStartingDownload() {
        val request = YoutubeDLRequest("https://example.invalid/media")
        try {
            request.enforceBundledJavascriptRuntime(tmp.root.absolutePath)
            fail("Missing packaged runtime must not be silently ignored")
        } catch (expected: IllegalStateException) {
            assertEquals("Bundled JavaScript runtime unavailable", expected.message)
        }
    }
}

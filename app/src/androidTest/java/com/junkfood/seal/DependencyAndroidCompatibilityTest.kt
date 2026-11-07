package com.junkfood.seal

import com.yausername.youtubedl_android.DownloaderJson
import com.yausername.youtubedl_android.mapper.VideoInfo
import com.yausername.youtubedl_common.utils.ZipUtils
import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Files
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DependencyAndroidCompatibilityTest {
    @Test fun bundledDownloaderStillStartsOffline() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val downloader = com.yausername.youtubedl_android.YoutubeDL.getInstance()
        downloader.init(context)
        val response = downloader.execute(com.yausername.youtubedl_android.YoutubeDLRequest(listOf("--version")))
        assertEquals(0, response.exitCode)
        assertTrue(response.out.trim().isNotEmpty())
    }
    @Test fun upstreamVideoInfoDeserializesWithUnknownFields() {
        val result = DownloaderJson.videoInfo("""{"id":"fixture","title":"Sample","duration":12,"formats":[],"unknown_fixture":{"x":1}}""")
        assertEquals("Sample", result.title)
        assertEquals("fixture", result.id)
    }
    @Test fun upstreamUpdaterTreeShapeStillParses() {
        val tree = DownloaderJson.release("""{"tag_name":"fixture","name":"Fixture release","assets":[{"name":"yt-dlp","browser_download_url":"https://example.com/yt-dlp"}]}""")
        assertEquals("fixture", tree.tag_name)
        assertEquals("yt-dlp", tree.assets[0].name)
    }
    private fun zip(name: String): ByteArray = ByteArrayOutputStream().also { bytes ->
        ZipOutputStream(bytes).use { z -> z.putNextEntry(ZipEntry(name)); z.write("fixture".toByteArray()); z.closeEntry() }
    }.toByteArray()
    @Test fun upstreamStreamAndFileZipPathsStillExtract() {
        val root = java.io.File(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "archive-${java.util.UUID.randomUUID()}").apply { mkdirs() }
        try {
            val archive = java.io.File(root, "fixture.zip").apply { writeBytes(zip("dir/sample.txt")) }
            val streamDir = java.io.File(root, "stream").apply { mkdirs() }
            val fileDir = java.io.File(root, "file").apply { mkdirs() }
            ZipUtils.unzip(ByteArrayInputStream(archive.readBytes()), streamDir)
            ZipUtils.unzip(archive, fileDir)
            assertEquals("fixture", java.io.File(streamDir,"dir/sample.txt").readText())
            assertEquals("fixture", java.io.File(fileDir,"dir/sample.txt").readText())
        } finally { root.deleteRecursively() }
    }
    @Test fun upstreamZipSlipGuardRejectsTraversal() {
        val root = java.io.File(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "reject-${java.util.UUID.randomUUID()}").apply { mkdirs() }
        try {
            try { ZipUtils.unzip(ByteArrayInputStream(zip("../escape.txt")), root); fail("must reject traversal") }
            catch (expected: IllegalAccessException) { assertFalse(java.io.File(root.parentFile,"escape.txt").exists()) }
        } finally { root.deleteRecursively() }
    }
}

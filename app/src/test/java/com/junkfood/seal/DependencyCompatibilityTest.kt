package com.junkfood.seal

import com.fasterxml.jackson.databind.ObjectMapper
import com.yausername.youtubedl_android.mapper.VideoInfo
import com.yausername.youtubedl_common.utils.ZipUtils
import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Files
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DependencyCompatibilityTest {
    @Test fun upstreamVideoInfoDeserializesWithUnknownFields() {
        val result = ObjectMapper().readValue("""{"id":"fixture","title":"Sample","duration":12,"formats":[],"unknown_fixture":{"x":1}}""", VideoInfo::class.java)
        assertEquals("Sample", result.title)
        assertEquals("fixture", result.id)
    }
    @Test fun upstreamUpdaterTreeShapeStillParses() {
        val tree = ObjectMapper().readTree("""{"tag_name":"fixture","assets":[{"name":"yt-dlp","browser_download_url":"https://example.com/yt-dlp"}]}""")
        assertEquals("fixture", tree.get("tag_name").asText())
        assertEquals("yt-dlp", tree.get("assets").get(0).get("name").asText())
    }
    private fun zip(name: String): ByteArray = ByteArrayOutputStream().also { bytes ->
        ZipOutputStream(bytes).use { z -> z.putNextEntry(ZipEntry(name)); z.write("fixture".toByteArray()); z.closeEntry() }
    }.toByteArray()
    @Test fun upstreamStreamAndFileZipPathsStillExtract() {
        val root = Files.createTempDirectory("seali-archive-compat").toFile()
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
        val root = Files.createTempDirectory("seali-archive-reject").toFile()
        try {
            try { ZipUtils.unzip(ByteArrayInputStream(zip("../escape.txt")), root); fail("must reject traversal") }
            catch (expected: IllegalAccessException) { assertFalse(java.io.File(root.parentFile,"escape.txt").exists()) }
        } finally { root.deleteRecursively() }
    }
}

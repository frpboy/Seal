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
    @Test fun realDownloaderCancelsAndRetriesLoopbackTransfer() {
        // This checks real yt-dlp transport/process cancellation, not media decoding,
        // a third-party provider, or the app's task lifecycle wiring.
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val downloader = com.yausername.youtubedl_android.YoutubeDL.getInstance()
        downloader.init(context)
        val bytes = ByteArray(256 * 1024) { (it * 31).toByte() }
        val server = java.net.ServerSocket(0, 8, java.net.InetAddress.getByName("127.0.0.1"))
        val slow = java.util.concurrent.atomic.AtomicBoolean(true)
        val worker = Thread {
            while (!server.isClosed) {
                try {
                    server.accept().use { socket ->
                        socket.soTimeout = 5000
                        val reader = socket.getInputStream().bufferedReader()
                        val request = reader.readLine() ?: return@use
                        var start = 0
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                            if (line.startsWith("Range: bytes=", true))
                                start = line.substringAfter('=').substringBefore('-').toInt()
                        }
                        val output = socket.getOutputStream()
                        val range = if (start > 0) "Content-Range: bytes $start-${bytes.lastIndex}/${bytes.size}\r\n" else ""
                        output.write(("HTTP/1.1 " + (if (start > 0) "206 Partial Content" else "200 OK") +
                            "\r\nContent-Type: video/mp4\r\nContent-Length: ${bytes.size - start}\r\n" +
                            range + "Accept-Ranges: bytes\r\nConnection: close\r\n\r\n").toByteArray())
                        output.flush()
                        if (!request.startsWith("HEAD")) {
                            var offset = start
                            while (offset < bytes.size) {
                                val count = minOf(1024, bytes.size - offset)
                                output.write(bytes, offset, count); output.flush(); offset += count
                                if (slow.get()) Thread.sleep(30)
                            }
                        }
                    }
                } catch (_: java.io.IOException) { /* Client cancellation closes the socket. */ }
            }
        }.apply { isDaemon = true; start() }
        val root = java.io.File(context.cacheDir, "transfer-${java.util.UUID.randomUUID()}").apply { mkdirs() }
        val target = java.io.File(root, "fixture.mp4")
        val id = "transfer-${java.util.UUID.randomUUID()}"
        val pool = java.util.concurrent.Executors.newSingleThreadExecutor()
        fun request() = com.yausername.youtubedl_android.YoutubeDLRequest("http://127.0.0.1:${server.localPort}/fixture.mp4").apply {
            addOption("--force-generic-extractor"); addOption("--no-playlist")
            addOption("--socket-timeout", "5"); addOption("--retries", "0")
            addOption("-o", target.absolutePath)
        }
        try {
            val first = pool.submit<Throwable?> {
                try { downloader.execute(request(), id); null } catch (error: Throwable) { error }
            }
            val partial = java.io.File(target.absolutePath + ".part")
            val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(30)
            while (partial.length() == 0L && !first.isDone && System.nanoTime() < deadline) Thread.sleep(50)
            assertTrue("real downloader must write partial data before cancel", partial.length() > 0)
            assertTrue("active process must be canceled", downloader.destroyProcessById(id))
            val error = first.get(15, java.util.concurrent.TimeUnit.SECONDS)
            assertTrue("cancel must not be a successful transfer", error is com.yausername.youtubedl_android.YoutubeDL.CanceledException)
            assertFalse("canceled transfer must not appear complete", target.exists())
            slow.set(false)
            assertEquals(0, downloader.execute(request(), id).exitCode)
            assertArrayEquals("retry must recover exact served bytes", bytes, target.readBytes())
        } finally {
            downloader.destroyProcessById(id); pool.shutdownNow()
            server.close(); worker.join(5000); root.deleteRecursively()
        }
    }

}

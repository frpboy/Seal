package com.frpboy.seali

import androidx.test.platform.app.InstrumentationRegistry
import com.frpboy.seali.util.DownloadUtil
import com.frpboy.seali.util.FunctionalEngine
import com.frpboy.seali.util.VideoClip
import com.frpboy.seali.util.VideoInfo
import com.frpboy.seali.util.CONVERT_MP3
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

/** No internet, user accounts or user media. Exercises real App initialization and downloadVideo. */
class FunctionalAppDownloadTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun initialized() {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45)
        val status = File(context.filesDir, "seali-init-status")
        val expected = "OK pid=${android.os.Process.myPid()}"
        while (System.nanoTime() < deadline) {
            val value = runCatching { status.readText() }.getOrDefault("")
            if (value == expected) break
            check(!value.startsWith("ERROR")) { "Current App initialization failed" }
            Thread.sleep(100)
        }
        assertEquals("Current process App initialization did not finish", expected, runCatching { status.readText() }.getOrDefault(""))
        val engine = File(context.noBackupFilesDir, "youtubedl-android/yt-dlp/yt-dlp")
        val hash = java.security.MessageDigest.getInstance("SHA-256").digest(engine.readBytes())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        assertEquals("Not the reviewed engine", FunctionalEngine.SHA256, hash)
    }

    private class FixtureServer(private val wav: ByteArray) : AutoCloseable {
        private val server = ServerSocket(0, 8, java.net.InetAddress.getByName("127.0.0.1"))
        val slowBodyStarted = CountDownLatch(1)
        val base = "http://127.0.0.1:${server.localPort}"
        @Volatile private var running = true
        private val sockets = java.util.Collections.synchronizedList(mutableListOf<Socket>())
        private val accept = Thread {
            while (running) try {
                val socket = server.accept(); sockets.add(socket)
                Thread {
                    try {
                        socket.soTimeout = 5000
                        val input = socket.getInputStream().bufferedReader()
                        val request = input.readLine() ?: return@Thread
                        while (!input.readLine().isNullOrEmpty()) { }
                        val slow = request.contains("/slow.wav")
                        val output = socket.getOutputStream()
                        val length = if (slow) wav.size * 1000L else wav.size.toLong()
                        output.write(("HTTP/1.1 200 OK\r\nContent-Type: audio/wav\r\nContent-Length: $length\r\nConnection: close\r\n\r\n").toByteArray())
                        if (!request.startsWith("HEAD")) {
                            if (slow) {
                                slowBodyStarted.countDown()
                                repeat(1000) { output.write(wav); output.flush(); Thread.sleep(100) }
                            } else output.write(wav)
                        }
                        output.flush()
                    } catch (_: Exception) { } finally { socket.close(); sockets.remove(socket) }
                }.start()
            } catch (_: Exception) { }
        }.apply { isDaemon = true; start() }
        override fun close() {
            running = false; server.close()
            synchronized(sockets) { sockets.toList().forEach { runCatching { it.close() } } }
            accept.join(2000)
        }
    }

    @Test fun actualAppFetchDownloadsAndConvertsMp3() {
        initialized()
        val fixtures = InstrumentationRegistry.getInstrumentation().context.assets.open("functional/tone.wav").readBytes()
        FixtureServer(fixtures).use { server ->
            val output = File(context.filesDir, "functional-app-download-${System.nanoTime()}").apply { mkdirs() }
            val previousAudio = App.audioDownloadDir
            val previousVideo = App.videoDownloadDir
            try {
                App.audioDownloadDir = output.absolutePath; App.videoDownloadDir = output.absolutePath
                val preferences = DownloadUtil.DownloadPreferences.EMPTY.copy(
                    extractAudio = true, convertAudio = true, audioConvertFormat = CONVERT_MP3,
                    outputTemplate = "%(title)s.%(ext)s", debug = true,
                )
                val info = DownloadUtil.fetchVideoInfoFromUrl("${server.base}/tone.wav", preferences = preferences).getOrThrow()
                assertEquals("wav", info.ext)
                val result = DownloadUtil.downloadVideo(info, taskId = "functional-app-mp3", downloadPreferences = preferences, progressCallback = null)
                val paths = result.getOrThrow()
                val mp3 = paths.map(::File).firstOrNull { it.extension == "mp3" && it.canonicalPath.startsWith(output.canonicalPath + File.separator) }
                assertNotNull("Production downloadVideo did not create MP3", mp3)
                assertTrue("MP3 empty", mp3!!.length() > 100)
                File(output, "app-consumer-result.txt").writeText("PASS production fetchVideoInfoFromUrl + downloadVideo HTTP fixture -> MP3; engine=${FunctionalEngine.VERSION}; ABI=${android.os.Build.SUPPORTED_ABIS.joinToString()}")
            } finally {
                App.audioDownloadDir = previousAudio; App.videoDownloadDir = previousVideo
            }
        }
    }

    @Test fun productionDownloadCancellationStopsChildAndReturnsFailure() {
        initialized()
        val fixtures = InstrumentationRegistry.getInstrumentation().context.assets.open("functional/tone.wav").readBytes()
        FixtureServer(fixtures).use { server ->
            val done = CountDownLatch(1)
            var result: Result<List<String>>? = null
            val cancelDir = File(context.filesDir, "functional-cancel-${System.nanoTime()}").apply { mkdirs() }
            val previousVideo = App.videoDownloadDir
            App.videoDownloadDir = cancelDir.absolutePath
            val preferences = DownloadUtil.DownloadPreferences.EMPTY.copy(outputTemplate = "cancel-fixture.%(ext)s")
            val worker = Thread {
                try {
                    result = DownloadUtil.downloadVideo(
                        VideoInfo(id = "slow-fixture", title = "cancel-fixture", originalUrl = "${server.base}/slow.wav", ext = "wav"),
                        taskId = "functional-cancel", downloadPreferences = preferences, progressCallback = null,
                    )
                } finally { done.countDown() }
            }
            worker.start()
            try {
                assertTrue("Production child did not request fixture", server.slowBodyStarted.await(30, TimeUnit.SECONDS))
                assertTrue("Production cancellation did not find process", YoutubeDL.getInstance().destroyProcessById("functional-cancel"))
                assertTrue("Cancelled download did not return", done.await(10, TimeUnit.SECONDS))
                assertTrue("Cancelled download reported success", result?.isFailure == true)
            } finally {
                YoutubeDL.getInstance().destroyProcessById("functional-cancel")
                server.close(); worker.join(5000)
                App.videoDownloadDir = previousVideo
                if (!worker.isAlive) cancelDir.deleteRecursively()
                assertFalse("Cancelled production worker survived bounded teardown", worker.isAlive)
            }
        }
    }

    @Test fun unsupportedSectionsAndLiveFailBeforeNetwork() {
        initialized()
        val unavailable = "http://127.0.0.1:1/not-used"
        val info = VideoInfo(id = "unsupported", title = "unsupported", originalUrl = unavailable)
        val sections = DownloadUtil.downloadVideo(info, taskId = "sections-rejected", downloadPreferences = DownloadUtil.DownloadPreferences.EMPTY.copy(videoClips = listOf(VideoClip(0, 1))), progressCallback = null)
        assertTrue(sections.exceptionOrNull()?.message.orEmpty().contains("Time-range downloads are unavailable"))
        val live = DownloadUtil.downloadVideo(info.copy(liveStatus = "is_live"), taskId = "live-rejected", downloadPreferences = DownloadUtil.DownloadPreferences.EMPTY, progressCallback = null)
        assertTrue(live.exceptionOrNull()?.message.orEmpty().contains("Live stream downloads are unavailable"))
    }
}

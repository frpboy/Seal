package com.frpboy.seali

import androidx.test.platform.app.InstrumentationRegistry
import com.yausername.ffmpeg.FFmpeg
import java.io.File
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

/** Local-only native qualification, separate from the production-App consumer tests. */
class FFmpegFunctionalTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val fixtures get() = InstrumentationRegistry.getInstrumentation().context.assets

    @Test fun localMergeMp3OpusCoverAndProbe() {
        val log = StringBuilder("ABI=${android.os.Build.SUPPORTED_ABIS.joinToString()}\n")
        val work = File(context.filesDir, "seali-ffmpeg-functional-${System.nanoTime()}").apply { mkdirs() }
        val evidence = File(work, "ffmpeg-functional.txt")
        try {
            FFmpeg.getInstance().init(context)
            val libs = File(context.noBackupFilesDir, "youtubedl-android/packages/ffmpeg/usr/lib")
            assertTrue("Missing extracted FFmpeg libraries", libs.isDirectory)
            fun run(label: String, vararg args: String, probe: Boolean = false): String {
                val binary = File(context.applicationInfo.nativeLibraryDir, if (probe) "libffprobe.so" else "libffmpeg.so")
                assertTrue("Missing $binary", binary.isFile)
                val process = ProcessBuilder(listOf(binary.absolutePath) + args).directory(work)
                    .redirectErrorStream(true).apply { environment()["LD_LIBRARY_PATH"] = libs.absolutePath }.start()
                var text = ""
                val reader = Thread { text = process.inputStream.bufferedReader().readText() }.apply { start() }
                try {
                    assertTrue("$label timed out", process.waitFor(120, TimeUnit.SECONDS))
                    reader.join(5000)
                    log.append("$label exit=${process.exitValue()} args=${args.joinToString(" ")}\n$text\n")
                    assertEquals("$label failed: $text", 0, process.exitValue())
                    return text
                } finally { if (process.isAlive) process.destroyForcibly(); reader.join(5000) }
            }
            for (name in listOf("tone.wav", "video.mp4", "cover.webp")) {
                fixtures.open("functional/$name").use { input -> File(work, name).outputStream().use { input.copyTo(it) } }
            }
            val version = run("version", "-version")
            assertTrue(version.contains("ffmpeg version 9.0.2"))
            assertFalse("Unexpected GPL configuration", version.contains("--enable-gpl"))
            val protocols = run("protocols", "-protocols")
            assertFalse("Unexpected HTTP network protocol", protocols.lineSequence().any { it.trim() in setOf("http", "https", "tcp", "tls", "udp", "rtmp") })
            run("mp3", "-hide_banner", "-loglevel", "error", "-y", "-i", "tone.wav", "-c:a", "libmp3lame", "tone.mp3")
            run("opus", "-hide_banner", "-loglevel", "error", "-y", "-i", "tone.wav", "-c:a", "libopus", "tone.opus")
            run("local-merge", "-hide_banner", "-loglevel", "error", "-y", "-i", "video.mp4", "-i", "tone.wav", "-map", "0:v:0", "-map", "1:a:0", "-c:v", "copy", "-c:a", "aac", "-shortest", "merged.mp4")
            val streams = run("probe-merge", "-v", "error", "-show_entries", "stream=codec_type", "-of", "csv=p=0", "merged.mp4", probe = true)
            assertTrue(streams.contains("video")); assertTrue(streams.contains("audio"))
            run("cover-webp-decode", "-hide_banner", "-loglevel", "error", "-y", "-i", "cover.webp", "-frames:v", "1", "cover.jpg")
            run("cover-embed", "-hide_banner", "-loglevel", "error", "-y", "-i", "tone.mp3", "-i", "cover.jpg", "-map", "0:a:0", "-map", "1:v:0", "-c", "copy", "-id3v2_version", "3", "-disposition:v:0", "attached_pic", "cover.mp3")
            val cover = run("probe-cover", "-v", "error", "-show_entries", "stream_disposition=attached_pic", "-of", "json", "cover.mp3", probe = true)
            assertTrue(cover.contains("\"attached_pic\": 1"))
            for (name in listOf("tone.mp3", "tone.opus", "merged.mp4", "cover.jpg", "cover.mp3")) {
                assertTrue("Missing or empty $name", File(work, name).length() > 0)
            }
            log.append("PASS local native merge/MP3/Opus/WebP decode/cover embed/ffprobe. This is not proof of the full App thumbnail or live network chain.\n")
        } finally { evidence.writeText(log.toString()) }
    }
}

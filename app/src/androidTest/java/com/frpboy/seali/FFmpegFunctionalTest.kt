package com.frpboy.seali

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.yausername.ffmpeg.FFmpeg
import java.io.File
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Bounded functional smoke of the bundled FFmpeg payload on the device this
 * test runs on (the CI lane runs it on a 16 KB page-size emulator).
 *
 * Exercises the same path the app uses: [FFmpeg.init] extracts
 * libffmpeg.zip.so from nativeLibraryDir into the app files directory, and
 * yt-dlp later executes libffmpeg.so from nativeLibraryDir as a child
 * process with LD_LIBRARY_PATH pointing at the extracted usr/lib. This test
 * performs that execution directly: version banner, a WebP encode/decode
 * round trip via libwebp, and an MP3 encode via libmp3lame. Evidence is
 * written to filesDir/seali-ffmpeg-functional/ffmpeg-functional.txt for the
 * workflow to pull.
 */
class FFmpegFunctionalTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private data class RunResult(val exitCode: Int, val output: String)

    private fun runPayload(workDir: File, libDir: File, log: StringBuilder, label: String, vararg args: String): RunResult {
        val binary = File(context.applicationInfo.nativeLibraryDir, "libffmpeg.so")
        assertTrue("libffmpeg.so missing from nativeLibraryDir", binary.isFile)
        val pb = ProcessBuilder(listOf<String>(binary.absolutePath) + args)
        pb.directory(workDir)
        pb.redirectErrorStream(true)
        pb.environment()["LD_LIBRARY_PATH"] = libDir.absolutePath
        val process = pb.start()
        var output = ""
        val reader = Thread { output = process.inputStream.bufferedReader().readText() }
        reader.start()
        val finished = process.waitFor(120, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            reader.join(5000)
            fail("$label did not finish within 120s")
        }
        reader.join(5000)
        val result = RunResult(process.exitValue(), output)
        log.append("== ").append(label).append("\n")
        log.append("cmd: libffmpeg.so ").append(args.joinToString(" ")).append("\n")
        log.append("exit: ").append(result.exitCode).append("\n")
        log.append(result.output).append("\n\n")
        return result
    }

    @Test
    fun ffmpegPayloadInitializesAndRunsOnThisDevice() {
        // Page-size context lands in the evidence so a run is self-describing.
        val log = StringBuilder()
        log.append("device PAGE_SIZE: ").append(readPageSize()).append("\n")
        log.append("abi list: ").append(android.os.Build.SUPPORTED_ABIS.joinToString()).append("\n\n")

        // 1. Extraction path used by App.onCreate.
        FFmpeg.getInstance().init(context)
        val extractedLibDir = File(context.noBackupFilesDir, "youtubedl-android/packages/ffmpeg/usr/lib")
        assertTrue("extracted ffmpeg usr/lib missing at $extractedLibDir", extractedLibDir.isDirectory)
        val libs = extractedLibDir.listFiles(java.io.FileFilter { f -> f.extension == "so" || f.name.contains(".so.") }).orEmpty()
        assertTrue("no shared libraries extracted under $extractedLibDir", libs.isNotEmpty())
        log.append("extracted ").append(libs.size).append(" shared libraries under ").append(extractedLibDir.absolutePath).append("\n\n")

        val workDir = File(context.filesDir, "seali-ffmpeg-functional")
        workDir.mkdirs()

        // 2. Version banner: proves the GPL payload executes on this kernel/page size.
        val version = runPayload(workDir, extractedLibDir, log, "version", "-version")
        assertEquals("ffmpeg -version exit code", 0, version.exitCode)
        assertTrue("version banner missing", version.output.contains("ffmpeg version"))

        // 3. WebP round trip through the payload's libwebp (encode then decode).
        val webp = File(workDir, "roundtrip.webp")
        val encode = runPayload(
            workDir, extractedLibDir, log, "webp-encode",
            "-hide_banner", "-loglevel", "error", "-y",
            "-f", "lavfi", "-i", "testsrc=duration=1:size=64x64:rate=1",
            "-frames:v", "1", "-c:v", "libwebp", webp.absolutePath,
        )
        assertEquals("webp encode exit code", 0, encode.exitCode)
        assertTrue("webp output missing or empty", webp.isFile && webp.length() > 0)

        val png = File(workDir, "roundtrip.png")
        val decode = runPayload(
            workDir, extractedLibDir, log, "webp-decode",
            "-hide_banner", "-loglevel", "error", "-y",
            "-i", webp.absolutePath, "-frames:v", "1", png.absolutePath,
        )
        assertEquals("webp decode exit code", 0, decode.exitCode)
        assertTrue("png output missing or empty", png.isFile && png.length() > 0)

        // 4. Audio encode through libmp3lame (touches a second GPL encoder path).
        val mp3 = File(workDir, "tone.mp3")
        val mp3run = runPayload(
            workDir, extractedLibDir, log, "mp3-encode",
            "-hide_banner", "-loglevel", "error", "-y",
            "-f", "lavfi", "-i", "sine=frequency=440:duration=1",
            "-codec:a", "libmp3lame", mp3.absolutePath,
        )
        assertEquals("mp3 encode exit code", 0, mp3run.exitCode)
        assertTrue("mp3 output missing or empty", mp3.isFile && mp3.length() > 0)

        File(workDir, "ffmpeg-functional.txt").writeText(log.toString())
    }

    private fun readPageSize(): String =
        try {
            val p = ProcessBuilder("getconf", "PAGE_SIZE").redirectErrorStream(true).start()
            p.waitFor(10, TimeUnit.SECONDS)
            p.inputStream.bufferedReader().readText().trim()
        } catch (e: Exception) {
            "unavailable: ${e.message}"
        }
}

package com.frpboy.seali

import androidx.test.platform.app.InstrumentationRegistry
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

/** Controlled loopback TLS/XML fixtures using the App's actual extracted Python stack. */
class PythonComponentsFunctionalTest {
    @Test fun importsHashesTlsAndBoundedXml() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fixtures = InstrumentationRegistry.getInstrumentation().context.assets
        YoutubeDL.getInstance().init(context)
        val work = File(context.filesDir, "seali-components-functional-${System.nanoTime()}").apply { mkdirs() }
        val python = File(context.applicationInfo.nativeLibraryDir, "libpython.so")
        val home = File(context.noBackupFilesDir, "youtubedl-android/packages/python/usr")
        assertTrue(python.isFile); assertTrue(home.isDirectory)
        for (name in listOf("python-components-check.py", "generate-localhost-fixture.py")) {
            fixtures.open("functional/$name").use { input -> File(work, name).outputStream().use { input.copyTo(it) } }
        }
        // The Python test generates a fresh localhost-only key; none is stored in source.
        val process = ProcessBuilder(python.absolutePath, File(work, "python-components-check.py").absolutePath, work.absolutePath)
            .directory(work).redirectErrorStream(true).apply {
                environment()["LD_LIBRARY_PATH"] = File(home, "lib").absolutePath
                environment()["PYTHONHOME"] = home.absolutePath
                environment()["HOME"] = work.absolutePath
                environment()["TMPDIR"] = work.absolutePath
                environment()["SSL_CERT_FILE"] = File(home, "etc/tls/cert.pem").absolutePath
                environment()["PYTHONNOUSERSITE"] = "1"
            }.start()
        var output = ""
        val reader = Thread { output = process.inputStream.bufferedReader().readText() }.apply { start() }
        try {
            assertTrue("Component fixture timed out", process.waitFor(45, TimeUnit.SECONDS))
            reader.join(5000)
            File(work, "python-components-result.txt").writeText(output)
            assertEquals(output, 0, process.exitValue())
            assertTrue(output.contains("controlled_valid_https"))
            assertTrue(output.contains("external_dtd_entity_no_fetch"))
        } finally {
            if (process.isAlive) process.destroyForcibly()
            process.waitFor(5, TimeUnit.SECONDS)
            reader.join(5000)
            File(work, "python-components-result.txt").writeText(output)
            File(work, "localhost-test-key.pem").delete()
            File(work, "localhost-test-cert.pem").delete()
            assertFalse("Fixture child must not survive cleanup", process.isAlive)
        }
    }
}

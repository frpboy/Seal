package com.frpboy.seali

import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.frpboy.seali.util.FunctionalEngine
import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Isolated App bootstrap data tests; never touches the user's engine or app state. */
class FunctionalEngineMigrationTest {
    private fun isolated(block: (ContextWrapper, File) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(context.cacheDir, "engine-migration-test-${System.nanoTime()}").apply { mkdirs() }
        val isolatedContext = object : ContextWrapper(context) {
            override fun getNoBackupFilesDir(): File = dir
        }
        try { block(isolatedContext, dir) } finally { dir.deleteRecursively() }
    }

    @Test fun oldEngineGetsVerifiedCurrentAndFirstBackupSurvives() = isolated { context, root ->
        val bin = File(root, "youtubedl-android/yt-dlp/yt-dlp").apply { parentFile!!.mkdirs(); writeText("old-alpha-fixture") }
        FunctionalEngine.install(context)
        val backup = File(root, "seali-functional-engine-state/yt-dlp.pre-functional-alpha")
        assertEquals("old-alpha-fixture", backup.readText())
        assertTrue(bin.length() > 1000000)
        bin.writeText("newer-user-approved-engine-fixture")
        FunctionalEngine.install(context)
        assertEquals("newer-user-approved-engine-fixture", bin.readText())
        assertEquals("old-alpha-fixture", backup.readText())
    }

    @Test fun updaterDirectoryRemovalCannotDeleteStateOrBackupAndFallbackIsCurrent() = isolated { context, root ->
        val bin = File(root, "youtubedl-android/yt-dlp/yt-dlp").apply { parentFile!!.mkdirs(); writeText("pre-alpha-fixture") }
        FunctionalEngine.install(context)
        bin.parentFile!!.deleteRecursively() // The published updater's failure cleanup boundary.
        bin.parentFile!!.mkdirs()
        // Mirrors init_ytdlp fallback using the transformed wrapper's raw engine, not the asset.
        val raw = context.resources.getIdentifier("ytdlp", "raw", context.packageName)
        assertTrue("Missing wrapper fallback", raw != 0)
        context.resources.openRawResource(raw).use { input -> bin.outputStream().use { input.copyTo(it) } }
        val hash = java.security.MessageDigest.getInstance("SHA-256").digest(bin.readBytes())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        assertEquals("Wrapper updater failure restores stale engine", FunctionalEngine.SHA256, hash)
        assertEquals("pre-alpha-fixture", File(root, "seali-functional-engine-state/yt-dlp.pre-functional-alpha").readText())
        assertEquals(FunctionalEngine.VERSION, File(root, "seali-functional-engine-state/functional-alpha-engine-version").readText())
    }

    @Test fun legitimateParentAliasIsAcceptedButChildSymlinkIsRejected() = isolated { context, root ->
        val alias = File(root.parentFile, "engine-parent-alias-${System.nanoTime()}")
        android.system.Os.symlink(root.path, alias.path)
        try {
            val aliasContext = object : ContextWrapper(context) {
                override fun getNoBackupFilesDir(): File = alias
            }
            FunctionalEngine.install(aliasContext)
            assertTrue(File(root, "youtubedl-android/yt-dlp/yt-dlp").length() > 1000000)
            val temporary = File(root, "youtubedl-android/yt-dlp/yt-dlp.alpha-new")
            android.system.Os.symlink(File(root, "outside-fixture").path, temporary.path)
            assertTrue(runCatching { FunctionalEngine.install(aliasContext) }.isFailure)
        } finally { alias.delete() }
    }

    @Test fun failedTemporaryOutputOpenLeavesOldEngineAndNoMigrationMarker() = isolated { context, root ->
        val bin = File(root, "youtubedl-android/yt-dlp/yt-dlp").apply { parentFile!!.mkdirs(); writeText("old-preserved") }
        // Occupy the temporary output path with a directory to force safe pre-swap failure.
        File(bin.parentFile, "yt-dlp.alpha-new").mkdirs()
        assertTrue(runCatching { FunctionalEngine.install(context) }.isFailure)
        assertEquals("old-preserved", bin.readText())
        assertFalse(File(root, "seali-functional-engine-state/functional-alpha-engine-version").exists())
    }
}

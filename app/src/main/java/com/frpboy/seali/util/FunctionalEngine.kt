package com.frpboy.seali.util

import android.content.Context
import java.io.File
import java.security.MessageDigest

/** A reviewed alpha starts with the reviewed engine, also on upgrades from older alphas. */
internal object FunctionalEngine {
    const val VERSION = "2026.10.26"
    const val SHA256 = "e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3"

    private var recoveredEngine = false

    fun install(context: Context) {
        val owned = context.noBackupFilesDir.canonicalFile
        val root = File(owned, "youtubedl-android/yt-dlp")
        check(root.isDirectory || root.mkdirs()) { "Cannot prepare engine directory" }
        check(root.canonicalFile == root.absoluteFile && root.canonicalPath.startsWith(owned.path + File.separator)) { "Unexpected engine child path" }
        val binary = File(root, "yt-dlp")
        check(!binary.exists() || binary.canonicalFile == binary.absoluteFile) { "Engine must not be a symlink" }
        val state = File(owned, "seali-functional-engine-state")
        check(state.isDirectory || state.mkdirs()) { "Cannot prepare alpha engine state" }
        check(state.canonicalFile == state.absoluteFile) { "Unexpected engine state path" }
        val migration = File(state, "functional-alpha-engine-version")
        for (child in listOf(migration, File(state, "yt-dlp.pre-functional-alpha"), File(state, "yt-dlp.pre-functional-alpha.new"), File(root, "yt-dlp.alpha-new"))) {
            check(child.canonicalFile == child.absoluteFile) { "Unexpected engine state child symlink" }
        }
        fun digest(file: File) = file.inputStream().use { input ->
            val hash = MessageDigest.getInstance("SHA-256")
            val bytes = ByteArray(65536)
            while (true) {
                val count = input.read(bytes)
                if (count < 0) break
                hash.update(bytes, 0, count)
            }
            hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
        }
        recoveredEngine = com.yausername.youtubedl_android.YoutubeDL.recoverInterruptedEngine(
            context, "functional-engine/yt-dlp", SHA256,
        )
        // Perform this version's migration once. Later user-approved engine updates survive.
        if (binary.isFile && migration.isFile && migration.readText() == VERSION) {
            // Preserve later approved updates, but repair metadata only for the exact bundled bytes.
            if (digest(binary) == SHA256) {
                recordBundledVersion(context)
            }
            return
        }
        if (binary.isFile && digest(binary) == SHA256) {
            migration.writeText(VERSION)
            recordBundledVersion(context)
            return
        }
        val temp = File(root, "yt-dlp.alpha-new")
        try {
            context.assets.open("functional-engine/yt-dlp").use { input ->
                java.io.FileOutputStream(temp).use { output -> input.copyTo(output); output.fd.sync() }
            }
            check(digest(temp) == SHA256) { "Bundled engine hash mismatch" }
            // Preserve the pre-alpha engine for inspection; never execute the backup.
            val backup = File(state, "yt-dlp.pre-functional-alpha")
            if (binary.exists() && !backup.exists()) {
                val backupTemp = File(state, "yt-dlp.pre-functional-alpha.new")
                try {
                    binary.copyTo(backupTemp, overwrite = true)
                    check(digest(backupTemp) == digest(binary)) { "Engine backup verification failed" }
                    check(backupTemp.renameTo(backup)) { "Cannot retain pre-alpha engine" }
                } finally { backupTemp.delete() }
            }
            check(temp.renameTo(binary)) { "Cannot install pinned alpha engine" }
            migration.writeText(VERSION)
            recordBundledVersion(context)
        } finally {
            temp.delete()
        }
    }
    fun verifyRecoveredEngine(context: Context) {
        if (!recoveredEngine) return
        val actualVersion = com.yausername.youtubedl_android.YoutubeDL.verifyRecoveredEngine(
            context, "functional-engine/yt-dlp", SHA256, VERSION,
        )
        com.yausername.youtubedl_android.YoutubeDL.recordVerifiedEngineVersion(context, actualVersion)
        PreferenceUtil.encodeString(YT_DLP_VERSION, actualVersion)
        recoveredEngine = false
    }

    private fun recordBundledVersion(context: Context) {
        com.yausername.youtubedl_android.YoutubeDL.recordVerifiedEngineVersion(context, VERSION)
        PreferenceUtil.encodeString(YT_DLP_VERSION, VERSION)
    }

}

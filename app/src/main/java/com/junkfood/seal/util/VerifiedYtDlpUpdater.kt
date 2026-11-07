package com.junkfood.seal.util

import android.content.Context
import com.junkfood.seal.BuildConfig
import com.junkfood.seal.R
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Seali-owned yt-dlp update path. Same feature and install layout as the
 * library's YoutubeDLUpdater (so YoutubeDL.version() and rollback keep
 * working), but every downloaded binary passes YtDlpUpdateVerifier before
 * install: monotonic version, pinned release key OpenPGP signature over
 * SHA2-256SUMS, hash match, size sanity, atomic rename. Failures fail closed:
 * the previously installed (and now security-fixed bundled) binary stays.
 */
object VerifiedYtDlpUpdater {

    private const val PREFS_NAME = "youtubedl-android"
    private const val DLP_VERSION_KEY = "dlpVersion"
    private const val DLP_VERSION_NAME_KEY = "dlpVersionName"
    private const val DLP_BINARY_NAME = "yt-dlp"
    private const val BASE_DIR_NAME = "youtubedl-android"
    private const val YTDLP_DIR_NAME = "ytdlp"
    private const val SUMS_ASSET = "SHA2-256SUMS"
    private const val SIG_ASSET = "SHA2-256SUMS.sig"

    /** yt-dlp release signing key (public.key from the yt-dlp repository),
     *  fingerprint-pinned. Rotation ships with a Seali release, never at runtime. */
    private val PINNED_KEY_FINGERPRINTS = setOf("AC0CBBE6848D6A873464AF4E57CF65933B5A7581")

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private fun installedVersion(appContext: Context): String =
        appContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(DLP_VERSION_KEY, null)
            ?: BuildConfig.BUNDLED_YTDLP_VERSION

    private fun ytdlpDir(appContext: Context): File =
        File(File(appContext.noBackupFilesDir, BASE_DIR_NAME), YTDLP_DIR_NAME)

    private fun download(url: String, target: File) {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw YtDlpUpdateVerifier.VerificationException(
                "download failed with HTTP ${response.code}"
            )
            response.body!!.byteStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    suspend fun update(appContext: Context, channel: YoutubeDL.UpdateChannel): YoutubeDL.UpdateStatus =
        withContext(Dispatchers.IO) {
            val releaseJson =
                client
                    .newCall(Request.Builder().url(channel.apiUrl).build())
                    .execute()
                    .use { response ->
                        if (!response.isSuccessful) return@use null
                        json.parseToJsonElement(response.body!!.string()).jsonObject
                    }
                    ?: throw YtDlpUpdateVerifier.VerificationException("release metadata unavailable")

            val tag = releaseJson.getValue("tag_name").jsonPrimitive.content
            val name = releaseJson["name"]?.jsonPrimitive?.content ?: tag
            val installed = installedVersion(appContext)
            if (!YtDlpUpdateVerifier.isNewerVersion(tag, installed)) {
                return@withContext YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE
            }

            val assets = releaseJson.getValue("assets").jsonArray.map { it.jsonObject }
            fun assetUrl(assetName: String): String =
                assets.firstOrNull { it["name"]?.jsonPrimitive?.content == assetName }
                    ?.getValue("browser_download_url")?.jsonPrimitive?.content
                    ?: throw YtDlpUpdateVerifier.VerificationException("release asset $assetName missing")

            val binary = File.createTempFile("yt-dlp", null, appContext.cacheDir)
            val sums = File.createTempFile("sums", null, appContext.cacheDir)
            val sig = File.createTempFile("sig", null, appContext.cacheDir)
            try {
                download(assetUrl(DLP_BINARY_NAME), binary)
                download(assetUrl(SUMS_ASSET), sums)
                download(assetUrl(SIG_ASSET), sig)
                val keyBytes =
                    appContext.resources.openRawResource(R.raw.ytdlp_release_pubkey).use { it.readBytes() }
                YtDlpUpdateVerifier.verifyDownload(
                    downloaded = binary,
                    assetName = DLP_BINARY_NAME,
                    candidateVersion = tag,
                    installedVersion = installed,
                    sumsFile = sums,
                    signatureFile = sig,
                    pinnedFingerprints = PINNED_KEY_FINGERPRINTS,
                    publicKeyBytes = keyBytes,
                )
                val dir = ytdlpDir(appContext)
                if (!dir.exists()) dir.mkdirs()
                YtDlpUpdateVerifier.atomicInstall(binary, File(dir, DLP_BINARY_NAME))
            } finally {
                binary.delete()
                sums.delete()
                sig.delete()
            }

            appContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(DLP_VERSION_KEY, tag)
                .putString(DLP_VERSION_NAME_KEY, name)
                .apply()
            YoutubeDL.UpdateStatus.DONE
        }
}

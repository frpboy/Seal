package com.yausername.youtubedl_android

import android.content.Context
import com.yausername.youtubedl_android.DownloaderJson.Release
import com.yausername.youtubedl_android.YoutubeDL.UpdateChannel
import com.yausername.youtubedl_android.YoutubeDL.UpdateStatus
import com.yausername.youtubedl_android.YoutubeDL.getInstance
import com.yausername.youtubedl_common.SharedPrefsHelper
import com.yausername.youtubedl_common.SharedPrefsHelper.update
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.net.URL

internal object YoutubeDLUpdater {
    private const val youtubeDLStableChannelUrl =
        "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest"
    private const val youtubeDLNightlyChannelUrl =
        "https://api.github.com/repos/yt-dlp/yt-dlp-nightly-builds/releases/latest"
    private const val youtubeDLMasterChannelUrl =
        "https://api.github.com/repos/yt-dlp/yt-dlp-master-builds/releases/latest"
    private const val dlpBinaryName = "yt-dlp"
    private const val dlpVersionKey = "dlpVersion"
    private const val dlpVersionNameKey = "dlpVersionName"

    @Throws(IOException::class, YoutubeDLException::class)
    internal fun update(
        appContext: Context?,
        youtubeDLChannel: UpdateChannel = UpdateChannel.STABLE
    ): UpdateStatus {
        val json = checkForUpdate(appContext!!, youtubeDLChannel)
            ?: return UpdateStatus.ALREADY_UP_TO_DATE
        val downloadUrl = getDownloadUrl(json)
        val file = download(appContext, downloadUrl)
        val ytdlpDir = getYoutubeDLDir(
            appContext
        )
        val binary = File(ytdlpDir, dlpBinaryName)
        try {
            /* purge older version */
            if (ytdlpDir.exists()) FileUtils.deleteDirectory(ytdlpDir)
            /* install newer version */ytdlpDir.mkdirs()
            FileUtils.copyFile(file, binary)
        } catch (e: Exception) {
            /* if something went wrong restore default version */
            FileUtils.deleteQuietly(ytdlpDir)
            getInstance().init_ytdlp(appContext, ytdlpDir)
            throw YoutubeDLException(e)
        } finally {
            file.delete()
        }
        updateSharedPrefs(appContext, getTag(json), getName(json))
        return UpdateStatus.DONE
    }

    private fun updateSharedPrefs(appContext: Context, tag: String, name: String) {
        update(appContext, dlpVersionKey, tag)
        update(appContext, dlpVersionNameKey, name)
    }

    @Throws(IOException::class)
    private fun checkForUpdate(appContext: Context, youtubeDLChannel: UpdateChannel): Release? {
        val url = URL(youtubeDLChannel.apiUrl)
        val json = url.openConnection().apply { connectTimeout = 5000; readTimeout = 10000 }.getInputStream().bufferedReader().use { DownloaderJson.release(it) }
        val newVersion = getTag(json)
        val oldVersion = SharedPrefsHelper[appContext, dlpVersionKey]
        return if (newVersion == oldVersion) {
            null
        } else json
    }

    private fun getTag(json: Release): String {
        return json.tag_name
    }

    private fun getName(json: Release): String {
        return json.name
    }

    @Throws(YoutubeDLException::class)
    private fun getDownloadUrl(json: Release): String {
        return json.downloadUrl()
    }

    @Throws(IOException::class)
    private fun download(appContext: Context, url: String): File {
        val downloadUrl = URL(url)
        val file = File.createTempFile(dlpBinaryName, null, appContext.cacheDir)
        FileUtils.copyURLToFile(downloadUrl, file, 5000, 10000)
        return file
    }

    private fun getYoutubeDLDir(appContext: Context): File {
        val baseDir = File(appContext.noBackupFilesDir, YoutubeDL.baseName)
        return File(baseDir, YoutubeDL.ytdlpDirName)
    }

    fun version(appContext: Context?): String? {
        return SharedPrefsHelper[appContext!!, dlpVersionKey]
    }

    fun versionName(appContext: Context?): String? {
        return SharedPrefsHelper[appContext!!, dlpVersionNameKey]
    }
}

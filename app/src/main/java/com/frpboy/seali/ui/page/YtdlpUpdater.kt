package com.frpboy.seali.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.frpboy.seali.App
import com.frpboy.seali.util.FDROID_RUNTIME_AUTO_UPDATE_CONSENT
import com.frpboy.seali.Downloader
import com.frpboy.seali.util.PreferenceUtil
import com.frpboy.seali.util.PreferenceUtil.getBoolean
import com.frpboy.seali.util.PreferenceUtil.getLong
import com.frpboy.seali.util.PreferenceUtil.getString
import com.frpboy.seali.util.UpdateUtil
import com.frpboy.seali.util.YT_DLP_AUTO_UPDATE
import com.frpboy.seali.util.YT_DLP_UPDATE_INTERVAL
import com.frpboy.seali.util.YT_DLP_UPDATE_TIME
import com.frpboy.seali.util.YT_DLP_VERSION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun YtdlpUpdater() {

    val downloaderState by Downloader.downloaderState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (downloaderState !is Downloader.State.Idle) return@LaunchedEffect

        // F-Droid consent is independent of legacy settings and the empty-version bootstrap.
        if (App.isFDroidBuild() &&
            (!FDROID_RUNTIME_AUTO_UPDATE_CONSENT.getBoolean() || !YT_DLP_AUTO_UPDATE.getBoolean()))
            return@LaunchedEffect

        if (!YT_DLP_AUTO_UPDATE.getBoolean() && YT_DLP_VERSION.getString().isNotEmpty())
            return@LaunchedEffect

        if (!PreferenceUtil.isNetworkAvailableForDownload()) {
            return@LaunchedEffect
        }

        val lastUpdateTime = YT_DLP_UPDATE_TIME.getLong()
        val currentTime = System.currentTimeMillis()

        if (currentTime < lastUpdateTime + YT_DLP_UPDATE_INTERVAL.getLong()) {
            return@LaunchedEffect
        }

        runCatching {
                Downloader.updateState(state = Downloader.State.Updating)
                withContext(Dispatchers.IO) { UpdateUtil.updateYtDlp() }
            }
            .onFailure { it.printStackTrace() }
        Downloader.updateState(state = Downloader.State.Idle)
    }
}

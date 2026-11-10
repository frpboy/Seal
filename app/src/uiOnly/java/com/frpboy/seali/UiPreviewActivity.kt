package com.frpboy.seali

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.dp
import com.frpboy.seali.download.Task
import com.frpboy.seali.ui.common.SettingsProvider
import com.frpboy.seali.ui.page.downloadv2.DownloadPageImplV2
import com.frpboy.seali.ui.page.downloadv2.classic.ClassicHomePageImpl
import com.frpboy.seali.ui.page.downloadv2.configure.FormatPageImpl
import com.frpboy.seali.ui.theme.SealTheme
import com.frpboy.seali.util.DownloadUtil
import com.frpboy.seali.util.Format
import com.frpboy.seali.util.VideoInfo
import com.tencent.mmkv.MMKV

/** No production App, Koin graph, downloader, updater or network startup. */
class UiPreviewApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        App.context = applicationContext
        App.packageInfo = packageManager.getPackageInfo(packageName, 0)
        MMKV.initialize(this)
    }
}

class UiPreviewActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SettingsProvider(calculateWindowSizeClass(this).widthSizeClass) {
                SealTheme {
                    UiOnlyPreview()
                }
            }
        }
    }
}

@Composable
fun UiOnlyPreview() {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var message by rememberSaveable { mutableStateOf("") }
    val sampleInfo = remember {
        VideoInfo(id = "preview", title = "Sample video - UI preview", uploader = "Sample creator",
            duration = 180.0, formats = listOf(
                Format(formatId="v1080", ext="mp4", vcodec="avc1", acodec="none", width=1920.0, height=1080.0, fps=30.0, formatNote="1080p", format="1080p MP4 video", resolution="1920x1080", tbr=1067.0, fileSize=24000000.0),
                Format(formatId="v720", ext="mp4", vcodec="avc1", acodec="none", width=1280.0, height=720.0, fps=30.0, formatNote="720p", format="720p MP4 video", resolution="1280x720", tbr=533.0, fileSize=12000000.0),
                Format(formatId="a128", ext="m4a", vcodec="none", acodec="mp4a", abr=128.0, tbr=128.0, format="128 kbps M4A audio", formatNote="128 kbps audio", resolution="audio only", fileSize=3000000.0)
            ))
    }
    val samples = remember {
        mutableStateMapOf<Task, Task.State>().apply {
            val prefs = DownloadUtil.DownloadPreferences.EMPTY
            val failed = Task("https://example.invalid/sample-failed", preferences=prefs)
            put(failed, Task.State(Task.DownloadState.Error(IllegalStateException("Sample error. No download was attempted."), Task.RestartableAction.Download), null, Task.ViewState(title="Sample queue item - retry / copy error", uploader="Sample creator")))
            val done = Task("https://example.invalid/sample-finished", preferences=prefs)
            put(done, Task.State(Task.DownloadState.Completed(null), null, Task.ViewState(title="Sample completed item - no real file", uploader="Sample creator")))
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Surface(color=MaterialTheme.colorScheme.secondaryContainer) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("Offline UI-only preview", style=MaterialTheme.typography.titleSmall)
                Text("Sample data. Downloads, updates, accounts and file access are unavailable.", style=MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    listOf("Classic", "V2 queue", "Formats").forEachIndexed { i, text ->
                        TextButton(onClick={page=i;message=""}) { Text(text) }
                    }
                }
                if(message.isNotEmpty()) Text(message, style=MaterialTheme.typography.bodySmall)
            }
        }
        CompositionLocalProvider(LocalUriHandler provides object : UriHandler {
            override fun openUri(uri: String) { message = "External links are disabled in this preview." }
        }) {
        Box(Modifier.weight(1f)) {
            when(page) {
                0 -> ClassicHomePageImpl(taskDownloadStateMap=samples,
                    onMenuOpen={message="Use Classic, V2 queue and Formats above to switch preview screens."},
                    onDownload={message="Preview only. Your link was not opened or downloaded."},
                    onActionPost={_,_->message="Preview only. Sample queue action; no task or file was changed."})
                1 -> DownloadPageImplV2(taskDownloadStateMap=samples, previewReservedBottomSpace=96.dp,
                    downloadCallback={message="Preview only. No download will run."},
                    pasteCallback={message="Preview only. No link processing will run. Classic has the real Paste field."},
                    onMenuOpen={message="Use the preview tabs above."},
                    onActionPost={_,_->message="Preview only. No retry, file action or external link will run."})
                else -> FormatPageImpl(videoInfo=sampleInfo, selectedSubtitleCodes=emptySet(),
                    onNavigateBack={page=0}, onDownloadPressed={message="Preview only. Selection inspected; nothing downloaded."})
            }
        }
        }
    }
}

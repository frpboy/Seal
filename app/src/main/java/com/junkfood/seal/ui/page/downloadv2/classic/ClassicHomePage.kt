package com.junkfood.seal.ui.page.downloadv2.classic

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.R
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.Task
import com.junkfood.seal.ui.common.HapticFeedback.slightHapticFeedback
import com.junkfood.seal.ui.page.downloadv2.UiAction
import com.junkfood.seal.ui.page.downloadv2.configure.Config
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialog
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel.Action
import com.junkfood.seal.ui.page.downloadv2.configure.FormatPage
import com.junkfood.seal.ui.page.downloadv2.configure.PlaylistSelectionPage
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.getErrorReport
import com.junkfood.seal.util.makeToast
import org.koin.compose.koinInject

private const val TAG = "ClassicHomePage"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassicHomePage(
    modifier: Modifier = Modifier,
    onMenuOpen: (() -> Unit) = {},
    dialogViewModel: DownloadDialogViewModel,
    downloader: DownloaderV2 = koinInject(),
) {
    val view = LocalView.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    ClassicHomePageImpl(
        modifier = modifier,
        taskDownloadStateMap = downloader.getTaskStateMap(),
        onMenuOpen = onMenuOpen,
        onDownload = { url ->
            view.slightHapticFeedback()
            dialogViewModel.postAction(
                Action.ShowSheet(if (url.isBlank()) null else listOf(url.trim()))
            )
        },
    ) { task, action ->
        view.slightHapticFeedback()
        when (action) {
            UiAction.Cancel -> downloader.cancel(task)
            UiAction.Delete -> downloader.remove(task)
            UiAction.Resume -> downloader.restart(task)
            is UiAction.CopyErrorReport -> {
                clipboardManager.setText(
                    AnnotatedString(getErrorReport(action.throwable, task.url))
                )
                context.makeToast(R.string.error_copied)
            }
            UiAction.CopyVideoURL -> {
                clipboardManager.setText(AnnotatedString(task.url))
                context.makeToast(R.string.link_copied)
            }
            is UiAction.OpenFile -> {
                action.filePath?.let {
                    FileUtil.openFile(path = it) { context.makeToast(R.string.file_unavailable) }
                }
            }
            is UiAction.OpenThumbnailURL -> {
                uriHandler.openUri(action.url)
            }
            is UiAction.OpenVideoURL -> {
                uriHandler.openUri(action.url)
            }
            is UiAction.ShareFile -> {
                val shareTitle = context.getString(R.string.share)
                FileUtil.createIntentForSharingFile(action.filePath)?.let {
                    context.startActivity(Intent.createChooser(it, shareTitle))
                }
            }
        }
    }

    var preferences by remember {
        mutableStateOf(DownloadUtil.DownloadPreferences.createFromPreferences())
    }
    val sheetValue by dialogViewModel.sheetValueFlow.collectAsStateWithLifecycle()
    val state by dialogViewModel.sheetStateFlow.collectAsStateWithLifecycle()

    val selectionState = dialogViewModel.selectionStateFlow.collectAsStateWithLifecycle().value

    var showDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(sheetValue) {
        if (sheetValue == DownloadDialogViewModel.SheetValue.Expanded) {
            showDialog = true
        } else {
            launch { sheetState.hide() }.invokeOnCompletion { showDialog = false }
        }
    }

    if (showDialog) {
        DownloadDialog(
            state = state,
            sheetState = sheetState,
            config = Config(),
            preferences = preferences,
            onPreferencesUpdate = { preferences = it },
            onActionPost = { dialogViewModel.postAction(it) },
        )
    }
    when (selectionState) {
        is DownloadDialogViewModel.SelectionState.FormatSelection ->
            FormatPage(
                state = selectionState,
                onDismissRequest = { dialogViewModel.postAction(Action.Reset) },
            )
        is DownloadDialogViewModel.SelectionState.PlaylistSelection -> {
            PlaylistSelectionPage(
                state = selectionState,
                onDismissRequest = { dialogViewModel.postAction(Action.Reset) },
            )
        }
        DownloadDialogViewModel.SelectionState.Idle -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassicHomePageImpl(
    modifier: Modifier = Modifier,
    taskDownloadStateMap: SnapshotStateMap<Task, Task.State>,
    onMenuOpen: (() -> Unit) = {},
    onDownload: (String) -> Unit = {},
    onActionPost: (Task, UiAction) -> Unit = { _, _ -> },
) {
    var urlInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val tasks = remember(taskDownloadStateMap.size) { taskDownloadStateMap.keys.sortedDescending() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Seali") },
                navigationIcon = {
                    IconButton(onClick = onMenuOpen) {
                        Icon(Icons.Outlined.Menu, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Paste or type a video link") },
                trailingIcon = {
                    IconButton(
                        onClick = { clipboardManager.getText()?.text?.let { urlInput = it.trim() } }
                    ) {
                        Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste")
                    }
                },
            )
            Button(
                onClick = { onDownload(urlInput) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            ) {
                Text("Download")
            }
            Text(
                text = "Downloads",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tasks.forEach { task ->
                    item(key = task.id) {
                        val state = taskDownloadStateMap[task]
                        ClassicTaskCard(
                            task = task,
                            state = state,
                            onActionPost = { action -> onActionPost(task, action) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassicTaskCard(
    task: Task,
    state: Task.State?,
    onActionPost: (UiAction) -> Unit,
) {
    val downloadState = state?.downloadState
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = state?.videoInfo?.title ?: task.url,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
            )
            Text(
                text = statusTextOf(downloadState),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (downloadState is Task.DownloadState.Running) {
                if (downloadState.progress >= 0f) {
                    LinearProgressIndicator(
                        progress = { downloadState.progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (downloadState) {
                    is Task.DownloadState.Cancelable -> {
                        IconButton(onClick = { onActionPost(UiAction.Cancel) }) {
                            Icon(Icons.Outlined.Cancel, contentDescription = "Cancel")
                        }
                    }
                    is Task.DownloadState.Restartable -> {
                        IconButton(onClick = { onActionPost(UiAction.Resume) }) {
                            Icon(Icons.Outlined.RestartAlt, contentDescription = "Restart")
                        }
                    }
                    else -> {}
                }
                if (downloadState is Task.DownloadState.Error) {
                    IconButton(
                        onClick = {
                            onActionPost(UiAction.CopyErrorReport(downloadState.throwable))
                        }
                    ) {
                        Icon(Icons.Outlined.ContentPaste, contentDescription = "Copy error")
                    }
                }
                if (downloadState is Task.DownloadState.Completed) {
                    IconButton(
                        onClick = { onActionPost(UiAction.OpenFile(downloadState.filePath)) }
                    ) {
                        Icon(Icons.Outlined.FileOpen, contentDescription = "Open")
                    }
                }
                IconButton(onClick = { onActionPost(UiAction.Delete) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

private fun statusTextOf(downloadState: Task.DownloadState?): String =
    when (downloadState) {
        is Task.DownloadState.FetchingInfo -> "Fetching video info..."
        is Task.DownloadState.Running ->
            if (downloadState.progressText.isNotBlank()) downloadState.progressText
            else "Downloading..."
        Task.DownloadState.Idle,
        Task.DownloadState.ReadyWithInfo -> "Queued"
        is Task.DownloadState.Completed -> "Completed"
        is Task.DownloadState.Canceled -> "Canceled"
        is Task.DownloadState.Error ->
            "Failed - ${downloadState.throwable.message ?: "unknown error"}"
        null -> "Pending"
    }

package com.junkfood.seal.ui.page.downloadv2.classic

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.ui.component.MediaListItem
import com.junkfood.seal.util.DatabaseUtil
import com.junkfood.seal.util.FileUtil.getFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import com.junkfood.seal.util.matchUrlFromClipboard
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val TAG = "ClassicHomePage"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassicHomePage(
    modifier: Modifier = Modifier,
    onMenuOpen: (() -> Unit) = {},
    onShowDownloads: () -> Unit = {},
    dialogViewModel: DownloadDialogViewModel,
    downloader: DownloaderV2 = koinInject(),
) {
    val view = LocalView.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    val history by remember { DatabaseUtil.getDownloadHistoryFlow() }
        .collectAsStateWithLifecycle(emptyList())
    val historySizes by produceState<Map<Int, Long>>(emptyMap(), history) {
        value = withContext(Dispatchers.IO) {
            history.associate { it.id to it.videoPath.getFileSize() }
        }
    }

    ClassicHomePageImpl(
        modifier = modifier,
        taskDownloadStateMap = downloader.getTaskStateMap(),
        completedDownloads = history,
        completedFileSizes = historySizes,
        onShowDownloads = onShowDownloads,
        onOpenCompleted = { info ->
            FileUtil.openFile(path = info.videoPath) { context.makeToast(R.string.file_unavailable) }
        },
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
            UiAction.Delete -> {
                // Recheck current state: a stale terminal card may have been restarted.
                val state = downloader.getTaskStateMap()[task]?.downloadState
                if (canRemoveClassicTask(state)) downloader.remove(task)
            }
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
    completedDownloads: List<DownloadedVideoInfo> = emptyList(),
    completedFileSizes: Map<Int, Long> = emptyMap(),
    onOpenCompleted: (DownloadedVideoInfo) -> Unit = {},
    onShowDownloads: () -> Unit = {},
    onActionPost: (Task, UiAction) -> Unit = { _, _ -> },
) {
    var urlInput by rememberSaveable { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val tasks = taskDownloadStateMap.keys.sortedDescending()
    val completedPaths = taskDownloadStateMap.values.mapNotNull {
        (it.downloadState as? Task.DownloadState.Completed)?.filePath
    }.toSet()
    val savedDownloads = classicSavedDownloads(completedDownloads, completedPaths)

    // Without a separate top app bar, Scaffold supplies the system-bar insets
    // once. Keep the menu and input in the same row, with a full touch target.
    Scaffold(modifier = modifier.fillMaxSize()) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onMenuOpen, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Outlined.Menu,
                        contentDescription = stringResource(R.string.classic_open_menu),
                    )
                }
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = {
                        Text(
                            stringResource(R.string.classic_link_hint),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                clipboardManager.getText()?.text?.let {
                                    matchUrlFromClipboard(it)
                                        .takeIf { url -> url.isNotEmpty() }
                                        ?.let { urlInput = it }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Outlined.ContentPaste,
                                contentDescription = stringResource(R.string.paste),
                            )
                        }
                    },
                )
            }
            Button(
                onClick = { onDownload(urlInput) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            ) {
                Text(stringResource(R.string.download))
            }
            Text(
                text = stringResource(R.string.downloads_history),
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
                savedDownloads.forEach { info ->
                    item(key = "history-${info.id}", contentType = "saved-download") {
                        MediaListItem(
                            title = info.videoTitle,
                            author = info.videoAuthor,
                            thumbnailUrl = info.thumbnailUrl,
                            videoPath = info.videoPath,
                            videoUrl = info.videoUrl,
                            videoFileSize = completedFileSizes[info.id] ?: 0L,
                            onClick = { onOpenCompleted(info) },
                            onLongClick = onShowDownloads,
                            onShowContextMenu = onShowDownloads,
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
                text = classicTaskTitle(state?.videoInfo?.title, state?.viewState?.title, task.url),
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
                    Task.DownloadState.Idle,
                    Task.DownloadState.ReadyWithInfo,
                    is Task.DownloadState.Cancelable -> {
                        IconButton(onClick = { onActionPost(UiAction.Cancel) }) {
                            Icon(
                                Icons.Outlined.Cancel,
                                contentDescription = stringResource(R.string.cancel)
                            )
                        }
                    }
                    is Task.DownloadState.Restartable -> {
                        IconButton(onClick = { onActionPost(UiAction.Resume) }) {
                            Icon(
                                Icons.Outlined.RestartAlt,
                                contentDescription = stringResource(R.string.restart)
                            )
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
                        Icon(
                            Icons.Outlined.ContentPaste,
                            contentDescription = stringResource(R.string.copy_error_report)
                        )
                    }
                }
                if (downloadState is Task.DownloadState.Completed) {
                    IconButton(
                        onClick = { onActionPost(UiAction.OpenFile(downloadState.filePath)) }
                    ) {
                        Icon(
                            Icons.Outlined.FileOpen,
                            contentDescription = stringResource(R.string.open_file)
                        )
                    }
                }
                if (canRemoveClassicTask(downloadState)) {
                    IconButton(onClick = { onActionPost(UiAction.Delete) }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun statusTextOf(downloadState: Task.DownloadState?): String =
    when (downloadState) {
        is Task.DownloadState.FetchingInfo -> stringResource(R.string.fetching_info)
        is Task.DownloadState.Running ->
            if (downloadState.progressText.isNotBlank()) downloadState.progressText
            else stringResource(R.string.status_downloading)
        Task.DownloadState.Idle,
        Task.DownloadState.ReadyWithInfo,
        null -> stringResource(R.string.status_enqueued)
        is Task.DownloadState.Completed -> stringResource(R.string.status_completed)
        is Task.DownloadState.Canceled -> stringResource(R.string.status_canceled)
        is Task.DownloadState.Error -> stringResource(R.string.status_error)
    }

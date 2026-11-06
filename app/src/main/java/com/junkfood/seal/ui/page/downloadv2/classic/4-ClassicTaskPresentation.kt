package com.junkfood.seal.ui.page.downloadv2.classic

import com.junkfood.seal.download.Task

internal fun canRemoveClassicTask(state: Task.DownloadState?): Boolean =
    state is Task.DownloadState.Restartable || state is Task.DownloadState.Completed

internal fun classicTaskTitle(videoTitle: String?, storedTitle: String?, url: String): String =
    videoTitle?.takeIf { it.isNotBlank() } ?: storedTitle?.takeIf { it.isNotBlank() } ?: url

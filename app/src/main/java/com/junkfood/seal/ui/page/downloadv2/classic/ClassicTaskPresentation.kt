package com.junkfood.seal.ui.page.downloadv2.classic

import com.junkfood.seal.download.Task

internal fun canRemoveClassicTask(state: Task.DownloadState?): Boolean =
    state is Task.DownloadState.Restartable || state is Task.DownloadState.Completed

internal fun classicTaskTitle(videoTitle: String?, storedTitle: String?, url: String): String =
    videoTitle?.takeIf { it.isNotBlank() } ?: storedTitle?.takeIf { it.isNotBlank() } ?: url

/** Saved history survives process restarts; only exact live completed paths are already visible. */
internal fun classicSavedDownloads(
    history: List<com.junkfood.seal.database.objects.DownloadedVideoInfo>,
    visibleCompletedPaths: Set<String>,
): List<com.junkfood.seal.database.objects.DownloadedVideoInfo> =
    history.asReversed().filter { it.videoPath !in visibleCompletedPaths }

package com.frpboy.seali.ui.page.downloadv2.classic

import com.frpboy.seali.download.Task

internal fun canRemoveClassicTask(state: Task.DownloadState?): Boolean =
    state is Task.DownloadState.Restartable || state is Task.DownloadState.Completed

internal fun classicTaskTitle(videoTitle: String?, storedTitle: String?, url: String): String =
    videoTitle?.takeIf { it.isNotBlank() } ?: storedTitle?.takeIf { it.isNotBlank() } ?: url

/** Saved history survives process restarts; only exact live completed paths are already visible. */
internal fun classicSavedDownloads(
    history: List<com.frpboy.seali.database.objects.DownloadedVideoInfo>,
    visibleCompletedPaths: Set<String>,
): List<com.frpboy.seali.database.objects.DownloadedVideoInfo> =
    history.asReversed().filter { it.videoPath !in visibleCompletedPaths }

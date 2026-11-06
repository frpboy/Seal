package com.junkfood.seal.util

import com.junkfood.seal.download.Task

object InterfaceSwitchGuard {
    fun hasActiveDownloads(states: Iterable<Task.DownloadState>): Boolean =
        states.any { it is Task.DownloadState.Running || it is Task.DownloadState.FetchingInfo }
}

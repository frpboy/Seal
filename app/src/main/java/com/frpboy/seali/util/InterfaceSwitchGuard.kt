package com.frpboy.seali.util

import com.frpboy.seali.download.Task

object InterfaceSwitchGuard {
    fun hasActiveDownloads(states: Iterable<Task.DownloadState>): Boolean =
        states.any { it is Task.DownloadState.Running || it is Task.DownloadState.FetchingInfo }
}

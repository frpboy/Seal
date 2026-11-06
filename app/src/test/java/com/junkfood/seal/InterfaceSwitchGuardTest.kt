package com.junkfood.seal

import com.junkfood.seal.download.Task
import com.junkfood.seal.util.InterfaceSwitchGuard
import kotlinx.coroutines.Job
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterfaceSwitchGuardTest {
    @Test
    fun `no states means no active downloads`() {
        assertFalse(InterfaceSwitchGuard.hasActiveDownloads(emptyList()))
    }

    @Test
    fun `running download blocks the switch`() {
        val states =
            listOf<Task.DownloadState>(
                Task.DownloadState.Running(job = Job(), taskId = "t1", progress = 0.5f)
            )
        assertTrue(InterfaceSwitchGuard.hasActiveDownloads(states))
    }

    @Test
    fun `fetching info blocks the switch`() {
        val states =
            listOf<Task.DownloadState>(Task.DownloadState.FetchingInfo(job = Job(), taskId = "t2"))
        assertTrue(InterfaceSwitchGuard.hasActiveDownloads(states))
    }

    @Test
    fun `terminal and queued states do not block the switch`() {
        val states =
            listOf<Task.DownloadState>(
                Task.DownloadState.Idle,
                Task.DownloadState.ReadyWithInfo,
                Task.DownloadState.Completed(filePath = null),
                Task.DownloadState.Canceled(action = Task.RestartableAction.Download),
                Task.DownloadState.Error(action = Task.RestartableAction.FetchInfo),
            )
        assertFalse(InterfaceSwitchGuard.hasActiveDownloads(states))
    }
}

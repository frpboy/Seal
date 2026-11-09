package com.frpboy.seali.ui.page.downloadv2

import com.frpboy.seali.download.Task
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueFilterTest {
    private val states = listOf(
        Task.DownloadState.Idle,
        Task.DownloadState.FetchingInfo(taskId = "fixture"),
        Task.DownloadState.ReadyWithInfo,
        Task.DownloadState.Running(taskId = "fixture"),
        Task.DownloadState.Canceled(Task.RestartableAction.Download),
        Task.DownloadState.Error(action = Task.RestartableAction.Download),
        Task.DownloadState.Completed("/fixture/video.mp4"),
    )

    @Test fun finishedIncludesOnlyCompletedSessionTasks() {
        states.forEach { state ->
            if (state is Task.DownloadState.Completed) assertTrue(Filter.Finished.matches(state))
            else assertFalse(Filter.Finished.matches(state))
        }
    }

    @Test fun allIncludesEverySessionState() {
        states.forEach { assertTrue(Filter.All.matches(it)) }
    }

    @Test fun downloadingExcludesFinishedAndRestartableStates() {
        states.forEach { state ->
            if (state is Task.DownloadState.Restartable || state is Task.DownloadState.Completed)
                assertFalse(Filter.Downloading.matches(state))
            else assertTrue(Filter.Downloading.matches(state))
        }
    }

    @Test fun canceledRetainsExistingErrorAndCanceledSemantics() {
        states.forEach { state ->
            if (state is Task.DownloadState.Restartable) assertTrue(Filter.Canceled.matches(state))
            else assertFalse(Filter.Canceled.matches(state))
        }
    }
}

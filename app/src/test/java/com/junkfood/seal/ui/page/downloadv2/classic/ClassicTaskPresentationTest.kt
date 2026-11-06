package com.junkfood.seal.ui.page.downloadv2.classic

import com.junkfood.seal.download.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicTaskPresentationTest {
    @Test
    fun `active queued and missing tasks cannot be removed`() {
        listOf(
                null,
                Task.DownloadState.Idle,
                Task.DownloadState.ReadyWithInfo,
                Task.DownloadState.FetchingInfo(taskId = "fetch"),
                Task.DownloadState.Running(taskId = "run")
            )
            .forEach { assertFalse(canRemoveClassicTask(it)) }
    }

    @Test
    fun `terminal tasks can be removed`() {
        listOf(
                Task.DownloadState.Completed(null),
                Task.DownloadState.Canceled(Task.RestartableAction.Download),
                Task.DownloadState.Error(action = Task.RestartableAction.FetchInfo)
            )
            .forEach { assertTrue(canRemoveClassicTask(it)) }
    }

    @Test
    fun `title falls back through stored metadata to URL`() {
        assertEquals("Video", classicTaskTitle("Video", "Stored", "url"))
        assertEquals("Stored", classicTaskTitle(null, "Stored", "url"))
        assertEquals("Stored", classicTaskTitle(" ", "Stored", "url"))
        assertEquals("url", classicTaskTitle("", " ", "url"))
        assertEquals("url", classicTaskTitle(null, null, "url"))
    }
}

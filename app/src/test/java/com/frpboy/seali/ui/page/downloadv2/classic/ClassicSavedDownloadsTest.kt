package com.frpboy.seali.ui.page.downloadv2.classic

import com.frpboy.seali.database.objects.DownloadedVideoInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class ClassicSavedDownloadsTest {
    private fun info(id: Int, path: String) = DownloadedVideoInfo(id, "Saved $id", "Author", "https://example.com/video", "", path)

    @Test fun historyRemainsVisibleWithNoInMemoryTasks() {
        val history = listOf(info(1, "/a.mp4"), info(2, "/b.mp4"))
        assertEquals(listOf(2, 1), classicSavedDownloads(history, emptySet()).map { it.id })
    }

    @Test fun exactCompletedPathIsNotDuplicatedButOtherDownloadsOfSameUrlRemain() {
        val history = listOf(info(1, "/a.mp4"), info(2, "/b.mp4"))
        assertEquals(listOf(2), classicSavedDownloads(history, setOf("/a.mp4")).map { it.id })
    }

    @Test fun deletionAndEmptyHistoryAreReflectedWithoutMutatingHistory() {
        val history = listOf(info(1, "content://downloads/1"))
        assertEquals(history, classicSavedDownloads(history, emptySet()))
        assertEquals(emptyList<DownloadedVideoInfo>(), classicSavedDownloads(emptyList(), emptySet()))
        assertEquals(1, history.size)
    }
}

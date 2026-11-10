package com.frpboy.seali.ui.page.downloadv2

import com.frpboy.seali.download.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CompletedFileSizeTest {
    private fun state(path: String?, size: Double = 0.0) =
        Task.State(Task.DownloadState.Completed(path), null, Task.ViewState(fileSizeApprox = size))

    @Test
    fun completedContentUriUsesUriAwareLookup() {
        val path = "content://com.android.externalstorage.documents/document/primary%3ADownload%2Fclip.mp4"
        var queriedPath: String? = null
        val result = state(path).withActualFileSize {
            queriedPath = it
            4096L
        }
        assertEquals(path, queriedPath)
        assertEquals(4096.0, result.fileSizeApprox, 0.0)
    }

    @Test
    fun completedRawPathStillUsesLookup() {
        assertEquals(2048.0, state("/storage/emulated/0/Download/clip.mp4")
            .withActualFileSize { 2048L }.fileSizeApprox, 0.0)
    }

    @Test
    fun reportedSizeDoesNotQueryProvider() {
        val state = state("content://provider/document/clip", 512.0)
        assertSame(state.viewState, state.withActualFileSize { error("must not query") })
    }

    @Test
    fun missingPathDoesNotQueryProvider() {
        val state = state(null)
        assertSame(state.viewState, state.withActualFileSize { error("must not query") })
    }

    @Test
    fun providerFailureOrUnknownSizePreservesViewState() {
        val state = state("content://provider/document/clip")
        assertSame(state.viewState, state.withActualFileSize { throw SecurityException() })
        assertSame(state.viewState, state.withActualFileSize { 0L })
        assertSame(state.viewState, state.withActualFileSize { -1L })
    }

    @Test
    fun unfinishedTaskDoesNotQueryProvider() {
        val state = Task.State(Task.DownloadState.Idle, null, Task.ViewState())
        assertSame(state.viewState, state.withActualFileSize { error("must not query") })
    }
}

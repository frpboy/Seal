package com.frpboy.seali.ui.page.downloadv2

import com.frpboy.seali.download.Task
import com.frpboy.seali.util.Format
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskClassificationTest {

    private val videoFormat = Format(vcodec = "h264", acodec = "aac")
    private val audioFormat = Format(vcodec = "none", acodec = "aac")

    private fun state(
        viewState: Task.ViewState = Task.ViewState(),
        downloadState: Task.DownloadState = Task.DownloadState.Idle,
    ) = Task.State(downloadState = downloadState, videoInfo = null, viewState = viewState)

    @Test
    fun taskWithVideoFormatsIsVideo() {
        val s = state(Task.ViewState(videoFormats = listOf(videoFormat)))
        assertTrue(s.isVideoTask())
        assertFalse(s.isAudioTask())
    }

    @Test
    fun taskWithOnlyAudioFormatsIsAudio() {
        val s = state(Task.ViewState(audioOnlyFormats = listOf(audioFormat)))
        assertFalse(s.isVideoTask())
        assertTrue(s.isAudioTask())
    }

    @Test
    fun completedVideoFileWithoutFormatsIsVideo() {
        val s =
            state(
                Task.ViewState(),
                Task.DownloadState.Completed("/storage/emulated/0/Download/Seali/clip.mp4"),
            )
        assertTrue(s.isVideoTask())
        assertFalse(s.isAudioTask())
    }

    @Test
    fun completedAudioFileWithoutFormatsIsAudio() {
        val s =
            state(
                Task.ViewState(),
                Task.DownloadState.Completed("/storage/emulated/0/Download/Seali/track.m4a"),
            )
        assertFalse(s.isVideoTask())
        assertTrue(s.isAudioTask())
    }

    @Test
    fun taskWithoutFormatsOrFileIsNeither() {
        val s = state(Task.ViewState(), Task.DownloadState.Completed(null))
        assertFalse(s.isVideoTask())
        assertFalse(s.isAudioTask())
    }
}

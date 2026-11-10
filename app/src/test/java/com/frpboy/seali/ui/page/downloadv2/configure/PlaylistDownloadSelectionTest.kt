package com.frpboy.seali.ui.page.downloadv2.configure

import com.frpboy.seali.util.DownloadType
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class PlaylistDownloadSelectionTest {
    private fun assertSubmission(initial: DownloadType, choices: List<DownloadType>, expected: DownloadType) {
        var selected = initial
        choices.forEach { selected = it }
        val events = mutableListOf<String>()
        submitPlaylistSelection(selected, { events.add("download:$it") }, { events.add("dismiss") })
        assertEquals(listOf("download:$expected", "dismiss"), events)
    }

    @Test fun videoToAudioDispatchesAudio() =
        assertSubmission(DownloadType.Video, listOf(DownloadType.Audio), DownloadType.Audio)

    @Test fun audioToVideoDispatchesVideo() =
        assertSubmission(DownloadType.Audio, listOf(DownloadType.Video), DownloadType.Video)

    @Test fun unchangedVideoDispatchesVideo() =
        assertSubmission(DownloadType.Video, emptyList(), DownloadType.Video)

    @Test fun unchangedAudioDispatchesAudio() =
        assertSubmission(DownloadType.Audio, emptyList(), DownloadType.Audio)

    @Test fun lastVisibleChoiceWinsAfterRepeatedChanges() =
        assertSubmission(DownloadType.Video,
            listOf(DownloadType.Audio, DownloadType.Video, DownloadType.Audio), DownloadType.Audio)

    @Test fun submissionFailureDoesNotDismissOrRetry() {
        var submits = 0
        var dismisses = 0
        try {
            submitPlaylistSelection(DownloadType.Audio, { submits++; error("test failure") }, { dismisses++ })
            fail("submission should throw")
        } catch (expected: IllegalStateException) {
            assertEquals("test failure", expected.message)
        }
        assertEquals(1, submits)
        assertEquals(0, dismisses)
    }
}

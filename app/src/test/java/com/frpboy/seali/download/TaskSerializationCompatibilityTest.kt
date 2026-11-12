package com.frpboy.seali.download

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskSerializationCompatibilityTest {
    @Test
    fun keepsLegacyNamesForPersistedTaskVariants() {
        assertEquals(
            "com.junkfood.seal.download.Task.TypeInfo.Playlist",
            Task.TypeInfo.Playlist.serializer().descriptor.serialName,
        )
        assertEquals(
            "com.junkfood.seal.download.Task.DownloadState.Running",
            Task.DownloadState.Running.serializer().descriptor.serialName,
        )
        assertEquals(
            "com.junkfood.seal.download.Task.RestartableAction.Download",
            Task.RestartableAction.Download.serializer().descriptor.serialName,
        )
    }
}

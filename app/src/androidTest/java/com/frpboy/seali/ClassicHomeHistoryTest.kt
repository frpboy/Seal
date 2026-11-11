package com.frpboy.seali

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.frpboy.seali.database.objects.DownloadedVideoInfo
import com.frpboy.seali.download.Task
import com.frpboy.seali.ui.page.downloadv2.classic.ClassicHomePageImpl
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ClassicHomeHistoryTest {
    @get:Rule val compose = createComposeRule()

    @Test fun persistedDownloadsShowAndOpenAlongsideLiveTask() {
        val saved = DownloadedVideoInfo(1, "Saved sample video", "Sample author", "https://example.com/sample", "", "/sample.mp4")
        val history = mutableStateListOf(saved)
        val states = mutableStateMapOf<Task, Task.State>()
        val active = Task("https://example.com/active", preferences = com.frpboy.seali.util.DownloadUtil.DownloadPreferences.EMPTY)
        states[active] = Task.State(Task.DownloadState.Running(taskId = "synthetic", progress = 0.5f), null, Task.ViewState(title = "Active sample download"))
        var opened = 0
        compose.setContent {
            MaterialTheme {
                ClassicHomePageImpl(taskDownloadStateMap = states, completedDownloads = history,
                    completedFileSizes = mapOf(1 to 1024L), onOpenCompleted = { opened++ })
            }
        }
        compose.onNodeWithText("Active sample download").assertIsDisplayed()
        compose.onNodeWithText("Saved sample video").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, opened) }
        // Capture real composed UI, with synthetic local-only history and no network download.
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val out = java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "classic-history.png")
        out.outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }

        compose.runOnIdle { history.clear() }
        compose.onNodeWithText("Saved sample video").assertDoesNotExist()
    }

    @Test fun roomHistorySurvivesReopenAndDrivesHome() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // A separate test database, never the user's app_database.
        val name = "classic-history-test-${java.util.UUID.randomUUID()}"
        fun open() = androidx.room.Room.databaseBuilder(
            context, com.frpboy.seali.database.AppDatabase::class.java, name
        ).build()
        var db = open()
        try {
            val saved = DownloadedVideoInfo(77, "Reopened saved video", "Test author", "https://example.com/reopened", "", "/reopened.mp4")
            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                db.videoInfoDao().insert(saved)
            }
            db.close()
            db = open()
            val flow = db.videoInfoDao().getDownloadHistoryFlow()
            val states = mutableStateMapOf<Task, Task.State>()
            compose.setContent {
                val history = flow.collectAsState(initial = emptyList()).value
                MaterialTheme {
                    ClassicHomePageImpl(taskDownloadStateMap = states, completedDownloads = history)
                }
            }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Reopened saved video").fetchSemanticsNodes().size == 1
            }
            compose.onNodeWithText("Reopened saved video").assertIsDisplayed()
            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                db.videoInfoDao().deleteInfoById(77)
            }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Reopened saved video").fetchSemanticsNodes().isEmpty()
            }
            compose.onNodeWithText("Reopened saved video").assertDoesNotExist()
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

}

package com.junkfood.seal

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.test.platform.app.InstrumentationRegistry
import com.junkfood.seal.download.Task
import com.junkfood.seal.ui.page.downloadv2.UiAction
import com.junkfood.seal.ui.page.downloadv2.classic.ClassicHomePageImpl
import com.junkfood.seal.util.DownloadUtil
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ClassicHomeControlsTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun label(id: Int) = context.getString(id)
    private fun screenshot(name: String) {
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        java.io.File(context.filesDir, name).outputStream().use {
            check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
        }
    }
    private fun header(fontScale: Float, image: String) {
        val states = mutableStateMapOf<Task, Task.State>()
        var menu = 0
        var submitted = ""
        var keyboard: SoftwareKeyboardController? = null
        var rootView: android.view.View? = null
        compose.setContent {
            val density = LocalDensity.current.density
            val view = LocalView.current
            val controller = LocalSoftwareKeyboardController.current
            SideEffect { keyboard = controller; rootView = view }
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                MaterialTheme { ClassicHomePageImpl(taskDownloadStateMap = states,
                    onMenuOpen = { menu++ }, onDownload = { submitted = it }) }
            }
        }
        compose.onNodeWithContentDescription(label(R.string.classic_open_menu)).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, menu) }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("fixture", "https://example.com/local-fixture"))
        }
        compose.onNodeWithContentDescription(label(R.string.paste)).assertIsDisplayed().performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals("https://example.com/local-fixture")
        compose.onNodeWithText(label(R.string.download), useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals("https://example.com/local-fixture", submitted) }
        compose.onNode(hasSetTextAction()).performClick().performTextInput("?fixture=typed")
        compose.onNode(hasSetTextAction()).assertIsFocused()
        // Ask for the IME the way a user re-taps a field, then require the real
        // window-insets visibility signal. Geometry heuristics alone are not
        // accepted as keyboard proof; the captured pixel is the evidence.
        val imeImage = image.replace(".png", "-ime.png")
        var imeVisible = false
        val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(15)
        while (!imeVisible && System.nanoTime() < deadline) {
            compose.runOnIdle { keyboard?.show() }
            Thread.sleep(250)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val view = checkNotNull(rootView)
                val insets = androidx.core.view.ViewCompat.getRootWindowInsets(view)
                imeVisible = insets?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
            }
        }
        // Always capture the actual IME state, pass or fail, before asserting.
        screenshot(imeImage)
        assertTrue(
            "IME must become visible at fontScale $fontScale on API ${android.os.Build.VERSION.SDK_INT}; see $imeImage",
            imeVisible,
        )
        compose.onNodeWithContentDescription(label(R.string.classic_open_menu)).assertIsDisplayed()
        compose.onNodeWithContentDescription(label(R.string.paste)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.download), useUnmergedTree = true).assertIsDisplayed()
        compose.waitForIdle()
        screenshot(image)
    }
    @Test fun normalFontMenuPasteAndDownloadControls() = header(1f, "classic-controls-normal.png")
    @Test fun largestFontFocusedInputControls() = header(2f, "classic-controls-font2.png")
    @Test fun cancelAndRetryControlsDispatchCurrentTask() {
        val task = Task("https://example.com/local-task", preferences = DownloadUtil.DownloadPreferences.EMPTY)
        val states = mutableStateMapOf<Task, Task.State>()
        val running = Task.State(Task.DownloadState.Running(taskId = "fixture", progress = 0.4f), null, Task.ViewState(title = "Callback fixture"))
        states[task] = running
        var action: UiAction? = null
        var actionTask: Task? = null
        compose.setContent { MaterialTheme { ClassicHomePageImpl(taskDownloadStateMap = states,
            onActionPost = { item, next -> actionTask = item; action = next }) } }
        compose.onNodeWithContentDescription(label(R.string.cancel)).assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(task, actionTask); assertEquals(UiAction.Cancel, action)
            states[task] = running.copy(downloadState = Task.DownloadState.Canceled(Task.RestartableAction.Download))
        }
        compose.onNodeWithContentDescription(label(R.string.restart)).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(task, actionTask); assertEquals(UiAction.Resume, action) }
        compose.onNodeWithContentDescription(label(R.string.cancel)).assertDoesNotExist()
    }
    @Test fun interactiveControlsExposeLabelsForScreenReaders() {
        val states = mutableStateMapOf<Task, Task.State>()
        states[Task("https://example.com/labeled", preferences = DownloadUtil.DownloadPreferences.EMPTY)] =
            Task.State(Task.DownloadState.Running(taskId = "labels", progress = 0.4f), null, Task.ViewState(title = "Label fixture"))
        compose.setContent { MaterialTheme { ClassicHomePageImpl(taskDownloadStateMap = states) } }
        compose.waitForIdle()
        // The URL field labels itself with its hint while empty.
        compose.onNodeWithText(label(R.string.classic_link_hint)).assertIsDisplayed()
        val unlabeled = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().filter { node ->
            // Editable fields announce their hint/value instead of a static label.
            if (node.config.contains(SemanticsActions.SetText)) return@filter false
            val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }
            val description = node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ")
            text.isNullOrBlank() && description.isNullOrBlank()
        }
        assertEquals("every clickable control needs text or a content description: $unlabeled", 0, unlabeled.size)
    }
}

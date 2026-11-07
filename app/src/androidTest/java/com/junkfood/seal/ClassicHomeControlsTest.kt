package com.junkfood.seal

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
        var keyboardBottom = 0
        compose.setContent {
            val density = LocalDensity.current.density
            val controller = LocalSoftwareKeyboardController.current
            val inset = WindowInsets.ime.getBottom(LocalDensity.current)
            SideEffect { keyboard = controller; keyboardBottom = inset }
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
        compose.runOnIdle { keyboard?.show() }
        compose.waitUntil(10_000) { keyboardBottom > 0 }
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
}

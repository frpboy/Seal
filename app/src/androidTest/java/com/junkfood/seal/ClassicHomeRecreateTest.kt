package com.junkfood.seal

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.junkfood.seal.download.Task
import com.junkfood.seal.ui.page.downloadv2.classic.ClassicHomePageImpl
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ClassicHomeRecreateTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun label(id: Int) = context.getString(id)

    @Test fun inputAndControlsSurviveRecreation() {
        val states = mutableStateMapOf<Task, Task.State>()
        var menu = 0
        var submitted = ""
        val render: @Composable () -> Unit = {
            MaterialTheme {
                ClassicHomePageImpl(
                    taskDownloadStateMap = states,
                    onMenuOpen = { menu++ },
                    onDownload = { submitted = it },
                )
            }
        }
        compose.activityRule.scenario.onActivity { it.setContent(render) }
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).performClick().performTextInput("https://example.com/recreated")
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.activityRule.scenario.onActivity { it.setContent(render) }
        compose.waitForIdle()
        // The in-progress URL input must survive recreation; losing it was the
        // kind of interruption the interface-switch polish set out to avoid.
        compose.onNode(hasSetTextAction()).assertTextEquals("https://example.com/recreated")
        compose.onNodeWithContentDescription(label(R.string.classic_open_menu)).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, menu) }
        compose.onNodeWithText(label(R.string.download), useUnmergedTree = true).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("https://example.com/recreated", submitted) }
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        java.io.File(context.filesDir, "classic-recreate.png").outputStream().use {
            check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}

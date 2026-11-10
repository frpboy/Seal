package com.frpboy.seali

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import com.frpboy.seali.ui.page.settings.about.CreditsPage
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Actual Credits composable with bundled notices, synthetic guest only. Pixel review is separate. */
class CreditsFunctionalVisualTest {
    @get:Rule val compose = createComposeRule()
    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val out = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, name)
        out.outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
    }
    @Test fun noticeDialogRendersAndScrolls() {
        compose.setContent { MaterialTheme { CreditsPage(onNavigateBack = {}) } }
        compose.onNodeWithText("Bundled native component notices").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Close").assertIsDisplayed()
        capture("seali-credits-notices-top.png")
        // The entire actual combined notice body is a single selectable scrolling Text node.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val names = context.assets.open("functional-notices/notice-index.txt").bufferedReader().use { it.readLines() }.filter { it.isNotBlank() }
        check(names.size == 21)
        val body = names.joinToString("\n\n") { name -> name + "\n\n" + context.assets.open("functional-notices/" + name).bufferedReader().use { it.readText() } }
        compose.onNodeWithText(body).assertIsDisplayed().performTouchInput { swipeUp() }
        capture("seali-credits-notices-scrolled.png")
        compose.onNodeWithText("Close").performClick()
    }
}

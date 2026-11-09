package com.junkfood.seal

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Process
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import com.junkfood.seal.ui.page.settings.about.AboutPage
import com.junkfood.seal.ui.page.settings.about.CreditsPage
import com.junkfood.seal.ui.page.settings.appearance.LanguagePage
import com.junkfood.seal.ui.page.settings.troubleshooting.TroubleShootingPage
import com.junkfood.seal.ui.theme.SealTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Production composables and typography, fresh synthetic emulator only. No external URI opens. */
class BrandingPagesCaptureTest {
    @get:Rule val compose = createComposeRule()

    @Test fun productionSettingsLabelsAndCallbacks() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.resources.configuration.locales[0].toLanguageTag() == "en-US")
        val output = File(context.filesDir, "branding-capture")
        check(output.mkdirs() || output.isDirectory)
        check(output.listFiles().orEmpty().isEmpty())
        fun diagnosticStage(value: String) {
            // Values are local fixed literals, never application state or exception text.
            instrumentation.sendStatus(0, Bundle().apply {
                putString("stream", "\nSEALI_BRANDING_STAGE=$value\n")
            })
        }
        diagnosticStage("native_init")
        // Preserve the application's real native initialization gate: a crash or incomplete init fails.
        compose.waitUntil(60_000) {
            File(context.filesDir, "seali-init-status").let {
                it.exists() && it.readText() == "OK pid=${Process.myPid()}"
            }
        }
        diagnosticStage("assertions")
        val screen = mutableStateOf("about")
        val opened = mutableListOf<String>()
        val uriRecorder = object : UriHandler {
            override fun openUri(uri: String) { opened.add(uri) }
        }
        compose.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriRecorder) {
                SealTheme(darkTheme = false) {
                    when (screen.value) {
                        "about" -> AboutPage(
                            onNavigateBack = { screen.value = "back" },
                            onNavigateToCreditsPage = { screen.value = "credits" },
                            onNavigateToUpdatePage = { error("Unexpected update action") },
                            onNavigateToDonatePage = { error("Unexpected donation action") },
                        )
                        "credits" -> CreditsPage(onNavigateBack = { screen.value = "about" })
                        "language" -> LanguagePage(onNavigateBack = { screen.value = "about" })
                        "troubleshooting" -> TroubleShootingPage(
                            onNavigateTo = { error("Unexpected secondary settings action") },
                            onBack = { screen.value = "about" },
                        )
                    }
                }
            }
        }
        fun capture(name: String) {
            compose.waitForIdle()
            val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            try {
                File(output, "$name.png").outputStream().use {
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
            } finally { bitmap.recycle() }
        }
        fun label(id: Int) = context.getString(id)
        compose.onNodeWithText("Developer").assertIsDisplayed()
        compose.onNodeWithText("Rahul Muraleedharan - https://www.frpboy.in/").assertIsDisplayed()
        capture("about-top")
        compose.onNodeWithText("Developer").performClick()
        compose.runOnIdle { assertEquals(listOf("https://www.frpboy.in/"), opened) }
        compose.onNode(hasScrollToIndexAction())
            .performScrollToNode(hasText(label(R.string.original_project_thanks)))
        compose.onNodeWithText(label(R.string.original_project_thanks)).assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction())
            .performScrollToNode(hasText(label(R.string.credits)))
        compose.onNodeWithText(label(R.string.credits)).assertIsDisplayed()
        capture("about-credits")
        compose.onNode(hasScrollToIndexAction())
            .performScrollToNode(hasText(label(R.string.original_project_thanks)))
        compose.onNodeWithText(label(R.string.original_project_thanks)).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("https://github.com/JunkFood02/Seal", opened.last()) }
        compose.onNode(hasScrollToIndexAction())
            .performScrollToNode(hasText(label(R.string.credits)))
        compose.onNodeWithText(label(R.string.credits)).assertIsDisplayed().performClick()
        compose.onNode(hasScrollToIndexAction())
            .performScrollToNode(hasText(label(R.string.original_project_credit)))
        compose.onNodeWithText(label(R.string.original_project_credit)).assertIsDisplayed()
        capture("credits")
        compose.onNodeWithContentDescription(label(R.string.back)).performClick()
        compose.runOnIdle { assertEquals("about", screen.value); screen.value = "language" }
        compose.onNodeWithText(label(R.string.seali_translate_desc)).assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals("https://github.com/frpboy/Seali/blob/main/CONTRIBUTING.md#translations", opened.last())
        }
        capture("language")
        compose.onNodeWithContentDescription(label(R.string.back)).performClick()
        compose.runOnIdle { assertEquals("about", screen.value); screen.value = "troubleshooting" }
        compose.onNodeWithText("Seali Issue Tracker").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("https://github.com/frpboy/Seali/issues", opened.last()) }
        capture("troubleshooting")
        compose.onNodeWithContentDescription(label(R.string.back)).performClick()
        compose.runOnIdle { assertEquals("about", screen.value) }
    }
}

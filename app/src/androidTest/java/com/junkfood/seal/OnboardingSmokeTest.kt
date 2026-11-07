package com.junkfood.seal

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.test.uiautomator.UiDevice
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsNotEnabled
import android.graphics.Bitmap
import androidx.lifecycle.ViewModelProvider
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.junkfood.seal.util.AUTO_UPDATE
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.YT_DLP_AUTO_UPDATE
import com.junkfood.seal.util.YT_DLP_VERSION
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual MainActivity and production onboarding. No network/download is requested. */
@RunWith(AndroidJUnit4::class)
class OnboardingSmokeTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun text(id: Int) = context.getString(id)
    private fun node(id: Int) = compose.onNodeWithText(text(id))

    @Before fun freshOrientation() {
        PreferenceUtil.encodeInt("onboarding_version", 0)
        AUTO_UPDATE.updateBoolean(false)
        YT_DLP_AUTO_UPDATE.updateBoolean(false)
        // Use the actual pinned bundled version to keep this offline UI fixture offline.
        PreferenceUtil.encodeString(YT_DLP_VERSION, BuildConfig.BUNDLED_YTDLP_VERSION)
    }

    private fun snapshot(name: String) {
        compose.waitForIdle()
        instrumentation.waitForIdleSync()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(context.filesDir, name).outputStream().use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally { bitmap.recycle() }
    }

    @Test fun largeFontLandscapeKeepsActionsReachable() {
        // Runner sets font_scale=1.5 for this single bounded invocation.
        check(context.resources.configuration.fontScale >= 1.49f)
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            activity.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            compose.waitUntil(10_000) {
                context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            }
            node(R.string.onboarding_next).assertIsDisplayed().performClick()
            node(R.string.onboarding_folder_body).performScrollTo().assertIsDisplayed()
            node(R.string.onboarding_next).assertIsDisplayed()
            node(R.string.onboarding_back).assertIsDisplayed()
            snapshot("onboarding-landscape-font15.png")
            node(R.string.onboarding_next).performClick()
            node(R.string.onboarding_finish).assertIsDisplayed().performClick()
            activity.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }

    @Test fun navigationBackAndManualUpdaterWithMotionOff() {
        ActivityScenario.launch(MainActivity::class.java).use {
            node(R.string.onboarding_skip).performClick()
            compose.onNodeWithContentDescription(text(R.string.show_navigation_drawer)).performClick()
            node(R.string.settings).performClick()
            node(R.string.about).performScrollTo().performClick()
            node(R.string.auto_update).performScrollTo().assertIsDisplayed()
            compose.onNode(isToggleable()).assertIsOff().assertIsNotEnabled()
            snapshot("onboarding-about-debug-update-off.png")
            node(R.string.auto_update).performClick()
            node(R.string.manual_test_build_update).assertIsDisplayed()
            snapshot("onboarding-manual-update.png")
            UiDevice.getInstance(instrumentation).pressBack()
            node(R.string.auto_update).assertIsDisplayed()
            UiDevice.getInstance(instrumentation).pressBack()
            node(R.string.about).assertIsDisplayed()
            snapshot("onboarding-settings-back.png")
        }
    }

    @Test fun stepsSurviveRecreationAndFinishPersists() {
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            node(R.string.onboarding_start_title).assertIsDisplayed()
            snapshot("onboarding-step-1.png")
            node(R.string.onboarding_next).performClick()
            node(R.string.onboarding_folder_title).assertIsDisplayed()
            activity.recreate()
            node(R.string.onboarding_folder_title).assertIsDisplayed()
            snapshot("onboarding-step-2-recreated.png")
            node(R.string.onboarding_next).performClick()
            node(R.string.onboarding_permission_title).assertIsDisplayed()
            snapshot("onboarding-step-3.png")
            node(R.string.onboarding_back).performClick()
            node(R.string.onboarding_folder_title).assertIsDisplayed()
            node(R.string.onboarding_next).performClick()
            node(R.string.onboarding_finish).performClick()
            node(R.string.onboarding_title).assertDoesNotExist()
            assertEquals(1, "onboarding_version".getInt())
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            node(R.string.onboarding_title).assertDoesNotExist()
        }
    }

    @Test fun skipPersistsAcrossRelaunch() {
        ActivityScenario.launch(MainActivity::class.java).use {
            node(R.string.onboarding_skip).performClick()
            node(R.string.onboarding_title).assertDoesNotExist()
            assertEquals(1, "onboarding_version".getInt())
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            node(R.string.onboarding_title).assertDoesNotExist()
        }
    }

    private fun shareIntent(url: String) = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    private fun assertSharedUrl(activity: MainActivity, url: String) {
        val vm = ViewModelProvider(activity)[DownloadDialogViewModel::class.java]
        val state = vm.sheetStateFlow.value as DownloadDialogViewModel.SheetState.Configure
        assertEquals(listOf(url), state.urlList)
        assertEquals(DownloadDialogViewModel.SheetValue.Expanded, vm.sheetValueFlow.value)
    }

    private fun awaitSharedUrl(activity: ActivityScenario<MainActivity>, url: String) {
        compose.waitUntil(10_000) {
            var matched = false
            activity.onActivity {
                val vm = ViewModelProvider(it)[DownloadDialogViewModel::class.java]
                matched = (vm.sheetStateFlow.value as? DownloadDialogViewModel.SheetState.Configure)
                    ?.urlList == listOf(url)
            }
            matched
        }
    }

    @Test fun expandedInputSheetDefersUntilHidden() {
        ActivityScenario.launch<MainActivity>(shareIntent("https://example.com/seali-modal-fixture")).use { activity ->
            activity.onActivity {
                ViewModelProvider(it)[DownloadDialogViewModel::class.java]
                    .postAction(DownloadDialogViewModel.Action.ShowSheet())
            }
            compose.waitForIdle()
            node(R.string.onboarding_title).assertDoesNotExist()
            snapshot("onboarding-input-sheet-deferred.png")
            activity.onActivity {
                val vm = ViewModelProvider(it)[DownloadDialogViewModel::class.java]
                assertEquals(DownloadDialogViewModel.SheetState.InputUrl, vm.sheetStateFlow.value)
                assertEquals(DownloadDialogViewModel.SheetValue.Expanded, vm.sheetValueFlow.value)
                vm.postAction(DownloadDialogViewModel.Action.HideSheet)
            }
            node(R.string.onboarding_title).assertIsDisplayed()
            snapshot("onboarding-after-sheet-hidden.png")
        }
    }

    @Test fun coldAndWarmShareDeferOrientation() {
        val cold = "https://example.com/seali-cold-share-fixture"
        val warm = "https://example.com/seali-warm-share-fixture"
        ActivityScenario.launch<MainActivity>(shareIntent(cold)).use { activity ->
            node(R.string.onboarding_title).assertDoesNotExist()
            node(R.string.settings_before_download).assertIsDisplayed()
            activity.onActivity { assertSharedUrl(it, cold) }
            snapshot("onboarding-cold-share.png")
            activity.recreate()
            node(R.string.onboarding_title).assertDoesNotExist()
            node(R.string.settings_before_download).assertIsDisplayed()
            activity.onActivity { assertSharedUrl(it, cold) }
            context.startActivity(shareIntent(warm))
            awaitSharedUrl(activity, warm)
            compose.waitForIdle()
            node(R.string.onboarding_title).assertDoesNotExist()
            node(R.string.settings_before_download).assertIsDisplayed()
            activity.onActivity { assertSharedUrl(it, warm) }
            snapshot("onboarding-warm-share.png")
            assertEquals(0, "onboarding_version".getInt())
        }
    }
}

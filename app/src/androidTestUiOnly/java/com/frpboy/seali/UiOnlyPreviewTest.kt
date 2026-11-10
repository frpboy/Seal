package com.frpboy.seali

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiOnlyPreviewTest {
    @get:Rule val rule = createAndroidComposeRule<UiPreviewActivity>()
    private fun capture(name: String) {
        rule.waitForIdle()
        val path=File(rule.activity.filesDir,name)
        path.outputStream().use { rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
        assertTrue(path.length()>0)
    }
    @Test fun launchAndInspectRealScreens() {
        rule.onNodeWithText("Offline UI-only preview").assertIsDisplayed()
        assertEquals("com.frpboy.seali.uipreview",rule.activity.packageName)
        assertEquals("Seali UI Preview",rule.activity.applicationInfo.loadLabel(rule.activity.packageManager).toString())
        assertEquals(PackageManager.PERMISSION_DENIED, rule.activity.checkSelfPermission(Manifest.permission.INTERNET))
        val info=rule.activity.packageManager.getPackageInfo(rule.activity.packageName,PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS)
        assertFalse(info.requestedPermissions?.contains(Manifest.permission.INTERNET)==true)
        assertEquals(UiPreviewApplication::class.java.name,rule.activity.applicationInfo.className)
        assertNotNull("Preview activity inventory must exist", info.activities)
        assertFalse(info.activities!!.any { it.name.endsWith("QuickDownloadActivity") || it.name.endsWith("MainActivity") })
        assertTrue(info.services.isNullOrEmpty())
        assertTrue(info.receivers.isNullOrEmpty())
        assertTrue(info.providers.isNullOrEmpty())
        capture("ui-preview-classic.png")
        rule.onNodeWithText("V2 queue").performClick()
        capture("ui-preview-v2.png")
        rule.onNodeWithText("Formats",useUnmergedTree=true).performClick()
        rule.onNodeWithText("Sample video - UI preview",substring=true).assertExists()
        capture("ui-preview-formats.png")
        rule.onNodeWithText("Classic").performClick()
        rule.onAllNodesWithText("Download",ignoreCase=true).onFirst().performClick()
        rule.onNodeWithText("Preview only. Your link was not opened or downloaded.").assertIsDisplayed()
    }
}

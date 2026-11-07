package com.junkfood.seal

import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val TIMEOUT = 15_000L

/**
 * B3: real external SAF + permission qualification on emulator (CI API 26 and
 * API 35 jobs). Unlike the FileProvider fixture, this drives the real system
 * DocumentsUI picker and then proves actual write + read inside the granted
 * tree, not just permission state.
 */
@RunWith(AndroidJUnit4::class)
class SafPermissionEmulatorTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device: UiDevice = UiDevice.getInstance(instrumentation)
    private val context
        get() = instrumentation.targetContext

    @Test
    fun documentTreeGrantAllowsRealWriteAndRead() {
        // Launch the real picker via the same contract the settings page uses.
        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
        context.startActivity(intent)

        // DocumentsUI: choose the visible "Documents" root and confirm.
        assertTrue("DocumentsUI picker did not open", device.wait(Until.hasObject(By.text("Documents")), TIMEOUT))
        device.findObject(UiSelector().text("Documents")).click()
        val confirm =
            device.wait(Until.findObject(By.textContains("Use this folder")), TIMEOUT)
                ?: device.wait(Until.findObject(By.textContains("SELECT")), TIMEOUT)
        assertNotNull("No confirm button in picker", confirm)
        confirm.click()
        // API 26-29 may show an "Allow" access dialog.
        device.wait(Until.hasObject(By.textContains("Allow")), 3_000L)
        device.findObject(UiSelector().textContains("Allow")).takeIf { it.exists() }?.click()
        device.waitForIdle()

        // Recover the granted tree from persisted permissions (the same grant
        // App.updateDownloadDir takes via takePersistableUriPermission).
        val grant =
            context.contentResolver.persistedUriPermissions.firstOrNull {
                it.isReadPermission && it.isWritePermission
            }
        assertNotNull("No persistable read+write tree grant recorded", grant)
        val tree: Uri = grant!!.uri

        // Prove real write + read inside the chosen folder via DocumentFile.
        val root = DocumentFile.fromTreeUri(context, tree)
        assertNotNull("Could not open granted tree", root)
        val file = root!!.createFile("text/plain", "seali-saf-qualification")
        assertNotNull("Create in granted tree failed", file)
        context.contentResolver.openOutputStream(file!!.uri)!!.use {
            it.write("seali-saf-ok".toByteArray())
        }
        val readBack =
            context.contentResolver.openInputStream(file.uri)!!.use { input ->
                input.readBytes().decodeToString()
            }
        assertEquals("seali-saf-ok", readBack)
        file.delete()
    }

    @Test
    fun notificationPermissionGrantIsVisibleToApp() {
        if (android.os.Build.VERSION.SDK_INT < 33) return
        instrumentation.uiAutomation
            .executeShellCommand(
                "pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS"
            )
            .close()
        val granted =
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        assertTrue("POST_NOTIFICATIONS grant not visible to app", granted)
    }
}

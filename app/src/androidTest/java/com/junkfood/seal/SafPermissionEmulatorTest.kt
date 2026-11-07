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
import org.junit.Assert.fail
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

    /** Dump the current window hierarchy as evidence when a picker assertion
     *  fails, so selector fixes are grounded in the real DocumentsUI layout
     *  instead of guessed labels. */
    private fun dumpUiHierarchy(): String =
        try {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand(
                    "uiautomator dump /data/local/tmp/seali-ui.xml && cat /data/local/tmp/seali-ui.xml"
                )
            ).use { it.readBytes().decodeToString() }.take(6_000)
        } catch (e: Exception) {
            "hierarchy dump failed: ${e.message}"
        }

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

        // DocumentsUI varies across API 26-35: the tree picker may open on a
        // roots list or on the last-used root behind a "Show roots" drawer.
        // Drive it adaptively instead of assuming one layout.
        val pickerOpened =
            device.wait(Until.hasObject(By.pkg("com.android.documentsui")), TIMEOUT) ||
                device.wait(Until.hasObject(By.pkg("com.google.android.documentsui")), 5_000L)
        if (!pickerOpened) fail("DocumentsUI picker did not open. Hierarchy:\n" + dumpUiHierarchy())
        // If no root labels are visible, expand the roots drawer first.
        if (device.findObject(UiSelector().text("Documents")).exists().not() &&
            device.findObject(UiSelector().text("Downloads")).exists().not()
        ) {
            device.findObject(UiSelector().descriptionContains("Show roots"))
                .takeIf { it.exists() }?.click()
            device.waitForIdle()
        }
        val root =
            device.wait(Until.findObject(By.text("Documents")), TIMEOUT)
                ?: device.wait(Until.findObject(By.text("Downloads")), TIMEOUT)
        if (root == null) fail("No Documents/Downloads root in picker. Hierarchy:\n" + dumpUiHierarchy())
        root.click()
        device.waitForIdle()
        // The confirm control differs by API level and stage.
        val confirm =
            device.wait(Until.findObject(By.textContains("Use this folder")), TIMEOUT)
                ?: device.wait(Until.findObject(By.textContains("SELECT")), 5_000L)
                ?: device.wait(Until.findObject(By.textContains("Allow")), 5_000L)
        if (confirm == null) fail("No confirm button in picker. Hierarchy:\n" + dumpUiHierarchy())
        confirm.click()
        // API 26-29 may show a second "Allow" access dialog.
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
        val treeRoot = DocumentFile.fromTreeUri(context, tree)
        assertNotNull("Could not open granted tree", treeRoot)
        val file = treeRoot!!.createFile("text/plain", "seali-saf-qualification")
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
        // Synchronous instrumented grant; a shell `pm grant` is not reliably
        // visible to the already-running app process on API 33+.
        instrumentation.uiAutomation.grantRuntimePermission(
            context.packageName,
            android.Manifest.permission.POST_NOTIFICATIONS,
        )
        val granted =
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        assertTrue("POST_NOTIFICATIONS grant not visible to app", granted)
    }
}

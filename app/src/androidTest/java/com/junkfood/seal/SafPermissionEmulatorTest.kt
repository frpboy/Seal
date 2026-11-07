package com.junkfood.seal

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import java.io.ByteArrayOutputStream
import java.io.File
import androidx.documentfile.provider.DocumentFile
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
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
 *
 * The picker is launched through SafTestHostActivity (debug target app): the tree
 * grant is only conferred via the ActivityResult path, and persistence and
 * the write/read proof below run against the target app's grants. This
 * qualifies the SAF contract on the device with the same flags the settings
 * page uses; it does not exercise the app's own updateDownloadDir path.
 */
@RunWith(AndroidJUnit4::class)
class SafPermissionEmulatorTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device: UiDevice = UiDevice.getInstance(instrumentation)
    private val context
        get() = instrumentation.targetContext

    /** Dump the current window hierarchy as evidence when a picker assertion
     *  fails, so selector fixes are grounded in the real DocumentsUI layout
     *  instead of guessed labels. Uses UiDevice's own dump (accessibility
     *  service), not the shell uiautomator binary, which produced empty
     *  output on the API 35 CI run. */
    private fun dumpUiHierarchy(): String =
        try {
            ByteArrayOutputStream().use { output ->
                device.dumpWindowHierarchy(output)
                val bytes = output.toByteArray()
                val file = File(context.filesDir, "saf-picker-hierarchy.xml")
                file.writeBytes(bytes)
                check(bytes.isNotEmpty()) { "hierarchy dump empty" }
                // The complete XML is a CI artifact. Do not cut off the
                // controls behind taskbar/outer windows in the assertion.
                "Full ${bytes.size}-byte hierarchy saved to ${file.name}"
            }
        } catch (e: Exception) {
            "hierarchy dump failed: ${e.message}"
        }

    @Test
    fun documentTreeGrantAllowsRealWriteAndRead() {
        for (name in listOf("saf-picker-hierarchy.xml", "saf-host-identity.txt")) {
            val old = File(context.filesDir, name)
            check(!old.exists() || old.delete()) { "Could not clear stale SAF evidence: $name" }
        }
        val priorGrants = context.contentResolver.persistedUriPermissions.map { it.uri }.toSet()
        var createdFile: DocumentFile? = null
        var acquiredTree: Uri? = null
        val scenario = ActivityScenario.launch<SafTestHostActivity>(
            Intent(context, SafTestHostActivity::class.java)
        )
        lateinit var host: SafTestHostActivity
        var cleanupHost: SafTestHostActivity? = null
        try {
            scenario.onActivity { activity ->
                host = activity
                cleanupHost = activity
                File(context.filesDir, "saf-host-identity.txt").writeText(
                    "target=${context.packageName}\n" +
                        "host_uid=${activity.hostUid} instrumentation_uid=${android.os.Process.myUid()}\n" +
                        "host_pid=${activity.hostPid} instrumentation_pid=${android.os.Process.myPid()}\n",
                )
                assertEquals(android.os.Process.myUid(), activity.hostUid)
                assertEquals(android.os.Process.myPid(), activity.hostPid)
                activity.launchPicker()
            }

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
            // API35 run42387288667 full XML: DocumentsUI's enabled
            // android:id/button1 reads exactly "USE THIS FOLDER" (uppercase).
            // Preserve the previously working API26 fallbacks unchanged.
            val confirm =
                device.wait(
                    Until.findObject(By.pkg("com.android.documentsui").text("USE THIS FOLDER")),
                    TIMEOUT,
                )
                    ?: device.wait(Until.findObject(By.textContains("Use this folder")), 5_000L)
                    ?: device.wait(Until.findObject(By.textContains("SELECT")), 5_000L)
                    ?: device.wait(Until.findObject(By.textContains("Allow")), 5_000L)
            if (confirm == null) fail("No confirm button in picker. Hierarchy:\n" + dumpUiHierarchy())
            confirm.click()
            // API 26-29 may show a second "Allow" access dialog.
            device.wait(Until.hasObject(By.textContains("Allow")), 3_000L)
            device.findObject(UiSelector().textContains("Allow")).takeIf { it.exists() }?.click()
            device.waitForIdle()

            // The picker confers the grant through the host's ActivityResult
            // callback; the host persists it like App.updateDownloadDir does.
            val completed = host.await(TIMEOUT)
            if (!completed) {
                fail("Picker result never reached host activity. Hierarchy:\n" + dumpUiHierarchy())
            }
            if (host.persistError != null) {
                fail("takePersistableUriPermission failed: " + host.persistError)
            }
            assertNotNull("Picker returned no tree URI", host.lastUri)
            val tree: Uri = host.lastUri!!
            assertTrue("Selected tree must be a fresh grant, not leftover state", tree !in priorGrants)
            acquiredTree = tree

            // The grant is recorded for the target app (the debug host's package).
            val grant =
                context.contentResolver.persistedUriPermissions.firstOrNull {
                    it.uri == tree && it.isReadPermission && it.isWritePermission
                }
            assertNotNull("No persistable read+write tree grant recorded", grant)

            // Prove real write + read inside the chosen folder via DocumentFile.
            val treeRoot = DocumentFile.fromTreeUri(context, tree)
            assertNotNull("Could not open granted tree", treeRoot)
            val file = treeRoot!!.createFile("text/plain", "seali-saf-qualification-${System.nanoTime()}")
            createdFile = file
            assertNotNull("Create in granted tree failed", file)
            context.contentResolver.openOutputStream(file!!.uri)!!.use {
                it.write("seali-saf-ok".toByteArray())
            }
            val readBack =
                context.contentResolver.openInputStream(file.uri)!!.use { input ->
                    input.readBytes().decodeToString()
                }
            assertEquals("seali-saf-ok", readBack)
        } finally {
            // Do not release any grant that existed before this test.
            try {
                createdFile?.let { assertTrue("Fixture deletion failed", it.delete()) }
            } finally {
                try {
                    // Callback may have arrived just before an earlier assertion failed.
                    val tree = acquiredTree ?: cleanupHost?.lastUri?.takeIf { it !in priorGrants }
                    tree?.let {
                        context.contentResolver.releasePersistableUriPermission(
                            it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                    }
                } finally {
                    scenario.close()
                }
            }
        }
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

package com.junkfood.seal

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.junkfood.seal.util.AUDIO_DIRECTORY
import com.junkfood.seal.util.CUSTOM_COMMAND
import com.junkfood.seal.util.PRIVATE_DIRECTORY
import com.junkfood.seal.util.SDCARD_DOWNLOAD
import com.junkfood.seal.util.SDCARD_URI
import com.junkfood.seal.util.VIDEO_DIRECTORY
import com.tencent.mmkv.MMKV
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Source proposal only. Two separate invocations, each expecting exactly one test.
 * Real DocumentsUI -> real app settings callback -> real persisted grant and I/O.
 * Not the standalone SAF fixture or a signed APK update. No production fix here.
 */
@RunWith(AndroidJUnit4::class)
class DirectorySettingsSafTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private val lifecycle get() = File(context.filesDir, "settings-host-lifecycle.txt")
    private val timeout = 15_000L

    @Test fun actualSdcardSettingsPersistsFreshGrant() = qualify(recreatePending = false)
    @Test fun pendingSdcardResultRoutesAfterRecreation() = qualify(recreatePending = true)

    private fun screenshot(name: String) {
        val image = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        val file = File(context.filesDir, name)
        try {
            file.outputStream().use {
                check(image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
            }
            check(file.length() > 0) { "Empty screenshot: $name" }
        } finally { image.recycle() }
    }

    private fun dumpHierarchy(name: String) {
        ByteArrayOutputStream().use { out ->
            device.dumpWindowHierarchy(out)
            check(out.size() > 0) { "Empty hierarchy: $name" }
            File(context.filesDir, name).writeBytes(out.toByteArray())
        }
    }

    private val pickerPackages = setOf("com.android.documentsui", "com.google.android.documentsui")

    private fun focusedPackageIs(packages: Set<String>): Boolean =
        device.currentPackageName in packages && instrumentation.uiAutomation.windows.any {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isActive && it.isFocused &&
                it.root?.packageName?.toString() in packages
        }

    private fun recordWindows(name: String) {
        File(context.filesDir, name).writeText(
            "uptime=${SystemClock.uptimeMillis()} current=${device.currentPackageName}\n" +
                instrumentation.uiAutomation.windows.joinToString("\n") {
                    "type=${it.type} active=${it.isActive} focused=${it.isFocused} " +
                        "package=${it.root?.packageName}"
                } + "\n" + if (lifecycle.exists()) lifecycle.readText() else "no host lifecycle file\n",
        )
    }

    private fun requirePickerForeground(suffix: String, stage: String) {
        recordWindows("settings-$suffix-$stage-windows.txt")
        assertTrue("Harness: DocumentsUI is not focused ACTIVE application window at $stage; no routing claim",
            focusedPackageIs(pickerPackages))
    }

    private fun awaitFocusedSettings(): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeout
        while (!focusedPackageIs(setOf(context.packageName)) && SystemClock.elapsedRealtime() < deadline) {
            SystemClock.sleep(100)
        }
        return focusedPackageIs(setOf(context.packageName))
    }

    private fun chooseRealTree(suffix: String) {
        requirePickerForeground(suffix, "before-root-selection")
        if (!device.findObject(UiSelector().text("Documents")).exists() &&
            !device.findObject(UiSelector().text("Downloads")).exists()
        ) {
            device.findObject(UiSelector().descriptionContains("Show roots"))
                .takeIf { it.exists() }?.click()
            device.waitForIdle()
        }
        val root = device.wait(Until.findObject(By.text("Documents")), timeout)
            ?: device.wait(Until.findObject(By.text("Downloads")), timeout)
        assertNotNull("No supported Documents/Downloads root; see settings picker XML", root)
        root!!.click()
        device.waitForIdle()
        // Exact labels already observed in the standalone SAF fixture on26/35.
        val confirm = device.wait(
            Until.findObject(By.pkg("com.android.documentsui").text("USE THIS FOLDER")), timeout,
        ) ?: device.wait(Until.findObject(By.textContains("Use this folder")), 5_000L)
            ?: device.wait(Until.findObject(By.textContains("SELECT")), 5_000L)
            ?: device.wait(Until.findObject(By.textContains("Allow")), 5_000L)
        assertNotNull("No supported folder confirmation; see settings picker XML", confirm)
        requirePickerForeground(suffix, "before-folder-confirmation")
        confirm!!.click()
        val allow = device.wait(
            Until.findObject(By.pkg("com.android.documentsui").text("ALLOW")), 3_000L,
        ) ?: device.wait(Until.findObject(By.textContains("Allow")), 3_000L)
        allow?.click()
        device.waitForIdle()
    }

    private fun qualify(recreatePending: Boolean) {
        assertEquals("Only an exclusive disposable emulator/profile is allowed",
            "true", InstrumentationRegistry.getArguments().getString("sealiDisposableProfile"))
        val suffix = if (recreatePending) "recreated" else "fresh"
        val kv = MMKV.defaultMMKV()
        val strings = listOf(SDCARD_URI, VIDEO_DIRECTORY, AUDIO_DIRECTORY)
            .associateWith { if (kv.containsKey(it)) kv.decodeString(it) else null }
        val bools = listOf(SDCARD_DOWNLOAD, PRIVATE_DIRECTORY, CUSTOM_COMMAND)
            .associateWith { if (kv.containsKey(it)) kv.decodeBool(it) else null }
        val videoBefore = App.videoDownloadDir
        val audioBefore = App.audioDownloadDir
        val priorGrants = context.contentResolver.persistedUriPermissions.map { it.uri }.toSet()
        var scenario: ActivityScenario<DirectorySettingsTestActivity>? = null
        var created: DocumentFile? = null
        var terminalFailure: Throwable? = null
        val automation = instrumentation.uiAutomation
        val priorServiceFlags = automation.serviceInfo.flags
        fun collectFailure(block: () -> Unit) {
            try { block() } catch (failure: Throwable) {
                val original = terminalFailure
                if (original == null) terminalFailure = failure else original.addSuppressed(failure)
            }
        }
        fun encodeChecked(key: String, value: String) {
            check(kv.encode(key, value)) { "MMKV encode failed: $key" }
            check(kv.decodeString(key) == value) { "MMKV round-trip failed: $key" }
        }
        fun encodeChecked(key: String, value: Boolean) {
            check(kv.encode(key, value)) { "MMKV encode failed: $key" }
            check(kv.decodeBool(key) == value) { "MMKV round-trip failed: $key" }
        }
        try {
            val service = automation.serviceInfo
            service.flags = priorServiceFlags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            automation.serviceInfo = service
            File(context.filesDir, "settings-$suffix-provenance.txt").writeText(
                "method=" + (if (recreatePending) "pendingSdcardResultRoutesAfterRecreation" else "actualSdcardSettingsPersistsFreshGrant") +
                    "\napi=${Build.VERSION.SDK_INT} pid=${android.os.Process.myPid()} " +
                    "uid=${android.os.Process.myUid()} uptime=${SystemClock.uptimeMillis()}\n",
            )
            // Snapshot before changing any synthetic preference or launching composition.
            encodeChecked(SDCARD_URI, "")
            encodeChecked(SDCARD_DOWNLOAD, false)
            encodeChecked(PRIVATE_DIRECTORY, false)
            encodeChecked(CUSTOM_COMMAND, false)
            check(!lifecycle.exists() || lifecycle.delete())
            if (Build.VERSION.SDK_INT <= 29) {
                assertEquals("Runner must grant storage before instrumentation, then revoke after process exits",
                    android.content.pm.PackageManager.PERMISSION_GRANTED,
                    context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            }
            scenario = ActivityScenario.launch(
                Intent(context, DirectorySettingsTestActivity::class.java),
            )
            scenario.onActivity {
                assertEquals("Host must run under target app UID", context.applicationInfo.uid, it.applicationInfo.uid)
                assertEquals(android.os.Process.myUid(), it.applicationInfo.uid)
            }
            if (!recreatePending) {
                // A canceled AUDIO picker must not write any destination or
                // block a later independent SDCARD selection.
                compose.onNodeWithText(context.getString(R.string.audio_directory))
                    .performScrollTo().performClick()
                assertTrue("Cancel regression: AUDIO picker did not open",
                    device.wait(Until.hasObject(By.pkg("com.android.documentsui")), timeout) ||
                        device.wait(Until.hasObject(By.pkg("com.google.android.documentsui")), 5_000L))
                requirePickerForeground(suffix, "cancel-before-back")
                device.pressBack()
                assertTrue("Harness: canceled picker did not return focused settings", awaitFocusedSettings())
                compose.waitForIdle()
                assertEquals(videoBefore, App.videoDownloadDir)
                assertEquals(audioBefore, App.audioDownloadDir)
                assertEquals("", kv.decodeString(SDCARD_URI, ""))
                assertEquals(priorGrants, context.contentResolver.persistedUriPermissions.map { it.uri }.toSet())
            }
            compose.onNodeWithText(context.getString(R.string.sdcard_directory))
                .performScrollTo().performClick()
            assertTrue("Actual SDCARD row did not open DocumentsUI",
                device.wait(Until.hasObject(By.pkg("com.android.documentsui")), timeout) ||
                    device.wait(Until.hasObject(By.pkg("com.google.android.documentsui")), 5_000L))
            requirePickerForeground(suffix, "picker-open")
            screenshot("settings-$suffix-picker.png")
            dumpHierarchy("settings-$suffix-picker.xml")
            if (recreatePending) {
                val before = lifecycle.readLines().size
                // Do not use ActivityScenario.recreate(): AndroidX first resumes
                // the host and could remove the picker from the foreground.
                // AndroidX Core 1.15.0 works around API26/27 stopped-Activity
                // recreate deferral. It may fall back to platform recreate;
                // second-onCreate + focused-picker checks below remain strict.
                scenario.onActivity { ActivityCompat.recreate(it) }
                val deadline = SystemClock.elapsedRealtime() + timeout
                while (lifecycle.readLines().size <= before && SystemClock.elapsedRealtime() < deadline) {
                    SystemClock.sleep(100)
                }
                assertTrue("Harness could not establish recreation WHILE picker pending; no routing claim",
                    lifecycle.readLines().size > before)
                requirePickerForeground(suffix, "recreated-before-selection")
                screenshot("settings-$suffix-pending.png")
            }
            chooseRealTree(suffix)
            val returned = awaitFocusedSettings()
            recordWindows("settings-$suffix-return-windows.txt")
            assertTrue("Harness: picker did not return to focused settings app; no routing claim", returned)
            assertEquals("Harness: intended settings host must be RESUMED", Lifecycle.State.RESUMED, scenario.state)
            scenario.onActivity {
                assertEquals(DirectorySettingsTestActivity::class.java, it.javaClass)
                assertEquals(Lifecycle.State.RESUMED, it.lifecycle.currentState)
                assertTrue("Harness: intended settings host lacks window focus", it.hasWindowFocus())
            }
            compose.waitForIdle()
            val stored = kv.decodeString(SDCARD_URI, "") ?: ""
            File(context.filesDir, "settings-$suffix-outcome.txt").writeText(
                "sdcard_uri=$stored\nvideo_before=$videoBefore\nvideo_after=${App.videoDownloadDir}\n" +
                    "audio_before=$audioBefore\naudio_after=${App.audioDownloadDir}\n" + lifecycle.readText(),
            )
            screenshot("settings-$suffix-after.png")
            dumpHierarchy("settings-$suffix-after.xml")
            assertTrue("Actual app callback did not persist SDCARD_URI; see outcome/lifecycle", stored.isNotEmpty())
            assertEquals("SDCARD result must not change VIDEO destination", videoBefore, App.videoDownloadDir)
            assertEquals("SDCARD result must not change AUDIO destination", audioBefore, App.audioDownloadDir)
            assertEquals(strings[VIDEO_DIRECTORY], kv.decodeString(VIDEO_DIRECTORY))
            assertEquals(strings[AUDIO_DIRECTORY], kv.decodeString(AUDIO_DIRECTORY))
            val tree = Uri.parse(stored)
            assertFalse("Must prove a fresh grant, not leftover permission", tree in priorGrants)
            assertNotNull("Actual app callback must persist read+write grant",
                context.contentResolver.persistedUriPermissions.find {
                    it.uri == tree && it.isReadPermission && it.isWritePermission
                })
            val root = DocumentFile.fromTreeUri(context, tree)
            assertNotNull(root)
            created = root!!.createFile("text/plain", "seali-settings-${System.nanoTime()}")
            assertNotNull(created)
            val file = created!!
            context.contentResolver.openOutputStream(file.uri)!!.use { it.write("settings-saf-ok".toByteArray()) }
            val text = context.contentResolver.openInputStream(file.uri)!!.use { it.readBytes().decodeToString() }
            assertEquals("settings-saf-ok", text)
            // Deliberately separate from persistence/I/O. A stale description is
            // a UI failure, not evidence that the underlying URI was not stored.
            compose.onNodeWithText(stored).assertExists()
        } catch (original: Throwable) {
            terminalFailure = original
            // Capture CURRENT failure state before any back/close/restore. A
            // diagnostic failure is suppressed onto the original, never a pass.
            collectFailure { dumpHierarchy("settings-$suffix-CURRENT-failure.xml") }
            collectFailure { screenshot("settings-$suffix-CURRENT-failure.png") }
            collectFailure { recordWindows("settings-$suffix-CURRENT-failure-windows.txt") }
            collectFailure {
                File(context.filesDir, "settings-$suffix-CURRENT-failure-outcome.txt").writeText(
                    "failure=${original.javaClass.name}: ${original.message}\n" +
                        "sdcard_uri=${kv.decodeString(SDCARD_URI)}\n" +
                        "video_before=$videoBefore video_after=${App.videoDownloadDir}\n" +
                        "audio_before=$audioBefore audio_after=${App.audioDownloadDir}\n" +
                        if (lifecycle.exists()) lifecycle.readText() else "no host lifecycle file\n",
                )
            }
        } finally {
            collectFailure { if (focusedPackageIs(pickerPackages)) device.pressBack() }
            collectFailure { scenario?.close() }
            collectFailure { created?.let { assertTrue("Synthetic file cleanup failed", it.delete()) } }
            // New-grant enumeration is safe ONLY in the exclusive disposable
            // profile required above, never in an owner's shared/real profile.
            collectFailure {
                context.contentResolver.persistedUriPermissions.filter { it.uri !in priorGrants }.forEach {
                    collectFailure {
                        context.contentResolver.releasePersistableUriPermission(
                            it.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                    }
                }
                check(context.contentResolver.persistedUriPermissions.none { it.uri !in priorGrants }) {
                    "New synthetic grant survived cleanup"
                }
            }
            strings.forEach { (key, value) -> collectFailure {
                if (value == null) {
                    kv.removeValueForKey(key)
                    check(!kv.containsKey(key)) { "MMKV removal failed: $key" }
                } else encodeChecked(key, value)
            } }
            bools.forEach { (key, value) -> collectFailure {
                if (value == null) {
                    kv.removeValueForKey(key)
                    check(!kv.containsKey(key)) { "MMKV removal failed: $key" }
                } else encodeChecked(key, value)
            } }
            collectFailure {
                App.videoDownloadDir = videoBefore
                App.audioDownloadDir = audioBefore
                check(App.videoDownloadDir == videoBefore && App.audioDownloadDir == audioBefore)
            }
            collectFailure {
                val service = automation.serviceInfo
                service.flags = priorServiceFlags
                automation.serviceInfo = service
            }
            // Permission revoke belongs to the runner AFTER instrumentation
            // returns and result/evidence are saved: revocation can kill us.

        }
        terminalFailure?.let { throw it }
    }
}

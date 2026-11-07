package com.junkfood.seal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Result recipient for the SAF tree picker used by SafPermissionEmulatorTest.
 *
 * ACTION_OPEN_DOCUMENT_TREE only confers the tree grant through the
 * ActivityResult path: a bare startActivity has no result recipient, so no
 * grant is delivered and takePersistableUriPermission can never be called.
 * The host belongs to the target app's DEBUG source set, so the callback
 * and instrumentation share one process and UID. It is not exported and is
 * excluded from every release source set. This mirrors the settings tree
 * contract and SDCARD persist call, but does not exercise updateDownloadDir.
 */
class SafTestHostActivity : ComponentActivity() {

    private val contract =
        object : ActivityResultContracts.OpenDocumentTree() {
            override fun createIntent(context: Context, input: Uri?): Intent {
                return super.createIntent(context, input).apply {
                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                    )
                }
            }
        }

    private val picker =
        registerForActivityResult(contract) { uri ->
            lastUri = uri
            persistError = null
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                } catch (e: SecurityException) {
                    persistError = e.message
                }
            }
            done.countDown()
        }

    private var canLaunch = false
    private var done = CountDownLatch(1)
    @Volatile var lastUri: Uri? = null
        private set
    @Volatile var persistError: String? = null
        private set
    val hostUid: Int = android.os.Process.myUid()
    val hostPid: Int = android.os.Process.myPid()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Recreation must not open a second picker.
        canLaunch = savedInstanceState == null
    }

    fun launchPicker() {
        check(canLaunch) { "Picker already launched or host recreated" }
        canLaunch = false
        picker.launch(null)
    }

    fun await(timeoutMs: Long): Boolean = done.await(timeoutMs, TimeUnit.MILLISECONDS)
}

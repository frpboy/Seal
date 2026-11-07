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
 * The emulator evidence matches this: on API 26 the picker flow completed but
 * no persistable grant existed. This host mirrors the flags of the settings
 * page launcher and the persist call of App.updateDownloadDir's SDCARD
 * branch. It runs in the TEST package, so it qualifies the SAF contract,
 * persistence and write/read mechanics on the device; it does not exercise
 * the app's own updateDownloadDir path.
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        picker.launch(null)
    }

    companion object {
        @Volatile var lastUri: Uri? = null
            private set
        @Volatile var persistError: String? = null
            private set
        @Volatile private var done = CountDownLatch(1)

        fun reset() {
            lastUri = null
            persistError = null
            done = CountDownLatch(1)
        }

        fun await(timeoutMs: Long): Boolean = done.await(timeoutMs, TimeUnit.MILLISECONDS)
    }
}

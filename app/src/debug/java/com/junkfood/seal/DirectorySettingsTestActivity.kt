package com.junkfood.seal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.junkfood.seal.ui.page.settings.directory.DownloadDirectoryPreferences
import java.io.File

/** Debug-only host of the ACTUAL settings composable, rebuilt in every onCreate.
 * No callback shim or replacement routing/persistence implementation.
 */
class DirectorySettingsTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        File(filesDir, "settings-host-lifecycle.txt").appendText(
            "onCreate=${System.nanoTime()} restored=${savedInstanceState != null} " +
                "uid=${android.os.Process.myUid()} pid=${android.os.Process.myPid()}\n",
        )
        setContent { MaterialTheme { DownloadDirectoryPreferences(onNavigateBack = { finish() }) } }
    }
}

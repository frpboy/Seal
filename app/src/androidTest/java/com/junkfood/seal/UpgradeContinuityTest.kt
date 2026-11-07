package com.junkfood.seal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.junkfood.seal.database.AppDatabase
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.util.PreferenceUtil.encodeString
import com.junkfood.seal.util.PreferenceUtil.getString
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A1: install-over upgrade continuity with preserved data.
 *
 * CI flow (upgrade-continuity.yml):
 * 1. Install the published v2.0.0-alpha.5 universal APK on the emulator.
 * 2. Run ONLY seedUpgradeState() against it (same signer -> instrumentation allowed).
 * 3. `adb install -r` the newly built universal APK (OS enforces same cert +
 *    non-downgrade versionCode; failure here fails the job).
 * 4. Run ONLY verifyUpgradeStatePreserved() - private app data (MMKV prefs and
 *    filesDir) must survive the upgrade, then the app must still launch.
 */
@RunWith(AndroidJUnit4::class)
class UpgradeContinuityTest {

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun seedUpgradeState() {
        encodeString(SEED_KEY, SEED_VALUE)
        java.io.File(context.filesDir, SEED_FILE).writeText(SEED_VALUE)
        assertEquals(SEED_VALUE, SEED_KEY.getString(""))
        // Real history row in the Room database (schema v5 on both sides -
        // verified against the v2.0.0-alpha.5 tag's exported schema).
        androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
            .build()
            .apply {
                kotlinx.coroutines.runBlocking {
                    videoInfoDao().insert(
                        DownloadedVideoInfo(
                            id = 0,
                            videoTitle = "Seali A1 seed",
                            videoAuthor = "Seali",
                            videoUrl = "https://example.invalid/a1-seed",
                            thumbnailUrl = "",
                            videoPath = SEED_PATH,
                        )
                    )
                }
                close()
            }
    }

    @Test
    fun verifyUpgradeStatePreserved() {
        assertEquals("MMKV preference lost across upgrade", SEED_VALUE, SEED_KEY.getString(""))
        val file = java.io.File(context.filesDir, SEED_FILE)
        assertEquals("filesDir content lost across upgrade", SEED_VALUE, file.readText())
        // Post-upgrade sanity: version code must exceed alpha5's 200_000_150.
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val vc = if (android.os.Build.VERSION.SDK_INT >= 28) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        assert(vc > 200_000_150L) { "versionCode $vc not greater than alpha5 baseline" }
        // History row seeded pre-upgrade must survive.
        androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
            .build()
            .apply {
                kotlinx.coroutines.runBlocking {
                    val history = videoInfoDao().getDownloadHistory()
                    assert(history.any { it.videoPath == SEED_PATH }) {
                        "Room download history lost across upgrade"
                    }
                }
                close()
            }
    }

    companion object {
        private const val SEED_KEY = "upgrade_continuity_seed"
        private const val SEED_VALUE = "seali-a1-marker"
        private const val SEED_FILE = "upgrade-continuity-seed.txt"
        private const val SEED_PATH = "/storage/emulated/0/Download/Seali/a1-seed.mp4"
    }
}

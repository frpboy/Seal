package com.junkfood.seal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.junkfood.seal.database.AppDatabase
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.util.PreferenceUtil.encodeString
import com.junkfood.seal.util.PreferenceUtil.getString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Continuity data assertions shared by the debug same-install qualification
 * and the separately gated release install-over harness.
 *
 * Running seed then verify without an intervening APK install proves only
 * same-install persistence. A signed install-over claim additionally needs
 * pinned actual baseline/candidate APK hashes, certificate/ABI verification,
 * baseline install, seed, candidate install-r, then verify on an isolated
 * supported device. The old alpha5 workflow does NOT qualify delivered alpha6.
 * All oracles use JUnit checks, independent of JVM assertion-enable flags.
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
        ContinuityOracles.versionExceedsAlpha5(vc)
        // History row seeded pre-upgrade must survive.
        androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
            .build()
            .apply {
                kotlinx.coroutines.runBlocking {
                    val history = videoInfoDao().getDownloadHistory()
                    ContinuityOracles.historyContainsMarker(history.map { it.videoPath }, SEED_PATH)
                }
                close()
            }
    }

    @Test
    fun negativeControlsRejectMissingState() {
        fun mustReject(label: String, check: () -> Unit) {
            var rejected = false
            try { check() } catch (expected: AssertionError) { rejected = true }
            assertTrue("Negative control did not reject $label", rejected)
        }
        mustReject("old version") { ContinuityOracles.versionExceedsAlpha5(200_000_150L) }
        mustReject("missing Room marker") { ContinuityOracles.historyContainsMarker(emptyList(), SEED_PATH) }
        ContinuityOracles.versionExceedsAlpha5(200_000_162L)
        ContinuityOracles.historyContainsMarker(listOf(SEED_PATH), SEED_PATH)
    }

    companion object {
        private const val SEED_KEY = "upgrade_continuity_seed"
        private const val SEED_VALUE = "seali-a1-marker"
        private const val SEED_FILE = "upgrade-continuity-seed.txt"
        private const val SEED_PATH = "/storage/emulated/0/Download/Seali/a1-seed.mp4"
    }
}

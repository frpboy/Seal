import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Locks the version-code ordering invariants every Play upload relies on. */
class VersionTest {
    @Test
    fun `tracks are strictly ordered for the same version numbers`() {
        val alpha = Version.Alpha(2, 0, 0, 1)
        val beta = Version.Beta(2, 0, 0, 1)
        val rc = Version.ReleaseCandidate(2, 0, 0, 1)
        val stable = Version.Stable(2, 0, 0)
        assertTrue(alpha.code < beta.code, "alpha must sort before beta")
        assertTrue(beta.code < rc.code, "beta must sort before rc")
        assertTrue(rc.code < stable.code, "rc must sort before stable")
    }

    @Test
    fun `incrementing the build number increments the version code`() {
        assertTrue(Version.Alpha(2, 0, 0, 5).code < Version.Alpha(2, 0, 0, 6).code)
    }

    @Test
    fun `higher releases always produce higher codes`() {
        val releases =
            listOf(
                Version.Alpha(2, 0, 0, 5),
                Version.Alpha(2, 0, 0, 6),
                Version.Beta(2, 0, 0, 1),
                Version.ReleaseCandidate(2, 0, 0, 1),
                Version.Stable(2, 0, 0),
                Version.Alpha(2, 0, 1, 1),
                Version.Stable(2, 1, 0),
            )
        releases.zipWithNext().forEach { (a, b) ->
            assertTrue(a.code < b.code, "${a.name} must sort before ${b.name}")
        }
    }

    @Test
    fun `current version matches the guarded versionCode in app build file`() {
        assertEquals(200_000_160L, currentVersion.code)
        assertEquals("2.0.0-alpha.6", currentVersion.name)
    }
}

package com.junkfood.seal.util

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Matrix for the false-baseline fix: every pref x on-disk-bytes combination,
 *  the corrupt-pref freeze regression, and the safe zipapp metadata probe. */
class YtDlpInstallResolutionTest {

    private val bundled = "2026.10.26"
    private val bundledHash = "e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3"
    private val legacyHash = "0011223344556677889900aabbccddee0011223344556677889900aabbccddee"

    private fun resolve(pref: String?, hash: String?) =
        YtDlpInstallResolution.resolve(pref, hash, bundledHash, bundled)

    @Test
    fun `pref absent and bundled bytes resolves bundled and repairs pref`() {
        val r = resolve(null, bundledHash)
        assertEquals(YtDlpInstallResolution.State.BUNDLED, r.state)
        assertEquals(bundled, r.effectiveVersion)
        assertEquals(bundled, r.prefRepairVersion)
    }

    @Test
    fun `pref absent and missing binary resolves bundled and repairs pref`() {
        val r = resolve(null, null)
        assertEquals(YtDlpInstallResolution.State.BUNDLED, r.state)
        assertEquals(bundled, r.effectiveVersion)
        assertEquals(bundled, r.prefRepairVersion)
    }

    @Test
    fun `pref absent and foreign unprobeable bytes resolves unknown legacy without repair`() {
        val r = resolve(null, legacyHash)
        assertEquals(YtDlpInstallResolution.State.UNKNOWN_LEGACY, r.state)
        assertNull(r.effectiveVersion)
        assertNull(r.prefRepairVersion)
    }

    @Test
    fun `stale high pref with bundled bytes is repaired down to bundled`() {
        // Missing-binary re-extract: init wrote the bundle back over a removed
        // updated binary while the pref still claims the newer version.
        val r = resolve("2999.01.01", bundledHash)
        assertEquals(YtDlpInstallResolution.State.BUNDLED, r.state)
        assertEquals(bundled, r.effectiveVersion)
        assertEquals(bundled, r.prefRepairVersion)
    }

    @Test
    fun `stale low pref with bundled bytes is repaired up to bundled`() {
        val r = resolve("2024.09.27", bundledHash)
        assertEquals(YtDlpInstallResolution.State.BUNDLED, r.state)
        assertEquals(bundled, r.prefRepairVersion)
    }

    @Test
    fun `matching pref with bundled bytes needs no repair`() {
        val r = resolve(bundled, bundledHash)
        assertEquals(YtDlpInstallResolution.State.BUNDLED, r.state)
        assertNull(r.prefRepairVersion)
    }

    @Test
    fun `pref recorded foreign bytes keeps pref version - newer binary not downgraded`() {
        val r = resolve("2999.01.01", legacyHash)
        assertEquals(YtDlpInstallResolution.State.PREF_RECORDED, r.state)
        assertEquals("2999.01.01", r.effectiveVersion)
        assertNull(r.prefRepairVersion)
    }

    @Test
    fun `pref recorded with missing binary resolves bundled and repairs`() {
        val r = resolve("2999.01.01", null)
        assertEquals(YtDlpInstallResolution.State.BUNDLED, r.state)
        assertEquals(bundled, r.prefRepairVersion)
    }

    @Test
    fun `corrupt pref with foreign bytes resolves unknown legacy - no silent freeze`() {
        // Regression: an unparseable pref previously fed isNewerVersion as the
        // installed side, which always answered "not newer" and froze updates.
        val r = resolve("corrupted", legacyHash)
        assertEquals(YtDlpInstallResolution.State.UNKNOWN_LEGACY, r.state)
        assertNull(r.effectiveVersion)
        assertNull(r.prefRepairVersion)
    }

    @Test
    fun `corrupt pref with bundled bytes still repairs to bundled`() {
        val r = resolve("corrupted", bundledHash)
        assertEquals(YtDlpInstallResolution.State.BUNDLED, r.state)
        assertEquals(bundled, r.prefRepairVersion)
    }

    @Test
    fun `bundled hash comparison is case insensitive`() {
        val r = resolve(null, bundledHash.uppercase())
        assertEquals(YtDlpInstallResolution.State.BUNDLED, r.state)
    }

    @Test
    fun `unknown legacy allows release equal to bundle`() {
        assertTrue(YtDlpInstallResolution.allowOverUnknownLegacy("2026.10.26", bundled))
    }

    @Test
    fun `unknown legacy allows release newer than bundle`() {
        assertTrue(YtDlpInstallResolution.allowOverUnknownLegacy("2026.11.05", bundled))
        assertTrue(YtDlpInstallResolution.allowOverUnknownLegacy("2026.10.26.1", bundled))
    }

    @Test
    fun `unknown legacy refuses release older than bundle`() {
        assertFalse(YtDlpInstallResolution.allowOverUnknownLegacy("2026.10.25", bundled))
        assertFalse(YtDlpInstallResolution.allowOverUnknownLegacy("2024.09.27", bundled))
    }

    @Test
    fun `malformed candidate tag is refused downstream by strict monotonic check`() {
        assertFalse(YtDlpUpdateVerifier.isNewerVersion("not-a-version", "0.0.0"))
    }

    @Test
    fun `two-part pref is unparseable and resolves unknown legacy`() {
        val r = resolve("2026.10", legacyHash)
        assertEquals(YtDlpInstallResolution.State.UNKNOWN_LEGACY, r.state)
        assertNull(r.effectiveVersion)
        assertNull(r.prefRepairVersion)
    }

    @Test
    fun `spoofed high version metadata cannot freeze updates`() {
        // Regression for review finding: a hostile or stale binary can CLAIM
        // any __version__ in its zipapp metadata (e.g. 2999.01.01). The
        // decision path never consumes that claim: pref-absent foreign bytes
        // stay UNKNOWN_LEGACY and resolve through a verified signed release
        // gated on >= the shipped bundle. The claim survives only as a
        // clearly marked unverified diagnostic hint (updater side).
        val claim = "2999.01.01"
        val r = resolve(null, legacyHash) // claim deliberately never passed in
        assertEquals(YtDlpInstallResolution.State.UNKNOWN_LEGACY, r.state)
        assertNull(r.effectiveVersion)
        assertNull(r.prefRepairVersion)
        assertTrue(YtDlpInstallResolution.allowOverUnknownLegacy("2026.11.05", bundled))
        assertTrue(YtDlpInstallResolution.allowOverUnknownLegacy(bundled, bundled))
        assertFalse(YtDlpInstallResolution.allowOverUnknownLegacy("2024.09.27", bundled))
        // And the claim itself parses - proving the decision ignores it, not
        // that the input was invalid.
        assertTrue(YtDlpInstallResolution.isParseableVersion(claim))
    }

    @Test
    fun `probeZipappVersion reads version metadata without executing`() {
        val zip = File.createTempFile("zipapp-probe", null)
        ZipOutputStream(zip.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("yt_dlp/version.py"))
            out.write("# Autogenerated\n\n__version__ = '2024.09.27'\n".toByteArray())
            out.closeEntry()
        }
        assertEquals("2024.09.27", YtDlpInstallResolution.probeZipappVersion(zip))
        zip.delete()
    }

    @Test
    fun `probeZipappVersion returns null for a non-zip binary`() {
        val notZip = File.createTempFile("zipapp-probe-notzip", null)
        notZip.writeBytes("seali-legacy-fixture-not-a-zip".toByteArray())
        assertNull(YtDlpInstallResolution.probeZipappVersion(notZip))
        notZip.delete()
    }

    @Test
    fun `probeZipappVersion returns null when the entry is missing or has no version`() {
        val noEntry = File.createTempFile("zipapp-probe-noentry", null)
        ZipOutputStream(noEntry.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("yt_dlp/other.py"))
            out.write("x = 1".toByteArray())
            out.closeEntry()
        }
        assertNull(YtDlpInstallResolution.probeZipappVersion(noEntry))
        noEntry.delete()

        val noVersion = File.createTempFile("zipapp-probe-noversion", null)
        ZipOutputStream(noVersion.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("yt_dlp/version.py"))
            out.write("# empty\n".toByteArray())
            out.closeEntry()
        }
        assertNull(YtDlpInstallResolution.probeZipappVersion(noVersion))
        noVersion.delete()
    }
}

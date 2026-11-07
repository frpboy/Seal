package com.junkfood.seal.util

import java.io.File
import java.util.zip.ZipFile

/**
 * Honest resolution of the actually installed yt-dlp binary.
 *
 * Fixes the false-baseline defect: the library extracts its bundled yt-dlp
 * only when the on-disk binary is missing (YoutubeDL.init_ytdlp), and
 * YoutubeDLUpdater only writes the dlpVersion pref after a successful update,
 * so an existing install that never updated keeps its OLD extracted binary
 * with the pref absent. The previous pref-or-bundled fallback then reported
 * the NEW bundled version and the updater skipped updates indefinitely.
 *
 * Ground truth order: (1) on-disk bytes proven identical to the pinned bundle
 * via SHA-256 win over any pref claim (covers missing-binary re-extract,
 * where the pref is stale-high); (2) a parseable pref records the last
 * verified install and is trusted for bytes that differ from the bundle
 * (verified updates always write the pref after the atomic rename);
 * (3) nothing else is trusted: unknown legacy binary. Its own zipapp metadata
 * (yt_dlp/version.py) may be probed WITHOUT execution, but that string is
 * self-asserted by whoever produced the file - it is kept only as a clearly
 * marked unverified diagnostic hint (no UI surface today) and NEVER drives an
 * update decision or the
 * verified dlpVersion pref. Unknown legacy is resolved only by installing a
 * verified, signature-checked upstream release not older than the shipped
 * bundle; nothing is deleted first.
 */
object YtDlpInstallResolution {

    enum class State {
        /** On-disk bytes are (or will be, after init re-extract) the pinned bundle. */
        BUNDLED,

        /** Parseable pref records a verified install whose bytes differ from the bundle. */
        PREF_RECORDED,

        /** Neither bundle bytes nor a usable pref: pre-baseline or unmanaged binary. */
        UNKNOWN_LEGACY,
    }

    data class Resolution(
        val state: State,
        /** Version the updater may compare against; null for UNKNOWN_LEGACY. */
        val effectiveVersion: String?,
        /**
         * When non-null, the caller writes dlpVersion/dlpVersionName to this
         * version. Set ONLY when the on-disk bytes were proven identical to
         * the pinned bundle hash, so the pref becomes truthful rather than
         * assumed. Self-asserted zipapp metadata never qualifies.
         */
        val prefRepairVersion: String?,
    )

    /** Same shape YtDlpUpdateVerifier.isNewerVersion requires: at least three
     *  dot-separated numeric parts. */
    fun isParseableVersion(version: String): Boolean {
        val parts = version.split('.')
        return parts.size >= 3 && parts.all { it.toIntOrNull() != null }
    }

    /**
     * Read the version a yt-dlp zipapp CLAIMS in its own metadata WITHOUT
     * executing anything: opens the ZIP central directory and reads the small
     * yt_dlp/version.py entry. Returns null on any failure (not a ZIP, entry
     * missing, no version line, unreadable). Safe against hostile content:
     * fixed-size read, regex parse, no code execution, no decompression bombs
     * beyond one capped entry.
     *
     * The result is SELF-ASSERTED by whoever produced the file: use it only
     * as an unverified diagnostic hint (no UI surface today), never as an
     * update-decision input and never as the verified dlpVersion pref.
     */
    fun probeZipappVersion(file: File): String? =
        try {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry("yt_dlp/version.py") ?: return null
                if (entry.size > 64 * 1024) return null
                val text = zip.getInputStream(entry).use { it.readBytes() }.decodeToString()
                Regex("""__version__\s*=\s*'([^']+)'""").find(text)?.groupValues?.get(1)
            }
        } catch (e: Exception) {
            null
        }

    fun resolve(
        prefVersion: String?,
        onDiskSha256: String?,
        bundledSha256: String,
        bundledVersion: String,
    ): Resolution =
        when {
            // Missing binary: library init re-extracts the bundle at startup,
            // so the honest version is the bundled one; if this raced init, a
            // verified update still installs over nothing safely.
            onDiskSha256 == null ->
                Resolution(
                    State.BUNDLED,
                    bundledVersion,
                    prefRepairVersion = if (prefVersion != bundledVersion) bundledVersion else null,
                )

            // Bytes are exactly the pinned bundle: honest version is bundled
            // regardless of the pref (stale-low, stale-high after a
            // missing-binary re-extract, or corrupt).
            onDiskSha256.equals(bundledSha256, ignoreCase = true) ->
                Resolution(
                    State.BUNDLED,
                    bundledVersion,
                    prefRepairVersion = if (prefVersion != bundledVersion) bundledVersion else null,
                )

            // Verified updates write the pref only after the atomic rename, so
            // a parseable pref means these bytes are that verified (possibly
            // newer than the bundle) version. Kept as-is: never downgraded.
            prefVersion != null && isParseableVersion(prefVersion) ->
                Resolution(State.PREF_RECORDED, prefVersion, prefRepairVersion = null)

            // Nothing trustworthy: an old extracted runtime from before this
            // baseline, an unmanaged binary, or one with a corrupt pref.
            // Self-asserted zipapp metadata does NOT change this state - only
            // a verified signed release may replace the binary, and no pref
            // baseline is written that the bytes did not prove.
            else ->
                Resolution(State.UNKNOWN_LEGACY, effectiveVersion = null, prefRepairVersion = null)
        }

    /**
     * Guard for UNKNOWN_LEGACY installs: accept only a verified upstream
     * release that is not older than the shipped bundle (newer than, or equal
     * to, the bundle). Replayed old-but-genuinely-signed releases are refused,
     * while the normal latest-release path proceeds.
     */
    fun allowOverUnknownLegacy(candidateTag: String, bundledVersion: String): Boolean =
        !YtDlpUpdateVerifier.isNewerVersion(bundledVersion, candidateTag)
}

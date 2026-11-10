package com.frpboy.seali.util

import com.yausername.youtubedl_android.YoutubeDLRequest

/**
 * GHSA-vx4q-3cr2-7cg2: never pass fragmented manifest URLs to aria2c.
 * Enforced at every execution boundary, including custom-config requests and
 * when the aria2 preference is off. Direct-file downloader choice is preserved.
 * Commands follow wrapper options, so this protocol override is last on the CLI.
 * This is one advisory workaround, not a claim that bundled yt-dlp is current.
 */
internal fun YoutubeDLRequest.enforceNativeManifestDownloads(): YoutubeDLRequest =
    addCommands(listOf("--downloader", "dash,m3u8:native"))

package com.frpboy.seali.util

import com.yausername.youtubedl_android.YoutubeDLRequest

/**
 * The functional alpha contains no aria2 binary and no FFmpeg network support.
 * Fence every execution boundary, including prior custom downloader overrides.
 * Commands follow wrapper options, so the native downloader is last on the CLI.
 */
internal fun YoutubeDLRequest.enforceNativeManifestDownloads(): YoutubeDLRequest =
    addCommands(listOf("--downloader", "native", "--downloader", "http,ftp,dash,m3u8:native", "--no-break-match-filters", "--break-match-filters", "!is_live & !is_upcoming & live_status!=?is_live & live_status!=?is_upcoming & live_status!=?post_live"))

package com.frpboy.seali.ui.component

import java.util.Locale

/** Literal local filename extension, not a probed media container or guessed duration. */
internal fun historyFileExtension(path: String): String? {
    if (path.any { it == '?' || it == '#' || it.isISOControl() }) return null
    return path.substringAfterLast('/').substringAfterLast('.', "")
        .takeIf { it.matches(Regex("[A-Za-z0-9]{1,8}")) }
        ?.uppercase(Locale.ROOT)
}

/** Presentation only. Original extractor identity stays in filters and stored records. */
internal fun historyProviderLabel(extractor: String): String =
    if (extractor.equals("youtube", ignoreCase = true)) "YouTube" else extractor

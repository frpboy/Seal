package com.frpboy.seali.ui.page.settings.about

/** Cosmetic display only. Callers keep the original destination for openUri. */
internal fun aboutLinkLabel(url: String): String =
    url.removePrefix("https://").removePrefix("www.").removeSuffix("/")

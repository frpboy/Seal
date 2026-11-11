package com.frpboy.seali.database.backup

import com.frpboy.seali.database.objects.CommandTemplate
import com.frpboy.seali.database.objects.DownloadedVideoInfo
import com.frpboy.seali.database.objects.OptionShortcut
import kotlinx.serialization.Serializable

@Serializable
data class Backup(
    val templates: List<CommandTemplate>? = null,
    val shortcuts: List<OptionShortcut>? = null,
    val downloadHistory: List<DownloadedVideoInfo>? = null,
)

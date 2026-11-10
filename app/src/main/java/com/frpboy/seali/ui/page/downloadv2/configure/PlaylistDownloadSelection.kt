package com.frpboy.seali.ui.page.downloadv2.configure

import com.frpboy.seali.util.DownloadType

/** Dispatch the current visible choice before dismissing, for both playlist and multi-URL sheets. */
internal fun submitPlaylistSelection(
    selectedType: DownloadType,
    onDownload: (DownloadType) -> Unit,
    onDismissRequest: () -> Unit,
) {
    onDownload(selectedType)
    onDismissRequest()
}

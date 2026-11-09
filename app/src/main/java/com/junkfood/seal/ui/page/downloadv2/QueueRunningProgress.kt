package com.junkfood.seal.ui.page.downloadv2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.download.Task

@Composable
internal fun QueueRunningProgress(state: Task.DownloadState.Running, modifier: Modifier = Modifier) {
    val progress = measuredQueueProgress(state.progress)
    val telemetry = remember(state.progressText) { queueProgressTelemetry(state.progressText) }
    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (progress == null) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        else LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text(
            text = progress?.let { stringResource(R.string.queue_progress_percent, (it * 100).toInt()) }
                ?: stringResource(R.string.status_downloading),
            style = MaterialTheme.typography.labelLarge,
        )
        telemetry.speed?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        telemetry.eta?.let { Text(stringResource(R.string.queue_progress_eta, it), style = MaterialTheme.typography.bodySmall) }
    }
}

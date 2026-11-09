package com.frpboy.seali.ui.page.downloadv2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.frpboy.seali.R
import com.frpboy.seali.download.Task
import com.frpboy.seali.util.SafeErrorReport

/** No image request/placeholder when metadata failed; original URL remains in existing local actions. */
@Composable
internal fun CompactQueueErrorCard(
    task: Task,
    viewState: Task.ViewState,
    error: Task.DownloadState.Error,
    modifier: Modifier = Modifier,
    onActionPost: (Task, UiAction) -> Unit,
    onShowActions: () -> Unit,
) {
    val title=remember(task.url,viewState.title) { queueTaskDisplayTitle(task.url,viewState.title) }
    val reason=remember(error.throwable) { SafeErrorReport.reason(error.throwable) }
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title ?: stringResource(R.string.queue_metadata_unavailable), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.status_error), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge)
            Text(reason, style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth()) {
                TextButton(modifier = Modifier.weight(1f), onClick = { onActionPost(task,UiAction.Resume) }) {
                    Text(stringResource(R.string.queue_retry))
                }
                TextButton(modifier = Modifier.weight(1f), onClick = { onActionPost(task,UiAction.CopyErrorReport(error.throwable)) }) {
                    Text(stringResource(R.string.copy_error_report))
                }
            }
            TextButton(onClick = onShowActions) { Text(stringResource(R.string.show_more_actions)) }
        }
    }
}

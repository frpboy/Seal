package com.junkfood.seal.ui.page.downloadv2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R

/** Uses the existing add-link flow. It never reads the clipboard or starts a download by itself. */
@Composable
fun QueueEmptyState(filter: Filter, onAddLink: () -> Unit, modifier: Modifier = Modifier) {
    val title = when (filter) {
        Filter.All -> R.string.you_ll_find_your_downloads_here
        Filter.Downloading -> R.string.seali_empty_active
        Filter.Canceled -> R.string.seali_empty_canceled
        Filter.Finished -> R.string.queue_no_finished_this_session
    }
    val hint = when (filter) {
        Filter.All -> R.string.seali_empty_action_hint
        Filter.Downloading -> R.string.seali_empty_active_hint
        Filter.Canceled -> R.string.seali_empty_canceled_hint
        Filter.Finished -> R.string.queue_saved_history_hint
    }
    BoxWithConstraints(modifier) {
        val compact = maxHeight < 400.dp
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.widthIn(max = 420.dp).verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (filter == Filter.All && !compact) {
                    Icon(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(120.dp),
                    )
                }
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(
                    stringResource(hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                FilledTonalButton(onClick = onAddLink) { Text(stringResource(R.string.seali_add_link)) }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

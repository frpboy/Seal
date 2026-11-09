package com.frpboy.seali.ui.page.settings.interaction

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ViewQuilt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.frpboy.seali.R
import com.frpboy.seali.download.DownloaderV2
import com.frpboy.seali.ui.component.PreferenceItem
import com.frpboy.seali.util.InterfaceSwitchGuard
import com.frpboy.seali.util.PreferenceUtil
import com.frpboy.seali.util.UI_STYLE_CLASSIC
import com.frpboy.seali.util.UI_STYLE_V2
import org.koin.compose.koinInject

@Composable
internal fun InterfaceStylePreference() {
                val context = LocalContext.current
                val downloader: DownloaderV2 = koinInject()
                var showInterfaceDialog by rememberSaveable { mutableStateOf(false) }
                val currentUiStyle = remember { PreferenceUtil.getUiStyle() }
                PreferenceItem(
                    title = stringResource(R.string.interface_style),
                    icon = Icons.Outlined.ViewQuilt,
                    description =
                        stringResource(
                            if (currentUiStyle == UI_STYLE_CLASSIC) R.string.interface_classic_summary
                            else R.string.interface_v2_summary
                        ),
                ) {
                    showInterfaceDialog = true
                }
                if (showInterfaceDialog) {
                    InterfaceStyleDialog(
                        current = currentUiStyle,
                        onDismiss = { showInterfaceDialog = false },
                        onSelect = { selected ->
                            val active =
                                InterfaceSwitchGuard.hasActiveDownloads(
                                    downloader.getTaskStateMap().values.map { it.downloadState }
                                )
                            if (active) {
                                Toast.makeText(
                                        context,
                                        R.string.interface_switch_active,
                                        Toast.LENGTH_LONG,
                                    )
                                    .show()
                            } else if (selected != currentUiStyle) {
                                PreferenceUtil.updateUiStyle(selected)
                                (context as? Activity)?.recreate()
                            }
                            showInterfaceDialog = false
                        },
                    )
                }
}

@Composable
private fun InterfaceStyleDialog(
    current: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    var selected by rememberSaveable(current) { mutableIntStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.interface_style)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                InterfaceChoice(
                    title = stringResource(R.string.interface_v2),
                    description = stringResource(R.string.interface_v2_summary),
                    selected = selected == UI_STYLE_V2,
                    onSelect = { selected = UI_STYLE_V2 },
                )
                InterfaceChoice(
                    title = stringResource(R.string.interface_classic),
                    description = stringResource(R.string.interface_classic_summary),
                    selected = selected == UI_STYLE_CLASSIC,
                    onSelect = { selected = UI_STYLE_CLASSIC },
                )
                Text(
                    text = stringResource(R.string.interface_switch_explanation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selected) }) {
                Text(stringResource(R.string.interface_switch))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun InterfaceChoice(title: String, description: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

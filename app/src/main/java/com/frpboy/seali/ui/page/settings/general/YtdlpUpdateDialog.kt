package com.frpboy.seali.ui.page.settings.general

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.frpboy.seali.App
import com.frpboy.seali.util.PreferenceUtil
import com.frpboy.seali.util.NightlyChannelSelection
import com.frpboy.seali.util.NightlyChannelPolicy
import com.frpboy.seali.util.FDROID_RUNTIME_AUTO_UPDATE_CONSENT
import com.frpboy.seali.util.PreferenceUtil.getBoolean
import kotlinx.coroutines.launch
import com.frpboy.seali.R
import com.frpboy.seali.ui.common.intState
import com.frpboy.seali.ui.component.ConfirmButton
import com.frpboy.seali.ui.component.DismissButton
import com.frpboy.seali.ui.component.SealDialog
import com.frpboy.seali.util.PreferenceStrings.getUpdateIntervalText
import com.frpboy.seali.util.PreferenceUtil.getLong
import com.frpboy.seali.util.PreferenceUtil.updateBoolean
import com.frpboy.seali.util.PreferenceUtil.updateInt
import com.frpboy.seali.util.PreferenceUtil.updateLong
import com.frpboy.seali.util.UpdateIntervalList
import com.frpboy.seali.util.YT_DLP_AUTO_UPDATE
import com.frpboy.seali.util.YT_DLP_NIGHTLY
import com.frpboy.seali.util.YT_DLP_STABLE
import com.frpboy.seali.util.YT_DLP_UPDATE_CHANNEL
import com.frpboy.seali.util.YT_DLP_UPDATE_INTERVAL
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

@Composable
private fun DialogSingleChoiceItem(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    label: String,
    labelContainerColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .selectable(selected = selected, enabled = true, onClick = onClick)
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        RadioButton(
            modifier = Modifier.clearAndSetSemantics {},
            selected = selected,
            onClick = onClick,
        )

        Text(text = text, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.weight(1f))
        Surface(modifier.padding(end = 12.dp), shape = CircleShape, color = labelContainerColor) {
            Text(
                modifier = Modifier.padding(4.dp),
                text = label,
                color = MaterialTheme.colorScheme.contentColorFor(labelContainerColor),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YtdlpUpdateChannelDialog(modifier: Modifier = Modifier, onDismissRequest: () -> Unit) {
    var selection by remember { mutableStateOf(PreferenceUtil.getYtdlpChannelSelection()) }
    var showNightlyWarning by remember { mutableStateOf(false) }
    var dontWarnPending by remember { mutableStateOf(false) }
    fun denyNightly() {
        selection = NightlyChannelSelection()
        dontWarnPending = false
        showNightlyWarning = false
    }
    if (showNightlyWarning) {
        AlertDialog(
            onDismissRequest = { denyNightly() },
            title = { Text(stringResource(R.string.runtime_nightly_warning_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.runtime_nightly_warning_body))
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                            value = dontWarnPending,
                            role = Role.Checkbox,
                            onValueChange = { dontWarnPending = it },
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = dontWarnPending, onCheckedChange = null)
                        Text(stringResource(R.string.runtime_nightly_dont_show_again))
                    }
                    Text(stringResource(R.string.runtime_nightly_save_hint))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selection = NightlyChannelSelection(YT_DLP_NIGHTLY, true, dontWarnPending)
                    showNightlyWarning = false
                }) { Text(stringResource(R.string.runtime_nightly_allow)) }
            },
            dismissButton = {
                TextButton(onClick = { denyNightly() }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    val scope = rememberCoroutineScope()
    val requestRuntimeConsent = rememberRuntimeUpdateConsent()
    var ytdlpAutoUpdate by remember {
        mutableStateOf(YT_DLP_AUTO_UPDATE.getBoolean() &&
            (!App.isFDroidBuild() || FDROID_RUNTIME_AUTO_UPDATE_CONSENT.getBoolean()))
    }
    var updateInterval by remember { mutableLongStateOf(YT_DLP_UPDATE_INTERVAL.getLong()) }

    SealDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        confirmButton = {
            ConfirmButton {
                scope.launch {
                    if (App.isFDroidBuild() && ytdlpAutoUpdate) {
                        if (!requestRuntimeConsent(true)) return@launch
                    }
                    if (App.isFDroidBuild())
                        FDROID_RUNTIME_AUTO_UPDATE_CONSENT.updateBoolean(ytdlpAutoUpdate)
                    YT_DLP_AUTO_UPDATE.updateBoolean(ytdlpAutoUpdate)
                    PreferenceUtil.saveYtdlpChannelSelection(selection)
                    YT_DLP_UPDATE_INTERVAL.updateLong(updateInterval)
                    onDismissRequest()
                }
            }
        },
        dismissButton = { DismissButton { onDismissRequest() } },
        title = { Text(text = stringResource(id = R.string.update)) },
        icon = { Icon(Icons.Outlined.SyncAlt, null) },
        text = {
            LazyColumn() {
                item {
                    Text(
                        stringResource(R.string.runtime_channel_pending_hint),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                item {
                    Text(
                        text = stringResource(id = R.string.update_channel),
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(top = 16.dp, bottom = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                item {
                    DialogSingleChoiceItem(
                        text = "yt-dlp",
                        selected = selection.channel == YT_DLP_STABLE,
                        label = "Stable",
                    ) {
                        selection = selection.copy(channel = YT_DLP_STABLE)
                    }
                }
                item {
                    DialogSingleChoiceItem(
                        text = "yt-dlp-nightly-builds",
                        selected = selection.channel == YT_DLP_NIGHTLY,
                        label = "Nightly",
                        labelContainerColor = MaterialTheme.colorScheme.tertiary,
                    ) {
                        if (selection.nightlyAllowed && !NightlyChannelPolicy.needsWarning(selection.dontWarnAgain)) {
                            selection = selection.copy(channel = YT_DLP_NIGHTLY)
                        } else {
                            dontWarnPending = false
                            showNightlyWarning = true
                        }
                    }
                }
                item {
                    Text(
                        text = stringResource(id = R.string.additional_settings),
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(top = 16.dp, bottom = 16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                item {
                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        OutlinedTextField(
                            value =
                                if (!ytdlpAutoUpdate) stringResource(id = R.string.disabled)
                                else getUpdateIntervalText(updateInterval),
                            onValueChange = {},
                            label = { Text(text = stringResource(id = R.string.auto_update)) },
                            readOnly = true,
                            modifier =
                                Modifier.fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                        )
                        ExposedDropdownMenu(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.disabled)) },
                                onClick = {
                                    ytdlpAutoUpdate = false
                                    expanded = false
                                },
                            )
                            for ((interval, stringId) in UpdateIntervalList) {
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = stringId)) },
                                    onClick = {
                                        ytdlpAutoUpdate = true
                                        updateInterval = interval
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

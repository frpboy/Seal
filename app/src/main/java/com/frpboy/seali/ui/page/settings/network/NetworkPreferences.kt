package com.frpboy.seali.ui.page.settings.network

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.OfflineBolt
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.SignalCellular4Bar
import androidx.compose.material.icons.outlined.SignalCellularConnectedNoInternet4Bar
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.frpboy.seali.R
import com.frpboy.seali.ui.common.booleanState
import com.frpboy.seali.ui.component.BackButton
import com.frpboy.seali.ui.component.PreferenceInfo
import com.frpboy.seali.ui.component.PreferenceItem
import com.frpboy.seali.ui.component.PreferenceSubtitle
import com.frpboy.seali.ui.component.PreferenceSwitch
import com.frpboy.seali.ui.component.PreferenceSwitchWithDivider
import com.frpboy.seali.util.ARIA2C
import com.frpboy.seali.util.CELLULAR_DOWNLOAD
import com.frpboy.seali.util.COOKIES
import com.frpboy.seali.util.CUSTOM_COMMAND
import com.frpboy.seali.util.FORCE_IPV4
import com.frpboy.seali.util.PROXY
import com.frpboy.seali.util.PreferenceUtil.getBoolean
import com.frpboy.seali.util.PreferenceUtil.updateBoolean
import com.frpboy.seali.util.PreferenceUtil.updateValue
import com.frpboy.seali.util.RATE_LIMIT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkPreferences(navigateToCookieProfilePage: () -> Unit = {}, onNavigateBack: () -> Unit) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )

    var showConcurrentDownloadDialog by remember { mutableStateOf(false) }
    var showRateLimitDialog by remember { mutableStateOf(false) }
    var showProxyDialog by remember { mutableStateOf(false) }
    var proxy by PROXY.booleanState
    var isCookiesEnabled by COOKIES.booleanState
    var forceIpv4 by FORCE_IPV4.booleanState

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(modifier = Modifier, text = stringResource(id = R.string.network)) },
                navigationIcon = { BackButton { onNavigateBack() } },
                scrollBehavior = scrollBehavior,
            )
        },
        content = {
            val savedCustomCommandEnabled by CUSTOM_COMMAND.booleanState
            val isCustomCommandEnabled = com.frpboy.seali.util.CandidateFeaturePolicy.customCommandsEnabled(savedCustomCommandEnabled)

            LazyColumn(contentPadding = it) {
                item {
                    PreferenceInfo(text = "Functional alpha: native downloads only. Aria2, custom commands, live streams and time-range downloads are unavailable. FFmpeg works on local media only.")
                }
                if (isCustomCommandEnabled)
                    item {
                        PreferenceInfo(
                            text = stringResource(id = R.string.custom_command_enabled_hint)
                        )
                    }
                item { PreferenceSubtitle(text = stringResource(R.string.general_settings)) }
                item {
                    var isRateLimitEnabled by remember { mutableStateOf(RATE_LIMIT.getBoolean()) }

                    PreferenceSwitchWithDivider(
                        title = stringResource(R.string.rate_limit),
                        description = stringResource(R.string.rate_limit_desc),
                        icon = Icons.Outlined.Speed,
                        enabled = !isCustomCommandEnabled,
                        isChecked = isRateLimitEnabled,
                        onChecked = {
                            isRateLimitEnabled = !isRateLimitEnabled
                            updateValue(RATE_LIMIT, isRateLimitEnabled)
                        },
                        onClick = { showRateLimitDialog = true },
                    )
                }
                item {
                    var isDownloadWithCellularEnabled by remember {
                        mutableStateOf(CELLULAR_DOWNLOAD.getBoolean())
                    }
                    PreferenceSwitch(
                        title = stringResource(R.string.download_with_cellular),
                        description = stringResource(R.string.download_with_cellular_desc),
                        icon =
                            if (isDownloadWithCellularEnabled) Icons.Outlined.SignalCellular4Bar
                            else Icons.Outlined.SignalCellularConnectedNoInternet4Bar,
                        isChecked = isDownloadWithCellularEnabled,
                        onClick = {
                            isDownloadWithCellularEnabled = !isDownloadWithCellularEnabled
                            updateValue(CELLULAR_DOWNLOAD, isDownloadWithCellularEnabled)
                        },
                    )
                }

                item { PreferenceSubtitle(text = stringResource(id = R.string.advanced_settings)) }

                item {
                    PreferenceSwitchWithDivider(
                        title = stringResource(id = R.string.proxy),
                        description = stringResource(id = R.string.proxy_desc),
                        icon = Icons.Outlined.VpnKey,
                        isChecked = proxy,
                        onChecked = {
                            proxy = !proxy
                            PROXY.updateBoolean(proxy)
                        },
                        onClick = { showProxyDialog = true },
                        enabled = !isCustomCommandEnabled,
                    )
                }
                item {
                    PreferenceItem(
                        title = stringResource(id = R.string.concurrent_download),
                        description = stringResource(R.string.concurrent_download_desc),
                        icon = Icons.Outlined.OfflineBolt,
                        enabled = !isCustomCommandEnabled,
                    ) {
                        showConcurrentDownloadDialog = true
                    }
                }
                item {
                    PreferenceSwitch(
                        title = stringResource(R.string.force_ipv4),
                        description = stringResource(id = R.string.force_ipv4_desc),
                        icon = Icons.Outlined.SettingsEthernet,
                        enabled = !isCustomCommandEnabled,
                        isChecked = forceIpv4,
                    ) {
                        forceIpv4 = !forceIpv4
                        FORCE_IPV4.updateBoolean(forceIpv4)
                    }
                }
                item {
                    PreferenceItem(
                        title = stringResource(R.string.cookies),
                        description = stringResource(R.string.cookies_desc),
                        icon = Icons.Outlined.Cookie,
                        onClick = { navigateToCookieProfilePage() },
                    )
                }
            }
        },
    )

    if (showConcurrentDownloadDialog) {
        ConcurrentDownloadDialog { showConcurrentDownloadDialog = false }
    }

    if (showRateLimitDialog) {
        RateLimitDialog { showRateLimitDialog = false }
    }
    if (showProxyDialog) {
        ProxyConfigurationDialog { showProxyDialog = false }
    }
}

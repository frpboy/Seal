package com.frpboy.seali.ui.page.settings.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.frpboy.seali.R
import com.frpboy.seali.ui.component.BackButton
import com.frpboy.seali.ui.component.PreferenceItem

private const val buyMeACoffeeUrl = "https://buymeacoffee.com/frpboy"
private const val patreonUrl = "https://www.patreon.com/c/FrpBoy"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorsPage(onNavigateBack: () -> Unit) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val uriHandler = LocalUriHandler.current
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(modifier = Modifier, text = stringResource(id = R.string.support_seali))
                },
                navigationIcon = { BackButton { onNavigateBack() } },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { values ->
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(values)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                    text = stringResource(id = R.string.support_seali_msg),
                    style = MaterialTheme.typography.bodyLarge,
                )
                PreferenceItem(
                    title = "Buy Me a Coffee",
                    description = "buymeacoffee.com/frpboy",
                    icon = Icons.Outlined.VolunteerActivism,
                ) {
                    uriHandler.openUri(buyMeACoffeeUrl)
                }
                PreferenceItem(
                    title = "Patreon",
                    description = "patreon.com/c/FrpBoy",
                    icon = Icons.Outlined.VolunteerActivism,
                ) {
                    uriHandler.openUri(patreonUrl)
                }
            }
        },
    )
}

@Composable
@Preview
fun SponsorPagePreview() {
    SponsorsPage {}
}

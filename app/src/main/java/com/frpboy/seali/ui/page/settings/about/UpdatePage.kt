package com.frpboy.seali.ui.page.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.frpboy.seali.App
import com.frpboy.seali.R
import com.frpboy.seali.ui.common.intState
import com.frpboy.seali.ui.component.BackButton
import com.frpboy.seali.ui.component.PreferenceInfo
import com.frpboy.seali.ui.component.PreferenceSingleChoiceItem
import com.frpboy.seali.ui.component.PreferenceSubtitle
import com.frpboy.seali.ui.component.PreferenceSwitchWithContainer
import com.frpboy.seali.ui.page.UpdateDialog
import com.frpboy.seali.util.AUTO_UPDATE
import com.frpboy.seali.util.PRE_RELEASE
import com.frpboy.seali.util.PreferenceUtil
import com.frpboy.seali.util.PreferenceUtil.updateBoolean
import com.frpboy.seali.util.PreferenceUtil.updateInt
import com.frpboy.seali.util.STABLE
import com.frpboy.seali.util.ToastUtil
import com.frpboy.seali.util.UPDATE_CHANNEL
import com.frpboy.seali.util.UpdateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatePage(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.auto_update)) },
                navigationIcon = { BackButton { onNavigateBack() } },
            )
        },
    ) { paddings ->
        PreferenceInfo(
            modifier = Modifier.padding(paddings),
            text = stringResource(R.string.app_updates_unavailable_candidate),
        )
    }
}

@Composable
fun ProgressIndicatorButton(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        modifier = modifier,
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        if (isLoading)
            Box(modifier = Modifier.size(18.dp)) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp).align(Alignment.Center),
                    strokeWidth = 3.dp,
                )
            }
        else Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text = text, modifier = Modifier.padding(start = 8.dp))
    }
}

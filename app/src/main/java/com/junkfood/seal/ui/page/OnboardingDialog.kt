package com.junkfood.seal.ui.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R

/** Orientation only: never requests a permission or changes download preferences. */
@Composable
fun OnboardingDialog(onComplete: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val titles = listOf(R.string.onboarding_start_title, R.string.onboarding_folder_title,
        R.string.onboarding_permission_title)
    val bodies = listOf(R.string.onboarding_start_body, R.string.onboarding_folder_body,
        R.string.onboarding_permission_body)
    val icons = listOf(Icons.Outlined.ContentPaste, Icons.Outlined.FolderOpen,
        Icons.Outlined.NotificationsNone)
    AlertDialog(
        onDismissRequest = onComplete,
        title = { Text(stringResource(R.string.onboarding_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(stringResource(R.string.onboarding_step, step + 1, titles.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                AnimatedContent(targetState = step,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                    label = "onboarding-step") { page ->
                    Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(icons[page], contentDescription = null, modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(titles[page]), style = MaterialTheme.typography.headlineSmall)
                        Text(stringResource(bodies[page]), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (step > 0) step-- else onComplete() }) {
                Text(stringResource(if (step > 0) R.string.onboarding_back else R.string.onboarding_skip))
            }
        },
        confirmButton = {
            Button(onClick = { if (step < titles.lastIndex) step++ else onComplete() }) {
                Text(stringResource(if (step == titles.lastIndex) R.string.onboarding_finish else R.string.onboarding_next))
            }
        },
    )
}

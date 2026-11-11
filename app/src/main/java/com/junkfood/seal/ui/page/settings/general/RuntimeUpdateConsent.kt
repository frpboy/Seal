package com.junkfood.seal.ui.page.settings.general

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.junkfood.seal.App
import com.junkfood.seal.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Ask before an executable download, without making baseline app use depend on consent. */
@Composable
fun rememberRuntimeUpdateConsent(): suspend (automatic: Boolean) -> Boolean {
    var pending by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    var automatic by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose { pending?.complete(false) }
    }
    if (pending != null) {
        fun finish(allowed: Boolean) { pending?.complete(allowed) }
        AlertDialog(
            onDismissRequest = { finish(false) },
            title = { Text(stringResource(R.string.runtime_update_consent_title)) },
            text = {
                Text(
                    stringResource(
                        if (automatic) R.string.runtime_update_consent_automatic
                        else R.string.runtime_update_consent_once
                    ),
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = { finish(true) }) {
                    Text(stringResource(
                        if (automatic) R.string.runtime_update_allow_automatic
                        else R.string.runtime_update_allow_once
                    ))
                }
            },
            dismissButton = {
                TextButton(onClick = { finish(false) }) {
                    Text(stringResource(R.string.runtime_update_decline))
                }
            },
        )
    }
    return { automaticRequest ->
        if (!App.isFDroidBuild()) true
        else withContext(Dispatchers.Main.immediate) {
            // A second request must not replace a prompt another caller is awaiting.
            if (pending != null) false
            else {
                val request = CompletableDeferred<Boolean>()
                automatic = automaticRequest
                pending = request
                try { request.await() }
                finally { if (pending === request) pending = null }
            }
        }
    }
}

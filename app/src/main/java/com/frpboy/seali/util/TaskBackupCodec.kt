package com.frpboy.seali.util

import com.frpboy.seali.download.Task
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Persisted polymorphic type IDs stay the stable legacy names (see the @SerialName
 * pins in Task.kt). Builds from the renamed-namespace lane before those pins emitted
 * the current-source FQNs instead; map those back to the stable persisted IDs so both
 * on-device forms restore. Not source namespaces or attribution. Read compatibility only.
 */
private val renamedTaskTypeIds = buildMap {
    val stable = "com.junkfood.seal.download.Task."
    val renamed = "com.frpboy.seali.download.Task."
    listOf(
        "TypeInfo.Playlist", "TypeInfo.CustomCommand", "TypeInfo.URL",
        "DownloadState.Idle", "DownloadState.FetchingInfo", "DownloadState.ReadyWithInfo",
        "DownloadState.Running", "DownloadState.Canceled", "DownloadState.Error", "DownloadState.Completed",
        "RestartableAction.FetchInfo", "RestartableAction.Download",
    ).forEach { suffix -> put(renamed + suffix, stable + suffix) }
}

internal fun normalizePersistedTaskTypeIds(element: JsonElement): JsonElement = when (element) {
    is JsonArray -> JsonArray(element.map(::normalizePersistedTaskTypeIds))
    is JsonObject -> JsonObject(element.mapValues { (key, value) ->
        if (key == "type" && value is JsonPrimitive && value.isString) {
            renamedTaskTypeIds[value.content]?.let(::JsonPrimitive) ?: value
        } else normalizePersistedTaskTypeIds(value)
    })
    else -> element
}

internal object TaskBackupCodec {
    private val json = Json { ignoreUnknownKeys = true; allowStructuredMapKeys = true }
    fun decode(raw: String): Map<Task, Task.State> =
        json.decodeFromJsonElement(normalizePersistedTaskTypeIds(json.parseToJsonElement(raw)))
    fun encode(value: Map<Task, Task.State>): String = json.encodeToString(value)
}

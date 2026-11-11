package com.frpboy.seali.util

import com.frpboy.seali.download.Task
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/** Old persisted polymorphic type IDs, not source namespaces or attribution. Read compatibility only. */
private val legacyTaskTypeIds = buildMap {
    val old = "com.junkfood.seal.download.Task."
    val current = "com.frpboy.seali.download.Task."
    listOf(
        "TypeInfo.Playlist", "TypeInfo.CustomCommand", "TypeInfo.URL",
        "DownloadState.Idle", "DownloadState.FetchingInfo", "DownloadState.ReadyWithInfo",
        "DownloadState.Running", "DownloadState.Canceled", "DownloadState.Error", "DownloadState.Completed",
        "RestartableAction.FetchInfo", "RestartableAction.Download",
    ).forEach { suffix -> put(old + suffix, current + suffix) }
}

internal fun migrateLegacyTaskTypeIds(element: JsonElement): JsonElement = when (element) {
    is JsonArray -> JsonArray(element.map(::migrateLegacyTaskTypeIds))
    is JsonObject -> JsonObject(element.mapValues { (key, value) ->
        if (key == "type" && value is JsonPrimitive && value.isString) {
            legacyTaskTypeIds[value.content]?.let(::JsonPrimitive) ?: value
        } else migrateLegacyTaskTypeIds(value)
    })
    else -> element
}

internal object TaskBackupCodec {
    private val json = Json { ignoreUnknownKeys = true; allowStructuredMapKeys = true }
    fun decode(raw: String): Map<Task, Task.State> =
        json.decodeFromJsonElement(migrateLegacyTaskTypeIds(json.parseToJsonElement(raw)))
    fun encode(value: Map<Task, Task.State>): String = json.encodeToString(value)
}

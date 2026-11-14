package com.frpboy.seali.util

import com.frpboy.seali.download.Task
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class TaskBackupMigrationTest {
    private fun fixture() = checkNotNull(javaClass.getResource("/migration/legacy-task-backup.json")).readText()
    private fun types(element: JsonElement): List<String> = when(element) {
        is JsonArray -> element.flatMap(::types)
        is JsonObject -> element.flatMap { (key,value) ->
            if (key == "type" && value is JsonPrimitive && value.isString) listOf(value.content) else types(value)
        }
        else -> emptyList()
    }
    @Test fun restoresRealPreRenameModelFixtureWithoutLosingAnyTask() {
        val result = TaskBackupCodec.decode(fixture())
        assertEquals(7,result.size)
        assertEquals((0..6).map { "legacy-fixture-$it" }.toSet(),result.keys.map { it.id }.toSet())
        assertTrue(result.values.any { it.downloadState is Task.DownloadState.Running })
        assertTrue(result.values.any { it.downloadState is Task.DownloadState.Error })
        assertTrue(result.values.any { it.downloadState is Task.DownloadState.Completed })
        assertTrue(result.keys.any { it.type is Task.TypeInfo.CustomCommand })
        assertTrue(result.keys.any { it.type is Task.TypeInfo.Playlist })
        result.keys.forEach { assertEquals("com.junkfood.seal.download.Task.DownloadState.Idle", it.preferences.newTitle) }
        val running=result.values.single { it.downloadState is Task.DownloadState.Running }.downloadState as Task.DownloadState.Running
        assertEquals(0.25f,running.progress)
        assertEquals("[download] 25% at 1MiB/s ETA 00:15",running.progressText)
    }
    @Test fun restoresBackupWrittenByRenamedNamespaceBuild() {
        val renamed=fixture().replace("\"type\":\"com.junkfood.seal.download.Task.","\"type\":\"com.frpboy.seali.download.Task.")
        val result=TaskBackupCodec.decode(renamed)
        // Whole-map equality is unusable here: each decode mints fresh referential
        // defaults that data-class equals still compares - the @Transient Throwable in
        // DownloadState.Error and the @Transient Job defaults in FetchingInfo/Running.
        // Persisted-form equality is the property that matters for backup fidelity.
        assertEquals(7,result.size)
        assertEquals(TaskBackupCodec.decode(fixture()).keys.map { it.id }.toSet(),result.keys.map { it.id }.toSet())
        assertEquals(TaskBackupCodec.encode(TaskBackupCodec.decode(fixture())),TaskBackupCodec.encode(result))
    }
    @Test fun roundTripUsesStableLegacyTypeIdsAndPreservesData() {
        val current=TaskBackupCodec.encode(TaskBackupCodec.decode(fixture()))
        assertTrue(types(Json.parseToJsonElement(current)).all { it.startsWith("com.junkfood.seal.download.Task.") })
        assertEquals(current,TaskBackupCodec.encode(TaskBackupCodec.decode(current)))
    }
    @Test fun mappingIsOnlyKnownDiscriminatorsNotArbitraryContentOrUnknownTypes() {
        val raw=Json.parseToJsonElement("""{"type":"com.frpboy.seali.download.Task.DownloadState.Idle","url":"com.frpboy.seali.download.Task.DownloadState.Idle","nested":{"type":"com.junkfood.seal.Unrelated"},"legacy":{"type":"com.junkfood.seal.download.Task.DownloadState.Idle"}}""")
        val migrated=normalizePersistedTaskTypeIds(raw) as JsonObject
        assertEquals("com.junkfood.seal.download.Task.DownloadState.Idle",(migrated["type"] as JsonPrimitive).content)
        assertEquals("com.frpboy.seali.download.Task.DownloadState.Idle",(migrated["url"] as JsonPrimitive).content)
        assertEquals((raw as JsonObject)["nested"],migrated["nested"])
        assertEquals((raw as JsonObject)["legacy"],migrated["legacy"])
    }
    @Test fun failedRestoreBlocksEmptySnapshotWriteAndKeepsOriginalBytes() {
        val original=fixture().replace("com.junkfood.seal.download.Task.DownloadState.Idle","com.junkfood.seal.download.Task.DownloadState.Unknown")
        var stored=original
        val guard=TaskBackupWriteGuard(TaskBackupCodec::decode,TaskBackupCodec::encode)
        assertTrue(guard.restore(stored).isFailure)
        assertTrue(guard.encodeForStorage(emptyMap()).onSuccess { stored=it }.isFailure)
        assertEquals(original,stored)
    }
    @Test fun validOrAbsentRestoreAllowsSubsequentSaveButNeverBeforeRead() {
        val guard=TaskBackupWriteGuard(TaskBackupCodec::decode,TaskBackupCodec::encode)
        assertTrue(guard.encodeForStorage(emptyMap()).isFailure)
        assertTrue(guard.restore(null).isSuccess)
        assertTrue(guard.encodeForStorage(emptyMap()).isSuccess)
        assertEquals(7,guard.restore(fixture()).getOrThrow()!!.size)
        assertTrue(guard.encodeForStorage(emptyMap()).isSuccess)
    }
    @Test fun malformedBackupRemainsProtectedAcrossRepeatedWrites() {
        val guard=TaskBackupWriteGuard(TaskBackupCodec::decode,TaskBackupCodec::encode)
        assertTrue(guard.restore("{broken").isFailure)
        repeat(3) { assertTrue(guard.encodeForStorage(emptyMap()).isFailure) }
        assertTrue(guard.restore(fixture()).isSuccess)
        assertTrue(guard.encodeForStorage(emptyMap()).isSuccess)
    }
}

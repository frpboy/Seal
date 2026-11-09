package com.frpboy.seali.util

/** A failed restore must never let a subsequent empty snapshot overwrite the unread original. */
internal class TaskBackupWriteGuard<T>(
    private val decode: (String) -> T,
    private val encode: (T) -> String,
) {
    @Volatile private var writesAllowed = false

    fun restore(raw: String?): Result<T?> = runCatching {
        raw?.let(decode)
    }.onSuccess { writesAllowed = true }.onFailure { writesAllowed = false }

    fun encodeForStorage(value: T): Result<String> = runCatching {
        check(writesAllowed) { "Saved queue backup is preserved until it can be restored" }
        encode(value)
    }
}

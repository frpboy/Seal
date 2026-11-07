package com.junkfood.seal.util

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import java.util.concurrent.ConcurrentHashMap

/** App-owned cancellation boundary. No reflection, shell commands or copied wrapper. */
object CancellableYoutubeDL {
    private class Execution(val thread: Thread) {
        @Volatile var canceled = false
    }
    private val running = ConcurrentHashMap<String, Execution>()

    fun execute(
        request: YoutubeDLRequest,
        processId: String? = null,
        callback: ((Float, Long, String) -> Unit)? = null,
    ): YoutubeDLResponse {
        if (processId == null) return YoutubeDL.getInstance().execute(request, null, callback)
        val execution = Execution(Thread.currentThread())
        check(running.putIfAbsent(processId, execution) == null) { "Process ID already exists" }
        try {
            // YoutubeDL.execute destroys its Process when its waiting thread is interrupted.
            val response = YoutubeDL.getInstance().execute(request, processId, callback)
            synchronized(execution) {
                if (execution.canceled) throw YoutubeDL.CanceledException()
                running.remove(processId, execution)
            }
            return response
        } catch (error: Exception) {
            if (execution.canceled) throw YoutubeDL.CanceledException()
            throw error
        } finally {
            synchronized(execution) {
                running.remove(processId, execution)
                // Do not leave this cancellation's interrupt flag on a shared worker.
                if (execution.canceled) Thread.interrupted()
            }
        }
    }

    fun cancel(id: String): Boolean {
        val execution = running[id] ?: return false
        synchronized(execution) {
            if (running[id] !== execution) return false
            execution.canceled = true
            execution.thread.interrupt()
            return true
        }
    }
}

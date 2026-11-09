package com.frpboy.seali.util

import java.util.Collections
import java.util.IdentityHashMap

/** A shareable summary, not a replacement for the original Throwable kept in state. */
object SafeErrorReport {
    fun describe(throwable: Throwable): String {
        val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        val lines = mutableListOf<String>()
        var current: Throwable? = throwable
        while (current != null && lines.size < 6 && seen.add(current)) {
            val type = current.javaClass.simpleName
                .takeIf { it.matches(Regex("[A-Za-z][A-Za-z0-9_$]{0,79}")) }
                ?: "Throwable"
            val detail = safeDetail(current.message)
            lines += (if (lines.isEmpty()) "Error: " else "Caused by: ") + "$type: $detail"
            current = current.cause
        }
        if (current != null) lines += "Additional causes omitted."
        lines += "Private messages, URLs, headers and stack traces are not included."
        return lines.joinToString("\n")
    }

    fun copyReport(versionReport: String, throwable: Throwable, url: String? = null): String =
        versionReport + "\nURL: " + SafeDiagnosticUrl.describe(url) + "\n" + describe(throwable)

    // Deliberately allowlist descriptions instead of trying to remove every possible secret
    // from arbitrary process output. An unrecognized message is never copied verbatim.
    private fun safeDetail(message: String?): String {
        if (message.isNullOrBlank()) return "No error message was provided."
        val text = message.lowercase()
        return when {
            "sign in to confirm" in text && "not a bot" in text ->
                "The server requested sign-in verification."
            "no supported javascript runtime" in text ->
                "No supported JavaScript runtime was found."
            "permission denied" in text -> "Permission was denied."
            "no space left on device" in text -> "The device reported insufficient storage."
            "timed out" in text || "timeout" in text -> "An operation timed out."
            "unable to resolve host" in text || "unknownhost" in text ->
                "A network host could not be resolved."
            "http error 403" in text -> "The server returned HTTP 403."
            "http error 416" in text -> "The server returned HTTP 416."
            else -> "Error details were withheld for privacy."
        }
    }
}

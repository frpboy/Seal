package com.frpboy.seali.util

import java.util.Collections
import java.util.IdentityHashMap

/** A shareable summary, not a replacement for the original Throwable kept in state. */
object SafeErrorReport {
    /** Local UI only: preserve useful process detail; never use this for sharing. */
    fun localDescription(throwable: Throwable): String =
        throwable.message?.takeIf { it.isNotBlank() } ?: throwable.javaClass.simpleName

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

    /** First recognized safe category in the existing cause chain; never raw process output. */
    fun reason(throwable: Throwable): String {
        val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        var current: Throwable? = throwable
        var count = 0
        while (current != null && count++ < 6 && seen.add(current)) {
            val detail = safeDetail(current.message)
            if (detail != "No error message was provided." && detail != "Error details were withheld for privacy.") return detail
            current = current.cause
        }
        return safeDetail(throwable.message)
    }

    fun copyReport(versionReport: String, throwable: Throwable, url: String? = null): String =
        versionReport + "\nURL: " + SafeDiagnosticUrl.describe(url) + "\n" + describe(throwable)

    fun copyLegacyReport(versionReport: String, localReport: String?, url: String?): String =
        versionReport + "\nURL: " + SafeDiagnosticUrl.describe(url) +
            "\nError: " + safeDetail(localReport) +
            "\nPrivate messages, URLs, headers and stack traces are not included."

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

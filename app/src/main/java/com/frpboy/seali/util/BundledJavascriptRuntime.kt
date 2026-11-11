package com.frpboy.seali.util

import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

/** Use only the packaged runtime and the solver already bound to the bundled engine. */
internal fun YoutubeDLRequest.enforceBundledJavascriptRuntime(
    nativeLibraryDir: String,
): YoutubeDLRequest {
    val runtime = File(nativeLibraryDir, "libqjs.so")
    check(runtime.isFile && runtime.canExecute()) { "Bundled JavaScript runtime unavailable" }
    return addCommands(
        listOf(
            "--no-js-runtimes",
            "--js-runtimes", "quickjs:${runtime.absolutePath}",
            "--no-remote-components",
            "--extractor-args", "youtube-ejs:dev=false",
        )
    )
}

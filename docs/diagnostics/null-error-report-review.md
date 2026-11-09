# Nullable error report preparation

Base: 0ebafd82e274753180fecbe73a0d3e4c2fd83b06 (post PR42).

The V2 configuration error view and copy action previously interpolated only nullable Throwable.message. This loses exception identity and causes, and emits literal null for absent messages.

Queue ActionSheet and ClassicHomePage both call TextUtil.getErrorReport(th, task.url), which had the same nullable-message defect. The shared formatter is now routed to the safe helper as well, preserving its signature while ignoring the URL. This covers the actual queue Copy report shown in the user screenshot, as well as the configuration error dialog.

This bounded patch changes presentation and copying only. It keeps the exact original Throwable and cause graph in state. A pure helper emits bounded exception class and cause class entries, with fixed allowlisted generic message categories. Arbitrary stderr, URLs, request headers, cookies, filenames, paths and stack traces are never copied verbatim. The URL is deliberately removed from the copied report. Unknown messages are explicitly withheld, not guessed. The original Retry callback is unchanged. Downloader, persistence, cancellation, notifications, dependencies and release workflow are unchanged.

Tradeoff: unrecognized message details are withheld, so future diagnosis may need a separately approved, private, scoped diagnostic capture. This is safer than a blacklist that can leak secrets in process output. Exception class and known safe category are still available, including nested causes. Output is English like the existing Retry text; localized diagnostics are follow-up work, not claimed here.

Five real Kotlin/JUnit tests passed: null fallback, nested cause and original identity retention, arbitrary secrets/path/URL exclusion, cyclic and deep graph termination, and allowlisted categories. Compiler was Kotlin2.2.20 from existing cache, JVM with JUnit4.13.2. This is not an Android UI compilation or visual verification. No extractor request, login, cookie access, dependency change, APK, upload or runtime test occurred.

Independent review required before push. Actual Android generic debug compile and actual error-dialog pixels remain open. Do not mark this merged, installed, runtime-qualified, or a fix for download failure, JavaScript support, or automatic Canceled. No new host or emulator experiment is authorized by this preparation.

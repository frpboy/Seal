# Nullable local errors and separate shareable reports

Composed against current main fc9b52bcff4ce310ddafe38de246c35e25b067d8, based on PR43 c8bf3a52 with a reviewed-purpose refinement prepared November11.

Local Downloader error state and configuration error view preserve useful original nonblank Throwable.message; absent/blank messages fall back to exception type instead of literal null. The exact original Throwable and causes remain in state. Retry, stored task URL and existing user-triggered Copy link are unchanged.

Copy report uses a separate bounded privacy formatter: exception/cause types and fixed known categories, no arbitrary stderr/header/cookie/path/stack text. Shareable URL report retains origin only and warns that even an origin may be sensitive. All media IDs, playlist capabilities, paths, query and fragment are withheld, including YouTube IDs. An identifier is not assumed public merely because it matches a public website format. Nothing is sent automatically. This is not a universal privacy cleanup of all existing diagnostic surfaces.

Legacy download-screen Copy report now sanitizes its stored raw local report separately, preserving that screen's local display while withholding media IDs and private text from the clipboard.

Scope:10files, no dependency/native/workflow/signing/package/version/storage/schema changes. Pure helper tests and source checks are supplemental; actual composed Android compile and unit results must be recorded separately. No runtime/error-dialog screenshot or download fix is claimed. This is not the full alpha7 namespace/native merge.

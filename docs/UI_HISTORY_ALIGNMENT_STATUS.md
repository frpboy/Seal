# History list source candidate

Child of reviewed action/input ed310272. History thumbnails use a consistent 96dp-wide16:9 crop; existing shared MediaImage audio-square behavior in FormatItem is intentionally unchanged. The row reserves a separate action-button column rather than overlaying text. Its existing content description is no longer erased by clearAndSetSemantics. Existing click/multiselect/delete/search behavior is untouched.

Available file summary adds only a safe literal filename extension from the existing stored local path. It is not a probed container. Unavailable files still say Unavailable; no path is exposed. YouTube casing is presentation-only for the exact extractor name, not stored/filter identity. DownloadedVideoInfo has no duration field. Duration and real portrait metadata remain open rather than fabricated; adding persistent duration would require its own schema, data and runtime review.

Four helper JVM tests and source boundary suites pass. Exact Android/Compose and actual pixels, compact widths, 200%fontscale, RTL, selection mode, menu alignment/actions, offline thumbnails and real stored histories remain unverified. No merge/release claim.

# Compact queue failures source candidate

Parent: namespace e50f580261f501380799dec6ea87ecef3eb48c70, not a change to draft49.

- Phone compact list default only when no persisted new layout preference exists; existing rememberSaveable state is retained. Older code never persisted this layout to disk; no old disk value to migrate. Explicit future toggles persist to queue_grid_view_v1, without changing Classic or saved folders.
- Existing Canceled predicate stays intact, displayed honestly as Failed / Canceled with actual per-filter counts.
- Errors without thumbnail metadata use a text-only compact card in both layouts. Safe known reason category only, actual title or domain-only URL fallback, metadata-unavailable when neither exists. Retry, Copy report and existing local actions remain available. No new thumbnail network request, raw error/log text, or automatic disclosure. Metadata-resolved cards retain existing rendering.
- Quick settings header distinguished from unchanged Settings destination.

Evidence: six pure production-helper JVM tests and all existing/new source boundary suites. No exact candidate Android/Compose compile or pixels checked yet. Not ready to merge. Device pixels at compact widths, fontscale/accessibility, both layout states/error/resume/report/actions, actual theme, state restoration/persistent preference and offline behavior remain open. Inherited exact namespace upgrade/schema/alias/provider/PendingIntent/backup/native/consent gates are unchanged. Additional expanded UI roadmap remains active, not complete.

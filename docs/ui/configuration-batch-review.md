# Configuration visibility and fetching preparation

Base151171f3, isolated from BatchA/diagnostics43/icons45/consent41. Source preparation, not compile/runtime/visual qualification.

Common subtitle/thumbnail chips are now visible directly in single/multiple-URL configuration instead of behind collapsed Additional settings. Existing cookies chip is retained only when profiles exist, with its existing explicit dialog; no cookie content is read or exposed by this delta. The single sheet passes its actual current selected type to the existing chips, not stale config.downloadType. Command mode omits irrelevant common settings. Existing preference-toggle persistence and callback are unchanged. MultiURL chips now receive selectedType rather than hardcoded Audio.

Custom adds an honest summary of actual current subtitle/thumbnail preferences only when enabled. It does not invent previously selected H.264/resolution/language: format-specific selections live in FormatPage's per-media state and are not saved by Config. A full previous-format history/selection summary remains open and requires a real state design; do not claim it done. Custom allows up to four lines when showing settings, rather than the inherited two-line clamp; large font-scale wrapping remains a pixel/iteration gate, not silently qualified.

Fetching keeps an indeterminate progress signal, adds current owned chevron mark, existing fetching label and truthful explanation. Dismissing requests cancellation through existing HideSheet->cancel logic, not a guarantee the native process has stopped. No fake progress percentage or wait estimate. No new native execution/network or lifecycle action.

Independent source review, actual Android compile and actual pixels/font scaling/landscape/Command/single+multiURL/cookieprofile/no-profile states required before merge. No emulator/host experiment or dependency change. Existing multiURL onDownload(initialDownloadType) source behavior is not modified under this presentation patch; it requires separate behavioral review before claiming selection-flow correctness.

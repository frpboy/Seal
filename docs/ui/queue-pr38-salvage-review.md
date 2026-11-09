# Preserve useful queue38 changes on the coherent A+D base

Live ref fetched November9,19:26:35IST: PR38 now d81362532cd178998c439b27871b4cc40d8252bc/tree dd7b9cd6440cfd7e77e935ba933e0f945ee8ccf1, child of earlier98606f56. The intervening three files are verification workflow/docs/source-boundary changes only, no app source change. This patch does not merge38 or import its workflow changes.

Parent17dca86f (Aae61152f+D17dca86f). Preserve exact predicate matches(state) extraction and all four actual QueueFilterTest tests from38. predict delegates to matches; truth table unchanged. Finished empty state now says No finished downloads in this session and directs to Downloads for saved history. Uses exact two strings from38, not its superseded layout/toggle strings. No session entries are read as complete saved history. No new navigation callback or automatic action.

Three source/provenance boundary tests pass, D4+A4 still pass. Kotlin QueueFilterTest is preserved exactly but not claimed executed locally in this delta; actual Android CI must run it. All current pixels/fontscale/filter/session-history/Classic/behavior/runtime gates remain. Proposed one normal fast-forward update to existing DRAFT46 after independent exact combined review. No merge or closure of38 yet, and no public artifact/release/settings change.

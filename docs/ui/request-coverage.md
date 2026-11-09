# Seali UI polish request coverage

Original owner request: November9,2026,19:14:46 IST, ends "Add this all". Base151171f35a97ea561ce8a723f744fa236de78853. This is an implementation tracker, not visual qualification or a release statement.

## Batch A, local preparation

- Empty All queue uses current owned chevron vector, meaningful add-link button and browser-sharing hint. The action opens the existing URL-entry flow, not a fake direct clipboard paste or automatic download. Exact Paste shortcut is not yet implemented.
- Empty filtered queues have short distinct active, canceled/failed, completed messages rather than repeating the large generic illustration. The canceled filter currently includes errors, so wording does not falsely say only canceled.
- Empty content sits below the existing header/filter area, scrolls at larger fonts and removes the fixed40%-height illustration layout. The chevron illustration hides in short available height. Actual font-scale/landscape pixels still required.
- Drawer replaces blank header space with current chevron+app name, separates the three primary destinations from settings/support, and renames drawer Sponsor to Support & Donate. Quick settings remain unchanged.
- Version footer is a TextButton opening About after closing drawer, retains app/version, removes internal route debug text.

## Remaining requested work, not dropped

- Config: selected format/subtitle/thumbnail summary, common always-visible relevant toggles, consistent chevrons in other sheets, fetching skeleton/subtle pulse. Need actual preference/selection-state binding, not invented summaries or reset choices.
- Interface: clearer Classic/v2 explanation or previews, discoverability, modern default clarity, confirmed transition. Existing v2 default and active-download switch guard/confirmation already exist. Do not migrate an established Classic selection or replace current guards.
- Appearance: one or two named curated accents alongside existing dynamic/custom hue/monochrome controls, stronger but subtle brand loading/About/progress treatment. Existing high-contrast dark mode already uses pure-black surfaces; preserve it, do not create redundant toggle.
- Queue: active filter+count in persistent existing header (already outside scrolling grid), prominent actual progress/speed/remaining time, safe swipe retry/open-folder/confirmed cancellation. Existing overflow is a Not implemented placeholder, separate follow-up. No automatic cancellation from a gesture.
- Accessibility: all affected states at normal/large fonts, touch targets, narrow/wide/landscape, empty/filtered/populated/running states, Classic/v2, light/dark/high-contrast. No accessibility completion claim from source.
- FAB: intentional empty/first-launch labeling still pending; Batch A adds a text action in the empty state while preserving existing FAB.

## Gates and boundaries

No download engine/state/classification/default preference/dependency/workflow change in Batch A. Separate diagnostics43, notification icons45 and F-Droid consent41 remain draft and held for their own qualification. Current actual source runtime route is not admitted; no host/emulator experiment was repeated. User device gallery depicts older installed alpha6, not these new UI pixels. Independent review, actual Android compile, actual Compose pixels including font scaling and behavior verification are required before merge or visual completion. No new release or store submission is implied.

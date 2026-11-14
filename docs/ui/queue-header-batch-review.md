# Persistent filter count, labeled empty FAB and view target

Parent source-cleared/compiled BatchAae61152f. Source only, not pixels/runtime ready.

Existing header+filter area already lives outside LazyVerticalGrid. Moved count/view subheader there instead of adding a second sticky implementation. Count now displays actual filteredMap.size for the active filter, including tasks without fetched format metadata. Previously the grid subheader counted only inferred video/audio tasks, disappeared on scroll/empty filters, and mislabeled or omitted unknown tasks. Singular/plural counts supported. No changes to filter prediction or task classification. Always shows zero truthfully for empty filter.

Compact empty queue now labels the existing FAB using the existing download text, matching expanded layout. Condition is total task map empty, not an empty filtered slice. Existing empty Add link action remains. No clipboard/background download/first-launch storage changes.

List/cards toggle now has a48dp target and an accessible text describing next view. Removed adjacent overflow button whose only action was the literal Not implemented yet toast, not a real command. No working action removed. Card/list preference semantics unchanged.

Source boundaries4PASS; originalA4PASS. Actual narrow/largefont/landscape/headerheight/empty/running/populated/list/grid pixels and toggling/filter count behavior remain required. Progress/speed/ETA and safe swipe cancel/retry/open-folder are separate remaining request work, not claimed by this batch. Later exact composition review required with other branches; no push/merge/release yet.

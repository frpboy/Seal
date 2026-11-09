# Preset resolution field source candidate

Local child of reviewed support842ee3d8, itself on draft50 exact876. The resolution selector already used a Material3 ExposedDropdownMenuBox. Its custom SealTextField rendered a filled/underlined TextField with outlined colors. It now uses actual Material3 OutlinedTextField plus a visible resolution label, consistent with the existing format selector. No new dependency/API, option values, onSelect callback, draft-save/cancel or persistent setting behavior changes.

58source assertions across16suites pass. Exact Android/Compose compile, dropdown interaction, long resolution labels, save/cancel, compact/RTL/fontscale and actual pixels remain open. Other preset/audio relevance and interface-mode organization work remains active. No new remote branch, workflow, runtime or merge action.

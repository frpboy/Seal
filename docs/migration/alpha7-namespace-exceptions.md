# Seali namespace compatibility and source dependencies

All first-party application implementation and generated namespace use com.frpboy.seali.

Intentional exceptions retained:
- Old com.junkfood.seal MainActivity/QuickDownloadActivity aliases and non-exported receiver bridge accept historical PendingIntent/component names without moving user data or enabling a new external receiver.
- Legacy notification extra prefix and exactly enumerated old task backup type IDs are read-only compatibility inputs. New stored task classes are com.frpboy.seali.
- Original historical Room schema JSON bytes remain unchanged at the new class path. Database filename, MMKV keys and provider authority remain stable.
- About/Credits, licenses, notices, upstream repository links and provenance identify Seal/JunkFood02 truthfully. They are not current developer branding.
- Source-built local FrpBoy common/library/ffmpeg dependency modules preserve original com.yausername Java API/resource namespaces and preferences. This is third-party compatibility, not unmigrated first-party app code. No upstream compiled classes.jar or JNI is carried in those modules.
- The separately reviewed native Python residual remains inherited/pruned; source-built wrapper modules do not prove full native closure source ownership. Aria2 has no consumed dependency or dummy module.

Theme.Seal internal styles are renamed Theme.Seali together with manifest references. FRPBOY developer identity and Seali repository branding coexist with required upstream credit.

Actual package/signature/install-over/runtime checks remain acceptance gates, not implied by this source ledger.

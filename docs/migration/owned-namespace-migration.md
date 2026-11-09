# Owned namespace migration and explicit upgrade bridges

Base: independently reviewed semantic composition3d3b50fadd7ebf285310a25f3b35f49ef6540740/treebc6d73070d224613cc580b69d9b64b4b124ae2de. This is a separate source delta, not an installed update or release qualification. New19:31 format/swipe/log/clipboard/history/adaptive/back proposals are not implemented here.

## Owned source and build identity

- Owned app packages/imports/path tree move from com.junkfood.seal to com.frpboy.seali across main/unit/instrumentation source sets. Color resource namespace becomes com.frpboy.seali.color; inherited com.kyant.monet and io.material code and authorship are not falsely renamed.
- Gradle applicationId stays com.frpboy.seali, .debug/.preview suffixes stay, version/code/signing/SDK/flavor/dependency controls unchanged. FileProvider stays ${applicationId}.provider. Database filename app_database, version5, MMKV initialization/keys and notification channel/group IDs stay.
- Historical Room1..5 exports move under com.frpboy.seali.database.AppDatabase with byte-identical schema JSON and identity hashes. schema-custody.json pins each original blob, not reconstructed schemas. No destructive fallback or table/data migration added.
- Instrumentation workflow selectors and source-boundary scripts follow the new package/path. Workflow triggers, runners, permissions, signing guards, release/upload/history gates are unchanged. This delta does not run them.
- IDE source zoom paths follow new code. Inherited selected device/emulator snapshots are cleared, not replaced with a fabricated developer-machine path/device. Run configurations and selection modes stay.

## Queue backup compatibility and failure protection

Actual pre-move Task sealed serializers used fully qualified type IDs. Twelve concrete TypeInfo/DownloadState/RestartableAction IDs were also found in the existing public alpha5 APK DEX. Blind renaming would make old queue JSON undecodable.

TaskBackupCodec maps ONLY known old string values at JSON type discriminator keys to the new IDs before normal serializer decode. Other keys, arbitrary user text/URLs and unknown types are not rewritten. New serialization uses current package IDs. The old Task list slot is not rewritten as a migration side effect; normal successful snapshot saves use the existing slot.

TaskBackupWriteGuard blocks saves until first successful restore or verified absence. A malformed/unknown/failed restore keeps writes blocked, keeps original raw bytes untouched and shows an explicit message that new queue changes are not persisted until restore succeeds. It does not silently overwrite unread old data with an empty map. The existing queue UI may be empty after an error, but the failure is visible, the old backup is preserved, and saved Room history is untouched. Full persistent recovery/export UX is not added or claimed. Known successfully decoded task states continue through the existing restart-as-canceled policy, unchanged.

Six actual Kotlin/JUnit codec/guard tests passed, including the generated pre-rename7task fixture, all old task kinds/states/actions, current roundtrip, untouched arbitrary text, unknown/malformed protection, no save before read and successful recovery unblocking saves. See fixture-provenance.json for portable-model extraction limits. This is not an Android MMKV or real installed-upgrade run.

## Android component and old notification contract

- New App/activities/service/receiver resolve under the new source namespace. Existing FileProvider authority and installed IDs do not move.
- Android supports activity-alias, so old explicit MainActivity and QuickDownloadActivity component names alias the new implementations. They have no intent filters, so they do not add duplicate launcher/share targets; exported behavior matches earlier exported activities.
- Android does not have receiver-alias. A real minimal old-FQCN NotificationActionReceiver subclass remains, delegates entirely to the new implementation, and is explicitly non-exported like the current receiver. There is no second download engine or duplicated cancellation logic.
- New notification extras use the new prefix; the implementation reads old extra-key prefixes for old PendingIntents. Existing old component/key strings remain as read/resolve compatibility, not branding. New notifications use the current implementation/keys.
- The old service has no external or PendingIntent service entry in the inspected source. No old service/Application class is kept. Old foreground service processes do not survive app replacement; actual package-replacement behavior remains a required test, not a source guarantee.

Prepared instrumentation checks cover PackageManager resolution/export policy, old aliases targeting new classes and provider/application identity. A separate Room test creates the exact old exported v5 schema/identity and a fixture row, then opens it with the renamed Room class and verifies the row. These tests are not executed yet. Manifest declarations alone do not qualify PendingIntent delivery, lifecycle, foreground-service restart, file grants, pinned shortcuts or an installed update.

## Static published identity evidence

Existing public release: https://github.com/frpboy/Seali/releases/tag/v2.0.0-alpha.5

Inspected existing asset: https://github.com/frpboy/Seali/releases/download/v2.0.0-alpha.5/Seali-main-arm64-v8a.apk

SHA256530882d0c8fad458870a91b59a62004ab46757a66837b43efb11bfa0ab741989. Static manifest reports packagecom.frpboy.seali, versionCode200000152, versionName2.0.0-alpha.5, providercom.frpboy.seali.provider and earlier old-FQCN components. This is evidence for that downloaded public APK only. It is not a signature audit, a check of the user's current installed alpha6, or proof a new signed upgrade succeeds. No APK was executed, installed, changed or newly distributed for this inspection.

## Intentional legacy/external references

One real receiver bridge, old Android alias component names, old notification input prefix and twelve old task type input IDs remain for upgrade compatibility. The legacy test fixture and compatibility tests necessarily name these old values. Historical audit/provenance documents retain their source references. Required upstream copyright/credits/changelog/references and actual io.github.junkfood02.youtubedl-android coordinates remain; replacing them would misstate authorship or dependency identity. This is not a zero-junkfood-bytes promise.

Existing About report/support URL cleanup was discovered during the audit and is tracked separately, not mixed into namespace changes or confused with required upstream credit.

## Still required

Independent exact migration review; actual Android compile/unit tests for the exact candidate and each relevant flavor; compile prepared instrumentation tests; actual updated manifest/DEX/schema/export inspection; installed-identity/signing continuity and non-destructive old->new upgrade checks with preferences/saved history/queue restoration; old/new PendingIntent delivery and explicit activity aliases; file grants, service behavior and all existing lane-specific native/privacy/network/UI/fontscale gates. No merge, publication, native/security closure, store submission, screenshots or current-device fix is implied.

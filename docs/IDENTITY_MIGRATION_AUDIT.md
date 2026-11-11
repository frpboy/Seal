# Seali identity migration audit

## Scope

This branch moves project-owned Kotlin packages from `com.junkfood.seal` to
`com.frpboy.seali` and moves the Android and color Gradle namespaces to the
same Seali-owned identity. It does not change the installed application ID
(`com.frpboy.seali`), signing configuration, Room database filename
(`app_database`), or upstream dependency coordinates.

## Compatibility decisions

- `applicationId` remains `com.frpboy.seali`; users retain the installed app
  identity and update path.
- Room keeps database version 5 and the `app_database` filename; no schema or
  destructive data migration is introduced.
- Serialized polymorphic task variants keep their legacy serial names so data
  written before the Kotlin package move continues to decode.
- A deprecated legacy notification receiver remains declared to accept actions
  from notifications posted before the namespace change. New notifications use
  the Seali-owned action keys.
- GPL notices, upstream attribution, and `io.github.junkfood02` dependency
  coordinates are preserved until independently built replacements exist.

## Verification required

- Generic debug unit tests and buildSrc unit tests.
- Android instrumentation and device upgrade testing from the installed
  alpha.6 baseline: database/history, backups, notification actions, downloads,
  and cancellation.
- Five signed generic APKs, signature checks, and device qualification before
  any release publication.

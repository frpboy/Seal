# Seali — Upstream v2 and Repository Audit

**Audit date:** 2026-11-04  
**Audited repository:** `frpboy/Seal`  
**Working branch:** `project/seali-rebrand`  
**Upstream:** `JunkFood02/Seal`  
**Scope:** upstream activity, v2 implementation status, architecture, test/CI readiness, Android identity, and a safe continuation plan.

## Executive conclusion

Seali should **not** be implemented as a fresh rewrite and should **not** blindly revert to the old v1 implementation.

The fork's `main` is at the same upstream baseline commit inspected for this audit (`7677f61a20fda4210e84215fef9e9b51d25daa47`, 2026-09-25). The Seali branch adds the Seali README and `UPSTREAM.md`; the application source remains the upstream code. The fork already contains the v2 download architecture and v2 Compose UI. There is no large v2 code import to perform at this point.

The real task is to turn the existing v2 preview code into a tested, independently branded, maintainable release. The highest-risk areas are download queue correctness, custom-command completion, yt-dlp JavaScript-runtime compatibility, and the near-absence of meaningful automated tests.

## 1. What upstream activity actually tells us

The public repository is not archived. However, its application development cadence has been very low compared with its earlier history:

| Evidence | Date | Meaning |
|---|---:|---|
| Stable `v1.13.1` | 2024-10-16 | Latest stable release visible in Releases |
| `v2.0.0-alpha.5` | 2024-11-29 | Latest v2 alpha release visible in Releases |
| Last functional source commits in the inspected recent history | 2025-04-06 | UI tweak, hard-coded directory fix, sponsor page fix, dependency update |
| Later commits through 2026-09-25 | Monthly | Commit messages are sponsor-info README updates, not app implementation changes |

The alpha.5 release notes explicitly say the new custom-command implementation is still under development and broken. The v2 changelog still marks v2.0.0 as unreleased. This is strong evidence of a long-running unfinished v2 effort, but it does **not** prove the maintainer's personal reason or prove that development will never resume.

References:
- Upstream releases: https://github.com/JunkFood02/Seal/releases
- v2 alpha.5: https://github.com/JunkFood02/Seal/releases/tag/v2.0.0-alpha.5
- v2 changelog: https://github.com/JunkFood02/Seal/blob/main/CHANGELOG.md
- upstream commit history: https://github.com/JunkFood02/Seal/commits/main/

## 2. Existing v2 implementation in this fork

The current source already contains:

- Kotlin + Jetpack Compose, single-activity navigation.
- A v2 home/download page under `ui/page/downloadv2`.
- A task-based download model: `Task`, `TaskFactory`, `DownloaderV2`.
- A queue with a hard-coded maximum concurrency of three.
- States for idle, fetching information, ready, running, canceled, error, and completed.
- Backup/restore of unfinished tasks through MMKV-backed preferences.
- Room for download history, command templates, cookie profiles, and shortcuts.
- youtubedl-android, FFmpeg, and aria2 integration.
- A saved-links preference key and a troubleshooting page.

This is a useful base. It should be preserved and hardened rather than replaced.

## 3. Confirmed gaps and risks

### P1 — Automated tests do not protect the v2 downloader

The local unit-test file contains only a trivial arithmetic test and a small delimiter helper test. The instrumented test checks the package name against `com.junkfood.Seal`, while the Gradle application ID is `com.junkfood.seal`. Package identifiers are case-sensitive, so that assertion is inconsistent with the configured application ID.

The current Android CI workflow builds `buildGenericRelease`; it does not explicitly run the unit-test and instrumentation-test suites.

**Action:** correct the assertion and introduce focused tests for task identity, queue ordering, state transitions, cancellation/restart, backup restore, and duplicate URL handling. Add `testGenericDebugUnitTest` (or the appropriate variant task) to CI. Instrumentation tests should run on a configured emulator job where feasible.

### P1 — The v2 custom-command path is not release-ready

The v2 alpha.5 release notes explicitly state that the new custom-command implementation is still broken. The downloader also retains a TODO list mentioning custom commands and ViewModel state integration.

**Action:** treat custom commands as a feature requiring a complete workflow test, not as a feature that is complete merely because the old template editor and task model exist. Verify template selection, argument composition, output directory, progress, cancellation, logs, restart behavior, and error reporting end to end.

### P1 — YouTube JavaScript-runtime compatibility needs a deliberate solution

An upstream issue opened on 2026-09-17 documents YouTube extraction failures caused by missing JavaScript-runtime support in the Android app. The issue proposes bundling Deno and reports an ARM64 test, but also identifies a substantial APK-size impact (about 88 MiB) and ABI/licensing/update questions.

**Action:** do not copy a large native binary into the app without a reproducible build and distribution review. First create a small compatibility spike: verify the current youtubedl-android runtime discovery behavior, supported Android ABIs, binary provenance and license, APK/AAB size, update strategy, and real YouTube extraction tests. Consider a runtime-provider abstraction so a future runtime change does not leak through the UI layer.

Reference: https://github.com/JunkFood02/Seal/issues/2640

### P2 — Queue persistence and identity need explicit invariants

The task model creates IDs from URL, task type, and a preferences hash. The queue uses tasks as map keys. This may intentionally suppress duplicate tasks, but the behavior should be explicit and tested. The creation timestamp is initialized outside the serialized primary-constructor properties, so restored tasks may not retain their original queue ordering.

**Action:** define whether duplicate URLs are rejected, merged, or queued separately. Persist an explicit enqueue sequence/time in the serialized task record. Add restore tests for queue order, in-progress tasks, canceled tasks, and tasks whose source URL or preferences changed.

### P2 — Downloader lifecycle and concurrency need stress tests

The v2 downloader uses a long-lived coroutine scope, snapshot-driven scheduling, and a concurrency limit. This is a reasonable first implementation, but the critical behavior is not covered by tests in the repository.

**Action:** test that no more than the configured number of tasks run, that cancellation releases a slot, that failed tasks do not block the queue, that process death restores a safe restartable state, and that simultaneous state changes cannot start the same task twice. Keep scheduler policy separate from Compose UI state.

### P2 — Android identity and inherited branding are still upstream

The Android application ID and namespace remain `com.junkfood.seal`; the manifest references Seal themes and upstream resources. The original launcher artwork and Seal labels are also still present.

**Action:** complete the Seali identity migration only after the baseline build and queue tests are stable. Give Seali a distinct application ID before distributing it as a separate app. Do not change the original package namespace everywhere in one blind replacement; separate the public application ID from internal Kotlin package migration. Verify Room data, MMKV preferences, backup/restore, FileProvider authorities, deep links, notifications, and upgrade behavior.

### P2 — CI/release workflow needs hardening

The release workflow uses a mutable third-party action reference ending in `@nightly` for APK signing. That is not a good foundation for a release pipeline.

**Action:** pin third-party actions to reviewed commit SHAs, keep signing material out of the repository, separate unsigned PR builds from signed release builds, and produce checksums plus reproducible source/version metadata. Do not publish a Seali binary until the new application ID and signing identity are finalized.

## 4. Recommended Seali roadmap

### Phase 0 — Establish a trustworthy baseline
- Keep the current upstream source baseline and preserve Git history.
- Record upstream commit IDs for every sync.
- Fix the incorrect instrumented package assertion.
- Make CI run unit tests and build all intended APK variants.
- Capture a clean baseline build and dependency report.

### Phase 1 — Make the v2 queue reliable
- Add unit tests for task IDs and state transitions.
- Persist queue sequence and stable task identity.
- Test restore after simulated process death.
- Test cancel, resume, retry, removal, and concurrency limits.
- Add structured error categories and actionable user-facing messages.

### Phase 2 — Finish incomplete v2 workflows
- Complete custom commands end to end.
- Verify saved links, playlists, format selection, and large-screen layouts.
- Ensure notifications and foreground-service behavior match current Android requirements.
- Test import/export compatibility and database migrations.

### Phase 3 — Fix extraction compatibility
- Build a Deno/runtime proof of concept.
- Verify ABI coverage, package size, license, security updates, and yt-dlp discovery.
- Add regression fixtures for representative YouTube and non-YouTube extractors.
- Keep site-specific failures distinguishable from app-level failures.

### Phase 4 — Complete Seali rebrand
- Distinct application ID and app label.
- Original Seali icon, adaptive icon, splash screen, theme names, and screenshots.
- Remove upstream app-name references from user-facing UI while retaining attribution in About/Credits and repository documentation.
- Update deep links, FileProvider, update metadata, release workflows, and privacy disclosures as applicable.

### Phase 5 — Release engineering
- Build signed APKs for supported ABIs.
- Verify install, update, backup/restore, and uninstall behavior on real devices.
- Publish source corresponding to each GPL-3.0 binary.
- Maintain a changelog that separates upstream-derived work from Seali changes.

## 5. Immediate implementation order

The first code changes should be deliberately small and testable:

1. Fix the incorrect package assertion in the instrumented test.
2. Add task-model and queue-state unit tests.
3. Add explicit queue-order persistence and restore tests if the tests confirm the ordering issue.
4. Build and test the v2 custom-command workflow.
5. Run the Deno compatibility spike separately; do not bundle the binary yet.
6. Only then begin the app identity migration.

## 6. What has and has not been changed

This audit is documentation only. No downloader behavior, package ID, application namespace, signing configuration, app icon, or release workflow has been changed by this audit. No claim is made that an Android build or device test has passed.

The GPL-3.0 license and original copyright notices must remain intact. Seali must clearly identify its upstream origin and must not imply endorsement by JunkFood02.

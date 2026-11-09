# Seali maintenance status

## Published alpha

[Seali 2.0.0-alpha.5](https://github.com/frpboy/Seali/releases/tag/v2.0.0-alpha.5) is the first public prerelease from this fork. Its package is `com.frpboy.seali`; the launcher branding and developer/support identity have been changed to Seali. Source packages still use `com.junkfood.seal` internally for compatibility and attribution. The README's earlier migration-in-progress wording predates this release.

The five actual public APKs were downloaded after publication and checked against GitHub's SHA256 asset digests, the Seali signing certificate, package/version and 16KB ZIP alignment. The known embedded upstream credential fragments were absent from those APKs. These targeted checks are not a complete security audit.

The final cleanup APK has not yet been device-tested. Earlier user recordings demonstrated Classic/v2 switching in both directions on an earlier build. Do not treat that as proof of all final-build behavior.

## Current qualification gates

- Classic correctness follow-up [PR14](https://github.com/frpboy/Seali/pull/14) is merged on main after exact-head CI passed. It is not included in the published alpha.
- The compact Classic header from [PR15](https://github.com/frpboy/Seali/pull/15) was incorporated through merged PR21; PR15 is closed as superseded, not merged separately. Device layout/accessibility qualification remains open. Recreate-preserving transition polish is separate unfinished work.
- Play storage and manual-update UI paths remain open. Play variants are for Google Play distribution only, never GitHub downloads or Actions artifacts. No Play publication or policy-compliance claim.
- About cleanup [PR16](https://github.com/frpboy/Seali/pull/16) is merged on main: duplicate donation rows are removed and Support requests opens the maintainer-provided Telegram group. This is not in the published alpha. About localization, translated sponsorship wording and content-URI file-size lookup still need follow-up.
- Historical review feedback is being revalidated against source. A passing build does not resolve every review thread.

Preserve the upstream GPL license, credits and meaningful history. Report fork-specific problems to this repository. The original project and its community do not operate this fork's support channels.

## Maintenance update: 2026-11-09

- [PR31](https://github.com/frpboy/Seali/pull/31) merged the Seali-specific community documentation and translation contribution route. [PR32](https://github.com/frpboy/Seali/pull/32) corrected public maintainer contact to `frpboy12@gmail.com`; the earlier address was incorrect. These documentation changes do not update published APKs.
- App contact, translation and upstream-credit changes in [PR30](https://github.com/frpboy/Seali/pull/30) remain unmerged pending actual visual qualification. Test builds and installation succeeded in the initial capture checks, but no complete screenshot set has been recovered. A later capture with a corrected lazy-list test harness stopped during dependency download, before app compilation or instrumentation. The harness correction is therefore not runtime-qualified; no visual pass is claimed. This is not full app-navigation, native-runtime, accessibility or all-locale qualification.
- README sponsor buttons in [PR33](https://github.com/frpboy/Seali/pull/33) remain an open proposal, separate from the current sidebar configuration in `.github/FUNDING.yml`. Financial support remains optional and separate from help requests.
- [Issue 34](https://github.com/frpboy/Seali/issues/34) is an open, unassigned good first issue for the exactly-one-hour duration boundary and local JVM tests. The source finding is not a shipped fix or a device-test result.
- Work toward source-built native packages is a source-review checkpoint only. There is no qualified replacement AAR, admitted native build or new published package. See [native dependency provenance](docs/NATIVE_DEPENDENCY_PROVENANCE.md) for the existing payload evidence and remaining limits.

Current status belongs here; dated audits and earlier release notes remain historical records. Passing a documentation build does not close app or release gates.

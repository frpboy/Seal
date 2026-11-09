# Seali maintenance status

## Published alpha

[Seali 2.0.0-alpha.5](https://github.com/frpboy/Seali/releases/tag/v2.0.0-alpha.5) is the first public prerelease from this fork. Its package is `com.frpboy.seali`; the launcher branding and developer/support identity have been changed to Seali. Source packages still use `com.junkfood.seal` internally for compatibility and attribution. The README's earlier migration-in-progress wording predates this release.

The five actual public APKs were downloaded after publication and checked against GitHub's SHA256 asset digests, the Seali signing certificate, package/version and 16KB ZIP alignment. The known embedded upstream credential fragments were absent from those APKs. These targeted checks are not a complete security audit.

The final cleanup APK has not yet been device-tested. Earlier user recordings demonstrated Classic/v2 switching in both directions on an earlier build. Do not treat that as proof of all final-build behavior.

## Current qualification gates

- Classic correctness follow-up [PR14](https://github.com/frpboy/Seali/pull/14) is merged on main after exact-head CI passed. It is not included in the published alpha.
- The approved compact Classic header is proposed in [PR15](https://github.com/frpboy/Seali/pull/15), now targeting main after PR14 merged. Device layout/accessibility qualification remains open. Recreate-preserving transition polish is separate unfinished work.
- Play storage and manual-update UI paths remain open. Play variants are for Google Play distribution only, never GitHub downloads or Actions artifacts. No Play publication or policy-compliance claim.
- About cleanup [PR16](https://github.com/frpboy/Seali/pull/16) is merged on main: duplicate donation rows are removed and Support requests opens the maintainer-provided Telegram group. This is not in the published alpha. About localization, translated sponsorship wording and content-URI file-size lookup still need follow-up.
- Historical review feedback is being revalidated against source. A passing build does not resolve every review thread.

Preserve the upstream GPL license, credits and meaningful history. Report fork-specific problems to this repository. The original project and its community do not operate this fork's support channels.

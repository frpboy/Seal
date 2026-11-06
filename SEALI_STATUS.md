# Seali maintenance status

## Published alpha

[Seali 2.0.0-alpha.5](https://github.com/frpboy/Seal/releases/tag/v2.0.0-alpha.5) is the first public prerelease from this fork. Its package is `com.frpboy.seali`; the launcher branding and developer/support identity have been changed to Seali. Source packages still use `com.junkfood.seal` internally for compatibility and attribution. The README's earlier migration-in-progress wording predates this release.

The five actual public APKs were downloaded after publication and checked against GitHub's SHA256 asset digests, the Seali signing certificate, package/version and 16KB ZIP alignment. The known embedded upstream credential fragments were absent from those APKs. These targeted checks are not a complete security audit.

The final cleanup APK has not yet been device-tested. Earlier user recordings demonstrated Classic/v2 switching in both directions on an earlier build. Do not treat that as proof of all final-build behavior.

## Current qualification gates

- Classic correctness follow-up: [PR14](https://github.com/frpboy/Seal/pull/14), pending its own exact-head tests and merge. This is separate from the published alpha.
- The approved compact Classic header is proposed in [PR15](https://github.com/frpboy/Seal/pull/15), stacked on PR14. Device layout/accessibility qualification remains open. Recreate-preserving transition polish is separate unfinished work.
- Play storage and manual-update UI paths remain open. No Play publication or policy-compliance claim.
- About localization/mail-handler fallback, translated sponsorship wording and content-URI file-size lookup still need follow-up.
- Historical review feedback is being revalidated against source. A passing build does not resolve every review thread.

Preserve the upstream GPL license, credits and meaningful history. Report fork-specific problems to this repository. The original project and its community do not operate this fork's support channels.

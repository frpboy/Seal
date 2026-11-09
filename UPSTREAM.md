# Upstream provenance and maintenance

## Origin

This repository continues work based on:

- Project: [Seal](https://github.com/JunkFood02/Seal)
- Original developer: [JunkFood02](https://github.com/JunkFood02)
- Original license: GNU General Public License v3.0
- Upstream default branch: `main`

## Continuation maintainer

- Project continuation: **Seali**
- Maintainer: **Rahul Muraleedharan**
- GitHub: [@frpboy](https://github.com/frpboy)

## Attribution and licensing policy

1. Preserve the original Git history wherever possible.
2. Keep the GPL-3.0 license and existing copyright notices intact.
3. Do not represent upstream code as original Seali-authored work.
4. Identify substantial Seali changes in commit messages, release notes, and documentation.
5. Keep a clear record of imported upstream changes and their source commits.
6. Clearly mark modified builds and comply with GPL-3.0 source-distribution requirements.
7. Do not use upstream branding, artwork, release channels, or community identity in a way that implies endorsement.

## Maintenance policy

Seali will maintain its own issue triage, roadmap, testing, release process, and documentation. Upstream changes may be reviewed and selectively integrated after compatibility and security checks. An upstream sync must never silently overwrite Seali-specific fixes.

## Current identity and compatibility

The repository is now standalone at [frpboy/Seali](https://github.com/frpboy/Seali). Seali uses the installed application ID `com.frpboy.seali`; the published alpha and later main changes are distinguished in [SEALI_STATUS.md](SEALI_STATUS.md). Detaching or renaming the repository does not replace inherited dependencies or qualify a new binary.

Thank you to JunkFood02 and the original contributors for Seal. Their authorship, history and required notices remain intact. See [CREDITS.md](CREDITS.md). Internal Kotlin namespaces and functional dependency coordinates are retained until a separately reviewed, tested migration; they do not identify Seali as an official upstream release.

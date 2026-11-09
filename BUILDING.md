# Building and testing Seali

Seali is an independent GPL-3.0 continuation of [JunkFood02/Seal](https://github.com/JunkFood02/Seal). This guide describes this fork, not upstream release support.

## Prerequisites

- JDK 21. Both app and buildSrc request this toolchain.
- Android SDK Platform 36, Build Tools 36.0.0 and platform-tools.
- The checked-in Gradle wrapper (Gradle 8.11.1).
- Set `ANDROID_HOME` to the SDK directory, or set `sdk.dir` in an untracked `local.properties` file.

## Debug build and unit tests

From the repository root:

```sh
./gradlew testGenericDebugUnitTest
./gradlew -p buildSrc test --console=plain
./gradlew assembleGenericDebug
```

Request buildSrc tests explicitly. Building the Android app does not automatically execute them. The main Android CI workflow runs both test commands.

Debug builds use `com.frpboy.seali.debug`, so they are separate from the published `com.frpboy.seali` app. Do not use a debug build to claim release-signature upgrade testing.

Product flavors are `generic`, `githubPreview`, `fdroid` and `play`. The preview flavor also adds `.preview` to its package name. The Play flavor is work in progress, not a store-ready distribution: scoped-storage output, update UI, policy review and device qualification remain open.

## Release signing

Generic and Play release packaging fail closed without complete signing configuration. Never commit a private keystore, passwords or generated signing material.

Use the existing environment variables or an untracked `keystore.properties` file as the build script specifies:

| Environment variable | Local property |
| --- | --- |
| `SEALI_UPLOAD_KEY_ALIAS` | `keyAlias` |
| `SEALI_UPLOAD_KEY_PASSWORD` | `keyPassword` |
| `SEALI_UPLOAD_STORE_PASSWORD` | `storePassword` |
| `SEALI_UPLOAD_KEYSTORE_FILE` | `storeFile` |
| `SEALI_UPLOAD_KEYSTORE_TYPE` | `storeType` (defaults to JKS) |

The CI secret `SEALI_UPLOAD_KEYSTORE_BASE64` supplies the keystore bytes to the runner; it is not a substitute for the local file-path variable. Follow the workflow's validation rather than printing secret values for debugging.

## Distribution and verification

GitHub distributes generic APKs only. Play APKs and AABs must not be uploaded to GitHub releases or Actions artifacts; Play distribution requires its separately approved Google Play process. Local/CI validation of the Play flavor is not publication permission.

The first Seali alpha is [v2.0.0-alpha.5](https://github.com/frpboy/Seali/releases/tag/v2.0.0-alpha.5). It provides separate universal, ARM64, ARMv7, x86 and x86_64 APKs. Use the universal APK when unsure which architecture your device uses. This is an alpha, not a stable or Play-qualified release.

The published APKs use the Seali package and signer, not upstream Seal's. Upstream app data does not migrate automatically across the package-name change. After the download-folder rename, existing files under `Download/Seal` and `Download/.Seal` are likewise never moved or deleted automatically: completed-download history keeps pointing at the old paths and stays valid, custom download folders chosen in settings are unaffected, and the app's clear-temp only covers the current folders. Existing downloads stay in the legacy folders; nothing is copied or moved for you. To reclaim space or consolidate, move anything you still need yourself and then delete the legacy folders manually from a file manager. Leftover temp files from interrupted downloads may remain under the old `Download/Seal/tmp` and can be removed the same way. ABI-specific version codes have intentional offsets from the universal version code.

The release-staging workflow is manual end to end. Routine push and pull request builds of `android_ci.yml` publish no APK artifacts by design. The `publish_apk_artifacts=true` input makes APKs publicly downloadable immediately; it is distribution, not a private qualification route. That upload is blocked until the exact signed candidate bytes have been qualified privately and approved for distribution. No admitted private exact-byte qualification route is established yet, so the current staging route remains blocked. A separate local build does not prove that a later CI build has identical bytes. Once the exact bytes qualify before public upload, the manual route uses `android_ci.yml` with `publish_apk_artifacts=true`, then `stage-alpha-release.yml` with that run ID. Staging requires the source run to be a successful same-repository manual dispatch, verifies the exact current-main source SHA, rejects existing tags, validates all five expected APK paths and creates a draft prerelease. It is not a general upload route for arbitrary workflows or commits. Published assets must not be silently replaced for later changes.

Build success and APK signature/checksum checks do not prove device behavior. Runtime tests must still cover downloads, cancellation/restart, playlist titles, storage destinations, interface switching, small/large screens, large fonts and accessibility. Check the release notes and PR evidence for what was actually tested.

## Next prerelease qualification

Source currently names 2.0.0-alpha.6; that is not evidence of a published release. Before staging, freeze the reviewed current-main commit, build all five generic APKs with the existing signer, and verify package/version, ABI version-code offsets, signatures and SHA-256 hashes against the actual retained files. Routine CI runs intentionally retain no APKs and cannot supply release assets.

Run applicable device checks on those exact bytes, including install/upgrade behavior, the changed Classic/About paths, downloads and cancellation/restart, storage destinations and native operations. Record device/API/page-size and any untested paths. A synthetic screenshot harness does not qualify AppEntry navigation or the complete downloader. Preserve current source, notices and dependency provenance; do not treat local replacement recipes as built dependencies. Any unresolved security, source-distribution or native packaging issue must be settled before distributing affected binaries.

Qualification must happen before public Actions artifact upload, not merely before draft creation. After exact-byte qualification and distribution approval, the existing manual staging route creates a draft prerelease only. Review its exact source, title/tag, notes and all five assets before publishing. Do not publish a documentation-only tag as an app update or reuse alpha.5 APKs under alpha.6.

# Building and testing Seali

Seali is an independent GPL-3.0 continuation of [JunkFood02/Seal](https://github.com/JunkFood02/Seal). This guide describes this fork, not upstream release support.

The next build requires Android 8.0 (API 26). The previously published alpha is unchanged. Android 7.0 and 7.1 are no longer supported by current source.

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

The first Seali alpha is [v2.0.0-alpha.5](https://github.com/frpboy/Seal/releases/tag/v2.0.0-alpha.5). It provides separate universal, ARM64, ARMv7, x86 and x86_64 APKs. Use the universal APK when unsure which architecture your device uses. This is an alpha, not a stable or Play-qualified release.

The published APKs use the Seali package and signer, not upstream Seal's. Upstream app data does not migrate automatically across the package-name change. ABI-specific version codes have intentional offsets from the universal version code.

The release-staging workflow accepts a successful `android_ci.yml` push build from this repository, verifies the exact current-main source SHA, rejects existing tags, validates all five expected APK paths and creates a draft prerelease. It is not a general upload route for arbitrary workflows or commits. Published assets must not be silently replaced for later changes.

Build success and APK signature/checksum checks do not prove device behavior. Runtime tests must still cover downloads, cancellation/restart, playlist titles, storage destinations, interface switching, small/large screens, large fonts and accessibility. Check the release notes and PR evidence for what was actually tested.

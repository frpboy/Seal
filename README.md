# Seali

<img src="fastlane/metadata/android/en-US/images/icon.png" width="112" height="112" alt="Seali app logo" />

<img src="https://komarev.com/ghpvc/?username=frpboy-seali&amp;label=README%20VIEWS&amp;color=0e75b6&amp;style=for-the-badge" alt="Seali README views" />

<sub>Badge requests counted by Komarev, not unique visitors or historical GitHub traffic.</sub>

**Seali** is an independently maintained continuation of the open-source Android video and audio downloader originally created by [JunkFood02](https://github.com/JunkFood02/Seal).

This repository is maintained by [Rahul Muraleedharan (@frpboy)](https://github.com/frpboy). Seali builds on the original project's work; it does not claim authorship of the upstream code.

> **Project status:** [Seali 2.0.0-alpha.5](https://github.com/frpboy/Seali/releases/tag/v2.0.0-alpha.5) is available as an alpha prerelease. The app uses the Seali identity and `com.frpboy.seali` package. The final cleanup build still needs device testing; it is not a stable or Play-qualified release. Later changes on main are not automatically included in those published APKs.

## Current maintenance and next release

The current source version is **2.0.0-alpha.6**, but it has not been published. The latest public APKs remain alpha.5. Documentation/contact corrections and merged Classic/About maintenance on main do not update an installed alpha.5 APK. See [current status](SEALI_STATUS.md) and the [Seali changelog](CHANGELOG.md#seali-maintenance-unreleased) for completed source changes and release limits.

The About/contact/translation changes in PR30 and queue-clarity changes in PR38 remain unmerged and await visual checks. The README sponsor buttons (PR33) and view-counter/safe-CI changes (PR39) are merged source maintenance, not new app releases. Native source-build preparation has not replaced the inherited dependencies. A new release needs exact-source signed APKs and the applicable runtime, native/security, source and notice checks; green documentation CI alone is not release qualification.

## Download and use

Download an APK from the [alpha release page](https://github.com/frpboy/Seali/releases/tag/v2.0.0-alpha.5). It provides universal, ARM64, ARMv7, x86 and x86_64 builds. Choose universal if unsure. Android 7.0 (API 24) or newer is required by the current build configuration. Seali installs separately from upstream Seal; upstream app data does not migrate automatically.

Paste a supported video link and tap Download, then choose the desired options. The interface can be switched between Classic and v2 in Settings > Appearance > Interface. Finish or cancel active downloads before switching.

Read [BUILDING.md](BUILDING.md) for the JDK 21/SDK 36 setup, real test commands, flavors and signing requirements. [SEALI_STATUS.md](SEALI_STATUS.md) distinguishes published checks from remaining device, storage and review gates.

## Screenshots

Actual user-device captures of **Seali 2.0.0-alpha.6**, supplied on November 9, 2026. The navigation and configuration views are cropped to Seali-only UI for privacy. No UI has been repainted or recreated. These captures show an installed alpha build, not qualification of the latest source or a new public release.

<p>
  <img src="docs/showcase/alpha6-device/queue.png" width="240" alt="Seali alpha.6 empty download queue, All tab" />
  <img src="docs/showcase/alpha6-device/navigation.png" width="240" alt="Seali alpha.6 navigation drawer, cropped" />
</p>

<p>
  <img src="docs/showcase/alpha6-device/video-options.png" width="300" alt="Seali alpha.6 video configuration controls, cropped" />
  <img src="docs/showcase/alpha6-device/audio-options.png" width="300" alt="Seali alpha.6 audio configuration and additional settings, cropped" />
</p>

Queue and navigation above; video and audio configuration below. The empty queue does not demonstrate a successful download. Personal media, links, notifications and error reports are excluded. [Capture scope and provenance](docs/showcase/README.md#user-device-gallery).

## About

Seali is based on the original Seal application, a Kotlin/Jetpack Compose Android interface for [yt-dlp](https://github.com/yt-dlp/yt-dlp).

The inherited feature set includes:

- Download video and audio from websites supported by yt-dlp.
- Extract audio and embed supported metadata and thumbnails.
- Download playlists.
- Use the bundled aria2c external downloader.
- Embed subtitles in downloaded videos.
- Create and run custom yt-dlp command templates.
- Manage downloads and command templates.
- Material Design 3 interface with dynamic color support.

These are inherited capabilities. Current compatibility and functionality will be revalidated as part of the Seali maintenance work.

## Thanks and credits

Thank you to **JunkFood02** for creating Seal and sharing its source, and to the original contributors, translators and dependency maintainers whose work makes Seali possible. See [CREDITS.md](CREDITS.md) for attribution and the distinction between Seali maintenance and upstream dependencies.

## Original project and attribution

Seali is a derivative work of **Seal**, originally created and maintained by **JunkFood02**.

- Original repository: [JunkFood02/Seal](https://github.com/JunkFood02/Seal)
- Original author: [@JunkFood02](https://github.com/JunkFood02)
- Original license: [GNU General Public License v3.0](LICENSE)
- Current continuation maintainer: [Rahul Muraleedharan (@frpboy)](https://github.com/frpboy)

The original Git history and applicable copyright and license notices are retained. Credit is also due to the upstream dependencies and projects acknowledged in the original source tree.

Seali is an independent community continuation and is not an official release of, or endorsed by, JunkFood02. Please report Seali-specific issues in this repository; upstream issues should be reported to the original project.

## Development

The project uses Kotlin, Jetpack Compose and Gradle. Internal source packages retain `com.junkfood.seal`; that is not the installed application identifier. See [BUILDING.md](BUILDING.md) before changing build or signing configuration.

Before submitting changes:

1. Read [CONTRIBUTING.md](CONTRIBUTING.md) and follow the repository's contribution conventions.
2. Keep changes focused and include tests where practical.
3. Clearly identify inherited behavior versus new Seali-specific changes.
4. Preserve all third-party copyright and license notices.
5. Do not claim that a feature is supported until it has been tested on the current codebase.

## License

Seali's inherited source code is distributed under the GNU General Public License, version 3. See [LICENSE](LICENSE).

When distributing modified builds, comply with GPL-3.0, including the requirements to provide the corresponding source code and preserve required notices. Modified versions should be clearly identified as modified so problems are not attributed to the original authors.

## Support

For Seali and the maintainer's other projects, use the [FrpBoy Projects Telegram group](https://t.me/FRPboy_Projects). Search existing repository issues before reporting a reproducible bug. Do not send private links, cookies, credentials or logs containing secrets to a public group.

Public maintainer email: [frpboy12@gmail.com](mailto:frpboy12@gmail.com), confirmed by the maintainer. Do not send passwords, cookies or other secrets.

Optional financial support is separate from help requests.

[![Buy Me a Coffee](https://img.shields.io/badge/Buy_Me_a_Coffee-FFDD00?style=for-the-badge&logo=buymeacoffee&logoColor=000000)](https://buymeacoffee.com/frpboy)
[![Support on Patreon](https://img.shields.io/badge/Support_on_Patreon-FF424D?style=for-the-badge&logo=patreon&logoColor=FFFFFF)](https://www.patreon.com/c/FrpBoy)

On main, PR16 keeps these options under the single Sponsor entry. The published alpha predates that cleanup and still has the duplicate About donation rows.

---

**Seali** - continued independently by Rahul Muraleedharan, with respect for the original work by JunkFood02.

# Seali

**Seali** is an independently maintained continuation of the open-source Android video and audio downloader originally created by [JunkFood02](https://github.com/JunkFood02/Seal).

This repository is maintained by [Rahul Muraleedharan (@frpboy)](https://github.com/frpboy). Seali builds on the original project's work; it does not claim authorship of the upstream code.

> **Project status:** [Seali 2.0.0-alpha.5](https://github.com/frpboy/Seal/releases/tag/v2.0.0-alpha.5) is available as an alpha prerelease. The app uses the Seali identity and `com.frpboy.seali` package. The final cleanup build still needs device testing; it is not a stable or Play-qualified release. Later changes on main are not automatically included in those published APKs.

## Download and use

Download an APK from the [alpha release page](https://github.com/frpboy/Seal/releases/tag/v2.0.0-alpha.5). It provides universal, ARM64, ARMv7, x86 and x86_64 builds. Choose universal if unsure. Android 7.0 (API 24) or newer is required by the current build configuration. Seali installs separately from upstream Seal; upstream app data does not migrate automatically.

Paste a supported video link and tap Download, then choose the desired options. The interface can be switched between Classic and v2 in Settings > Appearance > Interface. Finish or cancel active downloads before switching.

Read [BUILDING.md](BUILDING.md) for the JDK 21/SDK 36 setup, real test commands, flavors and signing requirements. [SEALI_STATUS.md](SEALI_STATUS.md) distinguishes published checks from remaining device, storage and review gates.

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

Optional financial support is separate from help requests: [Buy Me a Coffee](https://buymeacoffee.com/frpboy) or [Patreon](https://www.patreon.com/c/FrpBoy). The app keeps these options under its single Sponsor entry.

---

**Seali** - continued independently by Rahul Muraleedharan, with respect for the original work by JunkFood02.

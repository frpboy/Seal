# Seali

**Seali** is an independently maintained continuation of the open-source Android video and audio downloader originally created by [JunkFood02](https://github.com/JunkFood02/Seal).

This repository is maintained by [Rahul Muraleedharan (@frpboy)](https://github.com/frpboy). Seali builds on the original project's work; it does not claim authorship of the upstream code.

> **Project status:** Rebranding and maintenance handover are in progress. The existing application code, package identifier, resources, and build/release configuration still contain upstream Seal identity in places. Those will be reviewed and migrated deliberately in follow-up changes rather than being changed blindly in a documentation-only commit.

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

The project is an Android application written primarily in Kotlin and Jetpack Compose. The original project structure and build configuration are being retained while the continuation is audited.

Before submitting changes:

1. Read [CONTRIBUTING.md](CONTRIBUTING.md) and follow the repository's contribution conventions.
2. Keep changes focused and include tests where practical.
3. Clearly identify inherited behavior versus new Seali-specific changes.
4. Preserve all third-party copyright and license notices.
5. Do not claim that a feature is supported until it has been tested on the current codebase.

## License

Seali's inherited source code is distributed under the GNU General Public License, version 3. See [LICENSE](LICENSE).

When distributing modified builds, comply with GPL-3.0, including the requirements to provide the corresponding source code and preserve required notices. Modified versions should be clearly identified as modified so problems are not attributed to the original authors.

---

**Seali** — continued independently by Rahul Muraleedharan, with respect for the original work by JunkFood02.

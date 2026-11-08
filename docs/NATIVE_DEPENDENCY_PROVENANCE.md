# Third-party native dependency provenance

Facts below were verified on 2026-11-09 against the sources listed at the end.
This document records what is bound, what is not, and what remains open. It
makes no legal/compliance conclusion.

## Scope

Seali bundles native code through the `io.github.junkfood02.youtubedl-android`
Maven artifacts `library`, `ffmpeg`, and `aria2c`, pinned in
`gradle/libs.versions.toml`:

- `main` (commit aed0630a2e59816429bdc952f22bd41d17353c8c): version 0.17.3
- PR #25 head (e30bf3bcd4240618dc08f25d3446a0b2339d6f1e): version 0.18.1

Both artifact POMs declare "GPL-3.0 license" and point their SCM URL at the
upstream `github.com/yausername/youtubedl-android`; the publishing groupId is
the fork `io.github.junkfood02`.

## Version 0.17.3 (what main currently ships)

- `ffmpeg-0.17.3.aar` from Maven Central, SHA-256
  `e12402803f7a61802fded6bd021422688cb7a03e924068a1aec983dae88ea3c1`, matches
  Maven Central's published `.sha256` sidecar.
- Its arm64-v8a payload `libffmpeg.zip.so` has SHA-256
  `3e4dd8424f2ab3ca741ac37e121fac2efb4c3426948e16ace1ef9a1ffb482930`,
  byte-identical to the payload checked into the public repository
  `github.com/junkfood02/youtubedl-android` at commit
  `d7c6ea110e4c6191fe1958402a62e69fdaee6674` ("release 0.17.2", the public
  `master` HEAD). The published binary therefore equals the payload recorded
  in public source control together with the Kotlin wrapper sources.
- Payload self-description (from its own strings): built on a GitHub Actions
  runner at path `/home/runner/work/build-ffmpeg/build-ffmpeg`, Android NDK
  25.2.9519653, FFmpeg 7.0.1, configured with
  `--enable-gpl --enable-version3 --enable-libx264 --enable-libx265`;
  `libavutil` carries the banner "GPL version 3 or later".
- A public repository named `build-ffmpeg` was not located under either
  `junkfood02` or `yausername` on GitHub as of 2026-11-09, so the Actions
  build definition itself was not found in sources checked.
- Note: binary equality with the checked-in payload does not by itself
  establish complete corresponding source (exact build environment and the
  exact versions of bundled libraries such as libx264/libx265 are not pinned
  by the evidence above).

## Version 0.18.1 (what PR #25 proposes)

- `ffmpeg-0.18.1.aar` SHA-256
  `0a87ffa6cf912b0fe76c1a99b9107f543ee2f247935fae2c71f0822eb7bc5f49`,
  matches Maven Central's published `.sha256` sidecar.
- Its arm64-v8a payload `libffmpeg.zip.so` has SHA-256
  `4664a895283f9f31786be39b0c484c1f96631f02c7c7f9c70a4a4a2da324a092`, which
  does NOT match the payload at the public fork HEAD. The fork has no 0.18.x
  tag and only the `master` branch (whose HEAD is the 0.17.2 commit). The
  source state that produced the 0.18.1 artifacts was not located in sources
  checked.
- Payload self-description: termux-style build (prefix
  `/data/data/com.termux/files/usr`, `.termux-build` cache paths), Android
  NDK r28c, `--enable-gnutls`, `--enable-gpl --enable-version3
  --enable-libx264 --enable-libx265`; `libavutil` is version 59.39.100
  (FFmpeg 7.1.x ABI) and carries the banner "GPL version 3 or later".
- A bare string "7.1.4" in `libavutil` sits adjacent to a bundled component's
  `cpuflags` strings and is not FFmpeg's own version. In `termux-packages`
  history for `packages/ffmpeg/build.sh` (checked 2026-11-09), FFmpeg bumps
  run 7.1 -> 7.1.1 -> 7.1.2 -> 8.0 with no 7.1.3/7.1.4, and the payload's
  `--enable-gnutls` predates termux's switch to OpenSSL (commit 5cfbcdc).
  Termux recipe commits 7.1 (da2b4c8), 7.1.1 (c758559) and 7.1.2 (5700d51)
  are era-consistent candidates only; none is bound to this payload.

## Build recipe documentation

`BUILD_FFMPEG.md` in the fork documents the build method (termux-packages
`build-package.sh ffmpeg`, repack the resulting debs into the zip payload).
The method is documented but pins nothing: no termux-packages commit, no
FFmpeg version, no NDK version.

## aria2

The `aria2c` payload banner reads "GNU General Public License ... either
version 2 of the License, or (at your option) any later version", consistent
with the in-app credit label. Sources jars for these artifacts contain only
the Kotlin wrapper sources; they include no native build scripts, patches or
FFmpeg/aria2 source.

## Notice surface today

- Repository `LICENSE`: GNU General Public License v3.0.
- In-app: the Credits page (FFmpeg "GNU General Public License v3.0 or
  later", aria2 "GNU General Public License v2.0", yt-dlp "The Unlicense",
  and others). The payload zips carry no license documentation:
  0.17.3 contains `usr/lib` plus macOS archive metadata (`__MACOSX`,
  `usr/.DS_Store`); 0.18.1 contains `usr/lib` only. Neither has
  `usr/bin/ffmpeg` nor any `share/doc` copyright or license files.

## Open items

1. The source state that produced the 0.18.1 Maven artifacts is not located
   in sources checked (no tag, no branch, no matching checked-in payload).
2. No pinned termux-packages commit or NDK version is recorded for either
   payload's build.
3. The exact versions of the bundled libraries (libx264, libx265 and others
   in the configure line) are not enumerated anywhere checked.
4. The `build-ffmpeg` Actions repository referenced by the 0.17.3 payload's
   configure path is not publicly located.
5. The payload zips carry no license documentation files.

## Sources checked (2026-11-09)

- Maven Central artifacts and `.sha256` sidecars: ffmpeg 0.17.3 and 0.18.1,
  library 0.17.3 and 0.18.1, aria2c 0.18.1 (POMs and sources jars included).
- `github.com/junkfood02/youtubedl-android`: full branch and tag set, HEAD
  tree at d7c6ea1 including checked-in jniLibs payloads and BUILD_FFMPEG.md.
- `github.com/yausername/youtubedl-android` tags.
- `termux/termux-packages` commit history for `packages/ffmpeg/build.sh` and
  the current recipe.
- Repository existence checks for `junkfood02/build-ffmpeg` and
  `yausername/build-ffmpeg` (not found).

Not checked: termux-packages history for files other than
`packages/ffmpeg/build.sh`, jitpack, other junkfood02 repositories, Maven
Central version-listing metadata beyond the versions above.

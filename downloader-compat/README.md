# API24 downloader wrapper investigation

DRAFT PROTOTYPE. Not an accepted permanent architecture or a released module.

This module vendors the 13 Kotlin source files published in
`io.github.junkfood02.youtubedl-android:library:0.17.3`, retaining the package and
public request/process APIs. Original upstream author: yausername and contributors.
Source project: https://github.com/yausername/youtubedl-android
Published artifact fork: https://github.com/JunkFood02/youtubedl-android
License: GPL-3.0, preserved in LICENSE and upstream POM. This does not replace
or relicense upstream/native code.

Sources jar: https://repo.maven.apache.org/maven2/io/github/junkfood02/youtubedl-android/library/0.17.3/library-0.17.3-sources.jar
Sources SHA256: 263ed633ab505502a4b3c490400e4d8dd32e3c9c13645cc57bc10e3de81de207
Runtime AAR: https://repo.maven.apache.org/maven2/io/github/junkfood02/youtubedl-android/library/0.17.3/library-0.17.3.aar
AAR SHA256: 9d29b148187242d6e754fb59f126b62dde5c1c7fcadaa29d14dbe13659495e4c

The cache-safe ExtractDownloaderRuntime build task checks that exact SHA256
before extracting only `res/` and `jni/`. No native binaries are committed,
rebuilt, substituted, downloaded from an unpinned URL, or updated by this trial.
The old AAR's JVM classes are not packaged. FFmpeg/aria2/common stay on 0.17.3.

## Minimal source delta

Seven of the original thirteen Kotlin files remain unchanged except for final-newline normalization:
request/options, response, exceptions, callbacks and process/stream support.
The modified originals are YoutubeDL.kt, YoutubeDLUpdater.kt, and four mapper
files. Added DownloaderJson and PrimitiveStringSerializer use the app's existing
Kotlin serializer instead of eager Jackson. No reflection-based parser added.
The string primitive adapter preserves Jackson's numeric-to-string metadata
behavior while rejecting object/array coercion. Release metadata is a typed DTO;
missing fields, wrong types, missing exact yt-dlp asset, oversized input and
excessive nesting fail. The network read has finite timeouts and a2MiB cap.

Commons1.26/IO2.15.1/Lang3.18 remain patched. Google's NIO desugaring2.1.5
provides old-platform java.nio APIs; actual API24 and35 tests, not this statement,
must establish whether the chosen ZIP APIs work. minSdk stays24.

## Tests and limits

Six local module JVM fixtures passed. They caught and fixed a real compatibility
gap: JSON numbers in string metadata fields need explicit primitive conversion.
The application JVM/Android integration suites exercise the same mapper,
updater shape, ZIP APIs and bundled offline downloader startup. Their exact-head
CI results, release/R8, and min24/current35 runtime are required before advice.
No live updater, native dependency audit, accessibility or phone/provider matrix
success is claimed by this prototype.

## If adopted, update procedure and burden

1. Recover the new upstream sources/AAR from Maven Central and verify release
   provenance/license; record their hashes. Never change only the version/hash.
2. Compare all13 upstream Kotlin files. Preserve upstream process/init fixes and
   intentionally reapply only the six-file JSON adapter delta plus helpers.
3. Compare raw/native resource content; update extraction hash only after native
   packaging, ABI/signing/alignment and downloader runtime checks.
4. Re-run module fixtures, app tests, API24/35 integration and release/R8; scan the
   entire newly selected graph, including desugaring/native scope separately.
5. Review upstream security/release changes and document native/Python unknowns.

Adoption means owning about807lines of copied upstream JVM source plus the JSON
boundary, extraction task and tests. It avoids dropping Android7, but is a real
ongoing maintenance obligation. The safer short-term alternative is to keep the
release held until a compatible upstream fix exists. Dropping24 or keeping
unmitigated old advisories are not silently chosen alternatives.

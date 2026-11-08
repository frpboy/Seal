# Seali native remediation candidate - review packet

Status: source recipe and local candidate only. No repository adoption, new APK, Android execution, release or Play claim.

## Scope and provenance

Remote Seali candidate was fetched at e30bf3bcd4240618dc08f25d3446a0b2339d6f1e. Its current dependency is `io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1`. The official AAR SHA256 is `0a87ffa6cf912b0fe76c1a99b9107f543ee2f247935fae2c71f0822eb7bc5f49`. Maven metadata still names 0.18.1 latest. Upstream open issue 365 describes the same five nested WebP libraries; it is corroboration, not the provenance of the replacement binaries.

Inputs used by the recipe:

- https://repo.maven.apache.org/maven2/io/github/junkfood02/youtubedl-android/ffmpeg/0.18.1/ffmpeg-0.18.1.aar
- https://storage.googleapis.com/downloads.webmproject.org/releases/webp/libwebp-1.6.0.tar.gz, SHA256 `e4ab7009bf0629fd11982d4c2aa83964cf244cffba7347ecd39019a9e38c4564`
- https://dl.google.com/android/repository/android-ndk-r28c-linux.zip, SHA256 `dfb20d396df28ca02a8c708314b814a4d961dc9074f9a161932746f815aa552f`
- https://dl.google.com/android/repository/repository2-1.xml independently lists NDK 28.2.13676358 Linux size 722261334 and SHA1 `a7b54a5de87fecd125a17d54f73c446199e72a64`, matching the downloaded archive.
- https://developers.google.com/speed/webp/docs/compiling identifies 1.6.0 as its source-build example.
- https://github.com/yausername/youtubedl-android/issues/365 remains open. Proposed relinking there was not taken as a verified binary.
- https://github.com/yausername/youtubedl-android/issues/363 documents missing original producing inputs. This recipe does not reconstruct that original upstream build.

Original x86_64 `WebPGetDecoderVersion` disassembly returns `0x010600`, consistent with WebP 1.6.0. This is binary version evidence, not proof of the entire original source/patch set. The five files are `libsharpyuv.so`, `libwebp.so`, `libwebpdecoder.so`, `libwebpdemux.so`, `libwebpmux.so` under `usr/lib/` inside `libffmpeg.zip.so`.

## Rebuild and modification

Run `bash rebuild-webp.sh /absolute/nonexistent/workdir` on Linux x86_64 with curl, unzip, tar, Python3, CMake and make. Preparation used CMake 3.22.1 and NDK's own Clang toolchain. The target work directory must not exist; mkdir fails closed for existing directories/symlinks. The build executes under env -i with fixed PATH/locale/timezone/private HOME, freshly unpacks verified inputs and never reuses extracted inputs or CMake caches. Compiler flags and CMake environment from the caller are not inherited. Inputs are checked before unpacking. Tool versions are recorded in build-tool-versions.txt. Android API24 is retained, with ARM64 and x86_64 builds only. The app candidate's actual minimum is API26; the published alpha's API24 is not silently changed by this packet.

The build uses stock source, shared libraries, explicit `-Wl,-z,max-page-size=16384`, original unversioned SONAME behavior and 16-bit colorspace byte-swap support. No ELF header editing or decoder removal. Existing 32-bit payloads are unchanged because this fix concerns the two 64-bit page-size ABIs.

The AAR candidate replaces exactly five entries per affected ABI. ZIP inventory and every unrelated nested and top-level entry's decompressed bytes must remain equal. Original ZipInfo metadata is copied rather than mutating the source archive's metadata. No input payload is extracted onto disk by the patcher. Native candidate is not committed or uploaded in this packet.

Gradle integration is DRAFT SOURCE, not adopted or Android-qualified. `SealiWebpTransform` changes only AAR artifact bytes while keeping the original Maven module identity, classes/resources and transitive graph. The original input hash and deterministic replacement hash fail closed. Other AARs pass through unchanged. The app classpath requests a custom attribute; Gradle's existing AAR-to-Android transforms still operate on the AAR type. Native preparation has a 900-second process-group timeout (TERM then KILL after 30 seconds), four native compiler workers and a 512MiB/2-worker isolated Gradle qualification fixture. This is an operational ceiling, not a CPU/memory sandbox.

The isolated real Gradle8.11.1/JDK21 fixture actually executed the transform on the real original AAR with a synthetic Maven POM/transitive common JAR, retained original module identity, returned exactly transformed AAR + common JAR, and passed. Appended-byte original-AAR tamper failed before preparation. The Kotlin DSL registration separately compiled and reached that same expected hash-tamper failure. The scripted fixture is `qualification/run.sh` and is not part of app runtime. This proves Gradle transform mechanics, not the real Android variant graph. Full app dependencies/AGP compile, all final native inventory and runtime remain required before adoption or a signed preview. No local AAR is dropped into app/libs and no dependency exclusions flatten the original graph.

## Local evidence actually executed

- All ten rebuilt libraries pass ELF64 PT_LOAD alignment and congruence checks.
- Exact exported GLOBAL/WEAK dynamic symbol names, including version suffixes when present, match originals. No missing or added exports.
- SONAME unchanged and architecture checked. Rebuilt dependencies are restricted to the five-library set plus Android libc/libm/libdl. `libdl.so` is an added system dependency; this is not an identical dependency-graph claim.
- Readelf header/version output retained in `header-version-evidence.json`. NDK API24 selects the link-time platform API surface, not proof of actual Android API24 runtime behavior.
- Every patched nested FFmpeg payload contains 112 ELF64 files; all 112 pass the checker for each ABI. Other archive entry bytes are preserved. Candidate AAR SHA is in `native-remediation-evidence.json`.
- Two full recipe runs in distinct nonexistent private work directories, each freshly downloading/unpacking inputs and building with deliberately invalid caller CC/CFLAGS/LDFLAGS (plus invalid CMAKE_GENERATOR on run two), produced identical SHA256 for all ten rebuilt .so files and the candidate AAR. Existing-workdir negative control returned exit 1 before downloads/builds. Exact commands, per-file output hashes and rejection stderr are in fresh-two-build-comparison.json. This is local repeatability, not independent cross-host bit reproducibility.
- Native Linux host build of the same source passed `webp-roundtrip.c`: lossless encode/decode exact pixels; lossy encode/decode; mux and demux EXIF chunk; invalid decode rejection. This is NOT an Android runtime result.
- Both Android-ABI roundtrip executables compile. They have NOT run.
- Python syntax and shell syntax checks pass. The native alignment checker retains its separately reviewed 21-test suite.

Symbol matching does not prove ABI layout or behavior. The Android runtime and final application gates remain open.

## Mandatory runtime and final-artifact plan

1. Independently review this recipe, inputs, five-file boundary, SONAME/dependency/symbol checks and licensing.
2. Execute checksum-bound preparation independently. Test negative input hash, altered export, unrelated-entry and unknown/native-alignment controls before adoption.
3. Integrate through reviewed build source, preserving upstream classes/resources and transitive dependencies. Compile without signing first where possible. Generate a full native inventory for every final direct/nested native entry, including class, architecture, origin, version evidence, LOAD constraints and whether initialization loads it. Unknown provenance/version is explicit, not invented.
4. A bounded disposable 16KiB Android x86_64 image must prove `getconf PAGESIZE=16384`. Use an API image that actually supports 16KiB; an ordinary API35 x86_64 image is not proof. Root identity and IPv4/IPv6 network isolation precede synthetic tests. Do not touch owner providers or accounts.
5. Run Android `webp-roundtrip` against rebuilt libraries on 16KiB and supported ordinary-page environments. Exercise the standalone decoder library too, not merely the combined libwebp path.
6. Run the actual packaged FFmpeg/FFprobe and its extracted original payload with the five replacements: version/startup, synthetic PNG -> WebP -> PNG pixel/dimension check, lossy WebP, short animated WebP encode/decode/demux and a local synthetic remux. Capture exit codes and fatal linker/crash absence. No network download needed for this stage.
7. Inspect final APK/AAB direct and nested ELF64 inventory. Do not skip `.zip.so` or unsupported container errors. Verify current SDK `zipalign -c -P 16 -v 4` on final APK. APK ZIP alignment is separate from ELF LOAD alignment.
8. Install and launch actual app; onboarding/share/finish/skip/config/font/layout tests must execute and actual pixels be inspected. Verify FFmpeg operation through app integration, not only shell test. Check Android supported API behavior and a 16KiB environment. Signed preview can be generated/delivered only after the compatibility gates pass.
9. Preserve production certificate pin, fail-closed signing, encryption/private delivery and Play separation. No signing material in any evidence or Drive snapshot.

## Licensing

The original application and downloader retain GPL-3.0 attribution and corresponding-source obligations. WebP source includes BSD 3-Clause `COPYING`, `PATENTS` and `AUTHORS`, included here unchanged. When distributed, include source recipe, exact upstream source reference/hash, notices, modifications and relevant source alongside the existing GPL notices. This packet does not erase provenance gaps in the other bundled upstream native libraries.

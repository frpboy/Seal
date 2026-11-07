# Seali security baseline design (draft - not adopted, not pushed)

Status: local candidate branch `candidate/security-baseline` only. Release hold in effect.

## 1. Packaged baseline (implemented locally)

- Wrapper bump io.github.junkfood02.youtubedl-android 0.17.3 -> 0.18.1 (all three AARs):
  Python 3.11.10 -> 3.12.11, FFmpeg 7.0.1-pre -> exactly 7.1.1, aria2 unchanged at 1.37.0,
  adds libqjs (QuickJS) + yt_dlp_ejs which current yt-dlp requires for YouTube, 16 KB pages.
- Packaged yt-dlp override: app/src/main/res/raw/ytdlp = official yt-dlp 2026.10.26 zipapp,
  SHA-256 e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3.
  - Merge semantics (docs): "The build tools merge resources from a library module with those
    of a dependent app module. If a given resource name is defined in both modules, the
    resource from the app is used." and library references resolve to the merged resource.
    https://developer.android.com/studio/projects/android-library
  - Provenance: wrapper 0.18.1's own res/raw/ytdlp is byte-identical to the official
    2025.11.12 zipapp (hash match against official SHA2-256SUMS), so the override ships the
    exact format the loader already consumes. 2026.10.26 sums verified GPG-good against the
    key published in the yt-dlp repo (public.key, Simon Sawicki signing key).
  - Build-time enforcement: verifyBundledYtDlp Gradle task fails every resource merge if the
    file's SHA-256 differs from the pin; BUNDLED_YTDLP_VERSION in BuildConfig;
    DependencyAndroidCompatibilityTest asserts `yt-dlp --version` on device equals the pin
    (proves override + runtime compatibility end to end).
- yt-dlp 2026.10.26 requires Python >= 3.11 (verified: refuses 3.10). 0.18.1's 3.12.11 qualifies.

## 2. Residual after baseline

- FFmpeg 7.1.1 (bundled by 0.18.1): candidate-affected by the full 7.1.2 batch
  (https://patches.ffmpeg.org/security.html; cveawg ranges confirm "7.1.1 affected <8.0" for
  CVE-2025-9951 / CVE-2025-59728 / CVE-2025-59731 / 59732 / 59733 and "up to 7.1" for
  CVE-2025-1594). Build config: the wrapper's FFmpeg binaries are built from the Termux
  ffmpeg package recipe (BUILD_FFMPEG.md in youtubedl-android), a full default build with NO
  decoder/encoder/demuxer pruning. Stripped symbols make symbol-absence checks unreliable, so
  reachability uses registration strings instead: DASH demuxer long_name present in
  libavformat, AAC-encoder-only AVOption "aac_coder" present in libavcodec, jpeg2000 and
  OpenEXR codec strings present. All six CVEs of the 7.1.2 batch are therefore
  candidate-REACHABLE: CVE-2025-1594 (AAC encoder, when the user converts audio to AAC),
  CVE-2025-59728 (DASH demuxer, crafted manifest), CVE-2025-9951 (jpeg2000) and
  CVE-2025-59731/32/33 (OpenEXR) via crafted media the user downloads and post-processes.
  Confirmed-exploitable is NOT claimed; reachability rests on Termux build config + codec
  registration evidence + Seali call paths.
  Closure paths: (i) repack the wrapper's ffmpeg module from current Termux debs (Termux
  ships ffmpeg 9.0.2, past every fix; the wrapper only execs the ffmpeg/ffprobe binaries, so
  ABI risk is low, but this is a custom packaging step to qualify); (ii) wait for upstream to
  rebuild with a fixed FFmpeg. Open decision, not yet framed as accept-vs-build.
- aria2 1.37.0: CVE-2026-71832 (BitTorrent DoS, unfixed upstream, BT path never invoked by
  yt-dlp), CVE-2026-75421 (CVSS 4.0 low, -i input-file path never passed). Conditional on the
  off-by-default aria2 downloader preference.

## 3. Updater option (b): verified in-app updates (design, preserving the feature)

Current updater (YoutubeDLUpdater; re-checked at 0.18.1 - still no checksum/signature/size verification, tag-equality compare): HTTPS fetch of GitHub release JSON, no
checksum/signature/size verification, tag-equality compare (downgrade possible), runs on app
entry when enabled (default on), rollback to bundled on copy error.

Proposed (b) design:
- Authenticity: verify the downloaded binary against the release's SHA2-256SUMS and verify
  SHA2-256SUMS.sig with BouncyCastle OpenPGP (bcpg-jdk18on, MIT license - compatible with the
  app's GPLv3; adds ~2-3 MB method count heavy but acceptable). The yt-dlp release signing
  public key ships inside the app (raw resource), fingerprint-pinned.
- Key rotation: pinned fingerprint set only; a yt-dlp key change takes effect with a Seali
  release that ships the new key. Never fetch a new key at runtime (that would reintroduce
  TOFU-over-TLS). Documented operational risk: upstream key rotation breaks in-app updates
  until a Seali release; fails closed (keep current binary), which is safe.
- Downgrade protection: monotonic version compare (parse YYYY.M.D, require strictly newer
  than installed) instead of tag equality.
- Size sanity: require Content-Length and downloaded size within a sane bound (1-64 MB)
  before verification; verification is the actual gate.
- Atomic install: download to a temp file in the yt-dlp dir, verify, fsync, atomic rename
  over the target; on any failure keep the previous binary. Rollback target is the pinned
  bundled baseline (already fixed), so any updater failure state stays non-vulnerable.
- Cost estimate: ~150-250 lines Kotlin + bcpg dependency + device tests (valid sig accepted,
  tampered binary rejected, stale-version rejected, key-mismatch rejected).
- Alternative (a): remove the feature. Noted as a product decision for the user; (b) keeps
  the feature with real authenticity, so (a) is not required to ship the fixed baseline.

### 3.1 Install-state resolution (false-baseline fix)

Defect: the library extracts its bundle only when the on-disk binary is missing
(YoutubeDL.init_ytdlp) and writes dlpVersion only after a successful update, so an
existing install that never updated kept its old extracted binary while the updater's
pref-or-bundled fallback reported the new bundled version and skipped updates
indefinitely (false baseline).

Resolution (YtDlpInstallResolution + VerifiedYtDlpUpdater.resolveInstall), decided at
update-check time from the on-disk bytes, never from the pref alone:
1. Binary SHA-256 == pinned bundle hash (or binary missing; init re-extracts the bundle):
   BUNDLED. The pref is repaired to the bundled version only then, including the
   stale-high case after a missing-binary re-extract.
2. Bytes differ and a parseable pref exists: PREF_RECORDED. Verified updates write the
   pref only after the atomic rename, so the pref is trusted; a pref-recorded binary
   newer than the bundle is never downgraded.
3. Nothing else is trusted: UNKNOWN_LEGACY (includes a CORRUPT/unparseable pref,
   which previously froze updates because the monotonic compare always answered
   "not newer"). Only a verified, signature-checked upstream release not older
   than the shipped bundle may replace the binary; older releases are refused
   and nothing is deleted. The binary's self-asserted zipapp metadata
   (yt_dlp/version.py, read from the ZIP without executing anything) is kept
   only as a clearly marked unverified diagnostic hint (separate pref key,
   decision-free, not currently surfaced in the UI): a claimed version cannot
   freeze updates, and it is never written to the verified dlpVersion pref.

Boundary, stated plainly: an unmanaged binary (never updated through the app)
cannot be distinguished from a legacy runtime - its self-asserted metadata is
not proof - so it converges to the verified managed release (>= the shipped
bundle). No-downgrade is guaranteed for bundle bytes and pref-recorded
installs only, not unconditionally for unmanaged binaries. Detection never
executes unknown code. Known pre-existing limitation (assessed, unchanged): a
parseable pref claiming a version newer than upstream's latest (possible for
channel-switching nightly users, or manual pref tampering) reports
ALREADY_UP_TO_DATE until upstream passes it; the installed version is visible
in settings, and the pref's verified-install invariant makes this unreachable
through the app's own flows.

Ordering/concurrency: library init (startup, @Synchronized) extracts the bundle when
the binary is missing; resolution runs later at update-check time, so init always wins
and a missing binary resolves conservatively as BUNDLED. In-process update runs are
serialized by a mutex (auto update-check and the manual settings actions can race);
the atomic rename remains the last step, so any failure before it leaves the previous
binary untouched.

## 4. A1 upgrade continuity (implemented: UpgradeContinuityTest.kt + upgrade-continuity.yml)

Seeds and verifies ALL THREE state layers across install-over: MMKV preference, filesDir
marker, and a real Room DownloadedVideoInfo history row (schema v5 on both alpha5 and
current - verified against the tag's exported schemas, so no migration masks the test).
Workflow runs ONLY on workflow_dispatch (approved exact head) or push to main; never on
pull_request; keystore deleted after signing; no secret material in logs; the release
test APK is manually apksigner-signed with the upload key (androidTest would otherwise be
debug-signed and install-over + instrumentation would be refused).

Original design note:

Cert/versionCode asserts alone are a partial gate; full gate adds an install-over test:
1. CI emulator job installs the published alpha5 universal APK (public release asset).
2. Seed state: instrumented "seed" test (test-only Activity or shell am broadcast) writes a
   marker preference (MMKV) and a history DB row.
3. `adb install -r` the newly built universal APK (must succeed: same cert, versionCode
   200_000_160 > 200_000_150).
4. Instrumented "verify" test asserts the marker preference and DB row survived and the app
   launches to the Classic home.
Complements the static gate (draft step in /tmp/drafts/A1-signing-continuity-step.yml).

## 5. B3 SAF/permission (SafPermissionEmulatorTest.kt)

Drives the real system DocumentsUI tree picker via UI Automator (API 26 + API 35 jobs),
then proves actual write + read inside the granted tree via DocumentFile (not permission
state only), plus POST_NOTIFICATIONS grant visibility on API 33+.


## 6. FFmpeg closure research: Termux repack candidate (no adoption decision)

Evidence from owning sources:
- License: termux-packages ffmpeg build.sh declares TERMUX_PKG_LICENSE="GPL-3.0"
  (--enable-gpl --enable-version3 configure). Seali is GPLv3. Compatible. A full repack
  needs a transitive license inventory of every bundled dep (x264 GPL-2.0, aom BSD, etc.).
- Current Termux ffmpeg: 9.0.2-1 (packages-cf.termux.dev Packages index). Past every CVE
  fix in question (7.1.2/8.0.x batch and more).
- ABI/arch: the ffmpeg BINARY's own NEEDED set is only libav* + libm/libz/libdl/libc
  (readelf on the actual 9.0.2-1 aarch64 deb). The dependency closure sits one level down:
  libavcodec needs ~25 third-party libs, libavfilter ~15 more (libplacebo, zmq, harfbuzz...).
  However upstream's own 0.18.1 ffmpeg zip ALREADY bundles the full closure (165 lib
  entries incl. glib/gnutls/svt-av1), so the dpkg-deb extract+zip pattern in BUILD_FFMPEG.md
  is proven at this scale; a 9.0.2 repack follows the same pattern with a somewhat larger
  closure (new deps: libjxl etc.). APK size growth must be measured, not assumed.
- Invocation surface: wrapper execs the ffmpeg/ffprobe binaries (FFmpeg.kt unzip-and-exec),
  so an ffmpeg major bump carries no JNI/ABI link risk for the app; risk concentrates in
  CLI behavior changes (8.x/9.x deprecations) and must be qualified by running the 15-test
  suite + real post-processing downloads against the repacked binary.
- Termux min API level: build scripts target the Termux platform floor; the current wrapper
  binaries already run on Seali's minSdk 26 (0.18.1 tested), so a same-recipe repack keeps
  that floor. Exact TERMUX_API_LEVEL constant not yet pulled from source - marked open.
- Cost estimate: scripted deb-closure repack + size/license inventory + full runtime
  qualification. Medium, bounded; avoids maintaining a custom FFmpeg configure.

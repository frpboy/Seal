# Continuity gate status and proposed execution boundary

No install-over execution or new signing is authorized by this document.

## Evidence already established

- Source dac17ff20cb96947169b9918b905669f6194df48, Actions run42391297203.
- Actual tested synthetic merge2e786c635d98156550abf3ffbf9066f5f7a91d11 has treef881cc9562d9005e028210bb8735c76e9592244e, identical to source/reviewed tree.
- API26/35 SAF fixture: fresh tree grant, persistence, file create/write/read and cleanup passed. This is the debug fixture, not the app settings page.
- JVM migration suite20 cases,0failures/errors/skips; on-device seeded migration4 passed on26/35.
- Same-install seed/verify MMKV and filesDir JUnit checks passed. Previous version/Room Kotlin assertions did not establish always-on checking. The proposed shared JUnit oracles and explicit negative-control stage repair that evidence gap; new runtime proof remains required.
- Independently verified downloaded candidate and delivered baseline APK signatures with Android apksig8.9.2: same certificate88774e5211b61f7827db0ba6991a668000ceab9a9e9490648054b8a19b6e070f, verification successful/no errors.

## Exact artifact identities

| Item | Artifact | SHA256 | Bytes |
|---|---|---|---|
| Actual delivered alpha6 arm64 | https://github.com/frpboy/Seal/actions/runs/42337373247/artifacts/13560173491 | 2da1e4d2f7e388133aef0e6b6cc21cc3ac27b16aa3727b860a21440f4fc15030 | 42625813 |
| Qualified candidate arm64 | https://github.com/frpboy/Seal/actions/runs/42391297203/artifacts/13582067231 | d821a59953575174b06d8c356674950329c2ecf1ab025e1fd9f6ca146d8f840d | 66079247 |

Both contain ONLY arm64-v8a libraries. Existing x86_64 emulator workflow cannot qualify installation of these exact bytes. A same-run universal sibling is a separately labeled surrogate, not the delivered artifact. Do not silently substitute it.

## Dedicated test build proposal

`-PsealiContinuityReleaseTests=true` sets testBuildType=release and compiles only src/continuityTest/java. Default debug testing includes that shared source plus all existing debug tests. This avoids release-test compilation of debug-only SAF host fixtures. It does not build/sign/execute a test APK by itself.

Before privileged execution: prove the selected AGP release test task exists and compiles; confirm release minification/mapping compatibility with the OLD baseline (a test compiled against the candidate must not be assumed compatible with old obfuscated classes). Verify generated instrumentation targetPackage and test signer. The current upgrade-continuity.yml remains obsolete alpha5-only and must NOT be dispatched as a delivered-alpha6 gate.

## Exact delivered-alpha6 install-over execution plan, still held

1. Obtain a supported isolated ARM64 Android runtime. Do not use the owner's phone as a test fixture, purchase capacity, or claim an unverified hosted runner supports ARM64 emulation.
2. Verify pinned baseline/candidate file hashes, certificate equality, package identity, ABI and non-downgrade versionCode. Preserve the full source SHA/test-APK hash too.
3. Use an empty isolated runtime with the proper ABI. Install exact baseline. Install a compatible same-signer continuity test APK; execute explicit oracle negative-control and seed, require exact successful counts/failure-marker absence.
4. Install exact candidate with `adb install -r`, never uninstall/reset/clear app data. If install fails, stop and preserve error evidence, do not bypass downgrade/signature checks.
5. Execute verify with always-on JUnit checks for MMKV/files/Room/version, require exact counts and no errors/skips. Verify app launch/no crash, then process stop/restart/reopen and verify again if that additional scope has been approved.
6. Preserve install output, both package identities/cert hashes, runtime ABI, source/test hashes and test results in a single source-bound artifact. This is a gate proposal, not an execution report.

## App settings and lifecycle follow-on, separate from signed continuity

- Render actual DownloadDirectoryPreferences with real ActivityResult registration, select the SDCARD row, then drive the already-observed DocumentsUI confirmation flow. Verify SDCARD_URI equals the returned persisted URI, then real create/write/read and cleanup. Preserve/restore affected synthetic prefs and release only a new grant.
- Recreate while picker is pending and return consent afterward. Source currently stores editingDirectory with `remember`, default VIDEO; lost SDCARD selection is a concrete hypothesis to test, not an asserted product defect. Do not patch production state handling without reproducing/reviewing.
- Keep host-fixture recreation/late-callback behavior distinct from app settings recreation. The fixture's current per-instance latch is not evidence for either.
- Screenshot the actual settings page before/after grant. Keyboard-hidden, landscape, full contrast/accessibility, and real owner-phone behavior remain outside current screenshot proof.

## Other release holds

Unknown custom-binary convergence policy acknowledgement is separate from internal design review. FFmpeg security residuals, complete release checklist, reviewed merge/release words, and owner publishing authority are not waived by green qualification CI. No main merge, public release, Play upload, or phone nomination follows automatically.

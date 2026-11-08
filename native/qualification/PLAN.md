# Seali unsigned native qualification proposal

LOCAL ONLY. No push, dispatch, build, signing or release has been authorized by this packet.

## Exact base and preserved work

Remote candidate base: e30bf3bcd4240618dc08f25d3446a0b2339d6f1e.
Main: aed0630a2e59816429bdc952f22bd41d17353c8c. Candidate is not merged.

This separate worktree copies the reviewed clean native integration as a dependency. Original staged worktree is unchanged. Recipe, licenses and checker stay byte-identical. The earlier two-worker recipe patch is NOT adopted here. Four native compiler workers are appropriate only to the proposed standard hosted profile, not approval for another local attempt.

## Minimal new scope

- Opt-in `nativeQualification` build type copies debug settings but removes signing, adds `.nativequalification` identity and a diagnostic version suffix. It does not touch the guarded release tasks. Default builds do not create this type.
- Actual generic nativeQualification compile/runtime configurations must select exact reviewed transformed AAR SHA 6755c855f9e86a942b6819374093d9cee6265fed1723e7c326f3969f68b36af3, original Maven identity and same component set as the untransformed attribute baseline. This is not a claim that every dependency is independently audited.
- Full universal APK includes ARM64, ARMv7, x86_64 and x86. Existing reviewed ZIP/symlink/budget walker is reused. New adapter inventories ELF32 and ELF64, validates ABI/class/machine, and gates 64-bit PT_LOAD constraints. ELF32 is inventory only, not a 16KiB Android claim. Unsupported native locations fail closed.
- Final APK hash/size, native entry hashes, manifest metadata, no-signature inventory and SDK zipalign output are runner receipts. No APK/AAB/raw-log upload. Maximum aggregate receipt payload 8MiB, explicit log-only allowlist. Base64 records include filename, size and SHA-256 so downloaded ordinary workflow logs can reconstruct exact receipt bytes. No artifact/cache upload or job summary is used. Log retention follows existing repository policy, not a seven-day artifact promise.
- Manual-only workflow; contents:read; no signing keys, repository secrets, release/tag, deployment, provider or database operation. Checkout credentials not persisted. One run attempt required, exact dispatch SHA validated against actual checkout. No rerun authority.

## Before any external effect

Independent review must clear the complete base-relative patch, including new build type, copied reviewed integration and workflow. Re-read register and live main/candidate/source tree and all other triggers. Use the already registered manual workflow path `.github/workflows/android.yml` (live workflow407006274, active on default main) rather than registering a new path through a main push. This packet REPLACES its old manual release-build contents on the proposed candidate ref only. Main remains unchanged. No dispatch or publication is approved by this packet. Exact approved branch publication strategy, source pin and trigger inventory remain an external-execution gate.

Check actual account/public visibility/standard-host eligibility and all no-money budgets. `ubuntu-24.04` is proposed, not a tested execution-host claim. Standard runner preflight requires >=6,000,000KiB total RAM, >=2 CPUs and >=12,000,000KiB free disk; failure means stop, not tuning/retry. Job ceiling45min, selection20min + packaging20min maximum with TERM/KILL grace. Native transform has its existing15min child ceiling. Only process-group deadlines are enforced. Detached-descendant cleanup is UNKNOWN even on exit0; no claim that no Gradle process survives. A timeout is a failed phase with UNKNOWN cleanup, no retry. The disposable hosted job45min ceiling is the outer boundary, not a cleanup proof. Normal completion/failure writes stage/exit and job-status receipts; runner loss, forced cancellation or overall timeout may prevent the final receipt. Such an outcome is UNKNOWN until owning workflow logs/state establish it, never success. A separate bounded monitoring/checkpoint will be required when a run is approved.

A clean hosted transform necessarily rebuilds from checksum-pinned source as part of qualifying actual AGP use on that host; it is not another gratuitous repeatability run or another source-review loop. There is no prebuilt native binary committed or publicly uploaded by this packet.

## Evidence and gates

Executed now: six NEW small synthetic adapter fixtures (ARM64 pass, x64 4KiB fail, ARM32 inventory, x86 inventory, ABI mismatch reject, truncated reject). Workflow YAML/manual-only structure and each shell run-block syntax checked. No Gradle invocation, compiler or application build executed. Existing21 checker tests and native rebuild tests were NOT repeated.

Open even after this proposed job succeeds: Android installation/launch, actual16KiB page-size confirmation, FFmpeg/WebP operations, onboarding/share/state/UI/text-scale/accessibility, signed release packaging, AAB, signature continuity, private installer delivery and Play qualification. This unsigned debug-derived diagnostic is NOT the final production release APK and does not close those gates. GPL/corresponding-source and WebP COPYING/PATENTS/AUTHORS obligations and alpha labels remain.

## Review corrections

Named terminal handler emits Python JSON and preserves phase exit status, with tiny executed stage/exit fixtures. Packaged metadata now gates source-derived default version plus diagnostic suffix and existing first-ABI code adjustment(+2), package ID, debuggable status, single universal output/no filters, metadata filename and hash tied to the actually inspected APK. All three remaining action refs pinned to commits verified via their owning GitHub commit endpoints; action-binding receipts accompany the packet. This is pin provenance, not an audit of every action implementation.

## Log-only amendment

The artifact-upload step is removed to avoid metered artifact storage. Ordinary workflow logs retain a bounded, encoded allowlist only, never APK/AAB bytes. A missing/truncated envelope is incomplete evidence, not success. Overall runner loss or timeout may prevent log receipt emission. No new build or runtime test is claimed by this source amendment.

## Checksum-pinned private CMake provenance

Historical run42455567604 failed before native build because /usr/bin/cmake
was missing. Run42455997256 stopped before sudo/Gradle because the preinstalled
/usr/local/bin/cmake failed the ownership/nonwritable guard. Both stay failed.
The former adapter is removed, not weakened: no host CMake executable is used,
no root/sudo/system symlink/permission change or image-version assertion remains.

The revised recipe adds official CMake3.22.1 Linux x86_64 archive SHA256
73565c72355c6652e9db149249af36bcab44d9d478c5546fd926e69ad6b43640,
verified from https://cmake.org/files/v3.22/cmake-3.22.1-SHA-256.txt.
It downloads and checksum-checks before extraction/execution, under existing
fresh private workdir/controlled environment/timeouts. CMake is invoked by its
exact extracted absolute path for configure and build, never PATH lookup.
Version and archive/executable hashes enter build-tool-versions.txt.
This is CHANGED recipe and binary provenance. The earlier two local builds
reported CMake3.22.1 but used the distribution package, not this archive.
No cross-toolchain reproducibility is presumed. The transformed AAR expected
SHA remains6755c855...; differing outputs fail closed rather than updating it.
No APK/AAB upload, cache, deployment, signing or paid runner is added. All final
packaging/runtime gates remain; full source review precedes another bounded run.

## Narrow 16KiB runtime extension

Rebuild is necessary for a new runtime unit because prior unsigned APK was
intentionally runner-local, not retained. Existing packaging gates run first.
Stable SDK image system-images;android-35;google_apis_ps16k;x86_64 revision5
was observed in official SDK repository metadata. Boot must report PAGE_SIZE
16384 and x86_64; documented page-size compat properties disabled/read back.
No sudo/KVM permission changes: inaccessible acceleration fails immediately.
One clean emulator, boot240s, image install300s, runtime900s, same45minjob bound.
No fallback or retry. A disposable two-day fixture signing certificate signs a
COPY after unsigned packaging gates, not production key or update continuity.
Only appUID FFmpeg lossless WebP16x16 RGB roundtrip and app launch/process
presence are tested. It uses the installed app's actual nativeLibraryDir and
app-initialized codec extraction. No provider/media network/account/userdata.
Screen pixels are emulator fixture only, bounded existing8MiB log-only receipt
transport adds runtime16k.json and runtime16k.png to fixed allowlist. These must
be independently inspected/reconstructed, not treated as UI verification from
XML or exitstatus. No APK/key upload. Historical packaging receipts still say
runtimeNOTEXECUTED as their own stage; runtime16k.json alone records new runtime.
ARM64 devices, download UI codec wiring, broad UI/state/SAF/upgrade/AAB/release
signing/installer custody remain separate gates. Cleanup verifies owned emulator
exit when possible; detached descendants remain UNKNOWN. Forcedoutertermination
can prevent runtime receipt, therefore missingreceipt meansUNKNOWN not success.

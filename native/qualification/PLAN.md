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

## Proposed exact-path host adapter (source only)

Run42455567604 failed in selection before native downloads because the recipe
uses /usr/bin/cmake but image20261101.360.1 supplies /usr/local/bin/cmake3.31.6.
This amendment preserves every native recipe byte. It proposes ONE new symlink
on the disposable hosted VM, using only sudo -n /usr/bin/ln -sT, never force or
replacement, apt installation, permissions changes, security-policy changes,
custom/paid runners, or persistent account infrastructure. Execution needs
explicit approval for this elevated host operation as well as a new bounded run.
Capability is not authority. The image, OS, architecture, tool version, ownership,
permissions, absent target, post-link identity/hash, and all exact recipe tool
paths must pass before Gradle. Any mismatch fails without fallback or retry.
Host adapter evidence is ordinary preflight log output, not a twelfth required
qualification receipt; its local contract file is not in the upload/log allowlist.
The new host's actual ownership/permissions and CMake binary hash remain unknown
until that preflight runs. Different CMake provenance may change output: existing
expected transformed AAR SHA remains unchanged and fails closed on a mismatch.
This proposal neither executes the elevated operation nor qualifies the app.

Host-adapter review correction: reject every lexical symlink in the source path
and all parents, including cycles, before any version execution. Only the exact
new target link is allowed separately; its parent chain and root ownership are
checked. This fail-closed policy may reject a future image whose source path
contains symlinks; it must not be weakened automatically. Small temporary-file
fixtures mock ownership/platform version only, never invoke CMake or sudo.

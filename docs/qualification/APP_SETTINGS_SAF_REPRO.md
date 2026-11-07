# Actual settings SAF and pending-result reproduction proposal

Source plus narrow CI wiring proposal only. Not executed, not production fixes. Prior default-debug oracle run42393535947 settled green at reviewed tree9a1ce08c, independent of this proposed new job.

## Scope and source observations

`DownloadDirectoryPreferences` stores `editingDirectory` with plain `remember`, initially VIDEO. The result callback routes through `App.updateDownloadDir(uri, editingDirectory)`. Loss of the pending SDCARD selection during recreation is a hypothesis, not yet a reproduced product defect.

The same callback contains `if (editingDirectory != Directory.SDCARD)` around a `when` that includes `Directory.SDCARD -> sdcardUri = uri.toString()`. That inner SDCARD branch cannot execute under that condition. The fresh-flow test records backend persistence and real I/O before checking the visible description, so a stale description is not confused with missing persistence. No runtime failure is claimed from this source observation alone.

## Proposed test-only changes

- Debug-only nonexported `DirectorySettingsTestActivity` renders the actual settings composable in its own onCreate on every generation. No replacement callback, URI persistence shim or injected ActivityResultRegistry.
- `actualSdcardSettingsPersistsFreshGrant` clicks the actual SDCARD row and drives the real DocumentsUI labels already observed on API26/35. Requires SDCARD_URI, a NEW persisted read/write grant, unchanged audio/video state, actual create/write/read and deletion, then visible URI description.
- `pendingSdcardResultRoutesAfterRecreation` repeats the real flow but requests host recreation with the picker still open. Requires a new host onCreate record BEFORE selection and a focused, active DocumentsUI application window plus matching UiDevice currentPackageName immediately before root selection and folder confirmation. Package-node presence alone is not accepted. Return requires the intended settings host RESUMED with window focus. Failures of those predicates are harness failures, not routing evidence. If that cannot be established, it fails with a harness-specific message and makes no routing claim. Only then does it select the tree and apply the same app/persistence/I/O oracles. This is a real late result, not a synthetic registry dispatch.
- Both methods snapshot/restore affected MMKV values with checked encode/round-trip or checked removal, in-memory audio/video destinations while API26 storage runtime permission is runner-owned. Cleanup closes the scenario, removes only its synthetic file and releases only newly acquired grants, then restores state; runtime permission revoke is runner-side after BOTH instrumentation invocations and evidence capture, never inside the target process; original stage exit status is preserved. New-grant enumeration is permitted only in an EXCLUSIVE disposable emulator/profile with no concurrent grant consumer. The test requires explicit `-e sealiDisposableProfile true`; that flag alone does not prove isolation. The runner must provide it. Current failure hierarchy, nonempty screenshot, focused-window/outcome/lifecycle evidence is captured before cleanup on error. Diagnostic and cleanup failures are suppressed on the original failure, never converted to a pass. Screenshots require nonempty files and recycle their Bitmap.
- Both release-family exclusion gates include the new host's declaration/class descriptor, including pre-R8 merged manifests. Debug presence checks require BOTH hosts. No release source-set change.

## Why not ActivityScenario.recreate()

AndroidX ActivityScenario.recreate() first moves the activity to RESUMED, then recreates, then restores its prior state. That can pull the host ahead of an open picker and invalidate a pending-result reproduction. This proposal instead calls Activity.recreate() through scenario.onActivity and requires observable recreation while DocumentsUI remains foreground. Whether that works on each platform is deliberately an execution gate.

Source inspected: https://raw.githubusercontent.com/android/android-test/main/core/java/androidx/test/core/app/ActivityScenario.java (recreate method). This unpinned upstream source explains the harness choice; the actual resolved AndroidX dependency behavior still needs execution on the candidate.

## Concrete isolation and CI wiring proposal, execution held for review

A separate `settings-saf` matrix job on fresh `ubuntu-latest` VM per API launches one uniquely named AVD with force creation, `-wipe-data -no-snapshot`, no concurrent test worker. No device service or owner phone is attached. Before install the runner requires exactly one `emulator-*` serial, ro.kernel.qemu=1 and NO preexisting target package; it fails rather than uninstalling/clearing an existing package. The disposable-profile argument is set only AFTER these checks, not used as the isolation evidence itself. Record serial, VM job/run, fingerprint, current user and checkout.

The new script runs fresh1 and recreated1 separately, always collects CURRENT failure artifacts and pixels from filesDir tar plus lifecycle/windows/activity records after EACH invocation, and preserves a failure from either stage. Existing history-ui17/oracle1/seed1/verify1/migration4 and JVM suites are untouched. Existing normal build release-family scans cover both hosts; release instrumentation opt-in is not used. New job has no signing secrets or release task.

On API26 the runner grants storage before instrumentation and revokes it in an EXIT trap after evidence capture, checking dumpsys from outside the target process. A revoke that kills the target cannot hide the already recorded original JUnit failure. No in-process postrevoke check or product inference.

## Execution remains held for review

1. Compile debug app and instrumentation normally. Do not use release-continuity opt-in or signing changes. Check debug manifest/DEX includes both hosts; normal release-family exclusion checks must remain green.
2. On a clean EXCLUSIVE disposable API26 and API35 emulator/profile (not an owner/shared device), with `-e sealiDisposableProfile true`, run each method separately, expecting exactly `OK (1 test)` per invocation. Retain the existing 17 + oracle1 + seed1 + verify1 + migration4 stages unchanged if subsequently wiring this into CI. Do not fold new tests into 17 or mask a red result.
3. Capture `settings-host-lifecycle.txt`, `settings-fresh-*` and `settings-recreated-*` files via run-as. XML and screenshots are failure evidence too. Inspect actual pixels before reporting any visible UI clearance.
4. A failure must be categorized from its stage: picker selection, pending recreation harness, destination routing, persistence/I/O, or stale visible description. Do not patch production behavior before runtime reproduction and independent review.

No Android SDK/emulator execution or AGP compilation was performed during this source-preparation step. No test success is asserted. A local source diff, Python guard syntax check and reviewable patch do not replace device results. Owner-phone use, paid capacity, production changes, actual signed alpha6 install-over, merge and release remain outside this proposal.

## Two-phase one-shot source guard, no writes approved by this document

Phase1 stages reviewed source/scripts plus `settings-saf if:false`, all commits `[skip ci]`. No settings emulator jobs execute. Fresh-read exact resulting staging SHA and tree and recover current owner scope before any next write. Independent review is required before phase1 and again for the actual phase2 workflow-only diff.

`prepare-settings-saf-phase2.py VERIFIED_STAGING_SHA OUTPUT_PATH` creates a local proposal, never edits/pushes the live workflow. Phase2 matches only PR25 `synchronize` from the same repo/headbranch, `event.before == exactStagingSHA`, first run attempt. Checkout for that job pins exactStagingSHA. Missing `before` evaluates unequal and skips the job. Future pushes/reopen/manual/main/tag events cannot match the one transition; no privileged dispatch. Actual orchestration GITHUB_SHA and source checkout HEAD are recorded separately, along with event.before/after. Source pin is not a self-referential unknown final commit.

GitHub's current webhook contract explicitly lists synchronize `before` and `after` as required: https://docs.github.com/en/webhooks/webhook-events-and-payloads . Actual payload fields are captured to prove the event on execution, not assumed from the source guard.

Before upload `inspect-settings-evidence.py` opens each stage tar, copies only named settings/lifecycle evidence, records byte sizes/SHA256 and requires stage method provenance. Success and failure have different named requirements; missing CURRENT failure diagnostics produce a separate evidence error while original JUnit result stays intact. Raw product failure cannot become green or disappear due to a missing screenshot. Tar nonempty alone is never completeness proof. Artifact retention1day, cap20MiB per API enforced by a separate workflow ALWAYS finalizer, including early shell/build/device failures. Upload path is ONLY settings-upload/, never raw settings-evidence/. Finalizer fixed-name/type allowlists regular files, rejects symlinks, limits each text64KiB/image2MiB, records truncation/omission explicitly INCOMPLETE, preserves original method markers separately, then performs a second actual on-disk total/type check. Sanitizer failure blocks upload. Oversize evidence is never called complete. New job cache is read-only. Release guards are only source changes until actual normal build scans execute.

## Live cost check, November7 22:41 IST, recheck before final execution

https://github.com/frpboy/Seal is currently PUBLIC (live repository API). Standard ubuntu-latest is free/unlimited for public repos per current GitHub runner docs: https://docs.github.com/en/actions/how-tos/write-workflows/choose-where-workflows-run/choose-the-runner-for-a-job . No larger runner or paid ARM64 capacity.

Authenticated https://github.com/settings/billing Actions panel: GitHub Free, billable Actions usage $0, $76.21 consumed offset by $76.21 discounts, included compute $12/$12 already used, Actions storage displayed0GB/0.5GB. The public-repo free compute rule matters because included private compute is exhausted. New evidence is capped40MiB total (20MiB per API), one-day retention, no new cache writes; displayed storage is rounded, so recheck before execution rather than treating0GB as exact zero. Existing unrelated account usage is not permission to spend. Current billing policy: https://docs.github.com/en/billing/concepts/product-billing/github-actions . No billing/budget/account setting was changed.

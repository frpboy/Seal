#!/usr/bin/env bash
# Dedicated fresh hosted-VM job, exclusive wipe-data/no-snapshot AVD. No
# parallel tests/workers on this device. Never accept a phone/shared profile.
set -euo pipefail
mkdir -p settings-evidence
out=settings-evidence
exec > >(tee "$out/runner.log") 2>&1
printf 'checkout=%s orchestration_sha=%s run=%s attempt=%s\n' "$(git rev-parse HEAD)" "$GITHUB_SHA" "$GITHUB_RUN_ID" "$GITHUB_RUN_ATTEMPT" | tee "$out/identity.txt"
# Preserve actual event.before/after for validation, without unrelated payload.
python3 - <<'PY_EVENT' >> "$out/identity.txt"
import json,os
x=json.load(open(os.environ['GITHUB_EVENT_PATH']))
print('event='+os.environ['GITHUB_EVENT_NAME']+' action='+str(x.get('action'))+' before='+str(x.get('before'))+' after='+str(x.get('after')))
PY_EVENT
mapfile -t devices < <(adb devices | awk 'NR>1 && $2=="device" {print $1}')
[[ ${#devices[@]} == 1 && ${devices[0]} == emulator-* ]] || { echo 'HARNESS: not one exclusive emulator'; exit 1; }
export ANDROID_SERIAL=${devices[0]}
[[ $(adb shell getprop ro.kernel.qemu | tr -d '\r') == 1 ]] || { echo 'HARNESS: emulator proof missing'; exit 1; }
# No preexisting app/data install on this job's wiped AVD. Do not clear/uninstall
# a preexisting package to manufacture this proof: fail instead.
[[ -z $(adb shell pm path com.frpboy.seali.debug | tr -d '\r') ]] || { echo 'HARNESS: target already installed'; exit 1; }
{
  echo "serial=$ANDROID_SERIAL exclusive_job=$GITHUB_JOB run=$GITHUB_RUN_ID"
  adb shell getprop ro.build.fingerprint
  adb shell getprop ro.kernel.qemu
  adb shell am get-current-user
} >> "$out/identity.txt"
chmod +x gradlew
# Capture ACTUAL selected runtime dependency before building. Metadata in a
# version catalog alone does not establish resolved dependency selection.
./gradlew --console=plain :app:dependencyInsight --configuration genericDebugRuntimeClasspath --dependency androidx.core:core > "$out/core-runtime-resolution.txt"
head -n 100 "$out/core-runtime-resolution.txt"
grep -Fx 'androidx.core:core:1.15.0' "$out/core-runtime-resolution.txt"
./gradlew --console=plain :app:dependencyInsight --configuration genericDebugAndroidTestRuntimeClasspath --dependency androidx.compose.ui:ui-test-android > "$out/compose-runtime-resolution.txt"
head -n 100 "$out/compose-runtime-resolution.txt"
grep -Fx 'androidx.compose.ui:ui-test-android:1.8.0-rc02' "$out/compose-runtime-resolution.txt"
./gradlew assembleGenericDebug assembleGenericDebugAndroidTest
find app/build/outputs/apk/generic/debug -name '*x86_64*.apk' -print -quit | xargs adb install
python3 .github/scripts/check-debug-host-present.py
find app/build/outputs/apk/androidTest/generic/debug -name '*.apk' -print -quit | xargs adb install
api=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
permission_owned=0
# Runtime permission is runner-owned; never revoke inside the instrumented
# process, because revocation may kill it and hide the original JUnit failure.
if (( api <= 29 )); then
  adb shell pm grant com.frpboy.seali.debug android.permission.WRITE_EXTERNAL_STORAGE
  permission_owned=1
fi
cleanup_permission() {
  local original=$? cleanup=0
  trap - EXIT
  if (( permission_owned )); then
    adb shell pm revoke com.frpboy.seali.debug android.permission.WRITE_EXTERNAL_STORAGE || cleanup=1
    adb shell dumpsys package com.frpboy.seali.debug > "$out/permission-after-revoke.txt" || cleanup=1
    if grep -Eq 'android.permission.WRITE_EXTERNAL_STORAGE: granted=true' "$out/permission-after-revoke.txt"; then cleanup=1; fi
  fi
  printf 'original_exit=%s permission_cleanup_failure=%s\n' "$original" "$cleanup" > "$out/runner-final.txt"
  if (( original != 0 )); then exit "$original"; else exit "$cleanup"; fi
}
trap cleanup_permission EXIT
failed=0
: > "$out/summary.txt"
runner=com.frpboy.seali.debug.test/androidx.test.runner.AndroidJUnitRunner
for spec in 'fresh actualSdcardSettingsPersistsFreshGrant' 'recreated pendingSdcardResultRoutesAfterRecreation'; do
  read -r label method <<< "$spec"
  rc=0
  adb shell am instrument -w -r -e sealiDisposableProfile true -e class "com.junkfood.seal.DirectorySettingsSafTest#$method" "$runner" | tr -d '\r' | tee "$out/$label-result.txt" || rc=$?
  # Fresh and recreation always kept as independent1-test stages. A fresh
  # visible-description failure must not suppress pending-result evidence.
  if (( rc != 0 )) || ! grep -Fx 'OK (1 test)' "$out/$label-result.txt" || grep -Eq 'FAILURES|INSTRUMENTATION_FAILED|shortMsg=|INSTRUMENTATION_ABORTED|INSTRUMENTATION_STATUS_CODE: -[234]' "$out/$label-result.txt"; then
    echo "stage=$label FAILED transport_rc=$rc" | tee -a "$out/summary.txt"; failed=1
  else
    echo "stage=$label PASSED transport_rc=$rc" | tee -a "$out/summary.txt"
  fi
  # Collect evidence after EACH invocation, before the next may replace
  # lifecycle data; a full tar includes failure-time artifacts when present.
  if ! adb exec-out run-as com.frpboy.seali.debug tar -cf - files > "$out/$label-files.tar" || ! test -s "$out/$label-files.tar"; then
    echo "stage=$label evidence FAILED" | tee -a "$out/summary.txt"; failed=1
  fi
  if ! adb exec-out run-as com.frpboy.seali.debug cat files/settings-host-lifecycle.txt > "$out/$label-lifecycle.txt"; then failed=1; fi
  adb shell dumpsys activity activities > "$out/$label-activities.txt" || failed=1
  adb shell dumpsys window windows > "$out/$label-windows.txt" || failed=1
done
# Missing diagnostics are a separate evidence failure, never overwrite original
# method results or suppress uploading evidence of an expected product failure.
python3 .github/scripts/inspect-settings-evidence.py || failed=1
# The workflow's ALWAYS sanitizer handles all paths, including an earlier
# shell/build/device failure, and uploads only its bounded allowlisted output.
exit "$failed"

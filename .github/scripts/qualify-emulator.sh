#!/usr/bin/env bash
# One checkout/head, independent evidence stages, fail closed at the end.
set -euo pipefail
printf 'qualification_head=%s\n' "$(git rev-parse HEAD)" | tee qualification-head.txt
chmod +x gradlew
./gradlew assembleGenericDebug assembleGenericDebugAndroidTest
find app/build/outputs/apk/generic/debug -name '*x86_64*.apk' -print -quit | xargs adb install -r
python3 .github/scripts/check-debug-host-present.py
find app/build/outputs/apk/androidTest/generic/debug -name '*.apk' -print -quit | xargs adb install -r
failed=0
runner=com.frpboy.seali.debug.test/androidx.test.runner.AndroidJUnitRunner
run_stage() {
  local label="$1" classes="$2" expected="$3" output="$4" rc=0
  adb shell am instrument -w -r -e class "$classes" "$runner" | tr -d '\r' | tee "$output" || rc=$?
  if (( rc != 0 )) || ! grep -Fx "$expected" "$output" || grep -Eq 'FAILURES|INSTRUMENTATION_FAILED|shortMsg=|INSTRUMENTATION_ABORTED' "$output"; then
    echo "stage=$label FAILED transport_rc=$rc" | tee -a qualification-summary.txt
    failed=1
  else
    echo "stage=$label PASSED" | tee -a qualification-summary.txt
  fi
}
: > qualification-summary.txt
run_stage primary com.junkfood.seal.ClassicHomeHistoryTest,com.junkfood.seal.DependencyAndroidCompatibilityTest,com.junkfood.seal.ClassicHomeControlsTest,com.junkfood.seal.ClassicHomeRecreateTest,com.junkfood.seal.SafPermissionEmulatorTest 'OK (17 tests)' instrumentation-result.txt
# One additional explicit negative-control invocation checks the SAME always-on
# oracles used by verification. Keep 17+1+1+4 and oracle1 counts separate.
run_stage oracle 'com.junkfood.seal.UpgradeContinuityTest#negativeControlsRejectMissingState' 'OK (1 test)' uc-oracle-result.txt
# Ordered same-install persistence, NOT the signed old-to-new upgrade gate.
run_stage seed 'com.junkfood.seal.UpgradeContinuityTest#seedUpgradeState' 'OK (1 test)' uc-seed-result.txt
# Fail closed if seed failed; still collect verification as separate evidence.
run_stage verify 'com.junkfood.seal.UpgradeContinuityTest#verifyUpgradeStatePreserved' 'OK (1 test)' uc-verify-result.txt
run_stage migration com.junkfood.seal.YtDlpLegacyUpgradeEmulatorTest 'OK (4 tests)' ytdlp-migration-result.txt
for name in classic-history.png classic-controls-normal.png classic-controls-font2.png classic-controls-normal-ime.png classic-controls-font2-ime.png classic-recreate.png bundled-ytdlp-version.txt; do
  if ! adb exec-out run-as com.frpboy.seali.debug cat "files/$name" > "$name" || ! test -s "$name"; then
    echo "evidence=$name FAILED" | tee -a qualification-summary.txt
    failed=1
  fi
done
# Identity is mandatory even when the picker assertion fails. Full hierarchy
# is failure-only; absence on a green picker run is expected, not a new failure.
if ! adb exec-out run-as com.frpboy.seali.debug cat files/saf-host-identity.txt > saf-host-identity.txt || ! test -s saf-host-identity.txt; then
  echo 'evidence=saf-host-identity FAILED' | tee -a qualification-summary.txt
  failed=1
fi
if adb shell run-as com.frpboy.seali.debug test -s files/saf-picker-hierarchy.xml; then
  if ! adb exec-out run-as com.frpboy.seali.debug cat files/saf-picker-hierarchy.xml > saf-picker-hierarchy.xml || ! test -s saf-picker-hierarchy.xml; then
    echo 'evidence=saf-picker-hierarchy FAILED' | tee -a qualification-summary.txt
    failed=1
  fi
fi
exit "$failed"

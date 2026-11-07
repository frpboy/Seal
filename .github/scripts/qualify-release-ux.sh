#!/usr/bin/env bash
set -euo pipefail
collect_saved_images() {
  local image
  for image in onboarding-step-1 onboarding-step-2-recreated onboarding-step-3 onboarding-cold-share onboarding-warm-share onboarding-input-sheet-deferred onboarding-after-sheet-hidden onboarding-about-debug-update-off onboarding-manual-update onboarding-settings-back onboarding-landscape-font15; do
    adb exec-out run-as com.frpboy.seali.debug cat "files/$image.png" > "$image.png" || rm -f "$image.png"
  done
}
# Capture partial evidence before android-emulator-runner tears down the AVD.
trap collect_saved_images EXIT
# Disposable emulator only. Never run this root/fence script on a user's phone.
[[ "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" == 1 ]]
adb root
adb wait-for-device
uid="$(adb shell cmd package list packages -U com.frpboy.seali.debug | tr -d '\r' | sed -nE 's/^package:com.frpboy.seali.debug uid:([0-9]+)$/\1/p')"
[[ "$uid" =~ ^[0-9]+$ ]]
printf 'synthetic_target_uid=%s\n' "$uid" > ux-egress-fence.txt
for fw in iptables ip6tables; do
  adb shell "$fw" -I OUTPUT -m owner --uid-owner "$uid" -j REJECT
  adb shell "$fw" -C OUTPUT -m owner --uid-owner "$uid" -j REJECT
  printf '%s targetUID OUTPUT REJECT present\n' "$fw" >> ux-egress-fence.txt
done
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell settings put system font_scale 1.0
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
for setting in window_animation_scale transition_animation_scale animator_duration_scale; do
  [[ "$(adb shell settings get global "$setting" | tr -d '\r')" =~ ^0(\.0)?$ ]]
  printf '%s=0 verified\n' "$setting" >> ux-egress-fence.txt
done
runner=com.frpboy.seali.debug.test/androidx.test.runner.AndroidJUnitRunner
run_one() {
  local method="$1" output="ux-$1.txt"
  adb shell am instrument -w -r -e class "com.junkfood.seal.OnboardingSmokeTest#$method" "$runner" | tr -d '\r' | tee "$output"
  grep -Fx 'OK (1 test)' "$output"
  ! grep -Eq 'FAILURES|INSTRUMENTATION_FAILED|shortMsg=|INSTRUMENTATION_ABORTED' "$output"
}
for method in stepsSurviveRecreationAndFinishPersists skipPersistsAcrossRelaunch coldAndWarmShareDeferOrientation expandedInputSheetDefersUntilHidden navigationBackAndManualUpdaterWithMotionOff; do
  run_one "$method"
done
adb shell settings put system font_scale 1.5
run_one largeFontLandscapeKeepsActionsReachable
adb shell settings put system font_scale 1.0
for image in onboarding-step-1 onboarding-step-2-recreated onboarding-step-3 onboarding-cold-share onboarding-warm-share onboarding-input-sheet-deferred onboarding-after-sheet-hidden onboarding-about-debug-update-off onboarding-manual-update onboarding-settings-back onboarding-landscape-font15; do
  adb exec-out run-as com.frpboy.seali.debug cat "files/$image.png" > "$image.png"
  test -s "$image.png"
done
for fw in iptables ip6tables; do
  adb shell "$fw" -C OUTPUT -m owner --uid-owner "$uid" -j REJECT
  printf '%s targetUID OUTPUT REJECT remained present\n' "$fw" >> ux-egress-fence.txt
done

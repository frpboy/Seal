#!/usr/bin/env bash
set -euo pipefail
# Only fixed enums may leave a failed run. Never echo commands, paths or raw logs.
stage=preflight
failure_class=unknown
report_failure() {
  result=$?
  if [ "$result" -ne 0 ]; then
    case "$stage" in
      settings) failure_class=settings ;;
      instrumentation)
        failure_class=instrumentation_launch_or_process
        if grep -s -Fqx 'SEALI_BRANDING_STAGE=assertions' "$RUNNER_TEMP/seali-branding-instrumentation.txt"; then
          failure_class=test_assertion_or_process
        elif grep -s -Fqx 'SEALI_BRANDING_STAGE=native_init' "$RUNNER_TEMP/seali-branding-instrumentation.txt"; then
          failure_class=native_init_gate_or_process
        fi
        ;;
      postvalidation) failure_class=postvalidation ;;
      preflight|build|install) failure_class=$stage ;;
      *) stage=unknown; failure_class=unknown ;;
    esac
    printf 'SEALI_BRANDING_DIAGNOSTIC_V1 stage=%s failure_class=%s\n' "$stage" "$failure_class"
  fi
}
trap report_failure EXIT
git diff --exit-code -- .
chmod +x gradlew
stage=build
./gradlew assembleGenericDebug assembleGenericDebugAndroidTest
stage=install
find app/build/outputs/apk/generic/debug -name '*x86_64*.apk' -print -quit | xargs adb install -r
find app/build/outputs/apk/androidTest/generic/debug -name '*.apk' -print -quit | xargs adb install -r
stage=settings
adb shell wm size 720x1280
adb shell wm density 320
adb shell settings put system font_scale 1.0
adb shell svc wifi disable
adb shell svc data disable
adb shell settings put system system_locales en-US
stage=instrumentation
adb shell am instrument -w -r -e class com.junkfood.seal.BrandingPagesCaptureTest com.frpboy.seali.debug.test/androidx.test.runner.AndroidJUnitRunner > "$RUNNER_TEMP/seali-branding-instrumentation.txt" 2>&1
grep -Fq 'OK (1 test)' "$RUNNER_TEMP/seali-branding-instrumentation.txt"
! grep -Eq 'FAILURES|INSTRUMENTATION_FAILED|shortMsg=' "$RUNNER_TEMP/seali-branding-instrumentation.txt"
stage=postvalidation
mkdir -m 700 "$RUNNER_TEMP/seali-branding-capture"
for name in about-top about-credits credits language troubleshooting; do
  adb exec-out run-as com.frpboy.seali.debug cat "files/branding-capture/$name.png" > "$RUNNER_TEMP/seali-branding-capture/$name.png"
done
/usr/bin/python3 tools/branding-capture/encrypt.py

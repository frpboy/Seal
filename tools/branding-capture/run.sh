#!/usr/bin/env bash
set -euo pipefail
git diff --exit-code -- .
chmod +x gradlew
./gradlew assembleGenericDebug assembleGenericDebugAndroidTest
find app/build/outputs/apk/generic/debug -name '*x86_64*.apk' -print -quit | xargs adb install -r
find app/build/outputs/apk/androidTest/generic/debug -name '*.apk' -print -quit | xargs adb install -r
adb shell wm size 720x1280
adb shell wm density 320
adb shell settings put system font_scale 1.0
adb shell svc wifi disable
adb shell svc data disable
adb shell settings put system system_locales en-US
adb shell am instrument -w -r -e class com.junkfood.seal.BrandingPagesCaptureTest com.frpboy.seali.debug.test/androidx.test.runner.AndroidJUnitRunner > "$RUNNER_TEMP/seali-branding-instrumentation.txt" 2>&1
grep -Fq 'OK (1 test)' "$RUNNER_TEMP/seali-branding-instrumentation.txt"
! grep -Eq 'FAILURES|INSTRUMENTATION_FAILED|shortMsg=' "$RUNNER_TEMP/seali-branding-instrumentation.txt"
mkdir -m 700 "$RUNNER_TEMP/seali-branding-capture"
for name in about-top about-credits credits language troubleshooting; do
  adb exec-out run-as com.frpboy.seali.debug cat "files/branding-capture/$name.png" > "$RUNNER_TEMP/seali-branding-capture/$name.png"
done
/usr/bin/python3 tools/branding-capture/encrypt.py

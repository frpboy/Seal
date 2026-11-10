#!/usr/bin/env bash
set -euo pipefail
mapfile -t apps < <(find app/build/outputs/apk/uiOnly/debug -maxdepth 1 -name '*.apk')
mapfile -t tests < <(find app/build/outputs/apk/androidTest/uiOnly/debug -maxdepth 1 -name '*.apk')
test "${#apps[@]}" -eq 1
test "${#tests[@]}" -eq 1
adb install -r "${apps[0]}"
adb install -r "${tests[0]}"
adb shell am instrument -w -r -e class com.frpboy.seali.UiOnlyPreviewTest com.frpboy.seali.uipreview.test/androidx.test.runner.AndroidJUnitRunner | tee ui-only-instrumentation.txt
grep -F 'OK (1 test)' ui-only-instrumentation.txt
! grep -E 'FAILURES|INSTRUMENTATION_FAILED|shortMsg=|Process crashed' ui-only-instrumentation.txt
mkdir -p ui-only-guest-pixels
for screen in classic v2 v2-completed formats; do
  adb exec-out run-as com.frpboy.seali.uipreview cat "files/ui-preview-$screen.png" > "ui-only-guest-pixels/$screen.png"
done
python3 - <<'PY'
import base64,hashlib,pathlib,struct
for p in sorted(pathlib.Path('ui-only-guest-pixels').glob('*.png')):
 b=p.read_bytes()
 assert 0<len(b)<=524288, 'PNG exceeds reviewed 512KiB cap; never truncate'
 assert b[:8]==b'\x89PNG\r\n\x1a\n'
 w,h=struct.unpack('>II',b[16:24]);assert 0<w<=4096 and 0<h<=4096
 print('UI_PNG_BEGIN',p.name,len(b),hashlib.sha256(b).hexdigest(),w,h)
 text=base64.b64encode(b).decode('ascii')
 for i in range(0,len(text),120):print('UI_PNG_DATA',p.name,i//120,text[i:i+120])
 print('UI_PNG_END',p.name)
print('UI_GUEST_GATE=PASS')
PY

#!/usr/bin/env bash
# Process-group deadline only. Detached descendants are NOT claimed cleaned.
set -euo pipefail
stage=selection
terminal_receipt() {
    code=$?
    trap - EXIT
    python3 - "$stage" "$code" <<'PY'
import json, sys
from pathlib import Path
code = int(sys.argv[2])
Path('qualification-evidence/gradle-terminal.json').write_text(json.dumps({
    'stage': sys.argv[1], 'exit_code': code,
    'cleanup': 'UNKNOWN: no detached-descendant verification',
    'timeout_boundary': 'process group only; disposable hosted job ceiling 45min',
    'retry_authorized': False
}, indent=2)+'\n')
PY
    exit "$code"
}
trap terminal_receipt EXIT
# Owned tiny fixtures invoke this same terminal handler without Gradle.
if [[ "${SEALI_RECEIPT_FIXTURE:-}" == 1 ]]; then
    stage="${SEALI_FIXTURE_STAGE:?}"
    exit "${SEALI_FIXTURE_EXIT:?}"
fi
common=(--no-daemon --no-configuration-cache --max-workers=2 --console=plain '-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g' -Pkotlin.compiler.execution.strategy=in-process -PsealiNativeQualification=true -PnoSplits '-PABI_FILTERS=arm64-v8a;armeabi-v7a;x86_64;x86')
timeout --signal=TERM --kill-after=30s 1200s ./gradlew "${common[@]}" -I native/qualification/selection.init.gradle :app:qualifyNativeResolution
stage=packaging
timeout --signal=TERM --kill-after=30s 1200s ./gradlew "${common[@]}" :app:assembleGenericNativeQualification

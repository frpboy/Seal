#!/usr/bin/env bash
# Post-build evidence only. No strip/patch/package/upload, no target execution.
set -euo pipefail
: "${BUILD_ROOT:?}" "${ANDROID_NDK_HOME:?}"
TC="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin"
[[ -x "$TC/llvm-readelf" && -d "$BUILD_ROOT/stage/usr" ]]
mkdir -p "$BUILD_ROOT/evidence"
[[ -s "$BUILD_ROOT/stage/usr/bin/ffmpeg" && -s "$BUILD_ROOT/stage/usr/bin/ffprobe" ]]
find "$BUILD_ROOT/stage" -type f -print0 | sort -z | xargs -0 sha256sum > "$BUILD_ROOT/evidence/stage-sha256.txt"
while IFS= read -r -d '' f; do
 magic=$(od -An -tx1 -N4 "$f" | tr -d ' \n')
 [[ "$magic" = '7f454c46' ]] || continue
 name=${f#"$BUILD_ROOT/stage/"}; out="$BUILD_ROOT/evidence/${name//\//_}"
 "$TC/llvm-readelf" -h -l -d -V -n --wide "$f" > "$out.elf.txt"
 "$TC/llvm-readelf" --dyn-symbols --wide "$f" > "$out.symbols.txt"
done < <(find "$BUILD_ROOT/stage" -type f -print0)
# Preserve complete local/static relink inputs and all exact generated commands/configs.
find "$BUILD_ROOT" -type f \( -name '*.o' -o -name '*.a' -o -name 'config.log' -o -name 'config.h' -o -name 'config_components.h' -o -name 'config.mak' -o -name 'CMakeCache.txt' -o -name 'link.txt' -o -name 'flags.make' -o -name '*.pc' \) -print0 | sort -z | xargs -0 sha256sum > "$BUILD_ROOT/evidence/relink-input-sha256.txt"
printf '%s\n' 'Evidence captured. Still requires human NEEDED closure, API24 imports, 16KB alignment, actual config/license/codec review, Android runtime tests and source/relink delivery.'

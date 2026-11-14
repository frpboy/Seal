#!/usr/bin/env bash
# Source-only native preparation, no app edit, install, upload, signing or runtime.
set -euo pipefail
: "${ANDROID_NDK_ROOT:?Verified existing r28c root}" "${SOURCE_ARCHIVE:?}" "${WORK:?Fresh absolute output path}"
[[ "$WORK" = /* && ! -e "$WORK" ]]
printf '%s  %s\n' 1593761a62bd7ffe5eaa90a1530dc2c9e587110081da00f230557cc0c3fb3179 "$SOURCE_ARCHIVE" | sha256sum -c -
root="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$WORK/source" "$WORK/build"
python3 - "$SOURCE_ARCHIVE" "$WORK/source" <<'PY'
import tarfile,sys,pathlib
with tarfile.open(sys.argv[1]) as t:
 for m in t:
  p=pathlib.PurePosixPath(m.name)
  assert not p.is_absolute() and '..' not in p.parts and (m.isfile() or m.isdir()),m.name
 t.extractall(sys.argv[2])
PY
src="$WORK/source/MMKV-1.3.17"
cmake -S "$src/Android/MMKV/mmkv" -B "$WORK/build" \
 "-DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK_ROOT/build/cmake/android.toolchain.cmake" \
 -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-24 -DANDROID_STL=c++_static \
 -DCMAKE_BUILD_TYPE=Release -DCMAKE_EXPORT_COMPILE_COMMANDS=ON \
 '-DCMAKE_CXX_FLAGS=-DMMKV_STL_SHARED=0 -fvisibility=hidden -funwind-tables -fasynchronous-unwind-tables -O2' \
 '-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384,-z,common-page-size=16384,-z,separate-loadable-segments,-z,relro,-z,now,-z,noexecstack'
cmake --build "$WORK/build" --target mmkv --parallel 2
elf="$WORK/build/libmmkv.so"
test -f "$elf"
"$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf" -h -l -d -s -V --wide "$elf" > "$WORK/libmmkv.elf.txt"
python3 "$root/audit-python-elf-pages.py" "$elf" --canonical --output "$WORK/elf-layout.json"
python3 "$root/audit-native-import-closure.py" --readelf "$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf" --system "$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/24" --prefix "$WORK/build" --output "$WORK/api24-import-closure.json"
python3 - "$WORK/api24-import-closure.json" <<'PYNEEDED'
import json,sys
r=json.load(open(sys.argv[1]));target=[x for x in r['objects'] if x['path'].endswith('/libmmkv.so')]
assert len(target)==1
assert set(target[0]['needed'])=={'liblog.so','libz.so','libm.so','libdl.so','libc.so'},target[0]['needed']
PYNEEDED
# Keep unstripped custody; emit distinct stripped packaging output and audit it too.
mkdir -p "$WORK/packaging"
"$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" --strip-unneeded "$elf" -o "$WORK/packaging/libmmkv.so"
python3 "$root/audit-python-elf-pages.py" "$WORK/packaging/libmmkv.so" --canonical --output "$WORK/stripped-elf-layout.json"
sha256sum "$SOURCE_ARCHIVE" "$elf" "$WORK/packaging/libmmkv.so" | tee "$WORK/output-sha256.txt"
# New build paths/tool versions can change bytes. Do not auto-admit a new hash.

printf '%s\n' 'BUILD ONLY. Source/JNI/API/import/ELF review and data-preserving Android upgrade remain unrun.'

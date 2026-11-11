#!/usr/bin/env bash
# Bounded cross-compilation only. Output is not runtime-qualified.
# Caller provides original archive + already verified NDK r28c and host compiler.
set -euo pipefail
: "${NDK:?verified Android NDK r28c tree required}"
: "${RUNNER_SOURCE:?exact reviewed EJS-only driver source required}"
: "${SOURCE_ARCHIVE:?original QuickJS archive required}"
: "${WORK:?new empty work directory required}"
: "${BACKPORT_PATCH:?reviewed maintenance patch required}"
: "${ABI:?arm64-v8a or x86_64 required}"
# Resource bounds apply to compiler children too. No target execution in this recipe.
ulimit -v 1310720
ulimit -t 600
[ "$(sed -n 's/^Pkg.Revision *= *//p' "$NDK/source.properties")" = "28.2.13676358" ]

echo 'cfd2f86bc18559e3e59d8874e6da4f36db83a3631a627858ee19de8793d1d2fe  '"$RUNNER_SOURCE" | sha256sum -c -
[ ! -e "$WORK" ] || { echo 'WORK must not exist' >&2; exit 1; }
echo 'b376e839b322978313d929fd20663b11ba58b75df5a46c126dd19ea2fa70ad2a  '"$SOURCE_ARCHIVE" | sha256sum -c -
echo 'c30c6308ef87c667882a9c65cd9b3286224e6242a3ccadadf3c68f9161b22702  '"$BACKPORT_PATCH" | sha256sum -c -
case "$ABI" in
  arm64-v8a) TRIPLE=aarch64-linux-android ;;
  x86_64) TRIPLE=x86_64-linux-android ;;
  *) exit 2 ;;
esac
mkdir -p "$WORK/host" "$WORK/target" "$WORK/out"
# Archive is the pinned official source. Extraction safety still belongs to preflight.
tar -xJf "$SOURCE_ARCHIVE" --strip-components=1 -C "$WORK/host"
tar -xJf "$SOURCE_ARCHIVE" --strip-components=1 -C "$WORK/target"
for tree in host target; do
  (cd "$WORK/$tree" && patch --fuzz=0 -p1 < "$BACKPORT_PATCH")
  (cd "$WORK/$tree" && echo 'ec3aba7e06a6f0a53274672856819acf3777c9c91e3f23642f1d0781454eee63  quickjs.c
0477d38bd5b2ea0d5d51e190992a94c1470974cab0443c4703cbe76c8e975a68  libunicode.c' | sha256sum -c -)
done
# Do not build/link quickjs-libc/qjs/repl or any module loader.
cp "$RUNNER_SOURCE" "$WORK/target/seali-ejs-runner.c"
TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
CC="$TOOLCHAIN/${TRIPLE}24-clang"
CFLAGS=(-O2 -fPIE -fwrapv -D_GNU_SOURCE '-DCONFIG_VERSION="2026-06-04"')
# Deliberately no HAVE_CLOSEFROM autodetection: API24 stub owns API reachability.
for source in seali-ejs-runner quickjs dtoa libregexp libunicode cutils; do
  "$CC" "${CFLAGS[@]}" -I"$WORK/target" -c "$WORK/target/$source.c" -o "$WORK/out/$source.o"
done
"$CC" -pie -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384 \
  -Wl,-z,relro,-z,now -Wl,-z,separate-loadable-segments -Wl,--build-id=sha1 -o "$WORK/out/libqjs.so" \
  "$WORK/out/"*.o -lm -ldl
"$TOOLCHAIN/llvm-readelf" -h -l -d -s "$WORK/out/libqjs.so" > "$WORK/out/libqjs.elf.txt"
sha256sum "$WORK/out/"* > "$WORK/out/SHA256SUMS"
# Do not execute Android output here. Qualification must inspect ABI, API24 imports,
# PIE interpreter, LOAD+RELRO 16KB congruence, NEEDED closure and actual guest/phone use.

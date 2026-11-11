#!/usr/bin/env bash
# SOURCE-ONLY CANDIDATE. Not an admission to build or a qualified Android binary.
# Caller provides original archive + already verified NDK r28c and host compiler.
set -euo pipefail
echo 'BLOCKED: independent runtime-source, std/os, temp/cancel and target review pending; source preparation only' >&2
exit 78
# Remaining candidate recipe is retained for review; execution is not admitted.
: "${NDK:?verified Android NDK r28c tree required}"
: "${SOURCE_ARCHIVE:?original QuickJS archive required}"
: "${WORK:?new empty work directory required}"
: "${BACKPORT_PATCH:?reviewed maintenance patch required}"
: "${ABI:?arm64-v8a or x86_64 required}"
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
# Generate REPL bytecode using this very source version, never a target executable.
make -C "$WORK/host" -j2 qjsc
(cd "$WORK/host" && ./qjsc -s -c -o "$WORK/target/repl.c" -m repl.js)
TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
CC="$TOOLCHAIN/${TRIPLE}24-clang"
CFLAGS=(-O2 -fPIE -fwrapv -D_GNU_SOURCE '-DCONFIG_VERSION="2026-06-04"')
# Deliberately no HAVE_CLOSEFROM autodetection: API24 stub owns API reachability.
for source in qjs repl quickjs dtoa libregexp libunicode cutils quickjs-libc; do
  "$CC" "${CFLAGS[@]}" -I"$WORK/target" -c "$WORK/target/$source.c" -o "$WORK/out/$source.o"
done
"$CC" -pie -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384 \
  -Wl,-z,relro,-z,now -Wl,--build-id=sha1 -o "$WORK/out/libqjs.so" \
  "$WORK/out/"*.o -lm -ldl
"$TOOLCHAIN/llvm-readelf" -h -l -d -s "$WORK/out/libqjs.so" > "$WORK/out/libqjs.elf.txt"
sha256sum "$WORK/out/"* > "$WORK/out/SHA256SUMS"
# Do not execute Android output here. Qualification must inspect ABI, API24 imports,
# PIE interpreter, LOAD+RELRO 16KB congruence, NEEDED closure and actual guest/phone use.

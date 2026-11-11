#!/usr/bin/env bash
# Bounded source-only two-component build. No Python rebuild, package, upload or target execution.
set -euo pipefail
: "${ADMISSION_JSON:?}" "${ANDROID_NDK_HOME:?}" "${OPENSSL_SOURCE:?}" "${EXPAT_SOURCE:?}" "${BUILD_ROOT:?}" "${TARGET_ABI:?}"
# Prerequisite data is evidence, never a grant of user execution authority.
python3 - "$ADMISSION_JSON" <<'PY'
import json,sys
r=json.load(open(sys.argv[1]))
assert r.get('scope')=='source-only-arm64-components-prerequisites'
assert all(r.get(k) is True for k in ['source_archives_and_trees_verified','ndk_and_host_verified'])
PY
[[ ! -e "$BUILD_ROOT" && ! -L "$BUILD_ROOT" && "$BUILD_ROOT" = /* ]]
[[ "$(uname -sm)" = 'Linux x86_64' ]]
case "$TARGET_ABI" in
 arm64-v8a) target=android-arm64; triple=aarch64-linux-android; epoch=1794338241; header_sha=7833b1345772f15bf541cad3d142ceda413e4ed7fe2850813f8454cd1f46acd4 ;;
 *) exit 64 ;;
esac
export SOURCE_DATE_EPOCH="$epoch"
export ANDROID_NDK_ROOT="$ANDROID_NDK_HOME"
TC="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64"
export PATH="$TC/bin:$PATH"
export CC="$TC/bin/${triple}24-clang" AR="$TC/bin/llvm-ar" RANLIB="$TC/bin/llvm-ranlib"
# Don't disable deprecated API: old CPython/_crypt consumers import OPENSSL_3.0.0.
unset CFLAGS CXXFLAGS CPPFLAGS LDFLAGS CMAKE_TOOLCHAIN_FILE CMAKE_PREFIX_PATH
mkdir -m 700 "$BUILD_ROOT"; mkdir "$BUILD_ROOT/openssl" "$BUILD_ROOT/evidence"
bounded() { timeout --signal=TERM --kill-after=30s 1800 "$@"; }
# Deterministic disclosed Android SONAME patch after pristine source-tree verification.
python3 - "$OPENSSL_SOURCE/Configurations/15-android.conf" "$BUILD_ROOT/evidence/openssl-android-soname-patch.json" <<'PYSONAME'
import pathlib,hashlib,json,sys
p=pathlib.Path(sys.argv[1]);b=p.read_bytes()
assert hashlib.sha256(b).hexdigest()=='b4d3aaca9c7b3cb779d37997ca4ea8a081982868bc693482c7caadec70d0e4ad'
old=b'        shared_extension => ".so",'
new=b'        shared_extension => ".so.\\$(SHLIB_VERSION_NUMBER)",'
assert b.count(old)==1
updated=b.replace(old,new);p.write_bytes(updated)
pathlib.Path(sys.argv[2]).write_text(json.dumps({'file':str(p),'original_sha256':hashlib.sha256(b).hexdigest(),'patched_sha256':hashlib.sha256(updated).hexdigest(),'change':'Android shared extension .so to .so.$(SHLIB_VERSION_NUMBER), preserving ABI-major SONAME'},indent=2)+'\n')
PYSONAME
cd "$BUILD_ROOT/openssl"
bounded perl "$OPENSSL_SOURCE/Configure" "$target" -D__ANDROID_API__=24 \
 --prefix=/usr --openssldir=/usr/etc/tls --libdir=lib shared \
 no-apps no-tests no-zlib no-brotli no-zstd no-quic no-siv no-cms no-module no-legacy \
 -O2 -fPIC -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384
bounded make -j1
# New runner path can change build-info bytes. Preserve actual hash, do not
# auto-accept historical pins or change downstream locks.
sha256sum crypto/buildinf.h > "$BUILD_ROOT/evidence/actual-buildinf.sha256"
bounded make DESTDIR="$BUILD_ROOT/stage" install_sw
# no no-engine: old CPython _ssl may import ENGINE compatibility functions.
# Default provider is in libcrypto; no legacy/engine module bytes are carried over.
cd "$BUILD_ROOT"
bounded cmake -S "$EXPAT_SOURCE" -B "$BUILD_ROOT/expat" \
 "-DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
 "-DANDROID_ABI=$TARGET_ABI" -DANDROID_PLATFORM=android-24 \
 -DCMAKE_BUILD_TYPE=Release -DCMAKE_POSITION_INDEPENDENT_CODE=ON \
 -DCMAKE_INSTALL_PREFIX=/usr -DCMAKE_INSTALL_LIBDIR=lib \
 '-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384' \
 -DCMAKE_PLATFORM_NO_VERSIONED_SONAME=OFF -DEXPAT_SHARED_LIBS=ON -DEXPAT_BUILD_TOOLS=OFF -DEXPAT_BUILD_TESTS=OFF \
 -DEXPAT_BUILD_EXAMPLES=OFF -DEXPAT_BUILD_DOCS=OFF -DEXPAT_BUILD_FUZZERS=OFF \
 -DEXPAT_DTD=OFF -DEXPAT_GE=OFF -DEXPAT_NS=ON -DEXPAT_CHAR_TYPE=char \
 -DEXPAT_WITH_ARC4RANDOM=OFF -DEXPAT_WITH_ARC4RANDOM_BUF=OFF \
 -DCMAKE_FIND_USE_PACKAGE_REGISTRY=OFF -DCMAKE_FIND_USE_SYSTEM_PACKAGE_REGISTRY=OFF
bounded cmake --build "$BUILD_ROOT/expat" --parallel 1
bounded env DESTDIR="$BUILD_ROOT/stage" cmake --install "$BUILD_ROOT/expat"
for name in libssl.so.3 libcrypto.so.3 libexpat.so.1; do
 f="$BUILD_ROOT/stage/usr/lib/$name"; [[ -f "$f" ]]
 "$TC/bin/llvm-readelf" -h -l -d -V --wide "$f" > "$BUILD_ROOT/evidence/$name.elf.txt"
 "$TC/bin/llvm-readelf" --dyn-symbols --wide "$f" > "$BUILD_ROOT/evidence/$name.symbols.txt"
done
cp "$BUILD_ROOT/openssl/include/openssl/configuration.h" "$BUILD_ROOT/evidence/openssl-configuration.h"
cp "$BUILD_ROOT/openssl/configdata.pm" "$BUILD_ROOT/evidence/"
cp "$BUILD_ROOT/expat/expat_config.h" "$BUILD_ROOT/evidence/"
printf '%s\n' 'BUILD ONLY: verify all old consumer NAME+VERSION imports, NEEDED/API24/16KB, target imports/TLS/XML rejection, notices and source custody before packaging.'

#!/usr/bin/env bash
# REVIEW-ONLY: this packet does not authorize native/configure execution.
set -euo pipefail
: "${ADMISSION_JSON:?reviewed admission receipt}" "${ANDROID_NDK_HOME:?verified NDK tree}"
: "${FFMPEG_SOURCE:?verified FFmpeg9.0.2 tree}" "${LAME_SOURCE:?verified LAME4.0 tree}"
: "${OPUS_SOURCE:?verified Opus1.6.1 tree}" "${BUILD_ROOT:?absent private root}"
: "${TARGET_ABI:?arm64-v8a or x86_64}" "${STAGE_LIMIT_SECONDS:?reviewed seconds per stage}"
python3 - "$ADMISSION_JSON" <<'PY'
import json,sys
r=json.load(open(sys.argv[1])); keys=['build_only_execution_authorized','source_archives_and_trees_verified','ndk_revision_and_tree_verified','host_capacity_and_time_limit_accepted','source_and_relink_custody_ready']
if not all(r.get(k) is True for k in keys):sys.exit(78)
if r.get('scope') != 'native-build-only-no-packaging-runtime-publication':sys.exit(78)
PY
[[ "$STAGE_LIMIT_SECONDS" =~ ^[1-9][0-9]*$ ]] || exit 64
[[ ! -e "$BUILD_ROOT" && ! -L "$BUILD_ROOT" ]] || exit 64
[[ "$(uname -sm)" = 'Linux x86_64' ]] || exit 64
case "$TARGET_ABI" in
 arm64-v8a) triple=aarch64-linux-android; arch=aarch64 ;;
 x86_64) triple=x86_64-linux-android; arch=x86_64 ;;
 *) exit 64 ;;
esac
for p in "$BUILD_ROOT" "$ANDROID_NDK_HOME" "$FFMPEG_SOURCE" "$LAME_SOURCE" "$OPUS_SOURCE"; do
 [[ "$p" = /* && "$p" != *$'\n'* && "$p" != *';'* ]] || exit 64
done
TC="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64"
export CC="$TC/bin/${triple}24-clang" CXX="$TC/bin/${triple}24-clang++"
export AR="$TC/bin/llvm-ar" RANLIB="$TC/bin/llvm-ranlib" NM="$TC/bin/llvm-nm" STRIP="$TC/bin/llvm-strip"
[[ -x "$CC" && -x "$AR" ]] || exit 66
unset CMAKE_PREFIX_PATH CMAKE_TOOLCHAIN_FILE CPPFLAGS
mkdir -m 700 "$BUILD_ROOT"
prefix="$BUILD_ROOT/deps"
mkdir "$prefix" "$BUILD_ROOT/lame" "$BUILD_ROOT/ffmpeg"
export PKG_CONFIG_LIBDIR="$prefix/lib/pkgconfig" PKG_CONFIG_PATH= PKG_CONFIG_SYSROOT_DIR=
export CFLAGS='-O2 -fPIC' CXXFLAGS='-O2 -fPIC' LDFLAGS='-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384'
bounded() { timeout --signal=TERM --kill-after=30s "$STAGE_LIMIT_SECONDS" "$@"; }
# LAME4 configure still exposes FFmpeg's required lame_set_VBR_quality symbol.
# Decoder/frontend are unnecessary and would add mpg123/sndfile dependencies.
cd "$BUILD_ROOT/lame"
bounded "$LAME_SOURCE/configure" --host="$triple" --prefix="$prefix" --libdir="$prefix/lib" \
 --enable-static --disable-shared --with-pic --disable-decoder --disable-frontend --disable-nasm
bounded make -j1
bounded make install
[[ -s "$prefix/include/lame/lame.h" && -s "$prefix/lib/libmp3lame.a" ]] || exit 67
"$NM" -g --defined-only "$prefix/lib/libmp3lame.a" > "$BUILD_ROOT/lame-symbols.txt"
grep -Eq '[[:space:]]lame_set_VBR_quality$' "$BUILD_ROOT/lame-symbols.txt" || exit 67
# Float Opus API retained; no neural model download/program/test build.
bounded cmake -S "$OPUS_SOURCE" -B "$BUILD_ROOT/opus" \
 "-DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
 "-DANDROID_ABI=$TARGET_ABI" -DANDROID_PLATFORM=android-24 -DCMAKE_BUILD_TYPE=Release \
 -DCMAKE_POSITION_INDEPENDENT_CODE=ON -DBUILD_SHARED_LIBS=OFF \
 "-DCMAKE_INSTALL_PREFIX=$prefix" -DCMAKE_INSTALL_LIBDIR=lib \
 -DCMAKE_FIND_USE_PACKAGE_REGISTRY=OFF -DCMAKE_FIND_USE_SYSTEM_PACKAGE_REGISTRY=OFF \
 -DOPUS_PACKAGE_VERSION=1.6.1 -DCMAKE_DISABLE_FIND_PACKAGE_Git=TRUE \
 -DBUILD_TESTING=OFF -DOPUS_BUILD_SHARED_LIBRARY=OFF -DOPUS_BUILD_TESTING=OFF -DOPUS_BUILD_PROGRAMS=OFF \
 -DOPUS_INSTALL_PKG_CONFIG_MODULE=ON -DOPUS_ENABLE_FLOAT_API=ON -DOPUS_FIXED_POINT=OFF \
 -DOPUS_HARDENING=ON -DOPUS_FAST_MATH=OFF -DOPUS_DEEP_PLC=OFF -DOPUS_DRED=OFF -DOPUS_OSCE=OFF
bounded cmake --build "$BUILD_ROOT/opus" --parallel 1
bounded cmake --install "$BUILD_ROOT/opus"
cd "$BUILD_ROOT/ffmpeg"
# No GPL/version3/nonfree, no auto-host libraries, no external HTTP/TLS libraries.
# Native FFmpeg handles only local media. Python supplies actual network download.
bounded "$FFMPEG_SOURCE/configure" --prefix=/usr --target-os=android --arch="$arch" \
 --enable-cross-compile --cc="$CC" --cxx="$CXX" --ld="$CC" --ar="$AR" --ranlib="$RANLIB" \
 --nm="$NM" --strip="$STRIP" --sysroot="$TC/sysroot" \
 --pkg-config=pkg-config --pkg-config-flags=--static \
 "--extra-cflags=-O2 -fPIC -I$prefix/include" \
 "--extra-ldflags=-L$prefix/lib $LDFLAGS" \
 --enable-shared --disable-static --disable-autodetect --disable-doc --disable-debug \
 --disable-ffplay --disable-vulkan --disable-indevs --disable-outdevs --enable-indev=lavfi \
 --disable-network --enable-zlib --enable-libmp3lame --enable-libopus
bounded make -j1
bounded make DESTDIR="$BUILD_ROOT/stage" install
cp config.h config_components.h ffbuild/config.mak "$BUILD_ROOT/"
printf '%s\n' 'Build stage only; ELF inventory, source delivery, packaging and Android functional tests remain.'

#!/usr/bin/env bash
# Local native preparation only. No adoption or APK generation.
set -euo pipefail
root="$(cd "$(dirname "$0")" && pwd)"
requested="${1:?Usage: rebuild-webp.sh ABSOLUTE_NONEXISTENT_WORK_DIRECTORY}"
exec /usr/bin/env -i PATH=/usr/bin:/bin LC_ALL=C TZ=UTC SOURCE_DATE_EPOCH=1754524800 /bin/bash --noprofile --norc -s -- "$root" "$requested" <<'BUILD'
set -euo pipefail
root="$1"
requested="$2"
[[ "$requested" = /* ]] || { echo 'Absolute work path required' >&2; exit 1; }
# mkdir without -p is deliberate: existing directories/symlinks/caches fail.
umask 077
mkdir -m 700 -- "$requested"
work="$(cd "$requested" && pwd -P)"
export HOME="$work/private-home"
mkdir -m 700 "$HOME"
cd "$work"
{
 printf 'controlled_PATH=%s\nsource_date_epoch=%s\n' "$PATH" "$SOURCE_DATE_EPOCH"
 /usr/bin/make --version
 /usr/bin/python3 --version
 /usr/bin/curl --version
 /usr/bin/unzip -v
} > build-tool-versions.txt
fetch() { if [ ! -f "$2" ]; then curl --fail --location --silent --show-error "$1" -o "$2"; fi; }
# A new checksum-pinned toolchain provenance replaces the host CMake contract.
# Download/extract only inside the already fresh private work directory.
fetch https://cmake.org/files/v3.22/cmake-3.22.1-linux-x86_64.tar.gz cmake.tar.gz
printf '%s  %s\n' 73565c72355c6652e9db149249af36bcab44d9d478c5546fd926e69ad6b43640 cmake.tar.gz | sha256sum -c -
tar xzf cmake.tar.gz
cmake="$work/cmake-3.22.1-linux-x86_64/bin/cmake"
[[ -f "$cmake" && ! -L "$cmake" && -x "$cmake" ]]
[[ "$("$cmake" --version | head -1)" == 'cmake version 3.22.1' ]]
"$cmake" --version >> build-tool-versions.txt
sha256sum cmake.tar.gz "$cmake" >> build-tool-versions.txt
fetch https://dl.google.com/android/repository/android-ndk-r28c-linux.zip ndk.zip
fetch https://storage.googleapis.com/downloads.webmproject.org/releases/webp/libwebp-1.6.0.tar.gz source.tar.gz
fetch https://repo.maven.apache.org/maven2/io/github/junkfood02/youtubedl-android/ffmpeg/0.18.1/ffmpeg-0.18.1.aar original.aar
printf '%s  %s\n' \
 dfb20d396df28ca02a8c708314b814a4d961dc9074f9a161932746f815aa552f ndk.zip \
 e4ab7009bf0629fd11982d4c2aa83964cf244cffba7347ecd39019a9e38c4564 source.tar.gz \
 0a87ffa6cf912b0fe76c1a99b9107f543ee2f247935fae2c71f0822eb7bc5f49 original.aar | sha256sum -c -
# Expand only the three checksum-pinned official inputs, not user ZIP contents.
unzip -q ndk.zip
tar xzf source.tar.gz
ndk="$work/android-ndk-r28c"
"$ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/clang" --version >> build-tool-versions.txt
cat "$ndk/source.properties" >> build-tool-versions.txt
for abi in arm64-v8a x86_64; do
 "$cmake" -S libwebp-1.6.0 -B "build-$abi" \
  -DCMAKE_TOOLCHAIN_FILE="$ndk/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI="$abi" -DANDROID_PLATFORM=android-24 \
  -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=ON \
  -DCMAKE_C_FLAGS="-ffile-prefix-map=$work=/seali-native-source" \
  -DCMAKE_SHARED_LINKER_FLAGS='-Wl,-z,max-page-size=16384' \
  -DWEBP_BUILD_CWEBP=OFF -DWEBP_BUILD_DWEBP=OFF -DWEBP_BUILD_GIF2WEBP=OFF \
  -DWEBP_BUILD_IMG2WEBP=OFF -DWEBP_BUILD_VWEBP=OFF -DWEBP_BUILD_WEBPINFO=OFF \
  -DWEBP_BUILD_ANIM_UTILS=OFF -DWEBP_BUILD_EXTRAS=OFF -DWEBP_BUILD_WEBPMUX=OFF \
  -DWEBP_BUILD_LIBWEBPMUX=ON -DWEBP_ENABLE_SWAP_16BIT_CSP=ON
 "$cmake" --build "build-$abi" --parallel 4
 "$ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/clang" \
  --target="$([ "$abi" = arm64-v8a ] && printf aarch64-linux-android24 || printf x86_64-linux-android24)" \
  --sysroot="$ndk/toolchains/llvm/prebuilt/linux-x86_64/sysroot" \
  -I libwebp-1.6.0/src "$root/webp-roundtrip.c" -L "build-$abi" \
  -Wl,-z,max-page-size=16384 -lwebp -lwebpmux -lwebpdemux -o "build-$abi/webp-roundtrip"
done
python3 "$root/patch-ffmpeg-aar.py" "$work"

BUILD

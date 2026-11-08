#!/usr/bin/env bash
# Isolated real Gradle transform fixture. No Android application is built.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
work="${1:?new absolute qualification directory}"
gradle_home="${2:?Gradle 8.11.1 distribution directory}"
java_home="${3:?JDK21 directory}"
umask 077
mkdir -m 700 "$work"
work="$(cd "$work" && pwd -P)"
mkdir "$work/home" "$work/classes" "$work/empty"
export JAVA_HOME="$java_home"
"$java_home/bin/java" -version 2>&1 | grep -E 'version "21\.'
"$gradle_home/bin/gradle" --version | grep -F 'Gradle 8.11.1'
"$java_home/bin/javac" -proc:none -cp "$gradle_home/lib/*:$gradle_home/lib/plugins/*" \
 -d "$work/classes" "$root/../../buildSrc/src/main/java/SealiWebpTransform.java"
"$java_home/bin/jar" cf "$work/transform.jar" -C "$work/classes" .
cp "$root/qualification/build.gradle" "$work/build.gradle"
printf 'rootProject.name="seali-native-transform-fixture"\n' > "$work/settings.gradle"
mkdir -p "$work/repo/fixture/ffmpeg/0.18.1" "$work/repo/fixture/common/1"
input="$work/repo/fixture/ffmpeg/0.18.1/ffmpeg-0.18.1.aar"
curl --fail --location --silent --show-error --max-time 120 \
 https://repo.maven.apache.org/maven2/io/github/junkfood02/youtubedl-android/ffmpeg/0.18.1/ffmpeg-0.18.1.aar -o "$input"
printf '%s  %s\n' 0a87ffa6cf912b0fe76c1a99b9107f543ee2f247935fae2c71f0822eb7bc5f49 "$input" | sha256sum -c -
printf '<project><modelVersion>4.0.0</modelVersion><groupId>fixture</groupId><artifactId>ffmpeg</artifactId><version>0.18.1</version><packaging>aar</packaging><dependencies><dependency><groupId>fixture</groupId><artifactId>common</artifactId><version>1</version></dependency></dependencies></project>' > "${input%.aar}.pom"
printf '<project><modelVersion>4.0.0</modelVersion><groupId>fixture</groupId><artifactId>common</artifactId><version>1</version></project>' > "$work/repo/fixture/common/1/common-1.pom"
"$java_home/bin/jar" cf "$work/repo/fixture/common/1/common-1.jar" -C "$work/empty" .
run() {
 (cd "$work" && /usr/bin/env -i PATH=/usr/bin:/bin HOME="$work/home" JAVA_HOME="$java_home" \
 /usr/bin/timeout --signal=TERM --kill-after=30s 1000s "$gradle_home/bin/gradle" \
 --no-daemon --max-workers=2 -Dorg.gradle.jvmargs=-Xmx512m "-PrecipeDir=$root" qualify)
}
run > "$work/positive.log" 2>&1
cp "$input" "$work/original.aar"
printf x >> "$input"
if run > "$work/negative.log" 2>&1; then
 echo 'Hash tamper falsely passed' >&2; exit 1
fi
grep -F 'Unexpected FFmpeg input. Refusing native substitution.' "$work/negative.log"
cp "$work/original.aar" "$input"
printf 'PASS isolated Gradle identity/transitive fixture and hash-tamper negative. Android AGP/runtime not tested.\n'

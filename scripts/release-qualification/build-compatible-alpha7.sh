#!/usr/bin/env bash
# SOURCE-ONLY PREPARATION. Invoke only after final integrated-source review and run admission.
# Secrets enter this step's env only. Never xtrace, export to GITHUB_ENV, cache or archive them.
set -euo pipefail
set +x
umask 077
: "${RUNNER_TEMP:?}" "${KEYSTORE_BASE64:?}" "${KEY_ALIAS:?}" "${KEY_PASSWORD:?}" "${STORE_PASSWORD:?}"
work=$(mktemp -d "$RUNNER_TEMP/seali-alpha7-signing.XXXXXX")
cleanup() { rm -rf -- "$work"; }
trap cleanup EXIT
trap 'exit 130' HUP INT TERM
for name in SEALI_UPLOAD_KEYSTORE_FILE SEALI_UPLOAD_KEYSTORE_TYPE SEALI_UPLOAD_KEY_ALIAS SEALI_UPLOAD_KEY_PASSWORD SEALI_UPLOAD_STORE_PASSWORD; do
  test -z "${!name-}" || { echo 'FAIL: unexpected inherited signing override'; exit 1; }
done
test ! -e keystore.properties
printf '%s' "$KEYSTORE_BASE64" | tr -d '[:space:]' | base64 --decode > "$work/key.bin"
unset KEYSTORE_BASE64
test -s "$work/key.bin"
magic=$(od -An -tx1 -N4 "$work/key.bin" | tr -d '[:space:]')
case "$magic" in feedfeed) store_type=JKS;; 30*) store_type=PKCS12;; *) echo 'FAIL: unsupported keystore format'; exit 1;; esac
keytool -exportcert -alias "$KEY_ALIAS" -keystore "$work/key.bin" -storetype "$store_type" -storepass:env STORE_PASSWORD -file "$work/cert.der" > "$work/tool.log" 2>&1 || { echo 'FAIL: existing certificate could not be opened'; exit 1; }
test "$(sha256sum "$work/cert.der" | cut -d ' ' -f1)" = 88774e5211b61f7827db0ba6991a668000ceab9a9e9490648054b8a19b6e070f || { echo 'FAIL: certificate is not the original production identity'; exit 1; }
keytool -certreq -alias "$KEY_ALIAS" -keystore "$work/key.bin" -storetype "$store_type" -storepass:env STORE_PASSWORD -keypass:env KEY_PASSWORD -file "$work/check.csr" > "$work/tool.log" 2>&1 || { echo 'FAIL: existing private key could not be unlocked'; exit 1; }
# No secrets in command arguments, properties files or retained directories.
SEALI_UPLOAD_KEYSTORE_FILE="$work/key.bin" SEALI_UPLOAD_KEYSTORE_TYPE="$store_type" SEALI_UPLOAD_KEY_ALIAS="$KEY_ALIAS" SEALI_UPLOAD_KEY_PASSWORD="$KEY_PASSWORD" SEALI_UPLOAD_STORE_PASSWORD="$STORE_PASSWORD" \
 timeout --signal=TERM --kill-after=30s 1500 ./gradlew --no-daemon --no-configuration-cache --max-workers=2 -Dorg.gradle.jvmargs=-Xmx3072m -Pkotlin.compiler.execution.strategy=in-process -PfunctionalAlphaCandidate -PnoSplits -PABI_FILTERS=arm64-v8a :app:assembleGenericRelease
# Key deleted by trap before subsequent verifier/collection/upload steps.

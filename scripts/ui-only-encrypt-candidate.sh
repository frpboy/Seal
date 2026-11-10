#!/usr/bin/env bash
# Public artifact contains ciphertext only. Never put signing keys into custody tar.
set -euo pipefail
umask 077
cert=scripts/ui-only-custody-recipient.pem
cert_hash=scripts/ui-only-custody-recipient.sha256
sha256sum -c "$cert_hash"
openssl x509 -in "$cert" -noout -checkend 3600
mapfile -t apps < <(find app/build/outputs/apk/uiOnly/debug -maxdepth 1 -name '*.apk')
mapfile -t tests < <(find app/build/outputs/apk/androidTest/uiOnly/debug -maxdepth 1 -name '*.apk')
test "${#apps[@]}" -eq 1
test "${#tests[@]}" -eq 1
test -s "${apps[0]}"
test -s "${tests[0]}"
mkdir -p "$RUNNER_TEMP/ui-custody-private"
cp "${apps[0]}" "$RUNNER_TEMP/ui-custody-private/ui-only-candidate.apk"
cp "${tests[0]}" "$RUNNER_TEMP/ui-custody-private/ui-only-candidate-test.apk"
(cd "$RUNNER_TEMP/ui-custody-private" && sha256sum *.apk > SHA256SUMS)
printf '%s\n' "$(git rev-parse HEAD)" > "$RUNNER_TEMP/ui-custody-private/source-sha.txt"
tar -C "$RUNNER_TEMP/ui-custody-private" -cf "$RUNNER_TEMP/ui-candidate.tar" ui-only-candidate.apk ui-only-candidate-test.apk SHA256SUMS source-sha.txt
mkdir -p candidate-ciphertext
openssl cms -encrypt -binary -aes-256-cbc -in "$RUNNER_TEMP/ui-candidate.tar" -outform DER -out candidate-ciphertext/ui-candidate.cms "$cert"
python3 - <<'PY'
import hashlib,pathlib
p=pathlib.Path('candidate-ciphertext/ui-candidate.cms')
b=p.read_bytes();assert 0<len(b)<=50*1024*1024, 'Encrypted custody cap exceeded'
assert b[:1]==b'\x30', 'Expected DER CMS only'
assert sorted(x.name for x in p.parent.iterdir())==['ui-candidate.cms']
print('UI_CIPHERTEXT',len(b),hashlib.sha256(b).hexdigest())
PY
rm -rf "$RUNNER_TEMP/ui-custody-private" "$RUNNER_TEMP/ui-candidate.tar"

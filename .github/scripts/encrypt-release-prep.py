#!/usr/bin/env python3
"""Publish only bounded CMS ciphertext and a fixed safe-evidence allowlist."""
from pathlib import Path
import hashlib,json,subprocess
root=Path('private-release-upload')
root.mkdir(exist_ok=False)
inputs=sorted(Path('app/build/outputs/apk/generic/release').glob('*-arm64-v8a.apk'))
bundles=sorted(Path('app/build/outputs/bundle/playRelease').glob('*.aab'))
assert len(inputs)==len(bundles)==1,(len(inputs),len(bundles))
inputs+=bundles
# 300MiB is a fail-closed storage ceiling, NOT a claim about AAB size.
# Previous current-source universal Actions archive225372200B + delivered
# ARM64 APK66079247B total291451447B. Ceiling adds <=22MiB overhead/headroom.
cap=300*1024*1024
assert sum(p.stat().st_size for p in inputs) < cap, 'Release inputs exceed bounded storage ceiling'
manifest=[]
for source in inputs:
    assert source.is_file() and not source.is_symlink()
    target=root/(source.name+'.cms')
    subprocess.run(['openssl','cms','-encrypt','-binary','-aes-256-cbc','-in',str(source),
        '-out',str(target),'-outform','DER','.github/seali-release-recipient.pem'],check=True)
    manifest.append({'file':source.name,'bytes':source.stat().st_size,
        'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),
        'ciphertext':target.name,'ciphertext_bytes':target.stat().st_size,
        'ciphertext_sha256':hashlib.sha256(target.read_bytes()).hexdigest()})
assert sum(p.stat().st_size for p in root.iterdir()) < cap,'Ciphertext exceeds total ceiling'
(root/'encrypted-delivery.json').write_text(json.dumps(manifest,indent=2)+'\n')
assert sum(p.stat().st_size for p in root.iterdir()) <= cap,'Total ciphertext upload exceeds 300MiB ceiling'
print('Encrypted exactly ARM64 release APK + Play AAB; no plaintext binary upload')

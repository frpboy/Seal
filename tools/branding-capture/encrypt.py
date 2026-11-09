#!/usr/bin/env python3
"""One-use screenshot-only encrypted return. No plaintext fallbacks or arbitrary file paths."""
import base64
import hashlib
import io
import json
import os
from pathlib import Path
import secrets
import subprocess
import zipfile
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

NAMES = ('about-top.png', 'about-credits.png', 'credits.png', 'language.png', 'troubleshooting.png')
LIMIT = 1024 * 1024
TOTAL_LIMIT = 4 * LIMIT

def main():
    root = Path(os.environ['RUNNER_TEMP']) / 'seali-branding-capture'
    if set(p.name for p in root.iterdir()) != set(NAMES):
        raise ValueError('capture allowlist mismatch')
    blobs = {}
    for name in NAMES:
        path = root / name
        if path.is_symlink() or not path.is_file() or not 0 < path.stat().st_size <= LIMIT:
            raise ValueError('capture shape or size invalid')
        data = path.read_bytes()
        if data[:8] != b'\x89PNG\r\n\x1a\n' or data[12:16] != b'IHDR':
            raise ValueError('capture is not PNG')
        if int.from_bytes(data[16:20], 'big') != 720 or int.from_bytes(data[20:24], 'big') != 1280:
            raise ValueError('unexpected viewport')
        blobs[name] = data
    if sum(map(len, blobs.values())) > TOTAL_LIMIT:
        raise ValueError('capture total too large')
    def git(*args):
        return subprocess.check_output(['git', *args], text=True).strip()
    binding = {'protocol': 'seali-branding-v1', 'checkout_sha': git('rev-parse', 'HEAD'),
               'checkout_tree': git('rev-parse', 'HEAD^{tree}'),
               'run_id': os.environ['GITHUB_RUN_ID'], 'attempt': os.environ['GITHUB_RUN_ATTEMPT'],
               'repository': os.environ['GITHUB_REPOSITORY'], 'viewport': [720, 1280],
               'locale': 'en-US', 'theme': 'light', 'density_dpi': 320,
               'scope': 'production composables and callbacks; not AppEntry navigation or full runtime qualification'}
    if binding['checkout_sha'] != os.environ['GITHUB_SHA'] or binding['repository'] != 'frpboy/Seali':
        raise ValueError('source binding mismatch')
    manifest = {'binding': binding, 'files': {n: {'size': len(d), 'sha256': hashlib.sha256(d).hexdigest()} for n,d in blobs.items()}}
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, 'w', zipfile.ZIP_STORED) as z:
        z.writestr('manifest.json', json.dumps(manifest, sort_keys=True))
        for n, d in blobs.items(): z.writestr(n, d)
    aad = json.dumps(binding, sort_keys=True, separators=(',', ':')).encode()
    key, nonce = secrets.token_bytes(32), secrets.token_bytes(12)
    public = serialization.load_pem_public_key(Path('tools/branding-capture/recipient.pem').read_bytes())
    wrapped = public.encrypt(key, padding.OAEP(mgf=padding.MGF1(hashes.SHA256()), algorithm=hashes.SHA256(), label=b'seali-branding-v1'))
    ciphertext = AESGCM(key).encrypt(nonce, buf.getvalue(), aad)
    envelope = {'binding': binding, 'wrapped_key': base64.b64encode(wrapped).decode(),
                'nonce': base64.b64encode(nonce).decode(), 'aad': base64.b64encode(aad).decode(),
                'ciphertext': base64.b64encode(ciphertext).decode()}
    data = json.dumps(envelope, sort_keys=True, separators=(',', ':')).encode()
    encoded = base64.b64encode(data).decode()
    chunks = [encoded[i:i+4000] for i in range(0,len(encoded),4000)]
    if len(chunks) > 1900: raise ValueError('envelope log cap')
    # All validation and authenticated encryption complete before any return is emitted.
    print('SEALI_CAPTURE_V1_BEGIN ' + json.dumps({'chunks':len(chunks),'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()}))
    for i,c in enumerate(chunks): print(f'SEALI_CAPTURE_V1_CHUNK {i:04d} {c}')
    print('SEALI_CAPTURE_V1_END')

if __name__ == '__main__':
    try: main()
    except Exception:
        # Never print source file contents, plaintext images, keys, or captured app output.
        raise SystemExit('Seali private capture validation failed; no plaintext return permitted')

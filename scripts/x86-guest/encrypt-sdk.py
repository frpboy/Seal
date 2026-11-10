#!/usr/bin/env python3
"""Retain only bounded private SDK diagnostics, not SDK archives/images/userdata."""
import pathlib,os,tarfile,subprocess,hashlib
r=pathlib.Path(os.environ['RUNNER_TEMP'])/'seali-sdk-diagnostics';assert (r/'evidence/receipt.json').is_file()
archive=r/'diagnostics.tar.gz'
with tarfile.open(archive,'w:gz') as t:
 t.add(r/'evidence',arcname='evidence');t.add('scripts/x86-guest/prepare-sdk.py',arcname='recipes/prepare-sdk.py')
assert archive.stat().st_size<=20*1024*1024
recipient=pathlib.Path('scripts/python-components/recipient.pem');assert hashlib.sha256(recipient.read_bytes()).hexdigest()=='033aa324ba76e6d67ef28a0f2d0dc336e00823b3e023d6b87129f283660b8cc3'
cipher=r/'sdk-diagnostics.cms';subprocess.run(['openssl','cms','-encrypt','-binary','-aes-256-cbc','-in',str(archive),'-outform','DER','-out',str(cipher),str(recipient)],check=True);assert cipher.stat().st_size<=20*1024*1024
print('SDK_DIAGNOSTIC_CIPHERTEXT',cipher.stat().st_size,hashlib.sha256(cipher.read_bytes()).hexdigest())

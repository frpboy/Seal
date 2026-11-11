#!/usr/bin/env python3
"""Append only a statically verified EJS-only ELF to the existing ARM payload manifest."""
import argparse,hashlib,json,pathlib,shutil
p=argparse.ArgumentParser();p.add_argument('--elf',required=True);p.add_argument('--receipt',required=True);p.add_argument('--root',required=True);a=p.parse_args()
f=pathlib.Path(a.elf);r=json.loads(pathlib.Path(a.receipt).read_text());root=pathlib.Path(a.root)
assert f.is_file() and not f.is_symlink()
h=hashlib.sha256(f.read_bytes()).hexdigest()
assert r['elf_sha256']==h and r['abi']=='arm64-v8a' and r['api']==24
assert r['runtime_tested'] is False and r['source_security_accepted'] is False
manifest=root/'functional-alpha-payload-manifest.json';m=json.loads(manifest.read_text())
assert m['target']=='arm64-v8a'
dest=root/'app/src/main/jniLibs/arm64-v8a/libqjs.so'
assert dest.parent.is_dir() and not dest.parent.is_symlink(), 'existing reviewed ARM payload directory required'
assert not dest.exists() and not dest.is_symlink()
assert all(pathlib.Path(x['path']).name!='libqjs.so' for x in m['files'])
# Ensure previously reviewed bytes still match before extending the exact allowlist.
for x in m['files']:
 path=root/x['path'];assert path.is_file() and not path.is_symlink();assert hashlib.sha256(path.read_bytes()).hexdigest()==x['sha256']
shutil.copyfile(f,dest);dest.chmod(0o755)
m['files'].append({'path':str(dest.relative_to(root)),'bytes':dest.stat().st_size,'sha256':h})
m['quickjs_source']={'archive_sha256':'b376e839b322978313d929fd20663b11ba58b75df5a46c126dd19ea2fa70ad2a','maintenance_patch_sha256':'c30c6308ef87c667882a9c65cd9b3286224e6242a3ccadadf3c68f9161b22702','driver_sha256':'cfd2f86bc18559e3e59d8874e6da4f36db83a3631a627858ee19de8793d1d2fe','static_receipt_sha256':hashlib.sha256(pathlib.Path(a.receipt).read_bytes()).hexdigest(),'runtime_tested':False,'public_release_ready':False}
manifest.write_text(json.dumps(m,indent=2)+'\n')

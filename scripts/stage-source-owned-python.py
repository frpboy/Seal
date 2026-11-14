#!/usr/bin/env python3
"""Stage exact reviewed ARM64 producer outputs. Never builds, downloads, or executes target code."""
import argparse, hashlib, json, pathlib, shutil, zipfile
p=argparse.ArgumentParser();p.add_argument('--candidate',required=True);p.add_argument('--root',required=True);a=p.parse_args()
c=pathlib.Path(a.candidate);root=pathlib.Path(a.root)
pins={'libpython.zip.so':(20587205,'38f6e236ba307217895ffd180569b6cacb7c854cc43ad8c9eac461c3430638df'),'libpython.so':(52416,'9726de75497c51d033dd1b4bdb9931344815bb3260633d7aa46587e7ea8ab3ce')}
for name,(size,digest) in pins.items():
 f=c/name;assert f.is_file() and not f.is_symlink();assert f.stat().st_size==size and hashlib.sha256(f.read_bytes()).hexdigest()==digest,name
with zipfile.ZipFile(c/'libpython.zip.so') as z:
 b=z.read('usr/etc/tls/openssl.cnf');assert len(b)==118 and hashlib.sha256(b).hexdigest()=='dc0489b417d3ec948a5368da397dd00f4eba360e7170883d577d1f25e15c697d'
dest=root/'app/src/main/jniLibs/arm64-v8a';assert dest.is_dir(),'stage original reviewed media/QJS payload first'
media_pins = {
 'libffmpeg.so':'cd1b02f0e69b4e763c24260e2d8248473988efde15ce3d93dfe99c961624bbb0',
 'libffmpeg.zip.so':'cea7955e24ed489a01c0f166a8393057f3f65741986dd7eaa55986760e6d27e1',
 'libffprobe.so':'1a17e0d69db1dd8825427928cde93ea8e19ae67a41f89f9696e5242a38efc23d',
 'libqjs.so':'4d8e0ee76b73156888f657f097b53a1e96727f2d3fd6f7c0c4eb6c7e92bb7e19'}
assert set(f.name for f in dest.iterdir()) <= set(media_pins) | set(pins), 'unexpected existing payload'
for name,digest in media_pins.items():
 f=dest/name;assert f.is_file() and not f.is_symlink();assert hashlib.sha256(f.read_bytes()).hexdigest()==digest,name
for name in pins:shutil.copyfile(c/name,dest/name)
manifest=root/'functional-alpha-payload-manifest.json'
d=json.loads(manifest.read_text()) if manifest.exists() else {'target':'arm64-v8a','target_executed':False,'apk_built':False,'public_release_ready':False}
d['files']=[{'path':str(f.relative_to(root)),'bytes':f.stat().st_size,'sha256':hashlib.sha256(f.read_bytes()).hexdigest()} for f in sorted(dest.iterdir()) if f.is_file()]
for row in d['files']:
 name=pathlib.Path(row['path']).name
 if name in pins:row.update(bytes=pins[name][0],sha256=pins[name][1])
d['source_owned_python']={'version':'3.11.18','cryptodome':'3.24.2','target_executed':False,'source_custody':['521b89e816949445036fd01de7241324aa4da443ba25f338d59669d188404d07','e982ac7d619733c511a8e88ca5dbc87190f9c76394b03284c3b952d12e2bc0ae'],'arm64_only':True}
manifest.write_text(json.dumps(d,indent=2)+'\n')
print('exact reviewed source Python staged, ARM64 only; no Android execution')

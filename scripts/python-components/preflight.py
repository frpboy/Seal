#!/usr/bin/env python3
"""Read-only native build preflight. Never downloads, extracts, configures or builds."""
import argparse, hashlib, json, os, pathlib, platform, shutil, subprocess, tarfile
p=argparse.ArgumentParser();p.add_argument('--ndk',required=True);p.add_argument('--source-lock-root',required=True);p.add_argument('--out',required=True);a=p.parse_args()
def sha(p):
 h=hashlib.sha256()
 with open(p,'rb') as f:
  for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
 return h.hexdigest()
def run(v):
 try:return subprocess.run(v,capture_output=True,text=True,timeout=30).stdout[:10000]
 except Exception as e:return {'error':str(e)}
r={'kind':'preflight-evidence-not-execution-approval','host':platform.platform(),'disk':dict(zip(['total','used','free'],shutil.disk_usage(a.source_lock_root))),'memory':pathlib.Path('/proc/meminfo').read_text(),'tools':{},'archives':[]}
for cmd in ['cmake','make','perl','timeout']:
 path=shutil.which(cmd);r['tools'][cmd]={'path':path,'version':run([path,'--version']) if path else None}
locks=[('openssl.tar.gz','603f5602e2eef00d77fbd429d34dcd5822bb301757a1bc9cdb24c670f1eb859a'),('expat.tar.xz','c624ba2998fc65df1163a49641ff3f9ab73138b11641d92c86bee0fb01b1bc7f')]
for rel,want in locks:
 f=pathlib.Path(a.source_lock_root)/rel;actual=sha(f)
 if actual!=want:raise RuntimeError('Archive mismatch: '+rel)
 with tarfile.open(f) as t:
  manifest=[]
  for m in t.getmembers():
   name=pathlib.PurePosixPath(m.name)
   if name.is_absolute() or '..' in name.parts or not (m.isfile() or m.isdir()):raise RuntimeError('Unsafe archive member '+m.name)
   if m.issym() or m.islnk():
    target=pathlib.PurePosixPath(m.linkname)
    if target.is_absolute() or '..' in target.parts:raise RuntimeError('Review archive link '+m.name)
   manifest.append({'path':m.name,'type':m.type.decode(),'mode':oct(m.mode),'size':m.size,'link':m.linkname,'sha256':hashlib.sha256(t.extractfile(m).read()).hexdigest() if m.isfile() else None})
  r['archives'].append({'path':str(f),'sha256':actual,'members':manifest})
ndk=pathlib.Path(a.ndk).resolve();prop=ndk/'source.properties'
if not prop.is_file():raise RuntimeError('Missing NDK source.properties')
r['ndk']={'path':str(ndk),'properties':prop.read_text(),'tree':[]}
for f in sorted(ndk.rglob('*')):
 if f.is_symlink():r['ndk']['tree'].append({'path':str(f.relative_to(ndk)),'link':os.readlink(f)})
 elif f.is_file():r['ndk']['tree'].append({'path':str(f.relative_to(ndk)),'size':f.stat().st_size,'sha256':sha(f)})
r['ndk']['tree_digest']=hashlib.sha256(json.dumps(r['ndk']['tree'],sort_keys=True,separators=(',',':')).encode()).hexdigest()
tc=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin'
for cmd in ['aarch64-linux-android24-clang','x86_64-linux-android24-clang','llvm-ar','llvm-nm','llvm-readelf']:
 f=tc/cmd
 if not f.is_file():raise RuntimeError('Missing NDK tool '+str(f))
 r['tools'][cmd]={'path':str(f),'version':run([str(f),'--version'])}
pathlib.Path(a.out).write_text(json.dumps(r,indent=2)+'\n')
print(json.dumps({'receipt':a.out,'receipt_sha256':sha(a.out),'ndk_revision':prop.read_text(),'ndk_tree_digest':r['ndk']['tree_digest'],'archive_count':len(r['archives']),'execution_approved':False},indent=2))

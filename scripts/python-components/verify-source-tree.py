#!/usr/bin/env python3
"""Verify an already-extracted source tree against every regular file in its pinned archive."""
import argparse,hashlib,json,pathlib,tarfile
p=argparse.ArgumentParser();p.add_argument('--archive',required=True);p.add_argument('--sha256',required=True);p.add_argument('--tree',required=True);p.add_argument('--out',required=True);a=p.parse_args()
def sha(f):
 h=hashlib.sha256()
 with open(f,'rb') as x:
  for b in iter(lambda:x.read(1048576),b''):h.update(b)
 return h.hexdigest()
if sha(a.archive)!=a.sha256:raise RuntimeError('archive SHA mismatch')
root=pathlib.Path(a.tree).resolve();records=[];expected=set()
with tarfile.open(a.archive) as t:
 members=t.getmembers();tops={pathlib.PurePosixPath(m.name).parts[0] for m in members}
 if len(tops)!=1:raise RuntimeError('not single-root archive')
 for m in members:
  n=pathlib.PurePosixPath(m.name)
  if n.is_absolute() or '..' in n.parts or not(m.isfile() or m.isdir()):raise RuntimeError('unsupported archive member '+m.name)
  rel=pathlib.Path(*n.parts[1:]);f=root/rel
  if f.is_symlink():raise RuntimeError('unexpected symlink '+str(rel))
  if m.isdir():
   if not f.is_dir():raise RuntimeError('missing directory '+str(rel))
  else:
   want=hashlib.sha256(t.extractfile(m).read()).hexdigest()
   if not f.is_file() or sha(f)!=want:raise RuntimeError('tree mismatch '+str(rel))
   if m.mode & 0o111 and not(f.stat().st_mode & 0o111):raise RuntimeError('lost execute bit '+str(rel))
   expected.add(str(rel));records.append({'path':str(rel),'sha256':want,'size':m.size})
actual={str(f.relative_to(root)) for f in root.rglob('*') if f.is_file() or f.is_symlink()}
if actual!=expected:raise RuntimeError('extra/missing source files '+str(sorted(actual^expected)[:30]))
records.sort(key=lambda x:x['path']);r={'archive':a.archive,'archive_sha256':a.sha256,'tree':str(root),'files':len(records),'tree_digest':hashlib.sha256(json.dumps(records,sort_keys=True,separators=(',',':')).encode()).hexdigest(),'records':records,'native_execution':False}
pathlib.Path(a.out).write_text(json.dumps(r,indent=2)+'\n');print(json.dumps({k:v for k,v in r.items() if k!='records'},indent=2))

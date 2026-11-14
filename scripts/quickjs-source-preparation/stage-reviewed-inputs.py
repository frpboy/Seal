#!/usr/bin/env python3
"""Source-only input staging. No build/network/native execution. Hash-bound regular source files only."""
import argparse,hashlib,pathlib,tarfile,shutil,json
p=argparse.ArgumentParser();p.add_argument('--archive',required=True);p.add_argument('--patch',required=True);p.add_argument('--driver',required=True);p.add_argument('--out',required=True);a=p.parse_args()
items=[('quickjs-2026-06-04.tar.xz',a.archive,'b376e839b322978313d929fd20663b11ba58b75df5a46c126dd19ea2fa70ad2a'),('quickjs-june-maintenance.patch',a.patch,'c30c6308ef87c667882a9c65cd9b3286224e6242a3ccadadf3c68f9161b22702'),('seali-ejs-runner.c',a.driver,'cfd2f86bc18559e3e59d8874e6da4f36db83a3631a627858ee19de8793d1d2fe')]
for _,name,want in items:
 f=pathlib.Path(name);assert not f.is_symlink() and f.is_file();assert hashlib.sha256(f.read_bytes()).hexdigest()==want,name
with tarfile.open(a.archive) as t:
 total=0
 for m in t:
  parts=pathlib.PurePosixPath(m.name).parts
  assert parts and parts[0]=='quickjs-2026-06-04' and not m.name.startswith('/') and '..' not in parts
  assert m.isdir() or m.isfile(), 'no links/devices allowed in source archive'
  assert 0 <= m.size <= 64*1024*1024
  total += m.size
  assert total <= 64*1024*1024, 'bounded total source archive expansion'
out=pathlib.Path(a.out);out.mkdir(parents=True,exist_ok=False)
rows=[]
for dest,name,want in items:
 shutil.copyfile(name,out/dest);rows.append({'path':dest,'sha256':want,'bytes':(out/dest).stat().st_size})
(out/'inputs.json').write_text(json.dumps({'inputs':rows,'built':False,'runtime_tested':False},indent=2)+'\n')

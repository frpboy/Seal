#!/usr/bin/env python3
"""Audit source producer ELFs against their real DT_NEEDED closure and API24 stubs."""
import argparse,hashlib,json,re,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--readelf',required=True);p.add_argument('--system',required=True);p.add_argument('--prefix',action='append',required=True);p.add_argument('--output',required=True);a=p.parse_args()
cache={}
def metadata(path):
 path=path.resolve()
 if path in cache:return cache[path]
 out=subprocess.check_output([a.readelf,'--dyn-syms','--dynamic',str(path)],text=True)
 exports=set();imports=[]
 for line in out.splitlines():
  m=re.match(r'\s*\d+:\s+\S+\s+\d+\s+\S+\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)',line)
  if not m:continue
  bind,vis,ndx,name=m.groups()
  if ndx=='UND':
   if bind=='GLOBAL':imports.append(name.replace('@@','@'))
  elif bind in ('GLOBAL','WEAK') and vis in ('DEFAULT','PROTECTED'):
   exports.add(name.replace('@@','@'))
   if '@@' in name:exports.add(name.split('@@')[0])
 needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',out);soname=re.findall(r'\(SONAME\).*?\[(.*?)\]',out)
 x={'path':str(path),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'soname':soname[0] if soname else None,'needed':needed,'strong_imports':sorted(set(imports)),'exports':exports};cache[path]=x;return x
providers={};own=[]
for root in a.prefix:
 for path in sorted(Path(root).rglob('*')):
  if path.is_symlink() or not path.is_file():continue
  with path.open('rb') as f:head=f.read(18)
  if head[:6]!=b'\x7fELF\x02\x01' or int.from_bytes(head[16:18],'little') not in (2,3):continue
  x=metadata(path);own.append(x)
  if x['soname']:
   if x['soname'] in providers and providers[x['soname']]['sha256']!=x['sha256']:raise SystemExit('duplicate SONAME provider')
   providers[x['soname']]=x
for path in sorted(Path(a.system).glob('*.so')):
 if path.read_bytes()[:4]!=b'\x7fELF':continue
 x=metadata(path)
 providers.setdefault(path.name,x)
errors=[];results=[]
for obj in own:
 reachable={};queue=list(obj['needed'])
 while queue:
  name=queue.pop(0)
  if name in reachable:continue
  if name not in providers:errors.append({'path':obj['path'],'missing_provider':name});continue
  reachable[name]=providers[name];queue+=providers[name]['needed']
 exports=set(obj['exports'])
 for provider in reachable.values():exports|=provider['exports']
 missing=sorted(set(obj['strong_imports'])-exports)
 if missing:errors.append({'path':obj['path'],'unresolved_NAME_VERSION':missing})
 results.append({k:v for k,v in obj.items() if k!='exports'}|{'reachable':sorted(reachable),'unresolved':missing})
Path(a.output).write_text(json.dumps({'api':24,'own_elf_count':len(own),'errors':errors,'objects':results,'limits':'static strong-import scope check against API24 stub exports, not Android execution or weak-symbol behavior'},indent=2)+'\n')
print(json.dumps({'own_elf_count':len(own),'error_count':len(errors),'errors':errors[:8]}));raise SystemExit(bool(errors))

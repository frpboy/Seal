#!/usr/bin/env python3
"""Read-only exact old consumer symbol NAME+VERSION check for rebuilt SSL/crypto/Expat."""
import argparse,json,pathlib,subprocess,re,hashlib
p=argparse.ArgumentParser();p.add_argument('--stage',required=True);p.add_argument('--readelf',required=True);p.add_argument('--consumer-manifest',required=True);p.add_argument('--abi',required=True);p.add_argument('--out',required=True);a=p.parse_args()
def parse(f):
 s=subprocess.check_output([a.readelf,'--dyn-symbols','--wide',str(f)],text=True);out=set()
 for line in s.splitlines():
  c=line.split()
  if len(c)<8 or not c[0].endswith(':') or c[4] not in ('GLOBAL','WEAK') or c[6]=='UND':continue
  name=c[7]
  if '@@' in name:
   # Default versions satisfy unversioned imports as well as exact version imports.
   out.add(name.split('@@')[0]);out.add(name.replace('@@','@'))
  else:out.add(name)
 return out
root=pathlib.Path(a.stage);exports=set();libs=[]
for name in ['libssl.so.3','libcrypto.so.3','libexpat.so.1']:
 f=root/'usr/lib'/name;assert f.is_file();exports|=parse(f);libs.append({'name':name,'sha256':hashlib.sha256(f.read_bytes()).hexdigest()})
rows=[]
for x in json.load(open(a.consumer_manifest)):
 if x['abi']!=a.abi:continue
 req={n for n in x['required_imports'] if '@OPENSSL_' in n or n.startswith('XML_')}
 rows.append({'consumer':x['path'],'required_names_and_versions':sorted(req),'missing':sorted(req-exports)})
assert len(rows)==4,'Need exact four old consumers per ABI'
r={'method':'Exact NAME+VERSION for OpenSSL; unversioned XML names. @@ default definitions satisfy both forms. No normalization of version away.','abi':a.abi,'libraries':libs,'consumers':rows,'runtime_tested':False}
pathlib.Path(a.out).write_text(json.dumps(r,indent=2)+'\n')
assert not any(x['missing'] for x in rows),[(x['consumer'],x['missing']) for x in rows]
print(json.dumps({'abi':a.abi,'consumers':len(rows),'missing':0,'runtime_tested':False}))

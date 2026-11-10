#!/usr/bin/env python3
"""Generated configuration, all native ELF/static archive metadata and custody inventory, no native execution."""
import os,pathlib,json,hashlib,subprocess,re
root=pathlib.Path(os.environ['CUSTODY_ROOT']);build=root/'build';e=build/'evidence';tc=pathlib.Path(os.environ['ANDROID_NDK_HOME'])/'toolchains/llvm/prebuilt/linux-x86_64/bin';e.mkdir(exist_ok=True)
triple={'arm64-v8a':'aarch64-linux-android','x86_64':'x86_64-linux-android'}[os.environ['TARGET_ABI']]
cc=tc/(triple+'24-clang')
def macros(name,header,includes):
 cmd=[str(cc),'-dM','-E','-x','c']+['-I'+str(x) for x in includes]+['-include',str(header),'/dev/null']
 result=subprocess.run(cmd,capture_output=True,text=True,check=True,timeout=30)
 (e/(name+'-effective-macros.txt')).write_text(result.stdout)
 (e/(name+'-preprocessor.json')).write_text(json.dumps({'command':cmd,'returncode':result.returncode,'stderr':result.stderr},indent=2)+'\n')
 return {m.group(1):m.group(2).strip() for m in re.finditer(r'^#define\s+(\w+)(.*)$',result.stdout,re.M)}
ssl=macros('openssl',build/'openssl/include/openssl/configuration.h',[build/'openssl/include',pathlib.Path(os.environ['OPENSSL_SOURCE'])/'include'])
for macro in ['OPENSSL_NO_COMP_ALG','OPENSSL_NO_ZLIB','OPENSSL_NO_BROTLI','OPENSSL_NO_ZSTD','OPENSSL_NO_QUIC','OPENSSL_NO_SIV','OPENSSL_NO_CMS','OPENSSL_NO_MODULE','OPENSSL_NO_LEGACY']:
 assert macro in ssl,macro
assert 'OPENSSL_NO_COMP' not in ssl
expat=macros('expat',build/'expat/expat_config.h',[build/'expat',pathlib.Path(os.environ['EXPAT_SOURCE'])/'lib'])
for macro in ['XML_DTD','XML_GE']:
 assert expat.get(macro,'0')=='0',(macro,expat.get(macro))
assert expat.get('XML_NS')=='1',expat.get('XML_NS')
subprocess.run(['python3','scripts/python-components/check-old-consumer-symbols.py','--stage',str(build/'stage'),'--readelf',str(tc/'llvm-readelf'),'--consumer-manifest','scripts/python-components/old-consumer-imports.json','--abi',os.environ['TARGET_ABI'],'--out',str(e/'consumer-compatibility.json')],check=True)
rows=[]
for f in sorted(build.rglob('*')):
 if f.is_symlink():rows.append({'path':str(f.relative_to(build)),'link':os.readlink(f)});continue
 if not f.is_file():continue
 h=hashlib.sha256()
 with f.open('rb') as x:
  magic=x.read(8);x.seek(0)
  for b in iter(lambda:x.read(1048576),b''):h.update(b)
 rows.append({'path':str(f.relative_to(build)),'size':f.stat().st_size,'sha256':h.hexdigest()})
 if magic.startswith(b'\x7fELF') and not f.name.endswith('.o'):
  dest=e/('all-elf-'+str(f.relative_to(build)).replace('/','__')+'.txt')
  with dest.open('w') as out:subprocess.run([str(tc/'llvm-readelf'),'-h','-l','-d','-V','--dyn-symbols','--wide',str(f)],stdout=out,check=True)
(e/'build-inventory.json').write_text(json.dumps(rows,indent=2)+'\n')
print('GENERATED_CONFIGURATION_AND_4CONSUMER_COMPATIBILITY_PASS',os.environ['TARGET_ABI'],'NOT_RUNTIME_OR_SECURITY_CLEAR')

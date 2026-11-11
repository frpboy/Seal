#!/usr/bin/env python3
"""Generated configuration, all native ELF/static archive metadata and custody inventory, no native execution."""
import os,pathlib,json,hashlib,subprocess,re
root=pathlib.Path(os.environ['CUSTODY_ROOT']);build=root/'build';e=build/'evidence';tc=pathlib.Path(os.environ['ANDROID_NDK_HOME'])/'toolchains/llvm/prebuilt/linux-x86_64/bin';e.mkdir(exist_ok=True)
epoch,header_sha={'arm64-v8a':(1794338241,'7833b1345772f15bf541cad3d142ceda413e4ed7fe2850813f8454cd1f46acd4'),'x86_64':(1794338954,'03aa6ce4078e9501c747fb806b78862116b7051e336ad6a25442c2f8ac7586a3')}[os.environ['TARGET_ABI']]
actual_header_sha=hashlib.sha256((build/'openssl/crypto/buildinf.h').read_bytes()).hexdigest()
(e/'openssl-build-epoch.json').write_text(json.dumps({'abi':os.environ['TARGET_ABI'],'source_date_epoch':epoch,'actual_buildinf_header_sha256':actual_header_sha,'historical_header_sha256':header_sha,'binding':'New source build measured without auto-repin; historical comparison only'},indent=2)+'\n')
triple={'arm64-v8a':'aarch64-linux-android','x86_64':'x86_64-linux-android'}[os.environ['TARGET_ABI']]
cc=tc/(triple+'24-clang')
def macros(name,header,includes):
 cmd=[str(cc),'-dM','-E','-x','c']+['-I'+str(x) for x in includes]+['-include',str(header),'/dev/null']
 result=subprocess.run(cmd,capture_output=True,text=True,check=True,timeout=30)
 (e/(name+'-effective-macros.txt')).write_text(result.stdout)
 (e/(name+'-preprocessor.json')).write_text(json.dumps({'command':cmd,'returncode':result.returncode,'stderr':result.stderr},indent=2)+'\n')
 return {m.group(1):m.group(2).strip() for m in re.finditer(r'^#define\s+(\w+)(.*)$',result.stdout,re.M)}
ssl=macros('openssl',build/'openssl/include/openssl/configuration.h',[build/'openssl/include',pathlib.Path(os.environ['OPENSSL_SOURCE'])/'include'])
for macro in ['OPENSSL_NO_COMP_ALG','OPENSSL_NO_ZLIB','OPENSSL_NO_BROTLI','OPENSSL_NO_ZSTD','OPENSSL_NO_QUIC','OPENSSL_NO_SIV','OPENSSL_NO_CMS']:
 assert macro in ssl,macro
assert 'OPENSSL_NO_COMP' not in ssl
# Inspect the generated, exact scoped configdata table as data, without executing Perl.
configtext=(build/'openssl/configdata.pm').read_text()
blocks=re.findall(r'^our %disabled = \(\n(.*?)^\);',configtext,re.M|re.S)
assert len(blocks)==1,'Need exactly one generated disabled table'
disabled=dict(re.findall(r'^    "([^"\n]+)" => "([^"\n]+)",?$',blocks[0],re.M))
options=re.findall(r'^    "options" => "([^"\n]+)",?$',configtext,re.M)
assert len(options)==1,'Need exactly one generated options entry'
config={'disabled':disabled,'options':options[0],'configdata_sha256':hashlib.sha256(configtext.encode()).hexdigest(),'method':'Exact generated disabled table and options entry parsed as data; no Perl evaluation'}
(e/'openssl-effective-options.json').write_text(json.dumps(config,indent=2)+'\n')
for option in ['module','legacy']:
 assert option in config['disabled'] and 'no-'+option in config['options'].split(),option
for dirname in ['engines-3','ossl-modules']:
 directory=build/'stage/usr/lib'/dirname
 assert not directory.exists() or not any(x.is_file() or x.is_symlink() for x in directory.rglob('*')),str(directory)
# Bind actual output SONAME and internal NEEDED to the retained consumer ABI before exports check.
for name in ['libssl.so.3','libcrypto.so.3','libexpat.so.1']:
 dynamic=subprocess.check_output([str(tc/'llvm-readelf'),'-d','--wide',str(build/'stage/usr/lib'/name)],text=True)
 assert re.search(r'\(SONAME\).*\['+re.escape(name)+r'\]',dynamic),name
 if name=='libssl.so.3':
  assert re.search(r'\(NEEDED\).*\[libcrypto\.so\.3\]',dynamic)
  assert not re.search(r'\(NEEDED\).*\[libcrypto\.so\]',dynamic)
expat=macros('expat',build/'expat/expat_config.h',[build/'expat',pathlib.Path(os.environ['EXPAT_SOURCE'])/'lib'])
for macro in ['XML_DTD','XML_GE']:
 assert expat.get(macro,'0')=='0',(macro,expat.get(macro))
assert 'XML_NS' in expat
# API24 libc lacks later direct getrandom/arc4random interfaces. Preserve the
# actual historical Android syscall path and inspect target-preprocessed facts.
for macro in ['HAVE_ARC4RANDOM','HAVE_ARC4RANDOM_BUF','HAVE_GETRANDOM','XML_DEV_URANDOM']:
 assert macro not in expat,macro
assert 'HAVE_SYSCALL_GETRANDOM' in expat
(e/'expat-api24-entropy.json').write_text(json.dumps({'api':24,'HAVE_SYSCALL_GETRANDOM':True,
 'direct_getrandom':False,'arc4random':False,'arc4random_buf':False,'XML_DEV_URANDOM':False,
 'scope':'target preprocessor configuration, not execution or randomness quality proof'},indent=2)+'\n')
subprocess.run(['python3',str(pathlib.Path(__file__).with_name('check-old-consumer-symbols.py')),'--stage',str(build/'stage'),'--readelf',str(tc/'llvm-readelf'),'--consumer-manifest',str(pathlib.Path(__file__).with_name('old-consumer-imports.json')),'--abi',os.environ['TARGET_ABI'],'--out',str(e/'consumer-compatibility.json')],check=True)
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

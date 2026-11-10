#!/usr/bin/env python3
"""Pinned downloads and safe source preparation, no target/native build execution."""
import hashlib,json,os,pathlib,posixpath,subprocess,tarfile,urllib.request,zipfile
root=pathlib.Path(os.environ['RUNNER_TEMP'])/'seali-python-components-job'
root.mkdir(mode=0o700); inputs=root/'inputs';inputs.mkdir();sources=root/'sources';sources.mkdir();evidence=root/'evidence';evidence.mkdir()
locks=[('openssl.tar.gz','https://github.com/openssl/openssl/releases/download/openssl-3.5.9/openssl-3.5.9.tar.gz','603f5602e2eef00d77fbd429d34dcd5822bb301757a1bc9cdb24c670f1eb859a'),('expat.tar.xz','https://github.com/libexpat/libexpat/releases/download/R_2_10_0/expat-2.10.0.tar.xz','c624ba2998fc65df1163a49641ff3f9ab73138b11641d92c86bee0fb01b1bc7f'),('ndk.zip','https://dl.google.com/android/repository/android-ndk-r28c-linux.zip','dfb20d396df28ca02a8c708314b814a4d961dc9074f9a161932746f815aa552f')]
for rel,url,want in locks:
 p=inputs/rel;p.parent.mkdir(parents=True,exist_ok=True);h=hashlib.sha256()
 with urllib.request.urlopen(url,timeout=60) as r,p.open('wb') as f:
  for b in iter(lambda:r.read(1048576),b''):h.update(b);f.write(b)
 if h.hexdigest()!=want:raise RuntimeError('input hash mismatch: '+rel)
 print('PINNED_INPUT',rel,p.stat().st_size,want)
ndk_archive=inputs/'ndk.zip'
ndk_sha1=hashlib.sha1(ndk_archive.read_bytes()).hexdigest()
if ndk_archive.stat().st_size!=722261334 or ndk_sha1!='a7b54a5de87fecd125a17d54f73c446199e72a64':raise RuntimeError('official NDK archive size/SHA1 mismatch')
(evidence/'ndk-archive.json').write_text(json.dumps({'url':locks[-1][1],'bytes':ndk_archive.stat().st_size,'sha1':ndk_sha1,'sha256':locks[-1][2]},indent=2))
ndk=root/'ndk';ndk.mkdir()
with zipfile.ZipFile(inputs/'ndk.zip') as z:
 for m in z.infolist():
  n=pathlib.PurePosixPath(m.filename)
  if n.is_absolute() or '..' in n.parts or n.parts[0]!='android-ndk-r28c':raise RuntimeError('unsafe NDK path')
  if (m.external_attr>>16)&0o170000==0o120000:
   target=z.read(m).decode();resolved=posixpath.normpath(posixpath.join(str(n.parent),target))
   if target.startswith('/') or not resolved.startswith('android-ndk-r28c/'):raise RuntimeError('unsafe NDK link')
subprocess.run(['unzip','-q',str(inputs/'ndk.zip'),'-d',str(ndk)],check=True)
(inputs/'ndk.zip').unlink()
ndk=ndk/'android-ndk-r28c'
if 'Pkg.Revision = 28.2.13676358' not in (ndk/'source.properties').read_text():raise RuntimeError('NDK revision mismatch')
# Fail closed on measured hosted capacity, not a remembered runner size.
mem={line.split(':')[0]:int(line.split()[1])*1024 for line in pathlib.Path('/proc/meminfo').read_text().splitlines() if line.startswith(('MemTotal:','MemAvailable:'))}
import shutil
if mem['MemAvailable']<1500*1024*1024 or shutil.disk_usage(root).free<15*1024**3:raise RuntimeError('insufficient measured host capacity')
script=pathlib.Path('scripts/python-components')
subprocess.run(['python3',str(script/'preflight.py'),'--ndk',str(ndk),'--source-lock-root',str(inputs),'--out',str(evidence/'preflight.json')],check=True)
for name,rel,dirname in [('openssl','openssl.tar.gz','openssl-3.5.9'),('expat','expat.tar.xz','expat-2.10.0')]:
 p=inputs/rel;want=next(x[2] for x in locks if x[0]==rel)
 with tarfile.open(p) as t:t.extractall(sources,filter='data')
 subprocess.run(['python3',str(script/'verify-source-tree.py'),'--archive',str(p),'--sha256',want,'--tree',str(sources/dirname),'--out',str(evidence/(name+'-tree.json'))],check=True)
# Evidence-bound build-stage admission, never asserts runtime or publication ready.
recipe=script/'build-two-components.sh'
recipe_sha=hashlib.sha256(recipe.read_bytes()).hexdigest()
if recipe_sha!='07a437d742c736a32766b6ed28363edb25ed8845c7403e5776725f8ac8d6de0e':raise RuntimeError('recipe hash mismatch')
if os.environ['TARGET_ABI'] not in ['arm64-v8a','x86_64']:raise RuntimeError('ABI scope')
for cmd in ['cmake','make','perl','timeout','openssl','unzip']:
 if not shutil.which(cmd):raise RuntimeError('missing host tool: '+cmd)
receipt={'scope':'two-shared-components-build-only','execution_authorized':True,'source_archives_and_trees_verified':True,'ndk_and_host_verified':True,'source_notice_custody_ready':True,'target_abi':os.environ['TARGET_ABI'],'script_sha256':recipe_sha,'source_sha':os.environ['SOURCE_SHA'],'stage_seconds':1800,'overall_per_abi_seconds':3600,'runtime_tested':False,'publication_ready':False,'preflight_sha256':hashlib.sha256((evidence/'preflight.json').read_bytes()).hexdigest()}
(evidence/'admission.json').write_text(json.dumps(receipt,indent=2))
with open(os.environ['GITHUB_ENV'],'a') as f:
 for k,v in {'ADMISSION_JSON':evidence/'admission.json','ANDROID_NDK_HOME':ndk,'OPENSSL_SOURCE':sources/'openssl-3.5.9','EXPAT_SOURCE':sources/'expat-2.10.0','BUILD_ROOT':root/'build','CUSTODY_ROOT':root}.items():f.write(str(k)+'='+str(v)+'\n')

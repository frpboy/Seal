#!/usr/bin/env python3
"""Pinned downloads and safe source preparation, no target/native build execution."""
import hashlib,json,os,pathlib,posixpath,subprocess,tarfile,urllib.request,zipfile
root=pathlib.Path(os.environ['RUNNER_TEMP'])/'seali-native-job'
root.mkdir(mode=0o700); inputs=root/'inputs';inputs.mkdir();sources=root/'sources';sources.mkdir();evidence=root/'evidence';evidence.mkdir()
locks=[('ffmpeg-source.tar.xz','https://ffmpeg.org/releases/ffmpeg-9.0.2.tar.xz','8c3850283eb25fa026482078a04051e0be17347b09ef81a0849bec15a96e002e'),('sources/libmp3lame/source.tar','https://downloads.sourceforge.net/project/lame/lame/4.0/lame-4.0.tar.gz','3df5124d5ad3a98312ffd7ba6a9b36230e4f8a3e66d3ce0f425e336c32d216eb'),('sources/libopus/source.tar','https://ftp.osuosl.org/pub/xiph/releases/opus/opus-1.6.1.tar.gz','6ffcb593207be92584df15b32466ed64bbec99109f007c82205f0194572411a1'),('ndk.zip','https://dl.google.com/android/repository/android-ndk-r28c-linux.zip','dfb20d396df28ca02a8c708314b814a4d961dc9074f9a161932746f815aa552f')]
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
script=pathlib.Path('scripts/native-mincut')
subprocess.run(['python3',str(script/'preflight-native.py'),'--ndk',str(ndk),'--source-lock-root',str(inputs),'--out',str(evidence/'preflight.json')],check=True)
for name,rel,dirname in [('ffmpeg','ffmpeg-source.tar.xz','ffmpeg-9.0.2'),('lame','sources/libmp3lame/source.tar','lame-4.0'),('opus','sources/libopus/source.tar','opus-1.6.1')]:
 p=inputs/rel;want=next(x[2] for x in locks if x[0]==rel)
 with tarfile.open(p) as t:t.extractall(sources,filter='data')
 subprocess.run(['python3',str(script/'verify-source-tree.py'),'--archive',str(p),'--sha256',want,'--tree',str(sources/dirname),'--out',str(evidence/(name+'-tree.json'))],check=True)
# Evidence-bound build-stage admission, never asserts runtime or publication ready.
receipt={'scope':'native-build-only-no-packaging-runtime-publication','build_only_execution_authorized':True,'source_archives_and_trees_verified':True,'ndk_revision_and_tree_verified':True,'host_capacity_and_time_limit_accepted':True,'source_and_relink_custody_ready':True,'source_sha':os.environ['SOURCE_SHA'],'script_sha256':'ffc8ac51aa65b4c590a4effb2119a4de0ab1cdbdd32f78bb43abb3ac1a6b30a5','preflight_sha256':hashlib.sha256((evidence/'preflight.json').read_bytes()).hexdigest(),'stage_limit_seconds':1800,'overall_build_seconds':3600,'retention':'full inputs, sources, build tree, objects, archives, configs and commands encrypted before upload','target_abi':'x86_64','runtime_ready':False,'publication_ready':False}
(evidence/'admission.json').write_text(json.dumps(receipt,indent=2))
with open(os.environ['GITHUB_ENV'],'a') as f:
 for k,v in {'ADMISSION_JSON':evidence/'admission.json','ANDROID_NDK_HOME':ndk,'FFMPEG_SOURCE':sources/'ffmpeg-9.0.2','LAME_SOURCE':sources/'lame-4.0','OPUS_SOURCE':sources/'opus-1.6.1','BUILD_ROOT':root/'build','STAGE_LIMIT_SECONDS':1800,'TARGET_ABI':'x86_64','CUSTODY_ROOT':root}.items():f.write(str(k)+'='+str(v)+'\n')

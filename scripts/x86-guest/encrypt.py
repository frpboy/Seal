#!/usr/bin/env python3
"""Finite encrypted custody of source and complete native-build outputs, no APK."""
import hashlib,json,os,pathlib,shutil,subprocess,tarfile
root=pathlib.Path(os.environ['CUSTODY_ROOT']);e=root/'evidence'
(e/'outcome.json').write_text(json.dumps({'native_build_outcome':os.environ['BUILD_OUTCOME'],'app_build_outcome':os.environ.get('APP_BUILD_OUTCOME','not-run'),'source_sha':os.environ['SOURCE_SHA'],'target':'x86_64','runtime_tested':False,'runtime_outcome':os.environ.get('RUNTIME_OUTCOME','not-run'),'apk_built':os.environ.get('APP_BUILD_OUTCOME')=='success','publication_ready':False},indent=2))
recipes=root/'recipes';shutil.copytree('scripts/native-mincut',recipes)
# No NDK binary/archive, environment dump, credential, token or private key.
allow=[n for n in ['inputs','sources','evidence','recipes','app-qualification','python-pruned','build'] if (root/n).exists()]
# Fail closed if an accidental runtime image or symlink enters custody. Guest dirs
# are a RUNNER_TEMP sibling outside root, even if shutdown/removal fails.
for folder in ['app-qualification','evidence']:
 for p in (root/folder).rglob('*'):
  assert not p.is_symlink(),'custody symlink forbidden'
  assert not any(x.endswith('.avd') or x in ['runtime-avd','runtime-user','runtime-emulator','seali-functional-runtime-guest'] for x in p.relative_to(root).parts),'guest directory forbidden'
  assert p.suffix.lower() not in ['.img','.qcow2','.qcow'],'guest disk forbidden'
archive=root.parent/'seali-native-build.tar.gz'
with tarfile.open(archive,'w:gz') as t:
 for name in allow:t.add(root/name,arcname=name,recursive=True)
with tarfile.open(archive) as t:
 for m in t.getmembers():
  assert not any(x.endswith('.avd') or x in ['runtime-avd','runtime-user','runtime-emulator','seali-functional-runtime-guest'] for x in pathlib.PurePosixPath(m.name).parts),'guest directory in archive'
  assert pathlib.PurePosixPath(m.name).suffix.lower() not in ['.img','.qcow2','.qcow'],'guest disk in archive'
recipient=pathlib.Path('scripts/native-mincut/recipient.pem')
if hashlib.sha256(recipient.read_bytes()).hexdigest()!='033aa324ba76e6d67ef28a0f2d0dc336e00823b3e023d6b87129f283660b8cc3':raise RuntimeError('recipient drift')
cipher=root.parent/'native-build.cms'
temporary=root.parent/'native-build.cms.tmp'
cipher.unlink(missing_ok=True)
subprocess.run(['openssl','cms','-encrypt','-binary','-aes-256-cbc','-in',str(archive),'-outform','DER','-out',str(temporary),str(recipient)],check=True)
if temporary.stat().st_size>200*1024*1024:
 temporary.unlink()
 raise RuntimeError('cipher exceeds reviewed 200 MiB storage bound')
temporary.replace(cipher)
print('NATIVE_CIPHERTEXT',cipher.stat().st_size,hashlib.sha256(cipher.read_bytes()).hexdigest())
archive.unlink()

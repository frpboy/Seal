#!/usr/bin/env python3
"""Finite encrypted custody of source and complete native-build outputs, no APK."""
import hashlib,json,os,pathlib,shutil,subprocess,tarfile
root=pathlib.Path(os.environ['CUSTODY_ROOT']);e=root/'evidence'
(e/'outcome.json').write_text(json.dumps({'build_outcome':os.environ['BUILD_OUTCOME'],'source_sha':os.environ['SOURCE_SHA'],'target':'arm64-v8a','runtime_tested':False,'apk_built':os.environ.get('APP_BUILD_OUTCOME')=='success','publication_ready':False},indent=2))
recipes=root/'recipes';shutil.copytree('scripts/native-mincut',recipes)
# No NDK binary/archive, environment dump, credential, token or private key.
allow=[n for n in ['inputs','sources','evidence','recipes','app-qualification','python-pruned','build'] if (root/n).exists()]
archive=root.parent/'seali-native-build.tar.gz'
with tarfile.open(archive,'w:gz') as t:
 for name in allow:t.add(root/name,arcname=name,recursive=True)
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

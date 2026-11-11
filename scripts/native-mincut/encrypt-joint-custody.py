#!/usr/bin/env python3
"""Two finite CMS volumes containing one complete, ordered compressed custody archive."""
import hashlib,json,os,pathlib,shutil,subprocess,tarfile
CAP=200*1024*1024
CHUNK=CAP-1024*1024  # Reserve 1MiB per CMS envelope. Actual ciphertext cap checked below.
def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
 return h.hexdigest()
def encrypt_volumes(archive,recipient,out,chunk=CHUNK,cap=CAP):
 if archive.stat().st_size>2*chunk:raise RuntimeError('complete archive exceeds fixed two-volume capacity')
 rows=[]
 with archive.open('rb') as src:
  for index in range(2):
   plain=out/f'volume-{index+1}.part';cipher=out/('native-build.cms' if index==0 else 'components-build.cms')
   data=src.read(chunk);plain.write_bytes(data)
   try:
    subprocess.run(['openssl','cms','-encrypt','-binary','-aes-256-cbc','-in',str(plain),'-outform','DER','-out',str(cipher),str(recipient)],check=True)
    if cipher.stat().st_size>cap:raise RuntimeError('cipher exceeds unchanged 200MiB cap')
    rows.append({'order':index+1,'cipher_name':cipher.name,'cipher_sha256':sha(cipher),'cipher_bytes':cipher.stat().st_size,'plaintext_sha256':sha(plain),'plaintext_bytes':plain.stat().st_size})
   finally:plain.unlink(missing_ok=True)
  if src.read(1):raise RuntimeError('unconsumed archive bytes')
 manifest={'format':'ordered-decrypted-volumes-join-to-tar-gz-v1','archive_sha256':sha(archive),'archive_bytes':archive.stat().st_size,'volumes':rows,'volume_count':2,'cipher_total_bytes':sum(x['cipher_bytes'] for x in rows),'runtime_tested':False,'public_release_ready':False}
 if manifest['cipher_total_bytes']>2*cap:raise RuntimeError('aggregate ciphertext cap exceeded')
 return manifest

def main():
 root=pathlib.Path(os.environ['CUSTODY_ROOT']);components=pathlib.Path(os.environ['RUNNER_TEMP'])/'seali-python-components-job';out=root.parent
 for r in [root,components]:
  if not r.is_dir():raise RuntimeError('both complete custody roots required')
  (r/'evidence').mkdir(exist_ok=True)
 (root/'evidence/joint-outcome.json').write_text(json.dumps({'source_sha':os.environ['SOURCE_SHA'],'native_build_outcome':os.environ.get('BUILD_OUTCOME','unknown'),'app_build_outcome':os.environ.get('APP_BUILD_OUTCOME','unknown'),'component_build_outcome':os.environ.get('COMPONENT_BUILD_OUTCOME','unknown'),'runtime_tested':False,'publication_ready':False},indent=2))
 shutil.copytree('scripts/native-mincut',root/'recipes',dirs_exist_ok=True)
 shutil.copytree('scripts/python-components',components/'recipes',dirs_exist_ok=True)
 recipient=pathlib.Path('scripts/native-mincut/recipient.pem')
 if sha(recipient)!='033aa324ba76e6d67ef28a0f2d0dc336e00823b3e023d6b87129f283660b8cc3':raise RuntimeError('recipient drift')
 archive=out/'seali-joint-build.tar.gz'
 with tarfile.open(archive,'w:gz') as t:
  for label,r,names in [('native',root,['inputs','sources','evidence','recipes','app-qualification','python-pruned','build']),('components',components,['inputs','sources','evidence','recipes','build'])]:
   for name in names:
    if (r/name).exists():t.add(r/name,arcname=label+'/'+name,recursive=True)
 manifest=encrypt_volumes(archive,recipient,out)
 (out/'joint-custody-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
 # Public log contains only hashes/sizes/ordering, no source, APK or credentials.
 print('JOINT_CUSTODY_MANIFEST '+json.dumps(manifest,sort_keys=True),flush=True)
 archive.unlink()
if __name__=='__main__':main()

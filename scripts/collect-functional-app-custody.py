#!/usr/bin/env python3
import os,pathlib,shutil,subprocess,json,hashlib,zipfile
root=pathlib.Path(os.environ['CUSTODY_ROOT']);out=root/'app-qualification';out.mkdir(exist_ok=True)
for path in ['app/build/outputs','app/build/test-results','app/build/reports/tests']:
 p=pathlib.Path(path)
 if p.exists():shutil.copytree(p,out/p.name,dirs_exist_ok=True)
for n in ['functional-alpha-wrapper-manifest.json','functional-alpha-payload-manifest.json','functional-alpha-platform-jni.json']:
 p=pathlib.Path(n)
 if p.exists():shutil.copy(p,out/n)
with (out/'reviewed-app-source.tar').open('wb') as f:subprocess.run(['git','archive','HEAD'],stdout=f,check=True)
shutil.copytree('scripts',out/'app-recipes',dirs_exist_ok=True)
# Keep actual selected derived wrappers/Python, plus engine/source and public pin ledger.
# Do not call this full custody of unused ABI payloads in the original public AARs.
for name in ['app/libs/functional-alpha','app/src/main/assets/functional-engine']:
 p=pathlib.Path(name)
 if p.exists():shutil.copytree(p,out/p.name,dirs_exist_ok=True)
for name in ['yt-dlp','yt-dlp.tar.gz']:
 p=root/'app-inputs'/name
 if p.exists():shutil.copy(p,out/name)
(out/'public-input-custody-scope.txt').write_text('Original multi-ABI public AAR bytes are not retained in this packet. Exact public URLs/SHA256 are in evidence/app-inputs.json and replay recipes. Actual selected derived wrappers, Python prune outputs/manifest, engine+source, full native source/relink tree and APK evidence are retained. No full-original-AAR custody claim.\n')
rows=[]
for p in out.rglob('*'):
 if p.is_file():rows.append({'path':str(p.relative_to(out)),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
(out/'inventory.json').write_text(json.dumps(rows,indent=2))

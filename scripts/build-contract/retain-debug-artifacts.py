#!/usr/bin/env python3
"""Retain manifest-bound, debug-signed artifacts in unpublished custody only.

Adds files as new assets on the same pinned draft release the inputs came
from, after re-checking custody identity, binding every file to the reviewed
sha256 manifest and verifying each APK's actual signer is a single
non-production certificate. Never publishes, tags, edits, deletes, retries
or overwrites; no public artifact or API write beyond the pinned draft
asset upload. Mirrors custody-inputs.py retain boundaries.
"""
import argparse,hashlib,importlib.util,json,os,pathlib,re,subprocess,urllib.parse,urllib.request
_spec=importlib.util.spec_from_file_location('custody_inputs',pathlib.Path(__file__).with_name('custody-inputs.py'))
custody_inputs=importlib.util.module_from_spec(_spec);_spec.loader.exec_module(custody_inputs)
PRODUCTION_SIGNER='88774e5211b61f7827db0ba6991a668000ceab9a9e9490648054b8a19b6e070f'

def manifest_pairs(path):
    rows=[]
    for line in path.read_text().splitlines():
        digest,_,name=line.strip().partition('  ')
        if not re.fullmatch('[0-9a-f]{64}',digest) or not name:raise ValueError('malformed manifest line')
        rows.append((digest,pathlib.PurePosixPath(name)))
    if len(rows)!=2:raise ValueError('manifest must name exactly the app and test APK')
    prefixes=('app/build/outputs/apk/generic/debug/','app/build/outputs/apk/androidTest/generic/debug/')
    seen=set()
    for digest,name in rows:
        if name.is_absolute() or '..' in name.parts:raise ValueError('unsafe manifest path')
        hit=[p for p in prefixes if str(name).startswith(p) and name.suffix=='.apk']
        if len(hit)!=1 or hit[0] in seen:raise ValueError('manifest must pair one app and one androidTest APK')
        seen.add(hit[0])
    return rows

def signer_sha256(apksigner,apk):
    out=subprocess.run([str(apksigner),'verify','--print-certs',str(apk)],capture_output=True,text=True)
    if out.returncode!=0:raise ValueError('apksigner verify failed: '+apk.name)
    digests=re.findall(r'Signer #\d+ certificate SHA-256 digest: ([0-9a-f]{64})',out.stdout)
    if len(digests)!=1:raise ValueError('expected exactly one signer')
    if digests[0]==PRODUCTION_SIGNER:raise ValueError('refusing production-signed artifact in debug retention')
    return digests[0]

def retain(lock,manifest,apksigner,receipt_out):
    release,_=custody_inputs.check(lock)
    run=os.environ['GITHUB_RUN_ID'];sha=os.environ['GITHUB_SHA']
    if not run.isdigit() or not re.fullmatch('[0-9a-f]{40}',sha):raise ValueError('invalid run/source binding')
    existing={a['name'] for a in release['assets']}
    items=[]
    for digest,name in manifest_pairs(manifest):
        path=pathlib.Path(str(name))
        data=path.read_bytes()
        if hashlib.sha256(data).hexdigest()!=digest:raise ValueError('file differs from manifest: '+str(name))
        retained='Seali-alpha7-'+run+'-'+path.name
        if retained in existing:raise ValueError('output asset already exists; do not overwrite')
        items.append((retained,data,digest,signer_sha256(apksigner,path)))
    receipt={'source_sha':sha,'run_id':run,'custody_release_id':release['id'],'signer':'debug','runtime_tested':False,'public_release_ready':False,
             'artifacts':[{'name':n,'sha256':d,'bytes':len(data),'signer_sha256':s} for n,data,d,s in items]}
    items.append(('Seali-alpha7-'+run+'-debug-receipt.json',(json.dumps(receipt,indent=2)+'\n').encode(),None,None))
    results=[]
    for name,data,digest,_ in items:
        custody_inputs.check(lock)
        url='https://uploads.github.com/repos/'+custody_inputs.REPO+'/releases/'+str(release['id'])+'/assets?name='+urllib.parse.quote(name,safe='')
        request=urllib.request.Request(url,data=data,method='POST',headers={'Authorization':'Bearer '+os.environ['GH_TOKEN'],'Content-Type':'application/octet-stream','X-GitHub-Api-Version':'2022-11-28'})
        with urllib.request.build_opener(custody_inputs.NoRedirect).open(request,timeout=120) as response:a=json.load(response)
        d=hashlib.sha256(data).hexdigest()
        if a['name']!=name or a['size']!=len(data) or a.get('digest')!='sha256:'+d:raise ValueError('retained asset identity mismatch')
        results.append({'id':a['id'],'name':name,'sha256':d,'bytes':len(data)})
    custody_inputs.check(lock)
    receipt_out.write_text(json.dumps({'retained':results,'unpublished':True},indent=2)+'\n')
    return results

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--manifest',type=pathlib.Path,required=True);p.add_argument('--apksigner',type=pathlib.Path,required=True);p.add_argument('--receipt-out',type=pathlib.Path,required=True);a=p.parse_args()
    lock=json.loads(pathlib.Path(__file__).with_name('alpha7-input-lock.json').read_text())
    retain(lock,a.manifest,a.apksigner,a.receipt_out)
    print(json.dumps({'retained':3,'unpublished':True}))

#!/usr/bin/env python3
"""Pinned unpublished custody access. No publish, tag, settings, edit or delete APIs."""
import argparse,hashlib,json,os,pathlib,re,urllib.request,urllib.error,urllib.parse
REPO='frpboy/Seali';API='https://api.github.com'
class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self,req,fp,code,msg,headers,newurl):return None

def api(path,accept='application/vnd.github+json'):
    if not path.startswith('/repos/'+REPO+'/'):raise ValueError('foreign API')
    request=urllib.request.Request(API+path,headers={'Authorization':'Bearer '+os.environ['GH_TOKEN'],'Accept':accept,'X-GitHub-Api-Version':'2022-11-28'})
    return urllib.request.build_opener(NoRedirect).open(request,timeout=45)

def check(lock):
    c=lock['custody'];release=json.load(api('/repos/'+REPO+'/releases/'+str(c['release_id'])))
    for k in ['tag_name','name','target_commitish']:
        if release[k]!=c[k]:raise ValueError('custody '+k+' changed')
    if release['draft'] is not True or release['published_at'] is not None:raise ValueError('custody is not unpublished')
    try:api('/repos/'+REPO+'/git/ref/tags/'+c['tag_name'])
    except urllib.error.HTTPError as e:
        if e.code!=404:raise
    else:raise ValueError('custody tag exists')
    assets=[a for a in release['assets'] if a['id']==c['asset_id']]
    if len(assets)!=1:raise ValueError('input asset missing')
    a=assets[0];expected=lock['archive']
    if a['state']!='uploaded' or a['name']!=expected['name'] or a['size']!=expected['bytes'] or a.get('digest')!='sha256:'+expected['sha256']:raise ValueError('input asset identity changed')
    return release,a

def download(lock,output):
    _,a=check(lock)
    if output.exists():raise ValueError('output exists')
    try:response=api('/repos/'+REPO+'/releases/assets/'+str(a['id']),'application/octet-stream')
    except urllib.error.HTTPError as e:
        if e.code!=302:raise
        url=urllib.parse.urlparse(e.headers['Location'])
        if url.scheme!='https' or url.hostname!='release-assets.githubusercontent.com':raise ValueError('unexpected download redirect')
        response=urllib.request.urlopen(e.headers['Location'],timeout=60)
    with response,output.open('xb') as f:
        remaining=lock['archive']['bytes']
        while remaining:
            b=response.read(min(1024*1024,remaining))
            if not b:raise ValueError('truncated download')
            f.write(b);remaining-=len(b)
        if response.read(1):raise ValueError('oversize download')
    if hashlib.sha256(output.read_bytes()).hexdigest()!=lock['archive']['sha256']:raise ValueError('download digest mismatch')
    check(lock)

def retain(lock, apk, receipt):
    release,_=check(lock)
    run=os.environ['GITHUB_RUN_ID'];sha=os.environ['GITHUB_SHA']
    if not run.isdigit() or not re.fullmatch('[0-9a-f]{40}',sha):raise ValueError('invalid run/source binding')
    evidence=json.loads(receipt.read_text())
    data=apk.read_bytes();digest=hashlib.sha256(data).hexdigest()
    if evidence['apk_sha256']!=digest or evidence['signer_sha256']!='88774e5211b61f7827db0ba6991a668000ceab9a9e9490648054b8a19b6e070f' or evidence['code']!=200000172:raise ValueError('final verifier binding mismatch')
    items=[('Seali-alpha7-'+run+'-arm64-v8a.apk',data)]
    evidence.update(source_sha=sha,run_id=run,custody_release_id=release['id'],public_release_ready=False)
    items.append(('Seali-alpha7-'+run+'-receipt.json',(json.dumps(evidence,indent=2)+'\n').encode()))
    existing={a['name'] for a in release['assets']}
    if any(name in existing for name,_ in items):raise ValueError('output asset already exists; do not overwrite')
    results=[]
    for name,data in items:
        check(lock)
        url='https://uploads.github.com/repos/'+REPO+'/releases/'+str(release['id'])+'/assets?name='+urllib.parse.quote(name,safe='')
        request=urllib.request.Request(url,data=data,method='POST',headers={'Authorization':'Bearer '+os.environ['GH_TOKEN'],'Content-Type':'application/octet-stream','X-GitHub-Api-Version':'2022-11-28'})
        with urllib.request.build_opener(NoRedirect).open(request,timeout=120) as response:a=json.load(response)
        digest=hashlib.sha256(data).hexdigest()
        if a['name']!=name or a['size']!=len(data) or a.get('digest')!='sha256:'+digest:raise ValueError('retained asset identity mismatch')
        results.append({'id':a['id'],'name':name,'sha256':digest,'bytes':len(data)})
    check(lock)
    return results

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('action',choices=['download','retain']);p.add_argument('--output',type=pathlib.Path);p.add_argument('--apk',type=pathlib.Path);p.add_argument('--receipt',type=pathlib.Path);a=p.parse_args()
    lock=json.loads(pathlib.Path(__file__).with_name('alpha7-input-lock.json').read_text())
    if a.action=='download':
        if a.output is None:p.error('--output required')
        download(lock,a.output);print(json.dumps({'asset_id':lock['custody']['asset_id'],'sha256':lock['archive']['sha256'],'unpublished':True}))
    else:
        if a.apk is None or a.receipt is None:p.error('--apk and --receipt required')
        print(json.dumps({'retained':retain(lock,a.apk,a.receipt),'unpublished':True}))

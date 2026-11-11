#!/usr/bin/env python3
"""Fixed public source transport and whole-tree admission. No native execution."""
import argparse
import hashlib
import importlib.util
import json
import os
import platform
import shutil
import subprocess
import tarfile
import urllib.request
import zipfile
from pathlib import Path

INPUTS = [
 ('openssl.tar.gz','https://github.com/openssl/openssl/releases/download/openssl-3.5.9/openssl-3.5.9.tar.gz',53279637,'603f5602e2eef00d77fbd429d34dcd5822bb301757a1bc9cdb24c670f1eb859a'),
 ('expat.tar.xz','https://github.com/libexpat/libexpat/releases/download/R_2_10_0/expat-2.10.0.tar.xz',543404,'c624ba2998fc65df1163a49641ff3f9ab73138b11641d92c86bee0fb01b1bc7f'),
 ('ndk.zip','https://dl.google.com/android/repository/android-ndk-r28c-linux.zip',722261334,'dfb20d396df28ca02a8c708314b814a4d961dc9074f9a161932746f815aa552f')]


def require(ok, why):
    if not ok: raise ValueError(why)


def sha(path):
    h=hashlib.sha256()
    with path.open('rb') as f:
        for chunk in iter(lambda:f.read(1024*1024),b''): h.update(chunk)
    return h.hexdigest()


def module(name):
    spec=importlib.util.spec_from_file_location(name,Path(__file__).with_name(name+'.py'))
    m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m


def unpack_source(archive, dest):
    """Only fixed archives, no links/specials; validate full graph before writes."""
    require(not dest.exists(), 'fresh source root required')
    with tarfile.open(archive) as t:
        rows=t.getmembers();names=set();total=0;roots=set()
        require(len(rows)<=25000,'source member bound')
        for item in rows:
            parts=item.name.rstrip('/').split('/')
            require(parts and all(x not in ('','.','..') for x in parts) and not item.name.startswith('/'),'source traversal')
            name='/'.join(parts)
            require(name not in names and (item.isfile() or item.isdir()),'duplicate/link/special source member')
            names.add(name);roots.add(parts[0]);total+=item.size
            require(0<=item.size<=32*1024**2 and total<=250*1024**2,'source expansion bound')
        require(len(roots)==1,'source must have one root')
        kinds={x.name.rstrip('/'):x.isdir() for x in rows}
        for name in names:
            parts=name.split('/')
            require(all(kinds.get('/'.join(parts[:i]),True) for i in range(1,len(parts))),'source file ancestor')
        dest.mkdir(mode=0o700)
        for item in rows:
            p=dest/item.name
            if item.isdir(): p.mkdir(parents=True,exist_ok=True)
            else:
                p.parent.mkdir(parents=True,exist_ok=True)
                with t.extractfile(item) as source,p.open('xb') as out: shutil.copyfileobj(source,out)
                p.chmod(item.mode&0o777)
    return dest/next(iter(roots))


def download(root):
    rows=[]
    for name,url,size,digest in INPUTS:
        path=root/name;count=0;h=hashlib.sha256()
        with urllib.request.urlopen(url,timeout=30) as response,path.open('xb') as out:
            require(response.geturl().startswith('https://'),'HTTPS downgrade')
            for chunk in iter(lambda:response.read(1024*1024),b''):
                count+=len(chunk);require(count<=size,'download oversize');h.update(chunk);out.write(chunk)
        require(count==size and h.hexdigest()==digest,'source/NDK pin differs: '+name)
        rows.append({'file':name,'url':url,'bytes':count,'sha256':h.hexdigest()})
    return rows


def prepare(root):
    require(platform.system()=='Linux' and platform.machine()=='x86_64','Linux x86_64 required')
    require(not root.exists() and not root.is_symlink(),'fresh job root required')
    root.mkdir(mode=0o700)
    for n in ['inputs','sources','evidence','tmp','home']: (root/n).mkdir(mode=0o700)
    # Diagnostic data stays in evidence/, which only encrypted custody uploads.
    # Record measured prerequisites before each fail-closed admission check.
    disk=shutil.disk_usage(root.parent).free
    mem={line.split(':')[0]:int(line.split()[1])*1024 for line in Path('/proc/meminfo').read_text().splitlines() if line.startswith(('MemTotal:','MemAvailable:'))}
    diagnostic={'scope':'predownload-admission-diagnostic-only','disk_free_bytes':disk,
        'memory':mem,'network':{'status':'not-probed'},'downloads_started':False}
    diagnostic_path=root/'evidence/predownload-admission.json'
    def retain():
        diagnostic_path.write_text(json.dumps(diagnostic,indent=2)+'\n')
        diagnostic_path.chmod(0o600)
    retain()
    require(disk>=10*1024**3,'less than10GiB measured free disk')
    require(mem['MemAvailable']>=1500*1024**2,'less than1500MiB available RAM')
    v=module('input-validation')
    # Probe host capacity and isolation before any722MB NDK/source download.
    try:
        network=v.network_probe()
    except BaseException as error:
        diagnostic['network']={'status':'probe-exception','error_type':type(error).__name__}
        retain()
        raise
    # Fixed probe output only, no environment or unrelated host files. Bound the
    # retained JSON even if a diagnostic unexpectedly grows. Gate uses original.
    encoded=json.dumps(network)
    diagnostic['network']=(network if len(encoded.encode())<=32768 else
        {'status':'diagnostic-over-bound','bytes':len(encoded.encode()),
         'available':network.get('available'),'exit_code':network.get('exit_code'),
         'error':str(network.get('error',''))[:4096]})
    retain()
    require(network['available'],'network namespace isolation unavailable')
    receipt={'scope':'source-only-arm64-components-prerequisites','memory':mem,
        'pre_download_network':network,'pre_download_disk_free':shutil.disk_usage(root).free,
        'inputs':download(root/'inputs')}
    trees={}
    for name in ['openssl','expat']:
        archive=root/'inputs'/('openssl.tar.gz' if name=='openssl' else 'expat.tar.xz')
        tree=unpack_source(archive,root/'sources'/name);trees[name]=str(tree)
        subprocess.run(['/usr/bin/python3',str(Path(__file__).with_name('verify-source-tree.py')),
            '--archive',str(archive),'--sha256',sha(archive),'--tree',str(tree),
            '--out',str(root/'evidence'/(name+'-tree.json'))],check=True,timeout=120)
    # Unzip fixed archive only after its full SHA verified. unzip preserves
    # official executable bits and symlink targets; verify exact tree afterward.
    subprocess.run(['unzip','-q',str(root/'inputs/ndk.zip'),'-d',str(root/'ndk')],check=True,timeout=180)
    v=module('input-validation');ndk=root/'ndk/android-ndk-r28c'
    receipt['ndk']=v.verify_ndk(ndk,root/'inputs/ndk.zip')
    receipt['host']=v.host_tools();receipt['network']=v.network_probe()
    require(receipt['network']['available'],'network namespace isolation unavailable')
    receipt.update(source_archives_and_trees_verified=True,ndk_and_host_verified=True,trees=trees,ndk_root=str(ndk))
    (root/'evidence/prerequisites.json').write_text(json.dumps(receipt,indent=2)+'\n')
    return receipt


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--root',required=True);a=p.parse_args();prepare(Path(a.root))

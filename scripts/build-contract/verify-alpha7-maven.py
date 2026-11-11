#!/usr/bin/env python3
"""Verify selected staged Maven AAR bytes and their actual consumed/final-APK payloads."""
import argparse, hashlib, io, json, pathlib, zipfile, xml.etree.ElementTree as ET

def sha(data): return hashlib.sha256(data).hexdigest()

def verify(root, lock, resolved=None, apk=None, raw_path=None, selected_aars=None):
    root=pathlib.Path(root).resolve(strict=True)
    group=lock['group']; version=lock['version']; modules={'common','library','ffmpeg'}
    if {r['module'] for r in lock['artifacts']} != modules: raise ValueError('exactly three owned modules required')
    checked=[]
    for row in lock['artifacts']:
        module=row['module']; expected=f'{group}:{module}:{version}'
        if row['coordinate'] != expected: raise ValueError('coordinate mismatch')
        for name,entry in row['files'].items():
            relative=pathlib.PurePosixPath(name)
            if relative.is_absolute() or '..' in relative.parts: raise ValueError('unsafe artifact path')
            p=root/name
            if p.resolve() != p.absolute() or not p.is_file(): raise ValueError('missing/symlink artifact')
            data=p.read_bytes()
            if len(data)!=entry['bytes'] or sha(data)!=entry['sha256']: raise ValueError('artifact bytes differ: '+name)
        aar=root/row['aar']
        if row['aar'] not in row['files']: raise ValueError('AAR not digest-locked')
        with zipfile.ZipFile(aar) as z:
            names=z.namelist()
            if len(names)!=len(set(names)) or z.testzip() is not None: raise ValueError('bad AAR ZIP')
            if any(n.startswith(('jni/','lib/','prefab/')) or n.endswith('.so') for n in names): raise ValueError('AAR must not introduce native bytes')
            if module=='library':
                if sha(z.read('res/raw/ytdlp')) != lock['engine_sha256']: raise ValueError('Maven raw engine differs')
                if sha(z.read('assets/seali-engine/verify-runtime-engine.py')) != lock['checker_sha256']: raise ValueError('runtime checker differs')
                with zipfile.ZipFile(io.BytesIO(z.read('classes.jar'))) as classes:
                    for cls in ['YoutubeDL','VerifiedEngineReplacement','VerifiedPythonPayload','OwnedInvocation','OwnedPythonConfigKt']:
                        if f'com/yausername/youtubedl_android/{cls}.class' not in classes.namelist(): raise ValueError('required API/helper absent')
        pom=next(root/n for n in row['files'] if n.endswith('.pom'))
        tree=ET.fromstring(pom.read_bytes()); ns={'p':'http://maven.apache.org/POM/4.0.0'}
        own=[]
        for d in tree.findall('p:dependencies/p:dependency',ns):
            g=d.findtext('p:groupId',namespaces=ns); a=d.findtext('p:artifactId',namespaces=ns); v=d.findtext('p:version',namespaces=ns)
            if 'junkfood' in (g or ''): raise ValueError('old upstream Maven dependency remains')
            if g==group:
                if a not in modules or v!=version: raise ValueError('owned transitive dependency drift')
                own.append(a)
        if set(own) != {'common':set(), 'library':{'common'}, 'ffmpeg':{'common','library'}}[module]: raise ValueError('owned module transitive closure required')
        metadata=next(root/n for n in row['files'] if n.endswith('.module'))
        document=json.loads(metadata.read_text())
        component=document['component']
        if (component['group'],component['module'],component['version']) != (group,module,version): raise ValueError('Gradle module identity differs')
        runtimes=[]
        for variant in document['variants']:
            if variant.get('attributes',{}).get('org.gradle.category')!='library': continue
            variant_owned=[]
            for d in variant.get('dependencies',[]):
                if 'junkfood' in d['group']: raise ValueError('old provider in Gradle metadata')
                if d['group']==group:
                    if d['module'] not in modules or d.get('version',{}).get('requires')!=version: raise ValueError('Gradle module dependency drift')
                    variant_owned.append(d['module'])
            if variant.get('attributes',{}).get('org.gradle.usage')=='java-runtime': runtimes.append(set(variant_owned))
        if runtimes != [{'common':set(), 'library':{'common'}, 'ffmpeg':{'common','library'}}[module]]: raise ValueError('Gradle runtime module closure differs')
        checked.append({'coordinate':expected,'aar_sha256':sha(aar.read_bytes()),'native_payloads':0})
    if selected_aars is not None:
        actual=[sha(pathlib.Path(p).read_bytes()) for p in selected_aars]
        expected=[r['files'][r['aar']]['sha256'] for r in lock['artifacts']]
        if len(actual)!=3 or sorted(actual)!=sorted(expected): raise ValueError('actual packaging-selected AAR bytes differ')
    if resolved is not None:
        selected=[r for r in resolved if r['group']==group]
        if len(selected)!=3 or {r['name'] for r in selected}!=modules: raise ValueError('actual Gradle selection is not three owned modules')
        if any(r['group'].find('junkfood')>=0 for r in resolved): raise ValueError('old Maven provider selected')
        for row in selected:
            wanted=next(a for a in lock['artifacts'] if a['module']==row['name'])
            if row['version']!=version or sha(pathlib.Path(row['file']).read_bytes())!=wanted['files'][wanted['aar']]['sha256']: raise ValueError('Gradle-selected AAR differs')
    if apk is not None:
        if not raw_path: raise ValueError('raw path must be resolved from actual APK resource table')
        with zipfile.ZipFile(apk) as z:
            for name,expected in [('assets/functional-engine/yt-dlp',lock['engine_sha256']),('assets/seali-engine/verify-runtime-engine.py',lock['checker_sha256']),(raw_path,lock['engine_sha256'])]:
                if sha(z.read(name))!=expected: raise ValueError('final APK payload drift: '+name)
    return {'artifacts':checked,'engine_sha256':lock['engine_sha256'],'checker_sha256':lock['checker_sha256'],'gradle_selection_checked':resolved is not None or selected_aars is not None,'apk_payload_checked':apk is not None,'scope':'artifact identity/content only, not signing, runtime, API24, source/license completeness or release approval'}

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--root',required=True);p.add_argument('--lock',required=True);p.add_argument('--resolved-report');p.add_argument('--apk');p.add_argument('--raw-path');p.add_argument('--selected-aar',action='append');a=p.parse_args()
    print(json.dumps(verify(a.root,json.loads(pathlib.Path(a.lock).read_text()),json.loads(pathlib.Path(a.resolved_report).read_text()) if a.resolved_report else None,a.apk,a.raw_path,a.selected_aar),indent=2))

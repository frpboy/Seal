#!/usr/bin/env python3
"""Check staged alpha7 input custody only. Never download, execute, sign or edit inputs."""
import argparse,hashlib,json,pathlib

def verify(root, lock, native_roots=None):
    root=root.resolve(strict=True)
    for name,row in {**lock['files'],**lock['tracked_files']}.items():
        parts=pathlib.PurePosixPath(name)
        if parts.is_absolute() or '..' in parts.parts or str(parts)!=name:raise ValueError('unsafe lock path')
        path=root/name
        for ancestor in [path,*path.parents]:
            if ancestor==root:break
            if ancestor.is_symlink():raise ValueError('symlink input: '+name)
        if not path.is_file():raise ValueError('Missing reviewed alpha7 input: '+name+'; stage the locked ARM64 archive, never substitute upstream native AARs.')
        if path.stat().st_size!=row['bytes'] or hashlib.sha256(path.read_bytes()).hexdigest()!=row['sha256']:raise ValueError('input differs from reviewed custody: '+name)
    if 'maven_lock' in lock:
        import importlib.util
        spec=importlib.util.spec_from_file_location('alpha7_maven',root/'scripts/build-contract/verify-alpha7-maven.py')
        verifier=importlib.util.module_from_spec(spec);spec.loader.exec_module(verifier)
        verifier.verify(root,json.loads((root/lock['maven_lock']).read_text()))
    expected={p for p in lock['files'] if '/jniLibs/' in p}
    actual=set()
    if native_roots is None:
        # Test/default CLI roots cover conventional sourceSets in each actual source module.
        modules = ['app','color','dependencies/frpboy-common','dependencies/frpboy-ffmpeg','dependencies/frpboy-library','dependencies/frpboy-mmkv','dependencies/frpboy-mmkvannotation']
        native_roots = [p for module in modules for p in (root/module/'src').glob('*/jniLibs')]
    for directory in native_roots:
        directory=pathlib.Path(directory)
        if not directory.is_absolute(): directory=root/directory
        if not directory.resolve().is_relative_to(root):raise ValueError('native sourceSet outside repository')
        if directory.is_symlink():raise ValueError('symlink native sourceSet')
        for p in directory.rglob('*'):
            if p.is_symlink():raise ValueError('symlink native input')
            if p.is_file():actual.add(str(p.relative_to(root)))
    if actual!=expected:raise ValueError('unexpected native input paths: '+str(sorted(actual^expected)))
    return {'verified_files':len(lock['files'])+len(lock['tracked_files']),'scope':'input custody only; not APK/runtime/release qualification'}

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--root',type=pathlib.Path,required=True);p.add_argument('--native-root',type=pathlib.Path,action='append');a=p.parse_args()
    lock=json.loads((pathlib.Path(__file__).parent/'alpha7-input-lock.json').read_text())
    print(json.dumps(verify(a.root,lock,a.native_root)))

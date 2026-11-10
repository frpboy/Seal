#!/usr/bin/env python3
"""Private alpha ARM64 data-only payload assembly from independently reviewed native stage."""
import argparse,pathlib,hashlib,json,shutil,zipfile
p=argparse.ArgumentParser();p.add_argument('--stage',required=True);p.add_argument('--python-pruned',required=True);p.add_argument('--root',required=True);a=p.parse_args();stage=pathlib.Path(a.stage);root=pathlib.Path(a.root);python=pathlib.Path(a.python_pruned)
expected={'ffmpeg': 'bd3788f614dd4d063870302de37eec0d820d36f51208e72528e30125679610ea', 'ffprobe': '7fca21d996de35074fff1100947b65428c6a6ea315b8a1daf4a7667c0b39edc4', 'libavcodec.so': '8e2965bc1a22b542d8689464a438b2d263720356c51fb6814e777a9db814f7ec', 'libavdevice.so': 'c70ea65a745d82b8e78dd0559944c3f9e8d5bb1894dd3e649648d329218d654d', 'libavfilter.so': '2c741120e23084dedc8656e883a28a7147c451606aa477dde2f339f69e7499de', 'libavformat.so': 'e1d044ea4120f3aadd5043522f728f1033cf7a06cde0574741fe04d7ddcf10f8', 'libavutil.so': 'a27e717868dc46a9b08d373f473d5f8cde7eb225acc90b36eab7e588a7829d91', 'libswresample.so': 'c52e5f11c558483f29b3cd340b85c310a0903c9796cd5b0758615bac76eba4c5', 'libswscale.so': 'ade24b8718cf36d1c12c2cda0ba868cdd344b7548f69f93a3759f8c11cece127'}
for name,want in expected.items():
 source=stage/'usr'/('lib' if name.endswith('.so') else 'bin')/name
 assert not source.is_symlink() and hashlib.sha256(source.read_bytes()).hexdigest()==want,name
manifest=json.loads((python/'python-prune-manifest.json').read_text());py=next(x for x in manifest if x['abi']=='x86_64')
for name,key in [('libpython.so','launcher_unchanged_sha256'),('libpython.zip.so','output_zip_sha256')]:assert hashlib.sha256((python/'x86_64'/name).read_bytes()).hexdigest()==py[key]
dest=root/'app/src/main/jniLibs/x86_64';assert not dest.exists(),'fresh payload destination required';dest.mkdir(parents=True)
for name in ['libpython.so','libpython.zip.so']:shutil.copyfile(python/'x86_64'/name,dest/name)
for name in ['ffmpeg','ffprobe']:shutil.copyfile(stage/'usr/bin'/name,dest/f'lib{name}.so')
with zipfile.ZipFile(dest/'libffmpeg.zip.so','w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
 for name in sorted(n for n in expected if n.endswith('.so')):
  info=zipfile.ZipInfo('usr/lib/'+name,date_time=(1980,1,1,0,0,0));info.external_attr=0o100755<<16;info.compress_type=zipfile.ZIP_DEFLATED;z.writestr(info,(stage/'usr/lib'/name).read_bytes())
rows=[{'path':str(f.relative_to(root)),'bytes':f.stat().st_size,'sha256':hashlib.sha256(f.read_bytes()).hexdigest()} for f in sorted(dest.iterdir())]
(root/'functional-alpha-payload-manifest.json').write_text(json.dumps({'target':'x86_64','native_build_run':'https://github.com/frpboy/Seali/actions/runs/42875575427','native_source_sha':'5f77fbf6fac44d351f9a21ad804f8cfc892ad471','files':rows,'target_executed':False,'apk_built':False,'public_release_ready':False},indent=2)+'\n')

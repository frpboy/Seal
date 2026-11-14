#!/usr/bin/env python3
"""Private alpha ARM64 data-only payload assembly from independently reviewed native stage."""
import argparse,pathlib,hashlib,json,shutil,zipfile
p=argparse.ArgumentParser();p.add_argument('--stage',required=True);p.add_argument('--python-pruned',required=True);p.add_argument('--root',required=True);a=p.parse_args();stage=pathlib.Path(a.stage);root=pathlib.Path(a.root);python=pathlib.Path(a.python_pruned)
expected={'ffmpeg':'cd1b02f0e69b4e763c24260e2d8248473988efde15ce3d93dfe99c961624bbb0','ffprobe':'1a17e0d69db1dd8825427928cde93ea8e19ae67a41f89f9696e5242a38efc23d','libavcodec.so':'f54dabb565be743e44bc0ca97686863d861b769dbe0c8d6365c4c77731da1c0a','libavutil.so':'3b6123f9fea84feebad9010b6f401bc658562efbf935b7a95701240316ed02e5','libswscale.so':'e508675e7338cc1cd711678034885873fdcd9c2513e5b0f159fd48af98a2ec2e','libavformat.so':'c8d0f04c060c2cc8ef5e6a3e5d908240a81b40da3b6c4d56c4afbcd4b9639779','libavfilter.so':'bdad9d88f3506d8da87f4950e08fc4a16962f811e709926c849f9625e3016853','libswresample.so':'329db479514544b06c6a997eed7bdb6212d01755fa9ab0968068ea550d2af554','libavdevice.so':'6806cb3fb49d3b9a048535c0ef8da20a9b68178366d2193ce8c36e528e0ccb9b'}
for name,want in expected.items():
 source=stage/'usr'/('lib' if name.endswith('.so') else 'bin')/name
 assert not source.is_symlink() and hashlib.sha256(source.read_bytes()).hexdigest()==want,name
manifest=json.loads((python/'python-prune-manifest.json').read_text());py=next(x for x in manifest if x['abi']=='arm64-v8a')
for name,key in [('libpython.so','launcher_unchanged_sha256'),('libpython.zip.so','output_zip_sha256')]:assert hashlib.sha256((python/'arm64-v8a'/name).read_bytes()).hexdigest()==py[key]
dest=root/'app/src/main/jniLibs/arm64-v8a';assert not dest.exists(),'fresh payload destination required';dest.mkdir(parents=True)
for name in ['libpython.so','libpython.zip.so']:shutil.copyfile(python/'arm64-v8a'/name,dest/name)
for name in ['ffmpeg','ffprobe']:shutil.copyfile(stage/'usr/bin'/name,dest/f'lib{name}.so')
with zipfile.ZipFile(dest/'libffmpeg.zip.so','w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
 for name in sorted(n for n in expected if n.endswith('.so')):
  info=zipfile.ZipInfo('usr/lib/'+name,date_time=(1980,1,1,0,0,0));info.external_attr=0o100755<<16;info.compress_type=zipfile.ZIP_DEFLATED;z.writestr(info,(stage/'usr/lib'/name).read_bytes())
rows=[{'path':str(f.relative_to(root)),'bytes':f.stat().st_size,'sha256':hashlib.sha256(f.read_bytes()).hexdigest()} for f in sorted(dest.iterdir())]
(root/'functional-alpha-payload-manifest.json').write_text(json.dumps({'target':'arm64-v8a','native_build_run':'https://github.com/frpboy/Seali/actions/runs/42871513921','native_source_sha':'e681df4221bc26b37d6cc774d097a61348f18c26','files':rows,'target_executed':False,'apk_built':False,'public_release_ready':False},indent=2)+'\n')

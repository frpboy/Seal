#!/usr/bin/env python3
"""Data-only functional alpha wrapper preparation. Native FFmpeg packaging stays separately gated."""
import argparse,hashlib,pathlib,zipfile,io,json
p=argparse.ArgumentParser();p.add_argument('--input-dir',required=True);p.add_argument('--root',required=True);a=p.parse_args();inputs=pathlib.Path(a.input_dir);root=pathlib.Path(a.root)
engine=(inputs/'yt-dlp').read_bytes();engine_sha='e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3';assert hashlib.sha256(engine).hexdigest()==engine_sha
locks={'library':'9d29b148187242d6e754fb59f126b62dde5c1c7fcadaa29d14dbe13659495e4c','common':'92b33cdab9f5451a6258416db5b1034487641132bf3b40ff4ee9507e444ba198','ffmpeg':'e12402803f7a61802fded6bd021422688cb7a03e924068a1aec983dae88ea3c1'}
records=[]
for name,want in locks.items():
 b=(inputs/f'{name}-0.17.3.aar').read_bytes();assert hashlib.sha256(b).hexdigest()==want
 dst=root/'app/libs/functional-alpha'/f'{name}-wrapper.aar';dst.parent.mkdir(parents=True,exist_ok=True);changed=[]
 with zipfile.ZipFile(io.BytesIO(b)) as inp,zipfile.ZipFile(dst,'w',zipfile.ZIP_DEFLATED) as out:
  for i in inp.infolist():
   if i.filename.startswith('jni/'):
    changed.append({'path':i.filename,'action':'remove inherited native payload'});continue
   data=inp.read(i)
   if i.filename=='res/raw/ytdlp':
    data=engine;changed.append({'path':i.filename,'action':'replace same pinned reviewed fallback engine','sha256':engine_sha})
   out.writestr(i,data)
 with zipfile.ZipFile(dst) as check:
  assert not any(n.startswith('jni/') for n in check.namelist())
  if name=='library':assert hashlib.sha256(check.read('res/raw/ytdlp')).hexdigest()==engine_sha
 records.append({'component':name,'input_sha256':want,'output_sha256':hashlib.sha256(dst.read_bytes()).hexdigest(),'changes':changed})
assets=root/'app/src/main/assets/functional-engine';assets.mkdir(parents=True,exist_ok=True);(assets/'yt-dlp').write_bytes(engine)
(root/'functional-alpha-wrapper-manifest.json').write_text(json.dumps(records,indent=2)+'\n')

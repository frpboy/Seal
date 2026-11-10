#!/usr/bin/env python3
"""Remove native/runtime payloads from pinned AARs for uiOnly only. No code execution."""
import hashlib,json,pathlib,urllib.request,zipfile
root=pathlib.Path(__file__).resolve().parents[1]
rows=json.loads((root/'ui-preview-wrapper-transform.json').read_text())
for row in rows:
 name=row['component'];url=f'https://repo.maven.apache.org/maven2/io/github/junkfood02/youtubedl-android/{name}/0.17.3/{name}-0.17.3.aar'
 data=urllib.request.urlopen(url).read()
 assert hashlib.sha256(data).hexdigest()==row['original_sha256'],name+' input hash drift'
 import io
 dst=root/'app/libs/ui-preview'/f'{name}-wrapper-only.aar';dst.parent.mkdir(parents=True,exist_ok=True)
 with zipfile.ZipFile(io.BytesIO(data)) as z,zipfile.ZipFile(dst,'w',zipfile.ZIP_DEFLATED) as out:
  removed=[]
  for i in z.infolist():
   if i.filename.startswith('jni/') or i.filename=='res/raw/ytdlp':removed.append(i.filename);continue
   if not i.is_dir():out.writestr(i,z.read(i))
 assert removed==row['removed'],name+' removal inventory drift'
 assert hashlib.sha256(dst.read_bytes()).hexdigest()==row['output_sha256'],name+' output hash drift'
 print(name,'wrapper only verified')

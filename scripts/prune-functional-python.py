#!/usr/bin/env python3
"""Deterministic data-only GPL optional Python module cut; no target execution."""
import argparse,hashlib,io,json,pathlib,zipfile
p=argparse.ArgumentParser();p.add_argument('--library-aar',required=True);p.add_argument('--out',required=True);a=p.parse_args()
src=pathlib.Path(a.library_aar);data=src.read_bytes()
assert hashlib.sha256(data).hexdigest()=='9d29b148187242d6e754fb59f126b62dde5c1c7fcadaa29d14dbe13659495e4c'
removed_names={'usr/lib/libgdbm.so','usr/lib/libgdbm_compat.so','usr/lib/libreadline.so','usr/lib/libreadline.so.8','usr/lib/libreadline.so.8.2','usr/lib/libhistory.so','usr/lib/libhistory.so.8','usr/lib/libhistory.so.8.2','usr/lib/python3.11/lib-dynload/_gdbm.cpython-311.so','usr/lib/python3.11/lib-dynload/_dbm.cpython-311.so','usr/lib/python3.11/lib-dynload/readline.cpython-311.so','usr/lib/python3.11/lib-dynload/_curses_panel.cpython-311.so'}
out=pathlib.Path(a.out);out.mkdir(exist_ok=True,parents=True);manifest=[]
with zipfile.ZipFile(io.BytesIO(data)) as z:
 for abi in ['arm64-v8a','x86_64']:
  raw=z.read('jni/'+abi+'/libpython.zip.so');records=[];dst=io.BytesIO()
  with zipfile.ZipFile(io.BytesIO(raw)) as inp,zipfile.ZipFile(dst,'w') as target:
   found=set()
   for info in inp.infolist():
    b=inp.read(info)
    if info.filename in removed_names:
     found.add(info.filename);records.append({'path':info.filename,'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest(),'reason':('optional broken module: inherited missing libpanelw.so.6' if info.filename.endswith('_curses_panel.cpython-311.so') else 'optional GPL readline/GDBM linked module/library or its symlink')})
    else:target.writestr(info,b)
   assert found==removed_names,sorted(removed_names^found)
  d=out/abi;d.mkdir(exist_ok=True);b=dst.getvalue();(d/'libpython.zip.so').write_bytes(b);launcher=z.read('jni/'+abi+'/libpython.so');(d/'libpython.so').write_bytes(launcher)
  manifest.append({'abi':abi,'input_zip_sha256':hashlib.sha256(raw).hexdigest(),'output_zip_sha256':hashlib.sha256(b).hexdigest(),'output_zip_bytes':len(b),'launcher_unchanged_sha256':hashlib.sha256(launcher).hexdigest(),'removed':records,'native_execution':False})
(out/'python-prune-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
print(json.dumps(manifest,indent=2))

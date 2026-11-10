#!/usr/bin/env python3
"""Static pruned Python ELF and symlink closure. Never runs a target binary."""
import argparse,pathlib,zipfile,stat,os,posixpath,subprocess,re,json,tempfile
p=argparse.ArgumentParser();p.add_argument('--root',required=True);p.add_argument('--ndk',required=True);a=p.parse_args();root=pathlib.Path(a.root);readelf=pathlib.Path(a.ndk)/'toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf';rows=[]
for abi in ['arm64-v8a','x86_64']:
 graph=[];links=[]
 with zipfile.ZipFile(root/abi/'libpython.zip.so') as z,tempfile.TemporaryDirectory() as temp:
  names=set(z.namelist());provided={pathlib.PurePosixPath(n).name for n in names};system={'libc.so','libm.so','libdl.so','liblog.so','libz.so','libandroid.so'}
  assert len(z.namelist())==len(names) and z.testzip() is None
  for i in z.infolist():
   b=z.read(i)
   if stat.S_ISLNK(i.external_attr>>16):
    target=b.decode();dest=posixpath.normpath(posixpath.join(str(pathlib.PurePosixPath(i.filename).parent),target));assert not target.startswith('/') and dest in names,(i.filename,target);links.append({'path':i.filename,'target':target})
   if b[:4]!=b'\x7fELF':continue
   f=pathlib.Path(temp)/'elf';f.write_bytes(b);text=subprocess.check_output([str(readelf),'-d',str(f)],text=True);needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',text);assert not set(needed)-provided-system,(i.filename,needed)
   assert not any(any(s in n for s in ['gdbm','readline','libhistory']) for n in needed)
   graph.append({'entry':i.filename,'needed':needed})
  f=pathlib.Path(temp)/'launcher';f.write_bytes((root/abi/'libpython.so').read_bytes());text=subprocess.check_output([str(readelf),'-d',str(f)],text=True);needed=re.findall(r'\(NEEDED\).*?\[(.*?)\]',text);assert not set(needed)-provided-system
 rows.append({'abi':abi,'elf_count':len(graph),'symlink_count':len(links),'broken_links':[],'unresolved_needed':[],'needed_graph':graph,'symlinks':links,'launcher_needed':needed,'target_executed':False})
(root/'python-prune-closure.json').write_text(json.dumps(rows,indent=2)+'\n')
print('PASS static Python closure both selected qualification ABIs')

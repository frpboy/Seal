#!/usr/bin/env python3
"""Data-only replacement of three qualified shared components. No native execution."""
import argparse,pathlib,zipfile,stat,hashlib,json,io,re,subprocess,tempfile
p=argparse.ArgumentParser();p.add_argument('--root',required=True);p.add_argument('--stage',required=True);p.add_argument('--abi',required=True,choices=['arm64-v8a','x86_64']);p.add_argument('--lock',required=True);p.add_argument('--readelf',required=True);a=p.parse_args()
root=pathlib.Path(a.root);stage=pathlib.Path(a.stage);lock=json.loads(pathlib.Path(a.lock).read_text());entry=lock[a.abi]
sha=lambda b:hashlib.sha256(b).hexdigest()
source=root/a.abi/'libpython.zip.so';original=source.read_bytes();launcher=root/a.abi/'libpython.so';launcher_sha=sha(launcher.read_bytes())
assert sha(original)==entry['pruned_python_sha256'],'Need exact reviewed pruned input, not an already modified ZIP'
prune_manifest=json.loads((root/'python-prune-manifest.json').read_text());row=next(x for x in prune_manifest if x['abi']==a.abi)
assert row['output_zip_sha256']==sha(original) and row['launcher_unchanged_sha256']==launcher_sha
assert set(entry['new_libraries'])=={'libssl.so.3','libcrypto.so.3','libexpat.so.1'} and entry['build_run'],'All three qualified components and actual build provenance required'
replacement={};dynamic={}
for soname,r in entry['new_libraries'].items():
 f=stage/r['physical_file'];assert f.is_file() and not f.is_symlink();b=f.read_bytes();assert sha(b)==r['sha256'],soname
 text=subprocess.check_output([a.readelf,'-d','--wide',str(f)],text=True)
 assert re.search(r'\(SONAME\).*\['+re.escape(soname)+r'\]',text),soname
 needed=re.findall(r'\(NEEDED\).*\[(.*?)\]',text)
 if soname=='libssl.so.3':assert 'libcrypto.so.3' in needed and 'libcrypto.so' not in needed
 replacement['usr/lib/'+soname]=b;dynamic[soname]={'needed':needed,'sha256':sha(b),'physical_source':r['physical_file']}
removed=[];preserved=[];buf=io.BytesIO()
with zipfile.ZipFile(io.BytesIO(original)) as inp,zipfile.ZipFile(buf,'w') as out:
 assert len(inp.namelist())==len(set(inp.namelist())) and inp.testzip() is None
 actual={i.filename for i in inp.infolist() if i.filename in entry['remove_exact'] or i.filename.startswith(('usr/lib/engines-3/','usr/lib/ossl-modules/'))}
 assert actual==set(entry['remove_exact']),sorted(actual^set(entry['remove_exact']))
 for info in inp.infolist():
  b=inp.read(info)
  if info.filename in actual:
   assert sha(b)==entry['remove_exact'][info.filename],info.filename
   removed.append({'path':info.filename,'sha256':sha(b),'bytes':len(b),'mode':oct(info.external_attr>>16)})
  else:out.writestr(info,b);preserved.append({'path':info.filename,'sha256':sha(b),'mode':info.external_attr})
 for name,b in sorted(replacement.items()):
  i=zipfile.ZipInfo(name,(1980,1,1,0,0,0));i.create_system=3;i.external_attr=0o100755<<16;i.compress_type=zipfile.ZIP_DEFLATED;out.writestr(i,b)
 for name,target in [('libssl.so','libssl.so.3'),('libcrypto.so','libcrypto.so.3'),('libexpat.so','libexpat.so.1')]:
  i=zipfile.ZipInfo('usr/lib/'+name,(1980,1,1,0,0,0));i.create_system=3;i.external_attr=0o120777<<16;out.writestr(i,target.encode())
updated=buf.getvalue()
with zipfile.ZipFile(io.BytesIO(updated)) as z:
 assert len(z.namelist())==len(set(z.namelist())) and z.testzip() is None
 assert not any(n.startswith(('usr/lib/engines-3/','usr/lib/ossl-modules/')) for n in z.namelist())
 for r in preserved:
  i=z.getinfo(r['path']);assert sha(z.read(i))==r['sha256'] and i.external_attr==r['mode'],r['path']
 for old in entry['remove_exact']:
  if old not in replacement and old not in ['usr/lib/libssl.so','usr/lib/libcrypto.so','usr/lib/libexpat.so']:assert old not in z.namelist(),old
source.write_bytes(updated)
assert sha(launcher.read_bytes())==launcher_sha
row['output_zip_sha256']=sha(updated);row['output_zip_bytes']=len(updated);row['components_replaced']=True;row['previous_pruned_zip_sha256']=sha(original)
(root/'python-prune-manifest.json').write_text(json.dumps(prune_manifest,indent=2)+'\n')
receipt={'abi':a.abi,'source_commit':lock['source_commit'],'build_run':entry['build_run'],'old_zip_sha256':sha(original),'new_zip_sha256':sha(updated),'new_zip_bytes':len(updated),'launcher_unchanged_sha256':launcher_sha,'removed':removed,'new_libraries':dynamic,'new_aliases':{'libssl.so':'libssl.so.3','libcrypto.so':'libcrypto.so.3','libexpat.so':'libexpat.so.1'},'preserved_entry_count':len(preserved),'target_executed':False,'runtime_or_security_clear':False}
(root/('python-component-replacement-'+a.abi+'.json')).write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps({k:v for k,v in receipt.items() if k not in ['removed','new_libraries']},indent=2))

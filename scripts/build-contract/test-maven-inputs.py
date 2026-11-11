#!/usr/bin/env python3
import importlib.util,json,pathlib,tempfile,shutil,unittest,hashlib,zipfile,io
ROOT=pathlib.Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('maven_verify',ROOT/'scripts/build-contract/verify-alpha7-maven.py');v=importlib.util.module_from_spec(spec);spec.loader.exec_module(v)
class Contract(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.root=pathlib.Path(self.tmp.name);self.lock=json.loads((ROOT/'scripts/build-contract/alpha7-maven-lock.json').read_text());shutil.copytree(ROOT/'.qualification/maven',self.root/'.qualification/maven')
 def tearDown(self):self.tmp.cleanup()
 def reject(self):
  with self.assertRaises((ValueError,KeyError,zipfile.BadZipFile)):v.verify(self.root,self.lock)
 def rebind(self,row,path):
  p=self.root/path;row['files'][path]={'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
 def test_exact_staged_contract_passes(self):self.assertEqual(len(v.verify(self.root,self.lock)['artifacts']),3)
 def test_changed_aar_rejected(self):
  p=self.root/self.lock['artifacts'][0]['aar'];p.write_bytes(p.read_bytes()+b'bad');self.reject()
 def test_checker_identity_must_match(self):self.lock['checker_sha256']='0'*64;self.reject()
 def test_engine_identity_must_match(self):self.lock['engine_sha256']='0'*64;self.reject()
 def test_no_native_substitution_even_if_rehashed(self):
  row=self.lock['artifacts'][0];p=self.root/row['aar']
  with zipfile.ZipFile(p,'a') as z:z.writestr('jni/arm64-v8a/opaque.so',b'native')
  self.rebind(row,row['aar']);self.reject()
 def test_unsafe_path_rejected(self):self.lock['artifacts'][0]['files']['../outside']={'bytes':0,'sha256':'0'*64};self.reject()
 def test_old_provider_in_pom_rejected_even_if_rehashed(self):
  row=self.lock['artifacts'][1];name=next(n for n in row['files'] if n.endswith('.pom'));p=self.root/name;p.write_text(p.read_text().replace('<groupId>com.fasterxml.jackson.core</groupId>','<groupId>io.github.junkfood02.youtubedl-android</groupId>'));self.rebind(row,name);self.reject()
 def test_wrong_actual_gradle_selection_rejected(self):
  selected=[{'group':self.lock['group'],'name':r['module'],'version':self.lock['version'],'file':str(self.root/r['aar'])} for r in self.lock['artifacts']];selected[0]['version']='stale'
  with self.assertRaises(ValueError):v.verify(self.root,self.lock,selected)
 def test_final_apk_missing_checker_rejected(self):
  p=self.root/'missing.apk'
  with zipfile.ZipFile(p,'w') as z:z.writestr('assets/functional-engine/yt-dlp',b'wrong')
  with self.assertRaises((ValueError,KeyError)):v.verify(self.root,self.lock,apk=p,raw_path='res/raw/ytdlp')
 def test_module_runtime_dependency_drift_rejected_even_if_rehashed(self):
  row=self.lock['artifacts'][1];name=next(n for n in row['files'] if n.endswith('.module'));p=self.root/name;doc=json.loads(p.read_text())
  for variant in doc['variants']:
   for d in variant.get('dependencies',[]):
    if d['group']==self.lock['group']:d['version']['requires']='stale'
  p.write_text(json.dumps(doc));self.rebind(row,name);self.reject()
 def selected(self):return [self.root/r['aar'] for r in self.lock['artifacts']]
 def test_selected_aar_exact_bytes_pass(self):self.assertTrue(v.verify(self.root,self.lock,selected_aars=self.selected())['gradle_selection_checked'])
 def test_selected_aar_missing_rejected(self):
  with self.assertRaises(ValueError):v.verify(self.root,self.lock,selected_aars=self.selected()[:2])
 def test_selected_aar_duplicate_rejected(self):
  a=self.selected();a[0]=a[1]
  with self.assertRaises(ValueError):v.verify(self.root,self.lock,selected_aars=a)
 def test_selected_aar_modified_cache_rejected(self):
  a=self.selected();p=self.root/'cached.aar';p.write_bytes(a[0].read_bytes()+b'changed');a[0]=p
  with self.assertRaises(ValueError):v.verify(self.root,self.lock,selected_aars=a)
 def apk(self,change=None):
  with zipfile.ZipFile(self.root/self.lock['artifacts'][1]['aar']) as z:engine=z.read('res/raw/ytdlp');checker=z.read('assets/seali-engine/verify-runtime-engine.py')
  data={'assets/functional-engine/yt-dlp':engine,'assets/seali-engine/verify-runtime-engine.py':checker,'res/optimized':engine}
  if change:data[change]=b'changed'
  p=self.root/'final.apk'
  with zipfile.ZipFile(p,'w') as z:
   for n,b in data.items():z.writestr(n,b)
  return p
 def test_final_apk_optimized_raw_exact_payloads_pass(self):self.assertTrue(v.verify(self.root,self.lock,apk=self.apk(),raw_path='res/optimized')['apk_payload_checked'])
 def test_final_apk_each_payload_drift_rejected(self):
  for n in ['assets/functional-engine/yt-dlp','assets/seali-engine/verify-runtime-engine.py','res/optimized']:
   with self.subTest(n=n),self.assertRaises(ValueError):v.verify(self.root,self.lock,apk=self.apk(n),raw_path='res/optimized')
 def test_final_apk_requires_table_resolved_path(self):
  with self.assertRaises(ValueError):v.verify(self.root,self.lock,apk=self.apk())
 def test_symlink_repository_artifact_rejected(self):
  p=self.root/self.lock['artifacts'][0]['aar'];target=self.root/'outside.aar';p.rename(target);p.symlink_to(target);self.reject()
 def test_missing_required_recovery_api_rejected_even_if_rehashed(self):
  row=self.lock['artifacts'][1];p=self.root/row['aar']
  with zipfile.ZipFile(p) as z:entries={n:z.read(n) for n in z.namelist()}
  with zipfile.ZipFile(io.BytesIO(entries['classes.jar'])) as z:classes={n:z.read(n) for n in z.namelist() if not n.endswith('/VerifiedEngineReplacement.class')}
  buf=io.BytesIO()
  with zipfile.ZipFile(buf,'w') as z:
   for n,b in classes.items():z.writestr(n,b)
  entries['classes.jar']=buf.getvalue()
  with zipfile.ZipFile(p,'w') as z:
   for n,b in entries.items():z.writestr(n,b)
  self.rebind(row,row['aar']);self.reject()
if __name__=='__main__':unittest.main()

from pathlib import Path
import importlib.util,tempfile,hashlib,unittest
s=importlib.util.spec_from_file_location('v',Path(__file__).with_name('verify-alpha7-inputs.py'));m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class Inputs(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);self.root=Path(self.tmp.name);self.name='app/src/main/jniLibs/arm64-v8a/libtest.so';self.p=self.root/self.name;self.p.parent.mkdir(parents=True);self.p.write_bytes(b'known');self.lock={'files':{self.name:{'bytes':5,'sha256':hashlib.sha256(b'known').hexdigest()}},'tracked_files':{}}
 def test_positive(self):self.assertEqual(m.verify(self.root,self.lock)['verified_files'],1)
 def test_missing(self):
  self.p.unlink()
  with self.assertRaises(ValueError):m.verify(self.root,self.lock)
 def test_drift(self):
  self.p.write_bytes(b'wrong')
  with self.assertRaises(ValueError):m.verify(self.root,self.lock)
 def test_extra(self):
  self.p.with_name('unexpected.so').write_bytes(b'extra')
  with self.assertRaises(ValueError):m.verify(self.root,self.lock)
 def test_symlink(self):
  real=self.root/'real';real.write_bytes(b'known');self.p.unlink();self.p.symlink_to(real)
  with self.assertRaises(ValueError):m.verify(self.root,self.lock)
 def test_traversal(self):
  self.lock['files']['../outside']={'bytes':0,'sha256':''}
  with self.assertRaises(ValueError):m.verify(self.root,self.lock)
 def test_rogue_module_sourceset(self):
  p=self.root/'dependencies/frpboy-library/src/debug/jniLibs/arm64-v8a/rogue.so';p.parent.mkdir(parents=True);p.write_bytes(b'rogue')
  with self.assertRaises(ValueError):m.verify(self.root,self.lock)
 def test_explicit_custom_sourceset(self):
  p=self.root/'custom/native/rogue.so';p.parent.mkdir(parents=True);p.write_bytes(b'rogue')
  with self.assertRaises(ValueError):m.verify(self.root,self.lock,[self.p.parent,self.root/'custom/native'])
 def test_missing_tracked(self):
  self.lock['tracked_files']['tracked-engine']={'bytes':5,'sha256':hashlib.sha256(b'known').hexdigest()}
  with self.assertRaises(ValueError):m.verify(self.root,self.lock)
 def test_drift_tracked(self):
  p=self.root/'tracked-engine';p.write_bytes(b'wrong');self.lock['tracked_files']['tracked-engine']={'bytes':5,'sha256':hashlib.sha256(b'known').hexdigest()}
  with self.assertRaises(ValueError):m.verify(self.root,self.lock)
if __name__=='__main__':unittest.main()

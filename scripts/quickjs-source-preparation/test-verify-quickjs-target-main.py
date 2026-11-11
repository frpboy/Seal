#!/usr/bin/env python3
"""Mocked readelf/API24-stub main-path fixtures, no target/native execution."""
import importlib.util,pathlib,tempfile,sys,unittest,struct,json
from unittest.mock import patch
D=pathlib.Path(__file__).parent
s=importlib.util.spec_from_file_location('layout_tests',D/'test-verify-quickjs-target.py');t=importlib.util.module_from_spec(s);s.loader.exec_module(t);m=t.m
class Main(unittest.TestCase):
 def run_case(self,case):
  with tempfile.TemporaryDirectory() as d:
   d=pathlib.Path(d);ndk=d/'ndk';ndk.mkdir();(ndk/'source.properties').write_text('Pkg.Revision = 28.2.13676358')
   stub=ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/24/libc.so';stub.parent.mkdir(parents=True);stub.write_bytes(b'fixture-stub')
   elf=d/'libqjs.so';elf.write_bytes(t.fixture());out=d/'evidence'
   dyn='(NEEDED) Shared library: [libc.so]\n(FLAGS) BIND_NOW\n(FLAGS_1) Flags: NOW PIE'
   if case=='badneeded':dyn=dyn.replace('libc.so','libssl.so')
   if case=='nopie':dyn=dyn.replace('NOW PIE','NOW')
   if case=='rpath':dyn+='\n(RUNPATH) Library runpath: [/bad]'
   imports='  1: 0000000000000000 0 FUNC GLOBAL DEFAULT UND malloc@LIBC (2)\n  2: 0000000000000000 0 NOTYPE WEAK DEFAULT UND optional@LIBC'
   if case=='missing':imports=imports.replace('malloc','not_in_api24')
   def read(args,**kw):
    if str(stub)==args[-1]:return '  1: 0000000000001000 40 FUNC GLOBAL DEFAULT 5 malloc@@LIBC'
    if args[1]=='-d':return dyn
    if args[1]=='--dyn-symbols':return imports
    return 'mock complete readelf source'
   with patch.object(sys,'argv',['verify','--elf',str(elf),'--ndk',str(ndk),'--abi','arm64-v8a','--out',str(out)]),patch('subprocess.check_output',side_effect=read):m.main()
   return json.loads((out/'static-target-check.json').read_text())
 def test_positive_receipt_not_runtime(self):
  r=self.run_case('ok');self.assertEqual(r['weak_undefined_imports'],['optional']);self.assertFalse(r['runtime_tested'])
 def test_reject_bad_needed_missing_strong_pie_and_rpath(self):
  for c in ['badneeded','missing','nopie','rpath']:
   with self.subTest(c=c),self.assertRaises(AssertionError):self.run_case(c)
if __name__=='__main__':unittest.main()
